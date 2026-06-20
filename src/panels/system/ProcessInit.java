package panels.system;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProcessInit extends BasePanel {
    private JLabel cpuValueLabel, memValueLabel, procValueLabel;
    private JButton initBtn;
    private Timer statsTimer;
    private boolean updating = false;

    private static final String[] SYSTEM_WHITELIST = {
            "System", "System Idle Process", "Registry", "smss", "csrss", "wininit",
            "services", "lsass", "winlogon", "fontdrvhost", "dwm", "explorer",
            "sihost", "taskhostw", "ctfmon", "RuntimeBroker", "ShellExperienceHost",
            "SearchHost", "SearchIndexer", "StartMenuExperienceHost", "ApplicationFrameHost",
            "TextInputHost", "SecurityHealthService", "SecurityHealthSystray", "WmiPrvSE",
            "spoolsv", "svchost", "audiodg", "conhost", "dllhost", "LogonUI",
            "MoUsoCoreWorker", "AggregatorHost", "WUDFHost",
            "Memory Compression", "Secure System", "powershell"
    };

    private static final String[] EXCEPTION_WHITELIST = {
            "V3Main", "AhnLab", "KakaoTalk", "KakaoTalkUpdate", "Slack", "Discord", "Teams",
            "ms-teams", "Line", "Skype", "Zoom", "Outlook"
    };

    public ProcessInit() {
        super("Windows Process Reset", "Terminates all processes except the basic processes required for Windows and antivirus/messenger apps.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(makeCategoryHeader("Notice"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel noticeCard = makeCard();

        JLabel warningLabel = new JLabel("When games lag or the internet is slow");
        warningLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        warningLabel.setForeground(new Color(200, 60, 60));
        warningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tipLabel = new JLabel("Running this feature first helps improve speed when gaming or browsing the internet.");
        tipLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        tipLabel.setForeground(new Color(50, 110, 200));
        tipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel routineLabel = new JLabel("If managing your computer is difficult, it is good to run this feature once after booting.");
        routineLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        routineLabel.setForeground(new Color(100, 100, 110));
        routineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        routineLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        noticeCard.add(warningLabel);
        noticeCard.add(tipLabel);
        noticeCard.add(routineLabel);
        contentPanel.add(noticeCard);
        contentPanel.add(Box.createVerticalStrut(20));

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrapper.setBackground(Color.WHITE);
        btnWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        initBtn = makeButton("Windows Process Reset");
        initBtn.setPreferredSize(new Dimension(320, 56));
        initBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        initBtn.addActionListener(e -> runProcessInit());

        btnWrapper.add(initBtn);
        contentPanel.add(btnWrapper);
        contentPanel.add(Box.createVerticalStrut(20));

        contentPanel.add(makeCategoryHeader("Info"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel statsCard = makeCard();
        cpuValueLabel = addRow(statsCard, "CPU Usage:");
        memValueLabel = addRow(statsCard, "Physical Memory Usage:");
        procValueLabel = addRow(statsCard, "Running Processes:");
        contentPanel.add(statsCard);
        contentPanel.add(Box.createVerticalStrut(20));

        JPanel exceptionWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exceptionWrapper.setBackground(Color.WHITE);
        exceptionWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton exceptionBtn = makeButton("Exclusion List");
        exceptionBtn.addActionListener(e -> showExceptionInfo());
        exceptionWrapper.add(exceptionBtn);
        contentPanel.add(exceptionWrapper);

        statsTimer = new Timer(1000, e -> updateStats());
        statsTimer.start();
        updateStats();
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
                new EmptyBorder(8, 12, 8, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        return card;
    }

    private JLabel addRow(JPanel card, String key) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(248, 249, 252));
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        keyLabel.setForeground(new Color(100, 100, 110));
        keyLabel.setPreferredSize(new Dimension(160, 20));

        JLabel valueLabel = new JLabel("—");
        valueLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        valueLabel.setForeground(new Color(200, 60, 60));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);

        card.add(row);

        return valueLabel;
    }

    private void updateStats() {
        if (updating)
            return;
        updating = true;

        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                String cmd = "$c=(Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor | Where-Object {$_.Name -eq '_Total'}).PercentProcessorTime; " +
                        "$o=Get-CimInstance Win32_OperatingSystem; " +
                        "$m=[math]::Round(100-($o.FreePhysicalMemory/$o.TotalVisibleMemorySize*100)); " +
                        "$p=(Get-Process).Count; " +
                        "$c.ToString() + '|' + $m.ToString() + '|' + $p.ToString()";

                return CmdUtil.run(cmd).split("\\|");
            }

            @Override
            protected void done() {
                try {
                    String[] result = get();
                    if (result.length == 3) {
                        cpuValueLabel.setText(result[0] + " %");
                        memValueLabel.setText(result[1] + " %");
                        procValueLabel.setText(result[2]);
                    }
                } catch (Exception e) {
                    log(e.getMessage());
                } finally {
                    updating = false;
                }
            }
        }.execute();
    }

    private void runProcessInit() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "This will terminate all processes except the basic processes and antivirus/messenger apps.\nDo you want to continue?",
                "Process Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        clearLog();
        log("▶ Resetting Windows processes...");
        long startTime = System.currentTimeMillis();

        initBtn.setEnabled(false);
        long currentPid = ProcessHandle.current().pid();

        StringBuilder whitelist = new StringBuilder();
        for (String name : SYSTEM_WHITELIST)
            whitelist.append("'").append(name).append("',");
        for (String name : EXCEPTION_WHITELIST)
            whitelist.append("'").append(name).append("',");
        whitelist.setLength(whitelist.length() - 1);

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                String cmd = "Get-Process | Where-Object { $_.Id -ne " + currentPid +
                        " -and $_.Id -ne $PID" +
                        " -and ($_.ProcessName -notin @(" + whitelist + ")) } " +
                        "| ForEach-Object { Write-Output $_.ProcessName; Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }";

                return CmdUtil.runLines(cmd).size();
            }

            @Override
            protected void done() {
                try {
                    int killedCount = get();
                    log("▶ Terminated process count: " + killedCount);
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                } finally {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log("▶ Done (" + elapsed + "ms)");
                    initBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void showExceptionInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("[Basic Processes - required for Windows operation]\n");
        for (String name : SYSTEM_WHITELIST)
            sb.append("- ").append(name).append("\n");

        sb.append("\n[Excepted Processes - antivirus/messenger, etc.]\n");
        for (String name : EXCEPTION_WHITELIST)
            sb.append("- ").append(name).append("\n");

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(360, 320));

        JOptionPane.showMessageDialog(this, scroll, "Exclusion List", JOptionPane.INFORMATION_MESSAGE);
    }
}
