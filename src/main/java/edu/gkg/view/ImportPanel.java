package edu.gkg.view;

import edu.gkg.common.*;
import edu.gkg.common.SharedRecords.ProgressTick;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ImportPanel extends JPanel {

    private final JProgressBar   progress    = new JProgressBar(0, 100);
    private final JLabel         currentFile = new JLabel("当前文件：—");
    private final JLabel         speedLabel  = new JLabel("已用时 00:00:00   预计剩余 —");
    private final StatCard       cardSuccess = new StatCard("成功",  Theme.SUCCESS, "OK").value("0");
    private final StatCard       cardSkip    = new StatCard("跳过",  Theme.WARN,    "!").value("0");
    private final StatCard       cardFail    = new StatCard("失败",  Theme.DANGER,  "X").value("0");

    // ===== 在线下载 =====
    public final DatePickerField datePicker    = new DatePickerField(LocalDate.now().minusDays(1));
    public final JLabel          destPathLabel = UiUtil.mutedLabel("data/gkg-raw/YYYYMMDD/");
    public final JButton         downloadBtn   = UiUtil.primaryButton("下载该日期数据");
    public final JCheckBox       autoImportCb  = new JCheckBox("下载完成后自动导入", true);

    // 暴露给 ImportController 绑定
    public final JButton  runBtn    = UiUtil.primaryButton("开始导入");
    public final JButton  cleanBtn  = UiUtil.secondaryButton("数据清理");
    public final JButton  exportBtn = UiUtil.secondaryButton("结果导出");
    public final JButton  stopBtn   = UiUtil.secondaryButton("停止导入");
    public final JButton  clearBtn  = UiUtil.secondaryButton("清空数据库");
    public final DropZone drop;
    public final JTable   historyTable;
    public final DefaultTableModel historyModel;

    public ImportPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        datePicker.setOnChange(d -> {
            destPathLabel.setText("data/gkg-raw/" +
                    d.format(DateTimeFormatter.BASIC_ISO_DATE) + "/");
        });
        String initPath = "data/gkg-raw/" +
                datePicker.getSelectedDate().format(DateTimeFormatter.BASIC_ISO_DATE) + "/";
        destPathLabel.setText(initPath);

        autoImportCb.setOpaque(false);
        autoImportCb.setFont(Theme.FONT_DEFAULT);
        autoImportCb.setForeground(Theme.TEXT_PRIMARY);

        Card workbenchCard = new Card("数据工作台");
        JPanel topLine = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_MD, 0));
        topLine.setOpaque(false);
        topLine.add(UiUtil.mutedLabel("日期"));
        topLine.add(datePicker);
        topLine.add(UiUtil.mutedLabel("保存到"));
        topLine.add(destPathLabel);
        topLine.add(downloadBtn);
        topLine.add(autoImportCb);

        JPanel workbenchBody = new JPanel();
        workbenchBody.setOpaque(false);
        workbenchBody.setLayout(new BoxLayout(workbenchBody, BoxLayout.Y_AXIS));
        topLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        workbenchBody.add(topLine);
        workbenchCard.body(workbenchBody);

        Card dropCard = new Card("数据导入");
        drop = new DropZone(files -> { /* 由 ImportController.bind() 覆盖 */ });
        drop.setPreferredSize(new Dimension(0, 150));

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

        JPanel actionLine = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        actionLine.setOpaque(false);
        JPanel actBox = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        actBox.setOpaque(false);
        actBox.add(runBtn);
        actBox.add(cleanBtn);
        actBox.add(clearBtn);
        actBox.add(exportBtn);
        actionLine.add(actBox, BorderLayout.WEST);
        JPanel stopBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        stopBox.setOpaque(false);
        stopBox.add(stopBtn);
        actionLine.add(stopBox, BorderLayout.EAST);

        JPanel importStack = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        importStack.setOpaque(false);
        importStack.add(drop, BorderLayout.CENTER);
        JPanel importFooter = new JPanel();
        importFooter.setOpaque(false);
        importFooter.setLayout(new BoxLayout(importFooter, BoxLayout.Y_AXIS));
        progInner.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        importFooter.add(progInner);
        importFooter.add(Box.createVerticalStrut(Theme.SPACE_MD));
        importFooter.add(actionLine);
        importStack.add(importFooter, BorderLayout.SOUTH);
        dropCard.body(importStack);

        Card resultCard = new Card("导入结果汇总");
        JPanel stats = new JPanel(new GridLayout(3, 1, 0, Theme.SPACE_SM));
        stats.setOpaque(false);
        stats.add(cardSuccess);
        stats.add(cardSkip);
        stats.add(cardFail);
        resultCard.body(stats);
        resultCard.setPreferredSize(new Dimension(210, 0));

        Card historyCard = new Card("历史导入记录");
        historyTable = new JTable(
                historyModel = new DefaultTableModel(
                        new Object[]{"时间", "数据源", "类型", "大小", "成功", "跳过", "失败", "耗时"}, 0) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                });
        UiUtil.styleTable(historyTable);
        historyCard.body(new JScrollPane(historyTable));
        historyCard.setPreferredSize(new Dimension(0, 160));

        JPanel topRow = new JPanel(new BorderLayout(Theme.SPACE_LG, 0));
        topRow.setOpaque(false);
        topRow.add(dropCard, BorderLayout.CENTER);
        topRow.add(resultCard, BorderLayout.EAST);

        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        workbenchCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(topRow);
        main.add(Box.createVerticalStrut(Theme.SPACE_LG));
        main.add(workbenchCard);

        add(main, BorderLayout.CENTER);
        add(historyCard, BorderLayout.SOUTH);
    }

    public void addHistoryRow(String time, String name, String type,
                              String size, int ok, int skip, int fail, String elapsed) {
        historyModel.insertRow(0, new Object[]{time, name, type, size, ok, skip, fail, elapsed});
    }

    public JProgressBar progressBar()  { return progress; }
    public JLabel currentFileLabel()   { return currentFile; }
    public JLabel speedLabel()         { return speedLabel; }
    public StatCard successCard()      { return cardSuccess; }
    public StatCard skipCard()         { return cardSkip; }
    public StatCard failCard()         { return cardFail; }
}
