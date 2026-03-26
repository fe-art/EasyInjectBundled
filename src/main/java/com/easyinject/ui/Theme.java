package com.easyinject.ui;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Centralized theme constants and component factories for the installer UI.
 */
public final class Theme {

    // ── Colors ──────────────────────────────────────────────────────────────
    public static final Color BG        = new Color(43, 43, 43);       // #2B2B2B
    public static final Color BG_DARKER = new Color(30, 30, 30);       // #1E1E1E
    public static final Color FG        = new Color(224, 224, 224);    // #E0E0E0
    public static final Color BTN_BG    = new Color(60, 60, 60);       // #3C3C3C
    public static final Color BTN_HOVER = new Color(80, 80, 80);       // #505050
    public static final Color BTN_PRESS = new Color(40, 40, 40);       // #282828
    public static final Color BORDER    = new Color(100, 100, 100);    // #646464
    public static final Color DIVIDER   = new Color(60, 60, 60);       // #3C3C3C

    public static final Color SUCCESS   = new Color(76, 175, 80);      // #4CAF50
    public static final Color WARNING   = new Color(255, 179, 0);      // #FFB300
    public static final Color ERROR     = new Color(255, 82, 82);      // #FF5252
    public static final Color INFO      = new Color(129, 212, 250);    // #81D4FA
    public static final Color SUBTLE    = new Color(199, 206, 214);    // #C7CED6
    public static final Color MUTED     = new Color(158, 158, 158);    // #9E9E9E

    // ── Fonts ───────────────────────────────────────────────────────────────
    public static final Font TITLE_FONT   = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font HEADING_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font BODY_FONT    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL_FONT   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font BTN_FONT     = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font MONO_FONT    = new Font("Consolas", Font.PLAIN, 12);

    // ── Icon size ───────────────────────────────────────────────────────────
    private static final int ICON_SIZE = 16;

    private Theme() {}

    // ── Apply dark UIManager defaults ───────────────────────────────────────
    public static void applyDarkTheme() {
        UIManager.put("OptionPane.background", BG);
        UIManager.put("OptionPane.messageForeground", FG);
        UIManager.put("OptionPane.messageFont", BODY_FONT);
        UIManager.put("Panel.background", BG);
        UIManager.put("Panel.foreground", FG);
        UIManager.put("Label.background", BG);
        UIManager.put("Label.foreground", FG);
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("Button.background", BTN_BG);
        UIManager.put("Button.foreground", FG);
        UIManager.put("Button.font", BTN_FONT);
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80)),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        UIManager.put("TextField.background", BG_DARKER);
        UIManager.put("TextField.foreground", FG);
        UIManager.put("TextField.caretForeground", FG);
        UIManager.put("TextField.font", MONO_FONT);
        UIManager.put("ComboBox.background", BTN_BG);
        UIManager.put("ComboBox.foreground", FG);
        UIManager.put("ComboBox.selectionBackground", BTN_HOVER);
        UIManager.put("ComboBox.selectionForeground", FG);
        UIManager.put("ComboBox.font", BODY_FONT);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("Viewport.background", BG);
        UIManager.put("TextArea.background", BG_DARKER);
        UIManager.put("TextArea.foreground", FG);
    }

    // ── Component Factories ─────────────────────────────────────────────────

    /**
     * Standard styled button with rounded corners and hover/press states.
     * Ported from Main.createStyledButton().
     */
    public static JButton createButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Clear full bounds to prevent hover repaint trails/ghosting.
                Color clear = null;
                try {
                    Container p = getParent();
                    if (p != null) clear = p.getBackground();
                } catch (Throwable ignored) {}
                if (clear == null) clear = BG;
                g2.setColor(clear);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Background color based on state
                Color bgColor;
                if (!isEnabled()) {
                    bgColor = new Color(50, 50, 50);
                } else if (getModel().isPressed()) {
                    bgColor = BTN_PRESS;
                } else if (getModel().isRollover()) {
                    bgColor = BTN_HOVER;
                } else {
                    bgColor = BTN_BG;
                }

                int x = 1, y = 1;
                int w = Math.max(0, getWidth() - 2);
                int h = Math.max(0, getHeight() - 2);
                int arc = 10;

                g2.setColor(bgColor);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                g2.setColor(BORDER);
                if (w > 1 && h > 1) {
                    g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
                }

                super.paintComponent(g2);
                g2.dispose();
            }
        };

        btn.setFont(BTN_FONT);
        btn.setForeground(FG);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setDoubleBuffered(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        btn.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                btn.repaint();
                Container p = btn.getParent();
                if (p != null) {
                    int pad = 2;
                    p.repaint(btn.getX() - pad, btn.getY() - pad,
                              btn.getWidth() + pad * 2, btn.getHeight() + pad * 2);
                }
            }
        });

        return btn;
    }

    /**
     * Accent-colored button (e.g. green Install, blue Discord).
     */
    public static JButton createAccentButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color clear = null;
                try {
                    Container p = getParent();
                    if (p != null) clear = p.getBackground();
                } catch (Throwable ignored) {}
                if (clear == null) clear = BG;
                g2.setColor(clear);
                g2.fillRect(0, 0, getWidth(), getHeight());

                Color bgColor;
                if (!isEnabled()) {
                    bgColor = new Color(accent.getRed() / 2, accent.getGreen() / 2, accent.getBlue() / 2);
                } else if (getModel().isPressed()) {
                    bgColor = accent.darker();
                } else if (getModel().isRollover()) {
                    bgColor = accent.brighter();
                } else {
                    bgColor = accent;
                }

                int x = 1, y = 1;
                int w = Math.max(0, getWidth() - 2);
                int h = Math.max(0, getHeight() - 2);
                int arc = 10;

                g2.setColor(bgColor);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                super.paintComponent(g2);
                g2.dispose();
            }
        };

        btn.setFont(BTN_FONT);
        btn.setForeground(Color.WHITE);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setDoubleBuffered(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        btn.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                btn.repaint();
                Container p = btn.getParent();
                if (p != null) {
                    int pad = 2;
                    p.repaint(btn.getX() - pad, btn.getY() - pad,
                              btn.getWidth() + pad * 2, btn.getHeight() + pad * 2);
                }
            }
        });

        return btn;
    }

    // ── Vector Icons ────────────────────────────────────────────────────────

    /** Green circle with white checkmark. */
    public static Icon successIcon() {
        return circleIcon(SUCCESS, g2 -> {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float check = new Path2D.Float();
            check.moveTo(4, 8);
            check.lineTo(7, 11);
            check.lineTo(12, 5);
            g2.draw(check);
        });
    }

    /** Red circle with white X. */
    public static Icon errorIcon() {
        return circleIcon(ERROR, g2 -> {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(5, 5, 11, 11);
            g2.drawLine(11, 5, 5, 11);
        });
    }

    /** Amber circle with white exclamation. */
    public static Icon warningIcon() {
        return circleIcon(WARNING, g2 -> {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(8, 4, 8, 9);
            g2.fillOval(7, 11, 2, 2);
        });
    }

    /** Blue circle with white "i". */
    public static Icon infoIcon() {
        return circleIcon(INFO, g2 -> {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("i", 6, 13);
        });
    }

    /** Gray circle with dots (pending). */
    public static Icon pendingIcon() {
        return circleIcon(MUTED, g2 -> {
            g2.setColor(Color.WHITE);
            g2.fillOval(4, 7, 2, 2);
            g2.fillOval(7, 7, 2, 2);
            g2.fillOval(10, 7, 2, 2);
        });
    }

    /** Large (32px) success checkmark for the Done screen. */
    public static Icon largeSuccessIcon() {
        final int sz = 32;
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SUCCESS);
                g2.fillOval(x, y, sz, sz);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Float check = new Path2D.Float();
                check.moveTo(x + 8, y + 16);
                check.lineTo(x + 14, y + 22);
                check.lineTo(x + 24, y + 10);
                g2.draw(check);
                g2.dispose();
            }
            @Override public int getIconWidth() { return sz; }
            @Override public int getIconHeight() { return sz; }
        };
    }

    /** Large (32px) error X for the Done screen. */
    public static Icon largeErrorIcon() {
        final int sz = 32;
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ERROR);
                g2.fillOval(x, y, sz, sz);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 10, y + 10, x + 22, y + 22);
                g2.drawLine(x + 22, y + 10, x + 10, y + 22);
                g2.dispose();
            }
            @Override public int getIconWidth() { return sz; }
            @Override public int getIconHeight() { return sz; }
        };
    }

    // ── Panels / Layout helpers ─────────────────────────────────────────────

    /** Create a dark panel with BoxLayout Y axis. */
    public static JPanel createVBox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setOpaque(true);
        return p;
    }

    /** Create a dark panel with FlowLayout. */
    public static JPanel createHBox(int align) {
        JPanel p = new JPanel(new FlowLayout(align, 8, 0));
        p.setBackground(BG);
        p.setOpaque(true);
        return p;
    }

    /** Horizontal divider line. */
    public static JSeparator createDivider() {
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setForeground(DIVIDER);
        sep.setBackground(BG);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    /** Styled HTML label. */
    public static JLabel createLabel(String text, Font font, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(fg);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /** A card-like panel with a darker background and padding. */
    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_DARKER);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private interface IconPainter {
        void paint(Graphics2D g2);
    }

    private static Icon circleIcon(Color bg, IconPainter inner) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillOval(x, y, ICON_SIZE, ICON_SIZE);
                g2.translate(x, y);
                inner.paint(g2);
                g2.dispose();
            }
            @Override public int getIconWidth() { return ICON_SIZE; }
            @Override public int getIconHeight() { return ICON_SIZE; }
        };
    }
}
