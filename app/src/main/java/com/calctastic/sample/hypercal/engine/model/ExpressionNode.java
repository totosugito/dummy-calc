package com.calctastic.sample.hypercal.engine.model;

/**
 * Base abstract class for expression syntax tree nodes.
 * Recreated faithful to android.core.ZB from HiPER Calc.
 */
public abstract class ExpressionNode {
    public ExpressionNode parent;
    public int cursorPosition = -1; // -1 means cursor not in this node

    public ExpressionNode() {
    }

    public ExpressionNode getParent() {
        return parent;
    }

    public void setParent(ExpressionNode parent) {
        this.parent = parent;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int pos) {
        this.cursorPosition = pos;
    }

    public abstract int getLength();

    public abstract int getChildCount();

    public abstract ExpressionNode getChild(int index);

    public abstract boolean removeChild(ExpressionNode child);

    public boolean isContainer() {
        return getChildCount() > 0;
    }

    public abstract String toLatexString();
}
