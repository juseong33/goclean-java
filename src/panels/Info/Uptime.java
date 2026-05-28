package panels.Info;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.io.*;

public class Uptime extends BasePanel {
    private JLabel todayLabel, yesterdayLabel, dayBeforeLabel;
    private JTable table;
    private DefaultTableModel tableModel;

    public Uptime() {
        super("컴퓨터 사용시간", "컴퓨터의 켜진시간과 꺼진시간 한 달치를 체크합니다.");
    }

    /*
     * [컴퓨터 사용시간] 기능 UI 구현
     * 필요한 내용: {오늘, 어제, 그제 총 사용시간}, {타임라인}
     */
    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        // BorderLayout.North
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);

        // 정보 조회, 선택 저장 버튼을 하나의 Panel로 구현
        JButton refreshBtn = makeButton("정보 조회");
        refreshBtn.addActionListener(e -> loadInfo());

        JButton saveBtn = makeButton("선택 저장");
        saveBtn.addActionListener(e -> saveSelected());

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnPanel.add(refreshBtn, BorderLayout.WEST);
        btnPanel.add(saveBtn, BorderLayout.EAST);

        // 요약 패널 (hh시간 mm분 형태로 파싱해서 출력)
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        todayLabel = new JLabel("—");
        yesterdayLabel = new JLabel("—");
        dayBeforeLabel = new JLabel("—");

        summaryPanel.add(new JLabel("■ 오늘 총 사용시간 :"));
        summaryPanel.add(todayLabel);
        summaryPanel.add(new JLabel("■ 어제 :"));
        summaryPanel.add(yesterdayLabel);
        summaryPanel.add(new JLabel("■ 그제 :"));
        summaryPanel.add(dayBeforeLabel);

        topPanel.add(btnPanel);
        topPanel.add(summaryPanel);

        // 센터: 테이블 (정보 조회 버튼 액션 이후 내용들을 불러오기 위해서 DefaultTableModel 사용)
        String[] columns = {"", "날짜", "시간", "내용"};
        tableModel = new DefaultTableModel(columns, 0) {

            // 컬럼의 데이터 타입을 지정 (col == 0(체크박스) 이면 boolean, 나머지는 String)
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }

            // 셀 편집을 0번 컬럼(체크박스)만 클릭가능하게끔
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(0, 250));
        table.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        // 켜진시간, 꺼진시간을 폰트 색상으로 구분하기 위함
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                if ("켜진시간".equals(value))
                    setForeground(new Color(50, 130, 220));   // 파란색
                else if ("꺼진시간".equals(value))
                    setForeground(new Color(200, 60, 60));    // 빨간색

                return this;
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(tableScroll, BorderLayout.CENTER);
    }

    // SwingWorker로 백그라운드에서 실행해서 UI가 멈추지 않게 구현
    private void loadInfo() {
        tableModel.setRowCount(0);
        clearLog();
        log("▶ 사용시간 조회 중...");

        new SwingWorker<Void, Void>() {
            // 날짜별 사용시간 계산 (입력 순서를 유지하기 위해 LinkedHashMap으로 구성)
            // dailyUsage는 각 날짜별 사용시간을 ms로 갖고있음
            Map<String, Long> dailyUsage;
            String today, yesterday, dayBefore;

            @Override
            protected Void doInBackground() {
                String getUptimeInfo = "$start = (Get-Date).AddDays(-30); Get-WinEvent -LogName System " +
                        "| Where-Object { ($_.Id -eq 6005 -or $_.Id -eq 6006) -and $_.TimeCreated -ge $start } " +
                        "| Select-Object TimeCreated, Id | Sort-Object TimeCreated -Descending " +
                        "| ForEach-Object { $_.TimeCreated.ToString('yyyy-MM-dd HH:mm:ss') + ' ' + $_.Id }";

                List<String> lines = CmdUtil.runLines(getUptimeInfo);

                dailyUsage = new LinkedHashMap<>();
                String lastOffDate = null;
                String lastOffTime = null;

                // ex) today = 2026-05-25
                today = getDateBefore(0);
                yesterday = getDateBefore(1);
                dayBefore = getDateBefore(2);

                for (String line : lines) {
                    String[] parts = line.trim().split(" ");
                    // date: 2026-05-25 time: 14:59:15 eventId: 6005 이런식으로 파싱 됨
                    String date = parts[0];
                    String time = parts[1];
                    String eventId = parts[2];
                    //log("date: " + date + " time: " + time + " eventId: " + eventId);

                    // eventId가 6006이면 꺼진 거 (내림차순 정렬이기에 꺼진 시간을 먼저 저장해두고, 켜진 시간을 만났을 때 (꺼진시간 - 켜진(현재)시간)으로 사용 시간 계산)
                    if (eventId.equals("6006")) {
                        lastOffDate = date;
                        lastOffTime = date + " " + time;
                        tableModel.addRow(new Object[]{false, date, time, "꺼진시간"});
                    }
                    // eventId가 6005면 켜진 거
                    else if (eventId.equals("6005")) {
                        tableModel.addRow(new Object[]{false, date, time, "켜진시간"});
                        if (lastOffDate != null) {
                            try {
                                SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                                long on = sdfFull.parse(date + " " + time).getTime();
                                long off = sdfFull.parse(lastOffTime).getTime();

                                // 같은 날이면 단순 누적
                                if (date.equals(lastOffDate)) {
                                    dailyUsage.merge(date, off - on, Long::sum);
                                } else {
                                    // 날짜가 다르면 자정 기준으로 분할
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTimeInMillis(on);                // cal을 켜진 시간(on)으로 설정
                                    cal.add(Calendar.DATE, 1);      // 켜진 시간에서 하루 더함
                                    cal.set(Calendar.HOUR_OF_DAY, 0);       // 시, 분, 초를 전부 0으로 초기화
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.set(Calendar.MILLISECOND, 0);
                                    long midnight = cal.getTimeInMillis();  // 자정을 ms로 변환

                                    dailyUsage.merge(date, midnight - on, Long::sum);          // 켜진 날의 사용 시간 (자정 - 켜진시간)
                                    dailyUsage.merge(lastOffDate, off - midnight, Long::sum);  // 꺼진 날의 사용 시간 (꺼진시간 - 자정)
                                }
                            } catch (ParseException e) {
                                log(e.getMessage());
                            }
                            lastOffTime = null;
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                // 날짜에 해당하는 값(ms단위)을 가져와서 가공한 뒤 각 라벨에 값 세팅
                // 260528 - by juseong33: 노트북 환경에서 테스트 해봤는데, NPE 발생해서 수정 (powershell 명령어가 잘못된듯 ?)
                todayLabel.setText(formatUsageTime(dailyUsage.getOrDefault(today, 0L)));
                yesterdayLabel.setText(formatUsageTime(dailyUsage.getOrDefault(yesterday, 0L)));
                dayBeforeLabel.setText(formatUsageTime(dailyUsage.getOrDefault(dayBefore, 0L)));

                log("▶ 완료");
            }
        }.execute();
    }

    // n일 전 날짜 문자열 반환
    private String getDateBefore(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    // ms -> "hh시간 mm분" 형태로 변환
    private String formatUsageTime(long ms) {
        long hours = ms / (1000 * 60 * 60);
        long minutes = (ms % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%02d시간 %02d분", hours, minutes);
    }

    private void saveSelected() {
        BufferedWriter bw;
        // 체크된 행을 수집
        List<Object[]> selected = new ArrayList<>();    // 체크된 행의 수가 몇 개 인지 미리 알 수 없기에 배열 말고 ArrayList를 이용해서 동적으로 add()
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((boolean) tableModel.getValueAt(i, 0))    // i행 0열에서 체크가 되있으면 True 리턴
                selected.add(new Object[]{tableModel.getValueAt(i, 1), tableModel.getValueAt(i, 2), tableModel.getValueAt(i, 3)});
        }

        if (selected.isEmpty()) {
            log("▶ 선택된 항목이 없습니다.");
            return;
        }

        // data/uptime 폴더에 데이터 저장하기
        File dir = new File("data/uptime");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                log("[오류] 폴더 생성 실패");
                return;
            }
        }

        // 파일명: uptime_yyyy_mm_dd_hh-mm-ss.txt
        String fileName = "uptime_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt";
        File file = new File(dir, fileName);

        try {
            bw = new BufferedWriter(new FileWriter(file));

            bw.write("[선택된 항목]\n");
            bw.write(String.format("%-12s  %-10s  %s%n", "날짜", "시간", "이벤트"));
            bw.write("-------------------------------------------------\n");
            for (Object[] row : selected)
                bw.write(String.format("%-12s  %-10s  %s%n", row[0], row[1], row[2]));  //index 0에는 날짜, index 1에는 시간, index 2에는 이벤트

            bw.flush();
            bw.close();
            log("▶ 저장 완료");
        } catch (IOException e) {
            log(e.getMessage());
        }
    }
}