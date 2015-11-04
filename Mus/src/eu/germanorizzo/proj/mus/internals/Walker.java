package eu.germanorizzo.proj.mus.internals;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import eu.germanorizzo.proj.mus.internals.FileList.*;
import eu.germanorizzo.proj.mus.utils.*;

public class Walker {
	public static enum State {
		NEW, BUILDING, CALCULATING, FINISHED;
	}

	public class Status {
		public final State state;
		public final int totFiles, doneFilesOk, doneFilesKo;
		public final int percentageOn10k;
		public final long totSize, bytesPerSecond;
		public final int secondsRemaining;

		protected Status() {
			state = Walker.this.state;
			switch (state) {
			case BUILDING:
				totFiles = Walker.this.fileList.size();
				totSize = totalSize.get();
				doneFilesOk = 0;
				doneFilesKo = 0;
				percentageOn10k = 0;
				bytesPerSecond = 0;
				secondsRemaining = 0;
				break;

			case CALCULATING:
				totFiles = Walker.this.fileList.size();
				totSize = totalSize.get();
				doneFilesOk = Walker.this.filesOk.get();
				doneFilesKo = Walker.this.filesKo.get();
				percentageOn10k = (Walker.this.totalSize.get() == 0) ? 0
						: (int) (Walker.this.sizeProcessed.get() * 10000
								/ Walker.this.totalSize.get());
				long now = System.currentTimeMillis();
				bytesPerSecond = ((now - startOfComputation) / 1000) == 0 ? -1
						: Walker.this.sizeProcessed.get() / ((now - startOfComputation) / 1000);
				secondsRemaining = (bytesPerSecond == 0) ? -1
						: (int) ((Walker.this.totalSize.get() - Walker.this.sizeProcessed.get())
								/ bytesPerSecond);
				break;

			case FINISHED:
				totFiles = Walker.this.fileList.size();
				totSize = totalSize.get();
				doneFilesOk = Walker.this.filesOk.get();
				doneFilesKo = Walker.this.filesKo.get();
				percentageOn10k = 10000;
				bytesPerSecond = ((endOfComputation - startOfComputation) / 1000) == 0 ? -1
						: Walker.this.sizeProcessed.get()
								/ ((endOfComputation - startOfComputation) / 1000);
				secondsRemaining = (int) ((endOfComputation - startOfComputation) / 1000);
				break;

			case NEW:
			default:
				totFiles = 0;
				totSize = 0;
				doneFilesOk = 0;
				doneFilesKo = 0;
				percentageOn10k = 0;
				bytesPerSecond = 0;
				secondsRemaining = 0;
			}
		}
	}

	private final String[] files;
	private final String[] checksums;

	private final Semaphore semaphore = new Semaphore(0);

	private final AtomicLong totalSize = new AtomicLong();

	private final AtomicLong sizeProcessed = new AtomicLong();
	private final AtomicInteger filesOk = new AtomicInteger();
	private final AtomicInteger filesKo = new AtomicInteger();

	private final FileList fileList = new FileList();

	private long startOfComputation, endOfComputation;

	public Runnable onBuilding;
	public Consumer<Status> onCalculating, onFinished;
	public Consumer<Exception> onError;

	private volatile State state;

	public static Walker forFiles(String... files) {
		return new Walker(files, null);
	}

	public static Walker forChecksums(String... checksums) {
		return new Walker(null, checksums);
	}

	public static Walker forChecksums(List<String> checksums) {
		return new Walker(null, checksums.toArray(new String[checksums.size()]));
	}

	private Walker(String[] files, String[] checksums) {
		this.checksums = checksums;
		this.files = files;
		this.state = State.NEW;
	}

	public static List<String> areThereChecksumFiles(String[] files) {
		List<String> checksums = new ArrayList<>();
		for (String fileName : files) {
			Path f = Paths.get(fileName).toAbsolutePath();
			if (Files.isDirectory(f)) {
				try {
					Files.walkFileTree(f, new SimpleFileVisitor<Path>() {
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
								throws IOException {
							if (file.toString().endsWith("." + FileList.EXTENSION))
								checksums.add(file.toString());
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					continue;
				}
			} else {
				if (f.toString().endsWith("." + FileList.EXTENSION))
					checksums.add(f.toString());
			}
		}
		return checksums;
	}

	public void setOnBuilding(Runnable onBuilding) {
		this.onBuilding = onBuilding;
	}

	public void setOnCalculating(Consumer<Status> onCalculating) {
		this.onCalculating = onCalculating;
	}

	public void setOnFinished(Consumer<Status> onFinished) {
		this.onFinished = onFinished;
	}

	public void setOnError(Consumer<Exception> onError) {
		this.onError = onError;
	}

	public void work(int threads) {
		try {
			if (state != State.NEW)
				throw new IllegalStateException("This Walker is already used");

			state = State.BUILDING;
			if (onBuilding != null)
				onBuilding.run();

			if (checksums == null)
				buildTree();
			else
				try {
					loadChecksumTree();
				} catch (ChecksumVerificationFailedException cke) {
					String error = "One or more Mus files didn't pass integrity check:"
							+ MiscUtils.CRLF + MiscUtils.CRLF + cke.getMessage();
					if (onError != null)
						onError.accept(new Exception(error));
					if (fileList.size() == 0) {
						state = State.FINISHED;
						return;
					}
				}

			if (fileList.isEmpty())
				throw new IllegalArgumentException("No files to process!");

			state = State.CALCULATING;
			if (onCalculating != null)
				onCalculating.accept(getStatus());
			startOfComputation = System.currentTimeMillis();

			ExecutorService threadPool = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < fileList.size(); i++) {
				final int idx = i;
				threadPool.execute(() -> {
					boolean ok = fileList.calcChecksum(idx,
							(bytes) -> sizeProcessed.addAndGet(bytes));
					semaphore.release();
					(ok ? filesOk : filesKo).incrementAndGet();
				});
			}

			try {
				semaphore.acquire(fileList.size());
			} catch (InterruptedException e) {
			}

			threadPool.shutdown();

			endOfComputation = System.currentTimeMillis();
			state = State.FINISHED;
			if (onFinished != null)
				onFinished.accept(getStatus());
		} catch (Exception e) {
			if (onError != null)
				onError.accept(e);
		}
	}

	private void buildTree() throws IOException {
		for (String fileName : files) {
			Path f = Paths.get(fileName).toAbsolutePath();
			if (Files.isDirectory(f)) {
				fileList.addPath(f);
				Files.walkFileTree(f, new SimpleFileVisitor<Path>() {
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException {
						addFile(file);
						return FileVisitResult.CONTINUE;
					}
				});
			} else
				addFile(f);
		}
	}

	public void addFile(Path file) throws IOException {
		Info info = fileList.addPath(file);
		long size = Files.size(file);
		totalSize.addAndGet(size);
		info.size = size;
	}

	private void loadChecksumTree() throws IOException, ChecksumVerificationFailedException {
		List<String> errors = new ArrayList<>();
		for (String fn : checksums)
			try {
				loadChecksumTree(new File(fn));
			} catch (ChecksumVerificationFailedException e) {
				errors.add(e.getMessage());
			}
		if (!errors.isEmpty()) {
			// Composes an exception with all the checksum files that don't pass
			// checksum verification
			StringBuilder sb = new StringBuilder();
			for (String s : errors) {
				sb.append(s);
				sb.append(MiscUtils.CRLF);
			}
			throw new ChecksumVerificationFailedException(sb.toString());
		}
	}

	private void loadChecksumTree(File checksum)
			throws IOException, FileNotFoundException, ChecksumVerificationFailedException {
		//try to load checksum part (if it's there)
		byte[] contents;
		try (InputStream is = new FileInputStream(checksum)) {
			contents = new byte[(int) (checksum.length() - 42)];
			is.read(contents);
			byte[] candidate = new byte[42];
			is.read(candidate);
			String scandidate = new String(candidate);
			if (!scandidate.endsWith(FileList.LAST_LINE_SUFFIX)) {
				// general file checksum line may not be present. In this
				// case the file contents are recomposed, and checksum of the
				// Mus file is ignored
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream(
						(int) checksum.length())) {
					baos.write(contents);
					baos.write(candidate);
					contents = baos.toByteArray();
				}
			} else {
				String calc;
				try (InputStream cis = new ByteArrayInputStream(contents)) {
					calc = MiscUtils.computeMD5Checksum(cis, null);
				}
				if (!scandidate.startsWith(calc))
					throw new ChecksumVerificationFailedException(checksum.getAbsolutePath());
			}
		}

		try (Reader r = new StringReader(new String(contents, MiscUtils.UTF8));
				BufferedReader br = new BufferedReader(r)) {
			String line = null;
			while ((line = br.readLine()) != null) {
				// non-checksum lines are ignored
				if (MiscUtils.countChars(line, MiscUtils.TAB) != 2)
					continue;
				int pos = line.indexOf(MiscUtils.TAB);
				String cksum = line.substring(0, pos);
				int pos2 = line.indexOf(MiscUtils.TAB, pos + 1);
				long size = Long.parseLong(line.substring(pos + 1, pos2));
				String file = line.substring(pos2 + 1);
				Path p = Paths.get(checksum.getParent(), file);
				Info i = fileList.addPath(p);
				i.size = size;
				totalSize.addAndGet(size);
				if (cksum.length() == 32) {
					i.state = FileList.State.NULL;
					i.checksum = cksum;
				} else {
					i.state = FileList.State.ERR;
					i.error = cksum;
				}
			}
		}
	}

	public Status getStatus() {
		return new Status();
	}

	public FileList getFileList() {
		return fileList;
	}

}
