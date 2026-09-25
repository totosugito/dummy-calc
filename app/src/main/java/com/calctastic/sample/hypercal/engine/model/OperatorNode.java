package com.calctastic.sample.hypercal.engine.model;

/**
 * Representation of an operator token (e.g. +, −, ×, ÷, %).
 * Modeled after HiPER Calc SG / EnumC0300sa.
 */
public class OperatorNode extends ExpressionNode {
    public final String symbol;

    public OperatorNode(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public int getLength() {
        return 1;
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
        return symbol != null ? " " + symbol + " " : "";
    }
}
