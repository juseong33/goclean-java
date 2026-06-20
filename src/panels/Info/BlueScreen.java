package panels.Info;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlueScreen extends BasePanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    private final Map<String, String[]> BUGCHECK_MAP = new LinkedHashMap<>();
    {
        BUGCHECK_MAP.put("0x0000000a", new String[]{"IRQL_NOT_LESS_OR_EQUAL", "Driver conflict, faulty RAM"});
        BUGCHECK_MAP.put("0x0000001e", new String[]{"KMODE_EXCEPTION_NOT_HANDLED", "Faulty driver or hardware"});
        BUGCHECK_MAP.put("0x00000024", new String[]{"NTFS_FILE_SYSTEM", "Faulty hard disk, corrupted file system"});
        BUGCHECK_MAP.put("0x0000003b", new String[]{"SYSTEM_SERVICE_EXCEPTION", "Driver conflict, corrupted Windows"});
        BUGCHECK_MAP.put("0x00000050", new String[]{"PAGE_FAULT_IN_NONPAGED_AREA", "Faulty RAM, driver conflict"});
        BUGCHECK_MAP.put("0x0000007b", new String[]{"INACCESSIBLE_BOOT_DEVICE", "Hard disk contact failure, corrupted boot disk"});
        BUGCHECK_MAP.put("0x0000007e", new String[]{"SYSTEM_THREAD_EXCEPTION_NOT_HANDLED", "Driver conflict, faulty hardware"});
        BUGCHECK_MAP.put("0x0000007f", new String[]{"UNEXPECTED_KERNEL_MODE_TRAP", "Faulty hardware, overclocking/overheating"});
        BUGCHECK_MAP.put("0x0000009f", new String[]{"DRIVER_POWER_STATE_FAILURE", "Laptop battery contact failure, temporary power issue, failed resume from sleep"});
        BUGCHECK_MAP.put("0x000000c2", new String[]{"BAD_POOL_CALLER", "Driver or program conflict"});
        BUGCHECK_MAP.put("0x000000d1", new String[]{"DRIVER_IRQL_NOT_LESS_OR_EQUAL", "Faulty network/graphics driver"});
        BUGCHECK_MAP.put("0x000000ef", new String[]{"CRITICAL_PROCESS_DIED", "Corrupted Windows system files"});
        BUGCHECK_MAP.put("0x000000f4", new String[]{"CRITICAL_OBJECT_TERMINATION", "Faulty hard disk, abnormal system process termination"});
        BUGCHECK_MAP.put("0x00000124", new String[]{"WHEA_UNCORRECTABLE_ERROR", "Aged/faulty hardware such as CPU or RAM"});
    }

    public BlueScreen() {
        super("Blue Screen", "Check blue screen occurrence times and estimated causes. (past 3 months)");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        JButton refreshBtn = makeButton("Load Info");
        refreshBtn.addActionListener(e -> loadInfo());

        countLabel = new JLabel("Blue screen count: -");
        countLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));

        topPanel.add(refreshBtn, BorderLayout.WEST);
        topPanel.add(countLabel, BorderLayout.EAST);

        String[] columns = {"Occurrence Time", "Bug Check String", "Bug Code", "Cause (estimated)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setPreferredScrollableViewportSize(new Dimension(0, 250));
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(400);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(tableScroll, BorderLayout.CENTER);
    }

    private void loadInfo() {
        tableModel.setRowCount(0);
        clearLog();
        log("▶ Loading blue screen records...");

        new SwingWorker<Void, Void>() {
            List<String> lines;

            @Override
            protected Void doInBackground() {
                String getBlueScreenInfo = "$start = (Get-Date).AddMonths(-3); " +
                        "Get-WinEvent -FilterHashtable @{LogName='System'; ProviderName='Microsoft-Windows-WER-SystemErrorReporting'; Id=1001; StartTime=$start} -ErrorAction SilentlyContinue " +
                        "| Sort-Object TimeCreated -Descending " +
                        "| ForEach-Object { $_.TimeCreated.ToString('yyyy-MM-dd HH:mm:ss') + '||' + ($_.Message -replace '[\\r\\n]+', ' ') }";
                lines = CmdUtil.runLines(getBlueScreenInfo);

                return null;
            }

            @Override
            protected void done() {
                Pattern codePattern = Pattern.compile("0x[0-9a-fA-F]{8}");

                for (String line : lines) {
                    String[] parts = line.split("\\|\\|", 2);
                    if (parts.length < 2)
                        continue;

                    String time = parts[0];
                    String message = parts[1];

                    Matcher matcher = codePattern.matcher(message);
                    String code = matcher.find() ? matcher.group().toLowerCase() : "Unknown";

                    String[] info = BUGCHECK_MAP.get(code);
                    String bugCheckString = info != null ? info[0] : "Unknown";
                    String cause = info != null ? info[1] : "Unknown";

                    tableModel.addRow(new Object[]{time, bugCheckString, code, cause});
                }

                countLabel.setText("Blue screen count: " + tableModel.getRowCount());
                log("▶ Done");
            }
        }.execute();
    }
}
