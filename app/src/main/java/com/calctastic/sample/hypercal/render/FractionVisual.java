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
    public MathVisual integerVisual;
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

        // Mixed number ("a b/c", EnumC0300sa.sa) integer part: drawn at full size to the left
        // of the num/den stack, not scaled down like the numerator/denominator.
        if (integerVisual != null) {
            integerVisual.setScale(D);
            integerVisual.calculateLayout(basePaint);
        }

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
        float gapAbove = spaceWidth * 0.2f;
        float barThickness = spaceWidth * 0.3f;
        float gapBelow = spaceWidth * 0.2f;

        // Qg.java line 214-221 (m$3()): extra side padding when a child is itself a fraction,
        // so a nested fraction's bar doesn't touch this one's.
        float m3 = (numeratorVisual instanceof FractionVisual || denominatorVisual instanceof FractionVisual)
                ? spaceWidth * 1.0f : 0.0f;

        // Qg.java line 113: pointF.x = (m$3() * 2.0f) + Math.max(f2, f5)
        float maxW = Math.max(numW, denW);

        // Reserve room to the left for the mixed-number integer part, plus a small gap
        // (not present in Qg -- the plain fraction has no integer child).
        float intW = integerVisual != null ? integerVisual.b.x : 0.0f;
        float intGap = integerVisual != null ? spaceWidth * 0.5f : 0.0f;
        float stackX = intW + intGap;

        // Position numerator centered horizontally above fraction bar, offset by m$3() padding
        float numX = stackX + m3 + (maxW - numW) / 2.0f;
        float numY = 0.0f;
        if (numeratorVisual != null) {
            numeratorVisual.setPosition(numX, numY);
        }

        // Qg.java line 118: this.c = fHiPER (where fHiPER = gapAbove + f3)
        barY = numH + gapAbove;

        // Position denominator centered horizontally below fraction bar (Qg.java line 124)
        float denX = stackX + m3 + (maxW - denW) / 2.0f;
        float denY = barY + barThickness + gapBelow;
        if (denominatorVisual != null) {
            denominatorVisual.setPosition(denX, denY);
        }

        // Qg.java line 113: width = 2*m$3() + max(numW, denW), plus the integer part + gap
        b.x = stackX + maxW + 2.0f * m3;
        b.y = denY + denH;

        if (integerVisual != null) {
            // Vertically center the integer part against the whole mixed-number height
            integerVisual.setPosition(0.0f, (b.y - integerVisual.b.y) / 2.0f);
            b.y = Math.max(b.y, integerVisual.b.y);
        }

        // Qg.java line 129: this.m = ((-paint6.ascent()) * 0.4f) + f13 (where f13 = this.c = barY)
        this.m = ((-paint.ascent()) * 0.4f) + barY;
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float spaceWidth = paint.measureText(" ");
        float barThickness = spaceWidth * 0.3f;

        // Draw Fraction Bar: Qg.java line 304-313: canvas.drawRect(fMin - m$3(), barY, F(), barY + thickness, paint).
        // fMin (leftmost of numerator/denominator) already includes the m$3() left padding added in
        // calculateLayout, so fMin - m$3() cancels back to 0 and F() is the full (padded) width b.x.
        float barLeft = integerVisual != null ? integerVisual.HiPER.x + integerVisual.b.x + spaceWidth * 0.5f : 0.0f;
        Paint barPaint = new Paint(paint);
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(barLeft, barY, b.x, barY + barThickness, barPaint);

        // Draw mixed-number integer part
        if (integerVisual != null) {
            canvas.save();
            canvas.translate(integerVisual.HiPER.x, integerVisual.HiPER.y);
            integerVisual.draw(canvas, basePaint);
            canvas.restore();
        }

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

    // Center slot before/after the fraction: Qg does not override mo359HiPER(int), so the
    // MathVisual default applies (nudged just outside the fraction's bounds).

    /**
     * Hit testing: 100% FAITHFUL TO Qg.java lines 144-186:
     * - Check horizontal bounds: if tap is outside active children span,
     *   hit falls outside fraction into parent Sequence (Center / baseline level).
     * - If within bounds: if point.y < this.c (barY) -> hit numerator; else hit denominator.
     */
    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        if (integerVisual != null && point.x < integerVisual.HiPER.x + integerVisual.b.x) {
            PointF localPoint = new PointF(point.x - integerVisual.HiPER.x, point.y - integerVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = integerVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode.integerPart.getChild(0), 0);
        }

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
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode.numerator.getChild(0), 0);
        } else if (denominatorVisual != null) {
            PointF localPoint = new PointF(point.x - denominatorVisual.HiPER.x, point.y - denominatorVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = denominatorVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode.denominator.getChild(0), 0);
        }
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(fractionNode, 0);
    }
}
