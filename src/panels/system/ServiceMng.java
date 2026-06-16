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

// 서비스 시작/중지는 관리자 권한이 필요함
// 관리자 권한 없이 실행하면 경고창이 먼저 뜨게 구성
public class ServiceMng extends BasePanel {

    private JTable serviceTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton startBtn, stopBtn, refreshBtn;
    // [0]=Name(내부), [1]=DisplayName, [2]=State(KR), [3]=StartMode(KR), [4]=Description
    private List<String[]> allServices = new ArrayList<>();
    private List<String[]> displayedServices = new ArrayList<>();

    public ServiceMng() {
        super("서비스 관리", "서비스 프로그램을 시작/중지 시키는 기능입니다.");
    }

    @Override
    protected void initUI() {
        allServices = new ArrayList<>();
        displayedServices = new ArrayList<>();

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 검색 행
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setBackground(Color.WHITE);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchPanel.setBackground(Color.WHITE);

        JLabel searchLabel = new JLabel("검색:");
        searchLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        searchField = new JTextField(20);
        searchField.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(200, 30));

        // 테이블이 즉시 필터링되는 실시간 검색 기능을 구현
        // 입력 한 글자마다 반응하게 DocumentListener를 사용
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

        refreshBtn = makeButton("새로고침");
        refreshBtn.setPreferredSize(new Dimension(100, 30));
        refreshBtn.addActionListener(e -> loadServices());

        topRow.add(searchPanel, BorderLayout.WEST);
        topRow.add(refreshBtn, BorderLayout.EAST);

        contentPanel.add(topRow);
        contentPanel.add(Box.createVerticalStrut(10));

        // 테이블
        String[] columns = {"서비스명", "상태", "시작유형", "설명"};
        tableModel = new DefaultTableModel(columns, 0) {
            // 테이블 수정 불가능
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        serviceTable = new JTable(tableModel);
        serviceTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        serviceTable.setRowHeight(28);
        serviceTable.setShowGrid(false);
        serviceTable.setIntercellSpacing(new Dimension(0, 0));
        serviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceTable.setFillsViewportHeight(true);

        JTableHeader tableHeader = serviceTable.getTableHeader();
        tableHeader.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        tableHeader.setBackground(new Color(245, 246, 250));
        tableHeader.setForeground(new Color(80, 80, 100));
        tableHeader.setPreferredSize(new Dimension(0, 32));

        serviceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        serviceTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        serviceTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        serviceTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        serviceTable.getColumnModel().getColumn(3).setPreferredWidth(600);
        // 상태 열: 실행중=초록, 중지=회색
        serviceTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("맑은 고딕", Font.BOLD, 12));
                if (!sel)
                    setForeground("시작".equals(val) ? new Color(34, 150, 80) : new Color(160, 160, 170));
                setBorder(new EmptyBorder(0, 4, 0, 4));
                return this;
            }
        });

        serviceTable.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = serviceTable.getSelectedRow() != -1;
            startBtn.setEnabled(sel);   // 선택된 행 있으면 true → 버튼 활성화
            stopBtn.setEnabled(sel);    // 선택된 행 없으면 false → 버튼 비활성화
        });

        JScrollPane tableScroll = new JScrollPane(serviceTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setPreferredSize(new Dimension(600, 300));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(16);

        contentPanel.add(tableScroll);
        contentPanel.add(Box.createVerticalStrut(12));

        // 버튼 행
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        startBtn = makeButton("▶  실행");
        startBtn.setPreferredSize(new Dimension(110, 34));
        startBtn.setEnabled(false);
        startBtn.addActionListener(e -> controlService(true));

        stopBtn = makeDangerButton("■  중지");
        stopBtn.setPreferredSize(new Dimension(110, 34));
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> controlService(false));

        btnRow.add(startBtn);
        btnRow.add(stopBtn);

        contentPanel.add(btnRow);

        loadServices();
    }

    // PowerShell로 서비스 목록을 조회해서 allServices에 저장
    private void loadServices() {
        refreshBtn.setEnabled(false);
        startBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        tableModel.setRowCount(0);
        allServices.clear();
        displayedServices.clear();
        log("▶ 서비스 목록 불러오는 중...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                String cmd = "Get-CimInstance Win32_Service | " +
                    "Where-Object { $p = ($_.PathName -replace '\"','').Trim(); ($p -notlike 'C:\\Windows*') -and ($p -notlike '%SystemRoot%*') } | " +
                    "Sort-Object DisplayName | ForEach-Object { " +
                    "$state = if ($_.State -eq 'Running') { '시작' } else { '중지' }; " +
                    "$mode = switch ($_.StartMode) { 'Auto' { '자동' } 'Manual' { '수동' } 'Disabled' { '사용안함' } default { $_.StartMode } }; " +
                    "$desc = if ($_.Description) { ($_.Description -replace '[|\\r\\n]', ' ').Trim() } else { '' }; " +
                    "$_.Name + '|' + $_.DisplayName + '|' + $state + '|' + $mode + '|' + $desc }";

                List<String[]> services = new ArrayList<>();
                for (String line : CmdUtil.runLines(cmd)) {     // "Everything|Everything|시작|자동|파일 검색 도구입니다" 형태로 받음
                    String[] parts = line.split("\\|", 5);
                    if (parts.length >= 4) {
                        services.add(new String[]{
                            parts[0].trim(),                            // Everything
                            parts[1].trim(),                            // Everything
                            parts[2].trim(),                            // 시작
                            parts[3].trim(),                            // 자동
                            parts.length > 4 ? parts[4].trim() : ""     // 파일 검색 도구입니다.
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
                    log("▶ 서비스 " + allServices.size() + "개 로드 완료");
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    // allServices에서 검색어에 맞는 항목만 골라 테이블에 표시
    private void filterTable() {
        String search = searchField.getText().toLowerCase();

        tableModel.setRowCount(0);
        displayedServices.clear();

        for (String[] svc : allServices) {
            // testField에 문자열이 비어있으면 allServices가 테이블에 표시됨
            if (!search.isEmpty() && !svc[1].toLowerCase().contains(search) && !svc[4].toLowerCase().contains(search))
                continue;
            tableModel.addRow(new Object[]{svc[1], svc[2], svc[3], svc[4]});
            displayedServices.add(svc);
        }
    }


    // 실행/중지 버튼을 구분하여 명령어를 실행
    private void controlService(boolean start) {
        // 프로그램이 관리자 권한으로 실행되지 않았을 경우 경고창 출력
        if (!CmdUtil.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                "서비스를 제어하려면 관리자 권한이 필요합니다.\n프로그램을 관리자 권한으로 실행해 주세요.",
                "권한 부족", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int row = serviceTable.getSelectedRow();
        if (row < 0 || row >= displayedServices.size())
            return;

        String[] svc = displayedServices.get(row);
        String name = svc[0];
        String displayName = svc[1];
        String action = start ? "실행" : "중지";

        int confirm = JOptionPane.showConfirmDialog(this,
            "'" + displayName + "' 서비스를 " + action + "하시겠습니까?",
            "서비스 " + action, JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        startBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        log("▶ '" + displayName + "' " + action + " 중...");

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
                        ? "▶ '" + displayName + "' " + action + " 완료"
                        : "[결과] " + result);
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                } finally {
                    loadServices();
                }
            }
        }.execute();
    }
}
