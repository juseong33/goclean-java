package panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// 추상 클래스로 선언하여 기본 틀을 구현한 뒤, 각 기능들에서 상속받아서 구현하기
public abstract class BasePanel extends JPanel {
    public JPanel contentPanel; //기능 영역
    public JTextArea logArea;   //로그 영억

    public BasePanel(String title, String description) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 상단 헤더 (기능명, 설명)
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));  // Y_AXIS: 위 -> 아래로 정렬, X_AXIS: 좌 -> 우로 정렬
        header.setBackground(new Color(250, 250, 252));
        header.setBorder(new EmptyBorder(20, 24, 16, 24));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        titleLabel.setForeground(new Color(30, 30, 30));

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        descLabel.setForeground(new Color(120, 120, 120));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(descLabel);

        // 헤더 아래 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(220, 220, 220));

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.add(header, BorderLayout.CENTER);
        headerWrapper.add(separator, BorderLayout.SOUTH);

        // 컨텐츠 영역
        contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JScrollPane contentScroll = new JScrollPane(contentPanel);
        contentScroll.setBorder(null);

        // 하단 로그창
        logArea = new JTextArea(4, 0);
        logArea.setEditable(false);
        logArea.setFont(new Font("맑은 고딕", Font.PLAIN, 11)); // Consolas 폰트로 설정하면 글자가 깨짐
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(180, 255, 180));
        logArea.setMargin(new Insets(8, 12, 8, 12));
        logArea.setText("▶ 실행된 명령어가 여기에 표시됩니다.\n");

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60))
        );
        logScroll.setPreferredSize(new Dimension(0, 110));

        add(headerWrapper, BorderLayout.NORTH);
        add(contentScroll, BorderLayout.CENTER);
        add(logScroll, BorderLayout.SOUTH);

        initUI();
    }

    // 각 패널에서 구현하기
    protected abstract void initUI();

    // 로그창에 메세지 출력
    protected void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // 로그창 초기화
    protected void clearLog() {
        logArea.setText("");
    }

    // 공통 버튼 스타일
    protected JButton makeButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        btn.setBackground(new Color(50, 110, 200));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 34));
        return btn;
    }

    // 위험 버튼 (삭제, 강제종료 등)
    protected JButton makeDangerButton(String text) {
        JButton btn = makeButton(text);
        btn.setBackground(new Color(200, 60, 60));
        return btn;
    }
}