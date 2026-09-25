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
            // Placeholder width
            b.x = paint.measureText("0") * 0.5f;
            b.y = -paint.ascent() + paint.descent();
            m = -paint.ascent();
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
        if (!text.isEmpty()) {
            canvas.drawText(text, 0, m, paint);
        }
    }

    /**
     * Exact character measurement matching HiPER Calc C0294rh.mo359HiPER(int i):
     * x = paint.measureText(strSubstring)
     * y = baseline (this.m)
     */
    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        String text = numberNode.getText();
        if (index <= 0) {
            return new PointF(0, m);
        }
        if (index > text.length()) {
            index = text.length();
        }
        float x = paint.measureText(text.substring(0, index));
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
