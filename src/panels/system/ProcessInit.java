package panels.system;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProcessInit extends BasePanel {
    private JLabel cpuValueLabel, memValueLabel, procValueLabel;
    private JButton initBtn;
    private Timer statsTimer;
    private boolean updating = false;

    // 윈도우 동작에 필요한 기본 프로세스
    private static final String[] SYSTEM_WHITELIST = {
            "System", "System Idle Process", "Registry", "smss", "csrss", "wininit",
            "services", "lsass", "winlogon", "fontdrvhost", "dwm", "explorer",
            "sihost", "taskhostw", "ctfmon", "RuntimeBroker", "ShellExperienceHost",
            "SearchHost", "SearchIndexer", "StartMenuExperienceHost", "ApplicationFrameHost",
            "TextInputHost", "SecurityHealthService", "SecurityHealthSystray", "WmiPrvSE",
            "spoolsv", "svchost", "audiodg", "conhost", "dllhost", "LogonUI",
            "MoUsoCoreWorker", "AggregatorHost", "WUDFHost",
            "Memory Compression", "Secure System", "powershell"
    };

    // 백신/메신저 등 종료에서 제외할 프로세스
    private static final String[] EXCEPTION_WHITELIST = {
            "V3Main", "AhnLab", "KakaoTalk", "KakaoTalkUpdate", "Slack", "Discord", "Teams",
            "ms-teams", "Line", "Skype", "Zoom", "Outlook"
    };

    public ProcessInit() {
        super("윈도우 프로세스 초기화", "윈도우 사용에 필요한 기본 프로세스와 백신/메신저를 제외한 모든 프로세스를 종료시킵니다.");
    }

    /*
     * [윈도우 프로세스 초기화] 기능 UI 구현
     * 필요한 내용: {설명/안내 문구}, {초기화 버튼}, {CPU 사용률, 프로세스 수, 실제 메모리 (1초마다 갱신)}, {예외처리 방법}
     */
    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 안내 문구
        contentPanel.add(makeCategoryHeader("안내"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel noticeCard = makeCard();

        JLabel warningLabel = new JLabel("게임 중 렉이 걸리거나, 인터넷이 느릴 때");
        warningLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        warningLabel.setForeground(new Color(200, 60, 60));
        warningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tipLabel = new JLabel("이 기능을 먼저 실행해 주면, 게임이나 인터넷을 할 때 속도 개선에 도움이 됩니다.");
        tipLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        tipLabel.setForeground(new Color(50, 110, 200));
        tipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel routineLabel = new JLabel("컴퓨터 관리가 힘든 분들은 부팅 후 이 기능을 한 번 실행시켜준 뒤 사용하시면 좋습니다.");
        routineLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        routineLabel.setForeground(new Color(100, 100, 110));
        routineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        routineLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        noticeCard.add(warningLabel);
        noticeCard.add(tipLabel);
        noticeCard.add(routineLabel);
        contentPanel.add(noticeCard);
        contentPanel.add(Box.createVerticalStrut(20));

        // 초기화 버튼
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrapper.setBackground(Color.WHITE);
        btnWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        initBtn = makeButton("윈도우 프로세스 초기화");
        initBtn.setPreferredSize(new Dimension(320, 56));
        initBtn.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        initBtn.addActionListener(e -> runProcessInit());

        btnWrapper.add(initBtn);
        contentPanel.add(btnWrapper);
        contentPanel.add(Box.createVerticalStrut(20));

        // 정보
        contentPanel.add(makeCategoryHeader("정보"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel statsCard = makeCard();
        cpuValueLabel = addRow(statsCard, "CPU 사용률:");
        memValueLabel = addRow(statsCard, "실제 메모리 사용률:");
        procValueLabel = addRow(statsCard, "실행 중인 프로세스:");
        contentPanel.add(statsCard);
        contentPanel.add(Box.createVerticalStrut(20));

        // 예외처리 방법 버튼
        JPanel exceptionWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exceptionWrapper.setBackground(Color.WHITE);
        exceptionWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton exceptionBtn = makeButton("예외처리 방법");
        exceptionBtn.addActionListener(e -> showExceptionInfo());
        exceptionWrapper.add(exceptionBtn);
        contentPanel.add(exceptionWrapper);

        // 정보 1초마다 갱신
        statsTimer = new Timer(1000, e -> updateStats());
        statsTimer.start();
        updateStats();
    }

    // 카테고리 헤더 라벨
    private JLabel makeCategoryHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        label.setForeground(new Color(100, 100, 160));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    // 카드 패널 생성
    private JPanel makeCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(248, 249, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225)),
                new EmptyBorder(8, 12, 8, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        return card;
    }

    // 카드 안에 키-값 행 추가, 값 라벨 반환
    private JLabel addRow(JPanel card, String key) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(248, 249, 252));
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        keyLabel.setForeground(new Color(100, 100, 110));
        keyLabel.setPreferredSize(new Dimension(160, 20));

        JLabel valueLabel = new JLabel("—");
        valueLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        valueLabel.setForeground(new Color(200, 60, 60));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);

        card.add(row);

        return valueLabel;
    }

    // CPU 사용률 / 실제 메모리 사용률 / 프로세스 수를 조회해서 1초마다 라벨 갱신
    private void updateStats() {
        if (updating)
            return;
        updating = true;

        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                String cmd = "$c=(Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor | Where-Object {$_.Name -eq '_Total'}).PercentProcessorTime; " +
                        "$o=Get-CimInstance Win32_OperatingSystem; " +
                        "$m=[math]::Round(100-($o.FreePhysicalMemory/$o.TotalVisibleMemorySize*100)); " +
                        "$p=(Get-Process).Count; " +
                        "$c.ToString() + '|' + $m.ToString() + '|' + $p.ToString()";

                return CmdUtil.run(cmd).split("\\|");
            }

            @Override
            protected void done() {
                try {
                    String[] result = get();
                    if (result.length == 3) {
                        cpuValueLabel.setText(result[0] + " %");
                        memValueLabel.setText(result[1] + " %");
                        procValueLabel.setText(result[2] + " 개");
                    }
                } catch (Exception e) {
                    log(e.getMessage());
                } finally {
                    updating = false;
                }
            }
        }.execute();
    }

    // 기본 프로세스, 백신/메신저를 제외한 모든 프로세스 종료
    private void runProcessInit() {
        // 종료 전, 확인창 팝업
        int confirm = JOptionPane.showConfirmDialog(this,
                "기본 프로세스와 백신/메신저를 제외한 모든 프로세스를 종료합니다.\n계속하시겠습니까?",
                "프로세스 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.WARNING_MESSAGE)
            return;

        clearLog();
        log("▶ 윈도우 프로세스 초기화 중...");
        long startTime = System.currentTimeMillis();

        initBtn.setEnabled(false);
        long currentPid = ProcessHandle.current().pid();

        StringBuilder whitelist = new StringBuilder();
        for (String name : SYSTEM_WHITELIST)
            whitelist.append("'").append(name).append("',");
        for (String name : EXCEPTION_WHITELIST)
            whitelist.append("'").append(name).append("',");
        whitelist.setLength(whitelist.length() - 1);    // 끝에 , 제거

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                String cmd = "Get-Process | Where-Object { $_.Id -ne " + currentPid +
                        " -and $_.Id -ne $PID" +
                        " -and ($_.ProcessName -notin @(" + whitelist + ")) } " +
                        "| ForEach-Object { Write-Output $_.ProcessName; Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }";

                return CmdUtil.runLines(cmd).size();
            }

            @Override
            protected void done() {
                try {
                    int killedCount = get();
                    log("▶ 종료된 프로세스 수: " + killedCount);
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                } finally {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log("▶ 완료 (" + elapsed + "ms)");
                    initBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    // 예외처리(종료에서 제외되는 프로세스) 안내
    private void showExceptionInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("[기본 프로세스 - 윈도우 동작에 필요한 프로세스]\n");
        for (String name : SYSTEM_WHITELIST)
            sb.append("- ").append(name).append("\n");

        sb.append("\n[예외 프로세스 - 백신/메신저 등]\n");
        for (String name : EXCEPTION_WHITELIST)
            sb.append("- ").append(name).append("\n");

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(360, 320));

        JOptionPane.showMessageDialog(this, scroll, "예외처리 방법", JOptionPane.INFORMATION_MESSAGE);
    }
}
