package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
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

        // Decorative parentheses around the base (e.g. xʸ, PowerNode.needsParenthesesForBase()):
        // NOT a real ParenthesisNode -- confirmed against the real app that this "()" is purely
        // rendering, so it lives here rather than as a navigable child. Formula matches
        // ParenthesisVisual's own parenW so both look the same.
        boolean parens = powerNode.needsParenthesesForBase();
        float parenW = parens ? paint.measureText("(") * 0.9f : 0.0f;

        float baseW = baseVisual != null ? baseVisual.b.x : paint.measureText("0");
        float baseH = baseVisual != null ? baseVisual.b.y : (-paint.ascent() + paint.descent());
        float baseBaseline = baseVisual != null ? baseVisual.m : -paint.ascent();
        float baseColumnW = baseW + 2.0f * parenW;

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
        float totalW = baseColumnW + fMargin + expW;
        float totalH = Math.max(baseH, f12 + (baseH - baseBaseline));
        float totalBaseline = (totalH - baseH) + baseBaseline;

        // Position base (shifted right by parenW to leave room for the decorative "(")
        float baseY = totalH - baseH;
        if (baseVisual != null) {
            baseVisual.setPosition(parenW, baseY);
        }

        // Position exponent raised above baseline
        float expX = baseColumnW + fMargin;
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
            if (powerNode.needsParenthesesForBase()) {
                drawBaseParentheses(canvas, basePaint);
            }
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

    /** Decorative "(" ")" around the base -- same arc formula as ParenthesisVisual. */
    private void drawBaseParentheses(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float parenW = paint.measureText("(") * 0.9f;
        float thickness = Math.max(1.5f, paint.getTextSize() * 0.055f);
        float baseY = baseVisual.HiPER.y;
        float baseH = baseVisual.b.y;
        float baseColumnRight = baseVisual.HiPER.x + baseVisual.b.x + parenW;

        Paint arcPaint = new Paint(paint);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(thickness);

        RectF leftOval = new RectF(thickness / 2, baseY, parenW * 1.5f, baseY + baseH);
        canvas.drawArc(leftOval, 120, 120, false, arcPaint);

        RectF rightOval = new RectF(baseColumnRight - (parenW * 1.5f), baseY, baseColumnRight - (thickness / 2), baseY + baseH);
        canvas.drawArc(rightOval, 300, 120, false, arcPaint);
    }

    // Center before/after the whole power (index 0/1 on the PowerNode itself, e.g. after
    // insertSquare()): C0311tf.java never overrides mo359HiPER(int) at all -- confirmed by
    // decompile research -- so the MathVisual default applies here too, same as FractionVisual
    // and PlaceholderVisual. That default uses the power's OWN total width (b.x) for the
    // left/right split, unlike our previous override which delegated into the exponent's own
    // position-0, collapsing "after the power" onto "start of exponent" -- the bug that made
    // right-arrow look stuck at the exponent's left edge instead of moving past the whole power.

    /**
     * Hit testing, mirroring FractionVisual's bounds-check pattern: a tap can land far outside
     * this power's own content (e.g. in blank canvas past the end of the whole expression) yet
     * still get routed here by the parent SequenceVisual as the nearest/last child. Without an
     * explicit bounds check, such a tap would fall through to whichever slot's region contains
     * that x (usually the exponent, since it's rightmost) and get silently clamped to that
     * slot's own leftmost position -- the bug where tapping well to the right of "x^y" still put
     * the cursor at the exponent's left edge instead of past the whole power.
     */
    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        float baseLeft = baseVisual != null ? baseVisual.HiPER.x : 0.0f;
        float baseRight = baseVisual != null ? baseLeft + baseVisual.b.x : b.x;
        float expLeft = exponentVisual != null ? exponentVisual.HiPER.x : 0.0f;
        float expRight = exponentVisual != null ? expLeft + exponentVisual.b.x : b.x;

        float activeLeft = Math.min(baseLeft, expLeft);
        float activeRight = Math.max(baseRight, expRight);

        if (point.x < activeLeft) {
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(powerNode, 0);
        }
        if (point.x > activeRight) {
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(powerNode, 1);
        }

        if (exponentVisual != null && point.x >= expLeft) {
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
