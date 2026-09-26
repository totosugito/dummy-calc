package com.calctastic.sample.hypercal.engine.inserter;

import com.calctastic.sample.hypercal.engine.CursorNav;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.FractionNode;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;

/**
 * Insert logic for the fraction family of keypad buttons: "a/b", "a b/c" and "1/x". Pulled out of
 * ExpressionEditor (2026-09-26) -- see specs/btn_fraction.md, specs/btn_ab_c.md and
 * specs/btn_1_per_x.md for the button-by-button reasoning; this class only holds the code.
 *
 * Each method is static and takes the current {@code rootSequence}/{@code cursorPointer}
 * explicitly (no shared editor state) and returns the new {@link CursorPointer} to apply --
 * ExpressionEditor calls these and stores the result itself.
 */
public final class FractionInserter {
    private FractionInserter() {
    }

    /**
     * Faithful to HiPER Calc C0196hc.java & EA.m72HiPER:
     * - Case 3: cursor inside the numerator -> a/b jumps to the denominator.
     * - Case 1: an operand right before the cursor becomes the numerator, denominator is empty,
     *   and the cursor goes to the denominator.
     * - Case 2: nothing to lift (empty display, after an operator, on a placeholder) ->
     *   both slots are empty boxes and the cursor goes to the numerator.
     */
    public static CursorPointer insertFraction(SequenceNode rootSequence, CursorPointer cursorPointer) {
        if (cursorPointer != null && cursorPointer.node != null) {
            ExpressionNode slot = cursorPointer.node instanceof SequenceNode
                    ? cursorPointer.node : cursorPointer.node.getParent();
            FractionNode host = CursorNav.fractionOfSlot(slot);
            if (host != null && slot == host.numerator) {
                return CursorNav.startOf(host.denominator);
            }

            ExpressionNode target = cursorPointer.node;
            if (CursorNav.hasOperandBeforeCursor(cursorPointer) && target.getParent() instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) target.getParent();
                int idx = seq.getChildIndex(target);
                seq.removeChild(target);
                FractionNode frac = new FractionNode(target, null);
                seq.add(idx, frac);
                return CursorNav.startOf(frac.denominator);
            }
        }

        FractionNode frac = new FractionNode(null, null);
        CursorNav.insertAtCursor(rootSequence, cursorPointer, frac);
        return CursorNav.startOf(frac.numerator);
    }

    /**
     * "a b/c" mixed-number button. Faithful to HiPER's reuse of the fraction editing
     * path for the 3-child EnumC0300sa.sa node (C0197hd.java:734 branches on iL==3):
     * - Cursor inside the integer part -> jumps to the numerator.
     * - Cursor inside the numerator -> jumps to the denominator (same as plain a/b).
     * - An operand right before the cursor is lifted into the integer part (not the
     *   numerator, unlike plain a/b), numerator/denominator start empty.
     * - Otherwise: a fresh mixed number with all three slots empty, cursor in the integer part.
     */
    public static CursorPointer insertMixedFraction(SequenceNode rootSequence, CursorPointer cursorPointer) {
        if (cursorPointer != null && cursorPointer.node != null) {
            ExpressionNode slot = cursorPointer.node instanceof SequenceNode
                    ? cursorPointer.node : cursorPointer.node.getParent();
            FractionNode host = CursorNav.fractionOfSlot(slot);
            if (host != null && host.isMixed()) {
                if (slot == host.integerPart) {
                    return CursorNav.startOf(host.numerator);
                }
                if (slot == host.numerator) {
                    return CursorNav.startOf(host.denominator);
                }
            }

            ExpressionNode target = cursorPointer.node;
            if (CursorNav.hasOperandBeforeCursor(cursorPointer) && target.getParent() instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) target.getParent();
                int idx = seq.getChildIndex(target);
                seq.removeChild(target);
                FractionNode frac = FractionNode.createMixed(target, null, null);
                seq.add(idx, frac);
                return CursorNav.startOf(frac.numerator);
            }
        }

        FractionNode frac = FractionNode.createMixed(null, null, null);
        CursorNav.insertAtCursor(rootSequence, cursorPointer, frac);
        return CursorNav.startOf(frac.integerPart);
    }

    /**
     * "1/x" reciprocal button -- not part of the original HiPER Calc keypad (no decompiled
     * reference), designed to match the existing a/b conventions instead: see
     * specs/btn_1_per_x.md for the reasoning. An operand right before the cursor becomes the
     * denominator of "1/x" (like a/b's Case 1), cursor lands right after the fraction. With
     * nothing to wrap, falls back to an empty "1/[]" placeholder, cursor in the denominator.
     */
    public static CursorPointer insertReciprocal(SequenceNode rootSequence, CursorPointer cursorPointer) {
        if (cursorPointer != null && cursorPointer.node != null
                && CursorNav.hasOperandBeforeCursor(cursorPointer)
                && cursorPointer.node.getParent() instanceof SequenceNode) {
            ExpressionNode target = cursorPointer.node;
            SequenceNode seq = (SequenceNode) target.getParent();
            int idx = seq.getChildIndex(target);
            seq.removeChild(target);
            FractionNode frac = new FractionNode(new NumberNode("1"), target);
            seq.add(idx, frac);
            return CursorNav.afterNode(frac);
        }

        NumberNode den = new NumberNode("");
        FractionNode frac = new FractionNode(new NumberNode("1"), den);
        CursorNav.insertAtCursor(rootSequence, cursorPointer, frac);
        return new CursorPointer(den, 0);
    }
}
