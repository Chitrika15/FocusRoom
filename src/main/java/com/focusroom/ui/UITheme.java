package com.focusroom.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Aesthetic constants, colors, fonts, and UI component styling helpers.
 */
public class UITheme {

    // Color Palette
    public static final Color PRIMARY = new Color(37, 99, 235);       // Royal Blue #2563EB
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216); // Darker Blue #1D4ED8
    public static final Color ACCENT = new Color(16, 185, 129);       // Emerald Green #10B981
    public static final Color DANGER = new Color(239, 68, 68);        // Coral Red #EF4444
    public static final Color WARNING = new Color(245, 158, 11);      // Amber #F59E0B

    public static final Color BG_DARK = new Color(15, 23, 42);        // Slate 900
    public static final Color BG_LIGHT = new Color(248, 250, 252);    // Slate 50
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(226, 232, 240); // Slate 200

    public static final Color TEXT_MAIN = new Color(15, 23, 42);      // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);  // Slate 500
    public static final Color INPUT_BORDER = new Color(203, 213, 225);// Slate 300

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_TIMER = new Font("Consolas", Font.BOLD, 36);

    /**
     * Styles a standard primary action button.
     */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Styles an accent / success button.
     */
    public static JButton createSuccessButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Styles a danger / warning button.
     */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Styles a secondary / neutral button.
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(CARD_BG);
        btn.setForeground(TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(new LineBorder(INPUT_BORDER, 1), new EmptyBorder(7, 15, 7, 15)));
        return btn;
    }

    /**
     * Styles a card panel with padding and light border.
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(CARD_BORDER, 1),
                new EmptyBorder(16, 16, 16, 16)
        ));
        return card;
    }

    /**
     * Applies standard styling to input text fields.
     */
    public static void styleTextField(JTextField field) {
        field.setFont(FONT_BODY);
        field.setBorder(new CompoundBorder(
                new LineBorder(INPUT_BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));
    }
}
