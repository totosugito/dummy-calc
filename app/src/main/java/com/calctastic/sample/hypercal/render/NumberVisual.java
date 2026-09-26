package com.calctastic.sample.hypercal.render;

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
            // Empty box, so an empty "x^y" base/exponent (or sqrt/parenthesis content) reads as a
            // placeholder just like an empty a/b slot, instead of silently taking up no space.
            //
            // Sized to match a plain digit's own visual footprint at this visual's scale (D),
            // rather than the fraction placeholder's C0357yG-derived formula (measureText("0") *
            // 1.2f width, plus a "0.9f * density" padding term) -- that formula is faithful for a
            // top-level a/b slot, but produces a box visibly LARGER than an actual digit once
            // scaled down for a superscript (e.g. an empty "^y" noticeably bigger than an actual
            // "^2" at the same D). C0294rh (the real NumberVisual) never draws a box for an empty
            // number at all, so there's no original formula to match here regardless -- this is a
            // deliberate choice to size it like "the digit that could go here" instead.
            //
            // Height is ascent-to-baseline only (no descent tail): a digit like "2" has no
            // descender, so its actual ink never reaches the descent line even though a real
            // NumberVisual's b.y (below) reports the full ascent+descent line-height for baseline
            // bookkeeping. Drawing the placeholder rect that same full height made it visibly
            // taller than the digit it's standing in for -- the box's bottom edge should land on
            // the baseline, matching where "2"'s own bottom actually sits.
            b.x = paint.measureText("0");
            m = -paint.ascent();
            b.y = m;
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
