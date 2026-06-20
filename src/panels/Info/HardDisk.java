package panels.Info;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HardDisk extends BasePanel {
    private JComboBox<String> diskCombo;
    private JLabel tempValueLabel, tempStatusLabel;
    private JLabel powerOnHoursLabel, powerCycleLabel;
    private List<String> diskIds = new ArrayList<>();
    private boolean populatingCombo = false;

    public HardDisk() {
        super("Hard Disk Status Check", "Check the hard disk's temperature and usage statistics.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topPanel.setBackground(Color.WHITE);
        topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        diskCombo = new JComboBox<>();
        diskCombo.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        diskCombo.setPreferredSize(new Dimension(420, 34));
        diskCombo.addActionListener(e -> {
            if (!populatingCombo) {
                int idx = diskCombo.getSelectedIndex();
                if (idx >= 0)
                    loadDiskStats(idx);
            }
        });
        topPanel.add(diskCombo);

        contentPanel.add(topPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        contentPanel.add(makeCategoryHeader("Temperature"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel tempCard = makeCard();

        JPanel tempTopRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        tempTopRow.setBackground(new Color(248, 249, 252));

        tempValueLabel = new JLabel("—");
        tempValueLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 36));
        tempValueLabel.setForeground(new Color(50, 50, 50));

        tempStatusLabel = new JLabel("");
        tempStatusLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));

        tempTopRow.add(tempValueLabel);
        tempTopRow.add(tempStatusLabel);
        tempCard.add(tempTopRow);

        JPanel guidePanel = new JPanel();
        guidePanel.setLayout(new BoxLayout(guidePanel, BoxLayout.Y_AXIS));
        guidePanel.setBackground(new Color(248, 249, 252));
        guidePanel.setBorder(new EmptyBorder(0, 4, 6, 4));
        addGuideRow(guidePanel, "0 ~ 50°C", "Normal", new Color(40, 160, 80));
        addGuideRow(guidePanel, "51 ~ 60°C", "Slightly High", new Color(200, 130, 0));
        addGuideRow(guidePanel, "60°C and above", "Critical", new Color(200, 60, 60));
        tempCard.add(guidePanel);

        contentPanel.add(tempCard);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Usage Statistics"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel statsCard = makeCard();
        powerOnHoursLabel = addRow(statsCard, "Power-On Time");
        powerCycleLabel = addRow(statsCard, "Power Cycle Count");
        statsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, statsCard.getPreferredSize().height));
        contentPanel.add(statsCard);
        contentPanel.add(Box.createVerticalStrut(8));

        JLabel notice = new JLabel("※ Power-on time and power cycle count are SMART data, and may not be shown depending on the drive type (especially NVMe) "
                + "and permissions.");
        notice.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
        notice.setForeground(new Color(150, 150, 150));
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(notice);

        loadDiskList();
    }

    private void loadDiskList() {
        clearLog();
        log("▶ Loading disk list...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                String cmd = "Get-PhysicalDisk | ForEach-Object { $_.DeviceId + '|' + $_.FriendlyName + '|' + [math]::Round($_.Size / 1GB) }";
                List<String[]> result = new ArrayList<>();
                for (String line : CmdUtil.runLines(cmd)) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3)
                        result.add(parts);
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    List<String[]> disks = get();
                    populatingCombo = true;
                    diskIds.clear();
                    diskCombo.removeAllItems();
                    for (String[] parts : disks) {
                        diskIds.add(parts[0].trim());
                        diskCombo.addItem(parts[1].trim() + " (" + parts[2].trim() + "GB)");
                    }
                    populatingCombo = false;

                    if (diskCombo.getItemCount() > 0)
                        diskCombo.setSelectedIndex(0);

                    log("▶ List loaded");
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                }
            }
        }.execute();
    }

    private void loadDiskStats(int diskIndex) {
        if (diskIndex < 0 || diskIndex >= diskIds.size())
            return;
        String deviceId = diskIds.get(diskIndex);
        log("▶ Loading disk info...");

        new SwingWorker<String[], Void>() {
            boolean admin;

            @Override
            protected String[] doInBackground() {
                admin = CmdUtil.isAdmin();

                String tempCmd = "$d = Get-PhysicalDisk | Where-Object { $_.DeviceId -eq " + deviceId + " }; " +
                        "($d | Get-StorageReliabilityCounter).Temperature";
                String temp = CmdUtil.run(tempCmd).trim();

                String statCmd = "$d = Get-PhysicalDisk | Where-Object { $_.DeviceId -eq " + deviceId + " }; " +
                        "$rc = $d | Get-StorageReliabilityCounter; " +
                        "'HOURS=' + $rc.PowerOnHours; " +
                        "'CYCLE=' + $rc.StartStopCycleCount";
                String hours = "", cycle = "";
                for (String line : CmdUtil.run(statCmd).split("\n")) {
                    line = line.trim();
                    if (line.startsWith("HOURS="))
                        hours = line.substring(6).trim();
                    else if (line.startsWith("CYCLE="))
                        cycle = line.substring(6).trim();
                }
                return new String[]{temp, hours, cycle};
            }

            @Override
            protected void done() {
                try {
                    String[] v = get();
                    Integer temp = parseIntOrNull(v[0]);
                    Integer hours = parseIntOrNull(v[1]);
                    Integer cycle = parseIntOrNull(v[2]);

                    String fallback = admin ? "Unavailable" : "Administrator privileges required";

                    if (temp != null) {
                        tempValueLabel.setText(temp + " °C");
                        Color c;
                        String status;
                        if (temp <= 50) {
                            c = new Color(40, 160, 80);
                            status = "Normal";
                        } else if (temp <= 60) {
                            c = new Color(200, 130, 0);
                            status = "Slightly High";
                        } else {
                            c = new Color(200, 60, 60);
                            status = "Critical";
                        }
                        tempValueLabel.setForeground(c);
                        tempStatusLabel.setText(status);
                        tempStatusLabel.setForeground(c);
                    } else {
                        tempValueLabel.setText(fallback);
                        tempValueLabel.setForeground(new Color(150, 150, 150));
                        tempStatusLabel.setText("");
                    }

                    if (hours != null) {
                        int d = hours / 24, h = hours % 24;
                        powerOnHoursLabel.setText(d > 0 ? d + "d " + h + "h (" + hours + "h)" : hours + "h");
                    } else
                        powerOnHoursLabel.setText(fallback + " (NVMe driver limitation)");

                    powerCycleLabel.setText(cycle != null ? cycle + " times" : fallback + " (NVMe driver limitation)");

                    log("▶ Done");
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                }
            }
        }.execute();
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.trim().isEmpty())
            return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void addGuideRow(JPanel parent, String range, String desc, Color color) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBackground(new Color(248, 249, 252));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Malgun Gothic", Font.PLAIN, 10));
        dot.setForeground(color);

        JLabel rangeLabel = new JLabel(range);
        rangeLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        rangeLabel.setForeground(color);
        rangeLabel.setPreferredSize(new Dimension(100, 16));

        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        descLabel.setForeground(new Color(80, 80, 80));

        row.add(dot);
        row.add(rangeLabel);
        row.add(descLabel);

        parent.add(row);
    }

    private JLabel makeCategoryHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        label.setForeground(new Color(100, 100, 160));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private JPanel makeCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(248, 249, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225)),
                new EmptyBorder(4, 12, 4, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        return card;
    }

    private JLabel addRow(JPanel card, String key) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(248, 249, 252));
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        keyLabel.setForeground(new Color(100, 100, 110));
        keyLabel.setPreferredSize(new Dimension(140, 20));

        JLabel valueLabel = new JLabel("—");
        valueLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        valueLabel.setForeground(new Color(30, 30, 30));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        card.add(row);

        return valueLabel;
    }
}
