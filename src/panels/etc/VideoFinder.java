package panels.etc;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class VideoFinder extends BasePanel {
    private JComboBox<String> fileTypeCombo;
    private JComboBox<String> driveCombo;
    private JButton searchBtn;
    private JTable table;
    private DefaultTableModel tableModel;

    private Timer searchingTimer;
    private int searchingDots;

    public VideoFinder() {
        super("File Finder", "Find files of various extensions on your computer.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        JPanel topPanel = new JPanel(new BorderLayout(16, 0));
        topPanel.setBackground(Color.WHITE);

        JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        optionRow.setBackground(Color.WHITE);

        JLabel fileLabel = new JLabel("File Type:");
        fileLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));

        fileTypeCombo = new JComboBox<>(new String[]{
                "Video Files", "Audio Files (2MB+)", "Large Files (50MB+)",
                "Excel Files", "PowerPoint", "MS Word", "Hangul (Hwp)",
                "PDF Files", "PSD Files", "Zip Files"
        });
        fileTypeCombo.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));

        JLabel driveLabel = new JLabel("Drive:");
        driveLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));

        List<String> driveItems = new ArrayList<>();
        driveItems.add("All Drives");
        for (File root : File.listRoots())
            driveItems.add(root.getPath());

        driveCombo = new JComboBox<>(driveItems.toArray(new String[0]));
        driveCombo.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));

        optionRow.add(fileLabel);
        optionRow.add(fileTypeCombo);
        optionRow.add(driveLabel);
        optionRow.add(driveCombo);

        searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        searchBtn.setForeground(new Color(200, 30, 30));
        searchBtn.setBackground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.setPreferredSize(new Dimension(120, 40));
        searchBtn.addActionListener(e -> search());

        topPanel.add(optionRow, BorderLayout.CENTER);
        topPanel.add(searchBtn, BorderLayout.EAST);

        String[] columns = {"Path", "File Name", "Open Folder", "Size", "Date Created"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        tableScroll.getVerticalScrollBar().setUnitIncrement(18);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(18);

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(tableScroll, BorderLayout.CENTER);

        JScrollPane outerScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, contentPanel);
        if (outerScroll != null) {
            outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            outerScroll.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int padding = 40;
                    int gap = 16;
                    int bottomMargin = 24;
                    int available = outerScroll.getHeight() - padding - gap - bottomMargin - topPanel.getPreferredSize().height;

                    if (available > 0) {
                        table.setPreferredScrollableViewportSize(new Dimension(0, available));
                        tableScroll.revalidate();
                    }
                }
            });
        }
    }

    private void search() {
        String fileType = (String) fileTypeCombo.getSelectedItem();
        String drive = (String) driveCombo.getSelectedItem();

        List<File> roots = new ArrayList<>();
        if ("All Drives".equals(drive))
            roots.addAll(Arrays.asList(File.listRoots()));
        else
            roots.add(new File(drive));

        tableModel.setRowCount(0);
        clearLog();
        searchBtn.setEnabled(false);

        String searchingMessage = "▶ Searching " + fileType;
        log(searchingMessage);

        searchingDots = 0;
        searchingTimer = new Timer(1000, e -> {
            searchingDots = (searchingDots % 3) + 1;
            clearLog();
            log(searchingMessage + ".".repeat(searchingDots));
        });
        searchingTimer.start();

        new SearchWorker(roots, fileType).execute();
    }

    private class SearchWorker extends SwingWorker<Void, Object[]> {
        private final List<File> roots;
        private final String fileType;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        SearchWorker(List<File> roots, String fileType) {
            this.roots = roots;
            this.fileType = fileType;
        }

        @Override
        protected Void doInBackground() {
            for (File root : roots) {
                if (isCancelled())
                    break;
                searchDir(root);
            }
            return null;
        }

        private void searchDir(File dir) {
            if (isCancelled())
                return;

            File[] files = dir.listFiles();
            if (files == null)
                return;

            for (File file : files) {
                if (isCancelled())
                    return;

                if (file.isDirectory())
                    searchDir(file);
                else if (matches(file))
                    publish(new Object[]{file.getParent(), file.getName(), "Open",
                            CmdUtil.formatSize(file.length()), dateFormat.format(new Date(file.lastModified()))});
            }
        }

        private boolean matches(File file) {
            String name = file.getName().toLowerCase();
            long size = file.length();

            switch (fileType) {
                case "Video Files":
                    return hasExt(name, "mp4", "avi", "mkv", "mov", "wmv", "flv");
                case "Audio Files (2MB+)":
                    return hasExt(name, "mp3", "wav", "flac", "aac", "ogg") && size >= 2L * 1024 * 1024;
                case "Large Files (50MB+)":
                    return size >= 50L * 1024 * 1024;
                case "Excel Files":
                    return hasExt(name, "xls", "xlsx");
                case "PowerPoint":
                    return hasExt(name, "ppt", "pptx");
                case "MS Word":
                    return hasExt(name, "doc", "docx");
                case "Hangul (Hwp)":
                    return hasExt(name, "hwp");
                case "PDF Files":
                    return hasExt(name, "pdf");
                case "PSD Files":
                    return hasExt(name, "psd");
                case "Zip Files":
                    return hasExt(name, "zip", "rar", "7z");
                default:
                    return false;
            }
        }

        private boolean hasExt(String name, String... exts) {
            for (String ext : exts)
                if (name.endsWith("." + ext))
                    return true;
            return false;
        }

        @Override
        protected void process(List<Object[]> chunks) {
            for (Object[] row : chunks)
                tableModel.addRow(row);
        }

        @Override
        protected void done() {
            searchingTimer.stop();
            clearLog();
            log("▶ Search complete (" + tableModel.getRowCount() + " found)");
            searchBtn.setEnabled(true);
        }
    }

    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setText("Open");
            setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private static class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String filePath;

        ButtonEditor() {
            super(new JCheckBox());
            button = new JButton("Open");
            button.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                fireEditingStopped();
                if (filePath != null)
                    CmdUtil.run("explorer.exe /select,\"" + filePath + "\"");
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            String folder = (String) table.getValueAt(row, 0);
            String name = (String) table.getValueAt(row, 1);
            filePath = folder + File.separator + name;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Open";
        }
    }
}
