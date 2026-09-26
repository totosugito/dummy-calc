package com.calctastic.sample.hypercal.engine.inserter;

import com.calctastic.sample.hypercal.engine.CursorNav;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;

/**
 * Insert logic for the radical family of keypad buttons: "√x" and "ⁿ√x" (n-th root). Pulled out
 * of ExpressionEditor to match FractionInserter/PowerInserter's shape -- see
 * specs/btn_sqrt.md and specs/editable_slots_sequencenode.md.
 *
 * Unlike the fraction/power families, a radical is a PREFIX operator (it applies to whatever
 * comes after it, not whatever came before), so neither method here ever lifts an operand from
 * before the cursor the way FractionInserter/PowerInserter's Case 1 does -- pressing "5" then
 * "√x" produces "5√{}" (a fresh, empty radical placed right after the 5), never "√{5}". This
 * matches the original app's own behavior for √x (confirmed before this class existed, when the
 * logic lived directly in ExpressionEditor.insertSqrt()).
 */
public final class SqrtInserter {
    private SqrtInserter() {
    }

    /** "√x" -- a fresh, empty radical with no degree; cursor lands in the radicand. */
    public static CursorPointer insertSqrt(SequenceNode rootSequence, CursorPointer cursorPointer) {
        NumberNode radicand = new NumberNode("");
        SqrtNode sqrt = new SqrtNode(radicand);
        CursorNav.insertAtCursor(rootSequence, cursorPointer, sqrt);
        return new CursorPointer(radicand, 0);
    }

    /**
     * "ⁿ√x" -- n-th root, with both the degree and the radicand starting empty. Cursor lands in
     * the degree first (read/typed before the radicand, e.g. "3" then ▶ then "8" for the cube
     * root of 8) -- mirroring how "xʸ" lands the cursor in the part the user types first for that
     * button (the exponent), even though here it's the earlier slot in reading order rather than
     * the later one.
     */
    public static CursorPointer insertNthRoot(SequenceNode rootSequence, CursorPointer cursorPointer) {
        NumberNode degree = new NumberNode("");
        NumberNode radicand = new NumberNode("");
        SqrtNode sqrt = new SqrtNode(degree, radicand);
        CursorNav.insertAtCursor(rootSequence, cursorPointer, sqrt);
        return new CursorPointer(degree, 0);
    }
}
