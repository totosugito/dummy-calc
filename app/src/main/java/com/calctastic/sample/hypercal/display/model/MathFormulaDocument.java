package com.calctastic.sample.hypercal.display.model;

import com.calctastic.sample.hypercal.display.renderer.RenderContext;
import com.calctastic.sample.hypercal.display.renderer.TokenRendererRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * MathFormulaDocument:
 * Representasi dokumen formula dan manipulasi kursor/token (setara ZB.java, C0351xb.java, dan C0091Sd.java di HiPER).
 * Bertanggung jawab penuh terhadap:
 * 1. Penyimpanan daftar pohon token (AST).
 * 2. Posisi kursor utama dan sub-kursor pecahan (kotak atas/bawah).
 * 3. Operasi pengetikan angka, operator, pecahan, akar, pangkat, dan penghapusan token.
 * 4. Navigasi kursor kiri/kanan dan tap selection.
 */
public class MathFormulaDocument {

    public interface OnDocumentChangeListener {
        void onDocumentChanged();
    }

    private final List<MathToken> tokens = new ArrayList<>();
    private int cursorIndex = 0;
    private int fractionFocusIndex = -1;
    private boolean fractionInDenominator = false;
    private int fractionSubCursor = 0;

    private OnDocumentChangeListener changeListener;

    public void setOnDocumentChangeListener(OnDocumentChangeListener listener) {
        this.changeListener = listener;
    }

    private void notifyChange() {
        if (changeListener != null) {
            changeListener.onDocumentChanged();
        }
    }

    public List<MathToken> getTokens() {
        return tokens;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public int getFractionFocusIndex() {
        return fractionFocusIndex;
    }

    public boolean isFractionInDenominator() {
        return fractionInDenominator;
    }

    public int getFractionSubCursor() {
        return fractionSubCursor;
    }

    public void appendText(String text) {
        if (text == null || text.isEmpty()) return;

        if (fractionFocusIndex >= 0 && fractionFocusIndex < tokens.size()) {
            MathToken frac = tokens.get(fractionFocusIndex);
            List<MathToken> target = fractionInDenominator ? frac.secondaryChildren : frac.children;

            if (fractionSubCursor > 0 && fractionSubCursor <= target.size()) {
                MathToken prev = target.get(fractionSubCursor - 1);
                if (prev.type == MathToken.Type.NUMBER) {
                    prev.text = prev.text + text;
                } else {
                    target.add(fractionSubCursor, MathToken.number(text));
                    fractionSubCursor++;
                }
            } else {
                target.add(fractionSubCursor, MathToken.number(text));
                fractionSubCursor++;
            }
            notifyChange();
            return;
        }

        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            MathToken prev = tokens.get(cursorIndex - 1);
            if (prev.type == MathToken.Type.NUMBER) {
                prev.text = prev.text + text;
                notifyChange();
                return;
            }
        }

        tokens.add(cursorIndex, MathToken.number(text));
        cursorIndex++;
        notifyChange();
    }

    public void appendOperator(String op) {
        if (op == null || op.isEmpty()) return;

        if (fractionFocusIndex >= 0 && fractionFocusIndex < tokens.size()) {
            MathToken frac = tokens.get(fractionFocusIndex);
            List<MathToken> target = fractionInDenominator ? frac.secondaryChildren : frac.children;
            target.add(fractionSubCursor, MathToken.operator(op));
            fractionSubCursor++;
            notifyChange();
            return;
        }

        tokens.add(cursorIndex, MathToken.operator(op));
        cursorIndex++;
        notifyChange();
    }

    public void appendToken(MathToken token) {
        if (token == null) return;
        tokens.add(cursorIndex, token);
        cursorIndex++;
        notifyChange();
    }

    public void appendFraction() {
        MathToken prev = null;
        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            prev = tokens.remove(cursorIndex - 1);
            cursorIndex--;
        }

        List<MathToken> numList = new ArrayList<>();
        if (prev != null) {
            numList.add(prev);
        }

        MathToken fracToken = MathToken.fraction(numList, new ArrayList<>());
        tokens.add(cursorIndex, fracToken);

        fractionFocusIndex = cursorIndex;
        if (prev != null) {
            fractionInDenominator = true;
            fractionSubCursor = 0;
        } else {
            fractionInDenominator = false;
            fractionSubCursor = 0;
        }

        notifyChange();
    }

    public void appendSqrt() {
        appendToken(MathToken.sqrt(new ArrayList<>()));
    }

    public void appendPower() {
        appendToken(MathToken.power(new ArrayList<>(), new ArrayList<>()));
    }

    public void deleteBackward() {
        if (fractionFocusIndex >= 0 && fractionFocusIndex < tokens.size()) {
            MathToken frac = tokens.get(fractionFocusIndex);
            List<MathToken> target = fractionInDenominator ? frac.secondaryChildren : frac.children;
            if (fractionSubCursor > 0 && fractionSubCursor <= target.size()) {
                MathToken t = target.get(fractionSubCursor - 1);
                if (t.type == MathToken.Type.NUMBER && t.text.length() > 1) {
                    t.text = t.text.substring(0, t.text.length() - 1);
                } else {
                    target.remove(fractionSubCursor - 1);
                    fractionSubCursor--;
                }
            } else if (fractionInDenominator) {
                fractionInDenominator = false;
                fractionSubCursor = frac.children.size();
            } else {
                tokens.remove(fractionFocusIndex);
                cursorIndex = fractionFocusIndex;
                fractionFocusIndex = -1;
            }
            notifyChange();
            return;
        }

        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            MathToken token = tokens.get(cursorIndex - 1);
            if (token.type == MathToken.Type.NUMBER && token.text.length() > 1) {
                token.text = token.text.substring(0, token.text.length() - 1);
            } else {
                tokens.remove(cursorIndex - 1);
                cursorIndex--;
            }
            notifyChange();
        }
    }

    public void clearAll() {
        tokens.clear();
        cursorIndex = 0;
        fractionFocusIndex = -1;
        fractionInDenominator = false;
        fractionSubCursor = 0;
        notifyChange();
    }

    public void moveCursorLeft() {
        if (fractionFocusIndex >= 0 && fractionFocusIndex < tokens.size()) {
            if (fractionSubCursor > 0) {
                fractionSubCursor--;
            } else if (fractionInDenominator) {
                fractionInDenominator = false;
                MathToken frac = tokens.get(fractionFocusIndex);
                fractionSubCursor = frac.children.size();
            } else {
                cursorIndex = fractionFocusIndex;
                fractionFocusIndex = -1;
            }
            notifyChange();
            return;
        }

        if (cursorIndex > 0) {
            cursorIndex--;
            MathToken prev = tokens.get(cursorIndex);
            if (prev.type == MathToken.Type.FRACTION) {
                fractionFocusIndex = cursorIndex;
                fractionInDenominator = true;
                fractionSubCursor = prev.secondaryChildren.size();
            }
            notifyChange();
        }
    }

    public void moveCursorRight() {
        if (fractionFocusIndex >= 0 && fractionFocusIndex < tokens.size()) {
            MathToken frac = tokens.get(fractionFocusIndex);
            List<MathToken> target = fractionInDenominator ? frac.secondaryChildren : frac.children;
            if (fractionSubCursor < target.size()) {
                fractionSubCursor++;
            } else if (!fractionInDenominator) {
                fractionInDenominator = true;
                fractionSubCursor = 0;
            } else {
                cursorIndex = fractionFocusIndex + 1;
                fractionFocusIndex = -1;
            }
            notifyChange();
            return;
        }

        if (cursorIndex < tokens.size()) {
            MathToken next = tokens.get(cursorIndex);
            if (next.type == MathToken.Type.FRACTION) {
                fractionFocusIndex = cursorIndex;
                fractionInDenominator = false;
                fractionSubCursor = 0;
            } else {
                cursorIndex++;
            }
            notifyChange();
        }
    }

    public void handleTap(float touchX, float touchY, float startX, TokenRendererRegistry registry,
                          float baseTextSize, RenderContext ctx, List<RenderContext.FractionHitBox> hitBoxes) {
        for (RenderContext.FractionHitBox box : hitBoxes) {
            if (box.numBox.contains(touchX, touchY)) {
                fractionFocusIndex = box.tokenIndex;
                fractionInDenominator = false;
                MathToken frac = tokens.get(box.tokenIndex);
                fractionSubCursor = frac.children.size();
                notifyChange();
                return;
            } else if (box.denBox.contains(touchX, touchY)) {
                fractionFocusIndex = box.tokenIndex;
                fractionInDenominator = true;
                MathToken frac = tokens.get(box.tokenIndex);
                fractionSubCursor = frac.secondaryChildren.size();
                notifyChange();
                return;
            }
        }

        fractionFocusIndex = -1;
        int nearestIndex = tokens.size();
        float minDiff = Float.MAX_VALUE;

        float curX = startX;
        for (int i = 0; i < tokens.size(); i++) {
            float diff = Math.abs(curX - touchX);
            if (diff < minDiff) {
                minDiff = diff;
                nearestIndex = i;
            }
            curX += registry.measureWidth(tokens.get(i), baseTextSize, ctx);
        }
        if (Math.abs(curX - touchX) < minDiff) {
            nearestIndex = tokens.size();
        }

        cursorIndex = nearestIndex;
        notifyChange();
    }
}
