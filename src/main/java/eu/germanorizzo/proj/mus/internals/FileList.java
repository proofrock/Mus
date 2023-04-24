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
package eu.germanorizzo.proj.mus.internals;

import eu.germanorizzo.proj.mus.Mus;
import eu.germanorizzo.proj.mus.utils.MiscUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

public class FileList {
    public static final String LAST_LINE_SUFFIX = "\tThis file";
    public static final String EXTENSION = "mu5";
    private static final String FIRST_LINE = "# File created with %s <https://github.com/proofrock/Mus>";
    private final List<Info> list = Collections.synchronizedList(new ArrayList<>());
    private Path commonAncestor;

    public Info addPath(Path path) {
        if (commonAncestor == null)
            commonAncestor = path.getParent();
        else {
            while (!path.startsWith(commonAncestor))
                commonAncestor = commonAncestor.getParent();
        }
        if (Files.isDirectory(path))
            return null;

        Info info = new Info(path);
        list.add(info);
        Collections.sort(list);
        return info;
    }

    public int size() {
        return list.size();
    }

    private String getRelativePath(int index) {
        return commonAncestor.relativize(list.get(index).path).toString();
    }

    //public void clear() {
    //    list.clear();
    //    commonAncestor = null;
    //}

    public Info getFileInfo(int index) {
        return list.get(index);
    }

    public Path getCommonAncestor() {
        return commonAncestor;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public String getChecksumFileNamePreset() {
        if (isEmpty()) {// Shouldn't happen
            if (commonAncestor == null)
                return "Checksum." + EXTENSION;
            return commonAncestor.getFileName().toString() + '.' + EXTENSION;
        }
        Path firstSubNode = commonAncestor.relativize(list.get(0).path).getName(0);
        for (int i = 1; i < list.size(); i++) {
            Path oFirstSubNode = commonAncestor.relativize(list.get(i).path).getName(0);
            if (!oFirstSubNode.startsWith(firstSubNode))
                return commonAncestor.getFileName().toString() + '.' + EXTENSION;
        }
        return firstSubNode.toString() + '.' + EXTENSION;
    }

    public void writeToFile(OutputStream os) throws IOException {
        /* File format:
         * # disclaimer line
         * Checksum<TAB>File path
         * Mus file checksum<TAB>Fixed string ("This file")
         */
        StringBuilder sb = new StringBuilder(String.format(FIRST_LINE, Mus.HEADER_STRING));
        sb.append(MiscUtils.CRLF);
        for (int i = 0; i < list.size(); i++) {
            Info info = getFileInfo(i);
            if (info.state == State.OK)
                sb.append(info.checksum);
            else
                sb.append(info.error);
            sb.append(MiscUtils.TAB);
            sb.append(info.size);
            sb.append(MiscUtils.TAB);
            String fn = getRelativePath(i);
            fn = fn.replace('\\', '/');
            sb.append(fn);
            sb.append(MiscUtils.CRLF);
        }
        byte[] toWrite = sb.toString().getBytes(MiscUtils.UTF8);
        String checksum;
        try (InputStream is = new ByteArrayInputStream(toWrite)) {
            checksum = MiscUtils.computeChecksum(Mus.FORMAT, is, null);
        }
        os.write(toWrite);
        os.write(checksum.getBytes(MiscUtils.UTF8));
        os.write(LAST_LINE_SUFFIX.getBytes(MiscUtils.UTF8));
    }

    public CheckStatus calcChecksum(int format, int idx, IntConsumer onAdvancement) {
        Info i = list.get(idx);

        if (i.state == FileList.State.ERR)
            return CheckStatus.KO;

        i.state = State.WORKING;
        i.error = null;

        if (!Files.exists(i.path)) {
            i.state = FileList.State.ERR;
            i.error = "File doesn't exist anymore";
            return CheckStatus.MISSING;
        }

        boolean isVerification = i.checksum != null;

        try {
            if (isVerification && (Files.size(i.path) != i.size)) {
                i.state = State.ERR;
                i.error = "File size mismatch";
                return CheckStatus.KO;
            }

            String checksum;
            try (InputStream is = Files.newInputStream(i.path)) {
                checksum = MiscUtils.computeChecksum(format, is, onAdvancement);
            }

            if (!isVerification)
                i.checksum = checksum;
            else if (!checksum.equals(i.checksum)) {
                i.state = State.ERR;
                i.error = "Corrupted file! (" + checksum + " instead of " + i.checksum + ")";
                return CheckStatus.KO;
            }

            i.state = State.OK;

        } catch (Exception e) {
            i.error = "ERROR [" + e.getClass().getName() + "]: " + e.getMessage();
            i.state = State.ERR;
            return CheckStatus.KO;
        }

        return CheckStatus.OK;
    }

    public enum CheckStatus {OK, MISSING, KO}

    public enum State {
        NULL, WORKING, OK, ERR
    }

    public static class Info implements Comparable<Info> {
        public final Path path;
        public volatile State state = State.NULL;
        public volatile String checksum, error;
        public volatile long size = -1;

        Info(Path path) {
            this.path = path;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Info))
                return false;
            Info i = (Info) obj;
            return Objects.equals(state, i.state) && Objects.equals(checksum, i.checksum)
                    && Objects.equals(error, i.error) && Objects.equals(path, i.path);
        }

        public int compareTo(Info o) {
            return path.compareTo(o.path);
        }
    }
}
