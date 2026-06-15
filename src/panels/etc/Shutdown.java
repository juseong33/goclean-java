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

    private Timer clockTimer;   // 현재시간 갱신용
    private Timer waitTimer;    // 지정 시간 대기용

    private static final String MEMO_PLACEHOLDER = "메모를 입력해 주세요";

    public Shutdown() {
        super("종료 타이머", "지정한 시간에 메모글을 띄우거나 컴퓨터를 종료합니다.");
    }

    /*
     * [종료 타이머] 기능 UI 구현
     * 필요한 내용: {현재시간}, {지정 시간 선택}, {메모 입력}, {메모글 띄우기 / 컴퓨터 종료하기 선택}, {실행 버튼}
     */
    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 현재시간
        contentPanel.add(makeCategoryHeader("현재시간"));
        contentPanel.add(Box.createVerticalStrut(6));

        JPanel currentTimeCard = makeCard();

        currentTimeLabel = new JLabel();
        currentTimeLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        currentTimeLabel.setForeground(new Color(50, 110, 200));

        currentTimeCard.add(currentTimeLabel);
        currentTimeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, currentTimeCard.getPreferredSize().height));
        contentPanel.add(currentTimeCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // 시간 설정
        contentPanel.add(makeCategoryHeader("종료/알림 시간 설정"));
        contentPanel.add(Box.createVerticalStrut(6));

        JPanel timeCard = makeCard();
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        timeRow.setBackground(new Color(248, 249, 252));
        timeRow.setBorder(new EmptyBorder(0, 0, 0, 0));

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"시간선택", "5분후", "10분후", "30분후", "1시간후"});
        typeCombo.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        timeSpinner = new JSpinner(new SpinnerDateModel());
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "a hh:mm:ss"));
        timeSpinner.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        timeSpinner.setPreferredSize(new Dimension(140, 28));

        // 선택한 항목만큼 현재시간에 더해서 시간 선택 스피너에 반영
        typeCombo.addActionListener(e -> {
            int minutes;
            switch ((String) typeCombo.getSelectedItem()) {
                case "5분후":
                    minutes = 5;
                    break;
                case "10분후":
                    minutes = 10;
                    break;
                case "30분후":
                    minutes = 30;
                    break;
                case "1시간후":
                    minutes = 60;
                    break;
                default:
                    minutes = 0;
            }

            // 선택된 분 값 계산(현재 시간 + 분)해서 timeSpinner에 set
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

        // 메모 입력
        contentPanel.add(makeCategoryHeader("메모 입력"));
        contentPanel.add(Box.createVerticalStrut(6));

        memoField = new JTextField(MEMO_PLACEHOLDER);
        memoField.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        memoField.setForeground(Color.GRAY);
        memoField.setAlignmentX(Component.LEFT_ALIGNMENT);
        memoField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        memoField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225)),
                new EmptyBorder(4, 8, 4, 8)
        ));

        memoField.addFocusListener(new java.awt.event.FocusAdapter() {
            //입력 받기 시작하면 줄을 비우고 검은색 글꼴로 설정
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (memoField.getText().equals(MEMO_PLACEHOLDER)) {
                    memoField.setText("");
                    memoField.setForeground(Color.BLACK);
                }
            }

            // Focus가 사라지면 초기값 설정
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

        // 지정한 시간에 (메모글 띄우기 / 컴퓨터 종료하기)
        contentPanel.add(makeCategoryHeader("지정한 시간에 수행할 동작"));
        contentPanel.add(Box.createVerticalStrut(6));

        JPanel optionCard = makeCard();
        JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 5));
        optionRow.setBackground(new Color(248, 249, 252));

        memoRadio = new JRadioButton("메모글 띄우기", true);
        memoRadio.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        memoRadio.setBackground(new Color(248, 249, 252));

        shutdownRadio = new JRadioButton("컴퓨터 종료하기");
        shutdownRadio.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        shutdownRadio.setBackground(new Color(248, 249, 252));

        ButtonGroup group = new ButtonGroup();      // 버튼이 하나만 선택되게 하기위해 묶음
        group.add(memoRadio);
        group.add(shutdownRadio);

        optionRow.add(memoRadio);
        optionRow.add(shutdownRadio);
        optionCard.add(optionRow);
        optionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, optionCard.getPreferredSize().height));
        contentPanel.add(optionCard);
        contentPanel.add(Box.createVerticalStrut(20));

        // 실행 버튼
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        runBtn = makeButton("실행");
        runBtn.addActionListener(e -> startTimer());
        btnPanel.add(runBtn);
        contentPanel.add(btnPanel);

        // 현재시간 1초마다 갱신
        clockTimer = new Timer(1000, e -> updateCurrentTime());
        clockTimer.start();
        updateCurrentTime();
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

    // 현재시간 라벨 갱신
    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("a hh시 mm분 ss초");
        currentTimeLabel.setText(sdf.format(new Date()));
    }

    // 지정 시간까지 1초마다 체크하다가 도달하면 동작 실행
    private void startTimer() {
        if (waitTimer != null && waitTimer.isRunning()) {
            log("▶ 이미 실행 중입니다.");
            return;
        }

        Date target = (Date) timeSpinner.getValue();
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(target);

        Calendar nowCal = Calendar.getInstance();
        // 시간만 의미 있기에 targetCal에 오늘 날짜로 설정
        targetCal.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));

        // 지정 시간이 현재시간보다 이전이면 다음날로 설정
        if (targetCal.before(nowCal))
            targetCal.add(Calendar.DATE, 1);

        SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm:ss");
        log("▶ " + sdf.format(targetCal.getTime()) + "에 작업이 예약되었습니다.");

        runBtn.setEnabled(false);
        waitTimer = new Timer(1000, e -> {
            Calendar now = Calendar.getInstance();
            if (!now.before(targetCal)) {   // 현재시간이 목표시간에 도달했거나 지났으면
                waitTimer.stop();           // 더 이상 체크 안 함
                runBtn.setEnabled(true);    // 버튼 다시 활성화
                executeAction();            // 메모 띄우기 or 종료 실행
            }
        });
        waitTimer.start();
    }

    // 지정한 시간에 메모글 띄우기 / 컴퓨터 종료하기 수행
    private void executeAction() {
        if (memoRadio.isSelected()) {
            String memo = memoField.getText().trim();
            if (memo.isEmpty() || memo.equals(MEMO_PLACEHOLDER))
                memo = "지정한 시간이 되었습니다.";

            log("▶ 메모글을 띄웁니다.");
            JOptionPane.showMessageDialog(this, memo, "메모", JOptionPane.INFORMATION_MESSAGE);
        } else {
            log("▶ 컴퓨터를 종료합니다.");
            CmdUtil.run("shutdown /s /t 0");
        }
    }
}
