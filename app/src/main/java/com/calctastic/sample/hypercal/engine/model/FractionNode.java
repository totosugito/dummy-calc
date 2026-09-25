package com.calctastic.sample.hypercal.engine.model;

/**
 * Fraction expression node with numerator and denominator.
 * Faithful to HiPER Calc C0067Lb with EnumC0300sa.zb / Fraction rendered by Qg.
 */
public class FractionNode extends ExpressionNode {
    public ExpressionNode numerator;
    public ExpressionNode denominator;

    public FractionNode(ExpressionNode numerator, ExpressionNode denominator) {
        this.numerator = numerator != null ? numerator : new EmptyNode();
        this.numerator.setParent(this);
        this.denominator = denominator != null ? denominator : new EmptyNode();
        this.denominator.setParent(this);
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public ExpressionNode getChild(int index) {
        if (index == 0) return numerator;
        if (index == 1) return denominator;
        return null;
    }

    @Override
    public boolean removeChild(ExpressionNode child) {
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
        return "\\frac{" + num + "}{" + den + "}";
    }
}
