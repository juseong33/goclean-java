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

    private static final String[][] SITES = {
            {"KB Bank", "www.kbstar.com"},
            {"Woori Bank", "www.wooribank.com"}
    };

    private Map<String, JLabel> ipLabels;
    private Map<String, JLabel> statusLabels;

    public DnsCheck() {
        super("DNS Check", "<html>A service that checks whether your DNS has been tampered with.<br>" +
                "(If DNS is tampered with, personal information may be leaked.)</html>");
    }

    @Override
    protected void initUI() {
        ipLabels = new LinkedHashMap<>();
        statusLabels = new LinkedHashMap<>();

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JButton refreshBtn = makeButton("Load Info");
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(e -> loadInfo());
        contentPanel.add(refreshBtn);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("DNS Connection Status"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel dnsCard = makeCard();
        primaryDnsLabel = addRow(dnsCard, "Primary DNS");
        secondaryDnsLabel = addRow(dnsCard, "Secondary DNS");
        dnsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, dnsCard.getPreferredSize().height));
        contentPanel.add(dnsCard);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Major Site Connection Status"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel siteCard = makeCard();
        for (String[] site : SITES) {
            String name = site[0];
            JLabel[] labels = addSiteRow(siteCard, name);
            ipLabels.put(name, labels[0]);
            statusLabels.put(name, labels[1]);
        }
        siteCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, siteCard.getPreferredSize().height));
        contentPanel.add(siteCard);
        contentPanel.add(Box.createVerticalStrut(8));

        JLabel noticeLabel = new JLabel("※ Even legitimate sites may show \"Tampering Suspected\" depending on the connection environment.");
        noticeLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
        noticeLabel.setForeground(new Color(150, 150, 150));
        noticeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(noticeLabel);
        contentPanel.add(Box.createVerticalStrut(16));
    }

    private JLabel makeCategoryHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
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
        keyLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        keyLabel.setForeground(new Color(100, 100, 110));
        keyLabel.setPreferredSize(new Dimension(140, 20));

        JLabel valueLabel = new JLabel("—");
        valueLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        valueLabel.setForeground(new Color(30, 30, 30));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);

        card.add(row);

        return valueLabel;
    }

    private JLabel[] addSiteRow(JPanel card, String key) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(248, 249, 252));
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        keyLabel.setForeground(new Color(100, 100, 110));
        keyLabel.setPreferredSize(new Dimension(140, 20));

        JLabel ipLabel = new JLabel("—");
        ipLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        ipLabel.setForeground(new Color(30, 30, 30));

        JLabel statusLabel = new JLabel("-");
        statusLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        statusLabel.setForeground(new Color(100, 100, 110));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setPreferredSize(new Dimension(80, 20));

        row.add(keyLabel, BorderLayout.WEST);
        row.add(ipLabel, BorderLayout.CENTER);
        row.add(statusLabel, BorderLayout.EAST);

        card.add(row);

        return new JLabel[]{ipLabel, statusLabel};
    }

    private void loadInfo() {
        clearLog();
        log("▶ Starting DNS tampering check...");
        long startTime = System.currentTimeMillis();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                String dnsCmd = "(Get-DnsClientServerAddress -AddressFamily IPv4 | " +
                        "Where-Object { $_.ServerAddresses.Count -gt 0 } | " +
                        "Select-Object -First 1).ServerAddresses";
                publish("▶ Querying DNS server...  [" + dnsCmd + "]");
                List<String> dnsServers = CmdUtil.runLines(dnsCmd);

                primaryDnsLabel.setText(dnsServers.size() > 0 ? dnsServers.get(0) : "-");
                secondaryDnsLabel.setText(dnsServers.size() > 1 ? dnsServers.get(1) : "-");

                for (String[] site : SITES) {
                    String name = site[0];
                    String domain = site[1];

                    String localIpCmd = "(Resolve-DnsName " + domain +
                            " -Type A -ErrorAction SilentlyContinue).IPAddress";
                    publish("▶ Checking " + name + " connection...  [" + localIpCmd + "]");
                    List<String> localIps = CmdUtil.runLines(localIpCmd);

                    String safeIpCmd = "(Resolve-DnsName " + domain +
                            " -Type A -Server 8.8.8.8 -ErrorAction SilentlyContinue).IPAddress";
                    List<String> safeIps = CmdUtil.runLines(safeIpCmd);

                    JLabel ipLabel = ipLabels.get(name);
                    JLabel statusLabel = statusLabels.get(name);

                    if (localIps.isEmpty()) {
                        ipLabel.setText("-");
                        statusLabel.setText("Connection Failed");
                        statusLabel.setForeground(new Color(200, 60, 60));
                    } else if (!Collections.disjoint(localIps, safeIps)) {
                        ipLabel.setText(localIps.get(0));
                        statusLabel.setText("Connected");
                        statusLabel.setForeground(new Color(60, 170, 60));
                    } else {
                        ipLabel.setText(localIps.get(0));
                        statusLabel.setText("Tampering Suspected");
                        statusLabel.setForeground(new Color(200, 60, 60));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks)
                    log(msg);
            }

            @Override
            protected void done() {
                long elapsed = System.currentTimeMillis() - startTime;
                log("▶ Done (" + elapsed + "ms)");
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        }.execute();
    }
}
