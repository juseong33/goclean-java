package panels.etc;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Shutdown extends BasePanel {
    private JLabel currentTimeLabel;
    private JSpinner timeSpinner;
    private JTextField memoField;
    private JRadioButton memoRadio, shutdownRadio;
    private JButton runBtn;

    private Timer clockTimer;
    private Timer waitTimer;

    private static final String MEMO_PLACEHOLDER = "Please enter a memo";

    public Shutdown() {
        super("Shutdown Timer", "Shows a memo or shuts down the computer at a specified time.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(makeCategoryHeader("Current Time"));
        contentPanel.add(Box.createVerticalStrut(6));

        JPanel currentTimeCard = makeCard();

        currentTimeLabel = new JLabel();
        currentTimeLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        currentTimeLabel.setForeground(new Color(50, 110, 200));

        currentTimeCard.add(currentTimeLabel);
        currentTimeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, currentTimeCard.getPreferredSize().height));
        contentPanel.add(currentTimeCard);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Shutdown/Alert Time"));
        contentPanel.add(Box.createVerticalStrut(6));

        JPanel timeCard = makeCard();
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        timeRow.setBackground(new Color(248, 249, 252));
        timeRow.setBorder(new EmptyBorder(0, 0, 0, 0));

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Custom", "In 5 min", "In 10 min", "In 30 min", "In 1 hour"});
        typeCombo.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));

        timeSpinner = new JSpinner(new SpinnerDateModel());
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "a hh:mm:ss"));
        timeSpinner.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        timeSpinner.setPreferredSize(new Dimension(140, 28));

        typeCombo.addActionListener(e -> {
            int minutes;
            switch ((String) typeCombo.getSelectedItem()) {
                case "In 5 min":
                    minutes = 5;
                    break;
                case "In 10 min":
                    minutes = 10;
                    break;
                case "In 30 min":
                    minutes = 30;
                    break;
                case "In 1 hour":
                    minutes = 60;
                    break;
                default:
                    minutes = 0;
            }

            if (minutes > 0) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, minutes);
                timeSpinner.setValue(cal.getTime());
            }
        });

        timeRow.add(typeCombo);
        timeRow.add(timeSpinner);
        timeCard.add(timeRow);
        timeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, timeCard.getPreferredSize().height));
        contentPanel.add(timeCard);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Memo Input"));
        contentPanel.add(Box.createVerticalStrut(6));

        memoField = new JTextField(MEMO_PLACEHOLDER);
        memoField.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        memoField.setForeground(Color.GRAY);
        memoField.setAlignmentX(Component.LEFT_ALIGNMENT);
        memoField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        memoField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225)),
                new EmptyBorder(4, 8, 4, 8)
        ));

        memoField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (memoField.getText().equals(MEMO_PLACEHOLDER)) {
                    memoField.setText("");
                    memoField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (memoField.getText().trim().isEmpty()) {
                    memoField.setText(MEMO_PLACEHOLDER);
                    memoField.setForeground(Color.GRAY);
                }
            }
        });

        contentPanel.add(memoField);
        contentPanel.add(Box.createVerticalStrut(16));

        contentPanel.add(makeCategoryHeader("Action at Specified Time"));
        contentPanel.add(Box.createVerticalStrut(6));

        JPanel optionCard = makeCard();
        JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 5));
        optionRow.setBackground(new Color(248, 249, 252));

        memoRadio = new JRadioButton("Show Memo", true);
        memoRadio.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        memoRadio.setBackground(new Color(248, 249, 252));

        shutdownRadio = new JRadioButton("Shut Down Computer");
        shutdownRadio.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        shutdownRadio.setBackground(new Color(248, 249, 252));

        ButtonGroup group = new ButtonGroup();
        group.add(memoRadio);
        group.add(shutdownRadio);

        optionRow.add(memoRadio);
        optionRow.add(shutdownRadio);
        optionCard.add(optionRow);
        optionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, optionCard.getPreferredSize().height));
        contentPanel.add(optionCard);
        contentPanel.add(Box.createVerticalStrut(20));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        runBtn = makeButton("Run");
        runBtn.addActionListener(e -> startTimer());
        btnPanel.add(runBtn);
        contentPanel.add(btnPanel);

        clockTimer = new Timer(1000, e -> updateCurrentTime());
        clockTimer.start();
        updateCurrentTime();
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

    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm:ss");
        currentTimeLabel.setText(sdf.format(new Date()));
    }

    private void startTimer() {
        if (waitTimer != null && waitTimer.isRunning()) {
            log("▶ Already running.");
            return;
        }

        Date target = (Date) timeSpinner.getValue();
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(target);

        Calendar nowCal = Calendar.getInstance();
        targetCal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));

        if (targetCal.before(nowCal))
            targetCal.add(Calendar.DATE, 1);

        SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm:ss");
        log("▶ Task scheduled at " + sdf.format(targetCal.getTime()) + ".");

        runBtn.setEnabled(false);
        waitTimer = new Timer(1000, e -> {
            Calendar now = Calendar.getInstance();
            if (!now.before(targetCal)) {
                waitTimer.stop();
                runBtn.setEnabled(true);
                executeAction();
            }
        });
        waitTimer.start();
    }

    private void executeAction() {
        if (memoRadio.isSelected()) {
            String memo = memoField.getText().trim();
            if (memo.isEmpty() || memo.equals(MEMO_PLACEHOLDER))
                memo = "The specified time has arrived.";

            log("▶ Showing memo.");
            JOptionPane.showMessageDialog(this, memo, "Memo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            log("▶ Shutting down the computer.");
            CmdUtil.run("shutdown /s /t 0");
        }
    }
}
