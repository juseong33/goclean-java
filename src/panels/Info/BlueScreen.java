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

    // 버그 체크 코드 -> {버그 체크 문자열, 원인(추정)}
    private final Map<String, String[]> BUGCHECK_MAP = new LinkedHashMap<>();
    {
        BUGCHECK_MAP.put("0x0000000a", new String[]{"IRQL_NOT_LESS_OR_EQUAL", "드라이버 충돌, 램 불량"});
        BUGCHECK_MAP.put("0x0000001e", new String[]{"KMODE_EXCEPTION_NOT_HANDLED", "드라이버 또는 하드웨어 불량"});
        BUGCHECK_MAP.put("0x00000024", new String[]{"NTFS_FILE_SYSTEM", "하드디스크 불량, 파일시스템 손상"});
        BUGCHECK_MAP.put("0x0000003b", new String[]{"SYSTEM_SERVICE_EXCEPTION", "드라이버 충돌, 윈도우 손상"});
        BUGCHECK_MAP.put("0x00000050", new String[]{"PAGE_FAULT_IN_NONPAGED_AREA", "램 불량, 드라이버 충돌"});
        BUGCHECK_MAP.put("0x0000007b", new String[]{"INACCESSIBLE_BOOT_DEVICE", "하드디스크 접촉 불량, 부팅 디스크 손상"});
        BUGCHECK_MAP.put("0x0000007e", new String[]{"SYSTEM_THREAD_EXCEPTION_NOT_HANDLED", "드라이버 충돌, 하드웨어 불량"});
        BUGCHECK_MAP.put("0x0000007f", new String[]{"UNEXPECTED_KERNEL_MODE_TRAP", "하드웨어 불량, 오버클럭/과열"});
        BUGCHECK_MAP.put("0x0000009f", new String[]{"DRIVER_POWER_STATE_FAILURE", "노트북 배터리 접촉 불량, 일시적인 전원문제, 절전모드 복귀 실패"});
        BUGCHECK_MAP.put("0x000000c2", new String[]{"BAD_POOL_CALLER", "드라이버 또는 프로그램 충돌"});
        BUGCHECK_MAP.put("0x000000d1", new String[]{"DRIVER_IRQL_NOT_LESS_OR_EQUAL", "네트워크/그래픽 드라이버 불량"});
        BUGCHECK_MAP.put("0x000000ef", new String[]{"CRITICAL_PROCESS_DIED", "윈도우 시스템 파일 손상"});
        BUGCHECK_MAP.put("0x000000f4", new String[]{"CRITICAL_OBJECT_TERMINATION", "하드디스크 불량, 시스템 프로세스 비정상 종료"});
        BUGCHECK_MAP.put("0x00000124", new String[]{"WHEA_UNCORRECTABLE_ERROR", "CPU, 램 등 하드웨어 노후/불량"});
    }

    public BlueScreen() {
        super("블루스크린", "블루스크린 발생 시간과 추정 원인을 확인할 수 있습니다. (세 달치)");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        // 상단: 정보 조회 버튼, 발생 수
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        JButton refreshBtn = makeButton("정보 조회");
        refreshBtn.addActionListener(e -> loadInfo());

        countLabel = new JLabel("블루스크린 발생수: -");
        countLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));

        topPanel.add(refreshBtn, BorderLayout.WEST);
        topPanel.add(countLabel, BorderLayout.EAST);

        // 센터: 블루스크린 정보 테이블
        String[] columns = {"블루스크린 발생 시간", "버그 체크 문자열", "버그 코드", "원인(추정)"};
        tableModel = new DefaultTableModel(columns, 0) {
            // 셀을 수정할 수 없게 고정
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setPreferredScrollableViewportSize(new Dimension(0, 250));
        table.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(400);

        // 모든 셀을 가운데 정렬
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
        log("▶ 블루스크린 기록 조회 중...");

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
                // 0x + 16진수 8자리(예: 0x0000009f) 형태를 찾는 정규식 패턴을 미리 컴파일.
                // 버그 체크 코드는 항상 이 형식이므로, 메시지 전체 텍스트 안에서 이 패턴을 검색해 코드를 뽑아냄.
                Pattern codePattern = Pattern.compile("0x[0-9a-fA-F]{8}");

                for (String line : lines) {
                    String[] parts = line.split("\\|\\|", 2);
                    if (parts.length < 2)
                        continue;

                    String time = parts[0];     // 발생 시간 문자열 (yyyy-MM-dd HH:mm:ss)
                    String message = parts[1];  // 이벤트 메시지 전체 텍스트 (여기 안에 버그 체크 코드가 포함됨)

                    Matcher matcher = codePattern.matcher(message);
                    String code = matcher.find() ? matcher.group().toLowerCase() : "알 수 없음";

                    String[] info = BUGCHECK_MAP.get(code);
                    String bugCheckString = info != null ? info[0] : "알 수 없음";
                    String cause = info != null ? info[1] : "알 수 없음";

                    tableModel.addRow(new Object[]{time, bugCheckString, code, cause});
                }

                countLabel.setText("블루스크린 발생수: " + tableModel.getRowCount());
                log("▶ 완료");
            }
        }.execute();
    }
}
