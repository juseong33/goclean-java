package panels.Info;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HardDisk extends BasePanel {
    private JComboBox<String> diskCombo;
    private JLabel tempValueLabel, tempStatusLabel;
    private JLabel powerOnHoursLabel, powerCycleLabel;
    private List<String> diskIds = new ArrayList<>();
    private boolean populatingCombo = false;  // 콤보박스 선택 시 중복/조기 조회를 방지하기 위한 플래그

    public HardDisk() {
        super("하드디스크 상태점검", "하드디스크의 온도와 사용 통계를 확인합니다.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 드라이브 선택 콤보박스
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topPanel.setBackground(Color.WHITE);
        topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        diskCombo = new JComboBox<>();
        diskCombo.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        diskCombo.setPreferredSize(new Dimension(420, 34));
        diskCombo.addActionListener(e -> {
            if (!populatingCombo) {
                int idx = diskCombo.getSelectedIndex();
                if (idx >= 0)
                    loadDiskStats(idx);
            }
        });
        topPanel.add(diskCombo);

        contentPanel.add(topPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // 온도
        contentPanel.add(makeCategoryHeader("온도"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel tempCard = makeCard();

        JPanel tempTopRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        tempTopRow.setBackground(new Color(248, 249, 252));

        tempValueLabel = new JLabel("—");
        tempValueLabel.setFont(new Font("맑은 고딕", Font.BOLD, 36));
        tempValueLabel.setForeground(new Color(50, 50, 50));

        tempStatusLabel = new JLabel("");
        tempStatusLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));

        tempTopRow.add(tempValueLabel);
        tempTopRow.add(tempStatusLabel);
        tempCard.add(tempTopRow);

        JPanel guidePanel = new JPanel();
        guidePanel.setLayout(new BoxLayout(guidePanel, BoxLayout.Y_AXIS));
        guidePanel.setBackground(new Color(248, 249, 252));
        guidePanel.setBorder(new EmptyBorder(0, 4, 6, 4));
        addGuideRow(guidePanel, "0 ~ 50°C", "정상", new Color(40, 160, 80));
        addGuideRow(guidePanel, "51 ~ 60°C", "약간 높음", new Color(200, 130, 0));
        addGuideRow(guidePanel, "60°C 이상", "위험", new Color(200, 60, 60));
        tempCard.add(guidePanel);

        contentPanel.add(tempCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // 사용 통계
        contentPanel.add(makeCategoryHeader("사용 통계"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel statsCard = makeCard();
        powerOnHoursLabel = addRow(statsCard, "사용시간");
        powerCycleLabel = addRow(statsCard, "전원 인가 횟수");
        statsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, statsCard.getPreferredSize().height));
        contentPanel.add(statsCard);
        contentPanel.add(Box.createVerticalStrut(8));

        // 안내 문구
        JLabel notice = new JLabel("※ 사용시간·전원 인가 횟수는 SMART 정보로, 드라이브 종류(특히 NVMe)와 "
                + "권한에 따라 표시되지 않을 수 있습니다.");
        notice.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        notice.setForeground(new Color(150, 150, 150));
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(notice);

        loadDiskList();
    }

    private void loadDiskList() {
        clearLog();
        log("▶ 디스크 목록 조회 중...");

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                // 인덱스|이름|용량
                String cmd = "Get-PhysicalDisk | ForEach-Object { $_.DeviceId + '|' + $_.FriendlyName + '|' + [math]::Round($_.Size / 1GB) }";
                List<String[]> result = new ArrayList<>();
                for (String line : CmdUtil.runLines(cmd)) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3)
                        result.add(parts);
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    List<String[]> disks = get();
                    populatingCombo = true;
                    diskIds.clear();
                    diskCombo.removeAllItems();
                    for (String[] parts : disks) {
                        diskIds.add(parts[0].trim());
                        diskCombo.addItem(parts[1].trim() + " (" + parts[2].trim() + "GB)");
                    }
                    populatingCombo = false;

                    if (diskCombo.getItemCount() > 0)
                        diskCombo.setSelectedIndex(0);  // ActionListener -> loadDiskStats(0)

                    log("▶ 목록 로드 완료");
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                }
            }
        }.execute();
    }

    private void loadDiskStats(int diskIndex) {
        if (diskIndex < 0 || diskIndex >= diskIds.size())
            return;
        String deviceId = diskIds.get(diskIndex);
        log("▶ 디스크 정보 조회 중...");

        new SwingWorker<String[], Void>() {
            boolean admin;

            @Override
            protected String[] doInBackground() {
                admin = CmdUtil.isAdmin();

                // 온도 (run으로 단일 값 조회)
                String tempCmd = "$d = Get-PhysicalDisk | Where-Object { $_.DeviceId -eq " + deviceId + " }; " +
                        "($d | Get-StorageReliabilityCounter).Temperature";
                String temp = CmdUtil.run(tempCmd).trim();

                // 사용 통계 (빈 값도 줄이 유지되도록 접두어 출력)
                String statCmd = "$d = Get-PhysicalDisk | Where-Object { $_.DeviceId -eq " + deviceId + " }; " +
                        "$rc = $d | Get-StorageReliabilityCounter; " +
                        "'HOURS=' + $rc.PowerOnHours; " +
                        "'CYCLE=' + $rc.StartStopCycleCount";
                String hours = "", cycle = "";
                for (String line : CmdUtil.run(statCmd).split("\n")) {
                    line = line.trim();
                    if (line.startsWith("HOURS="))
                        hours = line.substring(6).trim();
                    else if (line.startsWith("CYCLE="))
                        cycle = line.substring(6).trim();
                }
                return new String[]{temp, hours, cycle};
            }

            @Override
            protected void done() {
                try {
                    String[] v = get();
                    Integer temp = parseIntOrNull(v[0]);
                    Integer hours = parseIntOrNull(v[1]);
                    Integer cycle = parseIntOrNull(v[2]);

                    String fallback = admin ? "측정 불가" : "관리자 권한 필요";

                    // 온도
                    if (temp != null) {
                        tempValueLabel.setText(temp + " °C");
                        Color c;
                        String status;
                        if (temp <= 50) {
                            c = new Color(40, 160, 80);
                            status = "정상";
                        } else if (temp <= 60) {
                            c = new Color(200, 130, 0);
                            status = "약간 높음";
                        } else {
                            c = new Color(200, 60, 60);
                            status = "위험";
                        }
                        tempValueLabel.setForeground(c);
                        tempStatusLabel.setText(status);
                        tempStatusLabel.setForeground(c);
                    } else {
                        tempValueLabel.setText(fallback);
                        tempValueLabel.setForeground(new Color(150, 150, 150));
                        tempStatusLabel.setText("");
                    }

                    // 사용시간 (n일 m시간 형태)
                    if (hours != null) {
                        int d = hours / 24, h = hours % 24;
                        powerOnHoursLabel.setText(d > 0 ? d + "일 " + h + "시간 (" + hours + "시간)" : hours + "시간");
                    } else
                        powerOnHoursLabel.setText(fallback + "(NVMe 드라이버 한계)");

                    // 전원 인가 횟수
                    powerCycleLabel.setText(cycle != null ? cycle + " 회" : fallback + "(NVMe 드라이버 한계)");

                    log("▶ 완료");
                } catch (Exception e) {
                    log("[오류] " + e.getMessage());
                }
            }
        }.execute();
    }

    // 문자열을 정수로 파싱, 비어있거나 숫자가 아니면 null 반환
    private static Integer parseIntOrNull(String s) {
        if (s == null || s.trim().isEmpty())
            return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void addGuideRow(JPanel parent, String range, String desc, Color color) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBackground(new Color(248, 249, 252));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
        dot.setForeground(color);

        JLabel rangeLabel = new JLabel(range);
        rangeLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        rangeLabel.setForeground(color);
        rangeLabel.setPreferredSize(new Dimension(100, 16));

        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        descLabel.setForeground(new Color(80, 80, 80));

        row.add(dot);
        row.add(rangeLabel);
        row.add(descLabel);

        parent.add(row);
    }

    private JLabel makeCategoryHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        label.setForeground(new Color(100, 100, 160));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private JPanel makeCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(248, 249, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225)),
                new EmptyBorder(4, 12, 4, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        return card;
    }

    private JLabel addRow(JPanel card, String key) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(248, 249, 252));
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        keyLabel.setForeground(new Color(100, 100, 110));
        keyLabel.setPreferredSize(new Dimension(140, 20));

        JLabel valueLabel = new JLabel("—");
        valueLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        valueLabel.setForeground(new Color(30, 30, 30));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        card.add(row);

        return valueLabel;
    }
}
