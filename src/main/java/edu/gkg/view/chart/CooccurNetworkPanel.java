package edu.gkg.view.chart;

import edu.gkg.common.SharedRecords.CooccurEdge;
import edu.gkg.common.SharedRecords.FocusNode;
import edu.gkg.common.Theme;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * F-20 共现网络图（自绘）。
 * 力导向布局：Fruchterman-Reingold 简化版，迭代 80 次。
 * 节点直径 = PageRank 映射；边粗细 = 共现次数映射。
 * 节点配色：按 PageRank 百分位映射品牌色→中性灰，强化中心、弱化边缘。
 */
public class CooccurNetworkPanel extends JPanel {

    private static final int FR_ITER = 80;

    private final Map<Integer, NodeView> nodes = new LinkedHashMap<>();
    private final List<EdgeView> edges = new ArrayList<>();

    private double zoom = 1.0;
    private double offsetX = 0, offsetY = 0;
    private NodeView dragging;
    private Point lastDragPoint;
    private NodeView hovered;
    private double rankMin = 0, rankMax = 1;

    public CooccurNetworkPanel() {
        setBackground(Theme.BG_CARD);
        setOpaque(true);
        renderDemo();
        installInteractions();
    }

    public void render(List<CooccurEdge> edgeData, List<FocusNode> ranks) {
        nodes.clear();
        edges.clear();
        Map<Integer, Double> rankMap = new HashMap<>();
        Map<Integer, Integer> degMap = new HashMap<>();
        for (FocusNode r : ranks) { rankMap.put(r.nodeId(), r.pageRank()); degMap.put(r.nodeId(), r.degree()); }
        for (CooccurEdge e : edgeData) {
            putNode(e.e1Id(), e.e1Name(), rankMap.getOrDefault(e.e1Id(), 0.01), degMap.getOrDefault(e.e1Id(), 0));
            putNode(e.e2Id(), e.e2Name(), rankMap.getOrDefault(e.e2Id(), 0.01), degMap.getOrDefault(e.e2Id(), 0));
            edges.add(new EdgeView(e.e1Id(), e.e2Id(), e.coCount()));
        }
        recomputeRankRange();
        layoutFruchtermanReingold();
        repaint();
    }

    private void putNode(int id, String name, double rank, int degree) {
        nodes.computeIfAbsent(id, k -> new NodeView(id, name, rank, degree, randomX(), randomY()));
    }

    private void recomputeRankRange() {
        rankMin = Double.MAX_VALUE; rankMax = Double.MIN_VALUE;
        for (NodeView n : nodes.values()) {
            rankMin = Math.min(rankMin, n.rank);
            rankMax = Math.max(rankMax, n.rank);
        }
        if (rankMax == rankMin) rankMax = rankMin + 1e-6;
    }

    private double randomX() { return 200 + Math.random() * 600; }
    private double randomY() { return 150 + Math.random() * 400; }

    private void renderDemo() {
        String[] names = {"Biden", "Trump", "Xi", "Putin", "Zelensky", "Macron", "UN", "NATO", "EU", "G7"};
        double[] ranks = {0.18, 0.16, 0.12, 0.11, 0.08, 0.07, 0.10, 0.08, 0.06, 0.04};
        List<CooccurEdge> demoEdges = new ArrayList<>();
        Random rand = new Random(42);
        for (int i = 0; i < names.length; i++) {
            for (int j = i + 1; j < names.length; j++) {
                if (rand.nextDouble() < 0.35) {
                    demoEdges.add(new CooccurEdge(i, names[i], j, names[j],
                            (int) (5 + rand.nextDouble() * 30)));
                }
            }
        }
        List<FocusNode> demoRanks = new ArrayList<>();
        for (int i = 0; i < names.length; i++) demoRanks.add(new FocusNode(i, names[i], ranks[i], 0));
        render(demoEdges, demoRanks);
    }

    private void layoutFruchtermanReingold() {
        int W = 1000, H = 700;
        int n = nodes.size();
        if (n == 0) return;
        double area = (double) W * H;
        double k = Math.sqrt(area / n);
        double t = W / 10.0;
        double cool = t / (FR_ITER + 1);

        List<NodeView> arr = new ArrayList<>(nodes.values());
        for (int iter = 0; iter < FR_ITER; iter++) {
            for (NodeView v : arr) { v.dx = 0; v.dy = 0; }
            for (int i = 0; i < n; i++) {
                NodeView v = arr.get(i);
                for (int j = i + 1; j < n; j++) {
                    NodeView u = arr.get(j);
                    double dx = v.x - u.x, dy = v.y - u.y;
                    double dist = Math.max(0.01, Math.hypot(dx, dy));
                    double force = k * k / dist;
                    v.dx += dx / dist * force;
                    v.dy += dy / dist * force;
                    u.dx -= dx / dist * force;
                    u.dy -= dy / dist * force;
                }
            }
            for (EdgeView e : edges) {
                NodeView v = nodes.get(e.from), u = nodes.get(e.to);
                if (v == null || u == null) continue;
                double dx = v.x - u.x, dy = v.y - u.y;
                double dist = Math.max(0.01, Math.hypot(dx, dy));
                double force = dist * dist / k;
                v.dx -= dx / dist * force;
                v.dy -= dy / dist * force;
                u.dx += dx / dist * force;
                u.dy += dy / dist * force;
            }
            for (NodeView v : arr) {
                double disp = Math.max(0.01, Math.hypot(v.dx, v.dy));
                v.x += v.dx / disp * Math.min(disp, t);
                v.y += v.dy / disp * Math.min(disp, t);
                v.x = Math.max(20, Math.min(W - 20, v.x));
                v.y = Math.max(20, Math.min(H - 20, v.y));
            }
            t -= cool;
        }
    }

    private void installInteractions() {
        MouseInputAdapter ma = new MouseInputAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragging = pick(e.getPoint());
                lastDragPoint = e.getPoint();
            }
            @Override public void mouseDragged(MouseEvent e) {
                Point p = e.getPoint();
                if (dragging != null) {
                    dragging.x += (p.x - lastDragPoint.x) / zoom;
                    dragging.y += (p.y - lastDragPoint.y) / zoom;
                } else {
                    offsetX += p.x - lastDragPoint.x;
                    offsetY += p.y - lastDragPoint.y;
                }
                lastDragPoint = p;
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) { dragging = null; }
            @Override public void mouseMoved(MouseEvent e) {
                NodeView h = pick(e.getPoint());
                if (h != hovered) { hovered = h; repaint(); }
                setToolTipText(h == null ? null
                        : String.format("<html><b>%s</b><br>PageRank: %.4f<br>度数: %d</html>",
                            h.name, h.rank, h.degree));
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener((MouseWheelEvent e) -> {
            zoom *= e.getPreciseWheelRotation() < 0 ? 1.1 : 0.9;
            zoom = Math.max(0.2, Math.min(zoom, 5.0));
            repaint();
        });
    }

    private NodeView pick(Point p) {
        Point2D w = screenToWorld(p);
        NodeView best = null;
        double bestD = Double.MAX_VALUE;
        for (NodeView n : nodes.values()) {
            double d = Math.hypot(w.getX() - n.x, w.getY() - n.y);
            if (d < n.radius() && d < bestD) { best = n; bestD = d; }
        }
        return best;
    }

    private Point2D screenToWorld(Point p) {
        return new Point2D.Double((p.x - offsetX) / zoom, (p.y - offsetY) / zoom);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.translate(offsetX, offsetY);
        g.scale(zoom, zoom);

        int maxCo = 1;
        for (EdgeView e : edges) maxCo = Math.max(maxCo, e.weight);
        for (EdgeView e : edges) {
            NodeView a = nodes.get(e.from), b = nodes.get(e.to);
            if (a == null || b == null) continue;
            float w = 0.4f + 3.5f * e.weight / maxCo;
            g.setStroke(new BasicStroke(w));
            boolean involvesHover = hovered != null && (a == hovered || b == hovered);
            g.setColor(involvesHover
                    ? new Color(Theme.BRAND.getRed(), Theme.BRAND.getGreen(), Theme.BRAND.getBlue(), 200)
                    : new Color(180, 188, 200, 130));
            g.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
        }

        g.setFont(Theme.FONT_LABEL);
        for (NodeView n : nodes.values()) {
            double r = n.radius();
            double t = (n.rank - rankMin) / (rankMax - rankMin);
            boolean hi = n == hovered;
            Color fill = blend(new Color(0xC4D2E4), Theme.BRAND, (float) t);
            if (hi) fill = Theme.BRAND_DARK;
            g.setColor(fill);
            g.fill(new Ellipse2D.Double(n.x - r, n.y - r, 2 * r, 2 * r));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(hi ? 2.5f : 1.5f));
            g.draw(new Ellipse2D.Double(n.x - r, n.y - r, 2 * r, 2 * r));
            g.setColor(hi ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
            int sw = g.getFontMetrics().stringWidth(n.name);
            g.drawString(n.name, (float) (n.x - sw / 2.0), (float) (n.y + r + 14));
        }
        g.dispose();

        // 图例（屏幕坐标，不受 zoom 影响）
        Graphics2D g2 = (Graphics2D) g0.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(Theme.FONT_LABEL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString("节点大小 ∝ PageRank", 12, getHeight() - 28);
        g2.drawString("边粗细 ∝ 共现次数", 12, getHeight() - 12);
        g2.dispose();
    }

    private static Color blend(Color a, Color b, float t) {
        int r = Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl= Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl);
    }

    private static class NodeView {
        final int id;
        final String name;
        double rank;
        int degree;
        double x, y, dx, dy;
        NodeView(int id, String name, double rank, int degree, double x, double y) {
            this.id = id; this.name = name; this.rank = rank; this.degree = degree;
            this.x = x; this.y = y;
        }
        double radius() { return 8 + Math.sqrt(rank) * 100; }
    }

    private record EdgeView(int from, int to, int weight) {}
}
