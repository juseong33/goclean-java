package panels.system;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartUpMng extends BasePanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JButton deleteBtn;
    private List<String[]> allStartups = new ArrayList<>();

    public StartUpMng() {
        super("Startup Manager", "Manage programs that run when the computer boots.");
    }

    @Override
    protected void initUI() {
        allStartups = new ArrayList<>();
        contentPanel.setLayout(new BorderLayout(0, 0));

        String[] columns = {"", "Program", "Open", "Path"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 2;
            }
        };

        table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(0, 300));
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 13));

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(36);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(500);

        table.getColumnModel().getColumn(2).setCellRenderer(new OpenButtonRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new OpenButtonEditor());

        JScrollPane tableScroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(16);

        contentPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.setBorder(new EmptyBorder(16, 0, 0, 0));

        deleteBtn = makeDangerButton("Delete");
        deleteBtn.setPreferredSize(new Dimension(80, 34));
        deleteBtn.addActionListener(e -> deleteSelected());
        btnRow.add(deleteBtn);

        contentPanel.add(btnRow, BorderLayout.SOUTH);

        loadStartups();
    }

    private void loadStartups() {
        tableModel.setRowCount(0);
        allStartups.clear();
        log("▶ Loading startup program list...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                String cmd = "Get-CimInstance Win32_StartupCommand | Sort-Object Name | " +
                        "ForEach-Object { $_.Name + '|' + $_.Command + '|' + $_.Location }";

                List<String[]> startups = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (String line : CmdUtil.runLines(cmd)) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        if (seen.add(name)) {
                            startups.add(new String[]{
                                    name,
                                    parts[1].trim(),
                                    parts.length > 2 ? parts[2].trim() : ""
                            });
                        }
                    }
                }
                return startups;
            }

            @Override
            protected void done() {
                try {
                    allStartups = get();
                    for (String[] item : allStartups) {
                        tableModel.addRow(new Object[]{false, item[0], "Open", item[1]});
                    }
                    log("▶ Loaded " + allStartups.size() + " startup programs");
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                }
            }
        }.execute();
    }

    private static String toRegistryPath(String location) {
        String loc = location.trim();
        String upper = loc.toUpperCase();
        if (upper.startsWith("HKLM\\"))
            return "HKLM:\\" + loc.substring(5);
        if (upper.startsWith("HKCU\\"))
            return "HKCU:\\" + loc.substring(5);
        if (upper.startsWith("HKU\\"))
            return "Registry::HKEY_USERS\\" + loc.substring(4);
        return null;
    }

    private static String extractExePath(String command) {
        String path = command.trim();
        if (path.startsWith("\"")) {
            int end = path.indexOf('"', 1);
            path = end > 0 ? path.substring(1, end) : path.substring(1);
        } else {
            int spaceIdx = path.indexOf(' ');
            if (spaceIdx > 0) path = path.substring(0, spaceIdx);
        }
        return expandEnvVars(path);
    }

    private static String expandEnvVars(String path) {
        Pattern p = Pattern.compile("%(\\w+)%", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String val = System.getenv(m.group(1));
            m.appendReplacement(sb, val != null ? Matcher.quoteReplacement(val) : m.group(0));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void deleteSelected() {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++)
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0)))
                rows.add(i);

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select items to delete.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!CmdUtil.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Administrator privileges are required to delete startup programs.\nPlease run the program as administrator.",
                    "Insufficient Privileges", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Do you want to delete the selected " + rows.size() + " startup program(s)?",
                "Delete Startup Programs", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.NO_OPTION)
            return;

        List<String[]> toDelete = new ArrayList<>();
        for (int row : rows)
            toDelete.add(allStartups.get(row));

        deleteBtn.setEnabled(false);
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                int count = 0;
                for (String[] item : toDelete) {
                    String name = item[0].replace("'", "''");
                    String regPath = toRegistryPath(item[2]);
                    if (regPath == null) {
                        log("[Notice] '" + item[0] + "' is an item located outside the registry.");
                        continue;
                    }
                    CmdUtil.run("Remove-ItemProperty -Path '" + regPath + "' -Name '" + name + "' -ErrorAction SilentlyContinue");
                    log("▶ Deleted: " + item[0]);
                    count++;
                }
                return count;
            }

            @Override
            protected void done() {
                try {
                    log("▶ Deleted " + get() + " item(s)");
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                } finally {
                    deleteBtn.setEnabled(true);
                    loadStartups();
                }
            }
        }.execute();
    }

    private static class OpenButtonRenderer extends JButton implements TableCellRenderer {
        OpenButtonRenderer() {
            setText("Open");
            setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private static class OpenButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String exePath;

        OpenButtonEditor() {
            super(new JCheckBox());
            button = new JButton("Open");
            button.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                fireEditingStopped();
                if (exePath != null)
                    CmdUtil.run("explorer.exe /select,\"" + exePath + "\"");
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            String command = (String) table.getValueAt(row, 3);
            exePath = extractExePath(command);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Open";
        }
    }
}
