package com.calctastic.sample.hypercal.engine.model;

/**
 * Representation of a numeric input node.
 * Faithful implementation of android.core.C0351xb & android.core.dd from HiPER Calc.
 */
public class NumberNode extends ExpressionNode {
    public StringBuilder text = new StringBuilder();
    public StringBuilder exponent = new StringBuilder();
    public boolean hasExponent = false;

    public NumberNode() {
    }

    public NumberNode(String initial) {
        text.append(initial);
    }

    @Override
    public int getLength() {
        if (!hasExponent) {
            return text.length();
        }
        return 1000 + exponent.length();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public ExpressionNode getChild(int index) {
        return null;
    }

    @Override
    public boolean removeChild(ExpressionNode child) {
        return false;
    }

    /**
     * Recreates HiPER Calc C0351xb.F(boolean) for toggle negate (±).
     */
    public void toggleNegate() {
        StringBuilder sb = hasExponent ? exponent : text;
        if (sb.length() > 0 && (sb.charAt(0) == '-' || sb.charAt(0) == '\u2212')) {
            sb.deleteCharAt(0);
            if (cursorPosition > 0) {
                cursorPosition--;
            }
        } else {
            sb.insert(0, '-');
            if (cursorPosition >= 0) {
                cursorPosition++;
            }
        }
    }

    public void insertChar(char c) {
        StringBuilder sb = hasExponent ? exponent : text;
        int maxLen = hasExponent ? 1000 + exponent.length() : text.length();
        int pos = cursorPosition;
        if (pos < 0 || pos > sb.length()) {
            pos = sb.length();
        }
        sb.insert(pos, c);
        cursorPosition = pos + 1;
    }

    public boolean deleteChar() {
        StringBuilder sb = hasExponent ? exponent : text;
        int pos = cursorPosition;
        if (pos < 0) {
            pos = sb.length();
        }
        if (pos > 0 && sb.length() > 0) {
            sb.deleteCharAt(pos - 1);
            cursorPosition = pos - 1;
            return true;
        }
        return false;
    }

    public boolean isNegative() {
        StringBuilder sb = hasExponent ? exponent : text;
        return sb.length() > 0 && (sb.charAt(0) == '-' || sb.charAt(0) == '\u2212');
    }

    public String getText() {
        return text.toString();
    }

    @Override
    public String toLatexString() {
        String base = text.toString();
        if (hasExponent) {
            return base + "e" + exponent.toString();
        }
        return base;
    }
}
