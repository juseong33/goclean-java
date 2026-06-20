import panels.Info.*;
import panels.etc.*;
import panels.network.DnsCheck;
import panels.system.*;
import panels.clean.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel content;

    public MainFrame() {
        setTitle("GoClean with Java");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 720);
        setResizable(false);
        setLocationRelativeTo(null);

        buildUI();

    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        JPanel topBar = buildTopBar();

        JPanel sidebar = buildSidebar();

        cardLayout = new CardLayout();
        content = new JPanel(cardLayout);
        content.setBackground(Color.WHITE);

        JPanel emptyPanel = new JPanel();
        emptyPanel.setBackground(Color.WHITE);
        content.add(emptyPanel, "empty");


        content.add(new ServiceMng(), "Service Manager");
        content.add(new StartUpMng(), "Startup Manager");
        content.add(new TaskScheduler(), "Task Scheduler");
        content.add(new ProcessInit(), "Process Manager");

        content.add(new PrivacyDelete(), "Privacy Cleanup");
        content.add(new ForceDelete(), "Force Delete");

        content.add(new HardDisk(), "Hard Disk");
        content.add(new SystemInfo(), "System Info");
        content.add(new Uptime(), "Uptime");
        content.add(new BlueScreen(), "Blue Screen");

        content.add(new DnsCheck(), "DNS Check");

        content.add(new Shutdown(), "Shutdown Timer");
        content.add(new VideoFinder(), "Video Finder");

        cardLayout.show(content, "empty");

        root.add(topBar, BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(new Color(40, 40, 40));
        topbar.setPreferredSize(new Dimension(0, 46));
        topbar.setBorder(new EmptyBorder(0, 16, 0, 16));

        JLabel programNameLabel = new JLabel("Goclean with Java");
        programNameLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 15));
        programNameLabel.setForeground(new Color(255, 255, 255));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 13));
        infoPanel.setBackground(new Color(40, 40, 40));

        JLabel idLabel = new JLabel("12245625");
        idLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        idLabel.setForeground(new Color(170, 170, 170));

        JLabel nameLabel = new JLabel("JuseongPark");
        nameLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        nameLabel.setForeground(new Color(204, 204, 204));

        JLabel codeLabel = new JLabel("CSE2107-003");
        codeLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        codeLabel.setForeground(new Color(170, 170, 170));

        infoPanel.add(idLabel);
        infoPanel.add(nameLabel);
        infoPanel.add(codeLabel);

        topbar.add(programNameLabel, BorderLayout.WEST);
        topbar.add(infoPanel, BorderLayout.EAST);

        return topbar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(new Color(245, 246, 250));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(12, 0, 12, 0));

        sidebar.add(makeCategoryLabel("  System"));
        sidebar.add(makeMenuButton("🔧", "Service Manager"));
        sidebar.add(makeMenuButton("🚀", "Startup Manager"));
        sidebar.add(makeMenuButton("📅", "Task Scheduler"));
        sidebar.add(makeMenuButton("📊", "Process Manager"));

        sidebar.add(Box.createVerticalStrut(8));

        sidebar.add(makeCategoryLabel("  Cleanup"));
        sidebar.add(makeMenuButton("🔒", "Privacy Cleanup"));
        sidebar.add(makeMenuButton("❌", "Force Delete"));

        sidebar.add(Box.createVerticalStrut(8));

        sidebar.add(makeCategoryLabel("  Info"));
        sidebar.add(makeMenuButton("💾", "Hard Disk"));
        sidebar.add(makeMenuButton("🖥️", "System Info"));
        sidebar.add(makeMenuButton("🕐", "Uptime"));
        sidebar.add(makeMenuButton("🛑", "Blue Screen"));

        sidebar.add(Box.createVerticalStrut(8));

        sidebar.add(makeCategoryLabel("  Network"));
        sidebar.add(makeMenuButton("🌐", "DNS Check"));

        sidebar.add(Box.createVerticalStrut(8));

        sidebar.add(makeCategoryLabel("  Etc"));
        sidebar.add(makeMenuButton("🎬", "Video Finder"));
        sidebar.add(makeMenuButton("🕘", "Shutdown Timer"));

        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JLabel makeCategoryLabel(String text) {
        JLabel label = new JLabel(text);

        label.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
        label.setForeground(new Color(150, 150, 160));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        label.setBorder(new EmptyBorder(4, 8, 2, 0));

        return label;
    }

    private JButton makeMenuButton(String emoji, String text) {
        JButton btn = new JButton();
        btn.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        btn.setForeground(new Color(50, 50, 60));
        btn.setBackground(new Color(245, 246, 250));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(emoji);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLabel.setBorder(new EmptyBorder(0, 20, 0, 8));

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        textLabel.setForeground(new Color(50, 50, 60));

        btn.add(iconLabel);
        btn.add(textLabel);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(230, 232, 245));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(245, 246, 250));
            }
        });

        btn.addActionListener(e -> cardLayout.show(content, text));

        return btn;
    }
}
