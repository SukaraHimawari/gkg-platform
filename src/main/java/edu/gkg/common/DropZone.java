package edu.gkg.common;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * 拖拽导入区：虚线圆角框 + 中央图标 + 提示文字 + 主按钮。
 * 支持拖拽文件/目录，也支持点击按钮触发文件选择。
 */
public class DropZone extends JPanel {

    private final JButton browseBtn = new JButton("选择文件 / 文件夹");
    private Consumer<List<File>> onFilesDropped;
    private boolean hover = false;

    public DropZone(Consumer<List<File>> onFilesDropped) {
        super(new GridBagLayout());
        this.onFilesDropped = onFilesDropped;
        setOpaque(false);
        setPreferredSize(new Dimension(640, 200));
        browseBtn.putClientProperty("JButton.buttonType", "roundRect");
        browseBtn.setBackground(Theme.BRAND);
        browseBtn.setForeground(Color.WHITE);
        browseBtn.setFocusPainted(false);

        JLabel icon = new JLabel("⤓");
        icon.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 40));
        icon.setForeground(Theme.BRAND);

        JLabel title = new JLabel("拖拽文件或文件夹到此处");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel hint = new JLabel("支持 CSV / TSV / GKG / GDELT 格式 / .zip");
        hint.setFont(Theme.FONT_LABEL);
        hint.setForeground(Theme.TEXT_SECONDARY);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.insets = new Insets(0, 0, Theme.SPACE_SM, 0);
        add(icon, gc);
        gc.gridy = 1; add(title, gc);
        gc.gridy = 2; add(hint, gc);
        gc.gridy = 3; gc.insets = new Insets(Theme.SPACE_LG, 0, 0, 0);
        add(browseBtn, gc);

        new DropTarget(this, new DropTargetAdapter() {
            @Override public void dragEnter(DropTargetDragEvent e) { hover = true; repaint(); }
            @Override public void dragExit(DropTargetEvent e)      { hover = false; repaint(); }
            @SuppressWarnings("unchecked")
            @Override public void drop(DropTargetDropEvent e) {
                hover = false; repaint();
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Object data = e.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof File) {
                        if (onFilesDropped != null) onFilesDropped.accept((List<File>) list);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    public JButton browseButton() { return browseBtn; }

    public void setConsumer(Consumer<List<File>> consumer) { this.onFilesDropped = consumer; }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(hover ? Theme.BRAND_SOFT : Theme.BG_DROP_ZONE);
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                Theme.RADIUS_LG, Theme.RADIUS_LG));
        g.setColor(hover ? Theme.BRAND : Theme.BORDER_DASHED);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{8, 6}, 0));
        g.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2,
                Theme.RADIUS_LG, Theme.RADIUS_LG));
        g.dispose();
        super.paintComponent(g0);
    }
}
