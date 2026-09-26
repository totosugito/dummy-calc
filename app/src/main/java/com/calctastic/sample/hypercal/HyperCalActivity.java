package com.calctastic.sample.hypercal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.calctastic.sample.R;
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
import com.calctastic.sample.hypercal.view.HyperCalDisplayView;
import java.util.ArrayList;
import java.util.List;

/**
 * HyperCal Activity with HiPER Calc expression rendering.
 * Keypad click -> Expression AST -> Custom Canvas MathVisual render with cursor.
 * Clicking directly on any term or character in the display repositions the cursor.
 */
public class HyperCalActivity extends Activity implements View.OnClickListener {

    private HyperCalDisplayView displayView;
    private android.widget.TextView formulaTextView;
    private SequenceNode rootSequence;
    private CursorPointer cursorPointer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hypercal);

        displayView = findViewById(R.id.hypercal_display);
        formulaTextView = findViewById(R.id.formula_text);
        rootSequence = new SequenceNode();

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
        cursorPointer = new CursorPointer(pow.exponent, pow.exponent.getLength());
        displayView.setExpressionRoot(rootSequence);
        displayView.setCursorPointer(cursorPointer);
        updateFormulaText();

        // Listen for touch taps on display that reposition cursor (BE.java & UF.java)
        displayView.setOnCursorChangedListener(new HyperCalDisplayView.OnCursorChangedListener() {
            @Override
            public void onCursorChanged(CursorPointer pointer) {
                cursorPointer = pointer;
                updateFormulaText();
            }
        });

        setupKeypad();
    }

    private void updateFormulaText() {
        if (formulaTextView != null && rootSequence != null) {
            formulaTextView.setText(rootSequence.toLatexString());
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
                R.id.btn_sqrt, R.id.btn_fraction, R.id.btn_power, R.id.btn_square, R.id.btn_reciprocal,
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

        if (id == R.id.btn_0) appendDigit('0');
        else if (id == R.id.btn_1) appendDigit('1');
        else if (id == R.id.btn_2) appendDigit('2');
        else if (id == R.id.btn_3) appendDigit('3');
        else if (id == R.id.btn_4) appendDigit('4');
        else if (id == R.id.btn_5) appendDigit('5');
        else if (id == R.id.btn_6) appendDigit('6');
        else if (id == R.id.btn_7) appendDigit('7');
        else if (id == R.id.btn_8) appendDigit('8');
        else if (id == R.id.btn_9) appendDigit('9');
        else if (id == R.id.btn_dot) appendDigit('.');
        else if (id == R.id.btn_negate) toggleNegate();
        else if (id == R.id.btn_add) appendOperator("+");
        else if (id == R.id.btn_sub) appendOperator("−");
        else if (id == R.id.btn_mul) appendOperator("×");
        else if (id == R.id.btn_div) appendOperator("÷");
        else if (id == R.id.btn_percent) appendOperator("%");
        else if (id == R.id.btn_sqrt) insertSqrt();
        else if (id == R.id.btn_fraction) insertFraction();
        else if (id == R.id.btn_power) insertPower();
        else if (id == R.id.btn_square) insertSquare();
        else if (id == R.id.btn_reciprocal) insertReciprocal();
        else if (id == R.id.btn_paren_open) insertParenthesis();
        else if (id == R.id.btn_clear) clearAll();
        else if (id == R.id.btn_delete) deleteChar();
        else if (id == R.id.btn_cursor_left) moveCursorLeft();
        else if (id == R.id.btn_cursor_right) moveCursorRight();
        else if (id == R.id.btn_cursor_up) moveCursorVertical(false);
        else if (id == R.id.btn_cursor_down) moveCursorVertical(true);

        updateFormulaText();
        displayView.invalidate();
    }


    private void setCursor(CursorPointer pointer) {
        cursorPointer = pointer;
        displayView.setCursorPointer(pointer);
    }

    // ---- Fraction slot helpers: numerator/denominator are SequenceNode slots (HiPER GA inside C0067Lb) ----

    /** Returns the fraction owning this slot sequence, or null if it is not a fraction slot. */
    private static FractionNode fractionOfSlot(ExpressionNode slot) {
        if (slot instanceof SequenceNode && slot.getParent() instanceof FractionNode) {
            return (FractionNode) slot.getParent();
        }
        return null;
    }

    private static CursorPointer afterNode(ExpressionNode node) {
        if (node instanceof FractionNode) return new CursorPointer(node, 1); // Center after fraction
        if (node instanceof EmptyNode) return new CursorPointer(node, 0);
        return new CursorPointer(node, node.getLength());
    }

    private static CursorPointer startOf(SequenceNode seq) {
        if (seq.getChildCount() == 0) return new CursorPointer(seq, 0);
        return new CursorPointer(seq.getChild(0), 0);
    }

    private static CursorPointer endOf(SequenceNode seq) {
        if (seq.getChildCount() == 0) return new CursorPointer(seq, 0);
        return afterNode(seq.getChild(seq.getChildCount() - 1));
    }

    /**
     * Inserts a new token at the cursor inside the cursor's own sequence.
     * An EmptyNode placeholder under the cursor is replaced (QA -> real token).
     */
    private void insertAtCursor(ExpressionNode node) {
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
            if (cursorPointer.node instanceof EmptyNode) {
                seq.removeChild(cursorPointer.node);
                seq.add(idx, node);
                return;
            }
            seq.add(cursorPointer.position == 0 ? idx : idx + 1, node);
            return;
        }
        rootSequence.add(node);
    }

    private void appendDigit(char c) {
        if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            num.insertChar(c);
            setCursor(new CursorPointer(num, num.cursorPosition));
            return;
        }
        NumberNode num = new NumberNode(String.valueOf(c));
        num.cursorPosition = 1;
        insertAtCursor(num);
        setCursor(new CursorPointer(num, 1));
    }

    private void toggleNegate() {
        if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            num.toggleNegate();
            cursorPointer = new CursorPointer(num, num.cursorPosition);
            displayView.setCursorPointer(cursorPointer);
        }
    }

    private void appendOperator(String symbol) {
        OperatorNode op = new OperatorNode(symbol);
        insertAtCursor(op);
        setCursor(new CursorPointer(op, 1));
    }

    private void insertSqrt() {
        NumberNode inner = new NumberNode("");
        SqrtNode sqrt = new SqrtNode(inner);
        insertAtCursor(sqrt);
        setCursor(new CursorPointer(inner, 0));
    }

    /**
     * True when the token right before the cursor is an operand that a/b can lift into the
     * numerator. Any completed token qualifies, a fraction included: a/b right after a finished
     * fraction nests it as the numerator of a new outer fraction (spec Section 2, Case 1 applies
     * uniformly to "angka/token", not just numbers) -- e.g. 1/2 -> a/b -> (1/2)/[].
     */
    private static boolean hasOperandBeforeCursor(CursorPointer cp) {
        ExpressionNode n = cp.node;
        if (n instanceof EmptyNode || n instanceof OperatorNode || n instanceof SequenceNode) {
            return false;
        }
        return cp.position > 0;
    }

    /**
     * Faithful to HiPER Calc C0196hc.java & EA.m72HiPER:
     * - Case 3: cursor inside the numerator -> a/b jumps to the denominator.
     * - Case 1: an operand right before the cursor becomes the numerator, denominator is empty,
     *   and the cursor goes to the denominator.
     * - Case 2: nothing to lift (empty display, after an operator, on a placeholder) ->
     *   both slots are empty boxes and the cursor goes to the numerator.
     */
    private void insertFraction() {
        if (cursorPointer != null && cursorPointer.node != null) {
            ExpressionNode slot = cursorPointer.node instanceof SequenceNode
                    ? cursorPointer.node : cursorPointer.node.getParent();
            FractionNode host = fractionOfSlot(slot);
            if (host != null && slot == host.numerator) {
                setCursor(startOf(host.denominator));
                return;
            }

            ExpressionNode target = cursorPointer.node;
            if (hasOperandBeforeCursor(cursorPointer) && target.getParent() instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) target.getParent();
                int idx = seq.getChildIndex(target);
                seq.removeChild(target);
                FractionNode frac = new FractionNode(target, null);
                seq.add(idx, frac);
                setCursor(startOf(frac.denominator));
                return;
            }
        }

        FractionNode frac = new FractionNode(null, null);
        insertAtCursor(frac);
        setCursor(startOf(frac.numerator));
    }

    private void insertPower() {
        ExpressionNode targetBase = null;
        if (cursorPointer != null && cursorPointer.node != null) {
            targetBase = cursorPointer.node;
        } else if (rootSequence.getChildCount() > 0) {
            targetBase = rootSequence.getChild(rootSequence.getChildCount() - 1);
        } else {
            targetBase = new NumberNode("x");
        }

        if (targetBase.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) targetBase.getParent();
            int idx = parent.getChildIndex(targetBase);
            parent.removeChild(targetBase);
            NumberNode exp = new NumberNode("");
            PowerNode pow = new PowerNode(targetBase, exp, "xʸ");
            parent.add(idx, pow);
            cursorPointer = new CursorPointer(exp, 0);
        } else {
            NumberNode exp = new NumberNode("");
            PowerNode pow = new PowerNode(targetBase, exp, "xʸ");
            rootSequence.addChild(pow);
            cursorPointer = new CursorPointer(exp, 0);
        }
        displayView.setCursorPointer(cursorPointer);
    }

    private void insertSquare() {
        ExpressionNode targetBase = null;
        if (cursorPointer != null && cursorPointer.node != null) {
            targetBase = cursorPointer.node;
        } else if (rootSequence.getChildCount() > 0) {
            targetBase = rootSequence.getChild(rootSequence.getChildCount() - 1);
        } else {
            targetBase = new NumberNode("x");
        }

        if (targetBase.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) targetBase.getParent();
            int idx = parent.getChildIndex(targetBase);
            parent.removeChild(targetBase);
            NumberNode exp = new NumberNode("2");
            PowerNode pow = new PowerNode(targetBase, exp, "x²");
            parent.add(idx, pow);
            cursorPointer = new CursorPointer(pow, 1);
        } else {
            NumberNode exp = new NumberNode("2");
            PowerNode pow = new PowerNode(targetBase, exp, "x²");
            rootSequence.addChild(pow);
            cursorPointer = new CursorPointer(pow, 1);
        }
        displayView.setCursorPointer(cursorPointer);
    }

    private void insertReciprocal() {
        NumberNode one = new NumberNode("1");
        NumberNode den = new NumberNode("");
        FractionNode frac = new FractionNode(one, den);
        insertAtCursor(frac);
        setCursor(new CursorPointer(den, 0));
    }

    private void insertParenthesis() {
        NumberNode inner = new NumberNode("");
        ParenthesisNode paren = new ParenthesisNode(inner);
        insertAtCursor(paren);
        setCursor(new CursorPointer(inner, 0));
    }

    private void clearAll() {
        rootSequence = new SequenceNode();
        displayView.setExpressionRoot(rootSequence);
        cursorPointer = new CursorPointer(rootSequence, 0);
        displayView.setCursorPointer(cursorPointer);
    }

    /**
     * Removes a token from its sequence and places the cursor where it was.
     * A fraction slot that becomes empty gets its EmptyNode placeholder back.
     */
    private void removeFromSequence(ExpressionNode node) {
        if (!(node.getParent() instanceof SequenceNode)) {
            return;
        }
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);
        seq.removeChild(node);
        if (seq.getChildCount() == 0) {
            if (fractionOfSlot(seq) != null) {
                EmptyNode empty = new EmptyNode();
                seq.add(empty);
                setCursor(new CursorPointer(empty, 0));
            } else {
                setCursor(new CursorPointer(seq, 0));
            }
        } else if (idx > 0) {
            setCursor(afterNode(seq.getChild(idx - 1)));
        } else {
            setCursor(new CursorPointer(seq.getChild(0), 0));
        }
    }

    /** DEL with the cursor at the start of {@code node}: deletes whatever sits right before it. */
    private void deleteBefore(ExpressionNode node) {
        if (!(node.getParent() instanceof SequenceNode)) {
            return;
        }
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);
        if (idx <= 0) {
            return;
        }
        ExpressionNode prev = seq.getChild(idx - 1);
        if (prev instanceof FractionNode) {
            // DEL right after a fraction steps into its denominator
            setCursor(endOf(((FractionNode) prev).denominator));
            return;
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
        setCursor(new CursorPointer(node, 0));
    }

    /** Replaces a fraction whose denominator is empty with its numerator tokens (unwrapping). */
    private void unwrapFraction(FractionNode frac) {
        if (!(frac.getParent() instanceof SequenceNode)) {
            return;
        }
        if (FractionNode.isEmptySlot(frac.numerator)) {
            removeFromSequence(frac);
            return;
        }
        SequenceNode seq = (SequenceNode) frac.getParent();
        int idx = seq.getChildIndex(frac);
        seq.removeChild(frac);
        List<ExpressionNode> tokens = new ArrayList<>(frac.numerator.children);
        for (int i = 0; i < tokens.size(); i++) {
            seq.add(idx + i, tokens.get(i));
        }
        setCursor(afterNode(tokens.get(tokens.size() - 1)));
    }

    private void deleteChar() {
        if (cursorPointer == null || cursorPointer.node == null) {
            return;
        }
        ExpressionNode node = cursorPointer.node;

        // Cursor directly on a sequence (e.g. empty root)
        if (node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) node;
            if (cursorPointer.position > 0 && cursorPointer.position <= seq.getChildCount()) {
                removeFromSequence(seq.getChild(cursorPointer.position - 1));
            }
            return;
        }

        // Center slot before/after a fraction
        if (node instanceof FractionNode) {
            FractionNode frac = (FractionNode) node;
            if (cursorPointer.position == 1) {
                if (FractionNode.isEmptySlot(frac.denominator)) {
                    unwrapFraction(frac);
                } else {
                    setCursor(endOf(frac.denominator));
                }
            } else {
                deleteBefore(frac);
            }
            return;
        }

        // Empty placeholder box (QA)
        if (node instanceof EmptyNode) {
            FractionNode frac = fractionOfSlot(node.getParent());
            if (frac == null) {
                removeFromSequence(node);
            } else if (node.getParent() == frac.denominator) {
                // DEL in empty denominator jumps to the end of the numerator
                setCursor(endOf(frac.numerator));
            } else if (FractionNode.isEmptySlot(frac.denominator)) {
                // Both slots empty: remove the whole fraction
                removeFromSequence(frac);
            } else {
                // Numerator empty but denominator filled: keep the numerator box visible
                setCursor(new CursorPointer(frac, 0));
            }
            return;
        }

        if (node instanceof NumberNode) {
            NumberNode num = (NumberNode) node;
            if (num.deleteChar()) {
                if (num.getLength() == 0 && num.getParent() instanceof SequenceNode) {
                    // Number emptied: drop it (a fraction slot gets its EmptyNode back)
                    removeFromSequence(num);
                } else {
                    setCursor(new CursorPointer(num, num.cursorPosition));
                }
                return;
            }
            if (num.getLength() > 0 || num.getParent() instanceof SequenceNode) {
                deleteBefore(num);
                return;
            }
            // Empty number inside sqrt/parenthesis/power: remove the wrapper
            if (num.getParent() != null) {
                removeFromSequence(num.getParent());
            }
            return;
        }

        removeFromSequence(node);
    }

    /** ▲ / ▼: numerator <-> denominator, faithful to Qg.HiPER(PointF, Df) / Qg.E(PointF, Df). */
    private void moveCursorVertical(boolean down) {
        CursorPointer target = displayView.findVerticalCursorTarget(down);
        if (target != null) {
            setCursor(target);
        }
    }

    private void moveCursorLeft() {
        if (cursorPointer == null || cursorPointer.node == null) return;
        ExpressionNode node = cursorPointer.node;

        // Inside NumberNode: move left character-by-character
        if (node instanceof NumberNode && cursorPointer.position > 0) {
            setCursor(new CursorPointer(node, cursorPointer.position - 1));
            return;
        }

        // Center after fraction: step left into the end of the denominator
        if (node instanceof FractionNode && cursorPointer.position == 1) {
            setCursor(endOf(((FractionNode) node).denominator));
            return;
        }

        if (!(node.getParent() instanceof SequenceNode)) return;
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);

        if (idx > 0) {
            setCursor(afterNode(seq.getChild(idx - 1)));
            return;
        }

        // At the start of a fraction slot
        FractionNode frac = fractionOfSlot(seq);
        if (frac != null) {
            if (seq == frac.denominator) {
                setCursor(endOf(frac.numerator));
            } else {
                setCursor(new CursorPointer(frac, 0)); // Center before fraction
            }
        }
    }

    private void moveCursorRight() {
        if (cursorPointer == null || cursorPointer.node == null) return;
        ExpressionNode node = cursorPointer.node;

        // Inside NumberNode: move right character-by-character
        if (node instanceof NumberNode && cursorPointer.position < node.getLength()) {
            setCursor(new CursorPointer(node, cursorPointer.position + 1));
            return;
        }

        // Center before fraction: step right into the start of the numerator
        if (node instanceof FractionNode && cursorPointer.position == 0) {
            setCursor(startOf(((FractionNode) node).numerator));
            return;
        }

        if (!(node.getParent() instanceof SequenceNode)) return;
        SequenceNode seq = (SequenceNode) node.getParent();
        int idx = seq.getChildIndex(node);

        if (idx >= 0 && idx < seq.getChildCount() - 1) {
            ExpressionNode next = seq.getChild(idx + 1);
            if (next instanceof NumberNode || next instanceof FractionNode || next instanceof EmptyNode) {
                setCursor(new CursorPointer(next, 0));
            } else {
                setCursor(afterNode(next));
            }
            return;
        }

        // At the end of a fraction slot
        FractionNode frac = fractionOfSlot(seq);
        if (frac != null) {
            if (seq == frac.numerator) {
                setCursor(startOf(frac.denominator));
            } else {
                setCursor(new CursorPointer(frac, 1)); // Center after fraction
            }
        }
    }
}
