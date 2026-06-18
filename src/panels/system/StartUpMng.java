package panels.system;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartUpMng extends BasePanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JButton deleteBtn;
    // [0]=Name, [1]=Command, [2]=Location
    private List<String[]> allStartups = new ArrayList<>();

    public StartUpMng() {
        super("시작 프로그램 관리", "컴퓨터 부팅시 실행되는 프로그램들을 관리합니다.");
    }

    @Override
    protected void initUI() {
        allStartups = new ArrayList<>();
        contentPanel.setLayout(new BorderLayout(0, 0));

        // 테이블: 체크박스 | 프로그램 | 열기 | 경로
        String[] columns = {"", "프로그램", "열기", "경로"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 2;
            }
        };

        table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(0, 300));
        table.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(36);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(500);

        table.getColumnModel().getColumn(2).setCellRenderer(new OpenButtonRenderer()); // 버튼이 보이게
        table.getColumnModel().getColumn(2).setCellEditor(new OpenButtonEditor());     // 버튼이 동작하게

        JScrollPane tableScroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(16);

        contentPanel.add(tableScroll, BorderLayout.CENTER);

        // 하단: 삭제 버튼 (우측 정렬)
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.setBorder(new EmptyBorder(16, 0, 0, 0));

        deleteBtn = makeDangerButton("삭제");
        deleteBtn.setPreferredSize(new Dimension(80, 34));
        deleteBtn.addActionListener(e -> deleteSelected());
        btnRow.add(deleteBtn);

        contentPanel.add(btnRow, BorderLayout.SOUTH);

        loadStartups();
    }

    private void loadStartups() {
        tableModel.setRowCount(0);
        allStartups.clear();
        log("▶ 시작 프로그램 목록 불러오는 중...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                // Win32_StartupCommand로 시작 프로그램 목록을 이름순 정렬 후 Name|Command|Location 형태로 출력
                String cmd = "Get-CimInstance Win32_StartupCommand | Sort-Object Name | " +
                        "ForEach-Object { $_.Name + '|' + $_.Command + '|' + $_.Location }";

                List<String[]> startups = new ArrayList<>();
                Set<String> seen = new HashSet<>(); // 이름 중복 체크용 (HKLM/HKCU 등 위치가 달라도 같은 프로그램이면 한 번만 표시)
                for (String line : CmdUtil.runLines(cmd)) {
                    String[] parts = line.split("\\|", 3); // Name|Command|Location 을 최대 3개로 분리
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        if (seen.add(name)) {   // seen에 없는 이름만 추가 (중복 제거)
                            startups.add(new String[]{
                                    name,
                                    parts[1].trim(),                            // Command (실행 경로)
                                    parts.length > 2 ? parts[2].trim() : ""    // Location (레지스트리 위치)
                            });
                        }
                    }
                }
                return startups;
            }

            @Override
            protected void done() {
                try {
                    allStartups = get();
                    for (String[] item : allStartups) {
                        // allStartups의 인덱스와 테이블 행 번호가 1:1 대응되므로 삭제 시 인덱스로 참조 가능
                        tableModel.addRow(new Object[]{false, item[0], "열기", item[1]});
                    }
                    log("▶ 시작 프로그램 " + allStartups.size() + "개 로드 완료");
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                }
            }
        }.execute();
    }

    // Win32_StartupCommand Location → PowerShell 레지스트리 경로 변환
    // HKLM\...\Run → HKLM:\...\Run / HKU\S-1-...\Run → Registry::HKEY_USERS\S-1-...\Run
    private static String toRegistryPath(String location) {
        String loc = location.trim();
        String upper = loc.toUpperCase();
        if (upper.startsWith("HKLM\\"))
            return "HKLM:\\" + loc.substring(5);
        if (upper.startsWith("HKCU\\"))
            return "HKCU:\\" + loc.substring(5);
        if (upper.startsWith("HKU\\"))
            return "Registry::HKEY_USERS\\" + loc.substring(4);
        return null; // 레지스트리 외 위치 (Startup 폴더 등)
    }

    // 실행 명령어 문자열에서 exe 경로만 추출 (인수 제거)
    private static String extractExePath(String command) {
        String path = command.trim();
        if (path.startsWith("\"")) {
            // 따옴표로 감싸진 경우: "C:\Program Files\app.exe" -args → C:\Program Files\app.exe
            int end = path.indexOf('"', 1);
            path = end > 0 ? path.substring(1, end) : path.substring(1);
        } else {
            // 따옴표 없는 경우: C:\app.exe -args → C:\app.exe
            int spaceIdx = path.indexOf(' ');
            if (spaceIdx > 0) path = path.substring(0, spaceIdx);
        }
        return expandEnvVars(path);
    }

    // %windir%, %APPDATA% 등 환경변수를 실제 경로로 치환
    private static String expandEnvVars(String path) {
        Pattern p = Pattern.compile("%(\\w+)%", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String val = System.getenv(m.group(1)); // 시스템 환경변수에서 값 조회
            m.appendReplacement(sb, val != null ? Matcher.quoteReplacement(val) : m.group(0));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void deleteSelected() {
        // 체크된 행 번호 수집
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++)
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0)))
                rows.add(i);

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택해 주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!CmdUtil.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "시작 프로그램을 삭제하려면 관리자 권한이 필요합니다.\n프로그램을 관리자 권한으로 실행해 주세요.",
                    "권한 부족", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "선택한 " + rows.size() + "개의 시작 프로그램을 삭제하시겠습니까?",
                "시작 프로그램 삭제", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.NO_OPTION)
            return;

        // 테이블 행 번호로 allStartups에서 삭제 대상 데이터 추출
        List<String[]> toDelete = new ArrayList<>();
        for (int row : rows)
            toDelete.add(allStartups.get(row));

        deleteBtn.setEnabled(false);
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                int count = 0;
                for (String[] item : toDelete) {
                    String name = item[0].replace("'", "''"); // PowerShell ' 구문오류 방지
                    // Win32_StartupCommand Location을 PowerShell 레지스트리 경로로 변환
                    // 예) HKLM\SOFTWARE\WOW6432Node\...\Run → HKLM:\SOFTWARE\WOW6432Node\...\Run
                    String regPath = toRegistryPath(item[2]);
                    if (regPath == null) {
                        // 시작 프로그램 폴더(.lnk) 등 레지스트리 외 위치는 처리 제외
                        log("[알림] '" + item[0] + "'는 레지스트리 외 위치의 항목입니다.");
                        continue;
                    }
                    // 해당 레지스트리 키의 값(시작 프로그램 항목)만 제거 (프로그램 자체는 삭제되지 않음)
                    CmdUtil.run("Remove-ItemProperty -Path '" + regPath + "' -Name '" + name + "' -ErrorAction SilentlyContinue");
                    log("▶ 삭제: " + item[0]);
                    count++;
                }
                return count;
            }

            @Override
            protected void done() {
                try {
                    log("▶ " + get() + "개 삭제 완료");
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                } finally {
                    deleteBtn.setEnabled(true);
                    loadStartups(); // 삭제 후 목록 갱신
                }
            }
        }.execute();
    }

    // "열기" 컬럼을 버튼처럼 보이게 그려주기
    private static class OpenButtonRenderer extends JButton implements TableCellRenderer {
        OpenButtonRenderer() {
            setText("열기");
            setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this; // 매 셀마다 동일한 버튼 컴포넌트를 재사용해서 그림
        }
    }

    // "열기" 버튼 클릭 시 해당 프로그램의 폴더를 탐색기로 여는 에디터
    private static class OpenButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String exePath; // 클릭된 행의 exe 경로 저장

        OpenButtonEditor() {
            super(new JCheckBox()); // DefaultCellEditor는 JCheckBox/JTextField/JComboBox 기반이어야 해서 더미로 JCheckBox 사용
            button = new JButton("열기");
            button.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                fireEditingStopped();   // 편집 상태 종료 (다음 클릭 정상 동작을 위해)
                if (exePath != null)
                    CmdUtil.run("explorer.exe /select,\"" + exePath + "\""); // 파일을 선택한 채로 탐색기 열기
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            String command = (String) table.getValueAt(row, 3); // 같은 행의 경로 컬럼(col 3) 값 읽기
            exePath = extractExePath(command);  // 실행 인수 제거 후 exe 경로만 추출
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "열기"; // 편집 완료 후 셀에 저장될 값 (버튼 텍스트 유지)
        }
    }
}
