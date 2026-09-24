package com.calctastic.sample.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import androidx.core.content.res.ResourcesCompat;
import com.calctastic.sample.R;

public final class CalcTypefaceHelper {

    private static Typeface sMonoFont;
    private static Typeface sSansMediumFont;

    private CalcTypefaceHelper() {}

    public static Typeface getFont(Context context, int fontResId) {
        try {
            return ResourcesCompat.getFont(context, fontResId);
        } catch (Exception e) {
            Log.e("CalcTypefaceHelper", "Could not load font", e);
            return Typeface.DEFAULT;
        }
    }

    public static Typeface getScreenCalculationFont(Context context) {
        if (sMonoFont == null) {
            sMonoFont = getFont(context, R.font.font_roboto_mono_variable);
        }
        return sMonoFont;
    }

    public static Typeface getSymbolAndLabelFont(Context context) {
        if (sSansMediumFont == null) {
            sSansMediumFont = getFont(context, R.font.font_inter_medium);
        }
        return sSansMediumFont;
    }
}
