package com.calctastic.sample.hypercal.render;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.NumberNode;

/**
 * Visual renderer for numbers (digits, decimal, scientific exponent).
 * Faithful implementation of android.core.C0294rh & C0351xb from HiPER Calc.
 */
public class NumberVisual extends MathVisual {
    public final NumberNode numberNode;

    public NumberVisual(NumberNode node) {
        super(node);
        this.numberNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        String text = numberNode.getText();
        if (text.isEmpty()) {
            // Empty box, same formula as PlaceholderVisual (C0357yG.mo63HiPER()) so an empty
            // "x^y" base/exponent (or sqrt/parenthesis content) reads as a placeholder box just
            // like an empty a/b slot, instead of silently taking up half a digit's width.
            //
            // Deviation from C0357yG: its "0.9f * density" term is a FIXED device-pixel value
            // that does NOT shrink with scale -- confirmed faithful for a top-level a/b slot,
            // but C0294rh (the real NumberVisual) never draws a box for an empty number at all,
            // and C0311tf (exponent layout) has no empty-placeholder handling either -- so this
            // exact combination (empty box inside a scaled-down exponent, e.g. 0.75x for xʸ) has
            // no original behavior to match. Left un-scaled, that fixed term dominates a small
            // exponent's box and makes it look oversized. We scale it by D so the box shrinks
            // proportionally with its own element scale instead.
            float density = Resources.getSystem().getDisplayMetrics().density;
            b.x = paint.measureText("0") * 1.2f;
            m = -paint.ascent() + 0.9f * density * D;
            b.y = paint.descent() + m;
            return;
        }

        b.x = paint.measureText(text);
        b.y = -paint.ascent() + paint.descent();
        m = -paint.ascent();
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        String text = numberNode.getText();
        if (text.isEmpty()) {
            float measure0 = paint.measureText("0");
            float insetX = 0.15f * measure0;
            Paint boxPaint = new Paint(paint);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(measure0 * 0.1f);
            canvas.drawRect(insetX, 0.0f, b.x - insetX, b.y, boxPaint);
            return;
        }
        canvas.drawText(text, 0, m, paint);
    }

    /**
     * Exact character measurement matching HiPER Calc C0294rh.mo359HiPER(int i):
     * x = paint.measureText(strSubstring)
     * y = baseline (this.m)
     */
    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        String text = numberNode.getText();
        if (text.isEmpty()) {
            // Same "just outside the box" convention as PlaceholderVisual/MathVisual's default
            // (index<=0 -> left of the box, else -> right of it) -- NOT (0, m), which sits at the
            // box's left inner edge/border instead of clearly beside it. This was the bug: every
            // empty box we draw via NumberVisual (x^y base/exponent, sqrt content, parenthesis
            // content) had the cursor rendered overlapping the box instead of beside it, unlike
            // the fraction's EmptyNode/PlaceholderVisual boxes which already used this convention.
            return super.getCursorPosition(index, basePaint);
        }

        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);
        if (index > text.length()) {
            index = text.length();
        }
        float x = paint.measureText(text.substring(0, Math.max(index, 0)));
        return new PointF(x, m);
    }

    /**
     * Exact hit testing logic from HiPER Calc C0294rh.java lines 155-207.
     * Iterates characters measuring width to find closest cursor split.
     */
    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        String text = numberNode.getText();
        if (text.isEmpty()) {
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(numberNode, 0);
        }

        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float prevWidth = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            float nextWidth = paint.measureText(text.substring(0, i + 1));
            float midPoint = (prevWidth + nextWidth) / 2.0f;
            if (point.x < midPoint) {
                return new com.calctastic.sample.hypercal.engine.model.CursorPointer(numberNode, i);
            }
            prevWidth = nextWidth;
        }

        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(numberNode, text.length());
    }
}
