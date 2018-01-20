package eu.germanorizzo.proj.mus.utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GUIUtils {
    private GUIUtils() {
    }

    public static void reportException(Component parent, Exception e) {
        e.printStackTrace();

        String message = e.getMessage();
        try {
            int start = message.lastIndexOf(':');
            start = message.lastIndexOf('.', start - 1);
            message = message.substring(start + 1).trim();
        } catch (Exception e1) {
            message = e.getMessage();
        }
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void reportError(Component parent, String error) {
        JOptionPane.showMessageDialog(parent, error, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void reportWarning(Component parent, String warning) {
        JOptionPane.showMessageDialog(parent, warning, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void reportInfo(Component parent, String info) {
        JOptionPane.showMessageDialog(parent, info, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean askConfirmation(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static void useNativeLF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
    }

    public static void fullScreen(JFrame frame) {
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }

    public static String askText(Window parent, String message) {
        return askText(parent, message, "");
    }

    public static String askText(Window parent, String message, String preset) {
        return (String) JOptionPane.showInputDialog(parent, message, "Input request",
                JOptionPane.QUESTION_MESSAGE, null, null, preset);
    }

    public static File selFileForLoad(Window parent, String title) {
        return selFileForLoad(parent, title, null);
    }

    public static File selFileForSave(Window parent, String title) {
        return selFileForSave(parent, title, null);
    }

    public static File selFileForLoad(Window parent, String title, String fileName) {
        return selFile(parent, title, fileName, FileDialog.LOAD);
    }

    public static File selFileForSave(Window parent, String title, String fileName) {
        return selFile(parent, title, fileName, FileDialog.SAVE);
    }

    private static File selFile(Window parent, String title, String fileName, int operation) {
        FileDialog fd;
        if (parent instanceof Dialog)
            fd = new FileDialog((Dialog) parent, title, operation);
        else if (parent instanceof Frame)
            fd = new FileDialog((Frame) parent, title, operation);
        else
            throw new UnsupportedOperationException(
                    "The parent must be either a frame or a dialog");

        if (fileName != null)
            fd.setFile(fileName);

        fd.setVisible(true);
        if (fd.getFile() == null)
            return null;
        return new File(fd.getDirectory(), fd.getFile());
    }

    public static File selDirectory(Window parent) {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (jfc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
            return null;
        return jfc.getSelectedFile();
    }

    public static void bringToFront(Window w) {
        boolean isAOT = w.isAlwaysOnTop();
        w.setAlwaysOnTop(true);
        w.setAlwaysOnTop(isAOT);
    }

    public static void updUI(JComponent... components) {
        for (JComponent component : components)
            SwingUtilities.invokeLater(component::updateUI);
    }

    public static JViewport scrollTableTo(JTable table, int line) {
        Rectangle rect = table.getCellRect(line, 0, true);
        JViewport vp = (JViewport) table.getParent();
        int y = Math.max(rect.y + rect.height - vp.getHeight(), 0);
        vp.setViewPosition(new Point(0, y));
        return vp;
    }
}
