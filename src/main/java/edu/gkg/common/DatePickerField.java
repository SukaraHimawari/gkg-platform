package edu.gkg.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * 可弹出日历的日期选择控件。
 * 点击文本框或按钮弹出月历，点击日期格子即选中并关闭弹窗。
 */
public class DatePickerField extends JPanel {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] WEEK_HEADERS = {"日", "一", "二", "三", "四", "五", "六"};

    private LocalDate selectedDate;
    private LocalDate minDate = LocalDate.of(2015, 2, 19);
    private LocalDate maxDate = LocalDate.now();
    private YearMonth viewMonth;
    private Consumer<LocalDate> onChange;

    private final JTextField displayField = new JTextField(12);
    private final JButton    toggleBtn;
    private JPopupMenu popup;

    public DatePickerField(LocalDate initial) {
        super(new BorderLayout(4, 0));
        setOpaque(false);
        this.selectedDate = clamp(initial);
        this.viewMonth    = YearMonth.from(selectedDate);

        displayField.setText(selectedDate.format(DISPLAY_FMT));
        displayField.setEditable(false);
        displayField.setFont(Theme.FONT_DEFAULT);
        displayField.setForeground(Theme.TEXT_PRIMARY);
        displayField.setBackground(Color.WHITE);
        displayField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        displayField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        displayField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle(); }
        });

        toggleBtn = new JButton("v");
        toggleBtn.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 11));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBackground(Theme.BG_CARD);
        toggleBtn.setForeground(Theme.TEXT_SECONDARY);
        toggleBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        toggleBtn.addActionListener(e -> toggle());

        add(displayField, BorderLayout.CENTER);
        add(toggleBtn,    BorderLayout.EAST);
    }

    public LocalDate getSelectedDate() { return selectedDate; }
    public void setMinDate(LocalDate d) { this.minDate = d; }
    public void setMaxDate(LocalDate d) { this.maxDate = d; }
    public void setOnChange(Consumer<LocalDate> cb) { this.onChange = cb; }

    private void toggle() {
        if (popup != null && popup.isVisible()) { popup.setVisible(false); return; }
        openCalendar();
    }

    private void openCalendar() {
        popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        popup.setBackground(Theme.BG_CARD);
        popup.add(buildCalendarPanel());
        popup.show(this, 0, getHeight() + 2);
    }

    private void refreshCalendar() {
        if (popup == null) return;
        popup.removeAll();
        popup.add(buildCalendarPanel());
        popup.revalidate(); popup.repaint();
    }

    private JPanel buildCalendarPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 4));
        root.setBackground(Theme.BG_CARD);
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildGrid(),   BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_CARD);
        JButton prev = navBtn("‹");
        JButton next = navBtn("›");
        prev.addActionListener(e -> { viewMonth = viewMonth.minusMonths(1); refreshCalendar(); });
        next.addActionListener(e -> { viewMonth = viewMonth.plusMonths(1); refreshCalendar(); });
        JLabel title = new JLabel(viewMonth.getYear() + " 年 " + viewMonth.getMonthValue() + " 月",
                SwingConstants.CENTER);
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.TEXT_PRIMARY);
        p.add(prev,  BorderLayout.WEST);
        p.add(title, BorderLayout.CENTER);
        p.add(next,  BorderLayout.EAST);
        return p;
    }

    private JPanel buildGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setBackground(Theme.BG_CARD);
        for (String h : WEEK_HEADERS) {
            JLabel lbl = new JLabel(h, SwingConstants.CENTER);
            lbl.setFont(Theme.FONT_LABEL);
            lbl.setForeground(Theme.TEXT_MUTED);
            lbl.setPreferredSize(new Dimension(36, 22));
            grid.add(lbl);
        }
        LocalDate first = viewMonth.atDay(1);
        int startDow = first.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < startDow; i++) grid.add(new JLabel());
        int days = viewMonth.lengthOfMonth();
        for (int d = 1; d <= days; d++) grid.add(buildDayCell(viewMonth.atDay(d)));
        return grid;
    }

    private JLabel buildDayCell(LocalDate day) {
        boolean isSelected = day.equals(selectedDate);
        boolean isToday    = day.equals(LocalDate.now());
        boolean disabled   = (minDate != null && day.isBefore(minDate))
                          || (maxDate != null && day.isAfter(maxDate));
        JLabel cell = new JLabel(String.valueOf(day.getDayOfMonth()), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                if (isSelected) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.BRAND);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                } else if (isToday) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.BRAND_SOFT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        cell.setOpaque(false);
        cell.setFont(Theme.FONT_DEFAULT);
        cell.setPreferredSize(new Dimension(36, 30));
        if (isSelected)       cell.setForeground(Color.WHITE);
        else if (disabled)    cell.setForeground(Theme.TEXT_MUTED);
        else if (isToday)     cell.setForeground(Theme.BRAND);
        else                  cell.setForeground(Theme.TEXT_PRIMARY);
        if (!disabled) {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (!isSelected) { cell.setBackground(Theme.BG_HOVER); cell.setOpaque(true); }
                    cell.repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    cell.setOpaque(false); cell.repaint();
                }
                @Override public void mouseClicked(MouseEvent e) { selectDate(day); }
            });
        }
        return cell;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        p.setBackground(Theme.BG_CARD);
        JButton todayBtn = new JButton("今天");
        todayBtn.setFont(Theme.FONT_LABEL);
        todayBtn.setForeground(Theme.BRAND);
        todayBtn.setBackground(Theme.BG_CARD);
        todayBtn.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
        todayBtn.setFocusPainted(false);
        todayBtn.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            if ((minDate == null || !today.isBefore(minDate))
                    && (maxDate == null || !today.isAfter(maxDate)))
                selectDate(today);
        });
        p.add(todayBtn);
        return p;
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        viewMonth    = YearMonth.from(date);
        displayField.setText(date.format(DISPLAY_FMT));
        if (popup != null) popup.setVisible(false);
        if (onChange != null) onChange.accept(date);
    }

    private LocalDate clamp(LocalDate d) {
        if (minDate != null && d.isBefore(minDate)) return minDate;
        if (maxDate != null && d.isAfter(maxDate))  return maxDate;
        return d;
    }

    private static JButton navBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 16));
        b.setForeground(Theme.TEXT_SECONDARY);
        b.setBackground(Theme.BG_CARD);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        b.setContentAreaFilled(false);
        return b;
    }
}
