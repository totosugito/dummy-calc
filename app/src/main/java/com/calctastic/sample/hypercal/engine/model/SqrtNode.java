package com.calctastic.sample.hypercal.engine.model;

/**
 * Radical expression node (Square root or N-th root).
 * Faithful to HiPER Calc C0067Lb with EnumC0300sa.x / ab.
 *
 * degree/radicand are SequenceNode slots (HiPER GA inside C0067Lb), matching FractionNode's
 * numerator/denominator -- see specs/editable_slots_sequencenode.md. This lets any expression
 * (including a nested fraction or a power) be inserted under the radical through the same
 * insert/nav/delete paths used everywhere else, instead of needing slot-specific handling.
 */
public class SqrtNode extends ExpressionNode {
    public SequenceNode degree;   // optional (nullable), e.g. 3 in cube root
    public SequenceNode radicand; // content under radical line

    public SqrtNode(ExpressionNode radicand) {
        this(null, radicand);
    }

    public SqrtNode(ExpressionNode degree, ExpressionNode radicand) {
        this.degree = degree != null ? toSlot(degree) : null;
        if (this.degree != null) {
            this.degree.setParent(this);
        }
        this.radicand = toSlot(radicand);
        this.radicand.setParent(this);
    }

    /**
     * Wraps a bare node into a SequenceNode slot. An empty slot holds a single empty
     * {@code NumberNode} -- NOT {@code EmptyNode} (that's the fraction-slot convention, see
     * {@link FractionNode#toSlot}) -- matching how radicand/degree are actually created elsewhere
     * (always a real, possibly-empty NumberNode, never null or an empty SequenceNode, so this
     * fallback is currently unreached dead code -- but see specs/btn_x_power_y.md Section N for
     * why getting this wrong is a real, visible bug the moment something DOES hit it: a
     * mismatched placeholder type renders via the wrong visual class, at the wrong box size).
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
