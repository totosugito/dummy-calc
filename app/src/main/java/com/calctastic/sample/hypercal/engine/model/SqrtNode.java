package com.calctastic.sample.hypercal.engine.model;

/**
 * Radical expression node (Square root or N-th root).
 * Faithful to HiPER Calc C0067Lb with EnumC0300sa.x / ab.
 */
public class SqrtNode extends ExpressionNode {
    public ExpressionNode degree;   // optional, e.g. 3 in cube root
    public ExpressionNode radicand; // content under radical line

    public SqrtNode(ExpressionNode radicand) {
        this(null, radicand);
    }

    public SqrtNode(ExpressionNode degree, ExpressionNode radicand) {
        this.degree = degree;
        if (degree != null) degree.setParent(this);
        this.radicand = radicand;
        if (radicand != null) radicand.setParent(this);
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public int getChildCount() {
        return (degree != null ? 1 : 0) + (radicand != null ? 1 : 0);
    }

    @Override
    public ExpressionNode getChild(int index) {
        if (degree != null) {
            if (index == 0) return degree;
            if (index == 1) return radicand;
        } else {
            if (index == 0) return radicand;
        }
        return null;
    }

    @Override
    public boolean removeChild(ExpressionNode child) {
        if (child == degree) {
            degree = null;
            return true;
        }
        if (child == radicand) {
            radicand = null;
            return true;
        }
        return false;
    }

    @Override
    public String toLatexString() {
        String rad = radicand != null ? radicand.toLatexString() : "";
        if (degree != null) {
            return "\\sqrt[" + degree.toLatexString() + "]{" + rad + "}";
        }
        return "\\sqrt{" + rad + "}";
    }
}
