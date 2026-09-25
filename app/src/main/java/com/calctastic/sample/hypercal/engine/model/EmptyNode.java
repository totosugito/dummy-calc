package com.calctastic.sample.hypercal.engine.model;

/**
 * Empty expression placeholder node (square box).
 * Faithful to HiPER Calc android.core.QA.java.
 */
public class EmptyNode extends ExpressionNode {

    public EmptyNode() {
    }

    @Override
    public int getLength() {
        return 0;
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

    @Override
    public String toLatexString() {
        return "\\square";
    }
}
