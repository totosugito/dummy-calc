package com.calctastic.sample.calctastic.ui;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Font size dinamis untuk history item — port dari p018j0/a.java (BUTTON_FONTSIZE_*)
 * dan p018j0/b.java (FONTSIZE_LIST_*).
 *
 * Layout selection (p005c0/b.java:550–578):
 *   phone portrait (aspect>1.5, widthDp<500) → ONE_COLUMN + PORTRAIT
 *   → keyboardToWidthRatio=1.0, columnsMap=5.0, ratioToWidth=0.42
 *
 * baseline = ((appWidthDp * 1.0) / 5.0) * 0.42
 * size     = max(baseline * ratioMap[portrait], minSizeDp)
 *
 * ratioMap portrait (ordinal 1):
 *   DESCRIPTION_HISTORY: 0.67, min 17
 *   STACK:               0.80, min 18
 */
public final class CalcFontSizeHelper {

    private CalcFontSizeHelper() {}

    /** FONTSIZE_LIST_DESCRIPTION_HISTORY — expression / editable input. */
    public static float expressionSp(Context context) {
        return listSp(context, 0.67f, 17f);
    }

    /** FONTSIZE_LIST_STACK — result (lebih besar). */
    public static float resultSp(Context context) {
        return listSp(context, 0.80f, 18f);
    }

    private static float listSp(Context context, float ratioMapPortrait, float minSizeDp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float appWidthDp = dm.widthPixels / dm.density;

        // ONE_COLUMN (p018j0/f.java:7) + PORTRAIT columnsMap (p018j0/a.java:7)
        float keyboardToWidthRatio = 1.0f;
        float columnsMapPortrait = 5.0f;
        float ratioToWidth = 0.42f;
        float baseline = ((appWidthDp * keyboardToWidthRatio) / columnsMapPortrait) * ratioToWidth;

        return Math.max(baseline * ratioMapPortrait, minSizeDp);
    }
}
