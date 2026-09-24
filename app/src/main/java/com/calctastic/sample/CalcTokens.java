package com.calctastic.sample;

/**
 * Shared token tables from CalculatorCommand / Equation.T (auto-paren ordinals 68–86).
 */
final class CalcTokens {

    private CalcTokens() {}

    /**
     * Functions that Equation.T() auto-appends "(" for (ordinals 68–86 in original).
     * NOT included (87+): ! ² √ e^ 10^ 1/ % -
     */
    static final String[] AUTO_PAREN_FUNCTIONS = {
        "abs", "ceil", "floor", "ln", "log",
        "sin", "cos", "tan", "asin", "acos", "atan",
        "arg", "conj", "re", "im"
    };

    /** Multi-char operator plains (one entry on backspace). Plain strings from CalculatorCommand. */
    static final String[] OPERATOR_TOKENS = {
        " Δ% ", " nPr ", " nCr ", " mod ", " + ", " − ", " × ", " / "
    };

    /** Postfix / compact plains with no auto "(" (ordinals 87–95, 102+). */
    static final String[] POSTFIX_TOKENS = {
        "10^", "e^", "1/", "²", "√", "°", "!"
    };

    static boolean isAutoParenFunction(String name) {
        for (String f : AUTO_PAREN_FUNCTIONS) {
            if (f.equals(name)) return true;
        }
        return false;
    }

    static String longestSuffix(String[] candidates, String s) {
        String best = null;
        for (String c : candidates) {
            if (s.endsWith(c) && (best == null || c.length() > best.length())) {
                best = c;
            }
        }
        return best;
    }

}
