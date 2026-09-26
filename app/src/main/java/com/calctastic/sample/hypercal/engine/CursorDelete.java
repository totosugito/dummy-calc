package com.calctastic.sample.hypercal.engine;

import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.EmptyNode;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.FractionNode;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.ParenthesisNode;
import com.calctastic.sample.hypercal.engine.model.PowerNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;

import java.util.ArrayList;
import java.util.List;

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
                if (FractionNode.isEmptySlot(frac.denominator)) {
                    return unwrapFraction(frac, cursorPointer);
                }
                return CursorNav.endOf(frac.denominator);
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
                // DEL in empty denominator jumps to the end of the numerator
                return CursorNav.endOf(frac.numerator);
            } else if (frac.isMixed() && node.getParent() == frac.numerator) {
                // DEL in empty numerator (mixed number) jumps to the end of the integer part,
                // unless the denominator is filled -- then keep the numerator box visible.
                if (!FractionNode.isEmptySlot(frac.denominator)) {
                    return new CursorPointer(frac, 0);
                } else if (!FractionNode.isEmptySlot(frac.integerPart)) {
                    return CursorNav.endOf(frac.integerPart);
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
            // at zero children -- refill it with an EmptyNode placeholder, same as a fraction's
            // numerator/denominator already did before this became generic (see
            // specs/editable_slots_sequencenode.md). A "plain" sequence that isn't anyone's
            // slot (e.g. the expression root) is allowed to genuinely end up empty.
            if (CursorNav.isWrapperSlot(seq)) {
                EmptyNode empty = new EmptyNode();
                seq.add(empty);
                return new CursorPointer(empty, 0);
            }
            return new CursorPointer(seq, 0);
        } else if (idx > 0) {
            return CursorNav.afterNode(seq.getChild(idx - 1));
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

    /**
     * Replaces a fraction whose denominator is empty with its remaining tokens (unwrapping):
     * numerator tokens for a plain a/b, or integer-part + numerator tokens for a mixed number.
     */
    private static CursorPointer unwrapFraction(FractionNode frac, CursorPointer fallback) {
        if (!(frac.getParent() instanceof SequenceNode)) {
            return fallback;
        }
        boolean intEmpty = !frac.isMixed() || FractionNode.isEmptySlot(frac.integerPart);
        boolean numEmpty = FractionNode.isEmptySlot(frac.numerator);
        if (intEmpty && numEmpty) {
            return removeFromSequence(frac, fallback);
        }
        SequenceNode seq = (SequenceNode) frac.getParent();
        int idx = seq.getChildIndex(frac);
        seq.removeChild(frac);
        List<ExpressionNode> tokens = new ArrayList<>();
        if (!intEmpty) {
            tokens.addAll(frac.integerPart.children);
        }
        if (!numEmpty) {
            tokens.addAll(frac.numerator.children);
        }
        for (int i = 0; i < tokens.size(); i++) {
            seq.add(idx + i, tokens.get(i));
        }
        return CursorNav.afterNode(tokens.get(tokens.size() - 1));
    }
}
