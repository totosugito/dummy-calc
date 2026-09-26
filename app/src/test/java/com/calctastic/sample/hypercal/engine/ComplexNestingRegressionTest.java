package com.calctastic.sample.hypercal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;

import org.junit.Test;

/**
 * Deeper, multi-button-family nesting scenarios, still headless (see
 * specs/testing_via_latex.md's "even faster tier" section) -- fraction inside exponent inside
 * fraction, power of power, a variable-like symbol mixed with both, and deleting one level of a
 * deep nest without disturbing the levels around it. {@link CursorRegressionTest} and
 * {@link FractionRegressionTest} each cover one button family (plus one level of cross-nesting);
 * this file goes further, chaining three or four levels deep, closer to what a real user might
 * actually type.
 *
 * Note on "variables": this app's model has no dedicated variable/symbol node -- NumberNode's
 * {@code insertChar} accepts any {@code char} with no digit validation (the engine layer doesn't
 * distinguish "2" from "x"), so {@code appendDigit('x')} is used below to stand in for a variable
 * key that isn't implemented as a separate button yet. This exercises the same code path a real
 * variable button would.
 */
public class ComplexNestingRegressionTest {

    private static ExpressionEditor newEditor() {
        SequenceNode root = new SequenceNode();
        return new ExpressionEditor(root, new CursorPointer(root, 0));
    }

    private static String latex(ExpressionEditor editor) {
        return editor.getRootSequence().toLatexString();
    }

    /**
     * A fraction raised to a fractional power: (1/2)^(3/4). The whole fraction gets lifted as the
     * power's base (FractionNode is a valid "operand before cursor" for
     * CursorNav.hasOperandBeforeCursor, same as any other node type), then a second, independent
     * fraction is built from scratch inside the empty exponent.
     */
    @Test
    public void fractionRaisedToAFractionalPower() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('1');
        editor.insertFraction();
        editor.appendDigit('2');
        editor.moveCursorRight(); // out of the denominator -> Center-after the fraction
        editor.insertPower();     // lifts the WHOLE fraction as the power's base
        editor.insertFraction();  // nothing to lift (cursor on the exponent's empty NumberNode)
        editor.appendDigit('3');
        editor.moveCursorRight(); // numerator -> denominator
        editor.appendDigit('4');

        assertEquals("\\frac{1}{2}^{\\frac{3}{4}}", latex(editor));
    }

    /**
     * Three levels deep in one denominator: 1 / (2 ^ (3 ^ (4/5))). Each xʸ/a-b nests into the
     * previous one's still-empty slot rather than lifting anything, so this is really testing
     * that {@code insertAtCursor}'s empty-placeholder replacement (specs/editable_slots_
     * sequencenode.md) keeps working correctly no matter how many levels deep it's called.
     */
    @Test
    public void threeLevelsDeep_powerOfPowerOfFraction_insideAFractionDenominator() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('1');
        editor.insertFraction();  // numerator=1, cursor in the empty denominator
        editor.insertPower();     // nothing to lift -> power fills the denominator, base empty
        editor.appendDigit('2');
        editor.moveCursorRight(); // base -> exponent
        editor.insertPower();     // nested power fills the (empty) exponent, base empty
        editor.appendDigit('3');
        editor.moveCursorRight(); // inner base -> inner exponent
        editor.insertFraction();  // fraction fills the innermost (empty) exponent
        editor.appendDigit('4');
        editor.moveCursorRight();
        editor.appendDigit('5');

        assertEquals("\\frac{1}{2^{3^{\\frac{4}{5}}}}", latex(editor));
    }

    /**
     * A variable-like symbol squared, over another variable-like symbol: x^2 / y. Exercises
     * appendDigit with a non-digit character (see class doc) plus lifting a WHOLE power (not just
     * a bare NumberNode) as a fraction's numerator.
     */
    @Test
    public void variableSquaredOverVariable() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('x');
        editor.insertSquare();   // x² -- exponent pre-filled, cursor lands Center-after the power
        editor.insertFraction(); // lifts the WHOLE x^2 power as the numerator
        editor.appendDigit('y');

        assertEquals("\\frac{x^{2}}{y}", latex(editor));
    }

    /**
     * Power of a power: (x^(1/2))^3 -- a variable base, a fractional exponent, and the entire
     * resulting power then itself raised to a plain numeric power. Chains xʸ twice in a row, each
     * time lifting whatever the cursor just finished building as the next power's base.
     *
     * This also surfaces a real ambiguity worth knowing about rather than silently accepting:
     * {@link com.calctastic.sample.hypercal.engine.model.PowerNode#needsParenthesesForBase()}
     * only adds parens around the base for a compound SequenceNode (2+ tokens), a negative
     * NumberNode, or a bare OperatorNode -- a nested PowerNode or FractionNode used as a base
     * does NOT get parens. So the LaTeX this produces, "x^{\frac{1}{2}}^{3}", is visually
     * ambiguous (looks like two stacked superscripts on the same "x", not "x^(1/2)" all raised to
     * the 3rd power) even though the underlying AST is unambiguous. Not fixed here -- this test
     * exists to document current, exact behavior so a future rendering fix doesn't accidentally
     * change the model-level LaTeX shape without someone noticing.
     */
    @Test
    public void powerOfAPowerWithAFractionalInnerExponent() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('x');
        editor.insertPower();     // xʸ: base=x, cursor in the empty exponent
        editor.insertFraction();  // fraction fills the exponent
        editor.appendDigit('1');
        editor.moveCursorRight();
        editor.appendDigit('2');
        editor.moveCursorRight(); // out of the fraction -> still inside the (outer) exponent slot
        editor.moveCursorRight(); // out of the exponent -> Center-after the whole inner power
        editor.insertPower();     // lifts the WHOLE inner power as the new outer power's base
        editor.appendDigit('3');

        assertEquals("x^{\\frac{1}{2}}^{3}", latex(editor));
    }

    /**
     * Deleting one level of a deep nest (Center-after DEL on the innermost power, three levels
     * down inside a fraction's denominator) must remove only that level -- the fraction and the
     * outer power around it must survive untouched, with the immediate parent slot correctly
     * refilled by its own placeholder convention (NumberNode for a power's base/exponent slot,
     * per specs/editable_slots_sequencenode.md).
     */
    @Test
    public void deletingInnermostOfADeepNest_onlyRemovesThatLevel() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('1');
        editor.insertFraction();
        editor.insertPower();
        editor.appendDigit('2');
        editor.moveCursorRight();
        editor.insertPower();
        editor.appendDigit('3');
        editor.moveCursorRight(); // out of the innermost exponent -> Center-after the inner power

        editor.deleteChar(); // remove only "^{3}"'s power -- the "3" and its wrapper

        assertTrue("outer power's exponent should be back to an empty placeholder: " + latex(editor),
                latex(editor).equals("\\frac{1}{2^{}}"));
    }
}
