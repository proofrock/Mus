/*
    This file is part of Mus

    Mus is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Mus is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Kryonist.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.germanorizzo.proj.mus;

import eu.germanorizzo.proj.mus.gfx.MainWindow;
import eu.germanorizzo.proj.mus.internals.FileList;
import eu.germanorizzo.proj.mus.internals.Walker;
import eu.germanorizzo.proj.mus.internals.Walker.Status;
import eu.germanorizzo.proj.mus.utils.GUIUtils;
import eu.germanorizzo.proj.mus.utils.MiscUtils;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class Mus {
    private static final String VERSION = "2.0.0";
    public static final int FORMAT = 2;
    public static final String HEADER_STRING = "Mus " + VERSION;
    private static final int CLI_REFRESH_TIMEOUT = 500;

    public static final String[] ALGO_BY_FORMAT = new String[]{"", "MD5", "SHA3-256"};
    public static final int[] ALGO_LEN_BY_FORMAT = new int[]{-1, 32, 64};

    private static void doHeadless(String... args) {
        System.out.println(HEADER_STRING);
        System.out.println();

        boolean doAutoFileName = args[0].equals("-a");
        boolean doVerify = args[0].equals("-v");

        if (doVerify) {
            doHeadlessVerification(Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        String[] files;
        final File checksumFileName;
        if (doAutoFileName) {
            files = Arrays.copyOfRange(args, 1, args.length);
            checksumFileName = null;
        } else {
            files = Arrays.copyOfRange(args, 0, args.length - 1);
            checksumFileName = new File(args[args.length - 1]);
        }

        final Walker walker = Walker.forFiles(FORMAT, files);

        walker.setOnBuilding(() -> System.out.print("Building file tree... "));

        final Thread updater = new Thread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                MiscUtils.sleep(CLI_REFRESH_TIMEOUT);
                outLine(walker.getStatus());
            }
        });
        updater.setDaemon(true);

        walker.setOnCalculating((s) -> {
            System.out.println("Ok.");
            System.out.println("Checksumming...");
            updater.start();
        });

        walker.setOnFinished((s) -> {
            System.out.print("Finished.  Speed: ");
            System.out.print(MiscUtils.formatSpeed(s.bytesPerSecond));
            System.out.print("  Time: ");
            System.out.print(MiscUtils.formatTime(s.secondsRemaining));
            System.out.println("                    ");

            File dest;
            if (doAutoFileName) {
                FileList fl = walker.getFileList();
                dest = new File(fl.getCommonAncestor().toFile(), fl.getChecksumFileNamePreset());
            } else
                dest = checksumFileName;

            System.out.print("Writing file " + dest + "... ");
            try (OutputStream os = new FileOutputStream(dest)) {
                walker.getFileList().writeToFile(os);
            } catch (Exception e) {
                handleException(e);
            }
            System.out.println("Ok.");
            System.out.println("All done.");
            System.exit(0);
        });

        walker.setOnError(Mus::handleException);

        new Thread(() -> walker.work(1)).start();
    }

    private static void doHeadlessVerification(String[] files) {
        List<String> checksums = Walker.areThereChecksumFiles(files);
        if (checksums.isEmpty())
            handleException(new Exception("No checksum files detected"));

        final Walker walker = Walker.forChecksums(checksums);

        walker.setOnBuilding(() -> System.out.print("Building file tree... "));

        final Thread updater = new Thread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                MiscUtils.sleep(CLI_REFRESH_TIMEOUT);
                outLine(walker.getStatus());
            }
        });
        updater.setDaemon(true);

        walker.setOnCalculating((s) -> {
            System.out.println("Ok.");
            System.out.println("Checksumming...");
            updater.start();
        });

        walker.setOnFinished((s) -> {
            System.out.print("Finished.  Speed: ");
            System.out.print(MiscUtils.formatSpeed(s.bytesPerSecond));
            System.out.print("  Time: ");
            System.out.print(MiscUtils.formatTime(s.secondsRemaining));
            System.out.println("                    ");

            boolean ok = true;
            if (s.filesKo != null && !s.filesKo.isEmpty()) {
                ok = false;
                System.out.println();
                System.out.println(s.filesKo.size() + " files corrupted:");
                for (String f : s.filesKo)
                    System.out.println(f);
            }
            if (s.filesMissing != null && !s.filesMissing.isEmpty()) {
                ok = false;
                System.out.println();
                System.out.println(s.filesMissing.size() + " files missing:");
                for (String f : s.filesMissing)
                    System.out.println(f);
            }

            System.out.println();
            System.out.println(ok ? "Ok." : "Some errors occurred");
            System.out.println("All done.");
            System.exit(0);
        });

        walker.setOnError(Mus::handleException);

        new Thread(() -> walker.work(1)).start();
    }

    private static void handleException(Exception e) {
        System.out.println();
        System.out.println("Error: " + e.getMessage());
        System.exit(-1);
    }

    private static void outLine(Status s) {
        System.out.print((s.doneFilesOk + s.doneFilesKo) + " of " + s.totFiles);
        System.out.print("  " + (s.percentageOn10k / 100) + "% done  Speed: ");
        System.out.print(MiscUtils.formatSpeed(s.bytesPerSecond));
        System.out.print("  ETA: " + MiscUtils.formatTime(s.secondsRemaining));
        System.out.print("                    \r");
    }

    public static void main(String[] args) {
        if (args.length > 0)
            doHeadless(args);
        else
            doGUI();
    }

    private static void doGUI() {
        GUIUtils.useNativeLF();

        EventQueue.invokeLater(() -> {
            try {
                MainWindow frame = new MainWindow();
                frame.setVisible(true);
            } catch (HeadlessException e) {
                showUsageAndAbort();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });
    }

    private static void showUsageAndAbort() {
        System.out.println(HEADER_STRING);
        System.err.println();
        System.err.println("Commandline usage: java -jar Mus.jar [-v] [-a] <files...> [checksum file]");
        System.err.println();
        System.err.println("Options:");
        System.err.println("      -v: verify one or more checksum file(s)");
        System.err.println("      -a: determine automatically checksum file name");
        System.err.println();
        System.exit(-1);
    }
}
