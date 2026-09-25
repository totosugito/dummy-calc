package com.calctastic.sample.calctastic.expression;

/**
 * Shared token tables from CalculatorCommand / Equation.T (auto-paren ordinals 68–86).
 */
public final class CalcTokens {

    private CalcTokens() {}

    /**
     * Functions that Equation.T() auto-appends "(" for (ordinals 68–86 in original).
     * NOT included (87+): ! ² √ e^ 10^ 1/ % -
     */
    public static final String[] AUTO_PAREN_FUNCTIONS = {
        "abs", "ceil", "floor", "ln", "log",
        "sin", "cos", "tan", "asin", "acos", "atan",
        "arg", "conj", "re", "im"
    };

    /** Multi-char operator plains (one entry on backspace). Plain strings from CalculatorCommand. */
    public static final String[] OPERATOR_TOKENS = {
        " Δ% ", " nPr ", " nCr ", " mod ", " + ", " − ", " × ", " / "
    };

    /** Postfix / compact plains with no auto "(" (ordinals 87–95, 102+). */
    public static final String[] POSTFIX_TOKENS = {
        "10^", "e^", "1/", "²", "√", "°", "!"
    };

    public static boolean isAutoParenFunction(String name) {
        for (String f : AUTO_PAREN_FUNCTIONS) {
            if (f.equals(name)) return true;
        }
        return false;
    }

    public static String longestSuffix(String[] candidates, String s) {
        String best = null;
        for (String c : candidates) {
            if (s.endsWith(c) && (best == null || c.length() > best.length())) {
                best = c;
            }
        }
        return best;
    }

}
