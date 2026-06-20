package panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public abstract class BasePanel extends JPanel {
    public JPanel contentPanel;
    public JTextArea logArea;

    public BasePanel(String title, String description) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(new Color(250, 250, 252));
        header.setBorder(new EmptyBorder(20, 24, 16, 24));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        titleLabel.setForeground(new Color(30, 30, 30));

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        descLabel.setForeground(new Color(120, 120, 120));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(descLabel);

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(220, 220, 220));

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.add(header, BorderLayout.CENTER);
        headerWrapper.add(separator, BorderLayout.SOUTH);

        contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JScrollPane contentScroll = new JScrollPane(contentPanel);
        contentScroll.setBorder(null);
        contentScroll.getVerticalScrollBar().setUnitIncrement(8);

        logArea = new JTextArea(4, 0);
        logArea.setEditable(false);
        logArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(180, 255, 180));
        logArea.setMargin(new Insets(8, 12, 8, 12));
        logArea.setText("▶ Executed commands will be shown here.\n");

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

    protected abstract void initUI();

    protected void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    protected void clearLog() {
        logArea.setText("");
    }

    protected JButton makeButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        btn.setBackground(new Color(50, 110, 200));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 34));
        return btn;
    }

    protected JButton makeDangerButton(String text) {
        JButton btn = makeButton(text);
        btn.setBackground(new Color(200, 60, 60));
        return btn;
    }
}
