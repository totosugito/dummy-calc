package com.calctastic.sample.hypercal.engine.model;

/**
 * Parenthesized expression node ( ... ).
 * Faithful to HiPER Calc IH / C0022Bd.
 *
 * content is a SequenceNode slot (HiPER GA inside C0067Lb), matching FractionNode's
 * numerator/denominator -- see specs/editable_slots_sequencenode.md. This lets any expression
 * (including a nested fraction or power) be inserted inside the parentheses through the same
 * insert/nav/delete paths used everywhere else, instead of needing slot-specific handling.
 */
public class ParenthesisNode extends ExpressionNode {
    public SequenceNode content;

    public ParenthesisNode(ExpressionNode content) {
        this.content = toSlot(content);
        this.content.setParent(this);
    }

    /**
     * Wraps a bare node into a SequenceNode slot. An empty slot holds a single empty
     * {@code NumberNode} -- NOT {@code EmptyNode} (that's the fraction-slot convention, see
     * {@link FractionNode#toSlot}) -- matching how content is actually created elsewhere (always
     * a real, possibly-empty NumberNode, never null or an empty SequenceNode, so this fallback is
     * currently unreached dead code -- but see specs/btn_x_power_y.md Section N for why getting
     * this wrong is a real, visible bug the moment something DOES hit it: a mismatched
     * placeholder type renders via the wrong visual class, at the wrong box size).
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
        return content != null ? 1 : 0;
    }

    @Override
    public ExpressionNode getChild(int index) {
        return index == 0 ? content : null;
    }

    @Override
    public boolean removeChild(ExpressionNode child) {
        if (child == content) {
            content = null;
            return true;
        }
        return false;
    }

    @Override
    public String toLatexString() {
        return "(" + (content != null ? content.toLatexString() : "") + ")";
    }
}
