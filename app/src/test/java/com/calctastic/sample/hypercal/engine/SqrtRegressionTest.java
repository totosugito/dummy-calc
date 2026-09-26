package com.calctastic.sample.hypercal.engine;

import static org.junit.Assert.assertEquals;

import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;

import org.junit.Test;

/**
 * Regression tests for the radical family ("√x", "ⁿ√x"), headlessly (see
 * specs/testing_via_latex.md's "even faster tier" section) -- same approach as
 * {@link CursorRegressionTest} and {@link FractionRegressionTest}.
 *
 * Unlike fraction/power, a radical is a PREFIX operator: pressing √x never lifts a preceding
 * operand (see SqrtInserter's class doc for why), so there is no "lifts the operand before it"
 * test here the way there is for fraction/power's Case 1.
 */
public class SqrtRegressionTest {

    private static ExpressionEditor newEditor() {
        SequenceNode root = new SequenceNode();
        return new ExpressionEditor(root, new CursorPointer(root, 0));
    }

    private static String latex(ExpressionEditor editor) {
        return editor.getRootSequence().toLatexString();
    }

    /** "√x": nothing is lifted from before the cursor -- it always inserts a fresh, empty radical. */
    @Test
    public void plainSqrt_neverLiftsAPrecedingOperand() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('5');
        editor.insertSqrt();
        editor.appendDigit('9');

        assertEquals("5\\sqrt{9}", latex(editor));
    }

    /** "ⁿ√x": cursor starts in the degree (typed first), ▶ moves into the radicand. */
    @Test
    public void nthRoot_cursorStartsInDegreeThenMovesToRadicand() {
        ExpressionEditor editor = newEditor();
        editor.insertNthRoot();
        editor.appendDigit('3');
        editor.moveCursorRight(); // degree -> radicand
        editor.appendDigit('8');

        assertEquals("\\sqrt[3]{8}", latex(editor));
    }

    /** ◀ from the start of the radicand steps back into the end of the degree, and vice versa. */
    @Test
    public void nthRoot_leftRightNavigationBetweenDegreeAndRadicand() {
        ExpressionEditor editor = newEditor();
        editor.insertNthRoot();
        editor.appendDigit('3');
        editor.moveCursorRight();
        editor.appendDigit('8');
        editor.moveCursorLeft();  // radicand NumberNode: pos 1 -> 0
        editor.moveCursorLeft();  // exit radicand -> end of degree
        editor.appendDigit('4');  // "34" as the degree now

        assertEquals("\\sqrt[34]{8}", latex(editor));
    }

    /**
     * DEL at Center-after a radical deletes the WHOLE radical outright, matching power/fraction
     * (specs/btn_fraction.md Section J's rule, now applied to sqrt too -- see specs/btn_sqrt.md).
     */
    @Test
    public void delAtCenterAfterSqrt_deletesWholeSqrt() {
        ExpressionEditor editor = newEditor();
        editor.insertSqrt();
        editor.appendDigit('9');
        editor.moveCursorRight(); // out of the radicand -> Center-after the sqrt

        editor.deleteChar();

        assertEquals("", latex(editor));
    }

    /**
     * DEL with the cursor resting just before a radical (either still inside the radicand's own
     * NumberNode at position 0, no degree present so there's nothing to step back into first, or
     * having escaped to CursorPointer(sqrt, 0)) must delete whatever precedes the radical in the
     * OUTER sequence, not the radical itself -- mirrors
     * CursorRegressionTest#delBeforePower_deletesPrecedingTokenNotThePower_fourLefts.
     */
    @Test
    public void delBeforeSqrt_deletesPrecedingTokenNotTheSqrt() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('2');
        editor.appendOperator("+");
        editor.insertSqrt();
        editor.appendDigit('9');
        editor.moveCursorLeft(); // radicand NumberNode: pos 1 -> 0
        editor.moveCursorLeft(); // escape to CursorPointer(sqrt, 0) -- Center-before

        editor.deleteChar();

        assertEquals("2\\sqrt{9}", latex(editor));
    }

    /** A radical nested inside a fraction's denominator: 1/√9. */
    @Test
    public void sqrtNestedInsideFractionDenominator() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('1');
        editor.insertFraction(); // numerator=1, cursor in the empty denominator
        editor.insertSqrt();     // fills the denominator slot directly, radicand empty
        editor.appendDigit('9');

        assertEquals("\\frac{1}{\\sqrt{9}}", latex(editor));
    }

    /** A fraction nested inside a radical: √(1/4). */
    @Test
    public void fractionNestedInsideSqrtRadicand() {
        ExpressionEditor editor = newEditor();
        editor.insertSqrt();     // fresh empty radicand
        editor.insertFraction(); // fills the radicand slot directly
        editor.appendDigit('1');
        editor.moveCursorRight();
        editor.appendDigit('4');

        assertEquals("\\sqrt{\\frac{1}{4}}", latex(editor));
    }

    /** A power nested inside an n-th root's radicand: ³√(2^5). */
    @Test
    public void powerNestedInsideNthRootRadicand() {
        ExpressionEditor editor = newEditor();
        editor.insertNthRoot();
        editor.appendDigit('3');
        editor.moveCursorRight(); // degree -> radicand
        editor.insertPower();     // fills the radicand slot directly, base empty
        editor.appendDigit('2');
        editor.moveCursorRight(); // base -> exponent
        editor.appendDigit('5');

        assertEquals("\\sqrt[3]{2^{5}}", latex(editor));
    }
}
