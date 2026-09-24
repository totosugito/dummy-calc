package com.calctastic.sample.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import androidx.core.content.res.ResourcesCompat;
import com.calctastic.sample.R;

public final class CalcTypefaceHelper {

    private static Typeface sMonoFont;
    private static Typeface sSansMediumFont;
    private static Typeface sSansRegularFont;
    private static Typeface sSerif1Font;
    private static Typeface sSerif2Font;

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

    /** SANS2 — digit keys (CalculatorCommand font id). */
    public static Typeface getSansRegular(Context context) {
        if (sSansRegularFont == null) {
            sSansRegularFont = getFont(context, R.font.font_inter_regular);
        }
        return sSansRegularFont;
    }

    /** MONO1 — `( ) i ∠` (p007d0/f.java). */
    public static Typeface getMono(Context context) {
        return getScreenCalculationFont(context);
    }

    /** SERI1 — `= π ± + − × ÷` (STIX Two Text). */
    public static Typeface getSerifOperator(Context context) {
        if (sSerif1Font == null) {
            sSerif1Font = getFont(context, R.font.font_stix_two_text_medium);
        }
        return sSerif1Font;
    }

    /** SERI2 — `a/b x² yˣ 1/x √ .` (Hepta Slab). */
    public static Typeface getSerifSlab(Context context) {
        if (sSerif2Font == null) {
            sSerif2Font = getFont(context, R.font.font_hepta_slab_medium);
        }
        return sSerif2Font;
    }

    /**
     * Apply font by CalculatorCommand.font id (f.java:31–41).
     * @param fontId MONO1 | SANS1 | SANS2 | SERI1 | SERI2
     */
    public static void applyFontId(android.widget.TextView view, String fontId, int style) {
        if (view == null || fontId == null) return;
        Context c = view.getContext();
        Typeface tf;
        switch (fontId) {
            case "MONO1":
                tf = getMono(c);
                break;
            case "SANS2":
                tf = getSansRegular(c);
                break;
            case "SERI1":
                tf = getSerifOperator(c);
                break;
            case "SERI2":
                tf = getSerifSlab(c);
                break;
            case "SANS1":
            default:
                tf = getSymbolAndLabelFont(c);
                break;
        }
        view.setTypeface(tf, style);
    }
}
