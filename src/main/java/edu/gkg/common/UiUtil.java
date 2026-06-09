package edu.gkg.common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public final class UiUtil {

    public static final Font UI_FONT     = Theme.FONT_DEFAULT;
    public static final Font TITLE_FONT  = Theme.FONT_TITLE;
    public static final Font MONO_FONT   = Theme.FONT_MONO;

    public static final Color COLOR_POS  = Theme.SENT_POS;
    public static final Color COLOR_NEG  = Theme.SENT_NEG;
    public static final Color COLOR_NEU  = Theme.SENT_NEU;
    public static final Color COLOR_HL   = Theme.BRAND;

    private UiUtil() {}

    public static void applyGlobalFont(Font f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object val = UIManager.get(key);
            if (val instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource(f));
            }
        }
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "警告", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String msg, Throwable t) {
        String detail = t == null ? msg : msg + "\n\n" + t.getClass().getSimpleName() + ": " + t.getMessage();
        JOptionPane.showMessageDialog(parent, detail, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "确认",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    public static void styleTable(JTable table) {
        table.setFont(Theme.FONT_DEFAULT);
        table.setRowHeight(28);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDER_LIGHT);
        table.setSelectionBackground(Theme.BRAND_SOFT);
        table.setSelectionForeground(Theme.TEXT_PRIMARY);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_SECTION);
        header.setBackground(Theme.BG_APP);
        header.setForeground(Theme.TEXT_SECONDARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean sel,
                                                           boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, v, sel, focus, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : Theme.BG_APP);
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    public static void autoSizeColumns(JTable table) {
        TableColumnModel cm = table.getColumnModel();
        for (int col = 0; col < cm.getColumnCount(); col++) {
            TableColumn tc = cm.getColumn(col);
            int max = 60;
            Component header = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, tc.getHeaderValue(), false, false, 0, col);
            max = Math.max(max, header.getPreferredSize().width + 16);
            int rows = Math.min(table.getRowCount(), 50);
            for (int row = 0; row < rows; row++) {
                Component c = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
                max = Math.max(max, c.getPreferredSize().width + 16);
            }
            tc.setPreferredWidth(Math.min(max, 360));
        }
    }

    public static JLabel makeStatusLabel(String text) {
        JLabel l = new JLabel(" " + text);
        l.setFont(Theme.FONT_LABEL);
        l.setForeground(Theme.TEXT_SECONDARY);
        l.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));
        return l;
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_DEFAULT);
        b.setFocusPainted(false);
        b.setBackground(Theme.BRAND);
        b.setForeground(Color.WHITE);
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_DEFAULT);
        b.setFocusPainted(false);
        b.setBackground(Theme.BG_CARD);
        b.setForeground(Theme.TEXT_PRIMARY);
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    public static JPanel hbox(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, Theme.SPACE_XS));
        p.setOpaque(false);
        for (JComponent c : items) p.add(c);
        return p;
    }

    public static JPanel vbox(JComponent... items) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (JComponent c : items) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(c);
            p.add(Box.createVerticalStrut(Theme.SPACE_SM));
        }
        return p;
    }

    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SECTION);
        l.setForeground(Theme.TEXT_PRIMARY);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SPACE_XS, 0));
        return l;
    }

    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_LABEL);
        l.setForeground(Theme.TEXT_SECONDARY);
        return l;
    }
}
