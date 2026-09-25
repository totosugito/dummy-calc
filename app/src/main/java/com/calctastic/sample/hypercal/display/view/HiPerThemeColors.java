package com.calctastic.sample.hypercal.display.view;

import android.graphics.Color;

/**
 * Palet warna HiPER Calculator (Amoled Dark Style)
 * diselaraskan dengan Calctastic dark background.
 */
public class HiPerThemeColors {
    // Background Display
    public static final int COLOR_DISPLAY_BG = Color.parseColor("#121212");

    // Formula & Primary Text (Key "86" in HiPER)
    public static final int COLOR_EXPRESSION_TEXT = Color.parseColor("#FFFFFF");

    // Secondary / Dimmed formula text
    public static final int COLOR_EXPRESSION_DIM = Color.parseColor("#A8A8A8");

    // Fraction line, Radicals, Math operators (Key "87" in HiPER: Color.rgb(147, 180, 255))
    public static final int COLOR_MATH_ACCENT = Color.parseColor("#93B4FF");

    // Placeholder Box (Kotak slot input pecahan/pangkat kosong HiPER - QA / C0357yG)
    public static final int COLOR_PLACEHOLDER_BOX = Color.parseColor("#446088");
    public static final int COLOR_PLACEHOLDER_ACTIVE_BOX = Color.parseColor("#93B4FF");

    // Calctastic Orange Accent (untuk status badge / kursor terpilih)
    public static final int COLOR_ORANGE_ACCENT = Color.parseColor("#FF9800");

    // Cursor Color (Key "85" & "84" in HiPER: Color.rgb(173, 198, 255) / Color.rgb(96, 112, 145))
    public static final int COLOR_CURSOR = Color.parseColor("#ADC6FF");
    public static final int COLOR_CURSOR_HANDLE = Color.parseColor("#607091");

    // Status Indicator Text (DEG, RAD, FIX, etc.)
    public static final int COLOR_STATUS_ACTIVE = Color.parseColor("#FF9800");
    public static final int COLOR_STATUS_INACTIVE = Color.parseColor("#555555");
}
