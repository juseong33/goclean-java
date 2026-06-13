package panels.network;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DnsCheck extends BasePanel {
    private JLabel primaryDnsLabel, secondaryDnsLabel;

    // 주요 사이트: 사이트명 -> 도메인
    private static final String[][] SITES = {
            {"국민은행", "www.kbstar.com"},
            {"우리은행", "www.wooribank.com"}
    };

    private Map<String, JLabel> ipLabels;
    private Map<String, JLabel> statusLabels;

    public DnsCheck() {
        // Description이 두 줄로 보이게 HTML로 처리
        super("DNS 변조 체크", "<html>DNS가 변조 되어있는지 체크하는 서비스입니다.<br>" +
                "(DNS가 변조되면 개인정보 유출 피해가 발생할 수 있습니다.)</html>");
    }

    /*
     * [DNS 변조 체크] 기능 UI 구현
     * 필요한 내용: {기본 DNS, 보조 DNS}, {주요 사이트별 현재 DNS 응답 IP와 정상연결 여부}
     */
    @Override
    protected void initUI() {
        ipLabels = new LinkedHashMap<>();
        statusLabels = new LinkedHashMap<>();

        // 추가된 컴포넌트들을 위에서 아래로 쌓기
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 조회 버튼
        JButton refreshBtn = makeButton("정보 조회");
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(e -> loadInfo());
        contentPanel.add(refreshBtn);
        contentPanel.add(Box.createVerticalStrut(16));  // 여백 생성

        // DNS 연결 상태
        contentPanel.add(makeCategoryHeader("DNS 연결 상태"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel dnsCard = makeCard();
        primaryDnsLabel = addRow(dnsCard, "기본 DNS");
        secondaryDnsLabel = addRow(dnsCard, "보조 DNS");
        dnsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, dnsCard.getPreferredSize().height));    // 가로는 최대, 세로는 동적으로
        contentPanel.add(dnsCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // 주요 사이트 연결 상태
        contentPanel.add(makeCategoryHeader("주요 사이트 연결 상태"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel siteCard = makeCard();
        for (String[] site : SITES) {
            String name = site[0];
            JLabel[] labels = addSiteRow(siteCard, name);
            ipLabels.put(name, labels[0]);      // labels[0]: ip 라벨
            statusLabels.put(name, labels[1]);  // labels[1]: 상태 라벨
        }
        siteCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, siteCard.getPreferredSize().height));
        contentPanel.add(siteCard);
        contentPanel.add(Box.createVerticalStrut(8));

        // 안내 문구 (사이트마다 접속 위치에 따라 다른 서버로 연결될 수 있어서 변조 의심이 표시될 수 있음을 설명)
        JLabel noticeLabel = new JLabel("※ 정상인 사이트도 접속 환경에 따라 \"변조 의심\"으로 뜰 수 있습니다.");
        noticeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        noticeLabel.setForeground(new Color(150, 150, 150));
        noticeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(noticeLabel);
        contentPanel.add(Box.createVerticalStrut(16));
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
                new EmptyBorder(4, 12, 4, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        keyLabel.setPreferredSize(new Dimension(140, 20));

        JLabel valueLabel = new JLabel("—");
        valueLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        valueLabel.setForeground(new Color(30, 30, 30));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);

        card.add(row);

        return valueLabel;
    }

    // 카드 안에 사이트명 - IP - 연결상태 행 추가, [IP 라벨, 상태 라벨] 반환
    // 여기서 key는 국민은행, 신한은행
    private JLabel[] addSiteRow(JPanel card, String key) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(248, 249, 252));
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        keyLabel.setForeground(new Color(100, 100, 110));
        keyLabel.setPreferredSize(new Dimension(140, 20));

        JLabel ipLabel = new JLabel("—");
        ipLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        ipLabel.setForeground(new Color(30, 30, 30));

        JLabel statusLabel = new JLabel("-");
        statusLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        statusLabel.setForeground(new Color(100, 100, 110));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setPreferredSize(new Dimension(80, 20));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(ipLabel, BorderLayout.CENTER);
        row.add(statusLabel, BorderLayout.EAST);

        card.add(row);

        return new JLabel[]{ipLabel, statusLabel};
    }

    // SwingWorker로 백그라운드에서 실행해서 UI가 멈추지 않게 구현
    private void loadInfo() {
        clearLog();
        log("▶ DNS 변조 체크 시작...");
        long startTime = System.currentTimeMillis();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                // 현재 사용중인 DNS 서버 (기본/보조) 조회
                String dnsCmd = "(Get-DnsClientServerAddress -AddressFamily IPv4 | " +
                        "Where-Object { $_.ServerAddresses.Count -gt 0 } | " +
                        "Select-Object -First 1).ServerAddresses";
                publish("▶ DNS 서버 조회 중...  [" + dnsCmd + "]");
                List<String> dnsServers = CmdUtil.runLines(dnsCmd);     // 결과가 여러줄로 나와서 List로 받기

                primaryDnsLabel.setText(dnsServers.size() > 0 ? dnsServers.get(0) : "-");   // 기본 DNS
                secondaryDnsLabel.setText(dnsServers.size() > 1 ? dnsServers.get(1) : "-"); // 보조 DNS

                // 주요 사이트별로 현재 DNS(localIps)와 공용 DNS(safeIps)의 응답 IP 목록을 비교해서 변조 여부 확인
                // 겹치는 IP가 하나라도 있으면 정상으로 판단 ( -First1 명령어를 사용했다가, 변조 의심이 너무 자주 떠서 바꿈 )
                for (String[] site : SITES) {
                    String name = site[0];
                    String domain = site[1];

                    String localIpCmd = "(Resolve-DnsName " + domain +
                            " -Type A -ErrorAction SilentlyContinue).IPAddress";
                    publish("▶ " + name + " 연결 확인 중...  [" + localIpCmd + "]");
                    List<String> localIps = CmdUtil.runLines(localIpCmd);

                    String safeIpCmd = "(Resolve-DnsName " + domain +
                            " -Type A -Server 8.8.8.8 -ErrorAction SilentlyContinue).IPAddress";
                    List<String> safeIps = CmdUtil.runLines(safeIpCmd);

                    JLabel ipLabel = ipLabels.get(name);
                    JLabel statusLabel = statusLabels.get(name);

                    if (localIps.isEmpty()) {
                        ipLabel.setText("-");
                        statusLabel.setText("연결실패");
                        statusLabel.setForeground(new Color(200, 60, 60));
                    } else if (!Collections.disjoint(localIps, safeIps)) {
                        ipLabel.setText(localIps.get(0));
                        statusLabel.setText("정상연결");
                        statusLabel.setForeground(new Color(60, 170, 60));
                    } else {
                        ipLabel.setText(localIps.get(0));
                        statusLabel.setText("변조 의심");
                        statusLabel.setForeground(new Color(200, 60, 60));
                    }
                }
                return null;
            }

            // publish 된 msg를 로그 출력
            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks)
                    log(msg);
            }

            @Override
            protected void done() {
                long elapsed = System.currentTimeMillis() - startTime;
                log("▶ 완료 (" + elapsed + "ms)");
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        }.execute();
    }
}
