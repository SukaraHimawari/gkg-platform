package edu.gkg.common;

import java.awt.*;

/**
 * 全局设计 token：色板 / 字体 / 间距 / 圆角。
 * 任何 view 都从这里读，不要散落硬编码颜色。
 */
public final class Theme {

    private Theme() {}

    // ===== 字体 =====
    public static final String FONT_FAMILY      = "微软雅黑";
    public static final Font   FONT_DEFAULT     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font   FONT_LABEL       = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font   FONT_SECTION     = new Font(FONT_FAMILY, Font.BOLD,  13);
    public static final Font   FONT_TITLE       = new Font(FONT_FAMILY, Font.BOLD,  16);
    public static final Font   FONT_HERO        = new Font(FONT_FAMILY, Font.BOLD,  22);
    public static final Font   FONT_BIG_NUMBER  = new Font(FONT_FAMILY, Font.BOLD,  28);
    public static final Font   FONT_MONO        = new Font("Consolas",  Font.PLAIN, 13);

    // ===== 色板（亮色主题）=====
    public static final Color BG_APP            = new Color(0xF7F8FA);
    public static final Color BG_CARD           = Color.WHITE;
    public static final Color BG_HOVER          = new Color(0xF1F4F8);
    public static final Color BG_DROP_ZONE      = new Color(0xF4F7FB);

    public static final Color BORDER_LIGHT      = new Color(0xE5E7EB);
    public static final Color BORDER_DASHED     = new Color(0xB7C2D0);

    public static final Color TEXT_PRIMARY      = new Color(0x1F2937);
    public static final Color TEXT_SECONDARY    = new Color(0x6B7280);
    public static final Color TEXT_MUTED        = new Color(0x9CA3AF);

    public static final Color BRAND             = new Color(0x2D7BF4); // 主蓝
    public static final Color BRAND_DARK        = new Color(0x1E63D6);
    public static final Color BRAND_SOFT        = new Color(0xE7F0FE);

    public static final Color SUCCESS           = new Color(0x22C55E);
    public static final Color SUCCESS_SOFT      = new Color(0xE9F8EF);
    public static final Color WARN              = new Color(0xF59E0B);
    public static final Color WARN_SOFT         = new Color(0xFEF3E2);
    public static final Color DANGER            = new Color(0xEF4444);
    public static final Color DANGER_SOFT       = new Color(0xFCE9E9);

    /** 情感正向 / 负向（与 SUCCESS / DANGER 对齐，便于复用语义）。 */
    public static final Color SENT_POS          = SUCCESS;
    public static final Color SENT_NEG          = DANGER;
    public static final Color SENT_NEU          = new Color(0x9CA3AF);

    /** 图表 8 色定性配色（Tableau 风格更克制版）。 */
    public static final Color[] CHART_PALETTE = {
            new Color(0x2D7BF4),
            new Color(0x22C55E),
            new Color(0xF59E0B),
            new Color(0xEF4444),
            new Color(0x8B5CF6),
            new Color(0x06B6D4),
            new Color(0xEC4899),
            new Color(0x64748B),
    };

    // ===== 间距 / 圆角 =====
    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;
    public static final int SPACE_XL = 24;

    public static final int RADIUS_SM = 6;
    public static final int RADIUS_MD = 10;
    public static final int RADIUS_LG = 14;

    /** 情感得分映射颜色（-10~+10），用于热力色和指针染色。 */
    public static Color sentimentColor(double tone) {
        if (tone >  1.0) return SENT_POS;
        if (tone < -1.0) return SENT_NEG;
        return SENT_NEU;
    }
}
