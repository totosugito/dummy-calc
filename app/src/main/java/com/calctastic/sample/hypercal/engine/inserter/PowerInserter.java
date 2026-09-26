package com.calctastic.sample.hypercal.engine.inserter;

import com.calctastic.sample.hypercal.engine.CursorNav;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.PowerNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;

/**
 * Insert logic for the power family of keypad buttons: "xʸ", "x²", "x³" and "x⁻¹". Pulled out of
 * ExpressionEditor (2026-09-26) -- see specs/btn_x_power_y.md and specs/btn_x_square.md for the
 * button-by-button reasoning (including the "()" decorative-parens and StackOverflow bugs found
 * along the way); this class only holds the code.
 *
 * Each public method is static and takes the current {@code rootSequence}/{@code cursorPointer}
 * explicitly (no shared editor state) and returns the new {@link CursorPointer} to apply --
 * ExpressionEditor calls these and stores the result itself.
 */
public final class PowerInserter {
    private PowerInserter() {
    }

    /**
     * "xʸ" general power button. The base is always shown in parentheses (confirmed against the
     * real app: pressing xʸ with nothing to lift produces "(□)^□"), but -- also per the user's
     * own re-check against the real app -- that "()" is purely decorative rendering
     * ({@link PowerNode#needsParenthesesForBase()}), not a real navigable ParenthesisNode. So the
     * base here is a plain operand, exactly like x²/x³/x⁻¹ (see {@link #insertPowerNode}); only
     * the exponent-empty/cursor-in-exponent behavior differs.
     */
    public static CursorPointer insertPower(SequenceNode rootSequence, CursorPointer cursorPointer) {
        return insertPowerNode(rootSequence, cursorPointer, "", "xʸ", true);
    }

    public static CursorPointer insertSquare(SequenceNode rootSequence, CursorPointer cursorPointer) {
        return insertPowerNode(rootSequence, cursorPointer, "2", "x²", false);
    }

    public static CursorPointer insertCube(SequenceNode rootSequence, CursorPointer cursorPointer) {
        return insertPowerNode(rootSequence, cursorPointer, "3", "x³", false);
    }

    /** "x⁻¹" -- reciprocal expressed as a power with a literal negative-one exponent. */
    public static CursorPointer insertNegativeOnePower(SequenceNode rootSequence, CursorPointer cursorPointer) {
        return insertPowerNode(rootSequence, cursorPointer, "-1", "x⁻¹", false);
    }

    /**
     * Shared base-selection + wrap-into-PowerNode logic behind "xʸ", "x²", "x³" and "x⁻¹".
     * Wraps whatever operand sits at the cursor bare (parentheses around xʸ's base, if any, are
     * purely decorative rendering via {@link PowerNode#needsParenthesesForBase()} -- there is no
     * separate ParenthesisNode -- see specs/btn_x_power_y.md).
     *
     * @param exponentText   initial exponent content ("" for xʸ so the user types it in)
     * @param symbol         display symbol stored on the PowerNode (used by PowerVisual/spec)
     * @param cursorInExponent when an operand IS lifted: true lands the cursor in the exponent
     *                         (xʸ, whose exponent is genuinely empty), false lands it after the
     *                         whole power (x², x³, x⁻¹, whose exponent is pre-filled already).
     *                         When nothing is lifted, the cursor always goes into the fresh empty
     *                         base instead, regardless of this flag -- jumping straight to an
     *                         already-filled exponent, or past a power with an empty base, would
     *                         make no sense for the user to type into next.
     */
    private static CursorPointer insertPowerNode(SequenceNode rootSequence, CursorPointer cursorPointer,
            String exponentText, String symbol, boolean cursorInExponent) {
        // cursorPointer.node can itself be a SequenceNode (e.g. a fresh/emptied expression, see
        // ExpressionEditor#reset()) -- that must NOT be used as the base: PowerNode's constructor
        // would reassign the sequence's own parent to the new PowerNode, which then gets added as
        // a child of that very same sequence, creating a base<->child cycle (confirmed via a
        // StackOverflowError in toLatexString when this shipped without the check below).
        ExpressionNode targetBase = null;
        if (cursorPointer != null && cursorPointer.node != null
                && !(cursorPointer.node instanceof SequenceNode)) {
            targetBase = cursorPointer.node;
        } else if (rootSequence.getChildCount() > 0
                && !(rootSequence.getChild(rootSequence.getChildCount() - 1) instanceof SequenceNode)) {
            targetBase = rootSequence.getChild(rootSequence.getChildCount() - 1);
        }

        if (targetBase == null) {
            NumberNode baseNum = new NumberNode("");
            NumberNode exp = new NumberNode(exponentText);
            PowerNode pow = new PowerNode(baseNum, exp, symbol);
            CursorNav.insertAtCursor(rootSequence, cursorPointer, pow);
            return new CursorPointer(baseNum, 0);
        }

        // Resolve targetBase's parent/index BEFORE constructing PowerNode -- its constructor
        // reassigns targetBase's parent to itself, which would otherwise make the
        // "is it in a sequence" check below see the wrong parent (the bug that caused
        // "5" -> x² to produce "55^{2}": the original NumberNode was left behind because this
        // check no longer saw the SequenceNode once the PowerNode had already claimed it).
        SequenceNode parent = targetBase.getParent() instanceof SequenceNode
                ? (SequenceNode) targetBase.getParent() : null;
        int idx = parent != null ? parent.getChildIndex(targetBase) : -1;
        if (parent != null) {
            parent.removeChild(targetBase);
        }

        NumberNode exp = new NumberNode(exponentText);
        PowerNode pow = new PowerNode(targetBase, exp, symbol);

        if (parent != null) {
            parent.add(idx, pow);
        } else {
            rootSequence.addChild(pow);
        }

        return cursorInExponent ? new CursorPointer(exp, 0) : new CursorPointer(pow, 1);
    }
}
