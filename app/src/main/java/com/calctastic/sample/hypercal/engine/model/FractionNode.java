package com.calctastic.sample.hypercal.engine.model;

/**
 * Fraction expression node with numerator and denominator.
 * Faithful to HiPER Calc C0067Lb with EnumC0300sa.zb / Fraction rendered by Qg.
 *
 * Numerator and denominator are always SequenceNode slots (HiPER GA inside C0067Lb),
 * so each slot can hold several tokens (e.g. 1+2). An empty slot holds a single
 * EmptyNode (QA) so the placeholder box stays visible.
 *
 * A mixed number ("a b/c" button) is the same C0067Lb node kind reused with a third
 * child slot for the whole-number part (HiPER EnumC0300sa.sa, a 3-child variant of the
 * plain 2-child fraction EnumC0300sa.mB -- see Rc.java:384-392). {@link #integerPart} is
 * null for a plain a/b fraction and non-null for a mixed number; use {@link #createMixed}
 * to build one.
 */
public class FractionNode extends ExpressionNode {
    public SequenceNode integerPart;
    public SequenceNode numerator;
    public SequenceNode denominator;

    public FractionNode(ExpressionNode numerator, ExpressionNode denominator) {
        this.numerator = toSlot(numerator);
        this.numerator.setParent(this);
        this.denominator = toSlot(denominator);
        this.denominator.setParent(this);
    }

    /** Builds a mixed number "integerPart numerator/denominator" (HiPER EnumC0300sa.sa). */
    public static FractionNode createMixed(ExpressionNode integerPart, ExpressionNode numerator, ExpressionNode denominator) {
        FractionNode frac = new FractionNode(numerator, denominator);
        frac.integerPart = toSlot(integerPart);
        frac.integerPart.setParent(frac);
        return frac;
    }

    public boolean isMixed() {
        return integerPart != null;
    }

    private static SequenceNode toSlot(ExpressionNode node) {
        if (node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) node;
            if (seq.getChildCount() == 0) {
                seq.add(new EmptyNode());
            }
            return seq;
        }
        SequenceNode seq = new SequenceNode();
        seq.add(node != null ? node : new EmptyNode());
        return seq;
    }

    /** True when the slot holds only the EmptyNode placeholder. */
    public static boolean isEmptySlot(SequenceNode slot) {
        return slot == null || slot.getChildCount() == 0
                || (slot.getChildCount() == 1 && slot.getChild(0) instanceof EmptyNode);
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public int getChildCount() {
        return isMixed() ? 3 : 2;
    }

    @Override
    public ExpressionNode getChild(int index) {
        if (isMixed()) {
            if (index == 0) return integerPart;
            if (index == 1) return numerator;
            if (index == 2) return denominator;
            return null;
        }
        if (index == 0) return numerator;
        if (index == 1) return denominator;
        return null;
    }

    @Override
    public boolean removeChild(ExpressionNode child) {
        if (child == integerPart) {
            integerPart = null;
            return true;
        }
        if (child == numerator) {
            numerator = null;
            return true;
        }
        if (child == denominator) {
            denominator = null;
            return true;
        }
        return false;
    }

    @Override
    public String toLatexString() {
        String num = numerator != null ? numerator.toLatexString() : "";
        String den = denominator != null ? denominator.toLatexString() : "";
        if (isMixed()) {
            String intStr = integerPart != null ? integerPart.toLatexString() : "";
            return intStr + "\\ \\frac{" + num + "}{" + den + "}";
        }
        return "\\frac{" + num + "}{" + den + "}";
    }
}
