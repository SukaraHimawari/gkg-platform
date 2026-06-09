package edu.gkg.common;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * 统计卡片：上方小标签 + 中间大数字 + 右上角圆形图标徽章。
 *
 * mockup 里"成功 2,158,742"、"跳过 312,456"、"新闻量 48,762" 都用这个。
 */
public class StatCard extends JPanel {

    private final String label;
    private final Color accent;
    private final String iconChar;
    private String value = "-";
    private String hint  = null;

    public StatCard(String label, Color accent, String iconChar) {
        super(new BorderLayout());
        this.label = label;
        this.accent = accent;
        this.iconChar = iconChar;
        setOpaque(false);
        setPreferredSize(new Dimension(180, 96));
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_MD, Theme.SPACE_LG, Theme.SPACE_MD, Theme.SPACE_LG));
    }

    public StatCard value(String v) { this.value = v; repaint(); return this; }
    public StatCard hint(String h)  { this.hint = h; repaint(); return this; }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        g.setColor(Theme.BG_CARD);
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, Theme.RADIUS_MD, Theme.RADIUS_MD));
        g.setColor(Theme.BORDER_LIGHT);
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1,
                Theme.RADIUS_MD, Theme.RADIUS_MD));

        // 左侧文本：label + value + hint
        g.setColor(Theme.TEXT_SECONDARY);
        g.setFont(Theme.FONT_LABEL);
        g.drawString(label, Theme.SPACE_LG, Theme.SPACE_LG + 4);

        g.setColor(accent);
        g.setFont(Theme.FONT_BIG_NUMBER);
        g.drawString(value, Theme.SPACE_LG, Theme.SPACE_LG + 40);

        if (hint != null) {
            g.setColor(Theme.TEXT_MUTED);
            g.setFont(Theme.FONT_LABEL);
            g.drawString(hint, Theme.SPACE_LG, h - Theme.SPACE_SM - 2);
        }

        // 右上角圆形徽章
        int badge = 32;
        int bx = w - Theme.SPACE_LG - badge;
        int by = Theme.SPACE_MD;
        g.setColor(softenWithWhite(accent, 0.85f));
        g.fillOval(bx, by, badge, badge);
        g.setColor(accent);
        g.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(iconChar);
        g.drawString(iconChar, bx + (badge - sw) / 2, by + badge - fm.getDescent() - 8);

        g.dispose();
    }

    private static Color softenWithWhite(Color c, float t) {
        int r = Math.round(c.getRed()   + (255 - c.getRed())   * t);
        int gn= Math.round(c.getGreen() + (255 - c.getGreen()) * t);
        int b = Math.round(c.getBlue()  + (255 - c.getBlue())  * t);
        return new Color(r, gn, b);
    }
}
