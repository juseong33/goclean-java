package panels.Info;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class SystemInfo extends BasePanel {
    // 시스템
    private JLabel cpuLabel, gpuLabel, soundLabel, installDateLabel;
    // 메모리
    private JLabel totalMemLabel, freeMemLabel;
    // 네트워크
    private JLabel ipLabel, macLabel;
    // 디스크
    private JList<String> driveList;
    private JLabel totalDiskLabel, usedDiskLabel, freeDiskLabel, percentLabel;
    private double usedRatio = 0.0;
    private JPanel progressBar;

    public SystemInfo() {
        super("시스템 정보", "현재 PC의 시스템, 메모리, 네트워크, 디스크 정보를 확인합니다.");
    }

    /*
     * [시스템 정보] 기능 UI 구현
     * 필요한 내용: {Cpu 정보, 그래픽카드, 사운드카드, 윈도우 설치일자}, {전체 메모리, 사용가능 메모리}, {사설 IP, 맥주소}, { *드라이브, 용량}
     */
    @Override
    protected void initUI() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 조회 버튼
        JButton refreshBtn = makeButton("정보 조회");
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(e -> loadInfo());
        contentPanel.add(refreshBtn);
        contentPanel.add(Box.createVerticalStrut(16));    // 카테고리와 박스 사이의 간격을 8로 설정

        // 시스템
        contentPanel.add(makeCategoryHeader("시스템"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel systemCard = makeCard();
        cpuLabel = addRow(systemCard, "CPU");
        gpuLabel = addRow(systemCard, "그래픽카드");
        soundLabel = addRow(systemCard, "사운드카드");
        installDateLabel = addRow(systemCard, "윈도우 설치일자");
        systemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, systemCard.getPreferredSize().height));
        contentPanel.add(systemCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // 메모리
        contentPanel.add(makeCategoryHeader("메모리"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel memCard = makeCard();
        totalMemLabel = addRow(memCard, "전체 메모리");
        freeMemLabel = addRow(memCard, "사용가능 메모리");
        contentPanel.add(memCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // 네트워크
        contentPanel.add(makeCategoryHeader("네트워크"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel netCard = makeCard();
        ipLabel = addRow(netCard, "사설 IP");
        macLabel = addRow(netCard, "맥주소");
        contentPanel.add(netCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // 디스크
        contentPanel.add(makeCategoryHeader("디스크"));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(makeDiskPanel());
        contentPanel.add(Box.createVerticalStrut(16));
    }

    private JPanel makeDiskPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        // 왼쪽: 드라이브 목록
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(Color.WHITE);
        listPanel.setPreferredSize(new Dimension(150, 0));

        driveList = new JList<>(new DefaultListModel<>());  // 이게.. 정보를 조회한 뒤에 addElement(), clear() 메소드를 이용해서 리스트를 수정하도록
        driveList.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        driveList.setBackground(new Color(248, 249, 252));
        driveList.setSelectionBackground(new Color(80, 140, 255));
        driveList.setSelectionForeground(Color.WHITE);
        driveList.setBorder(new EmptyBorder(4, 8, 4, 8));
        driveList.setFixedCellHeight(32);
        driveList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDriveInfo(driveList.getSelectedValue());    //getSelectedValue로 디스크 정보를 넘김
        });

        // 디스크가 많을 경우 스크롤로 조회
        JScrollPane scroll = new JScrollPane(driveList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        listPanel.add(scroll, BorderLayout.CENTER);

        // 오른쪽: 용량 정보 + 막대 바
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        JPanel infoCard = makeCard();
        totalDiskLabel = addRow(infoCard, "전체 용량");
        usedDiskLabel  = addRow(infoCard, "사용 중");
        freeDiskLabel  = addRow(infoCard, "남은 용량");
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int usedWidth = (int)(getWidth() * usedRatio);
                g2.setColor(new Color(80, 140, 255));
                g2.fillRoundRect(0, 0, usedWidth, getHeight(), 6, 6);
                g2.setColor(new Color(220, 220, 225));
                g2.fillRoundRect(usedWidth, 0, getWidth() - usedWidth, getHeight(), 6, 6);
            }
        };
        progressBar.setPreferredSize(new Dimension(0, 20));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        percentLabel = new JLabel("— %");
        percentLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        percentLabel.setForeground(new Color(80, 140, 255));
        percentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        percentLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        infoPanel.add(infoCard);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(progressBar);
        infoPanel.add(percentLabel);

        panel.add(listPanel, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    // 카테고리(시스템, 메모리, 네트워크, 디스크) 헤더 라벨
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
        //card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        return card;
    }

    // 카드 안에 키-값(정보를 불러오기 전에는 value가 -로 고정) 행 추가, 값 라벨 반환
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

    private void updateDriveInfo(String drive) {
        // TODO: CmdUtil로 해당 드라이브 용량 조회 후 라벨 및 progressBar 업데이트
        String used = CmdUtil.run("(Get-PSDrive " + drive + ").Used");
        String free = CmdUtil.run("(Get-PSDrive " + drive + ").Free");

        long usedBytes = Long.parseLong(used.trim());
        long freeBytes = Long.parseLong(free.trim());
        long totalBytes = usedBytes + freeBytes;

        totalDiskLabel.setText(CmdUtil.formatSize(totalBytes));
        usedDiskLabel.setText(CmdUtil.formatSize(usedBytes));
        freeDiskLabel.setText(CmdUtil.formatSize(freeBytes));

        usedRatio = (double) usedBytes / totalBytes;
        percentLabel.setText(String.format("%.1f%% 사용 됨", usedRatio * 100));
        progressBar.repaint();
    }

    // SwingWorker로 백그라운드에서 실행해서 UI가 멈추지 않게 구현
    private void loadInfo() {
        // TODO: CmdUtil로 데이터 조회 후 각 라벨에 setText()
        clearLog();
        log("▶ 시스템 정보 조회 중...");
        long startTime = System.currentTimeMillis();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                // 시스템
                String cpuCmd = "(Get-WmiObject Win32_Processor).Name";
                publish("▶ CPU 조회 중...  [" + cpuCmd + "]");
                cpuLabel.setText(CmdUtil.run(cpuCmd));

                String gpuCmd = "(Get-CimInstance Win32_VideoController).Name";
                publish("▶ 그래픽카드 조회 중...  [" + gpuCmd + "]");
                gpuLabel.setText(CmdUtil.run(gpuCmd));

                String soundCmd = "(Get-WmiObject Win32_SoundDevice | Select-Object -First 1).Name";
                publish("▶ 사운드카드 조회 중...  [" + soundCmd + "]");
                soundLabel.setText(CmdUtil.run(soundCmd));

                String installDateCmd = "(Get-WmiObject Win32_OperatingSystem).ConvertToDateTime((Get-WmiObject Win32_OperatingSystem).InstallDate).ToString('yyyy-MM-dd')";
                publish("▶ 윈도우 설치일자 조회 중...  [" + installDateCmd + "]");
                installDateLabel.setText(CmdUtil.run(installDateCmd));

                // 메모리
                String totalMemCmd = "(Get-WmiObject Win32_OperatingSystem).TotalVisibleMemorySize";
                publish("▶ 전체 메모리 조회 중...  [" + totalMemCmd + "]");
                long totalMemKBytes = Long.parseLong(CmdUtil.run(totalMemCmd)) * 1000;
                totalMemLabel.setText(CmdUtil.formatSize(totalMemKBytes));

                String freeMemCmd = "(Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory";
                publish("▶ 사용가능 메모리 조회 중...  [" + freeMemCmd + "]");
                long freeMemKBytes = Long.parseLong(CmdUtil.run(freeMemCmd)) * 1000;
                freeMemLabel.setText(CmdUtil.formatSize(freeMemKBytes));

                // 네트워크
                String ipCmd = "ipconfig | Select-String 'IPv4'";
                publish("▶ IP 조회 중...  [" + ipCmd + "]");
                String ipLine = CmdUtil.run(ipCmd);
                ipLabel.setText(ipLine.substring(ipLine.lastIndexOf(":") + 1).trim());

                String macCmd = "(Get-NetAdapter | Where-Object { $_.Status -eq 'Up' }).MacAddress";
                publish("▶ 맥주소 조회 중...  [" + macCmd + "]");
                macLabel.setText(CmdUtil.run(macCmd));

                // 디스크
                String diskCmd = "(Get-PSDrive -PSProvider FileSystem).Name";
                publish("▶ 디스크 목록 조회 중...  [" + diskCmd + "]");
                List<String> diskNames = CmdUtil.runLines(diskCmd);
                DefaultListModel<String> model = (DefaultListModel<String>) driveList.getModel();
                model.clear();
                for (String name : diskNames)
                    model.addElement(name);

                return null;
            }

            // 로그 출력 (publish한 문자열이 List로 담겨져있음)
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
