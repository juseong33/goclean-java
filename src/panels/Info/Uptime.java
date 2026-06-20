package panels.Info;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.io.*;

public class Uptime extends BasePanel {
    private JLabel todayLabel, yesterdayLabel, dayBeforeLabel;
    private JTable table;
    private DefaultTableModel tableModel;

    public Uptime() {
        super("Uptime", "Checks the computer's power-on and power-off times over the past month.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);

        JButton refreshBtn = makeButton("Load Info");
        refreshBtn.addActionListener(e -> loadInfo());

        JButton saveBtn = makeButton("Save Selected");
        saveBtn.addActionListener(e -> saveSelected());

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnPanel.add(refreshBtn, BorderLayout.WEST);
        btnPanel.add(saveBtn, BorderLayout.EAST);

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        todayLabel = new JLabel("—");
        yesterdayLabel = new JLabel("—");
        dayBeforeLabel = new JLabel("—");

        summaryPanel.add(new JLabel("■ Today total usage :"));
        summaryPanel.add(todayLabel);
        summaryPanel.add(new JLabel("■ Yesterday :"));
        summaryPanel.add(yesterdayLabel);
        summaryPanel.add(new JLabel("■ Day before :"));
        summaryPanel.add(dayBeforeLabel);

        topPanel.add(btnPanel);
        topPanel.add(summaryPanel);

        String[] columns = {"", "Date", "Time", "Event"};
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
        table.setPreferredScrollableViewportSize(new Dimension(0, 250));
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                if ("Power On".equals(value))
                    setForeground(new Color(50, 130, 220));
                else if ("Power Off".equals(value))
                    setForeground(new Color(200, 60, 60));

                return this;
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(tableScroll, BorderLayout.CENTER);
    }

    private void loadInfo() {
        tableModel.setRowCount(0);
        clearLog();
        log("▶ Loading usage time...");

        new SwingWorker<Void, Void>() {
            Map<String, Long> dailyUsage;
            String today, yesterday, dayBefore;

            @Override
            protected Void doInBackground() {
                String getUptimeInfo = "$start = (Get-Date).AddDays(-30); Get-WinEvent -LogName System " +
                        "| Where-Object { ($_.Id -eq 6005 -or $_.Id -eq 6006) -and $_.TimeCreated -ge $start } " +
                        "| Select-Object TimeCreated, Id | Sort-Object TimeCreated -Descending " +
                        "| ForEach-Object { $_.TimeCreated.ToString('yyyy-MM-dd HH:mm:ss') + ' ' + $_.Id }";

                List<String> lines = CmdUtil.runLines(getUptimeInfo);

                dailyUsage = new LinkedHashMap<>();
                String lastOffDate = null;
                String lastOffTime = null;

                today = getDateBefore(0);
                yesterday = getDateBefore(1);
                dayBefore = getDateBefore(2);

                for (String line : lines) {
                    String[] parts = line.trim().split(" ");
                    String date = parts[0];
                    String time = parts[1];
                    String eventId = parts[2];

                    if (eventId.equals("6006")) {
                        lastOffDate = date;
                        lastOffTime = date + " " + time;
                        tableModel.addRow(new Object[]{false, date, time, "Power Off"});
                    }
                    else if (eventId.equals("6005")) {
                        tableModel.addRow(new Object[]{false, date, time, "Power On"});
                        if (lastOffDate != null) {
                            try {
                                SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                                long on = sdfFull.parse(date + " " + time).getTime();
                                long off = sdfFull.parse(lastOffTime).getTime();

                                if (date.equals(lastOffDate)) {
                                    dailyUsage.merge(date, off - on, Long::sum);
                                } else {
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTimeInMillis(on);
                                    cal.add(Calendar.DATE, 1);
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.set(Calendar.MILLISECOND, 0);
                                    long midnight = cal.getTimeInMillis();

                                    dailyUsage.merge(date, midnight - on, Long::sum);
                                    dailyUsage.merge(lastOffDate, off - midnight, Long::sum);
                                }
                            } catch (ParseException e) {
                                log(e.getMessage());
                            }
                            lastOffTime = null;
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                todayLabel.setText(formatUsageTime(dailyUsage.get(today)));
                yesterdayLabel.setText(formatUsageTime(dailyUsage.get(yesterday)));
                dayBeforeLabel.setText(formatUsageTime(dailyUsage.get(dayBefore)));

                log("▶ Done");
            }
        }.execute();
    }

    private String getDateBefore(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    private String formatUsageTime(long ms) {
        long hours = ms / (1000 * 60 * 60);
        long minutes = (ms % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%02dh %02dm", hours, minutes);
    }

    private void saveSelected() {
        BufferedWriter bw;
        List<Object[]> selected = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((boolean) tableModel.getValueAt(i, 0))
                selected.add(new Object[]{tableModel.getValueAt(i, 1), tableModel.getValueAt(i, 2), tableModel.getValueAt(i, 3)});
        }

        if (selected.isEmpty()) {
            log("▶ No items selected.");
            return;
        }

        File dir = new File("data/uptime");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                log("[Error] Failed to create folder");
                return;
            }
        }

        String fileName = "uptime_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt";
        File file = new File(dir, fileName);

        try {
            bw = new BufferedWriter(new FileWriter(file));

            bw.write("[Selected Items]\n");
            bw.write(String.format("%-12s  %-10s  %s%n", "Date", "Time", "Event"));
            bw.write("-------------------------------------------------\n");
            for (Object[] row : selected)
                bw.write(String.format("%-12s  %-10s  %s%n", row[0], row[1], row[2]));

            bw.flush();
            bw.close();
            log("▶ Saved");
        } catch (IOException e) {
            log(e.getMessage());
        }
    }
}
