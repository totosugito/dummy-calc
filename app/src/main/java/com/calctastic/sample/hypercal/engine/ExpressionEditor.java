package com.calctastic.sample.hypercal.engine;

import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.EmptyNode;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.FractionNode;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.OperatorNode;
import com.calctastic.sample.hypercal.engine.model.ParenthesisNode;
import com.calctastic.sample.hypercal.engine.model.PowerNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Keypad-driven editing of the expression AST: insertion, deletion, and left/right cursor
 * navigation. Holds the expression root and the cursor itself, but has no Android dependency
 * (no View, no Paint) so it can be exercised without an Activity/View.
 *
 * Moved out of HyperCalActivity (2026-09-26) to separate "editing the model" from "wiring up
 * the UI" -- see specs/btn_fraction.md and specs/display_scaling_typography.md for pointers to
 * where fraction/cursor logic now lives.
 *
 * Up/down cursor navigation (▲/▼) is NOT here: it depends on the laid-out visual tree
 * (HyperCalDisplayView.findVerticalCursorTarget), which needs a Paint/font-metrics-backed
 * layout this class deliberately doesn't have. HyperCalActivity computes that target and calls
 * {@link #setCursorPointer(CursorPointer)} with it.
 */
public class ExpressionEditor {
    private SequenceNode rootSequence;
    private CursorPointer cursorPointer;

    public ExpressionEditor(SequenceNode rootSequence, CursorPointer initialCursor) {
        this.rootSequence = rootSequence;
        this.cursorPointer = initialCursor;
    }

    public SequenceNode getRootSequence() {
        return rootSequence;
    }

    public CursorPointer getCursorPointer() {
        return cursorPointer;
    }

    public void setCursorPointer(CursorPointer pointer) {
        this.cursorPointer = pointer;
    }

    private void setCursor(CursorPointer pointer) {
        this.cursorPointer = pointer;
    }

    public void reset() {
        rootSequence = new SequenceNode();
        cursorPointer = new CursorPointer(rootSequence, 0);
    }

    // ---- Fraction slot helpers: numerator/denominator are SequenceNode slots (HiPER GA inside C0067Lb) ----

    /** Returns the fraction owning this slot sequence, or null if it is not a fraction slot. */
    private static FractionNode fractionOfSlot(ExpressionNode slot) {
        if (slot instanceof SequenceNode && slot.getParent() instanceof FractionNode) {
            return (FractionNode) slot.getParent();
        }
        return null;
    }

    private static CursorPointer afterNode(ExpressionNode node) {
        if (node instanceof FractionNode) return new CursorPointer(node, 1); // Center after fraction
        if (node instanceof EmptyNode) return new CursorPointer(node, 0);
        return new CursorPointer(node, node.getLength());
    }

    private static CursorPointer startOf(SequenceNode seq) {
        if (seq.getChildCount() == 0) return new CursorPointer(seq, 0);
        return new CursorPointer(seq.getChild(0), 0);
    }

    private static CursorPointer endOf(SequenceNode seq) {
        if (seq.getChildCount() == 0) return new CursorPointer(seq, 0);
        return afterNode(seq.getChild(seq.getChildCount() - 1));
    }

    /**
     * Inserts a new token at the cursor inside the cursor's own sequence.
     * An EmptyNode placeholder under the cursor is replaced (QA -> real token).
     */
    private void insertAtCursor(ExpressionNode node) {
        if (cursorPointer == null || cursorPointer.node == null) {
            rootSequence.add(node);
            return;
        }
        if (cursorPointer.node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) cursorPointer.node;
            int idx = Math.max(0, Math.min(cursorPointer.position, seq.getChildCount()));
            seq.add(idx, node);
            return;
        }
        ExpressionNode parent = cursorPointer.node.getParent();
        if (parent instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) parent;
            int idx = seq.getChildIndex(cursorPointer.node);
            if (cursorPointer.node instanceof EmptyNode) {
                seq.removeChild(cursorPointer.node);
                seq.add(idx, node);
                return;
            }
            seq.add(cursorPointer.position == 0 ? idx : idx + 1, node);
            return;
        }
        rootSequence.add(node);
    }

    public void appendDigit(char c) {
        if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            num.insertChar(c);
            setCursor(new CursorPointer(num, num.cursorPosition));
            return;
        }
        NumberNode num = new NumberNode(String.valueOf(c));
        num.cursorPosition = 1;
        insertAtCursor(num);
        setCursor(new CursorPointer(num, 1));
    }

    public void toggleNegate() {
        if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            num.toggleNegate();
            setCursor(new CursorPointer(num, num.cursorPosition));
        }
    }

    public void appendOperator(String symbol) {
        OperatorNode op = new OperatorNode(symbol);
        insertAtCursor(op);
        setCursor(new CursorPointer(op, 1));
    }

    public void insertSqrt() {
        NumberNode inner = new NumberNode("");
        SqrtNode sqrt = new SqrtNode(inner);
        insertAtCursor(sqrt);
        setCursor(new CursorPointer(inner, 0));
    }

    /**
     * True when the token right before the cursor is an operand that a/b can lift into the
     * numerator. Any completed token qualifies, a fraction included: a/b right after a finished
     * fraction nests it as the numerator of a new outer fraction (spec Section 2, Case 1 applies
     * uniformly to "angka/token", not just numbers) -- e.g. 1/2 -> a/b -> (1/2)/[].
     */
    private static boolean hasOperandBeforeCursor(CursorPointer cp) {
        ExpressionNode n = cp.node;
        if (n instanceof EmptyNode || n instanceof OperatorNode || n instanceof SequenceNode) {
            return false;
        }
        return cp.position > 0;
    }

    /**
     * Faithful to HiPER Calc C0196hc.java & EA.m72HiPER:
     * - Case 3: cursor inside the numerator -> a/b jumps to the denominator.
     * - Case 1: an operand right before the cursor becomes the numerator, denominator is empty,
     *   and the cursor goes to the denominator.
     * - Case 2: nothing to lift (empty display, after an operator, on a placeholder) ->
     *   both slots are empty boxes and the cursor goes to the numerator.
     */
    public void insertFraction() {
        if (cursorPointer != null && cursorPointer.node != null) {
            ExpressionNode slot = cursorPointer.node instanceof SequenceNode
                    ? cursorPointer.node : cursorPointer.node.getParent();
            FractionNode host = fractionOfSlot(slot);
            if (host != null && slot == host.numerator) {
                setCursor(startOf(host.denominator));
                return;
            }

            ExpressionNode target = cursorPointer.node;
            if (hasOperandBeforeCursor(cursorPointer) && target.getParent() instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) target.getParent();
                int idx = seq.getChildIndex(target);
                seq.removeChild(target);
                FractionNode frac = new FractionNode(target, null);
                seq.add(idx, frac);
                setCursor(startOf(frac.denominator));
                return;
            }
        }

        FractionNode frac = new FractionNode(null, null);
        insertAtCursor(frac);
        setCursor(startOf(frac.numerator));
    }

    public void insertPower() {
        ExpressionNode targetBase;
        if (cursorPointer != null && cursorPointer.node != null) {
            targetBase = cursorPointer.node;
        } else if (rootSequence.getChildCount() > 0) {
            targetBase = rootSequence.getChild(rootSequence.getChildCount() - 1);
        } else {
            targetBase = new NumberNode("x");
        }

        if (targetBase.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) targetBase.getParent();
            int idx = parent.getChildIndex(targetBase);
            parent.removeChild(targetBase);
            NumberNode exp = new NumberNode("");
            PowerNode pow = new PowerNode(targetBase, exp, "xʸ");
            parent.add(idx, pow);
            setCursor(new CursorPointer(exp, 0));
        } else {
            NumberNode exp = new NumberNode("");
            PowerNode pow = new PowerNode(targetBase, exp, "xʸ");
            rootSequence.addChild(pow);
            setCursor(new CursorPointer(exp, 0));
        }
    }

    public void insertSquare() {
        ExpressionNode targetBase;
        if (cursorPointer != null && cursorPointer.node != null) {
            targetBase = cursorPointer.node;
        } else if (rootSequence.getChildCount() > 0) {
            targetBase = rootSequence.getChild(rootSequence.getChildCount() - 1);
        } else {
            targetBase = new NumberNode("x");
        }

        if (targetBase.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) targetBase.getParent();
            int idx = parent.getChildIndex(targetBase);
            parent.removeChild(targetBase);
            NumberNode exp = new NumberNode("2");
            PowerNode pow = new PowerNode(targetBase, exp, "x²");
            parent.add(idx, pow);
            setCursor(new CursorPointer(pow, 1));
        } else {
            NumberNode exp = new NumberNode("2");
            PowerNode pow = new PowerNode(targetBase, exp, "x²");
            rootSequence.addChild(pow);
            setCursor(new CursorPointer(pow, 1));
        }
    }

    public void insertReciprocal() {
        NumberNode one = new NumberNode("1");
        NumberNode den = new NumberNode("");
        FractionNode frac = new FractionNode(one, den);
        insertAtCursor(frac);
        setCursor(new CursorPointer(den, 0));
    }

    public void insertParenthesis() {
        NumberNode inner = new NumberNode("");
        ParenthesisNode paren = new ParenthesisNode(inner);
        insertAtCursor(paren);
        setCursor(new CursorPointer(inner, 0));
    }

    /**
     * Removes a token from its sequence and places the cursor where it was.
     * A fraction slot that becomes empty gets its EmptyNode placeholder back.
     */
    private void removeFromSequence(ExpressionNode node) {
        if (!(node.getParent() instanceof SequenceNode)) {
            return;
        }
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);
        seq.removeChild(node);
        if (seq.getChildCount() == 0) {
            if (fractionOfSlot(seq) != null) {
                EmptyNode empty = new EmptyNode();
                seq.add(empty);
                setCursor(new CursorPointer(empty, 0));
            } else {
                setCursor(new CursorPointer(seq, 0));
            }
        } else if (idx > 0) {
            setCursor(afterNode(seq.getChild(idx - 1)));
        } else {
            setCursor(new CursorPointer(seq.getChild(0), 0));
        }
    }

    /** DEL with the cursor at the start of {@code node}: deletes whatever sits right before it. */
    private void deleteBefore(ExpressionNode node) {
        if (!(node.getParent() instanceof SequenceNode)) {
            return;
        }
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);
        if (idx <= 0) {
            return;
        }
        ExpressionNode prev = seq.getChild(idx - 1);
        if (prev instanceof FractionNode) {
            // DEL right after a fraction steps into its denominator
            setCursor(endOf(((FractionNode) prev).denominator));
            return;
        }
        if (prev instanceof NumberNode && prev.getLength() > 0) {
            NumberNode num = (NumberNode) prev;
            num.cursorPosition = -1; // delete from the end
            num.deleteChar();
            if (num.getLength() == 0) {
                seq.removeChild(num);
            }
        } else {
            seq.removeChild(prev);
        }
        setCursor(new CursorPointer(node, 0));
    }

    /** Replaces a fraction whose denominator is empty with its numerator tokens (unwrapping). */
    private void unwrapFraction(FractionNode frac) {
        if (!(frac.getParent() instanceof SequenceNode)) {
            return;
        }
        if (FractionNode.isEmptySlot(frac.numerator)) {
            removeFromSequence(frac);
            return;
        }
        SequenceNode seq = (SequenceNode) frac.getParent();
        int idx = seq.getChildIndex(frac);
        seq.removeChild(frac);
        List<ExpressionNode> tokens = new ArrayList<>(frac.numerator.children);
        for (int i = 0; i < tokens.size(); i++) {
            seq.add(idx + i, tokens.get(i));
        }
        setCursor(afterNode(tokens.get(tokens.size() - 1)));
    }

    public void deleteChar() {
        if (cursorPointer == null || cursorPointer.node == null) {
            return;
        }
        ExpressionNode node = cursorPointer.node;

        // Cursor directly on a sequence (e.g. empty root)
        if (node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) node;
            if (cursorPointer.position > 0 && cursorPointer.position <= seq.getChildCount()) {
                removeFromSequence(seq.getChild(cursorPointer.position - 1));
            }
            return;
        }

        // Center slot before/after a fraction
        if (node instanceof FractionNode) {
            FractionNode frac = (FractionNode) node;
            if (cursorPointer.position == 1) {
                if (FractionNode.isEmptySlot(frac.denominator)) {
                    unwrapFraction(frac);
                } else {
                    setCursor(endOf(frac.denominator));
                }
            } else {
                deleteBefore(frac);
            }
            return;
        }

        // Empty placeholder box (QA)
        if (node instanceof EmptyNode) {
            FractionNode frac = fractionOfSlot(node.getParent());
            if (frac == null) {
                removeFromSequence(node);
            } else if (node.getParent() == frac.denominator) {
                // DEL in empty denominator jumps to the end of the numerator
                setCursor(endOf(frac.numerator));
            } else if (FractionNode.isEmptySlot(frac.denominator)) {
                // Both slots empty: remove the whole fraction
                removeFromSequence(frac);
            } else {
                // Numerator empty but denominator filled: keep the numerator box visible
                setCursor(new CursorPointer(frac, 0));
            }
            return;
        }

        if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            if (num.deleteChar()) {
                if (num.getLength() == 0 && num.getParent() instanceof SequenceNode) {
                    // Number emptied: drop it (a fraction slot gets its EmptyNode back)
                    removeFromSequence(num);
                } else {
                    setCursor(new CursorPointer(num, num.cursorPosition));
                }
                return;
            }
            if (num.getLength() > 0 || num.getParent() instanceof SequenceNode) {
                deleteBefore(num);
                return;
            }
            // Empty number inside sqrt/parenthesis/power: remove the wrapper
            if (num.getParent() != null) {
                removeFromSequence(num.getParent());
            }
            return;
        }

        removeFromSequence(node);
    }

    public void moveCursorLeft() {
        if (cursorPointer == null || cursorPointer.node == null) return;
        ExpressionNode node = cursorPointer.node;

        // Inside NumberNode: move left character-by-character
        if (node instanceof NumberNode && cursorPointer.position > 0) {
            setCursor(new CursorPointer(node, cursorPointer.position - 1));
            return;
        }

        // Center after fraction: step left into the end of the denominator
        if (node instanceof FractionNode && cursorPointer.position == 1) {
            setCursor(endOf(((FractionNode) node).denominator));
            return;
        }

        if (!(node.getParent() instanceof SequenceNode)) return;
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);

        if (idx > 0) {
            setCursor(afterNode(seq.getChild(idx - 1)));
            return;
        }

        // At the start of a fraction slot
        FractionNode frac = fractionOfSlot(seq);
        if (frac != null) {
            if (seq == frac.denominator) {
                setCursor(endOf(frac.numerator));
            } else {
                setCursor(new CursorPointer(frac, 0)); // Center before fraction
            }
        }
    }

    public void moveCursorRight() {
        if (cursorPointer == null || cursorPointer.node == null) return;
        ExpressionNode node = cursorPointer.node;

        // Inside NumberNode: move right character-by-character
        if (node instanceof NumberNode && cursorPointer.position < node.getLength()) {
            setCursor(new CursorPointer(node, cursorPointer.position + 1));
            return;
        }

        // Center before fraction: step right into the start of the numerator
        if (node instanceof FractionNode && cursorPointer.position == 0) {
            setCursor(startOf(((FractionNode) node).numerator));
            return;
        }

        if (!(node.getParent() instanceof SequenceNode)) return;
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);

        if (idx >= 0 && idx < seq.getChildCount() - 1) {
            ExpressionNode next = seq.getChild(idx + 1);
            if (next instanceof NumberNode || next instanceof FractionNode || next instanceof EmptyNode) {
                setCursor(new CursorPointer(next, 0));
            } else {
                setCursor(afterNode(next));
            }
            return;
        }

        // At the end of a fraction slot
        FractionNode frac = fractionOfSlot(seq);
        if (frac != null) {
            if (seq == frac.numerator) {
                setCursor(startOf(frac.denominator));
            } else {
                setCursor(new CursorPointer(frac, 1)); // Center after fraction
            }
        }
    }
}
