package edu.gkg.view.chart;

import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class ClusterResultsPanel extends JPanel {

    private final JPanel listPanel = new JPanel();

    public ClusterResultsPanel() {
        super(new BorderLayout());
        setOpaque(false);

        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        render(List.of());
    }

    public void render(List<String> clusters) {
        listPanel.removeAll();
        if (clusters == null || clusters.isEmpty()) {
            listPanel.add(emptyState());
        } else {
            for (int i = 0; i < clusters.size(); i++) {
                ClusterCard card = new ClusterCard(i + 1, clusters.get(i));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                listPanel.add(card);
                listPanel.add(Box.createVerticalStrut(Theme.SPACE_MD));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    public int getClusterCardCount() {
        int count = 0;
        for (Component component : listPanel.getComponents()) {
            if (component instanceof ClusterCard) count++;
        }
        return count;
    }

    private JComponent emptyState() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_LG, 0, Theme.SPACE_LG));
        JLabel label = UiUtil.mutedLabel("运行 K-Means 后，聚类结果会显示在这里。");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    private static final class ClusterCard extends JPanel {
        private final int ordinal;
        private final ClusterSummary summary;

        private ClusterCard(int ordinal, String rawText) {
            super(new BorderLayout(Theme.SPACE_MD, Theme.SPACE_SM));
            this.ordinal = ordinal;
            this.summary = ClusterSummary.parse(rawText);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_MD, Theme.SPACE_MD, Theme.SPACE_MD, Theme.SPACE_MD));

            add(header(), BorderLayout.NORTH);
            add(themeList(), BorderLayout.CENTER);
        }

        private JComponent header() {
            JPanel header = new JPanel(new BorderLayout(Theme.SPACE_SM, 0));
            header.setOpaque(false);

            JLabel title = new JLabel("Cluster " + summary.clusterName().replaceFirst("^Cluster\\s*", ""));
            title.setFont(Theme.FONT_TITLE);
            title.setForeground(Theme.TEXT_PRIMARY);

            JLabel records = new JLabel(summary.recordText());
            records.setFont(Theme.FONT_LABEL);
            records.setForeground(Theme.TEXT_SECONDARY);

            header.add(title, BorderLayout.WEST);
            header.add(records, BorderLayout.EAST);
            return header;
        }

        private JComponent themeList() {
            JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, Theme.SPACE_XS));
            chips.setOpaque(false);
            for (String theme : summary.themes()) {
                chips.add(new ThemeChip(theme));
            }
            return chips;
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension preferred = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, preferred.height);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.BG_CARD);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), Theme.RADIUS_MD, Theme.RADIUS_MD));
            Color accent = Theme.CHART_PALETTE[(ordinal - 1) % Theme.CHART_PALETTE.length];
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 36));
            g.fill(new RoundRectangle2D.Double(0, 0, 6, getHeight(), Theme.RADIUS_SM, Theme.RADIUS_SM));
            g.setColor(Theme.BORDER_LIGHT);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1,
                    Theme.RADIUS_MD, Theme.RADIUS_MD));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    private static final class ThemeChip extends JLabel {
        private ThemeChip(String text) {
            super(text);
            setFont(Theme.FONT_MONO);
            setForeground(Theme.TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XS, Theme.SPACE_SM,
                    Theme.SPACE_XS, Theme.SPACE_SM));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.BRAND_SOFT);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                    Theme.RADIUS_SM, Theme.RADIUS_SM));
            g.setColor(Theme.BORDER_LIGHT);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1,
                    Theme.RADIUS_SM, Theme.RADIUS_SM));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    private record ClusterSummary(String clusterName, String recordText, List<String> themes) {
        private static ClusterSummary parse(String rawText) {
            String text = rawText == null ? "" : rawText.strip();
            String[] lines = text.split("\\R", 2);
            String header = lines.length > 0 && !lines[0].isBlank() ? lines[0].strip() : "Cluster";
            String clusterName = header.replaceAll("\\s*\\([^)]*\\)", "");
            String recordText = "";
            int open = header.indexOf('(');
            int close = header.indexOf(')', open + 1);
            if (open >= 0 && close > open) {
                recordText = header.substring(open + 1, close);
            }

            String themesText = lines.length > 1 ? lines[1] : "";
            themesText = themesText.replaceFirst("(?i)^Top themes:\\s*", "");
            List<String> themes = new ArrayList<>();
            for (String theme : themesText.split(",")) {
                String trimmed = theme.trim();
                if (!trimmed.isEmpty()) themes.add(trimmed);
            }
            if (themes.isEmpty()) themes.add(text);
            return new ClusterSummary(clusterName, recordText, themes);
        }
    }
}
