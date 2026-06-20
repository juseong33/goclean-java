package panels.system;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceMng extends BasePanel {

    private JTable serviceTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton startBtn, stopBtn, refreshBtn;
    private List<String[]> allServices = new ArrayList<>();
    private List<String[]> displayedServices = new ArrayList<>();

    public ServiceMng() {
        super("Service Manager", "A feature to start/stop service programs.");
    }

    @Override
    protected void initUI() {
        allServices = new ArrayList<>();
        displayedServices = new ArrayList<>();

        contentPanel.setLayout(new BorderLayout(0, 10));

        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setBackground(Color.WHITE);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchPanel.setBackground(Color.WHITE);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(200, 30));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        });

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        refreshBtn = makeButton("Refresh");
        refreshBtn.setPreferredSize(new Dimension(100, 30));
        refreshBtn.addActionListener(e -> loadServices());

        topRow.add(searchPanel, BorderLayout.WEST);
        topRow.add(refreshBtn, BorderLayout.EAST);


        String[] columns = {"Service Name", "Status", "Start Type", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        serviceTable = new JTable(tableModel);
        serviceTable.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        serviceTable.setRowHeight(28);
        serviceTable.setShowGrid(false);
        serviceTable.setIntercellSpacing(new Dimension(0, 0));
        serviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceTable.setFillsViewportHeight(true);
        serviceTable.setPreferredScrollableViewportSize(new Dimension(0, 200));

        JTableHeader tableHeader = serviceTable.getTableHeader();
        tableHeader.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        tableHeader.setBackground(new Color(245, 246, 250));
        tableHeader.setForeground(new Color(80, 80, 100));
        tableHeader.setPreferredSize(new Dimension(0, 32));

        serviceTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        serviceTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        serviceTable.getColumnModel().getColumn(0).setMaxWidth(200);
        serviceTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        serviceTable.getColumnModel().getColumn(1).setMaxWidth(60);
        serviceTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        serviceTable.getColumnModel().getColumn(2).setMaxWidth(80);
        serviceTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Malgun Gothic", Font.BOLD, 12));
                if (!sel)
                    setForeground("Running".equals(val) ? new Color(34, 150, 80) : new Color(160, 160, 170));
                setBorder(new EmptyBorder(0, 4, 0, 4));
                return this;
            }
        });

        serviceTable.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = serviceTable.getSelectedRow() != -1;
            startBtn.setEnabled(sel);
            stopBtn.setEnabled(sel);
        });

        JScrollPane tableScroll = new JScrollPane(serviceTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(16);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Color.WHITE);

        startBtn = makeButton("▶  Start");
        startBtn.setPreferredSize(new Dimension(110, 34));
        startBtn.setEnabled(false);
        startBtn.addActionListener(e -> controlService(true));

        stopBtn = makeDangerButton("■  Stop");
        stopBtn.setPreferredSize(new Dimension(110, 34));
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> controlService(false));

        btnRow.add(startBtn);
        btnRow.add(stopBtn);

        contentPanel.add(topRow,      BorderLayout.NORTH);
        contentPanel.add(tableScroll, BorderLayout.CENTER);
        contentPanel.add(btnRow,      BorderLayout.SOUTH);

        loadServices();
    }

    private void loadServices() {
        refreshBtn.setEnabled(false);
        startBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        tableModel.setRowCount(0);
        allServices.clear();
        displayedServices.clear();
        log("▶ Loading service list...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                String cmd = "Get-CimInstance Win32_Service | " +
                    "Where-Object { $p = ($_.PathName -replace '\"','').Trim(); ($p -notlike 'C:\\Windows*') -and ($p -notlike '%SystemRoot%*') } | " +
                    "Sort-Object DisplayName | ForEach-Object { " +
                    "$state = if ($_.State -eq 'Running') { 'Running' } else { 'Stopped' }; " +
                    "$mode = switch ($_.StartMode) { 'Auto' { 'Auto' } 'Manual' { 'Manual' } 'Disabled' { 'Disabled' } default { $_.StartMode } }; " +
                    "$desc = if ($_.Description) { ($_.Description -replace '[|\\r\\n]', ' ').Trim() } else { '' }; " +
                    "$_.Name + '|' + $_.DisplayName + '|' + $state + '|' + $mode + '|' + $desc }";

                List<String[]> services = new ArrayList<>();
                for (String line : CmdUtil.runLines(cmd)) {
                    String[] parts = line.split("\\|", 5);
                    if (parts.length >= 4) {
                        services.add(new String[]{
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim(),
                            parts.length > 4 ? parts[4].trim() : ""
                        });
                    }
                }
                return services;
            }

            @Override
            protected void done() {
                try {
                    allServices = get();
                    filterTable();
                    log("▶ Loaded " + allServices.size() + " services");
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void filterTable() {
        String search = searchField.getText().toLowerCase();

        tableModel.setRowCount(0);
        displayedServices.clear();

        for (String[] svc : allServices) {
            if (!search.isEmpty() && !svc[1].toLowerCase().contains(search) && !svc[4].toLowerCase().contains(search))
                continue;
            tableModel.addRow(new Object[]{svc[1], svc[2], svc[3], svc[4]});
            displayedServices.add(svc);
        }
    }


    private void controlService(boolean start) {
        if (!CmdUtil.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                "Administrator privileges are required to control services.\nPlease run the program as administrator.",
                "Insufficient Privileges", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int row = serviceTable.getSelectedRow();
        if (row < 0 || row >= displayedServices.size())
            return;

        String[] svc = displayedServices.get(row);
        String name = svc[0];
        String displayName = svc[1];
        String action = start ? "Start" : "Stop";

        int confirm = JOptionPane.showConfirmDialog(this,
            "Do you want to " + action.toLowerCase() + " the '" + displayName + "' service?",
            "Service " + action, JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        startBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        log("▶ " + action + ": '" + displayName + "'...");

        String cmd = start
            ? "Start-Service -Name '" + name + "'"
            : "Stop-Service -Name '" + name + "' -Force";

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return CmdUtil.run(cmd);
            }

            @Override
            protected void done() {
                try {
                    String result = get().trim();
                    log(result.isEmpty()
                        ? "▶ " + action + " complete: '" + displayName + "'"
                        : "[Result] " + result);
                } catch (Exception e) {
                    log("[Error] " + e.getMessage());
                } finally {
                    loadServices();
                }
            }
        }.execute();
    }
}
