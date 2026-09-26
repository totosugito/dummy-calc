package com.calctastic.sample.hypercal.view;

import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Dynamic screen-scale math for the HiPER display, faithful to C0332vh.onMeasure (lines 797-811)
 * and Tg.HiPER(V.HiPER). Pulled out of HyperCalDisplayView (2026-09-26) since it's pure math with
 * no View/touch/draw concerns -- see specs/display_scaling_typography.md Task 7 for the decompile
 * findings this implements.
 */
final class HyperCalScale {
    // HiPER Calc nominal base size from AbstractC0293re.java line 144: "100" -> 14.0f
    static final float NOMINAL_BASE_SIZE = 14.0f;

    // HiPER keypad reference size in design units: C0341wd.mo344HiPER() with AbstractC0060Ig theme values
    //   x = max(("41" + "39_F") * 5, ("33" + "30_F") * 4) + "6" + "8" = max(255, 256) + 8 = 264
    //   y = ("42_F" + "40_F") * 3 + ("34_F" + "31_F") * 5 + "7" + "9" + "11" = 90 + 190 + 5 = 285
    // C0332vh.java line 772 then adds 2 * "1" to both axes.
    private static final float REF_KEYPAD_WIDTH = 266.0f;
    private static final float REF_KEYPAD_HEIGHT = 287.0f;
    // Theme padding "95" (AbstractC0060Ig line 58) and UF.E = 3.6 editor lines (UF.java line 78)
    private static final float DISPLAY_PADDING = 4.0f;
    private static final float EDITOR_LINES = 3.6f;

    private HyperCalScale() {
    }

    /** Line height (-ascent + descent) of the given font at a nominal design size (scale 1.0). */
    static float lineHeight(Typeface typeface, float size) {
        Paint p = new Paint();
        p.setTypeface(typeface);
        p.setTextSize(size);
        return -p.ascent() + p.descent();
    }

    /**
     * Reference display height M at scale 1.0, after GestureDetectorOnGestureListenerC0122aI.G():
     * 2 * "95" padding + header line ("102" = 8) + UF.d() (3.6 lines of "100" = 14)
     * + result line ("103" = 15) + status line ("102" = 8) + "95".
     * The header rect (m283HiPER) is approximated by one "102" line.
     */
    static float referenceDisplayHeight(Typeface typeface) {
        return 2.0f * DISPLAY_PADDING
                + lineHeight(typeface, 8.0f)
                + EDITOR_LINES * lineHeight(typeface, NOMINAL_BASE_SIZE)
                + lineHeight(typeface, 15.0f)
                + lineHeight(typeface, 8.0f) + DISPLAY_PADDING;
    }

    /**
     * Dynamic screen scale faithful to C0332vh.onMeasure (lines 797-811) and Tg.HiPER(V.HiPER):
     *   f  = point.x / pointF.x
     *   f2 = point.y / (pointF.y + M), capped at 1.2f * f
     * Tg.HiPER(V.HiPER) returns f2 (field H), which AbstractC0335wD.k() multiplies with D.
     * areaWidth/areaHeight are the whole calculator area (activity content), not just the display.
     */
    static float computeTextSize(int areaWidth, int areaHeight, Typeface typeface) {
        float f = areaWidth / REF_KEYPAD_WIDTH;
        float f2 = areaHeight / (REF_KEYPAD_HEIGHT + referenceDisplayHeight(typeface));
        float f3 = 1.2f * f;
        if (f2 > f3) {
            f2 = f3;
        }
        // HiPER AbstractC0293re.java line 527: textSize = k() * c0215jD.c, k() = Tg.HiPER(V) * D
        return f2 * NOMINAL_BASE_SIZE;
    }
}
