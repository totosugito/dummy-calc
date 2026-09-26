package com.calctastic.sample.hypercal.engine.model;

/**
 * Power/Superscript expression node (e.g. x^y, x^2).
 * Faithful to HiPER Calc C0067Lb with EnumC0300sa.Ca / Mc / K / qB / WB / T.
 *
 * base/exponent are SequenceNode slots (HiPER GA inside C0067Lb), matching FractionNode's
 * numerator/denominator -- see specs/editable_slots_sequencenode.md. This lets any expression
 * (including a nested fraction or another power) be inserted into either slot through the same
 * insert/nav/delete paths used everywhere else, instead of needing slot-specific handling.
 */
public class PowerNode extends ExpressionNode {
    public SequenceNode base;
    public SequenceNode exponent;
    public String operationName; // "xʸ", "x²", "x³", "x⁻¹", etc.
    public boolean isClosed = true;

    public PowerNode(ExpressionNode base, ExpressionNode exponent) {
        this(base, exponent, "xʸ");
    }

    public PowerNode(ExpressionNode base, ExpressionNode exponent, String operationName) {
        this.base = toSlot(base);
        this.base.setParent(this);
        this.exponent = toSlot(exponent);
        this.exponent.setParent(this);
        this.operationName = operationName != null ? operationName : "xʸ";
    }

    /**
     * Wraps a bare node into a SequenceNode slot. An empty slot holds a single empty
     * {@code NumberNode} -- NOT {@code EmptyNode} (that's the fraction-slot convention, see
     * {@link FractionNode#toSlot}) -- matching how every power base/exponent is actually created
     * elsewhere (PowerInserter always passes a real, possibly-empty NumberNode, never null or an
     * empty SequenceNode, so this fallback is currently unreached dead code -- but see
     * specs/btn_x_power_y.md Section N for why getting this wrong is a real, visible bug the
     * moment something DOES hit it: a mismatched placeholder type renders via the wrong visual
     * class, at the wrong box size).
     */
    private static SequenceNode toSlot(ExpressionNode node) {
        if (node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) node;
            if (seq.getChildCount() == 0) {
                seq.add(new NumberNode(""));
            }
            return seq;
        }
        SequenceNode seq = new SequenceNode();
        seq.add(node != null ? node : new NumberNode(""));
        return seq;
    }

    public SequenceNode getBase() {
        return base;
    }

    public void setBase(SequenceNode base) {
        this.base = base;
        if (base != null) {
            base.setParent(this);
        }
    }

    public SequenceNode getExponent() {
        return exponent;
    }

    public void setExponent(SequenceNode exponent) {
        this.exponent = exponent;
        if (exponent != null) {
            exponent.setParent(this);
        }
    }

    /**
     * Exact parenthesis requirement logic for base of power from C0067Lb.java E$1(ZB zb).
     * If base is negative, or is an operator sequence / binary expression without parens,
     * base must be enclosed in parentheses when elevated to a power (e.g. (-5)^2).
     *
     * base is now always a SequenceNode slot, so this looks at the slot's content: more than
     * one token means a compound expression (needs parens), a single negative number or a bare
     * operator token also needs parens, anything else (including an empty/placeholder slot)
     * doesn't.
     */
    public boolean needsParenthesesForBase() {
        if (base == null || base.getChildCount() == 0) {
            return false;
        }
        if (base.getChildCount() > 1) {
            // Compound expression with multiple terms needs parens e.g. (a + b)^2
            return true;
        }
        ExpressionNode only = base.getChild(0);
        if (only instanceof NumberNode) {
            // Negative number needs parens e.g. (-2)^x
            return ((NumberNode) only).isNegative();
        }
        return only instanceof OperatorNode;
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
