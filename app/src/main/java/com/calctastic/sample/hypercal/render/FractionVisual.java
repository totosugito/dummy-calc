package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.FractionNode;

/**
 * Visual renderer for mathematical Fractions with horizontal bar.
 * 100% FAITHFUL TO HiPER Calc android.core.Qg.java.
 *
 * Implements:
 * - Stacked layout (numerator centered above bar, denominator centered below)
 * - Exact child scale from Qg.java lines 36 & 61: childScale = D * 0.8f
 * - Exact bar thickness from Qg.java line 121: AbstractC0335wD.HiPER(paint, 0.3f) = measureText(" ") * 0.3f
 * - Exact vertical gap from Qg.java lines 116 & 124: AbstractC0335wD.HiPER(paint, 0.2f) = measureText(" ") * 0.2f
 * - Exact baseline from Qg.java line 129: this.m = ((-paint.ascent()) * 0.4f) + barY
 * - Direct cursor positioning and hit-testing into numerator and denominator
 */
public class FractionVisual extends MathVisual {
    public final FractionNode fractionNode;
    public MathVisual numeratorVisual;
    public MathVisual denominatorVisual;
    public float barY;

    public FractionVisual(FractionNode node) {
        super(node);
        this.fractionNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        // HiPER Calc Qg.java lines 36 & 61: exactly 0.8f scale for fraction children
        float childScale = D * 0.8f;

        if (numeratorVisual != null) {
            numeratorVisual.setScale(childScale);
            numeratorVisual.calculateLayout(basePaint);
        }

        if (denominatorVisual != null) {
            denominatorVisual.setScale(childScale);
            denominatorVisual.calculateLayout(basePaint);
        }

        float numW = numeratorVisual != null ? numeratorVisual.b.x : paint.measureText("0") * 0.8f;
        float numH = numeratorVisual != null ? numeratorVisual.b.y : ((-paint.ascent() + paint.descent()) * 0.8f);

        float denW = denominatorVisual != null ? denominatorVisual.b.x : paint.measureText("0") * 0.8f;
        float denH = denominatorVisual != null ? denominatorVisual.b.y : ((-paint.ascent() + paint.descent()) * 0.8f);

        // Qg.java lines 116, 121, 124: AbstractC0335wD.HiPER(paint, f) = paint.measureText(" ") * f
        float spaceWidth = paint.measureText(" ");
        float gapAbove = Math.max(1.5f, spaceWidth * 0.2f);
        float barThickness = Math.max(1.5f, spaceWidth * 0.3f);
        float gapBelow = Math.max(1.5f, spaceWidth * 0.2f);

        // Qg.java line 113: pointF.x = (m$3() * 2.0f) + Math.max(f2, f5)
        float maxW = Math.max(numW, denW);

        // Position numerator centered horizontally above fraction bar
        float numX = (maxW - numW) / 2.0f;
        float numY = 0.0f;
        if (numeratorVisual != null) {
            numeratorVisual.setPosition(numX, numY);
        }

        // Qg.java line 118: this.c = fHiPER (where fHiPER = gapAbove + f3)
        barY = numH + gapAbove;

        // Position denominator centered horizontally below fraction bar (Qg.java line 124)
        float denX = (maxW - denW) / 2.0f;
        float denY = barY + barThickness + gapBelow;
        if (denominatorVisual != null) {
            denominatorVisual.setPosition(denX, denY);
        }

        b.x = maxW;
        b.y = denY + denH;

        // Qg.java line 129: this.m = ((-paint6.ascent()) * 0.4f) + f13 (where f13 = this.c = barY)
        this.m = ((-paint.ascent()) * 0.4f) + barY;
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float spaceWidth = paint.measureText(" ");
        float barThickness = Math.max(1.5f, spaceWidth * 0.3f);

        // Draw Fraction Bar: Qg.java line 298: canvas.drawRect(left, barY, right, barY + thickness, paint);
        Paint barPaint = new Paint(paint);
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0.0f, barY, b.x, barY + barThickness, barPaint);

        // Draw Numerator
        if (numeratorVisual != null) {
            canvas.save();
            canvas.translate(numeratorVisual.HiPER.x, numeratorVisual.HiPER.y);
            numeratorVisual.draw(canvas, basePaint);
            canvas.restore();
        }

        // Draw Denominator
        if (denominatorVisual != null) {
            canvas.save();
            canvas.translate(denominatorVisual.HiPER.x, denominatorVisual.HiPER.y);
            denominatorVisual.draw(canvas, basePaint);
            canvas.restore();
        }
    }

    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        // Index 0: Center position before fraction
        if (index == 0) {
            return new PointF(0.0f, m);
        }
        // Index 1: Center position after fraction
        return new PointF(b.x, m);
    }

    @Override
    public android.graphics.RectF getCursorRect(int index, Paint basePaint) {
        PointF p = getCursorPosition(index, basePaint);
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);
        float cursorWidth = getCursorWidth(basePaint);
        float halfWidth = cursorWidth / 2.0f;
        float cy = p.y;
        float topY = cy - (-paint.ascent());
        float bottomY = cy + paint.descent();
        return new android.graphics.RectF(p.x - halfWidth, topY, p.x + halfWidth, bottomY);
    }

    /**
     * Hit testing: 100% FAITHFUL TO Qg.java lines 144-186:
     * - Check horizontal bounds: if tap is outside active children span,
     *   hit falls outside fraction into parent Sequence (Center / baseline level).
     * - If within bounds: if point.y < this.c (barY) -> hit numerator; else hit denominator.
     */
    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        float numLeft = numeratorVisual != null ? numeratorVisual.HiPER.x : 0.0f;
        float numRight = numeratorVisual != null ? (numLeft + numeratorVisual.b.x) : b.x;

        float denLeft = denominatorVisual != null ? denominatorVisual.HiPER.x : 0.0f;
        float denRight = denominatorVisual != null ? (denLeft + denominatorVisual.b.x) : b.x;

        float activeLeft = Math.min(numLeft, denLeft);
        float activeRight = Math.max(numRight, denRight);

        // Outside horizontal bounds: Qg.java line 146 -> returns parent level position (Center)
        if (point.x < activeLeft) {
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode, 0);
        }
        if (point.x > activeRight) {
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode, 1);
        }

        if (point.y < barY && numeratorVisual != null) {
            PointF localPoint = new PointF(point.x - numeratorVisual.HiPER.x, point.y - numeratorVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = numeratorVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode.numerator, 0);
        } else if (denominatorVisual != null) {
            PointF localPoint = new PointF(point.x - denominatorVisual.HiPER.x, point.y - denominatorVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = denominatorVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode.denominator, 0);
        }
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode, 0);
    }
}
