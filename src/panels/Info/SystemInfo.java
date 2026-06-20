package panels.Info;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class SystemInfo extends BasePanel {
    private JLabel cpuLabel, gpuLabel, soundLabel, installDateLabel;
    private JLabel totalMemLabel, freeMemLabel;
    private JLabel ipLabel, macLabel;
    private JList<String> driveList;
    private JLabel totalDiskLabel, usedDiskLabel, freeDiskLabel, percentLabel;
    private double usedRatio = 0.0;
    private JPanel progressBar;

    public SystemInfo() {
        super("System Info", "Check this PC's system, memory, network, and disk information.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JButton refreshBtn = makeButton("Load Info");
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(e -> loadInfo());
        contentPanel.add(refreshBtn);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("System"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel systemCard = makeCard();
        cpuLabel = addRow(systemCard, "CPU");
        gpuLabel = addRow(systemCard, "Graphics Card");
        soundLabel = addRow(systemCard, "Sound Card");
        installDateLabel = addRow(systemCard, "Windows Install Date");
        systemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, systemCard.getPreferredSize().height));
        contentPanel.add(systemCard);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Memory"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel memCard = makeCard();
        totalMemLabel = addRow(memCard, "Total Memory");
        freeMemLabel = addRow(memCard, "Available Memory");
        contentPanel.add(memCard);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Network"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel netCard = makeCard();
        ipLabel = addRow(netCard, "Private IP");
        macLabel = addRow(netCard, "MAC Address");
        contentPanel.add(netCard);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Disk"));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(makeDiskPanel());
        contentPanel.add(Box.createVerticalStrut(16));
    }

    private JPanel makeDiskPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(Color.WHITE);
        listPanel.setPreferredSize(new Dimension(150, 0));

        driveList = new JList<>(new DefaultListModel<>());
        driveList.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        driveList.setBackground(new Color(248, 249, 252));
        driveList.setSelectionBackground(new Color(80, 140, 255));
        driveList.setSelectionForeground(Color.WHITE);
        driveList.setBorder(new EmptyBorder(4, 8, 4, 8));
        driveList.setFixedCellHeight(32);
        driveList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDriveInfo(driveList.getSelectedValue());
        });

        JScrollPane scroll = new JScrollPane(driveList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        listPanel.add(scroll, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        JPanel infoCard = makeCard();
        totalDiskLabel = addRow(infoCard, "Total Capacity");
        usedDiskLabel  = addRow(infoCard, "Used");
        freeDiskLabel  = addRow(infoCard, "Free");
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int usedWidth = (int)(getWidth() * usedRatio);
                g2.setColor(new Color(80, 140, 255));
                g2.fillRoundRect(0, 0, usedWidth, getHeight(), 6, 6);
                g2.setColor(new Color(220, 220, 225));
                g2.fillRoundRect(usedWidth, 0, getWidth() - usedWidth, getHeight(), 6, 6);
            }
        };
        progressBar.setPreferredSize(new Dimension(0, 20));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        percentLabel = new JLabel("— %");
        percentLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        percentLabel.setForeground(new Color(80, 140, 255));
        percentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        percentLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        infoPanel.add(infoCard);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(progressBar);
        infoPanel.add(percentLabel);

        panel.add(listPanel, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
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

    private void updateDriveInfo(String drive) {
        String used = CmdUtil.run("(Get-PSDrive " + drive + ").Used");
        String free = CmdUtil.run("(Get-PSDrive " + drive + ").Free");

        long usedBytes = Long.parseLong(used.trim());
        long freeBytes = Long.parseLong(free.trim());
        long totalBytes = usedBytes + freeBytes;

        totalDiskLabel.setText(CmdUtil.formatSize(totalBytes));
        usedDiskLabel.setText(CmdUtil.formatSize(usedBytes));
        freeDiskLabel.setText(CmdUtil.formatSize(freeBytes));

        usedRatio = (double) usedBytes / totalBytes;
        percentLabel.setText(String.format("%.1f%% used", usedRatio * 100));
        progressBar.repaint();
    }

    private void loadInfo() {
        clearLog();
        log("▶ Loading system info...");
        long startTime = System.currentTimeMillis();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                String cpuCmd = "(Get-WmiObject Win32_Processor).Name";
                publish("▶ Loading CPU...  [" + cpuCmd + "]");
                cpuLabel.setText(CmdUtil.run(cpuCmd));

                String gpuCmd = "(Get-CimInstance Win32_VideoController).Name";
                publish("▶ Loading graphics card...  [" + gpuCmd + "]");
                gpuLabel.setText(CmdUtil.run(gpuCmd));

                String soundCmd = "(Get-WmiObject Win32_SoundDevice | Select-Object -First 1).Name";
                publish("▶ Loading sound card...  [" + soundCmd + "]");
                soundLabel.setText(CmdUtil.run(soundCmd));

                String installDateCmd = "(Get-WmiObject Win32_OperatingSystem).ConvertToDateTime((Get-WmiObject Win32_OperatingSystem).InstallDate).ToString('yyyy-MM-dd')";
                publish("▶ Loading Windows install date...  [" + installDateCmd + "]");
                installDateLabel.setText(CmdUtil.run(installDateCmd));

                String totalMemCmd = "(Get-WmiObject Win32_OperatingSystem).TotalVisibleMemorySize";
                publish("▶ Loading total memory...  [" + totalMemCmd + "]");
                long totalMemKBytes = Long.parseLong(CmdUtil.run(totalMemCmd)) * 1000;
                totalMemLabel.setText(CmdUtil.formatSize(totalMemKBytes));

                String freeMemCmd = "(Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory";
                publish("▶ Loading available memory...  [" + freeMemCmd + "]");
                long freeMemKBytes = Long.parseLong(CmdUtil.run(freeMemCmd)) * 1000;
                freeMemLabel.setText(CmdUtil.formatSize(freeMemKBytes));

                String ipCmd = "ipconfig | Select-String 'IPv4'";
                publish("▶ Loading IP...  [" + ipCmd + "]");
                String ipLine = CmdUtil.run(ipCmd);
                ipLabel.setText(ipLine.substring(ipLine.lastIndexOf(":") + 1).trim());

                String macCmd = "(Get-NetAdapter | Where-Object { $_.Status -eq 'Up' }).MacAddress";
                publish("▶ Loading MAC address...  [" + macCmd + "]");
                macLabel.setText(CmdUtil.run(macCmd));

                String diskCmd = "(Get-PSDrive -PSProvider FileSystem).Name";
                publish("▶ Loading disk list...  [" + diskCmd + "]");
                List<String> diskNames = CmdUtil.runLines(diskCmd);
                DefaultListModel<String> model = (DefaultListModel<String>) driveList.getModel();
                model.clear();
                for (String name : diskNames)
                    model.addElement(name);

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks)
                    log(msg);
            }

            @Override
            protected void done() {
                long elapsed = System.currentTimeMillis() - startTime;
                log("▶ Done (" + elapsed + "ms)");
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        }.execute();
    }
}
