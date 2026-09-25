package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;

/**
 * Radical / Square Root visual renderer.
 * 100% FAITHFUL TO HiPER Calc android.core.C0102Vh.java (SqrtVisual).
 *
 * Implements:
 * - 5-point polygonal radical sign outline
 * - Normal vector perpendicular expansion for uniform line thickness
 * - Baseline alignment matching C0102Vh: m = ((f + f3) - fK) + fMeasureText
 * - Multi-segment quad strip polygon generation with miter joints
 */
public class SqrtVisual extends MathVisual {
    public final SqrtNode sqrtNode;
    public MathVisual radicandVisual;
    public MathVisual degreeVisual;

    public SqrtVisual(SqrtNode node) {
        super(node);
        this.sqrtNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        // HiPER Sqrt scale: C0102Vh.k() * 0.9f
        float fK2 = paint.getTextSize() * 0.25f;

        // Measure degree if present
        float degreeWidth = 0.0f;
        float degreeHeight = 0.0f;
        if (degreeVisual != null) {
            degreeVisual.setScale(D * 0.6f);
            degreeVisual.calculateLayout(basePaint);
            degreeWidth = degreeVisual.b.x;
            degreeHeight = degreeVisual.b.y;
        }

        // Measure radicand under root line
        if (radicandVisual != null) {
            radicandVisual.setScale(D);
            radicandVisual.calculateLayout(basePaint);
        }

        float radicandW = radicandVisual != null ? radicandVisual.b.x : paint.measureText("0") * 0.8f;
        float radicandH = radicandVisual != null ? radicandVisual.b.y : (-paint.ascent() + paint.descent());
        float radicandM = radicandVisual != null ? radicandVisual.m : -paint.ascent();

        // Extra radical margins from HiPER Calc C0102Vh
        float fTopMargin = fK2 * 0.5f;
        float fTickWidth = fK2 * 2.5f;
        float fTickHeight = radicandH + fTopMargin + (fK2 * 0.5f);

        float totalW = degreeWidth + fTickWidth + radicandW + fK2;
        float totalH = Math.max(degreeHeight, fTickHeight);

        // Radical bar baseline alignment
        float fRadicandBaseline = (totalH - radicandH) + radicandM;
        this.m = fRadicandBaseline;

        // Position children
        float startX = 0;
        if (degreeVisual != null) {
            degreeVisual.setPosition(0, 0);
            startX += degreeWidth;
        }

        float radicandX = startX + fTickWidth;
        float radicandY = totalH - radicandH;
        if (radicandVisual != null) {
            radicandVisual.setPosition(radicandX, radicandY);
        }

        b.x = totalW;
        b.y = totalH;
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float fK2 = paint.getTextSize() * 0.25f;
        float fTickWidth = fK2 * 2.5f;
        float degreeWidth = degreeVisual != null ? degreeVisual.b.x : 0.0f;

        float fStartX = degreeWidth;
        float fBottomY = b.y - (fK2 * 0.2f);
        float fTopY = (b.y - (radicandVisual != null ? radicandVisual.b.y : (-paint.ascent() + paint.descent()))) - (fK2 * 0.3f);
        if (fTopY < 2.0f) fTopY = 2.0f;

        // 5-point polygon radical path from C0102Vh.java lines 60-100
        PointF p0 = new PointF(fStartX, fBottomY - (fK2 * 1.5f));
        PointF p1 = new PointF(fStartX + (fTickWidth * 0.35f), fBottomY - (fK2 * 0.8f));
        PointF p2 = new PointF(fStartX + (fTickWidth * 0.65f), fBottomY);
        PointF p3 = new PointF(fStartX + fTickWidth, fTopY);
        PointF p4 = new PointF(b.x, fTopY);

        PointF[] pointFArr = new PointF[]{p0, p1, p2, p3, p4};

        // Thickness computation
        float fThickness = Math.max(1.8f, paint.getTextSize() * 0.065f);
        float f4 = 2.0f;
        float[] fArr = new float[]{fThickness * 0.9f, fThickness * 1.2f, fThickness * 1.6f, fThickness};
        float[] fArr2 = new float[4];
        float[] fArr3 = new float[4];
        float[] fArr4 = new float[4];
        float[] fArr5 = new float[4];

        int i5 = 4;
        for (int i6 = 0; i6 < i5; i6++) {
            int i7 = i6 + 1;
            PointF pNext = pointFArr[i7];
            PointF pCur = pointFArr[i6];
            float f8 = pNext.x - pCur.x;
            float f9 = pNext.y - pCur.y;
            float fSqrt = (float) Math.sqrt((f9 * f9) + (f8 * f8));
            if (fSqrt == 0) fSqrt = 1.0f;
            fArr2[i6] = f8 / fSqrt;
            fArr3[i6] = f9 / fSqrt;
            fArr4[i6] = ((-fArr3[i6]) * fArr[i6]) / f4;
            fArr5[i6] = (fArr2[i6] * fArr[i6]) / f4;
        }

        Path path = new Path();
        path.setFillType(Path.FillType.WINDING);

        // Segment quad strips
        for (int i10 = 0; i10 < 4; i10++) {
            PointF pCur = pointFArr[i10];
            PointF pNext = pointFArr[i10 + 1];
            float f12 = pCur.x;
            float f13 = fArr4[i10];
            float f14 = pNext.x;
            float f17 = pCur.y;
            float f18 = fArr5[i10];
            float f19 = pNext.y;

            float[] xArr = {f12 + f13, f14 + f13, f14 - f13, f12 - f13};
            float[] yArr = {f17 + f18, f19 + f18, f19 - f18, f17 - f18};
            addPolygonToPath(path, xArr, yArr);
        }

        // Joints between segments
        for (int i14 = 1; i14 < 4; i14++) {
            PointF pJoint = pointFArr[i14];
            int iPrev = i14 - 1;
            float sign = (fArr2[iPrev] * fArr3[i14]) - (fArr3[iPrev] * fArr2[i14]) > 0.0f ? -1.0f : 1.0f;
            float jx = pJoint.x;
            float jy = pJoint.y;

            float x1 = (fArr4[iPrev] * sign) + jx;
            float y1 = (fArr5[iPrev] * sign) + jy;
            float x2 = (fArr4[i14] * sign) + jx;
            float y2 = (fArr5[i14] * sign) + jy;

            float[] xJoint = {jx, x1, x2};
            float[] yJoint = {jy, y1, y2};
            addPolygonToPath(path, xJoint, yJoint);
        }

        Paint fillPaint = new Paint(paint);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, fillPaint);

        // Draw children
        if (degreeVisual != null) {
            canvas.save();
            canvas.translate(degreeVisual.HiPER.x, degreeVisual.HiPER.y);
            degreeVisual.draw(canvas, basePaint);
            canvas.restore();
        }
        if (radicandVisual != null) {
            canvas.save();
            canvas.translate(radicandVisual.HiPER.x, radicandVisual.HiPER.y);
            radicandVisual.draw(canvas, basePaint);
            canvas.restore();
        }
    }

    /**
     * Exact polygon orientation helper from HiPER Calc C0102Vh.HiPER(Path, float[], float[])
     */
    private static void addPolygonToPath(Path path, float[] fArr, float[] fArr2) {
        int length = fArr.length;
        float f = 0.0f;
        for (int i = 0; i < length; i++) {
            int length2 = (i + 1) % fArr.length;
            f += (fArr[i] * fArr2[length2]) - (fArr[length2] * fArr2[i]);
        }
        if (f >= 0.0f) {
            path.moveTo(fArr[0], fArr2[0]);
            for (int i3 = 1; i3 < fArr.length; i3++) {
                path.lineTo(fArr[i3], fArr2[i3]);
            }
        } else {
            path.moveTo(fArr[fArr.length - 1], fArr2[fArr.length - 1]);
            for (int length4 = fArr.length - 2; length4 >= 0; length4--) {
                path.lineTo(fArr[length4], fArr2[length4]);
            }
        }
        path.close();
    }

    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        if (radicandVisual != null) {
            PointF p = radicandVisual.getCursorPosition(index, basePaint);
            return new PointF(radicandVisual.HiPER.x + p.x, radicandVisual.HiPER.y + p.y);
        }
        return new PointF(b.x, m);
    }

    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        if (radicandVisual != null) {
            PointF localPoint = new PointF(point.x - radicandVisual.HiPER.x, point.y - radicandVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = radicandVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(sqrtNode.radicand, 0);
        }
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(sqrtNode, 0);
    }
}
