package com.calctastic.sample.calctastic.expression;

/**
 * DMS entry point for the sample calculator.
 * Full DMS state machine lives in DegreeString + DegreeMinuteSecond (ports of raw sources).
 *
 * AlgebraicInputHandler case 103 applies Q(103) only to the current/last numeric
 * operand — convertTrailing extracts that operand, pressDms runs the original cycle.
 */
public final class DmsHelper {

    private DmsHelper() {}

    /**
     * Apply one DMS press to the trailing operand before endIdx.
     * Returns full new expression, or null if no operand / no change.
     */
    public static String convertTrailing(String expr, int endIdx) {
        if (expr == null || endIdx <= 0 || endIdx > expr.length()) return null;
        String before = expr.substring(0, endIdx);
        String after = expr.substring(endIdx);
        if (before.isEmpty()) return null;

        char last = before.charAt(before.length() - 1);
        boolean endsOperand = Character.isDigit(last) || last == '.' || last == '°'
                || last == '\'' || last == '"';
        if (!endsOperand) return null; // after operator — original returns without change

        // Scan back over number + DMS markers + exponent
        int i = before.length();
        int start = i;
        while (start > 0) {
            char c = before.charAt(start - 1);
            if (Character.isDigit(c) || c == '.' || c == ','
                    || c == '°' || c == '\'' || c == '"'
                    || c == 'E' || c == 'e') {
                start--;
            } else if (c == '+' || c == '-') {
                if (start == 1) {
                    start--;
                    break;
                }
                char prev = before.charAt(start - 2);
                if (prev == '(' || prev == '+' || prev == '-' || prev == '×'
                        || prev == '÷' || prev == '/' || prev == '−'
                        || prev == ' ' || prev == '^' || prev == '%') {
                    start--;
                }
                break;
            } else {
                break;
            }
        }

        String operand = before.substring(start, i);
        if (operand.isEmpty()) return null;

        String next = DegreeMinuteSecond.pressDms(operand);
        if (next == null || next.equals(operand)) return null;
        return before.substring(0, start) + next + after;
    }
}
