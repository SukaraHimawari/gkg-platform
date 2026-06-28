package edu.gkg;

import com.formdev.flatlaf.FlatLightLaf;
import edu.gkg.common.DbHelper;
import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;
import edu.gkg.view.MainFrame;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

public class App {

    public static void main(String[] args) {
        try {
            DbHelper.getConnection().close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "数据库初始化失败：" + e.getMessage(),
                    "启动错误", JOptionPane.ERROR_MESSAGE);
        }
        configureFlatLaf();
        UiUtil.applyGlobalFont(Theme.FONT_DEFAULT);
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    private static void configureFlatLaf() {
        UIManager.put("Component.focusWidth",  0);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.arc",         Theme.RADIUS_MD);
        UIManager.put("Button.arc",            Theme.RADIUS_MD);
        UIManager.put("TextComponent.arc",     Theme.RADIUS_SM);
        UIManager.put("ProgressBar.arc",       Theme.RADIUS_LG);
        UIManager.put("ScrollBar.thumbArc",    Theme.RADIUS_SM);

        UIManager.put("Component.accentColor",     Theme.BRAND);
        UIManager.put("Button.default.background", Theme.BRAND);
        UIManager.put("Button.default.foreground", java.awt.Color.WHITE);

        UIManager.put("TabbedPane.tabHeight",          36);
        UIManager.put("TabbedPane.selectedBackground", Theme.BG_CARD);
        UIManager.put("TabbedPane.underlineColor",     Theme.BRAND);
        UIManager.put("TabbedPane.contentAreaColor",   Theme.BG_APP);

        UIManager.put("Panel.background",          Theme.BG_APP);
        UIManager.put("TableHeader.background",    Theme.BG_APP);
        UIManager.put("Table.gridColor",           Theme.BORDER_LIGHT);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines",   false);
        UIManager.put("Table.rowHeight",           28);

        UIManager.put("MenuBar.background",           Theme.BG_CARD);
        UIManager.put("MenuItem.selectionBackground", Theme.BRAND_SOFT);
        UIManager.put("MenuItem.selectionForeground", Theme.TEXT_PRIMARY);

        UIManager.put("defaultFont", new FontUIResource(Theme.FONT_DEFAULT));

        try { FlatLightLaf.setup(); } catch (Exception ignored) {}
    }
}
