package com.calctastic.sample.hypercal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.calctastic.sample.R;
import com.calctastic.sample.hypercal.engine.CursorNav;
import com.calctastic.sample.hypercal.engine.ExpressionEditor;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.FractionNode;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.OperatorNode;
import com.calctastic.sample.hypercal.engine.model.PowerNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;
import com.calctastic.sample.hypercal.view.HyperCalDisplayView;

/**
 * HyperCal Activity with HiPER Calc expression rendering.
 * Keypad click -> Expression AST -> Custom Canvas MathVisual render with cursor.
 * Clicking directly on any term or character in the display repositions the cursor.
 *
 * This class only wires the keypad/View together; the actual insert/delete/cursor-navigation
 * logic lives in {@link ExpressionEditor} (moved out 2026-09-26 to keep this file to UI wiring --
 * see specs/btn_fraction.md and specs/display_scaling_typography.md for where things moved).
 */
public class HyperCalActivity extends Activity implements View.OnClickListener {

    private HyperCalDisplayView displayView;
    private android.widget.TextView formulaTextView;
    private ExpressionEditor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hypercal);

        displayView = findViewById(R.id.hypercal_display);
        formulaTextView = findViewById(R.id.formula_text);

        SequenceNode rootSequence = new SequenceNode();
        editor = new ExpressionEditor(rootSequence, new CursorPointer(rootSequence, 0));

        // Sample initial expression to showcase HiPER rendering immediately
        NumberNode n1 = new NumberNode("5");
        SqrtNode sqrt = new SqrtNode(new NumberNode("9"));
        OperatorNode plus = new OperatorNode("+");
        FractionNode frac = new FractionNode(new NumberNode("1"), new NumberNode("2"));
        OperatorNode mul = new OperatorNode("×");
        PowerNode pow = new PowerNode(new NumberNode("3"), new NumberNode("2"), "x²");

        rootSequence.addChild(n1);
        rootSequence.addChild(plus);
        rootSequence.addChild(sqrt);
        rootSequence.addChild(new OperatorNode("+"));
        rootSequence.addChild(frac);
        rootSequence.addChild(mul);
        rootSequence.addChild(pow);

        // Put cursor in pow exponent
        editor.setCursorPointer(CursorNav.endOf(pow.exponent));
        displayView.setExpressionRoot(rootSequence);
        displayView.setCursorPointer(editor.getCursorPointer());
        updateFormulaText();

        // Listen for touch taps on display that reposition cursor (BE.java & UF.java)
        displayView.setOnCursorChangedListener(new HyperCalDisplayView.OnCursorChangedListener() {
            @Override
            public void onCursorChanged(CursorPointer pointer) {
                editor.setCursorPointer(pointer);
                updateFormulaText();
            }
        });

        setupKeypad();
    }

    private void updateFormulaText() {
        if (formulaTextView != null) {
            formulaTextView.setText(editor.getRootSequence().toLatexString());
        }
    }

    private void setupKeypad() {
        int[] buttonIds = new int[]{
                // Digits
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                // Dot & Negate
                R.id.btn_dot, R.id.btn_negate,
                // Operators
                R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div, R.id.btn_percent,
                // Functions
                R.id.btn_sqrt, R.id.btn_fraction, R.id.btn_mixed_fraction, R.id.btn_power, R.id.btn_square, R.id.btn_reciprocal,
                R.id.btn_cube, R.id.btn_neg_one,
                R.id.btn_paren_open, R.id.btn_paren_close,
                // Edit & Nav
                R.id.btn_clear, R.id.btn_delete, R.id.btn_cursor_left, R.id.btn_cursor_right,
                R.id.btn_cursor_up, R.id.btn_cursor_down
        };

        for (int id : buttonIds) {
            View b = findViewById(id);
            if (b != null) {
                b.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_0) editor.appendDigit('0');
        else if (id == R.id.btn_1) editor.appendDigit('1');
        else if (id == R.id.btn_2) editor.appendDigit('2');
        else if (id == R.id.btn_3) editor.appendDigit('3');
        else if (id == R.id.btn_4) editor.appendDigit('4');
        else if (id == R.id.btn_5) editor.appendDigit('5');
        else if (id == R.id.btn_6) editor.appendDigit('6');
        else if (id == R.id.btn_7) editor.appendDigit('7');
        else if (id == R.id.btn_8) editor.appendDigit('8');
        else if (id == R.id.btn_9) editor.appendDigit('9');
        else if (id == R.id.btn_dot) editor.appendDigit('.');
        else if (id == R.id.btn_negate) editor.toggleNegate();
        else if (id == R.id.btn_add) editor.appendOperator("+");
        else if (id == R.id.btn_sub) editor.appendOperator("−");
        else if (id == R.id.btn_mul) editor.appendOperator("×");
        else if (id == R.id.btn_div) editor.appendOperator("÷");
        else if (id == R.id.btn_percent) editor.appendOperator("%");
        else if (id == R.id.btn_sqrt) editor.insertSqrt();
        else if (id == R.id.btn_fraction) editor.insertFraction();
        else if (id == R.id.btn_mixed_fraction) editor.insertMixedFraction();
        else if (id == R.id.btn_power) editor.insertPower();
        else if (id == R.id.btn_square) editor.insertSquare();
        else if (id == R.id.btn_cube) editor.insertCube();
        else if (id == R.id.btn_neg_one) editor.insertNegativeOnePower();
        else if (id == R.id.btn_reciprocal) editor.insertReciprocal();
        else if (id == R.id.btn_paren_open) editor.insertParenthesis();
        else if (id == R.id.btn_clear) clearAll();
        else if (id == R.id.btn_delete) editor.deleteChar();
        else if (id == R.id.btn_cursor_left) editor.moveCursorLeft();
        else if (id == R.id.btn_cursor_right) editor.moveCursorRight();
        else if (id == R.id.btn_cursor_up) moveCursorVertical(false);
        else if (id == R.id.btn_cursor_down) moveCursorVertical(true);

        displayView.setCursorPointer(editor.getCursorPointer());
        updateFormulaText();
        displayView.invalidate();
    }

    private void clearAll() {
        editor.reset();
        displayView.setExpressionRoot(editor.getRootSequence());
    }

    /**
     * ▲ / ▼: numerator <-> denominator, faithful to Qg.HiPER(PointF, Df) / Qg.E(PointF, Df).
     * Stays here (not in ExpressionEditor) because it needs the laid-out visual tree
     * (font metrics/Paint) to find the target -- see HyperCalDisplayView.findVerticalCursorTarget.
     */
    private void moveCursorVertical(boolean down) {
        CursorPointer target = displayView.findVerticalCursorTarget(down);
        if (target != null) {
            editor.setCursorPointer(target);
        }
    }
}
