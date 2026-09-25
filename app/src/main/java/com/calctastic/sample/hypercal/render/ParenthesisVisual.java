package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.engine.model.ParenthesisNode;

/**
 * Visual renderer for parenthesized expression: ( content ).
 * Faithful to HiPER Calc IH / C0022Bd.
 */
public class ParenthesisVisual extends MathVisual {
    public final ParenthesisNode parenNode;
    public MathVisual insideVisual;

    public ParenthesisVisual(ParenthesisNode node) {
        super(node);
        this.parenNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        if (insideVisual != null) {
            insideVisual.setScale(D);
            insideVisual.calculateLayout(basePaint);
        }

        float insideW = insideVisual != null ? insideVisual.b.x : paint.measureText("0");
        float insideH = insideVisual != null ? insideVisual.b.y : (-paint.ascent() + paint.descent());
        float insideM = insideVisual != null ? insideVisual.m : -paint.ascent();

        float parenW = paint.measureText("(") * 0.9f;

        if (insideVisual != null) {
            insideVisual.setPosition(parenW, 0);
        }

        b.x = insideW + (2.0f * parenW);
        b.y = insideH;
        m = insideM;
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float parenW = paint.measureText("(") * 0.9f;
        float thickness = Math.max(1.5f, paint.getTextSize() * 0.055f);

        Paint arcPaint = new Paint(paint);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(thickness);

        // Draw left parenthesis arc
        RectF leftOval = new RectF(thickness / 2, 0, parenW * 1.5f, b.y);
        canvas.drawArc(leftOval, 120, 120, false, arcPaint);

        // Draw content
        if (insideVisual != null) {
            canvas.save();
            canvas.translate(insideVisual.HiPER.x, insideVisual.HiPER.y);
            insideVisual.draw(canvas, basePaint);
            canvas.restore();
        }

        // Draw right parenthesis arc
        RectF rightOval = new RectF(b.x - (parenW * 1.5f), 0, b.x - (thickness / 2), b.y);
        canvas.drawArc(rightOval, 300, 120, false, arcPaint);
    }

    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        if (insideVisual != null) {
            PointF p = insideVisual.getCursorPosition(index, basePaint);
            return new PointF(insideVisual.HiPER.x + p.x, insideVisual.HiPER.y + p.y);
        }
        return new PointF(b.x / 2f, m);
    }

    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        if (insideVisual != null) {
            PointF localPoint = new PointF(point.x - insideVisual.HiPER.x, point.y - insideVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = insideVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(parenNode.content, 0);
        }
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(parenNode, 0);
    }
}
