package com.calctastic.sample.hypercal.engine.model;

/**
 * Parenthesized expression node ( ... ).
 * Faithful to HiPER Calc IH / C0022Bd.
 */
public class ParenthesisNode extends ExpressionNode {
    public ExpressionNode content;

    public ParenthesisNode(ExpressionNode content) {
        this.content = content;
        if (content != null) content.setParent(this);
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
