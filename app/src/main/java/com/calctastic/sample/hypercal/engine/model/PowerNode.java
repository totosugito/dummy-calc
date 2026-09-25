package com.calctastic.sample.hypercal.engine.model;

/**
 * Power/Superscript expression node (e.g. x^y, x^2).
 * Faithful to HiPER Calc C0067Lb with EnumC0300sa.Ca / Mc / K / qB / WB / T.
 *
 * Contains complete method structure:
 * - base and exponent child references with parent hierarchy
 * - navigation, cloning, and type checking
 * - needsParenthesesForBase() 100% faithful to C0067Lb.E$1(null)
 */
public class PowerNode extends ExpressionNode {
    public ExpressionNode base;
    public ExpressionNode exponent;
    public String operationName; // "xʸ", "x²", "x³", "x⁻¹", etc.
    public boolean isClosed = true;

    public PowerNode(ExpressionNode base, ExpressionNode exponent) {
        this(base, exponent, "xʸ");
    }

    public PowerNode(ExpressionNode base, ExpressionNode exponent, String operationName) {
        this.base = base;
        if (base != null) {
            base.setParent(this);
        }
        this.exponent = exponent;
        if (exponent != null) {
            exponent.setParent(this);
        }
        this.operationName = operationName != null ? operationName : "xʸ";
    }

    public ExpressionNode getBase() {
        return base;
    }

    public void setBase(ExpressionNode base) {
        this.base = base;
        if (base != null) {
            base.setParent(this);
        }
    }

    public ExpressionNode getExponent() {
        return exponent;
    }

    public void setExponent(ExpressionNode exponent) {
        this.exponent = exponent;
        if (exponent != null) {
            exponent.setParent(this);
        }
    }

    /**
     * Exact parenthesis requirement logic for base of power from C0067Lb.java E$1(ZB zb).
     * If base is negative, or is an operator sequence / binary expression without parens,
     * base must be enclosed in parentheses when elevated to a power (e.g. (-5)^2).
     */
    public boolean needsParenthesesForBase() {
        if (base == null) {
            return false;
        }
        if (base instanceof NumberNode) {
            // Negative number needs parens e.g. (-2)^x
            return ((NumberNode) base).isNegative();
        }
        if (base instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) base;
            // Compound expression with multiple terms needs parens e.g. (a + b)^2
            return seq.getChildCount() > 1;
        }
        if (base instanceof OperatorNode) {
            return true;
        }
        return false;
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
        if (index == 0) return base;
        if (index == 1) return exponent;
        return null;
    }

    @Override
    public boolean removeChild(ExpressionNode child) {
        if (child == base) {
            base = null;
            return true;
        }
        if (child == exponent) {
            exponent = null;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "PowerNode(base=" + base + ", exponent=" + exponent + ")";
    }

    @Override
    public String toLatexString() {
        String baseStr = base != null ? base.toLatexString() : "";
        String expStr = exponent != null ? exponent.toLatexString() : "";
        if (needsParenthesesForBase()) {
            baseStr = "(" + baseStr + ")";
        }
        return baseStr + "^{" + expStr + "}";
    }
}
