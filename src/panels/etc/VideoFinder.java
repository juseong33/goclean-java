package panels.etc;

import panels.BasePanel;
import util.CmdUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class VideoFinder extends BasePanel {
    private JComboBox<String> fileTypeCombo;
    private JComboBox<String> driveCombo;
    private JButton searchBtn;
    private JTable table;
    private DefaultTableModel tableModel;

    private Timer searchingTimer;
    private int searchingDots;

    public VideoFinder() {
        super("파일 찾기", "다양한 확장자의 파일을 컴퓨터에서 찾습니다.");
    }

    @Override
    protected void initUI() {
        contentPanel.setLayout(new BorderLayout(0, 16));

        // 상단: 찾을 파일 종류 / 드라이브 선택 / 검색하기 버튼
        JPanel topPanel = new JPanel(new BorderLayout(16, 0));
        topPanel.setBackground(Color.WHITE);

        JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        optionRow.setBackground(Color.WHITE);

        // 찾을 파일 종류
        JLabel fileLabel = new JLabel("찾을 파일:");
        fileLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));

        fileTypeCombo = new JComboBox<>(new String[]{
                "동영상파일", "음악파일(2MB이상 크기)", "대용량 파일(50MB이상 크기)",
                "엑셀파일", "파워포인트", "Ms워드", "한글(Hwp)",
                "Pdf파일", "Psd파일", "Zip파일"
        });
        fileTypeCombo.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        // 드라이브 선택
        JLabel driveLabel = new JLabel("드라이브:");
        driveLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));

        List<String> driveItems = new ArrayList<>();
        driveItems.add("전체 드라이브");
        for (File root : File.listRoots())
            driveItems.add(root.getPath()); // 연결된 드라이브가 문자열로 반환되어 List에 추가

        driveCombo = new JComboBox<>(driveItems.toArray(new String[0]));
        driveCombo.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        optionRow.add(fileLabel);
        optionRow.add(fileTypeCombo);
        optionRow.add(driveLabel);
        optionRow.add(driveCombo);

        // 검색하기 버튼
        searchBtn = new JButton("검색하기");
        searchBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        searchBtn.setForeground(new Color(200, 30, 30));
        searchBtn.setBackground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.setPreferredSize(new Dimension(120, 40));
        searchBtn.addActionListener(e -> search());

        topPanel.add(optionRow, BorderLayout.CENTER);
        topPanel.add(searchBtn, BorderLayout.EAST);

        // 검색 결과 테이블
        String[] columns = {"경로", "파일명", "폴더 열기", "크기", "만든 날짜"};
        tableModel = new DefaultTableModel(columns, 0) {
            // 폴더 열기 버튼이 있는 열만 클릭 가능하게 함
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
        table.setGridColor(new Color(220, 220, 225));
        table.setSelectionBackground(new Color(230, 232, 245));
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225)));
        tableScroll.getVerticalScrollBar().setUnitIncrement(18);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(18);

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(tableScroll, BorderLayout.CENTER);

        // 전체 패널 스크롤은 없애고, 테이블 내부 스크롤만 사용하도록
        // 바깥 스크롤뷰의 높이에 맞춰 테이블 뷰포트 높이를 매번 재계산
        JScrollPane outerScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, contentPanel);
        if (outerScroll != null) {
            outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            outerScroll.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int padding = 40;   // contentPanel 상하 여백 합
                    int gap = 16;       // contentPanel BorderLayout 행간 여백
                    int bottomMargin = 24; // 테이블과 로그 영역 사이 여백 (좌우 여백과 동일)
                    int available = outerScroll.getHeight() - padding - gap - bottomMargin - topPanel.getPreferredSize().height;

                    if (available > 0) {
                        table.setPreferredScrollableViewportSize(new Dimension(0, available));
                        tableScroll.revalidate();
                    }
                }
            });
        }
    }

    // 검색 시작
    private void search() {
        String fileType = (String) fileTypeCombo.getSelectedItem();
        String drive = (String) driveCombo.getSelectedItem();

        List<File> roots = new ArrayList<>();
        if ("전체 드라이브".equals(drive))
            roots.addAll(Arrays.asList(File.listRoots()));
        else
            roots.add(new File(drive));

        tableModel.setRowCount(0);
        clearLog();
        searchBtn.setEnabled(false);

        // 검색 중임을 알 수 있도록 "검색 중." -> "검색 중.." -> "검색 중..." 형태로 갱신
        String searchingMessage = "▶ " + fileType + " 검색 중";
        log(searchingMessage);

        searchingDots = 0;
        searchingTimer = new Timer(1000, e -> {
            searchingDots = (searchingDots % 3) + 1;
            clearLog();
            log(searchingMessage + ".".repeat(searchingDots));
        });
        searchingTimer.start();

        new SearchWorker(roots, fileType).execute();
    }

    // 디렉토리를 재귀적으로 탐색하며 조건에 맞는 파일을 찾는 백그라운드 작업
    private class SearchWorker extends SwingWorker<Void, Object[]> {
        private final List<File> roots;
        private final String fileType;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        SearchWorker(List<File> roots, String fileType) {
            this.roots = roots;
            this.fileType = fileType;
        }

        @Override
        protected Void doInBackground() {
            for (File root : roots) {
                if (isCancelled())
                    break;
                searchDir(root);
            }
            return null;
        }

        private void searchDir(File dir) {
            if (isCancelled())
                return;

            File[] files = dir.listFiles();
            if (files == null)
                return;  // 접근 권한이 없는 폴더는 건너뜀

            for (File file : files) {
                if (isCancelled())
                    return;

                if (file.isDirectory())
                    searchDir(file);
                else if (matches(file))
                    publish(new Object[]{file.getParent(), file.getName(), "열기",
                            CmdUtil.formatSize(file.length()), dateFormat.format(new Date(file.lastModified()))});
            }
        }

        // 선택한 파일 종류 기준으로 파일이 조건에 맞는지 확인
        private boolean matches(File file) {
            String name = file.getName().toLowerCase();
            long size = file.length();

            switch (fileType) {
                case "동영상파일":
                    return hasExt(name, "mp4", "avi", "mkv", "mov", "wmv", "flv");
                case "음악파일(2MB이상 크기)":
                    return hasExt(name, "mp3", "wav", "flac", "aac", "ogg") && size >= 2L * 1024 * 1024;
                case "대용량 파일(50MB이상 크기)":
                    return size >= 50L * 1024 * 1024;
                case "엑셀파일":
                    return hasExt(name, "xls", "xlsx");
                case "파워포인트":
                    return hasExt(name, "ppt", "pptx");
                case "Ms워드":
                    return hasExt(name, "doc", "docx");
                case "한글(Hwp)":
                    return hasExt(name, "hwp");
                case "Pdf파일":
                    return hasExt(name, "pdf");
                case "Psd파일":
                    return hasExt(name, "psd");
                case "Zip파일":
                    return hasExt(name, "zip", "rar", "7z");
                default:
                    return false;
            }
        }

        private boolean hasExt(String name, String... exts) {
            for (String ext : exts)
                if (name.endsWith("." + ext))
                    return true;
            return false;
        }

        @Override
        protected void process(List<Object[]> chunks) {
            for (Object[] row : chunks)
                tableModel.addRow(row);
        }

        @Override
        protected void done() {
            searchingTimer.stop();
            clearLog();
            log("▶ 검색 완료 (" + tableModel.getRowCount() + "개 발견)");
            searchBtn.setEnabled(true);
        }
    }

    // "폴더 열기" 버튼 표시용
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setText("열기");
            setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    // "폴더 열기" 버튼 클릭 시 해당 파일이 있는 폴더를 탐색기로 열고 파일을 선택해줌
    private static class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String filePath;

        ButtonEditor() {
            super(new JCheckBox());
            button = new JButton("열기");
            button.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                fireEditingStopped();
                if (filePath != null)
                    CmdUtil.run("explorer.exe /select,\"" + filePath + "\"");   // select를 이용해 폴더에 파일이 선택 되게 설정
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            String folder = (String) table.getValueAt(row, 0);
            String name = (String) table.getValueAt(row, 1);
            filePath = folder + File.separator + name;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "열기";
        }
    }
}
