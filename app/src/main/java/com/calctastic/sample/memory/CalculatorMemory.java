package com.calctastic.sample.memory;

/**
 * Calculator memory register 0 — mirrors CalcMemory + Calculator MEMORY_SAVE/PLUS/MINUS/CLEAR.
 */
public class CalculatorMemory {

    /** Register 0 value as plain string; null = empty (CalcMemory.g()). */
    public String value = null;

    /** Current operand as plain string — live result preferred, else raw input (like inputHandler.e()). */
    String currentOperandString(String liveResult, String currentInput) {
        if (liveResult != null && !liveResult.isEmpty()) {
            return liveResult.replaceFirst("^<o>=</o>\\s*", "").trim();
        }
        return currentInput.toString().trim();
    }

    static double parseOperand(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String formatMem(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    /** MEMORY_SAVE (ordinal 44): store current operand into register 0. */
    public void memorySave(String liveResult, String currentInput) {
        String cur = currentOperandString(liveResult, currentInput);
        if (!cur.isEmpty()) {
            value = formatMem(parseOperand(cur));
        }
    }

    /**
     * MEMORY_PLUS/MINUS (ordinals 48/49), Calculator.java:155–165:
     * mem0 = (mem0 or identity) ± currentOperand; store to register 0.
     */
    public void memoryPlusMinus(boolean plus, String liveResult, String currentInput) {
        double cur = parseOperand(currentOperandString(liveResult, currentInput));
        double mem = (value == null || value.isEmpty()) ? 0 : parseOperand(value);
        double result = plus ? mem + cur : mem - cur;
        value = formatMem(result);
    }

}
