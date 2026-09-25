package com.calctastic.sample.hypercal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.calctastic.sample.R;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.FractionNode;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.OperatorNode;
import com.calctastic.sample.hypercal.engine.model.ParenthesisNode;
import com.calctastic.sample.hypercal.engine.model.PowerNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;
import com.calctastic.sample.hypercal.view.HyperCalDisplayView;

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
                R.id.btn_clear, R.id.btn_delete, R.id.btn_cursor_left, R.id.btn_cursor_right
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

        updateFormulaText();
        displayView.invalidate();
    }

    private void appendDigit(char c) {
        if (cursorPointer != null && cursorPointer.node instanceof com.calctastic.sample.hypercal.engine.model.EmptyNode) {
            com.calctastic.sample.hypercal.engine.model.EmptyNode empty = (com.calctastic.sample.hypercal.engine.model.EmptyNode) cursorPointer.node;
            NumberNode num = new NumberNode(String.valueOf(c));
            num.cursorPosition = 1;
            if (empty.getParent() instanceof FractionNode) {
                FractionNode frac = (FractionNode) empty.getParent();
                if (empty == frac.numerator) {
                    frac.numerator = num;
                    num.setParent(frac);
                } else if (empty == frac.denominator) {
                    frac.denominator = num;
                    num.setParent(frac);
                }
            } else if (empty.getParent() instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) empty.getParent();
                int idx = seq.getChildIndex(empty);
                seq.removeChild(empty);
                seq.add(idx, num);
            }
            cursorPointer = new CursorPointer(num, 1);
            displayView.setCursorPointer(cursorPointer);
        } else if (cursorPointer != null && cursorPointer.node instanceof FractionNode) {
            // Typing while at Center baseline slot after/before fraction
            FractionNode frac = (FractionNode) cursorPointer.node;
            ExpressionNode parent = frac.getParent();
            if (parent instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) parent;
                int idx = seq.getChildIndex(frac);
                NumberNode num = new NumberNode(String.valueOf(c));
                num.cursorPosition = 1;
                if (cursorPointer.position == 0) {
                    seq.add(idx, num);
                } else {
                    seq.add(idx + 1, num);
                }
                cursorPointer = new CursorPointer(num, 1);
                displayView.setCursorPointer(cursorPointer);
            }
        } else if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            num.insertChar(c);
            cursorPointer = new CursorPointer(num, num.cursorPosition);
            displayView.setCursorPointer(cursorPointer);
        } else if (cursorPointer != null && cursorPointer.node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) cursorPointer.node;
            NumberNode num = new NumberNode(String.valueOf(c));
            num.cursorPosition = 1;
            seq.addChild(num);
            cursorPointer = new CursorPointer(num, 1);
            displayView.setCursorPointer(cursorPointer);
        } else {
            NumberNode num = new NumberNode(String.valueOf(c));
            num.cursorPosition = 1;
            rootSequence.addChild(num);
            cursorPointer = new CursorPointer(num, 1);
            displayView.setCursorPointer(cursorPointer);
        }
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
        if (cursorPointer != null && cursorPointer.node != null && cursorPointer.node.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) cursorPointer.node.getParent();
            int idx = parent.getChildIndex(cursorPointer.node);
            parent.add(idx + 1, op);
        } else {
            rootSequence.addChild(op);
        }
        cursorPointer = new CursorPointer(op, 1);
        displayView.setCursorPointer(cursorPointer);
    }

    private void insertSqrt() {
        NumberNode inner = new NumberNode("");
        SqrtNode sqrt = new SqrtNode(inner);
        insertNodeAtCursor(sqrt);
        cursorPointer = new CursorPointer(inner, 0);
        displayView.setCursorPointer(cursorPointer);
    }

    /**
     * Faithful to HiPER Calc C0196hc.java & EA.m72HiPER:
     * - If active token before cursor exists, it becomes numerator, denominator is empty, and cursor goes to denominator.
     * - If cursor inside fraction numerator, pressing a/b jumps to denominator.
     * - If empty/fresh, numerator is empty, denominator is empty, cursor goes to numerator.
     */
    private void insertFraction() {
        if (cursorPointer != null && cursorPointer.node != null) {
            // If already in numerator, pressing a/b jumps to denominator
            if (cursorPointer.node.getParent() instanceof FractionNode) {
                FractionNode parentFrac = (FractionNode) cursorPointer.node.getParent();
                if (cursorPointer.node == parentFrac.numerator) {
                    if (parentFrac.denominator != null) {
                        cursorPointer = new CursorPointer(parentFrac.denominator, 0);
                        displayView.setCursorPointer(cursorPointer);
                        return;
                    }
                }
            }

            // Wrap prior node into numerator (C0196hc.java lines 347-363)
            ExpressionNode targetNumerator = cursorPointer.node;
            ExpressionNode parent = targetNumerator.getParent();
            if (parent instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) parent;
                int idx = seq.getChildIndex(targetNumerator);
                seq.removeChild(targetNumerator);

                com.calctastic.sample.hypercal.engine.model.EmptyNode emptyDen = new com.calctastic.sample.hypercal.engine.model.EmptyNode();
                FractionNode frac = new FractionNode(targetNumerator, emptyDen);
                seq.add(idx, frac);

                cursorPointer = new CursorPointer(emptyDen, 0);
                displayView.setCursorPointer(cursorPointer);
                return;
            }
        }

        // Fresh fraction if no prior node: 2 empty boxes (QA in HiPER)
        com.calctastic.sample.hypercal.engine.model.EmptyNode emptyNum = new com.calctastic.sample.hypercal.engine.model.EmptyNode();
        com.calctastic.sample.hypercal.engine.model.EmptyNode emptyDen = new com.calctastic.sample.hypercal.engine.model.EmptyNode();
        FractionNode frac = new FractionNode(emptyNum, emptyDen);
        insertNodeAtCursor(frac);
        cursorPointer = new CursorPointer(emptyNum, 0);
        displayView.setCursorPointer(cursorPointer);
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
        insertNodeAtCursor(frac);
        cursorPointer = new CursorPointer(den, 0);
        displayView.setCursorPointer(cursorPointer);
    }

    private void insertParenthesis() {
        NumberNode inner = new NumberNode("");
        ParenthesisNode paren = new ParenthesisNode(inner);
        insertNodeAtCursor(paren);
        cursorPointer = new CursorPointer(inner, 0);
        displayView.setCursorPointer(cursorPointer);
    }

    private void insertNodeAtCursor(ExpressionNode node) {
        if (cursorPointer != null && cursorPointer.node != null && cursorPointer.node.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) cursorPointer.node.getParent();
            int idx = parent.getChildIndex(cursorPointer.node);
            parent.add(idx + 1, node);
        } else {
            rootSequence.addChild(node);
        }
    }

    private void clearAll() {
        rootSequence = new SequenceNode();
        displayView.setExpressionRoot(rootSequence);
        cursorPointer = new CursorPointer(rootSequence, 0);
        displayView.setCursorPointer(cursorPointer);
    }

    private void deleteChar() {
        // If on EmptyNode in Fraction
        if (cursorPointer != null && cursorPointer.node instanceof com.calctastic.sample.hypercal.engine.model.EmptyNode) {
            com.calctastic.sample.hypercal.engine.model.EmptyNode empty = (com.calctastic.sample.hypercal.engine.model.EmptyNode) cursorPointer.node;
            if (empty.getParent() instanceof FractionNode) {
                FractionNode frac = (FractionNode) empty.getParent();
                if (empty == frac.denominator) {
                    // DEL in empty denominator jumps to numerator
                    cursorPointer = new CursorPointer(frac.numerator, frac.numerator.getLength());
                    displayView.setCursorPointer(cursorPointer);
                    return;
                } else if (empty == frac.numerator) {
                    // If numerator is empty and denominator is empty, remove fraction completely
                    if (frac.denominator instanceof com.calctastic.sample.hypercal.engine.model.EmptyNode) {
                        ExpressionNode fracParent = frac.getParent();
                        if (fracParent instanceof SequenceNode) {
                            SequenceNode seq = (SequenceNode) fracParent;
                            int idx = seq.getChildIndex(frac);
                            seq.removeChild(frac);
                            if (seq.getChildCount() > 0) {
                                int nextIdx = Math.max(0, idx - 1);
                                ExpressionNode nextNode = seq.getChild(nextIdx);
                                cursorPointer = new CursorPointer(nextNode, nextNode.getLength());
                            } else {
                                cursorPointer = new CursorPointer(seq, 0);
                            }
                            displayView.setCursorPointer(cursorPointer);
                            return;
                        }
                    }
                }
            }
        }

        // If at Center baseline after fraction
        if (cursorPointer != null && cursorPointer.node instanceof FractionNode) {
            FractionNode frac = (FractionNode) cursorPointer.node;
            if (cursorPointer.position == 1) {
                // DEL after fraction enters denominator
                cursorPointer = new CursorPointer(frac.denominator, frac.denominator.getLength());
                displayView.setCursorPointer(cursorPointer);
                return;
            } else {
                // DEL before fraction removes previous item in sequence
                if (frac.getParent() instanceof SequenceNode) {
                    SequenceNode seq = (SequenceNode) frac.getParent();
                    int idx = seq.getChildIndex(frac);
                    if (idx > 0) {
                        seq.removeChild(seq.getChild(idx - 1));
                        cursorPointer = new CursorPointer(frac, 0);
                        displayView.setCursorPointer(cursorPointer);
                        return;
                    }
                }
            }
        }

        if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            if (num.deleteChar()) {
                cursorPointer = new CursorPointer(num, num.cursorPosition);
                displayView.setCursorPointer(cursorPointer);
                return;
            }

            // If number in denominator became empty, revert to EmptyNode
            if (num.getParent() instanceof FractionNode) {
                FractionNode frac = (FractionNode) num.getParent();
                com.calctastic.sample.hypercal.engine.model.EmptyNode empty = new com.calctastic.sample.hypercal.engine.model.EmptyNode();
                if (num == frac.denominator) {
                    frac.denominator = empty;
                    empty.setParent(frac);
                    cursorPointer = new CursorPointer(empty, 0);
                    displayView.setCursorPointer(cursorPointer);
                    return;
                } else if (num == frac.numerator) {
                    frac.numerator = empty;
                    empty.setParent(frac);
                    cursorPointer = new CursorPointer(empty, 0);
                    displayView.setCursorPointer(cursorPointer);
                    return;
                }
            }
        }

        if (cursorPointer != null && cursorPointer.node != null && cursorPointer.node != rootSequence) {
            ExpressionNode toRemove = cursorPointer.node;
            ExpressionNode parent = toRemove.getParent();
            if (parent instanceof SequenceNode) {
                SequenceNode seq = (SequenceNode) parent;
                int idx = seq.getChildIndex(toRemove);
                seq.removeChild(toRemove);
                if (seq.getChildCount() > 0) {
                    int nextIdx = Math.max(0, idx - 1);
                    ExpressionNode nextNode = seq.getChild(nextIdx);
                    cursorPointer = new CursorPointer(nextNode, nextNode.getLength());
                } else {
                    cursorPointer = new CursorPointer(seq, 0);
                }
                displayView.setCursorPointer(cursorPointer);
                return;
            }
        }

        if (rootSequence.getChildCount() > 0) {
            rootSequence.removeChild(rootSequence.getChild(rootSequence.getChildCount() - 1));
            if (rootSequence.getChildCount() > 0) {
                ExpressionNode last = rootSequence.getChild(rootSequence.getChildCount() - 1);
                cursorPointer = new CursorPointer(last, last.getLength());
            } else {
                cursorPointer = new CursorPointer(rootSequence, 0);
            }
            displayView.setCursorPointer(cursorPointer);
        }
    }

    private void moveCursorLeft() {
        if (rootSequence.getChildCount() == 0 || cursorPointer == null) return;

        // Inside NumberNode: move left character-by-character
        if (cursorPointer.node instanceof NumberNode && cursorPointer.position > 0) {
            cursorPointer = new CursorPointer(cursorPointer.node, cursorPointer.position - 1);
            displayView.setCursorPointer(cursorPointer);
            return;
        }

        // Inside FractionNode at position 1 (Center after fraction): step left into denominator
        if (cursorPointer.node instanceof FractionNode && cursorPointer.position == 1) {
            FractionNode frac = (FractionNode) cursorPointer.node;
            ExpressionNode target = frac.denominator != null ? frac.denominator : frac.numerator;
            cursorPointer = new CursorPointer(target, target.getLength());
            displayView.setCursorPointer(cursorPointer);
            return;
        }

        // Inside Fraction denominator: left arrow jumps to end of numerator
        if (cursorPointer.node != null && cursorPointer.node.getParent() instanceof FractionNode) {
            FractionNode frac = (FractionNode) cursorPointer.node.getParent();
            if (cursorPointer.node == frac.denominator) {
                if (frac.numerator != null) {
                    cursorPointer = new CursorPointer(frac.numerator, frac.numerator.getLength());
                    displayView.setCursorPointer(cursorPointer);
                    return;
                }
            } else if (cursorPointer.node == frac.numerator) {
                // Left arrow from start of numerator steps out to Center before fraction
                cursorPointer = new CursorPointer(frac, 0);
                displayView.setCursorPointer(cursorPointer);
                return;
            }
        }

        // Inside FractionNode at position 0 (Center before fraction): step left to previous token in sequence
        if (cursorPointer.node instanceof FractionNode && cursorPointer.position == 0) {
            FractionNode frac = (FractionNode) cursorPointer.node;
            if (frac.getParent() instanceof SequenceNode) {
                SequenceNode parent = (SequenceNode) frac.getParent();
                int idx = parent.getChildIndex(frac);
                if (idx > 0) {
                    ExpressionNode prev = parent.getChild(idx - 1);
                    if (prev instanceof FractionNode) {
                        cursorPointer = new CursorPointer(prev, 1);
                    } else {
                        cursorPointer = new CursorPointer(prev, prev.getLength());
                    }
                    displayView.setCursorPointer(cursorPointer);
                    return;
                }
            }
        }

        // Top-level sequence traversal
        if (cursorPointer.node != null && cursorPointer.node.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) cursorPointer.node.getParent();
            int idx = parent.getChildIndex(cursorPointer.node);
            if (idx > 0) {
                ExpressionNode prev = parent.getChild(idx - 1);
                if (prev instanceof FractionNode) {
                    cursorPointer = new CursorPointer(prev, 1); // Center after fraction
                } else {
                    cursorPointer = new CursorPointer(prev, prev.getLength());
                }
                displayView.setCursorPointer(cursorPointer);
            }
        }
    }

    private void moveCursorRight() {
        if (rootSequence.getChildCount() == 0 || cursorPointer == null) return;

        // Inside NumberNode: move right character-by-character
        if (cursorPointer.node instanceof NumberNode && cursorPointer.position < cursorPointer.node.getLength()) {
            cursorPointer = new CursorPointer(cursorPointer.node, cursorPointer.position + 1);
            displayView.setCursorPointer(cursorPointer);
            return;
        }

        // Inside FractionNode at position 0 (Center before fraction): step right into numerator
        if (cursorPointer.node instanceof FractionNode && cursorPointer.position == 0) {
            FractionNode frac = (FractionNode) cursorPointer.node;
            ExpressionNode target = frac.numerator != null ? frac.numerator : frac.denominator;
            cursorPointer = new CursorPointer(target, 0);
            displayView.setCursorPointer(cursorPointer);
            return;
        }

        // Inside Fraction numerator: right arrow jumps to start of denominator
        if (cursorPointer.node != null && cursorPointer.node.getParent() instanceof FractionNode) {
            FractionNode frac = (FractionNode) cursorPointer.node.getParent();
            if (cursorPointer.node == frac.numerator) {
                if (frac.denominator != null) {
                    cursorPointer = new CursorPointer(frac.denominator, 0);
                    displayView.setCursorPointer(cursorPointer);
                    return;
                }
            } else if (cursorPointer.node == frac.denominator) {
                // Right arrow from denominator steps out to Center after fraction
                cursorPointer = new CursorPointer(frac, 1);
                displayView.setCursorPointer(cursorPointer);
                return;
            }
        }

        // Inside FractionNode at position 1 (Center after fraction): step right to next node in Sequence
        if (cursorPointer.node instanceof FractionNode && cursorPointer.position == 1) {
            FractionNode frac = (FractionNode) cursorPointer.node;
            if (frac.getParent() instanceof SequenceNode) {
                SequenceNode parent = (SequenceNode) frac.getParent();
                int idx = parent.getChildIndex(frac);
                if (idx >= 0 && idx < parent.getChildCount() - 1) {
                    ExpressionNode next = parent.getChild(idx + 1);
                    if (next instanceof FractionNode) {
                        cursorPointer = new CursorPointer(next, 0);
                    } else {
                        cursorPointer = new CursorPointer(next, 0);
                    }
                    displayView.setCursorPointer(cursorPointer);
                    return;
                }
            }
        }

        // Top-level sequence traversal
        if (cursorPointer.node != null && cursorPointer.node.getParent() instanceof SequenceNode) {
            SequenceNode parent = (SequenceNode) cursorPointer.node.getParent();
            int idx = parent.getChildIndex(cursorPointer.node);
            if (idx >= 0 && idx < parent.getChildCount() - 1) {
                ExpressionNode next = parent.getChild(idx + 1);
                if (next instanceof FractionNode) {
                    cursorPointer = new CursorPointer(next, 0); // Center before fraction
                } else {
                    cursorPointer = new CursorPointer(next, 0);
                }
                displayView.setCursorPointer(cursorPointer);
            }
        }
    }
}
