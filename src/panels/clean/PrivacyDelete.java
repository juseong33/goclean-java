package panels.clean;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PrivacyDelete extends BasePanel {

    // 체크박스와 해당 항목을 삭제할 때 실행할 PowerShell 명령어를 묶어서 관리
    private static class CleanItem {
        final String label;
        final String command;
        JCheckBox checkbox;

        CleanItem(String label, String command) {
            this.label = label;
            this.command = command;
        }
    }

    private static final Color PROGRESS_COLOR = new Color(80, 140, 255);
    private static final Color PROGRESS_DONE_COLOR = new Color(76, 175, 80);
    private List<CleanItem> items;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton deleteBtn;

    public PrivacyDelete() {
        super("개인정보 삭제", "컴퓨터나 인터넷 사용에 따른 사용기록과 목록을 삭제합니다.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        // 총 10개의 객체(label, command)를 담음
        items = new ArrayList<>();

        // 삭제 항목, 명령어 정의 (좌측 5개, 우측 5개 순서로 리스트에 추가)
        items.add(new CleanItem("열어본 페이지 목록 삭제",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\TypedPaths\\*' -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("자동완성 패스워드 삭제",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\IntelliForms\\Storage2' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("폼 자동완성 정보 삭제",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\IntelliForms\\Storage1' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("워드패드 열기 목록 삭제",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Applets\\Wordpad\\Recent File List' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("URL 히스토리 삭제",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\TypedURLs' -Recurse -Force -ErrorAction SilentlyContinue; " +
                        "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Internet Explorer\\TypedURLsTime' -Recurse -Force -ErrorAction SilentlyContinue"));

        items.add(new CleanItem("실행 목록 삭제",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RunMRU\\*' -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("최근 열어본 문서 목록 삭제",
                "Remove-Item -Path \"$env:APPDATA\\Microsoft\\Windows\\Recent\\*\" -Recurse -Force -ErrorAction SilentlyContinue; " +
                        "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RecentDocs\\*' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("시스템 부팅 속도 최적화",
                "Remove-Item -Path 'C:\\Windows\\Prefetch\\*.pf' -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("그림판 기록 삭제",
                "Remove-Item -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Applets\\Paint\\Recent File List' -Recurse -Force -ErrorAction SilentlyContinue"));
        items.add(new CleanItem("오피스 문서 기록 삭제",
                "Get-ChildItem -Path 'HKCU:\\Software\\Microsoft\\Office' -Recurse -ErrorAction SilentlyContinue " +
                        "| Where-Object { $_.PSChildName -eq 'File MRU' -or $_.PSChildName -eq 'Place MRU' } " +
                        "| ForEach-Object { Remove-Item -Path $_.PSPath -Recurse -Force -ErrorAction SilentlyContinue }"));

        // 체크박스 그리드 (좌 5, 우 5 -> 2열 5행으로 배치)
        JPanel gridPanel = new JPanel(new GridLayout(5, 2, 40, 10));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "컴퓨터/인터넷 사용기록 삭제하기",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("맑은 고딕", Font.BOLD, 12)));

        // 좌측 5개를 먼저 채우고, 그 다음 우측 5개를 채워야 GridLayout(5,2) 순서상 좌->우로 짝이 맞음
        for (int i = 0; i < 5; i++) {
            gridPanel.add(makeCheckRow(items.get(i)));
            gridPanel.add(makeCheckRow(items.get(i + 5)));
        }

        // 하단 진행상태 / 삭제 버튼 패널
        statusLabel = new JLabel("준비");
        statusLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 18));
        progressBar.setForeground(PROGRESS_COLOR);

        deleteBtn = makeButton("삭제");
        deleteBtn.addActionListener(e -> startCleanup());

        JPanel bottomPanel = new JPanel(new BorderLayout(12, 0));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(16, 0, 0, 0));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(deleteBtn, BorderLayout.EAST);

        contentPanel.add(gridPanel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    // 매개변수로 받은 CleanItem 클래스의 label을 이용해서 만든 체크박스를 Return (For문에 이용)
    private JCheckBox makeCheckRow(CleanItem item) {
        JCheckBox checkBox = new JCheckBox(item.label, false);
        checkBox.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        checkBox.setBackground(Color.WHITE);
        item.checkbox = checkBox;
        return checkBox;
    }

    // 선택된 항목들을 백그라운드에서 순차적으로 삭제
    private void startCleanup() {
        // 선택된 객체들을 새로운 리스트에 담음
        List<CleanItem> selected = new ArrayList<>();
        for (CleanItem item : items)
            if (item.checkbox.isSelected()) selected.add(item);

        if (selected.isEmpty()) {
            log("▶ 선택된 항목이 없습니다.");
            return;
        }

        clearLog();
        deleteBtn.setEnabled(false);
        progressBar.setValue(0);
        statusLabel.setText("삭제 중...");
        log("▶ 사용기록 삭제 시작");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (CleanItem item : selected) {
                    CmdUtil.run(item.command);
                    log("▶ " + item.label + " 완료");
                }
                return null;
            }

            @Override
            protected void done() {
                deleteBtn.setEnabled(true);
                progressBar.setForeground(PROGRESS_DONE_COLOR);
                progressBar.setValue(100);
                log("▶ 모든 작업이 완료되었습니다.");

                // 잠시 100%로 채워진 모습을 보여준 뒤 다시 비움
                new Timer(400, e -> {
                    statusLabel.setText("준비");
                    progressBar.setValue(0);
                    progressBar.setForeground(PROGRESS_COLOR);
                    ((Timer) e.getSource()).stop();
                }).start();
            }
        }.execute();
    }
}