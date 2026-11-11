package Capstone.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;


public final class Theme {

    public static final Color BG = new Color(0xF3F6F8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color DARK = new Color(0x0B2E3B);
    public static final Color PRIMARY = new Color(0x0E7C86);
    public static final Color PRIMARY_DARK = new Color(0x0A5F67);
    public static final Color PRIMARY_LIGHT = new Color(0xDDF1F2);
    public static final Color ACCENT = new Color(0xF28C28);
    public static final Color ACCENT_DARK = new Color(0xD97611);
    public static final Color TEXT = new Color(0x1B2A33);
    public static final Color MUTED = new Color(0x6B7C87);
    public static final Color BORDER = new Color(0xDCE4E9);
    public static final Color SUCCESS = new Color(0x2E9E5B);
    public static final Color WARN = new Color(0xD98E04);
    public static final Color DANGER = new Color(0xD64545);

    private static final String FONT_NAME = "Segoe UI";

    private Theme() {}

    public static void install() {
        try {
            UIManager.put("defaultFont", new Font(FONT_NAME, Font.PLAIN, 14));
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("TabbedPane.tabHeight", 40);
            UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
            UIManager.put("TabbedPane.underlineColor", PRIMARY);
            UIManager.put("TabbedPane.inactiveUnderlineColor", BORDER);
            UIManager.put("Component.focusColor", PRIMARY);
            UIManager.put("Component.focusedBorderColor", PRIMARY);
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception flatLafMissing) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // keep the default look
            }
        }
    }


    public static Font font(int style, float size) {
        return new Font(FONT_NAME, style, 14).deriveFont(style, size);
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    public static JLabel title(String text) {
        return label(text, Font.BOLD, 22f, TEXT);
    }

    public static JLabel heading(String text) {
        return label(text, Font.BOLD, 16f, TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, Font.PLAIN, 13f, MUTED);
    }

    public static String money(double value) {
        return String.format(Locale.US, "R %,.2f", value);
    }

    public static String html(String text, int widthPx) {
        String safe = text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;");
        return "<html><div style='width:" + widthPx + "px'>" + safe + "</div></html>";
    }

    private static final String[] STAR_FONTS =
            {"Segoe UI Symbol", "Apple Symbols", "DejaVu Sans", Font.DIALOG};

    public static Font starFont(int style, float size) {
        // Falls back to the default font on systems without a glyph for ★/☆
        for (String name : STAR_FONTS) {
            Font candidate = new Font(name, style, 14);
            if (candidate.canDisplay('\u2605') && candidate.canDisplay('\u2606')) {
                return candidate.deriveFont(style, size);
            }
        }
        return font(style, size);
    }

    public static JLabel starLabel(String text, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(starFont(Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    public static String stars(int rating) {
        int r = Math.max(0, Math.min(5, rating));
        return "*".repeat(r) + "*".repeat(5 - r);
    }


    public static class RoundedPanel extends JPanel {
        private final int arc;
        private Color fill;
        private Color line = BORDER;

        public RoundedPanel(int arc) {
            this(arc, SURFACE);
        }

        public RoundedPanel(int arc, Color fill) {
            this.arc = arc;
            this.fill = fill;
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        public RoundedPanel borderColor(Color color) {
            this.line = color;
            return this;
        }

        public RoundedPanel fillColor(Color color) {
            this.fill = color;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
            if (line != null) {
                g2.setColor(line);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }


    public static class ViewportPanel extends JPanel implements Scrollable {
        public ViewportPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }


    private static JButton button(String label, Color background, Color textColor) {
        JButton button = new JButton(label);
        button.setBackground(background);
        button.setForeground(textColor);
        button.setFont(font(Font.BOLD, 14f));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(9, 20, 9, 20));
        return button;
    }

    public static JButton primary(String label) {
        return button(label, PRIMARY, Color.WHITE);
    }

    public static JButton accent(String label) {
        return button(label, ACCENT, Color.WHITE);
    }

    public static JButton ghost(String label) {
        JButton button = button(label, SURFACE, PRIMARY_DARK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 19, 8, 19)));
        return button;
    }

    public static JButton onDark(String label) {
        return button(label, new Color(0x24505F), Color.WHITE);
    }


    public static class Badge extends JLabel {
        public Badge(String text, Color background) {
            super(text);
            setOpaque(true);
            setBackground(background);
            setForeground(Color.WHITE);
            setFont(font(Font.BOLD, 12f));
            setBorder(new EmptyBorder(3, 10, 3, 10));
        }
    }


    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(font(Font.PLAIN, 14f));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createLineBorder(BORDER));
        pane.getViewport().setBackground(SURFACE);
        return pane;
    }


    public static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(font(Font.PLAIN, 14f));
        field.setMargin(new Insets(6, 8, 6, 8));
        return field;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    public static String friendly(Throwable t) {
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}