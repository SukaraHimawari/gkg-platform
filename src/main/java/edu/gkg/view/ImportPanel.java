package edu.gkg.view;

import edu.gkg.common.*;
import edu.gkg.common.DatePickerField;
import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.SharedRecords.ImportResult;
import edu.gkg.common.SharedRecords.ProgressTick;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ImportPanel extends JPanel {

    private final JProgressBar progress  = new JProgressBar(0, 100);
    private final JLabel       currentFile = new JLabel("当前文件：—");
    private final JLabel       speedLabel  = new JLabel("已用时 00:00:00   预计剩余 —");
    private final StatCard     cardSuccess = new StatCard("成功",  Theme.SUCCESS, "✓").value("0");
    private final StatCard     cardSkip    = new StatCard("跳过",  Theme.WARN,    "!").value("0");
    private final StatCard     cardFail    = new StatCard("失败",  Theme.DANGER,  "✕").value("0");
    private final JTable       historyTable;
    private final DropZone     dropZone;
    private final JButton      runBtn   = UiUtil.primaryButton("▶ 开始导入");
    private final JButton      cleanBtn = UiUtil.secondaryButton("数据清理");
    private final JButton      exportBtn= UiUtil.secondaryButton("结果导出");
    private final JButton      stopBtn  = UiUtil.secondaryButton("停止导入");

    // ===== 在线下载控件 =====
    private final DatePickerField datePicker   = new DatePickerField(LocalDate.now().minusDays(1));
    private final JLabel          destPathLabel= UiUtil.mutedLabel("data/gkg-raw/YYYYMMDD/");
    private final JButton         downloadBtn  = UiUtil.primaryButton("▼ 下载该日期数据");
    private final JCheckBox       autoImportCb = new JCheckBox("下载完成后自动导入", true);

    /** Controller 在外面通过 setOnFilesChosen 来收文件 */
    private Consumer<List<File>> onFilesChosen;
    private final List<File> pendingFiles = new ArrayList<>();

    public ImportPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        JPanel left = new JPanel(new BorderLayout(0, Theme.SPACE_LG));
        left.setOpaque(false);

        Card dropCard = new Card("数据导入");
        dropZone = new DropZone(files -> handleFilesChosen(files));
        dropZone.browseButton().addActionListener(e -> openChooser());
        dropCard.body(dropZone);

        Card progCard = new Card("导入进度");
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(0, 18));
        progress.setForeground(Theme.BRAND);
        currentFile.setFont(Theme.FONT_LABEL);
        currentFile.setForeground(Theme.TEXT_SECONDARY);
        speedLabel.setFont(Theme.FONT_LABEL);
        speedLabel.setForeground(Theme.TEXT_MUTED);

        JPanel progInner = new JPanel();
        progInner.setOpaque(false);
        progInner.setLayout(new BoxLayout(progInner, BoxLayout.Y_AXIS));
        currentFile.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progInner.add(currentFile);
        progInner.add(Box.createVerticalStrut(Theme.SPACE_SM));
        progInner.add(progress);
        progInner.add(Box.createVerticalStrut(Theme.SPACE_SM));
        progInner.add(speedLabel);
        progCard.body(progInner);

        Card actionCard  = new Card("操作");
        JPanel actBox    = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        actBox.setOpaque(false);
        actBox.add(runBtn); actBox.add(cleanBtn); actBox.add(exportBtn);
        JPanel actRow = new JPanel(new BorderLayout());
        actRow.setOpaque(false);
        actRow.add(actBox, BorderLayout.WEST);
        JPanel stopBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        stopBox.setOpaque(false);
        stopBox.add(stopBtn);
        actRow.add(stopBox, BorderLayout.EAST);
        actionCard.body(actRow);

        // ===== 在线下载 Card =====
        Card downloadCard = new Card("在线下载 GKG 数据");
        datePicker.setOnChange(d -> refreshDestLabel());
        refreshDestLabel();

        autoImportCb.setOpaque(false);
        autoImportCb.setFont(Theme.FONT_DEFAULT);
        autoImportCb.setForeground(Theme.TEXT_PRIMARY);

        JPanel dlRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        dlRow1.setOpaque(false);
        dlRow1.add(UiUtil.mutedLabel("选择日期"));
        dlRow1.add(datePicker);

        JPanel dlRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        dlRow2.setOpaque(false);
        dlRow2.add(UiUtil.mutedLabel("保存到"));
        dlRow2.add(destPathLabel);

        JPanel dlRow3 = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        dlRow3.setOpaque(false);
        dlRow3.add(downloadBtn);
        dlRow3.add(autoImportCb);

        JPanel dlBody = new JPanel();
        dlBody.setOpaque(false);
        dlBody.setLayout(new BoxLayout(dlBody, BoxLayout.Y_AXIS));
        dlRow1.setAlignmentX(Component.LEFT_ALIGNMENT);
        dlRow2.setAlignmentX(Component.LEFT_ALIGNMENT);
        dlRow3.setAlignmentX(Component.LEFT_ALIGNMENT);
        dlBody.add(dlRow1);
        dlBody.add(Box.createVerticalStrut(Theme.SPACE_SM));
        dlBody.add(dlRow2);
        dlBody.add(Box.createVerticalStrut(Theme.SPACE_MD));
        dlBody.add(dlRow3);
        downloadCard.body(dlBody);

        JPanel leftCenter = new JPanel();
        leftCenter.setOpaque(false);
        leftCenter.setLayout(new BoxLayout(leftCenter, BoxLayout.Y_AXIS));
        downloadCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        progCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftCenter.add(downloadCard);
        leftCenter.add(Box.createVerticalStrut(Theme.SPACE_LG));
        leftCenter.add(dropCard);
        leftCenter.add(Box.createVerticalStrut(Theme.SPACE_LG));
        leftCenter.add(progCard);
        leftCenter.add(Box.createVerticalStrut(Theme.SPACE_LG));
        leftCenter.add(actionCard);
        left.add(leftCenter, BorderLayout.NORTH);

        Card resultCard = new Card("导入结果汇总");
        JPanel stats = new JPanel(new GridLayout(3, 1, 0, Theme.SPACE_MD));
        stats.setOpaque(false);
        stats.add(cardSuccess);
        stats.add(cardSkip);
        stats.add(cardFail);
        resultCard.body(stats);
        resultCard.setPreferredSize(new Dimension(280, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, resultCard);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(Theme.SPACE_LG);
        split.setResizeWeight(0.7);
        split.setContinuousLayout(true);

        Card historyCard = new Card("历史导入记录");
        historyTable = new JTable(new DefaultTableModel(
                new Object[]{"时间", "数据源", "类型", "大小", "成功", "跳过", "失败", "耗时"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        UiUtil.styleTable(historyTable);
        seedHistory((DefaultTableModel) historyTable.getModel());
        historyCard.body(new JScrollPane(historyTable));
        historyCard.setPreferredSize(new Dimension(0, 220));

        add(split,       BorderLayout.CENTER);
        add(historyCard, BorderLayout.SOUTH);
    }

    // ---------- 文件选择 ----------

    private void openChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] arr = fc.getSelectedFiles();
            if (arr != null && arr.length > 0) {
                handleFilesChosen(List.of(arr));
            } else if (fc.getSelectedFile() != null) {
                handleFilesChosen(List.of(fc.getSelectedFile()));
            }
        }
    }

    private void handleFilesChosen(List<File> files) {
        pendingFiles.clear();
        pendingFiles.addAll(files);
        if (!files.isEmpty()) {
            String s = files.size() == 1 ? files.get(0).getName() : files.size() + " 个文件 / 目录";
            currentFile.setText("待导入：" + s);
        }
        if (onFilesChosen != null) onFilesChosen.accept(files);
    }

    private void seedHistory(DefaultTableModel m) {
        m.addRow(new Object[]{"2026-06-09 16:11", "data.zip", "ZIP", "528 MB", "—", "—", "—", "已解压"});
        m.addRow(new Object[]{"等待真实数据接入", "—", "—", "—", "—", "—", "—", "—"});
    }

    private void refreshDestLabel() {
        LocalDate d = selectedDate();
        String dir = "data/gkg-raw/" + d.format(DateTimeFormatter.BASIC_ISO_DATE) + "/";
        destPathLabel.setText(dir);
    }

    public void appendDownloadHistory(String date, DownloadResult r) {
        DefaultTableModel m = (DefaultTableModel) historyTable.getModel();
        if (m.getRowCount() > 0
                && "等待真实数据接入".equals(m.getValueAt(m.getRowCount() - 1, 0))) {
            m.removeRow(m.getRowCount() - 1);
        }
        NumberFormat nf = NumberFormat.getIntegerInstance();
        m.insertRow(0, new Object[]{
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                "GDELT 在线 " + date, "在线下载", "~350 MB",
                nf.format(r.success()), "—", nf.format(r.failed()),
                formatElapsed(r.elapsedMs())
        });
    }

    public void appendHistory(String src, String type, String size, ImportResult r) {
        DefaultTableModel m = (DefaultTableModel) historyTable.getModel();
        if (m.getRowCount() > 0
                && "等待真实数据接入".equals(m.getValueAt(m.getRowCount() - 1, 0))) {
            m.removeRow(m.getRowCount() - 1);
        }
        NumberFormat nf = NumberFormat.getIntegerInstance();
        m.insertRow(0, new Object[]{
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                src, type, size,
                nf.format(r.success()), nf.format(r.skipped()), nf.format(r.failed()),
                formatElapsed(r.elapsedMs())
        });
    }

    private static String formatElapsed(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    public void applyProgress(ProgressTick tick) {
        progress.setValue(tick.percent());
        progress.setString(tick.percent() + "%");
        currentFile.setText("当前文件：" + tick.currentFile());
        speedLabel.setText(tick.message());
    }

    public void applyResult(ImportResult r) {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        cardSuccess.value(nf.format(r.success()));
        cardSkip.value(nf.format(r.skipped()));
        cardFail.value(nf.format(r.failed()));
        speedLabel.setText("完成，用时 " + formatElapsed(r.elapsedMs()));
    }

    public void resetProgress() {
        progress.setValue(0);
        progress.setString("0%");
        currentFile.setText("当前文件：—");
        speedLabel.setText("已用时 00:00:00   预计剩余 —");
    }

    // ---------- 暴露给 Controller ----------

    public JButton runButton()    { return runBtn; }
    public JButton cleanButton()  { return cleanBtn; }
    public JButton exportButton() { return exportBtn; }
    public JButton stopButton()   { return stopBtn; }

    public JProgressBar progressBar() { return progress; }
    public JLabel currentFileLabel()  { return currentFile; }
    public JLabel speedLabel()        { return speedLabel; }
    public StatCard successCard()     { return cardSuccess; }
    public StatCard skipCard()        { return cardSkip; }
    public StatCard failCard()        { return cardFail; }
    public DropZone dropZone()        { return dropZone; }

    public List<File> pendingFiles()  { return List.copyOf(pendingFiles); }
    public void clearPending()        { pendingFiles.clear(); }

    public void setOnFilesChosen(Consumer<List<File>> sink) { this.onFilesChosen = sink; }

    // ===== 在线下载控件 getter =====

    public JButton   downloadButton()  { return downloadBtn; }
    public boolean   autoImport()      { return autoImportCb.isSelected(); }
    public LocalDate selectedDate() {
        return datePicker.getSelectedDate();
    }
}
