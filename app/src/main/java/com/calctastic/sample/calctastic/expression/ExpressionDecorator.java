package com.calctastic.sample.calctastic.expression;

/**
 * Angle-unit / hyperbolic display decoration — mirrors CalculatorCommand.C()
 * (equationStringStyled + AngleUnit.modifier or &lt;sup&gt;h&lt;/sup&gt;).
 */
public final class ExpressionDecorator {

    private ExpressionDecorator() {}

    /**
     * Display decoration — mirrors CalculatorCommand.C():
     *   hyperbolic → equationStringStyled + "&lt;sup&gt;&lt;o&gt;h&lt;/o&gt;&lt;/sup&gt;"
     *   else       → equationStringStyled + angleUnit.modifier  (d/r/g)
     * T.a.b(str, mod) is plain concatenation after the function name.
     * The "(" is a separate PARENTH_OPEN entry, so after formatEquation
     * the text is "sin&lt;p&gt;(&lt;/p&gt;" — match after the name, not "sin(".
     *
     * @param plainCursor cursor index in pre-decoration plain text (from formatEquation)
     * @param outCursor  [0] receives cursor index in post-decoration visible text
     */
    public static String decorateAngleFunctions(String tagged, int plainCursor, int[] outCursor, String angleUnit, boolean hyperbolic) {
        if (outCursor != null && outCursor.length > 0) {
            outCursor[0] = plainCursor;
        }
        if (tagged == null || tagged.isEmpty()) return tagged;
        String sup;
        if (hyperbolic) {
            sup = "<sup><o>h</o></sup>";
        } else if ("DEG".equals(angleUnit)) {
            sup = "<sup><o>d</o></sup>";
        } else if ("RAD".equals(angleUnit)) {
            sup = "<sup><o>r</o></sup>";
        } else {
            sup = "<sup><o>g</o></sup>";
        }
        // Visible chars inserted per decoration (tags stripped): "d"/"r"/"g"/"h"
        final int visibleLen = 1;
        int extraBeforeCursor = 0;
        // Longest first so "asin" is not partially matched by "sin"
        String[] fns = { "asin", "acos", "atan", "sin", "cos", "tan", "arg" };
        for (String fn : fns) {
            int idx = 0;
            while (true) {
                int i = tagged.indexOf(fn, idx);
                if (i < 0) break;
                if (i > 0 && Character.isLetterOrDigit(tagged.charAt(i - 1))) {
                    idx = i + fn.length();
                    continue;
                }
                int after = i + fn.length();
                if (tagged.startsWith("<sup>", after)) {
                    idx = after;
                    continue;
                }
                if (after < tagged.length() && Character.isLetter(tagged.charAt(after))) {
                    idx = after;
                    continue;
                }
                // Count how many visible plain chars appear before this insert point
                // (equals plain length of prefix up to `after`)
                int plainBefore = stripTags(tagged.substring(0, after)).length();
                if (plainBefore < plainCursor) {
                    extraBeforeCursor += visibleLen;
                }
                tagged = tagged.substring(0, after) + sup + tagged.substring(after);
                idx = after + sup.length();
            }
        }
        if (outCursor != null && outCursor.length > 0) {
            outCursor[0] = plainCursor + extraBeforeCursor;
        }
        return tagged;
    }

    /** Remove CalcTastic markup tags to get visible/plain length (q0.g / EquationPrintout). */
    static String stripTags(String s) {
        return s.replaceAll("</?[^/<>]+>", "");
    }

    /** Back-compat: decorate without cursor adjustment (history path). */
    public static String decorateAngleFunctions(String tagged, String angleUnit, boolean hyperbolic) {
        return decorateAngleFunctions(tagged, Integer.MAX_VALUE, null, angleUnit, hyperbolic);
    }

}
