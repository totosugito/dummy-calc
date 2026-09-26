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
 * Everything about "where the cursor goes": the ◀/▶ arrow-key entry points ({@link #moveLeft},
 * {@link #moveRight}) plus the lower-level slot/boundary helpers they (and the per-button insert
 * logic under {@code engine.inserter} -- {@link com.calctastic.sample.hypercal.engine.inserter.FractionInserter},
 * {@link com.calctastic.sample.hypercal.engine.inserter.PowerInserter}) are built on.
 *
 * Pulled out of ExpressionEditor (2026-09-26, then again same day once it grew back to 730 lines)
 * so this logic isn't duplicated or reached into via private methods -- everything here is
 * pure/static, no editor state (no rootSequence -- these only ever walk the tree via
 * {@code node.getParent()}). See specs/btn_fraction.md Section H for the file-split history.
 */
public final class CursorNav {
    private CursorNav() {
    }

    /** Returns the fraction owning this slot sequence, or null if it is not a fraction slot. */
    public static FractionNode fractionOfSlot(ExpressionNode slot) {
        if (slot instanceof SequenceNode && slot.getParent() instanceof FractionNode) {
            return (FractionNode) slot.getParent();
        }
        return null;
    }

    public static CursorPointer afterNode(ExpressionNode node) {
        if (node instanceof FractionNode) return new CursorPointer(node, 1); // Center after fraction
        // Single-position tokens: EmptyNode has nothing to be "before" or "after" separately, and
        // neither does an operator (+, -, x, /, ...) -- it's a single atomic symbol, not a
        // multi-character run like NumberNode. Giving it a distinct position 1 here used to create
        // a dead-click bug: moveCursorLeft/Right never actually use an operator's own position
        // (they always jump straight past it to the neighboring operand), so that extra stop was
        // pure dead weight that happened to render at the same spot as the adjacent operand's own
        // boundary -- making it look like passing a single "+" needed two arrow presses.
        if (node instanceof EmptyNode || node instanceof OperatorNode) return new CursorPointer(node, 0);
        return new CursorPointer(node, node.getLength());
    }

    public static CursorPointer startOf(SequenceNode seq) {
        if (seq.getChildCount() == 0) return new CursorPointer(seq, 0);
        return new CursorPointer(seq.getChild(0), 0);
    }

    public static CursorPointer endOf(SequenceNode seq) {
        if (seq.getChildCount() == 0) return new CursorPointer(seq, 0);
        return afterNode(seq.getChild(seq.getChildCount() - 1));
    }

    /**
     * The cursor position for landing on {@code node} when approaching it from its left (used
     * when moveCursorRight steps onto a new sibling). Mirrors the fact that a fresh/multi-content
     * node (number, fraction, empty slot) should be entered at its own start, while a single-unit
     * node (power, sqrt, parenthesis) is approached at its "Center before" boundary instead.
     */
    public static CursorPointer enterFromLeft(ExpressionNode node) {
        if (node instanceof NumberNode || node instanceof FractionNode || node instanceof EmptyNode) {
            return new CursorPointer(node, 0);
        }
        return afterNode(node);
    }

    /**
     * True when {@code seq} is an editable slot owned by some wrapper node (fraction, power,
     * sqrt, parenthesis) -- see specs/editable_slots_sequencenode.md. Such a slot must never be
     * left with zero children: emptying it out should refill it with a single empty placeholder
     * instead (see {@link #freshPlaceholder}), the same way a fraction's numerator/denominator
     * already do. This is false for a "plain" sequence that isn't anyone's slot (e.g. the
     * expression root), which is allowed to genuinely have zero children.
     */
    public static boolean isWrapperSlot(SequenceNode seq) {
        return isWrapperSlot(seq.getParent());
    }

    private static boolean isWrapperSlot(ExpressionNode owner) {
        return owner instanceof FractionNode || owner instanceof PowerNode
                || owner instanceof SqrtNode || owner instanceof ParenthesisNode;
    }

    /**
     * The right kind of empty placeholder to refill {@code seq} with once it's been emptied out,
     * matching whichever convention its owner already uses elsewhere -- NOT a single hardcoded
     * choice. Fraction slots (numerator/denominator/integerPart) use {@code EmptyNode} (`QA`);
     * everywhere else that's SequenceNode-wrapped now (power base/exponent, sqrt radicand/degree,
     * parenthesis content) uses a plain empty {@code NumberNode}, per the convention noted in
     * specs/btn_1_per_x.md. Mixing these up is a real bug that shipped once (2026-09-26, see
     * specs/btn_x_power_y.md Section N): refilling a power/sqrt/parenthesis slot with an
     * {@code EmptyNode} instead makes it render via {@code PlaceholderVisual}'s box formula
     * instead of {@code NumberVisual}'s -- a visibly different (and, since Section M, differently
     * SIZED) box popping up after DEL than the one the button itself would have created fresh.
     */
    public static ExpressionNode freshPlaceholder(SequenceNode seq) {
        ExpressionNode owner = seq.getParent();
        if (owner instanceof FractionNode) {
            return new EmptyNode();
        }
        return new NumberNode("");
    }

    /**
     * True when the token right before the cursor is an operand that a/b (and friends: mixed
     * number, 1/x, xʸ) can lift into their own first slot. Any completed token qualifies, a
     * fraction included: a/b right after a finished fraction nests it (spec Section 2, Case 1
     * applies uniformly to "angka/token", not just numbers) -- e.g. 1/2 -> a/b -> (1/2)/[].
     */
    public static boolean hasOperandBeforeCursor(CursorPointer cp) {
        ExpressionNode n = cp.node;
        if (n instanceof EmptyNode || n instanceof OperatorNode || n instanceof SequenceNode) {
            return false;
        }
        return cp.position > 0;
    }

    /**
     * ◀ left-arrow navigation. Pure function of the current cursor -- returns the SAME
     * {@code cursorPointer} instance whenever there's nowhere meaningful to go (mirrors the
     * original's bare {@code return;} early-outs), so callers can always do
     * {@code setCursor(CursorNav.moveLeft(cursorPointer))} unconditionally.
     *
     * Moved here from ExpressionEditor (2026-09-26) alongside {@link #moveRight}, since both are
     * pure cursor-position logic built entirely on this class's own helpers and never touch the
     * expression root -- see specs/btn_fraction.md Section H.
     */
    public static CursorPointer moveLeft(CursorPointer cursorPointer) {
        if (cursorPointer == null || cursorPointer.node == null) return cursorPointer;
        ExpressionNode node = cursorPointer.node;

        // Inside NumberNode: move left character-by-character
        if (node instanceof NumberNode && cursorPointer.position > 0) {
            return new CursorPointer(node, cursorPointer.position - 1);
        }

        // Center after fraction: step left into the end of the denominator
        if (node instanceof FractionNode && cursorPointer.position == 1) {
            return endOf(((FractionNode) node).denominator);
        }

        // Center after power (xʸ/x²/x³/x⁻¹): step left into the end of the exponent
        if (node instanceof PowerNode && cursorPointer.position == 1) {
            return endOf(((PowerNode) node).exponent);
        }

        // Center after parenthesis: step left into the end of its content
        if (node instanceof ParenthesisNode && cursorPointer.position == 1) {
            return endOf(((ParenthesisNode) node).content);
        }

        // Center after sqrt: step left into the end of the radicand
        if (node instanceof SqrtNode && cursorPointer.position == 1) {
            return endOf(((SqrtNode) node).radicand);
        }

        ExpressionNode parent = node.getParent();
        if (!(parent instanceof SequenceNode)) return cursorPointer;
        SequenceNode seq = (SequenceNode) parent;
        int idx = seq.getChildIndex(node);

        if (idx > 0) {
            ExpressionNode prev = seq.getChild(idx - 1);
            // Operators are a single atomic symbol with no internal content of their own to stop
            // in -- skip straight past to whatever precedes it (or the sequence start, if it's
            // the very first token) instead of resting the cursor on/around the operator itself.
            // Without this, crossing "4 + 3" left-to-right took two dead-looking presses: one to
            // land on the operator's own boundary, then another that renders at that exact same
            // spot as the boundary of the token before it (see specs/operator_cursor_nav.md).
            if (prev instanceof OperatorNode) {
                return idx > 1 ? afterNode(seq.getChild(idx - 2)) : new CursorPointer(seq, 0);
            }
            return afterNode(prev);
        }

        // At the start of a slot (fraction/power/sqrt/parenthesis): step out to the sibling
        // slot, or to Center just before the whole wrapper node -- see
        // specs/editable_slots_sequencenode.md, this is the same shape for every slot type
        // now that they're all SequenceNode.
        FractionNode frac = fractionOfSlot(seq);
        if (frac != null) {
            if (seq == frac.denominator) {
                return endOf(frac.numerator);
            } else if (seq == frac.numerator && frac.isMixed()) {
                return endOf(frac.integerPart);
            } else {
                return new CursorPointer(frac, 0); // Center before fraction
            }
        }
        ExpressionNode owner = seq.getParent();
        if (owner instanceof PowerNode) {
            PowerNode pow = (PowerNode) owner;
            return seq == pow.exponent ? endOf(pow.base) : new CursorPointer(pow, 0);
        }
        if (owner instanceof ParenthesisNode) {
            return new CursorPointer(owner, 0); // Center before parenthesis
        }
        if (owner instanceof SqrtNode) {
            SqrtNode sqrt = (SqrtNode) owner;
            return (seq == sqrt.radicand && sqrt.degree != null)
                    ? endOf(sqrt.degree) : new CursorPointer(sqrt, 0);
        }
        return cursorPointer;
    }

    /** ▶ right-arrow navigation -- mirror of {@link #moveLeft}, see its doc for the general shape. */
    public static CursorPointer moveRight(CursorPointer cursorPointer) {
        if (cursorPointer == null || cursorPointer.node == null) return cursorPointer;
        ExpressionNode node = cursorPointer.node;

        // Inside NumberNode: move right character-by-character
        if (node instanceof NumberNode && cursorPointer.position < node.getLength()) {
            return new CursorPointer(node, cursorPointer.position + 1);
        }

        // Center before fraction: step right into the start of the integer part (mixed
        // number) or numerator (plain a/b)
        if (node instanceof FractionNode && cursorPointer.position == 0) {
            FractionNode frac = (FractionNode) node;
            return startOf(frac.isMixed() ? frac.integerPart : frac.numerator);
        }

        // Center before power: step right into the start of the base
        if (node instanceof PowerNode && cursorPointer.position == 0) {
            return startOf(((PowerNode) node).base);
        }

        // Center before parenthesis: step right into the start of its content
        if (node instanceof ParenthesisNode && cursorPointer.position == 0) {
            return startOf(((ParenthesisNode) node).content);
        }

        // Center before sqrt: step right into the start of the degree (if any) or radicand
        if (node instanceof SqrtNode && cursorPointer.position == 0) {
            SqrtNode sqrt = (SqrtNode) node;
            return startOf(sqrt.degree != null ? sqrt.degree : sqrt.radicand);
        }

        ExpressionNode parent = node.getParent();
        if (!(parent instanceof SequenceNode)) return cursorPointer;
        SequenceNode seq = (SequenceNode) parent;
        int idx = seq.getChildIndex(node);

        if (idx >= 0 && idx < seq.getChildCount() - 1) {
            ExpressionNode next = seq.getChild(idx + 1);
            // Mirror of moveLeft's operator-skip above: don't rest the cursor on/around the
            // operator itself, jump straight to whatever follows it (or the sequence end).
            if (next instanceof OperatorNode) {
                if (idx + 2 < seq.getChildCount()) {
                    return enterFromLeft(seq.getChild(idx + 2));
                }
                return new CursorPointer(seq, seq.getChildCount());
            }
            return enterFromLeft(next);
        }

        // At the end of a slot (fraction/power/sqrt/parenthesis): step out to the sibling
        // slot, or to Center just after the whole wrapper node -- mirror of moveLeft's
        // "start of a slot" handling above.
        FractionNode frac = fractionOfSlot(seq);
        if (frac != null) {
            if (seq == frac.integerPart) {
                return startOf(frac.numerator);
            } else if (seq == frac.numerator) {
                return startOf(frac.denominator);
            } else {
                return new CursorPointer(frac, 1); // Center after fraction
            }
        }
        ExpressionNode owner = seq.getParent();
        if (owner instanceof PowerNode) {
            PowerNode pow = (PowerNode) owner;
            return seq == pow.base ? startOf(pow.exponent) : new CursorPointer(pow, 1);
        }
        if (owner instanceof ParenthesisNode) {
            return new CursorPointer(owner, 1);
        }
        if (owner instanceof SqrtNode) {
            SqrtNode sqrt = (SqrtNode) owner;
            return seq == sqrt.degree ? startOf(sqrt.radicand) : new CursorPointer(sqrt, 1);
        }
        return cursorPointer;
    }

    /**
     * True when {@code node} is an empty placeholder standing in for "nothing typed here yet" --
     * either an {@code EmptyNode} (QA, used by fraction slots) or a {@code NumberNode} with no
     * text (the convention used by xʸ/x²/x³/x⁻¹/√/parenthesis's base/exponent/radicand/content
     * slots, see specs/btn_x_power_y.md). Both represent the same thing and should be replaced,
     * not left behind as an empty sibling, when a new compound token is inserted at the cursor.
     */
    private static boolean isEmptyPlaceholder(ExpressionNode node) {
        return node instanceof EmptyNode || (node instanceof NumberNode && node.getLength() == 0);
    }

    /**
     * Inserts a new token at the cursor inside the cursor's own sequence.
     * An empty placeholder under the cursor is replaced (QA, or an empty NumberNode -> real
     * token), rather than left behind as an invisible-but-still-there empty sibling. Missing this
     * for empty NumberNode used to be a real bug (2026-09-26): pressing xʸ again with the cursor
     * in a fresh, still-empty exponent (itself created by a previous xʸ) inserted the new nested
     * PowerNode as a SIBLING of the old empty NumberNode instead of replacing it -- invisible in
     * the debug LaTeX (an empty NumberNode's toLatexString() is "", so `5^{^{}}` looked correct),
     * but a real extra empty box rendered next to the nested power. See
     * specs/editable_slots_sequencenode.md and specs/btn_x_power_y.md Section L.
     */
    public static void insertAtCursor(SequenceNode rootSequence, CursorPointer cursorPointer, ExpressionNode node) {
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
            if (isEmptyPlaceholder(cursorPointer.node)) {
                seq.removeChild(cursorPointer.node);
                seq.add(idx, node);
                return;
            }
            seq.add(cursorPointer.position == 0 ? idx : idx + 1, node);
            return;
        }
        rootSequence.add(node);
    }
}
