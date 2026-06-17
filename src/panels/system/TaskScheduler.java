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
    // [0]=TaskName, [1]=Author, [2]=FilePath(표시용)
    private List<String[]> allTasks = new ArrayList<>();

    public TaskScheduler() {
        super("작업 스케줄러 관리", "불필요한 작업 스케줄러가 등록된 경우 광고창이 뜨고, 컴퓨터 속도가 느려집니다.");
    }

    @Override
    protected void initUI() {
        allTasks = new ArrayList<>();
        contentPanel.setLayout(new BorderLayout());

        // 상단: 카운트 라벨
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topRow.setBackground(Color.WHITE);
        topRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        countLabel = new JLabel("작업 스케줄러: 로딩 중...");
        countLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        countLabel.setForeground(new Color(50, 110, 200));
        topRow.add(countLabel);

        // 테이블: 체크박스 | 프로그램 | 제작사 | 경로
        String[] columns = {"", "프로그램", "제작사", "경로"};
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
        table.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));

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

        // 하단: 삭제 버튼 (우측 정렬)
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.setBorder(new EmptyBorder(16, 0, 0, 0));

        deleteBtn = makeDangerButton("삭제");
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
        countLabel.setText("작업 스케줄러: 로딩 중...");
        log("▶ 작업 스케줄러 목록 불러오는 중...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                // 루트(\) 레벨 작업만 조회, 이름순 정렬 후 TaskName|Author|FilePath 형태로 출력
                String cmd = "Get-ScheduledTask | " +
                        "Where-Object { $_.TaskPath -eq '\\' } | " +
                        "Sort-Object TaskName | " +
                        "ForEach-Object { " +
                        "$a = if ($_.Author) { ($_.Author -replace '[|\\r\\n]',' ').Trim() } else { '알 수 없음' }; " +
                        "$p = $env:SystemRoot + '\\System32\\Tasks\\' + $_.TaskName; " +
                        "$_.TaskName + '|' + $a + '|' + $p" +
                        "}";

                List<String[]> tasks = new ArrayList<>();
                for (String line : CmdUtil.runLines(cmd)) {
                    // "TaskName|Author|FilePath" 형태, FilePath 안에 | 가 있어도 잘리지 않도록 최대 3개로 분리
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 2) {
                        tasks.add(new String[]{
                                parts[0].trim(),                            // TaskName
                                parts[1].trim(),                            // Author
                                parts.length > 2 ? parts[2].trim() : ""    // FilePath (TaskName, Author만 있는 경우, parts[2]를 바로 쓰면 Exception 발생)
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
                    countLabel.setText("작업 스케줄러: " + allTasks.size() + "개");
                    log("▶ 작업 스케줄러 " + allTasks.size() + "개 로드 완료");
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                }
            }
        }.execute();
    }

    private void deleteSelected() {
        List<Integer> rows = new ArrayList<>();     // 체크된 행 인덱스를 담음
        for (int i = 0; i < tableModel.getRowCount(); i++)
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0)))
                rows.add(i);

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택해 주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "선택한 " + rows.size() + "개의 작업 스케줄러를 삭제하시겠습니까?",
                "작업 스케줄러 삭제", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.NO_OPTION)
            return;

        if (!CmdUtil.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "작업 스케줄러를 삭제하려면 관리자 권한이 필요합니다.\n프로그램을 관리자 권한으로 실행해 주세요.",
                    "권한 부족", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String[]> toDelete = new ArrayList<>();    // 삭제할 항목들을 담음
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
                    log("▶ 삭제: " + task[0]);
                    count++;
                }
                return count;
            }

            @Override
            protected void done() {
                try {
                    log("▶ " + get() + "개 삭제 완료");      // doInBackground에서 반환된 count 개수
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                } finally {
                    deleteBtn.setEnabled(true);
                    loadTasks();
                }
            }
        }.execute();
    }
}
