package com.calctastic.sample.hypercal.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.PowerNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;

import org.junit.Test;

/**
 * Regression tests replaying, in plain JVM code, the button-press sequences that were previously
 * only checkable by tapping the emulator and reading either a screenshot or the debug LaTeX
 * dump (see specs/testing_via_latex.md). ExpressionEditor and everything it delegates to
 * (CursorNav, CursorDelete, the inserters, the model classes) has no Android dependency, so the
 * exact same button-driven scenarios can be asserted here directly against
 * {@link ExpressionEditor#getRootSequence()}'s LaTeX and {@link ExpressionEditor#getCursorPointer()}
 * -- no adb, no uiautomator, no screenshot.
 *
 * Each test's comment names the specs/*.md section it reproduces, so a future change that
 * breaks one of these fixes points straight back at the writeup explaining why the behavior
 * matters.
 */
public class CursorRegressionTest {

    private static ExpressionEditor newEditor() {
        SequenceNode root = new SequenceNode();
        return new ExpressionEditor(root, new CursorPointer(root, 0));
    }

    private static String latex(ExpressionEditor editor) {
        return editor.getRootSequence().toLatexString();
    }

    /** specs/btn_x_power_y.md Section K: "5 +" then xʸ must not lift the "+" as the base. */
    @Test
    public void powerAfterOperator_doesNotLiftTheOperatorAsBase() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('5');
        editor.appendOperator("+");
        editor.insertPower();

        assertTrue("base must not contain the '+' operator: " + latex(editor),
                !latex(editor).contains("+)") && !latex(editor).contains("+}"));
        assertTrue("cursor should land in the fresh empty base",
                editor.getCursorPointer().node instanceof NumberNode
                        && ((NumberNode) editor.getCursorPointer().node).getLength() == 0);
    }

    /**
     * specs/btn_x_power_y.md Section L (insertAtCursor half): nesting xʸ inside an already-empty
     * exponent must REPLACE the empty placeholder, not leave it behind as an invisible sibling
     * box next to the new nested power.
     */
    @Test
    public void nestedPowerInEmptyExponent_doesNotLeaveStrayPlaceholder() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('5');
        editor.insertPower(); // cursor now in the (empty) exponent
        editor.insertPower(); // nest another power into that same empty exponent
        editor.appendDigit('3');

        assertEquals("5^{3^{}}", latex(editor));

        PowerNode outer = (PowerNode) editor.getRootSequence().getChild(0);
        assertEquals("outer exponent slot must hold exactly the nested power, no leftover box",
                1, outer.exponent.getChildCount());
    }

    /**
     * specs/btn_fraction.md Section J: DEL right after a power (Center-after) deletes the WHOLE
     * power in one press, matching the (later-changed-to-match) fraction behavior.
     */
    @Test
    public void delAtCenterAfterPower_deletesWholePower() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('5');
        editor.insertPower();
        editor.appendDigit('3');
        editor.moveCursorRight(); // out of the exponent -> Center-after the power

        editor.deleteChar();

        assertEquals("", latex(editor));
    }

    /**
     * specs/btn_fraction.md Section L: DEL with the cursor resting just before a power -- either
     * still inside the base's own NumberNode at position 0 (three ◀ presses), or having escaped
     * all the way to CursorPointer(pow, 0) (four ◀ presses) -- must delete whatever precedes the
     * power in the OUTER sequence (the "+"), not the power itself, and not silently do nothing.
     */
    @Test
    public void delBeforePower_deletesPrecedingTokenNotThePower_threeLefts() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('2');
        editor.appendOperator("+");
        editor.appendDigit('5');
        editor.insertPower();
        editor.appendDigit('3');
        editor.moveCursorLeft(); // exponent NumberNode: pos 1 -> 0
        editor.moveCursorLeft(); // exit exponent -> end of base (base NumberNode pos 1)
        editor.moveCursorLeft(); // base NumberNode: pos 1 -> 0 (dead keystroke before the fix)

        editor.deleteChar();

        assertEquals("25^{3}", latex(editor));
    }

    @Test
    public void delBeforePower_deletesPrecedingTokenNotThePower_fourLefts() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('2');
        editor.appendOperator("+");
        editor.appendDigit('5');
        editor.insertPower();
        editor.appendDigit('3');
        editor.moveCursorLeft();
        editor.moveCursorLeft();
        editor.moveCursorLeft();
        editor.moveCursorLeft(); // now genuinely CursorPointer(pow, 0) -- Center-before

        editor.deleteChar();

        assertEquals("25^{3}", latex(editor));
    }

    /**
     * specs/btn_fraction.md Section K: deleting a compound node (here, a whole power) that sat
     * right after an operator must leave the cursor AFTER that operator, not before it.
     */
    @Test
    public void deletingPowerAfterOperator_leavesCursorAfterOperatorNotBefore() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('2');
        editor.appendOperator("+");
        editor.appendDigit('5');
        editor.insertPower();
        editor.appendDigit('3');
        editor.moveCursorRight(); // Center-after the power

        editor.deleteChar(); // removes the whole power (Section J)

        assertEquals("2 + ", latex(editor));
        CursorPointer cp = editor.getCursorPointer();
        assertEquals(editor.getRootSequence(), cp.node);
        assertEquals("cursor must sit right AFTER the '+', not before it", 2, cp.position);
    }

    /** Plain digit delete must be unaffected by all of the above fixes. */
    @Test
    public void plainDigitDelete_stillWorks() {
        ExpressionEditor editor = newEditor();
        editor.appendDigit('2');
        editor.appendOperator("+");
        editor.appendDigit('3');

        editor.deleteChar();

        assertEquals("2 + ", latex(editor));
    }
}
