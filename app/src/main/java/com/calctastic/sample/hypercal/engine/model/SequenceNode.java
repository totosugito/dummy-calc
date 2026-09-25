package com.calctastic.sample.hypercal.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Container node representing a sequence of terms (e.g. root or inside parentheses).
 * Modeled after HiPER Calc C0030Db (Group) / GA (BinarySequence).
 */
public class SequenceNode extends ExpressionNode {
    public final List<ExpressionNode> children = new ArrayList<>();

    public SequenceNode() {
    }

    public void add(ExpressionNode node) {
        children.add(node);
        node.setParent(this);
    }

    public void addChild(ExpressionNode node) {
        add(node);
    }

    public void add(int index, ExpressionNode node) {
        children.add(index, node);
        node.setParent(this);
    }

    public int getChildIndex(ExpressionNode node) {
        return children.indexOf(node);
    }

    @Override
    public int getLength() {
        return children.size();
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public ExpressionNode getChild(int index) {
        if (index >= 0 && index < children.size()) {
            return children.get(index);
        }
        return null;
    }

    @Override
    public boolean removeChild(ExpressionNode child) {
        int idx = children.indexOf(child);
        if (idx >= 0) {
            children.remove(idx);
            child.setParent(null);
            return true;
        }
        return false;
    }

    public void clear() {
        children.clear();
        cursorPosition = -1;
    }

    @Override
    public String toLatexString() {
        StringBuilder sb = new StringBuilder();
        for (ExpressionNode child : children) {
            sb.append(child.toLatexString());
        }
        return sb.toString();
    }
}
