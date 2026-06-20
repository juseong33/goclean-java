package panels.clean;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ForceDelete extends BasePanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton selectBtn, deleteBtn;

    public ForceDelete() {
        super("Force Delete", "Forcibly delete files that cannot be deleted normally.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        String[] columns = {"", "File Name", "Path"};
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
        table.setPreferredScrollableViewportSize(new Dimension(0, 300));
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(360);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));

        selectBtn = makeButton("Select Files to Delete");
        selectBtn.setPreferredSize(new Dimension(150, 34));
        selectBtn.addActionListener(e -> selectFiles());

        deleteBtn = makeDangerButton("Delete");
        deleteBtn.addActionListener(e -> deleteSelected());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(16, 0, 0, 0));
        bottomPanel.add(selectBtn);
        bottomPanel.add(deleteBtn);

        contentPanel.add(tableScroll, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void selectFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Select Files to Delete");

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION)
            return;

        for (File file : chooser.getSelectedFiles())
            tableModel.addRow(new Object[]{true, file.getName(), file.getAbsolutePath()});
        log("▶ " + chooser.getSelectedFiles().length + " file(s) added");
    }

    private void deleteSelected() {
        List<Integer> selectedRows = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++)
            if ((boolean) tableModel.getValueAt(i, 0))
                selectedRows.add(i);

        if (selectedRows.isEmpty()) {
            log("▶ No items selected.");
            return;
        }

        clearLog();
        deleteBtn.setEnabled(false);
        selectBtn.setEnabled(false);
        log("▶ Starting force delete");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (int i = selectedRows.size() - 1; i >= 0; i--) {
                    int row = selectedRows.get(i);
                    String name = (String) tableModel.getValueAt(row, 1);
                    String path = (String) tableModel.getValueAt(row, 2);

                    String command = "Remove-Item -LiteralPath '" + path.replace("'", "''") +
                            "' -Force -Recurse -ErrorAction SilentlyContinue";
                    CmdUtil.run(command);

                    if (new File(path).exists())
                        log("▶ " + name + " delete failed");
                    else {
                        log("▶ " + name + " deleted");
                        SwingUtilities.invokeLater(() -> tableModel.removeRow(row));
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                deleteBtn.setEnabled(true);
                selectBtn.setEnabled(true);
                log("▶ All tasks completed.");
            }
        }.execute();
    }
}
