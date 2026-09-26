package com.calctastic.sample.hypercal.engine;

import com.calctastic.sample.hypercal.engine.inserter.FractionInserter;
import com.calctastic.sample.hypercal.engine.inserter.PowerInserter;
import com.calctastic.sample.hypercal.engine.inserter.SqrtInserter;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.OperatorNode;
import com.calctastic.sample.hypercal.engine.model.ParenthesisNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;

/**
 * Keypad-driven editing of the expression AST: insertion, deletion, and left/right cursor
 * navigation. Holds the expression root and the cursor itself, but has no Android dependency
 * (no View, no Paint) so it can be exercised without an Activity/View.
 *
 * Moved out of HyperCalActivity (2026-09-26) to separate "editing the model" from "wiring up
 * the UI" -- see specs/btn_fraction.md and specs/display_scaling_typography.md for pointers to
 * where fraction/cursor logic now lives.
 *
 * This file has been split twice since (2026-09-26, see specs/btn_fraction.md Section H) as it
 * kept growing with each new button:
 * - Fraction-family ("a/b", "a b/c", "1/x") inserts -> {@link FractionInserter}.
 * - Power-family ("xʸ", "x²", "x³", "x⁻¹") inserts -> {@link PowerInserter}.
 * - Shared slot/boundary helpers plus ◀/▶ navigation -> {@link CursorNav}.
 * - DEL/backspace logic -> {@link CursorDelete}.
 * This class is now just the state holder (root sequence + cursor) plus the handful of
 * one-line inserts that don't need their own file (digit, negate, operator, sqrt, parenthesis),
 * delegating everything else to the classes above.
 *
 * Up/down cursor navigation (▲/▼) is NOT here: it depends on the laid-out visual tree
 * (HyperCalDisplayView.findVerticalCursorTarget), which needs a Paint/font-metrics-backed
 * layout this class deliberately doesn't have. HyperCalActivity computes that target and calls
 * {@link #setCursorPointer(CursorPointer)} with it.
 */
public class ExpressionEditor {
    private SequenceNode rootSequence;
    private CursorPointer cursorPointer;

    public ExpressionEditor(SequenceNode rootSequence, CursorPointer initialCursor) {
        this.rootSequence = rootSequence;
        this.cursorPointer = initialCursor;
    }

    public SequenceNode getRootSequence() {
        return rootSequence;
    }

    public CursorPointer getCursorPointer() {
        return cursorPointer;
    }

    public void setCursorPointer(CursorPointer pointer) {
        setCursor(pointer);
    }

    /**
     * Every cursor change goes through here so {@code pointer.node}'s own
     * {@code cursorPosition} field (used internally by {@link NumberNode#insertChar} and
     * {@link NumberNode#deleteChar}) is always kept in sync with the {@link CursorPointer}
     * wrapper. This used to happen only as a side effect of
     * {@code HyperCalDisplayView#setCursorPointer} syncing it on every redraw -- which worked in
     * the app (a redraw happens between any two button presses) but meant this class silently
     * depended on the View layer for its own internal consistency, despite the class-level doc
     * comment's claim of having no Android dependency. Surfaced 2026-09-26 by
     * {@code CursorRegressionTest}, a plain-JUnit test suite exercising this class headlessly
     * (see specs/testing_via_latex.md) -- {@code moveCursorLeft()} into a NumberNode followed
     * immediately by {@code deleteChar()}, with no redraw in between, deleted the wrong character
     * because the node's stale {@code cursorPosition} hadn't been updated yet.
     */
    private void setCursor(CursorPointer pointer) {
        this.cursorPointer = pointer;
        if (pointer != null && pointer.node != null) {
            pointer.node.setCursorPosition(pointer.position);
        }
    }

    public void reset() {
        rootSequence = new SequenceNode();
        cursorPointer = new CursorPointer(rootSequence, 0);
    }

    public void appendDigit(char c) {
        if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            num.insertChar(c);
            setCursor(new CursorPointer(num, num.cursorPosition));
            return;
        }
        NumberNode num = new NumberNode(String.valueOf(c));
        num.cursorPosition = 1;
        CursorNav.insertAtCursor(rootSequence, cursorPointer, num);
        setCursor(new CursorPointer(num, 1));
    }

    public void toggleNegate() {
        if (cursorPointer != null && cursorPointer.node instanceof NumberNode) {
            NumberNode num = (NumberNode) cursorPointer.node;
            num.toggleNegate();
            setCursor(new CursorPointer(num, num.cursorPosition));
        }
    }

    public void appendOperator(String symbol) {
        OperatorNode op = new OperatorNode(symbol);
        CursorNav.insertAtCursor(rootSequence, cursorPointer, op);
        setCursor(new CursorPointer(op, 1));
    }

    public void insertSqrt() {
        setCursor(SqrtInserter.insertSqrt(rootSequence, cursorPointer));
    }

    public void insertNthRoot() {
        setCursor(SqrtInserter.insertNthRoot(rootSequence, cursorPointer));
    }

    public void insertFraction() {
        setCursor(FractionInserter.insertFraction(rootSequence, cursorPointer));
    }

    public void insertMixedFraction() {
        setCursor(FractionInserter.insertMixedFraction(rootSequence, cursorPointer));
    }

    public void insertReciprocal() {
        setCursor(FractionInserter.insertReciprocal(rootSequence, cursorPointer));
    }

    public void insertPower() {
        setCursor(PowerInserter.insertPower(rootSequence, cursorPointer));
    }

    public void insertSquare() {
        setCursor(PowerInserter.insertSquare(rootSequence, cursorPointer));
    }

    public void insertCube() {
        setCursor(PowerInserter.insertCube(rootSequence, cursorPointer));
    }

    public void insertNegativeOnePower() {
        setCursor(PowerInserter.insertNegativeOnePower(rootSequence, cursorPointer));
    }

    public void insertParenthesis() {
        NumberNode inner = new NumberNode("");
        ParenthesisNode paren = new ParenthesisNode(inner);
        CursorNav.insertAtCursor(rootSequence, cursorPointer, paren);
        setCursor(new CursorPointer(inner, 0));
    }

    public void deleteChar() {
        setCursor(CursorDelete.deleteChar(cursorPointer));
    }

    public void moveCursorLeft() {
        setCursor(CursorNav.moveLeft(cursorPointer));
    }

    public void moveCursorRight() {
        setCursor(CursorNav.moveRight(cursorPointer));
    }
}
