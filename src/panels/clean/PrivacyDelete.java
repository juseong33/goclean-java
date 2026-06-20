package panels.clean;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PrivacyDelete extends BasePanel {

    private static class CleanItem {
        final String label;
        final String command;
        JCheckBox checkbox;

        CleanItem(String label, String command) {
            this.label = label;
            this.command = command;
        }
    }

    private static final Color PROGRESS_COLOR = new Color(80, 140, 255);
    private static final Color PROGRESS_DONE_COLOR = new Color(76, 175, 80);
    private List<CleanItem> items;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton deleteBtn;

    public PrivacyDelete() {
        super("Privacy Cleanup", "Deletes usage records and lists created by computer and internet use.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        items = new ArrayList<>();

        items.add(new CleanItem("Delete visited page list",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\TypedPaths\\*' -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("Delete autocomplete passwords",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\IntelliForms\\Storage2' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("Delete form autocomplete data",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\IntelliForms\\Storage1' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("Delete WordPad recent file list",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Applets\\Wordpad\\Recent File List' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("Delete URL history",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\TypedURLs' -Recurse -Force -ErrorAction SilentlyContinue; " +
                        "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\TypedURLsTime' -Recurse -Force -ErrorAction SilentlyContinue"));

        items.add(new CleanItem("Delete Run history",
                "reg delete \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RunMRU\" /va /f"));
        items.add(new CleanItem("Delete recently opened documents list",
                "Remove-Item -Path \"$env:APPDATA\\Microsoft\\Windows\\Recent\\*\" -Recurse -Force -ErrorAction SilentlyContinue; " +
                        "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RecentDocs\\*' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("Optimize system boot speed",
                "Remove-Item -Path 'C:\\Windows\\Prefetch\\*.pf' -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("Delete Paint history",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Applets\\Paint\\Recent File List' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("Delete Office document history",
                "Get-ChildItem -Path 'HKCU:\\Software\\Microsoft\\Office' -Recurse -ErrorAction SilentlyContinue " +
                        "| Where-Object { $_.PSChildName -eq 'File MRU' -or $_.PSChildName -eq 'Place MRU' } " +
                        "| ForEach-Object { Remove-Item -Path $_.PSPath -Recurse -Force -ErrorAction SilentlyContinue }"));

        JPanel gridPanel = new JPanel(new GridLayout(5, 2, 40, 10));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Delete computer/internet usage records",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("Malgun Gothic", Font.BOLD, 12)));

        for (int i = 0; i < 5; i++) {
            gridPanel.add(makeCheckRow(items.get(i)));
            gridPanel.add(makeCheckRow(items.get(i + 5)));
        }

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 18));
        progressBar.setForeground(PROGRESS_COLOR);

        deleteBtn = makeButton("Delete");
        deleteBtn.addActionListener(e -> startCleanup());

        JPanel bottomPanel = new JPanel(new BorderLayout(12, 0));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(16, 0, 0, 0));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(deleteBtn, BorderLayout.EAST);

        contentPanel.add(gridPanel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JCheckBox makeCheckRow(CleanItem item) {
        JCheckBox checkBox = new JCheckBox(item.label, false);
        checkBox.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        checkBox.setBackground(Color.WHITE);
        item.checkbox = checkBox;
        return checkBox;
    }

    private void startCleanup() {
        List<CleanItem> selected = new ArrayList<>();
        for (CleanItem item : items)
            if (item.checkbox.isSelected()) selected.add(item);

        if (selected.isEmpty()) {
            log("▶ No items selected.");
            return;
        }

        clearLog();
        deleteBtn.setEnabled(false);
        progressBar.setValue(0);
        statusLabel.setText("Deleting...");
        log("▶ Starting usage record cleanup");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (CleanItem item : selected) {
                    CmdUtil.run(item.command);
                    log("▶ " + item.label + " done");
                }
                return null;
            }

            @Override
            protected void done() {
                deleteBtn.setEnabled(true);
                progressBar.setForeground(PROGRESS_DONE_COLOR);
                progressBar.setValue(100);
                log("▶ All tasks completed.");

                new Timer(400, e -> {
                    statusLabel.setText("Ready");
                    progressBar.setValue(0);
                    progressBar.setForeground(PROGRESS_COLOR);
                    ((Timer) e.getSource()).stop();
                }).start();
            }
        }.execute();
    }
}
