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
     * Default AbstractC0335wD.mo359HiPER(int) (used by any visual that doesn't override it,
     * e.g. Fraction/Placeholder/Power/Sqrt/Parenthesis at their own boundary indices):
     *   index 0    -> (-0.5 * cursorWidth, 0), nudged just left of this visual
     *   otherwise  -> (b.x + 0.5 * cursorWidth, 0), nudged just right of this visual
     */
    public PointF getCursorPosition(int index, Paint basePaint) {
        float halfW = getCursorWidth(basePaint) / 2.0f;
        if (index <= 0) {
            return new PointF(-halfW, 0.0f);
        }
        return new PointF(b.x + halfW, 0.0f);
    }

    /**
     * Cursor caret thickness matching HiPER Calc AbstractC0335wD.HiPER(Paint):
     * paint.measureText(" ") * 0.35f (ZD.Sc). No minimum clamp in the original.
     */
    public float getCursorWidth(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);
        return paint.measureText(" ") * 0.35f;
    }

    /**
     * Calculate bounding rect for cursor in local coordinates.
     * Faithful to the DEFAULT AbstractC0335wD.mo360HiPER()/mo359HiPER(int) (used by any
     * visual that does not override them, e.g. Sequence/Fraction/Power/Sqrt/Parenthesis):
     *   mo359HiPER(0)    = (-0.5 * cursorWidth, 0)         // nudged just left of this visual
     *   mo359HiPER(last) = (b.x + 0.5 * cursorWidth, 0)    // nudged just right of this visual
     *   mo360HiPER()     = RectF(x - w/2, 0, x + w/2, b.y) // full local height, centered on x
     * A subclass overriding getCursorPosition for an interior index (e.g. mid-digit in
     * NumberVisual) returns the real x there, which this formula centers on without nudging.
     */
    public RectF getCursorRect(int index, Paint basePaint) {
        PointF point = getCursorPosition(index, basePaint);
        if (point == null) {
            return null;
        }

        float halfWidth = getCursorWidth(basePaint) / 2.0f;
        return new RectF(point.x - halfWidth, 0.0f, point.x + halfWidth, b.y);
    }

    /**
     * Touch coordinate hit testing.
     * Equivalent to HiPER(PointF, boolean, boolean) in AbstractC0335wD.
     */
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        return null;
    }
}
