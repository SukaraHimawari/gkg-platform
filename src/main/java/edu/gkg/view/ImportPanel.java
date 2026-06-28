package edu.gkg.view;

import edu.gkg.common.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

public class ImportPanel extends JPanel {

    private final JProgressBar   progress    = new JProgressBar(0, 100);
    private final JLabel         currentFile = new JLabel("当前文件：—");
    private final JLabel         speedLabel  = new JLabel("已用时 00:00:00   预计剩余 —");
    private final StatCard       cardSuccess = new StatCard("成功",  Theme.SUCCESS, "✓").value("0");
    private final StatCard       cardSkip    = new StatCard("跳过",  Theme.WARN,    "!").value("0");
    private final StatCard       cardFail    = new StatCard("失败",  Theme.DANGER,  "✕").value("0");
    private final JTable         historyTable;
    private final DefaultTableModel historyModel;

    // 暴露给 ImportController 绑定
    public final JButton  runBtn    = UiUtil.primaryButton("▶ 开始导入");
    public final JButton  cleanBtn  = UiUtil.secondaryButton("数据清理");
    public final JButton  exportBtn = UiUtil.secondaryButton("结果导出");
    public final JButton  stopBtn   = UiUtil.secondaryButton("停止导入");
    public final DropZone drop;

    public ImportPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        JPanel left = new JPanel(new BorderLayout(0, Theme.SPACE_LG));
        left.setOpaque(false);

        Card dropCard = new Card("数据导入");
        drop = new DropZone(files -> { /* 由 ImportController.bind() 覆盖 */ });
        drop.browseButton().addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                currentFile.setText("当前文件：" + selected.getName());
            }
        });
        dropCard.body(drop);

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

        Card actionCard = new Card("操作");
        JPanel actBox = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
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

        JPanel leftCenter = new JPanel();
        leftCenter.setOpaque(false);
        leftCenter.setLayout(new BoxLayout(leftCenter, BoxLayout.Y_AXIS));
        dropCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        progCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionCard.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        historyModel = new DefaultTableModel(
                new Object[]{"时间", "数据源", "类型", "大小", "成功", "跳过", "失败", "耗时"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        UiUtil.styleTable(historyTable);
        seedHistory(historyModel);
        historyCard.body(new JScrollPane(historyTable));
        historyCard.setPreferredSize(new Dimension(0, 220));

        add(split,       BorderLayout.CENTER);
        add(historyCard, BorderLayout.SOUTH);
    }

    private void seedHistory(DefaultTableModel m) {
        m.addRow(new Object[]{"2026-06-09 16:11", "data.zip", "ZIP", "528 MB", "—", "—", "—", "已解压"});
        m.addRow(new Object[]{"等待真实数据接入", "—", "—", "—", "—", "—", "—", "—"});
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
