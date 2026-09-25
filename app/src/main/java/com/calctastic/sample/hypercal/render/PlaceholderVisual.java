package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.EmptyNode;

/**
 * Visual renderer for empty placeholder node (box outline).
 * 100% FAITHFUL TO HiPER Calc android.core.C0357yG.java.
 *
 * Implements:
 * - Box width: paint.measureText("0") * 1.2f (C0357yG line 72)
 * - Box height: (-paint.ascent() + paint.descent())
 * - Stroke width: paint.measureText("0") * 0.1f (C0357yG line 210)
 * - Inset margin: 0.15f * paint.measureText("0") (C0357yG line 206)
 * - Stroke drawing: canvas.drawRect(left, top, right, bottom, strokePaint) (C0357yG line 218)
 * - Cursor centered in the box
 */
public class PlaceholderVisual extends MathVisual {
    public final EmptyNode emptyNode;

    public PlaceholderVisual(EmptyNode node) {
        super(node);
        this.emptyNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float measure0 = paint.measureText("0");
        b.x = measure0 * 1.2f;
        float fontHeight = -paint.ascent() + paint.descent();
        b.y = fontHeight;
        m = -paint.ascent();
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float measure0 = paint.measureText("0");
        float insetX = 0.15f * measure0;
        float strokeWidth = Math.max(1.5f, measure0 * 0.1f);

        Paint boxPaint = new Paint(paint);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(strokeWidth);
        boxPaint.setColor(0x80FFFFFF); // Semi-transparent secondary outline matching HiPER theme

        float top = m + paint.ascent() * 0.9f;
        float bottom = m + paint.descent() * 0.9f;
        float left = insetX;
        float right = b.x - insetX;

        canvas.drawRect(left, top, right, bottom, boxPaint);
    }

    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        return new PointF(b.x / 2.0f, m);
    }

    @Override
    public RectF getCursorRect(int index, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);
        float cursorWidth = Math.max(2.0f, paint.measureText(" ") * 0.35f);
        float cx = b.x / 2.0f;
        float topY = m + paint.ascent() * 0.75f;
        float bottomY = m + paint.descent() * 0.75f;
        return new RectF(cx - cursorWidth / 2.0f, topY, cx + cursorWidth / 2.0f, bottomY);
    }

    @Override
    public CursorPointer hitTest(PointF point, Paint basePaint) {
        return new CursorPointer(emptyNode, 0);
    }
}
