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
package eu.germanorizzo.proj.mus.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.IntConsumer;

public class MiscUtils {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final String CRLF = "\r\n";
    public static final char TAB = '\t';
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    private static final String FMT_FLOAT_FOR_SIZE = "%.1f %s";
    private static final float KB = 1 << 10;
    private static final float MB = 1 << 20;
    private static final float GB = 1 << 30;
    private static final int BUF_SIZE = 65536;

    private MiscUtils() {
    }

    public static void sleep(int howMuch) {
        try {
            Thread.sleep(howMuch);
        } catch (Exception ignored) {
        }
    }

    private static String bytes2MD5(byte[] bytes) {
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

    public static String formatSpeed(long bps) {
        if (bps < 0)
            return "--";
        if (bps < KB)
            return bps + " Bps";
        if (bps < MB)
            return String.format(FMT_FLOAT_FOR_SIZE, bps / KB, "KBps");
        if (bps < GB)
            return String.format(FMT_FLOAT_FOR_SIZE, bps / MB, "MBps");
        return String.format(FMT_FLOAT_FOR_SIZE, bps / GB, "GBps");
    }

    public static String formatSize(long bytes) {
        if (bytes < 0)
            return "--";
        if (bytes < KB)
            return bytes + " bytes";
        if (bytes < MB)
            return String.format(FMT_FLOAT_FOR_SIZE, bytes / KB, "Kb");
        if (bytes < GB)
            return String.format(FMT_FLOAT_FOR_SIZE, bytes / MB, "Mb");
        return String.format(FMT_FLOAT_FOR_SIZE, bytes / GB, "Gb");
    }

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
