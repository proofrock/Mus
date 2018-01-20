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

public class Mus {
    private static final String VERSION = "0.3.1";
    public static final String HEADER_STRING = "Mus " + VERSION;
    private static final int CLI_REFRESH_TIMEOUT = 500;

    private static void doHeadless(String... args) {
        boolean doAutoFileName = args[0].equals("-a");

        String[] files;
        final File checksumFileName;
        if (doAutoFileName) {
            files = Arrays.copyOfRange(args, 1, args.length);
            checksumFileName = null;
        } else {
            files = Arrays.copyOfRange(args, 0, args.length - 1);
            checksumFileName = new File(args[args.length - 1]);
        }

        System.out.println(HEADER_STRING);
        System.out.println();

        final Walker walker = Walker.forFiles(files);

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
        System.err.println("Commandline usage: java -jar Mus.jar [-a] <files...> [checksum file]");
        System.err.println();
        System.err.println("Options:");
        System.err.println("      -a: determine automatically checksum file name");
        System.err.println();
        System.exit(-1);
    }
}
