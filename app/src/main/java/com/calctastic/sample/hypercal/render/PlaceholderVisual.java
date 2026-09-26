package com.calctastic.sample.hypercal.render;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.EmptyNode;

/**
 * Visual renderer for empty placeholder node (box outline).
 * 100% FAITHFUL TO HiPER Calc android.core.C0357yG.java.
 *
 * Implements:
 * - Box width: paint.measureText("0") * 1.2f (C0357yG.mo63HiPER(), "not visible-slot" branch)
 * - Baseline / height (C0357yG.mo63HiPER()): m = G() + 0.9f * density; b.y = descent() + m
 *   (G() = -paint.ascent()); no minimum clamp in the original.
 * - Stroke width: paint.measureText("0") * 0.1f; inset margin: 0.15f * paint.measureText("0")
 *   (C0357yG.HiPER(Canvas, String))
 * - Cursor position: C0357yG does not override mo359HiPER(int), so the base
 *   AbstractC0335wD default applies (see MathVisual) — cursor sits just left of the box,
 *   not centered inside it.
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

        float density = Resources.getSystem().getDisplayMetrics().density;
        m = -paint.ascent() + 0.9f * density;
        b.y = paint.descent() + m;
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float measure0 = paint.measureText("0");
        float insetX = 0.15f * measure0;
        float strokeWidth = measure0 * 0.1f;

        // Color: AbstractC0335wD.HiPER(Paint, String) (used by C0357yG for this box) returns the
        // paint UNCHANGED when the theme has no entry for the highlight key it looks up, which is
        // the normal (non-highlighted) case here — so the box outline is the same color as the
        // surrounding text, not a separate hardcoded tint.
        Paint boxPaint = new Paint(paint);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(strokeWidth);

        canvas.drawRect(insetX, 0.0f, b.x - insetX, b.y, boxPaint);
    }

    @Override
    public CursorPointer hitTest(PointF point, Paint basePaint) {
        return new CursorPointer(emptyNode, 0);
    }
}
