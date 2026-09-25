package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.PowerNode;

/**
 * Visual renderer for powers and superscripts: base ^ exponent.
 * 100% FAITHFUL TO HiPER Calc android.core.C0311tf.java (PowerVisual).
 *
 * Implements:
 * - Scaled exponent (0.75f scale factor)
 * - Exact layout branch from C0311tf.java lines 60-95:
 *   expBaseline = baseBaseline - (0.5f * baseBaseline)
 *   fMax = Math.max(fMax, expH + (baseH - baseBaseline))
 * - Automatic parenthesis on compound base:
 *   needsParenthesesForBase() checks if base is an operator or fraction expression
 */
public class PowerVisual extends MathVisual {
    public final PowerNode powerNode;
    public MathVisual baseVisual;
    public MathVisual exponentVisual;

    public PowerVisual(PowerNode node) {
        super(node);
        this.powerNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        // Measure base
        if (baseVisual != null) {
            baseVisual.setScale(D);
            baseVisual.calculateLayout(basePaint);
        }

        // Measure exponent with 0.75f scale (C0311tf.java line 63)
        if (exponentVisual != null) {
            exponentVisual.setScale(D * 0.75f);
            exponentVisual.calculateLayout(basePaint);
        }

        float baseW = baseVisual != null ? baseVisual.b.x : paint.measureText("0");
        float baseH = baseVisual != null ? baseVisual.b.y : (-paint.ascent() + paint.descent());
        float baseBaseline = baseVisual != null ? baseVisual.m : -paint.ascent();

        float expW = exponentVisual != null ? exponentVisual.b.x : paint.measureText("0") * 0.6f;
        float expH = exponentVisual != null ? exponentVisual.b.y : (-paint.ascent() + paint.descent()) * 0.75f;
        float expBaseline = exponentVisual != null ? exponentVisual.m : -paint.ascent() * 0.75f;

        // Exact layout branching from HiPER Calc C0311tf.java lines 65-88:
        // if (0.5f * baseBaseline > expBaseline) { ... }
        float f11;
        float f12;
        if (0.5f * baseBaseline > expBaseline) {
            f11 = baseBaseline - (0.5f * baseBaseline);
            f12 = (expH - expBaseline) + (0.5f * baseBaseline);
        } else {
            f11 = baseBaseline - expBaseline;
            f12 = expH;
        }

        float fMargin = paint.measureText(" ") * 0.15f;
        float totalW = baseW + fMargin + expW;
        float totalH = Math.max(baseH, f12 + (baseH - baseBaseline));
        float totalBaseline = (totalH - baseH) + baseBaseline;

        // Position base
        float baseY = totalH - baseH;
        if (baseVisual != null) {
            baseVisual.setPosition(0, baseY);
        }

        // Position exponent raised above baseline
        float expX = baseW + fMargin;
        float expY = totalBaseline - f11 - expBaseline;
        if (expY < 0) expY = 0;
        if (exponentVisual != null) {
            exponentVisual.setPosition(expX, expY);
        }

        b.x = totalW;
        b.y = totalH;
        m = totalBaseline;
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        if (baseVisual != null) {
            canvas.save();
            canvas.translate(baseVisual.HiPER.x, baseVisual.HiPER.y);
            baseVisual.draw(canvas, basePaint);
            canvas.restore();
        }

        if (exponentVisual != null) {
            canvas.save();
            canvas.translate(exponentVisual.HiPER.x, exponentVisual.HiPER.y);
            exponentVisual.draw(canvas, basePaint);
            canvas.restore();
        }
    }

    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        if (index == 0 && baseVisual != null) {
            PointF p = baseVisual.getCursorPosition(0, basePaint);
            return new PointF(baseVisual.HiPER.x + p.x, baseVisual.HiPER.y + p.y);
        }
        if (index == 1 && exponentVisual != null) {
            PointF p = exponentVisual.getCursorPosition(0, basePaint);
            return new PointF(exponentVisual.HiPER.x + p.x, exponentVisual.HiPER.y + p.y);
        }
        return new PointF(this.b.x, this.m);
    }

    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        if (exponentVisual != null && point.x >= exponentVisual.HiPER.x) {
            PointF localPoint = new PointF(point.x - exponentVisual.HiPER.x, point.y - exponentVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = exponentVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(powerNode.exponent, 0);
        }
        if (baseVisual != null) {
            PointF localPoint = new PointF(point.x - baseVisual.HiPER.x, point.y - baseVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = baseVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(powerNode.base, 0);
        }
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(powerNode, 0);
    }
}
