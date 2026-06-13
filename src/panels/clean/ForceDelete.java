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
        super("파일 강제 삭제", "삭제되지 않는 파일을 강제로 삭제할 수 있습니다.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        // 센터: 체크박스 + 파일 경로를 보여주는 테이블
        // DefaultTableModel로 한 이유는 고른 파일을 기반으로 목록의 행 단위로 추가/제거 하기 위함
        String[] columns = {"", "파일명", "경로"};
        tableModel = new DefaultTableModel(columns, 0) {

            // col 0은 체크박스, 나머지는 텍스트인걸 JTable 내부에서 호출
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            // col 0(체크박스)만 수정할 수 있게 오버라이딩함 (파일명, 경로는 읽기전용)
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(0, 300));    // 높이가 300px를 넘어가면 스크롤바가 생김
        table.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(360);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));

        // 하단: 파일 선택, 삭제 버튼
        selectBtn = makeButton("삭제할 파일 선택");
        selectBtn.setPreferredSize(new Dimension(150, 34));
        selectBtn.addActionListener(e -> selectFiles());

        deleteBtn = makeDangerButton("삭제");
        deleteBtn.addActionListener(e -> deleteSelected());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(16, 0, 0, 0));
        bottomPanel.add(selectBtn);
        bottomPanel.add(deleteBtn);

        contentPanel.add(tableScroll, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    // 파일 탐색기를 열어 강제 삭제할 파일들을 목록에 추가
    private void selectFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("삭제할 파일 선택");

        int result = chooser.showOpenDialog(this);  // result:0 -> 열기, result:1 -> 취소, result:-1 -> 오류
        if (result != JFileChooser.APPROVE_OPTION)
            return;  // result:0일때만 Table에 추가하기

        for (File file : chooser.getSelectedFiles())
            tableModel.addRow(new Object[]{true, file.getName(), file.getAbsolutePath()});
        log("▶ " + chooser.getSelectedFiles().length + "개 파일 추가됨");
    }

    // 체크된 파일들을 백그라운드에서 강제로 삭제
    private void deleteSelected() {
        List<Integer> selectedRows = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++)
            if ((boolean) tableModel.getValueAt(i, 0))  // 0열에서 선택된 체크박스 행만 selectedRows에 추가
                selectedRows.add(i);

        if (selectedRows.isEmpty()) {
            log("▶ 선택된 항목이 없습니다.");
            return;
        }

        clearLog();
        deleteBtn.setEnabled(false);    //작업이 실행 되는 동안 버튼을 비활성화
        selectBtn.setEnabled(false);    //작업이 실행 되는 동안 버튼을 비활성화
        log("▶ 파일 강제 삭제 시작");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                // 행 번호가 삭제되면서 바뀌지 않도록 뒤에서부터 처리
                for (int i = selectedRows.size() - 1; i >= 0; i--) {
                    int row = selectedRows.get(i);
                    String name = (String) tableModel.getValueAt(row, 1);
                    String path = (String) tableModel.getValueAt(row, 2);

                    String command = "Remove-Item -LiteralPath '" + path.replace("'", "''") +
                            "' -Force -Recurse -ErrorAction SilentlyContinue";
                    CmdUtil.run(command);

                    if (new File(path).exists())
                        log("▶ " + name + " 삭제 실패");
                    else {
                        log("▶ " + name + " 삭제 완료");
                        SwingUtilities.invokeLater(() -> tableModel.removeRow(row));
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                deleteBtn.setEnabled(true);
                selectBtn.setEnabled(true);
                log("▶ 모든 작업이 완료되었습니다.");
            }
        }.execute();
    }
}
