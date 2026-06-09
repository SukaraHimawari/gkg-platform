package edu.gkg.common;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * 圆角白底卡片容器。可选标题区。
 * 用法：new Card("标题").body(component) 或直接 setLayout 在 card 上加东西。
 */
public class Card extends JPanel {

    private final String title;
    private final JLabel titleLabel;
    private final JPanel body;

    public Card() { this(null); }

    public Card(String title) {
        super(new BorderLayout());
        this.title = title;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        if (title != null) {
            titleLabel = new JLabel(title);
            titleLabel.setFont(Theme.FONT_SECTION);
            titleLabel.setForeground(Theme.TEXT_PRIMARY);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SPACE_MD, 0));
            add(titleLabel, BorderLayout.NORTH);
        } else {
            titleLabel = null;
        }
        body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
    }

    public Card body(Component c) {
        body.removeAll();
        body.add(c, BorderLayout.CENTER);
        return this;
    }

    public JPanel bodyPanel() { return body; }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.BG_CARD);
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                Theme.RADIUS_MD, Theme.RADIUS_MD));
        g.setColor(Theme.BORDER_LIGHT);
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1,
                Theme.RADIUS_MD, Theme.RADIUS_MD));
        g.dispose();
        super.paintComponent(g0);
    }
}
