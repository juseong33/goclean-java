import panels.SystemInfo;

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
        setMinimumSize(new Dimension(850, 720));
        setLocationRelativeTo(null);

        buildUI();

//      setVisible(true); main 함수에서 호출하도록 변경
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        // 상단 바
        JPanel topBar = buildTopBar();

        // 왼쪽 사이드바
        JPanel sidebar = buildSidebar();

        // 오른쪽 컨텐츠
        // 새 창(JFrame)을 띄워서 각 기능들을 구현보다 CardLayout(패널만 교체)로 구현
        cardLayout = new CardLayout();
        content = new JPanel(cardLayout);
        content.setBackground(Color.WHITE);

        // 빈 화면 (시작 화면)
        JPanel emptyPanel = new JPanel();
        emptyPanel.setBackground(Color.WHITE);
        content.add(emptyPanel, "empty");

        // 각 기능 패널들 등록
        content.add(new SystemInfo(), "시스템 정보");

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

        // 프로그램 이름 (좌측)
        JLabel programNameLabel = new JLabel("Goclean with Java");
        programNameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        programNameLabel.setForeground(new Color(255, 255, 255));

        // 학번 - 이름 - 학수번호 (우측)
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 13));
        infoPanel.setBackground(new Color(40, 40, 40));

        JLabel idLabel = new JLabel("12245625");
        idLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        idLabel.setForeground(new Color(170, 170, 170));

        JLabel nameLabel = new JLabel("JuseongPark");
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        nameLabel.setForeground(new Color(204, 204, 204));

        JLabel codeLabel = new JLabel("CSE2107-003");
        codeLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
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

        // 시스템
        sidebar.add(makeCategoryLabel("  시스템"));
        sidebar.add(makeMenuButton("\uD83D\uDD27", "서비스 관리"));  //이모지 크기 때문에 text 여백이 생기는 문제가 있다..
        sidebar.add(makeMenuButton("🚀", "시작 프로그램 관리"));
        sidebar.add(makeMenuButton("📅", "작업 스케줄러 관리"));
        sidebar.add(makeMenuButton("📊", "프로세스 관리"));

        sidebar.add(Box.createVerticalStrut(8));

        // 청소
        sidebar.add(makeCategoryLabel("  청소"));
        sidebar.add(makeMenuButton("🗑️", "임시파일 삭제"));
        sidebar.add(makeMenuButton("🔒", "개인정보 삭제"));
        sidebar.add(makeMenuButton("❌", "파일 강제 삭제"));

        sidebar.add(Box.createVerticalStrut(8));

        // 정보
        sidebar.add(makeCategoryLabel("  정보"));
        sidebar.add(makeMenuButton("💾", "하드디스크"));
        sidebar.add(makeMenuButton("🖥️", "시스템 정보"));
        sidebar.add(makeMenuButton("\uD83D\uDD50", "컴퓨터 사용시간"));
        sidebar.add(makeMenuButton("🛑", "블루스크린 로그"));

        sidebar.add(Box.createVerticalStrut(8));

        // 네트워크
        sidebar.add(makeCategoryLabel("  네트워크"));
        sidebar.add(makeMenuButton("🌐", "DNS 체크 / 초기화"));

        sidebar.add(Box.createVerticalStrut(8));

        // 기타
        sidebar.add(makeCategoryLabel("  기타"));
        sidebar.add(makeMenuButton("🎬", "동영상 파일 찾기"));
        sidebar.add(makeMenuButton("🕘", "종료 타이머"));

        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    // 카테고리 헤더 라벨 (클릭 안 됨)
    private JLabel makeCategoryLabel(String text) {
        JLabel label = new JLabel(text);

        label.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        label.setForeground(new Color(150, 150, 160));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        label.setBorder(new EmptyBorder(4, 8, 2, 0));

        return label;
    }

    // 메뉴 버튼 (클릭 됨)
    // 텍스트에 이모지랑 글씨를 같이 넣으면 폰트를 하나로밖에 못 써서 이모지가 깨지는 현상 있음
    // 이모지는 Segoe UI Emoji 폰트를 따로 지정하여 이모지, 텍스트 라벨 분리한 뒤 btn에 결합
    private JButton makeMenuButton(String emoji, String text) {
        JButton btn = new JButton();
        btn.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        btn.setForeground(new Color(50, 50, 60));
        btn.setBackground(new Color(245, 246, 250));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 이모지 라벨
        JLabel iconLabel = new JLabel(emoji);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLabel.setBorder(new EmptyBorder(0, 20, 0, 8));

        // 텍스트 라벨
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
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
