package com.calctastic.sample.hypercal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;

import org.junit.Test;

/**
 * Regression tests for the fraction family ("a/b", "a b/c") headlessly, same approach as
 * {@link CursorRegressionTest} -- see specs/testing_via_latex.md's "even faster tier" section for
 * why this works without an emulator (ExpressionEditor has no Android dependency) and the
 * NumberNode.cursorPosition sync pitfall to be aware of when writing new ones.
 *
 * Covers plain "a/b", mixed "a b/c", the fraction side of specs/btn_fraction.md Sections J/K
 * (whole-fraction delete + cursor-after-operator), and fraction/power nesting in both directions
 * (a power inside a fraction slot, and a fraction inside a power's exponent) since those two
 * button families interact and were only ever checked separately on-device before.
 */
public class FractionRegressionTest {

    private static ExpressionEditor newEditor() {
        SequenceNode root = new SequenceNode();
        return new ExpressionEditor(root, new CursorPointer(root, 0));
    }

    private static String latex(ExpressionEditor editor) {
        return editor.getRootSequence().toLatexString();
    }

    /**
     * "a/b" with an operand already typed: FractionInserter's Case 1 -- the "5" gets lifted into
     * the numerator, denominator starts empty, cursor lands in the denominator ready to type.
     */
    @Test
    public void plainFraction_liftsPrecedingOperandAsNumerator() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('5');
        editor.insertFraction();
        editor.appendDigit('3');

        assertEquals("\\frac{5}{3}", latex(editor));
    }

    /**
     * "a/b" with nothing to lift (fresh expression): FractionInserter's Case 2 -- both slots
     * start as empty placeholder boxes, cursor lands in the numerator first.
     */
    @Test
    public void plainFraction_withNothingToLift_startsWithBothSlotsEmpty() {
        ExpressionEditor editor = newEditor();
        editor.insertFraction();

        assertTrue("both slots should render as empty placeholders: " + latex(editor),
                latex(editor).equals("\\frac{}{}") || latex(editor).equals("\\frac{\\square}{\\square}"));

        editor.appendDigit('1');
        editor.moveCursorRight(); // FractionInserter's Case 3: numerator -> denominator
        editor.appendDigit('2');

        assertEquals("\\frac{1}{2}", latex(editor));
    }

    /**
     * "a b/c" with an operand already typed: lifted into the INTEGER part (not the numerator,
     * unlike plain a/b -- see FractionInserter.insertMixedFraction's doc comment), cursor lands
     * in the numerator. ▶ out of the numerator (mirroring plain a/b's own Case 3) reaches the
     * denominator.
     */
    @Test
    public void mixedFraction_liftsPrecedingOperandAsIntegerPart() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('7');
        editor.insertMixedFraction();
        editor.appendDigit('1');
        editor.moveCursorRight();
        editor.appendDigit('2');

        assertEquals("7\\ \\frac{1}{2}", latex(editor));
    }

    /**
     * "a b/c" with nothing to lift: a fresh mixed number, all three slots empty, cursor starts in
     * the integer part -- then ▶▶ walks integer part -> numerator -> denominator, exactly
     * mirroring the real keypad's arrow-key navigation.
     */
    @Test
    public void mixedFraction_withNothingToLift_cursorStartsInIntegerPart() {
        ExpressionEditor editor = newEditor();
        editor.insertMixedFraction();
        editor.appendDigit('3');
        editor.moveCursorRight(); // integer part -> numerator
        editor.appendDigit('1');
        editor.moveCursorRight(); // numerator -> denominator
        editor.appendDigit('4');

        assertEquals("3\\ \\frac{1}{4}", latex(editor));
    }

    /**
     * specs/btn_fraction.md Section J, fraction side: DEL at Center-after a fraction deletes the
     * WHOLE fraction outright, matching power's behavior (see
     * CursorRegressionTest#delAtCenterAfterPower_deletesWholePower for the power equivalent).
     */
    @Test
    public void delAtCenterAfterFraction_deletesWholeFraction() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('5');
        editor.insertFraction();
        editor.appendDigit('3');
        editor.moveCursorRight(); // out of the denominator -> Center-after the fraction

        editor.deleteChar();

        assertEquals("", latex(editor));
    }

    /**
     * specs/btn_fraction.md Section K, fraction side: deleting a whole fraction that sat right
     * after an operator must leave the cursor AFTER that operator, not before it (the
     * CursorNav.afterNode(operatorNode) pitfall documented in Section K's writeup).
     */
    @Test
    public void deletingFractionAfterOperator_leavesCursorAfterOperatorNotBefore() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('2');
        editor.appendOperator("+");
        editor.appendDigit('5');
        editor.insertFraction();
        editor.appendDigit('3');
        editor.moveCursorRight(); // Center-after the fraction

        editor.deleteChar(); // removes the whole fraction (Section J)

        assertEquals("2 + ", latex(editor));
        CursorPointer cp = editor.getCursorPointer();
        assertEquals(editor.getRootSequence(), cp.node);
        assertEquals("cursor must sit right AFTER the '+', not before it", 2, cp.position);
    }

    /**
     * specs/btn_fraction.md Section L, fraction side: DEL with the cursor resting just before a
     * fraction -- either still inside the numerator's own NumberNode at position 0, or having
     * escaped all the way to CursorPointer(frac, 0) -- must delete whatever precedes the fraction
     * in the OUTER sequence, not the fraction itself (mirrors
     * CursorRegressionTest#delBeforePower_deletesPrecedingTokenNotThePower_*).
     */
    @Test
    public void delBeforeFraction_deletesPrecedingTokenNotTheFraction() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('2');
        editor.appendOperator("+");
        editor.appendDigit('5');
        editor.insertFraction();
        editor.appendDigit('3');
        editor.moveCursorLeft(); // denominator NumberNode: pos 1 -> 0
        editor.moveCursorLeft(); // exit denominator -> end of numerator (numerator NumberNode pos 1)
        editor.moveCursorLeft(); // numerator NumberNode: pos 1 -> 0

        editor.deleteChar();

        assertEquals("2\\frac{5}{3}", latex(editor));
    }

    /**
     * Fraction nested inside a power's exponent: "5 xʸ (1/4)" -- xʸ leaves the exponent empty,
     * a/b then fills it since it's the current cursor's own empty slot content, not something
     * lifted from outside. Exercises the two button families together, which was previously only
     * ever checked separately, one button at a time, on-device.
     */
    @Test
    public void fractionNestedInsidePowerExponent() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('5');
        editor.insertPower();   // xʸ: base=5, cursor in the empty exponent
        editor.insertFraction(); // nothing to lift (cursor on the exponent's empty NumberNode)
        editor.appendDigit('1');
        editor.moveCursorRight(); // numerator -> denominator
        editor.appendDigit('4');

        assertEquals("5^{\\frac{1}{4}}", latex(editor));
    }

    /**
     * Power nested inside a fraction's denominator: "1/(2^3)" -- a/b lifts nothing into the
     * denominator (it's a fresh EmptyNode), xʸ then fills that slot directly, base=2, ▶ into the
     * exponent, exponent=3. The reverse direction of
     * {@link #fractionNestedInsidePowerExponent()}.
     */
    @Test
    public void powerNestedInsideFractionDenominator() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('1');
        editor.insertFraction(); // numerator=1, cursor in the empty denominator
        editor.insertPower();    // nothing to lift (cursor on the denominator's EmptyNode)
        editor.appendDigit('2'); // power's base
        editor.moveCursorRight(); // base -> exponent
        editor.appendDigit('3');

        assertEquals("\\frac{1}{2^{3}}", latex(editor));
    }

    /**
     * DEL at Center-after a power that lives inside a fraction's denominator must still delete
     * just that inner power (Section J's rule applies at any nesting depth), leaving the
     * denominator's own empty placeholder behind rather than touching the outer fraction.
     */
    @Test
    public void delAtCenterAfterPower_insideFractionDenominator_deletesOnlyTheInnerPower() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('1');
        editor.insertFraction();
        editor.insertPower();
        editor.appendDigit('2');
        editor.moveCursorRight(); // base -> exponent
        editor.appendDigit('3');
        editor.moveCursorRight(); // out of the exponent -> Center-after the inner power

        editor.deleteChar();

        assertTrue("outer fraction must survive with an empty denominator: " + latex(editor),
                latex(editor).equals("\\frac{1}{}") || latex(editor).equals("\\frac{1}{\\square}"));
    }
}
