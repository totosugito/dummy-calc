package com.calctastic.sample.calctastic.expression;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of CalcTastic's p0.e formatting engine that processes markup tags
 * like <o>operator</o>, <p>parenthesis</p>, <b>bold</b>, <i>italic</i>, etc.
 */
public final class CalcSpannableFormatter {

    public static final Pattern TAG_PATTERN = Pattern.compile("<(.+?)>(.*?)</\\1>");

    // Default theme colors matching CalcTastic Theme (Modern/Monokai/Rustic)
    // Operator color: Vibrant Orange (#FF9800)
    // Parenthesis color: Soft Amber / Gold (#FFD54F)
    // Function/Error color: Red (#EF5350)
    public static int OPERATOR_COLOR = Color.parseColor("#FF9800");
    public static int PARENTHESIS_COLOR = Color.parseColor("#FFCA28");
    public static int ERROR_COLOR = Color.parseColor("#EF5350");

    public static SpannableStringBuilder format(String str) {
        if (str == null) return new SpannableStringBuilder("");
        SpannableStringBuilder ssb = new SpannableStringBuilder(str);
        if (!str.contains("</")) {
            return ssb;
        }

        Matcher matcher = TAG_PATTERN.matcher(ssb);
        while (matcher.find()) {
            String tag = matcher.group(1);
            String content = matcher.group(2);
            int start = matcher.start();
            ssb.replace(start, matcher.end(), content);
            int end = start + content.length();

            switch (tag) {
                case "o": // Operator tag from CalcTastic
                    ssb.setSpan(new ForegroundColorSpan(OPERATOR_COLOR), start, end, 33);
                    break;
                case "p": // Parenthesis tag from CalcTastic
                    ssb.setSpan(new ForegroundColorSpan(PARENTHESIS_COLOR), start, end, 33);
                    break;
                case "b": // Bold
                    ssb.setSpan(new StyleSpan(1), start, end, 33);
                    break;
                case "i": // Italic
                    ssb.setSpan(new StyleSpan(2), start, end, 33);
                    break;
                case "e": // Error
                    ssb.setSpan(new ForegroundColorSpan(ERROR_COLOR), start, end, 33);
                    break;
                case "sup": // Superscript (fraction numerator or power)
                    ssb.setSpan(new android.text.style.RelativeSizeSpan(0.75f), start, end, 33);
                    ssb.setSpan(new android.text.style.SuperscriptSpan(), start, end, 33);
                    break;
                case "sub": // Subscript
                    ssb.setSpan(new android.text.style.RelativeSizeSpan(0.75f), start, end, 33);
                    ssb.setSpan(new android.text.style.SubscriptSpan(), start, end, 33);
                    break;
                case "sf": // Small font (fraction denominator)
                    ssb.setSpan(new android.text.style.RelativeSizeSpan(0.75f), start, end, 33);
                    break;
                case "hw": // Half-width space (for mixed fractions)
                    ssb.setSpan(new android.text.style.ScaleXSpan(0.5f), start, end, 33);
                    break;
                case "dim": // Dimmed placeholder text
                    ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#777777")), start, end, 33);
                    break;
            }
            matcher.reset(ssb);
        }
        return ssb;
    }
}
