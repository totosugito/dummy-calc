package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;

/**
 * Base visual layout element.
 * Faithful recreation of android.core.AbstractC0335wD from HiPER Calc.
 * Handles dimensional calculation, hierarchy, baseline, hit-testing, and precise cursor coordinate locating.
 */
public abstract class MathVisual {
    public MathVisual parent;
    public ExpressionNode modelNode;

    // Dimensions: b.x = width, b.y = height (same field names as AbstractC0335wD)
    public final PointF b = new PointF(0.0f, 0.0f);
    // Position within parent: HiPER.x = left, HiPER.y = top
    public final PointF HiPER = new PointF(0.0f, 0.0f);
    // Baseline offset
    public float m = 0.0f;
    // Scale factor (D in HiPER)
    public float D = 1.0f;

    public MathVisual(ExpressionNode node) {
        this.modelNode = node;
    }

    public void setScale(float scale) {
        this.D = scale;
    }

    public void setPosition(float x, float y) {
        this.HiPER.x = x;
        this.HiPER.y = y;
    }

    /**
     * Measure and layout dimensions.
     * Equivalent to mo63HiPER() in AbstractC0335wD.
     */
    public abstract void calculateLayout(Paint basePaint);

    /**
     * Render Canvas content.
     * Equivalent to HiPER(Canvas, String) in AbstractC0335wD.
     */
    public abstract void draw(Canvas canvas, Paint basePaint);

    /**
     * Returns cursor position relative to this visual component.
     * Equivalent to mo359HiPER(int pos) in AbstractC0335wD.
     * x is horizontal offset, y is baseline offset (this.m).
     */
    public abstract PointF getCursorPosition(int index, Paint basePaint);

    /**
     * Cursor caret thickness matching HiPER Calc AbstractC0335wD.HiPER(Paint):
     * paint.measureText(" ") * 0.35f (ZD.Sc)
     */
    public float getCursorWidth(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);
        return Math.max(3.0f, paint.measureText(" ") * 0.35f);
    }

    /**
     * Calculate bounding rect for cursor in local coordinates.
     * Faithful to AbstractC0335wD.mo360HiPER() & C0294rh.mo360HiPER():
     * top = cyBaseline - (-paint.ascent())
     * bottom = cyBaseline + paint.descent()
     */
    public RectF getCursorRect(int index, Paint basePaint) {
        PointF point = getCursorPosition(index, basePaint);
        if (point == null) {
            return null;
        }

        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float cursorWidth = getCursorWidth(basePaint);
        float halfWidth = cursorWidth / 2.0f;
        float cyBaseline = point.y;
        float topY = cyBaseline - (-paint.ascent());
        float bottomY = cyBaseline + paint.descent();

        return new RectF(point.x - halfWidth, topY, point.x + halfWidth, bottomY);
    }

    /**
     * Touch coordinate hit testing.
     * Equivalent to HiPER(PointF, boolean, boolean) in AbstractC0335wD.
     */
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        return null;
    }
}
