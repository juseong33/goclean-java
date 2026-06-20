package panels.system;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TaskScheduler extends BasePanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JButton deleteBtn;
    private JLabel countLabel;
    private List<String[]> allTasks = new ArrayList<>();

    public TaskScheduler() {
        super("Task Scheduler", "Unnecessary scheduled tasks can cause ad popups and slow down your computer.");
    }

    @Override
    protected void initUI() {
        allTasks = new ArrayList<>();
        contentPanel.setLayout(new BorderLayout());

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topRow.setBackground(Color.WHITE);
        topRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        countLabel = new JLabel("Scheduled tasks: loading...");
        countLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        countLabel.setForeground(new Color(50, 110, 200));
        topRow.add(countLabel);

        String[] columns = {"", "Program", "Author", "Path"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 13));

        table.setPreferredScrollableViewportSize(new Dimension(0, 200));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(36);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(440);

        JScrollPane tableScroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(16);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.setBorder(new EmptyBorder(16, 0, 0, 0));

        deleteBtn = makeDangerButton("Delete");
        deleteBtn.setPreferredSize(new Dimension(80, 34));
        deleteBtn.addActionListener(e -> deleteSelected());
        btnRow.add(deleteBtn);

        contentPanel.add(topRow,      BorderLayout.NORTH);
        contentPanel.add(tableScroll, BorderLayout.CENTER);
        contentPanel.add(btnRow,      BorderLayout.SOUTH);

        loadTasks();
    }

    private void loadTasks() {
        tableModel.setRowCount(0);
        allTasks.clear();
        countLabel.setText("Scheduled tasks: loading...");
        log("▶ Loading scheduled task list...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                String cmd = "Get-ScheduledTask | " +
                        "Where-Object { $_.TaskPath -eq '\\' } | " +
                        "Sort-Object TaskName | " +
                        "ForEach-Object { " +
                        "$a = if ($_.Author) { ($_.Author -replace '[|\\r\\n]',' ').Trim() } else { 'Unknown' }; " +
                        "$p = $env:SystemRoot + '\\System32\\Tasks\\' + $_.TaskName; " +
                        "$_.TaskName + '|' + $a + '|' + $p" +
                        "}";

                List<String[]> tasks = new ArrayList<>();
                for (String line : CmdUtil.runLines(cmd)) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 2) {
                        tasks.add(new String[]{
                                parts[0].trim(),
                                parts[1].trim(),
                                parts.length > 2 ? parts[2].trim() : ""
                        });
                    }
                }
                return tasks;
            }

            @Override
            protected void done() {
                try {
                    allTasks = get();
                    for (String[] task : allTasks)
                        tableModel.addRow(new Object[]{false, task[0], task[1], task[2]});
                    countLabel.setText("Scheduled tasks: " + allTasks.size());
                    log("▶ Loaded " + allTasks.size() + " scheduled tasks");
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                }
            }
        }.execute();
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
                    "Administrator privileges are required to delete scheduled tasks.\nPlease run the program as administrator.",
                    "Insufficient Privileges", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Do you want to delete the selected " + rows.size() + " scheduled task(s)?",
                "Delete Scheduled Tasks", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.NO_OPTION)
            return;

        List<String[]> toDelete = new ArrayList<>();
        for (int row : rows)
            toDelete.add(allTasks.get(row));

        deleteBtn.setEnabled(false);
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                int count = 0;
                for (String[] task : toDelete) {
                    String name = task[0].replace("'", "''");
                    CmdUtil.run("Unregister-ScheduledTask -TaskName '" + name + "' -TaskPath '\\' -Confirm:$false");
                    log("▶ Deleted: " + task[0]);
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
                    loadTasks();
                }
            }
        }.execute();
    }
}
