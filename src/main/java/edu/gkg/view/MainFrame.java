package edu.gkg.view;

import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class MainFrame extends JFrame {

    private final JLabel statusLeft  = makeFooterLabel("数据库记录数：12,458,721 条");
    private final JLabel statusMid   = makeFooterLabel("图谱实体数：3,245,881 个");
    private final JLabel statusRight = makeFooterLabel("● 就绪");

    public MainFrame() {
        super("GKG 全球新闻语义分析与主题追踪平台");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 860);
        setLocationRelativeTo(null);

        setJMenuBar(buildMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_DEFAULT);
        tabs.putClientProperty("JTabbedPane.tabType",      "underlined");
        tabs.putClientProperty("JTabbedPane.tabAreaInsets","12,16,0,16");
        tabs.addTab("  数据管理  ", new ImportPanel());
        tabs.addTab("  查询检索  ", new QueryPanel());
        tabs.addTab("  挖掘分析  ", new AnalysisPanel());
        tabs.addTab("  关于  ",     buildAboutPanel());

        setLayout(new BorderLayout());
        add(buildHeaderBar(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        getContentPane().setBackground(Theme.BG_APP);
    }

    private JComponent buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel logo = new JLabel("GKG");
        logo.setOpaque(true);
        logo.setBackground(Theme.BRAND);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 14));
        logo.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        left.add(logo);

        JLabel title = new JLabel("全球新闻语义分析与主题追踪平台");
        title.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 15));
        title.setForeground(Theme.TEXT_PRIMARY);
        left.add(title);

        bar.add(left, BorderLayout.WEST);
        return bar;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT));

        JMenu file = new JMenu("文件");
        file.add(new JMenuItem("导入 GKG..."));
        file.add(new JMenuItem("导出结果..."));
        file.addSeparator();
        JMenuItem exit = new JMenuItem("退出");
        exit.addActionListener(e -> dispose());
        file.add(exit);

        JMenu view = new JMenu("视图");
        view.add(new JMenuItem("数据管理"));
        view.add(new JMenuItem("查询检索"));
        view.add(new JMenuItem("挖掘分析"));

        JMenu help = new JMenu("帮助");
        help.add(new JMenuItem("关于"));

        bar.add(file); bar.add(view); bar.add(help);
        return bar;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        left.setOpaque(false);
        left.add(statusLeft);
        left.add(statusMid);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(statusRight);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JLabel makeFooterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_LABEL);
        l.setForeground(Theme.TEXT_SECONDARY);
        return l;
    }

    private JComponent buildAboutPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_APP);
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel hero = new JLabel("GKG 全球新闻语义分析与主题追踪平台");
        hero.setFont(Theme.FONT_HERO);
        hero.setForeground(Theme.TEXT_PRIMARY);
        hero.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("分析 GDELT 全球新闻知识图谱数据，洞察全球事件与趋势");
        subtitle.setFont(Theme.FONT_DEFAULT);
        subtitle.setForeground(Theme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 24, 0));

        String html = """
                <html>
                <table cellpadding="6" style="font-family:微软雅黑;font-size:13px;">
                  <tr>
                    <td style="color:#6B7280;vertical-align:top;"><b>技术栈</b></td>
                    <td style="color:#1F2937;">Java 17 &nbsp;+&nbsp; Swing &nbsp;+&nbsp; FlatLaf &nbsp;+&nbsp; SQLite &nbsp;+&nbsp; JFreeChart &nbsp;+&nbsp; Weka</td>
                  </tr>
                  <tr>
                    <td style="color:#6B7280;vertical-align:top;"><b>团队</b></td>
                    <td style="color:#1F2937;">
                      A · 界面 / 查询 / 导出 / PageRank / 情感分析<br>
                      B · 数据 / 解析 / 共现 / 聚类
                    </td>
                  </tr>
                  <tr>
                    <td style="color:#6B7280;vertical-align:top;"><b>版本</b></td>
                    <td style="color:#1F2937;">v1.0 &nbsp;&nbsp; 2026 / 06</td>
                  </tr>
                </table>
                </html>
                """;
        JLabel body = new JLabel(html);
        body.setFont(Theme.FONT_DEFAULT);
        body.setForeground(Theme.TEXT_PRIMARY);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(hero);
        inner.add(subtitle);
        inner.add(body);

        p.add(inner);
        return p;
    }

    public void setStatus(String text) { statusRight.setText("● " + text); }
}
