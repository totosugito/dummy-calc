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

/**
 * DEL/backspace logic: deleting one character or token, including the fraction unwrap/nesting
 * nuance (Task 19/20b in specs/btn_fraction.md). Pulled out of ExpressionEditor (2026-09-26,
 * once it grew back to 730 lines after the fraction/power inserter split) since this is a
 * cohesive, sizable, single-purpose unit distinct from both insertion and navigation -- see
 * specs/btn_fraction.md Section H.
 *
 * Like {@link CursorNav}, this never touches the expression root -- it only walks the tree via
 * {@code node.getParent()} -- so {@link #deleteChar} is a pure function of the current
 * {@link CursorPointer}, returning the SAME instance whenever nothing happens (mirroring the
 * original's bare {@code return;} early-outs) so callers can always do
 * {@code setCursor(CursorDelete.deleteChar(cursorPointer))} unconditionally.
 */
public final class CursorDelete {
    private CursorDelete() {
    }

    public static CursorPointer deleteChar(CursorPointer cursorPointer) {
        if (cursorPointer == null || cursorPointer.node == null) {
            return cursorPointer;
        }
        ExpressionNode node = cursorPointer.node;

        // Cursor directly on a sequence (e.g. empty root)
        if (node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) node;
            if (cursorPointer.position > 0 && cursorPointer.position <= seq.getChildCount()) {
                return removeFromSequence(seq.getChild(cursorPointer.position - 1), cursorPointer);
            }
            return cursorPointer;
        }

        // Center slot before/after a fraction
        if (node instanceof FractionNode) {
            FractionNode frac = (FractionNode) node;
            if (cursorPointer.position == 1) {
                // Center after the fraction: DEL removes the whole fraction outright, matching
                // Power/Sqrt/Parenthesis, which already behave this way at their own Center-after
                // position. Editing numerator/denominator content requires positioning the cursor
                // explicitly INSIDE the fraction first (tap or ◀/▶), not via DEL auto-entering it.
                return removeFromSequence(frac, cursorPointer);
            }
            return deleteBefore(frac, cursorPointer);
        }

        // Empty placeholder box (QA)
        if (node instanceof EmptyNode) {
            FractionNode frac = CursorNav.fractionOfSlot(node.getParent());
            if (frac == null) {
                // An EmptyNode can also end up as the sole content of a power/sqrt/parenthesis
                // slot (refilled by removeFromSequence once a number is deleted down to
                // nothing there -- see specs/editable_slots_sequencenode.md). DEL there should
                // remove the whole wrapper, same as DEL on an empty NumberNode slot does.
                if (node.getParent() instanceof SequenceNode) {
                    SequenceNode slot = (SequenceNode) node.getParent();
                    ExpressionNode owner = slot.getParent();
                    if (slot.getChildCount() == 1
                            && (owner instanceof SqrtNode || owner instanceof ParenthesisNode || owner instanceof PowerNode)) {
                        return removeFromSequence(owner, cursorPointer);
                    }
                }
                return removeFromSequence(node, cursorPointer);
            } else if (node.getParent() == frac.denominator) {
                // DEL in empty denominator deletes the numerator's last character
                return enterAndDelete(CursorNav.endOf(frac.numerator));
            } else if (frac.isMixed() && node.getParent() == frac.numerator) {
                // DEL in empty numerator (mixed number) deletes the integer part's last
                // character, unless the denominator is filled -- then keep the numerator box
                // visible.
                if (!FractionNode.isEmptySlot(frac.denominator)) {
                    return new CursorPointer(frac, 0);
                } else if (!FractionNode.isEmptySlot(frac.integerPart)) {
                    return enterAndDelete(CursorNav.endOf(frac.integerPart));
                } else {
                    return removeFromSequence(frac, cursorPointer);
                }
            } else if (frac.isMixed() && node.getParent() == frac.integerPart) {
                // DEL in empty integer part (mixed number, at the very start): behaves like
                // DEL right before the whole fraction, deleting whatever precedes it.
                if (FractionNode.isEmptySlot(frac.numerator) && FractionNode.isEmptySlot(frac.denominator)) {
                    return removeFromSequence(frac, cursorPointer);
                } else {
                    return deleteBefore(frac, cursorPointer);
                }
            } else if (FractionNode.isEmptySlot(frac.denominator)) {
                // Both slots empty: remove the whole fraction
                return removeFromSequence(frac, cursorPointer);
            } else {
                // Numerator empty but denominator filled: keep the numerator box visible
                return new CursorPointer(frac, 0);
            }
        }

        if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            if (num.deleteChar()) {
                if (num.getLength() == 0 && num.getParent() instanceof SequenceNode) {
                    // Number emptied: drop it (a fraction/power/sqrt/parenthesis slot gets its
                    // EmptyNode placeholder back, see removeFromSequence)
                    return removeFromSequence(num, cursorPointer);
                }
                return new CursorPointer(num, num.cursorPosition);
            }
            // Empty number that is the sole content of a power/sqrt/parenthesis slot: DEL
            // removes the whole wrapper (there's nothing left inside it to delete). This must
            // be checked BEFORE the generic "parent is a SequenceNode" case below, since with
            // slots now always SequenceNode-wrapped (see specs/editable_slots_sequencenode.md)
            // that check alone can no longer tell "genuinely empty box" apart from "an empty
            // number with other tokens before it in the same slot".
            if (num.getLength() == 0 && num.getParent() instanceof SequenceNode) {
                SequenceNode slot = (SequenceNode) num.getParent();
                ExpressionNode owner = slot.getParent();
                if (slot.getChildCount() == 1
                        && (owner instanceof SqrtNode || owner instanceof ParenthesisNode || owner instanceof PowerNode)) {
                    return removeFromSequence(owner, cursorPointer);
                }
            }
            if (num.getLength() > 0 || num.getParent() instanceof SequenceNode) {
                return deleteBefore(num, cursorPointer);
            }
            return cursorPointer;
        }

        return removeFromSequence(node, cursorPointer);
    }

    /**
     * DEL "entering" a slot at its end (e.g. Center-after-fraction stepping into a non-empty
     * denominator, or an empty denominator stepping back into the numerator): if the last token
     * there is a plain number with real digits, delete its last character immediately instead of
     * merely moving the cursor there. Without this, that DEL press produced no visible change at
     * all -- the exact same "dead keystroke" issue already fixed for operators, see
     * specs/operator_cursor_nav.md -- requiring a second press before anything actually happened.
     *
     * If the last token is itself a compound (nested fraction/power/sqrt/parenthesis), this just
     * moves the cursor in without deleting -- reaching through multiple nested levels in a single
     * press would be a separate, more surprising behavior change, not what this fixes.
     */
    private static CursorPointer enterAndDelete(CursorPointer target) {
        if (target.node instanceof NumberNode && target.node.getLength() > 0) {
            return deleteChar(target);
        }
        return target;
    }

    /**
     * Removes a token from its sequence and returns the cursor position that should follow.
     * A fraction slot that becomes empty gets its EmptyNode placeholder back.
     *
     * @param fallback cursor to return unchanged if {@code node} turns out to have no
     *                 SequenceNode parent (defensive; not expected to trigger from deleteChar's
     *                 own call sites, all of which already know their node sits in a sequence)
     */
    private static CursorPointer removeFromSequence(ExpressionNode node, CursorPointer fallback) {
        if (!(node.getParent() instanceof SequenceNode)) {
            return fallback;
        }
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);
        seq.removeChild(node);
        if (seq.getChildCount() == 0) {
            // A slot owned by a wrapper node (fraction/power/sqrt/parenthesis) must never sit
            // at zero children -- refill it with an empty placeholder of whichever kind that
            // slot's owner actually uses (CursorNav.freshPlaceholder -- EmptyNode for fractions,
            // an empty NumberNode for everything else, see specs/btn_x_power_y.md Section N). A
            // "plain" sequence that isn't anyone's slot (e.g. the expression root) is allowed to
            // genuinely end up empty.
            if (CursorNav.isWrapperSlot(seq)) {
                ExpressionNode placeholder = CursorNav.freshPlaceholder(seq);
                seq.add(placeholder);
                return new CursorPointer(placeholder, 0);
            }
            return new CursorPointer(seq, 0);
        } else if (idx > 0) {
            ExpressionNode prev = seq.getChild(idx - 1);
            if (prev instanceof OperatorNode) {
                // The deleted node sat right after an operator (e.g. "2 + <fraction>", the
                // fraction just removed) -- land the cursor exactly where the deleted node was
                // (right after the operator), NOT further back past it. That's what
                // CursorNav.afterNode(operatorNode) would do instead: it deliberately returns a
                // single "position 0" for any OperatorNode (see specs/operator_cursor_nav.md,
                // operators are never a resting spot of their own), but that position 0 RENDERS
                // as just before the operator -- so calling afterNode() on the operator here
                // put the cursor on the wrong side of it ("2| +" instead of "2 + |"). A plain
                // sequence-position pointer at the original index renders correctly at that
                // boundary without resting "on" the operator itself.
                return new CursorPointer(seq, idx);
            }
            return CursorNav.afterNode(prev);
        } else {
            return new CursorPointer(seq.getChild(0), 0);
        }
    }

    /** DEL with the cursor at the start of {@code node}: deletes whatever sits right before it. */
    private static CursorPointer deleteBefore(ExpressionNode node, CursorPointer fallback) {
        if (!(node.getParent() instanceof SequenceNode)) {
            return fallback;
        }
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);
        if (idx <= 0) {
            return fallback;
        }
        ExpressionNode prev = seq.getChild(idx - 1);
        if (prev instanceof FractionNode) {
            // DEL right after a fraction steps into its denominator
            return CursorNav.endOf(((FractionNode) prev).denominator);
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
        return new CursorPointer(node, 0);
    }
}
