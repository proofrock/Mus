package eu.germanorizzo.proj.mus.utils;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.util.function.*;

public class MiscUtils {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static final String CRLF = "\r\n";
	public static final char TAB = '\t';

	private MiscUtils() {
	}

	public static void sleep(int howMuch) {
		try {
			Thread.sleep(howMuch);
		} catch (Exception e) {
		}
	}

	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	public static String bytes2MD5(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(HEX_CHARS[(b >> 4) & 15]);
			sb.append(HEX_CHARS[b & 15]);
		}
		return sb.toString();
	}

	public static String formatTime(int seconds) {
		if (seconds < 0)
			return "--";
		if (seconds < 60)
			return seconds + " s";
		if (seconds < 3600)
			return (seconds / 60) + " m " + (seconds % 60) + " s";
		return (seconds / 3600) + " h " + ((seconds % 3600) / 60) + " m " + (seconds % 60) + " s";
	}

	private static final String FMT_FLOAT_FOR_SIZE = "%.1f";
	private static final float KB = 1 << 10;
	private static final float MB = 1 << 20;
	private static final float GB = 1 << 30;

	public static String formatSpeed(long bps) {
		if (bps < 0)
			return "--";
		if (bps < KB)
			return bps + " Bps";
		if (bps < MB)
			return String.format(FMT_FLOAT_FOR_SIZE, Float.valueOf(bps / KB)) + " KBps";
		if (bps < GB)
			return String.format(FMT_FLOAT_FOR_SIZE, Float.valueOf(bps / MB)) + " MBps";
		return String.format(FMT_FLOAT_FOR_SIZE, Float.valueOf(bps / GB)) + " GBps";
	}

	public static String formatSize(long bytes) {
		if (bytes < 0)
			return "--";
		if (bytes < KB)
			return bytes + " bytes";
		if (bytes < MB)
			return String.format(FMT_FLOAT_FOR_SIZE, Float.valueOf(bytes / KB)) + " Kb";
		if (bytes < GB)
			return String.format(FMT_FLOAT_FOR_SIZE, Float.valueOf(bytes / MB)) + " Mb";
		return String.format(FMT_FLOAT_FOR_SIZE, Float.valueOf(bytes / GB)) + " Gb";
	}

	private static final int BUF_SIZE = 65536;

	public static String computeMD5Checksum(InputStream is, IntConsumer onAdvancement)
			throws IOException {
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");

			int read;
			byte[] buf = new byte[BUF_SIZE];
			while ((read = is.read(buf)) >= 0) {
				m.update(buf, 0, read);
				if (onAdvancement != null)
					onAdvancement.accept(read);
			}

			return MiscUtils.bytes2MD5(m.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	public static int countChars(String str, char c) {
		int count = 0;
		for (char _c : str.toCharArray())
			if (_c == c)
				count++;
		return count;
	}
}
