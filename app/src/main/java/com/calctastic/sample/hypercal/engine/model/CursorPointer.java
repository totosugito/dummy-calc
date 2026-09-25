package com.calctastic.sample.hypercal.engine.model;

/**
 * Cursor pointer pointing to a specific node and character/term index.
 * Recreated faithful to android.core.C0157eA from HiPER Calc.
 */
public final class CursorPointer {
    public final ExpressionNode node;
    public final int position;

    public CursorPointer(ExpressionNode node, int position) {
        this.node = node;
        this.position = position;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CursorPointer)) return false;
        CursorPointer other = (CursorPointer) obj;
        return this.node == other.node && this.position == other.position;
    }

    @Override
    public int hashCode() {
        return (node != null ? node.hashCode() * 31 : 0) + position;
    }

    @Override
    public String toString() {
        return "CursorPointer(node=" + node + ", pos=" + position + ")";
    }
}
