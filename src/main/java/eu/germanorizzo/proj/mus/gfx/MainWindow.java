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
package eu.germanorizzo.proj.mus.gfx;

import eu.germanorizzo.proj.mus.Mus;
import eu.germanorizzo.proj.mus.internals.FileList;
import eu.germanorizzo.proj.mus.internals.FileList.Info;
import eu.germanorizzo.proj.mus.internals.FileList.State;
import eu.germanorizzo.proj.mus.internals.Walker;
import eu.germanorizzo.proj.mus.utils.GUIUtils;
import eu.germanorizzo.proj.mus.utils.MiscUtils;
import eu.germanorizzo.proj.mus.utils.TableContent;
import eu.germanorizzo.proj.mus.utils.TableContent.Column;
import eu.germanorizzo.proj.mus.utils.TableContent.Row;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {
    private static final int REFRESH_TIMEOUT = 1000 / 12; // 12 FPS

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() + 1;

    private static final Column[] COLUMNS_4_CALC = new Column[]{new Column(Icon.class, "", 22),
            new Column(String.class, "File name"), new Column(String.class, "Checksum", 300)};

    private static final ImageIcon ICON_OK = new ImageIcon(
            MainWindow.class.getResource("/icons/d16/Emoji Symbols-100-16.png"));
    private static final ImageIcon ICON_WORKING = new ImageIcon(
            MainWindow.class.getResource("/icons/d16/Emoji Orte-81-16.png"));
    private static final ImageIcon ICON_ERR = new ImageIcon(
            MainWindow.class.getResource("/icons/d16/Emoji Symbols-134-16.png"));

    private static final String SUFFIX_LBL_FILE_NUM = " Files";
    private static final String SUFFIX_LBL_FILE_OK = " Files Ok";
    private static final String SUFFIX_LBL_FILE_KO = " Errors";

    private final JPanel contentPane;
    private final JTextField tfDirectory;
    private final JTable tblFiles;
    private final JProgressBar pbProgress;
    private final JButton btOpen;
    private final JButton btSave;
    private final JButton btSaveAs;
    private final JButton btClear;
    private final JButton btInfo;
    private final JSpinner spnThreads;
    private final JLabel lblFileNumber;
    private final JLabel lblFileOk;
    private final JLabel lblFileKo;
    private final JLabel lblSize;

    private volatile FileList fileList;
    private File lastDir = null;

    /**
     * Create the frame.
     */
    public MainWindow() {
        setIconImage(Toolkit.getDefaultToolkit().getImage(MainWindow.class
                .getResource("/icons/d22/Emoji Natur-04-22.png")));
        setTitle(Mus.HEADER_STRING);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 720, 562);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        JLabel lblRootDir = new JLabel("Base folder:");

        tfDirectory = new JTextField();
        tfDirectory.setEditable(false);
        tfDirectory.setColumns(10);

        JScrollPane scrollPane = new JScrollPane();

        pbProgress = new JProgressBar();
        pbProgress.setStringPainted(true);

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        toolBar.setFloatable(false);

        spnThreads = new JSpinner();
        spnThreads.setToolTipText(
                "How many threads to use (specify 1 if dealing with slow media i.e. CD-ROM)");
        spnThreads.setModel(new SpinnerNumberModel(DEFAULT_THREADS, 1, 12, 1));
        spnThreads.setValue(Integer.valueOf(1));

        JLabel lblThreads = new JLabel("Threads:");

        JPanel panel = new JPanel();
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        GroupLayout gl_contentPane = new GroupLayout(contentPane);
        gl_contentPane
                .setHorizontalGroup(
                        gl_contentPane.createParallelGroup(Alignment.LEADING)
                                .addComponent(toolBar, GroupLayout.DEFAULT_SIZE, 694,
                                        Short.MAX_VALUE)
                                .addComponent(pbProgress, GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)
                                .addComponent(panel, GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)
                                .addGroup(Alignment.TRAILING,
                                        gl_contentPane.createSequentialGroup().addContainerGap()
                                                .addComponent(lblRootDir)
                                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                                .addComponent(tfDirectory, GroupLayout.DEFAULT_SIZE, 514,
                                                        Short.MAX_VALUE)
                                                .addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblThreads)
                                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                                .addComponent(spnThreads, GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addContainerGap())
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE));
        gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_contentPane.createSequentialGroup()
                        .addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                        .addGap(12)
                        .addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRootDir)
                                .addComponent(spnThreads, GroupLayout.PREFERRED_SIZE, 20,
                                        GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblThreads).addComponent(tfDirectory,
                                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 369, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel, GroupLayout.PREFERRED_SIZE, 39,
                                GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(pbProgress,
                                GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)));

        lblFileNumber = new JLabel("0 Files");
        lblFileNumber.setToolTipText("Total number of files");
        lblFileNumber.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Objects-127-22.png")));

        Component horizontalGlue_1 = Box.createHorizontalGlue();

        lblFileOk = new JLabel("0 Ok");
        lblFileOk.setToolTipText("Number of files for which the operation succeeded");
        lblFileOk.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Smiley-03-22.png")));

        Component horizontalGlue_2 = Box.createHorizontalGlue();

        lblFileKo = new JLabel("0 Errors");
        lblFileKo.setToolTipText("Number of files for which the operation did NOT succeed");
        lblFileKo.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Smiley-17-22.png")));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        Component horizontalStrut_1 = Box.createHorizontalStrut(20);
        panel.add(horizontalStrut_1);
        panel.add(lblFileNumber);
        panel.add(horizontalGlue_1);

        lblSize = new JLabel("0 Kb");
        lblSize.setToolTipText("Total size to checksum");
        lblSize.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Objects-98-22.png")));
        panel.add(lblSize);

        Component horizontalGlue_4 = Box.createHorizontalGlue();
        panel.add(horizontalGlue_4);
        panel.add(lblFileOk);
        panel.add(horizontalGlue_2);
        panel.add(lblFileKo);

        Component horizontalStrut_4 = Box.createHorizontalStrut(20);
        panel.add(horizontalStrut_4);

        btOpen = new JButton("Choose files...");
        btOpen.setToolTipText("Choose files/folders to checksum...");
        btOpen.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Objects-88-22.png")));
        toolBar.add(btOpen);

        Component horizontalStrut_2 = Box.createHorizontalStrut(20);
        toolBar.add(horizontalStrut_2);

        btSave = new JButton("Save");
        btSave.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Objects-87-22.png")));
        btSave.setToolTipText("Save checksum file");
        toolBar.add(btSave);

        Component horizontalStrut = Box.createHorizontalStrut(20);
        toolBar.add(horizontalStrut);

        btSaveAs = new JButton("Save as...");
        btSaveAs.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Objects-87-22.png")));
        btSaveAs.setToolTipText("Save checksum file as...");
        toolBar.add(btSaveAs);

        Component horizontalStrut_3 = Box.createHorizontalStrut(20);
        toolBar.add(horizontalStrut_3);

        btClear = new JButton("Clear");
        btClear.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Symbols-112-22.png")));
        btClear.setToolTipText("Reset the interface");
        toolBar.add(btClear);

        Component horizontalGlue = Box.createHorizontalGlue();
        toolBar.add(horizontalGlue);

        btInfo = new JButton("");
        btInfo.setIcon(new ImageIcon(MainWindow.class
                .getResource("/icons/d22/Emoji Natur-04-22.png")));
        btInfo.setToolTipText("Get info");
        toolBar.add(btInfo);

        tblFiles = new JTable();
        tblFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblFiles.setRowHeight(22);
        scrollPane.setViewportView(tblFiles);
        contentPane.setLayout(gl_contentPane);

        init();
    }

    private void init() {
        btInfo.addActionListener((ae) -> {
            GUIUtils.reportInfo(this, Mus.HEADER_STRING);
        });

        btOpen.addActionListener((ae) -> {
            File[] f = selFilesToChecksum();
            if (f == null)
                return;
            String[] names = new String[f.length];
            for (int i = 0; i < f.length; i++)
                names[i] = f[i].getAbsolutePath();
            work(names);
        });

        btSave.addActionListener((ae) -> {
            save(true);
        });

        btSaveAs.addActionListener((ae) -> {
            save(false);
        });

        btClear.addActionListener((ae) -> {
            reinit();
        });

        this.setDropTarget(new DropTarget(this, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    Transferable t = dtde.getTransferable();
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    if (!t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                        return;
                    @SuppressWarnings("unchecked")
                    List<File> list = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    String[] files = new String[list.size()];
                    for (int i = 0; i < files.length; i++)
                        files[i] = list.get(i).getAbsolutePath();
                    work(files);
                } catch (Exception e) {
                    GUIUtils.reportException(MainWindow.this, e);
                }
            }
        }));

        reinit();
    }

    public void save(boolean autoFileName) {
        File file = new File(fileList.getCommonAncestor().toFile(),
                fileList.getChecksumFileNamePreset());

        if (!autoFileName) {
            file = selFileToSave(file);
            if (file == null)
                return;
        }

        if (file.exists() && !GUIUtils.askConfirmation(this, "File already exists. Overwrite?"))
            return;

        try {
            try (OutputStream os = new FileOutputStream(file)) {
                fileList.writeToFile(os);
            }
        } catch (Exception e) {
            GUIUtils.reportException(this, e);
        }
        GUIUtils.reportInfo(this, "Saved checksum file to " + file.getAbsolutePath());
    }

    private void reinit() {
        fileList = null;

        tfDirectory.setText("");
        btSave.setEnabled(false);
        btSaveAs.setEnabled(false);
        btClear.setEnabled(false);
        btOpen.setEnabled(true);

        new TableContent(COLUMNS_4_CALC).apply(tblFiles); // "empty"

        pbProgress.setMaximum(0);
        pbProgress.setValue(0);
        pbProgress.setString("0/0");

        lblFileNumber.setText(0 + SUFFIX_LBL_FILE_NUM);
        lblFileOk.setText(0 + SUFFIX_LBL_FILE_OK);
        lblFileKo.setText(0 + SUFFIX_LBL_FILE_KO);
        lblSize.setText(MiscUtils.formatSize(0));

        GUIUtils.updUI(tfDirectory, btSave, btSaveAs, btClear, btOpen, tblFiles, pbProgress,
                lblFileNumber, lblFileOk, lblFileKo);
    }

    private void work(String... files) {
        reinit();

        final int threads = ((Integer) spnThreads.getValue()).intValue();

        List<String> checksums = Walker.areThereChecksumFiles(files);
        final Walker walker;
        if (checksums.isEmpty())
            walker = Walker.forFiles(Mus.FORMAT, files);
        else if (GUIUtils.askConfirmation(this, "Mus checksum files have been detected\n"
                + "Do you want to switch to Verification mode?"))
            walker = Walker.forChecksums(checksums);
        else
            walker = Walker.forFiles(Mus.FORMAT, files);

        fileList = walker.getFileList();

        TableContent tc = new TableContent(COLUMNS_4_CALC);
        tc.apply(tblFiles);

        btOpen.setEnabled(false);
        tfDirectory.setText("Working...");

        walker.setOnBuilding(() -> {
            pbProgress.setString("Building file tree...");

            GUIUtils.updUI(pbProgress);
        });

        final Thread updater = new Thread(() -> {
            while (true) {
                MiscUtils.sleep(REFRESH_TIMEOUT);

                Walker.Status s = walker.getStatus();
                if (s.state != Walker.State.CALCULATING)
                    return;

                pbProgress.setValue(s.percentageOn10k);
                pbProgress.setString("Files: " + (s.doneFilesOk + s.doneFilesKo) + "/" + s.totFiles
                        + "  Speed: " + MiscUtils.formatSpeed(s.bytesPerSecond) + "  ETA: "
                        + MiscUtils.formatTime(s.secondsRemaining));

                lblFileOk.setText(s.doneFilesOk + SUFFIX_LBL_FILE_OK);
                lblFileKo.setText(s.doneFilesKo + SUFFIX_LBL_FILE_KO);

                JComponent vp = GUIUtils.scrollTableTo(tblFiles,
                        +s.doneFilesOk + s.doneFilesKo + threads + 1);

                GUIUtils.updUI(tblFiles, pbProgress, lblFileOk, lblFileKo, vp);
            }
        });
        updater.setDaemon(true);

        walker.setOnCalculating((s) -> {
            genRows(tc);

            pbProgress.setMaximum(10000);
            pbProgress.setValue(0);
            pbProgress.setString("Files: 0/" + s.totFiles);
            lblFileNumber.setText(s.totFiles + SUFFIX_LBL_FILE_NUM);
            lblSize.setText(MiscUtils.formatSize(s.totSize));

            GUIUtils.updUI(tblFiles, pbProgress, lblFileNumber, lblSize);

            updater.start();
        });

        walker.setOnFinished((s) -> {
            btSave.setEnabled(true);
            btSaveAs.setEnabled(true);
            btClear.setEnabled(true);
            btOpen.setEnabled(true);
            tfDirectory.setText(fileList.getCommonAncestor().toString());

            pbProgress.setValue(10000);
            pbProgress.setString("Finished.  Speed: " + MiscUtils.formatSpeed(s.bytesPerSecond)
                    + "  Time: " + MiscUtils.formatTime(s.secondsRemaining));

            lblFileOk.setText(s.doneFilesOk + SUFFIX_LBL_FILE_OK);
            lblFileKo.setText(s.doneFilesKo + SUFFIX_LBL_FILE_KO);

            GUIUtils.updUI(tblFiles, pbProgress, lblFileOk, lblFileKo);
        });

        walker.setOnError((e) -> {
            GUIUtils.reportException(this, e);
            reinit();
        });

        new Thread(() -> walker.work(threads)).start();
    }

    public void genRows(TableContent tc) {
        for (int i = 0; i < fileList.size(); i++)
            tc.contents.add(info2row(fileList.getFileInfo(i)));
    }

    private Row info2row(final Info info) {
        return new TableContent.RowCalc((col) -> {
            switch (col) {
                case 0:
                    switch (info.state) {
                        case OK:
                            return ICON_OK;
                        case ERR:
                            return ICON_ERR;
                        case WORKING:
                            return ICON_WORKING;
                        default:
                            return null;
                    }
                case 1:
                    return info.path.toString();
                case 2:
                    switch (info.state) {
                        case OK:
                            return info.checksum;
                        case ERR:
                            return info.error;
                        case WORKING:
                            return info.checksum == null ? "Calculating..."
                                    : "Checking " + info.checksum + "...";
                        default:
                            return "";
                    }
            }
            return null;
        }, () -> {
            if (info.state == State.ERR)
                return Color.YELLOW;
            return Color.WHITE;
        });
    }

    private File[] selFilesToChecksum() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select stuff to checksum");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        if (lastDir != null)
            chooser.setCurrentDirectory(lastDir);
        int returnVal = chooser.showOpenDialog(this);
        try {
            if (returnVal == JFileChooser.APPROVE_OPTION)
                return chooser.getSelectedFiles();
            return null;
        } finally {
            lastDir = chooser.getCurrentDirectory();
        }
    }

    private File selFileToSave(File presetFile) {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Select file to save");
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);

        FileFilter ff = new FileNameExtensionFilter("Mus Checksum Files", FileList.EXTENSION);
        chooser.addChoosableFileFilter(ff);
        chooser.setFileFilter(ff);

        chooser.setCurrentDirectory(presetFile.getParentFile());
        chooser.setSelectedFile(presetFile);

        int returnVal = chooser.showSaveDialog(this);
        try {
            if (returnVal == JFileChooser.APPROVE_OPTION)
                return chooser.getSelectedFile();
            return null;
        } finally {
            lastDir = chooser.getCurrentDirectory();
        }
    }
}
