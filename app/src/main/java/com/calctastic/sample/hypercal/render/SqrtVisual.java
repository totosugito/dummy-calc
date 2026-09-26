package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;

/**
 * Radical / Square Root (and n-th root) visual renderer.
 *
 * The 5-point polygonal outline + perpendicular-normal thickness expansion + miter joints below
 * still follow the general SHAPE of HiPER Calc's android.core.C0102Vh.java, but the specific
 * proportions (tick width, margins, thickness) are NOT a literal port -- C0102Vh's own draw
 * method mixes in a same-named zero-arg method that returns a float in one call site
 * (`k() * 0.9f`) and an AbstractC0335wD (a child visual) in another (`AbstractC0335wD
 * abstractC0335wDK = k();`), which cannot both be the same method; the decompiler has collapsed
 * two distinct obfuscated methods onto one name. Rather than guess which original formula that
 * first call was actually part of, this class uses its own proportions, chosen to look like a
 * normal, uncluttered radical sign and scale sensibly with content height instead of a fixed
 * font-size fraction (which produced a too-wide gap before the tick and an oversized vinculum
 * overshoot at small sizes -- found 2026-09-26 while adding n-th-root support, see
 * specs/btn_sqrt.md).
 *
 * calculateLayout() stores the exact geometry (tick/vinculum position and size) it computes as
 * instance fields, and draw() reuses those fields directly instead of recomputing its own
 * (slightly different) copies of the same formulas -- the previous version computed fTickWidth/
 * fBottomY/fTopY independently in both methods, which could silently drift out of sync.
 */
public class SqrtVisual extends MathVisual {
    public final SqrtNode sqrtNode;
    public MathVisual radicandVisual;
    public MathVisual degreeVisual;

    // Geometry computed once in calculateLayout() and reused as-is by draw() -- see class doc.
    private float tickWidth;
    private float tickHeight;
    private float thickness;
    private float startX;
    private float contentGap;
    // How far the tick/vinculum/radicand are pushed down from this visual's own top, to leave
    // room for the degree floating above the vinculum (n-th root only; 0 for plain √x).
    private float vinculumY;

    public SqrtVisual(SqrtNode node) {
        super(node);
        this.sqrtNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        // Measure degree if present. Per standard math typesetting (matching plain LaTeX
        // \sqrt[n]{...}), the degree floats ABOVE the vinculum, tucked over the tick's rising
        // stroke -- not down at the baseline (a previous version placed it at the bottom, which
        // looked wrong; found 2026-09-26 when a real n-th-root button first exercised this code
        // path, see specs/btn_sqrt.md).
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

        // Breathing room between the vinculum and the content's own top/left, proportional to
        // the content's height so it doesn't look oversized for a single digit or cramped for a
        // tall fraction/power underneath the root -- content should never touch the radical
        // sign's strokes directly.
        float topMargin = radicandH * 0.22f;
        tickHeight = radicandH + topMargin;
        // The tick (checkmark) portion's width scales with its own height, like a real radical
        // glyph -- a taller root gets a proportionally wider hook, not a fixed pixel width.
        tickWidth = tickHeight * 0.32f;
        thickness = Math.max(1.6f, paint.getTextSize() * 0.05f);
        contentGap = radicandH * 0.12f;

        // The degree sits entirely above the vinculum, flush with this visual's own top (y=0) --
        // the vinculum itself is pushed down by the degree's full height to make room. Using
        // anything less than the FULL degreeHeight here made the degree's top go negative (outside
        // this visual's own declared bounding box), which got silently clipped by whatever drew
        // above it (found 2026-09-26 on first real on-device check of the degree slot, see
        // specs/btn_sqrt.md) -- MathVisual has no notion of a child rendering "outside" its own
        // [0, b.y] box, so that space must actually be reserved, not just implied.
        vinculumY = degreeVisual != null ? degreeHeight : 0.0f;

        float totalW = tickWidth + contentGap + radicandW + (thickness * 0.5f);
        float totalH = vinculumY + tickHeight;

        // Radical bar baseline alignment
        float fRadicandBaseline = (totalH - radicandH) + radicandM;
        this.m = fRadicandBaseline;

        // Position children
        startX = 0;
        if (degreeVisual != null) {
            // Flush with the top (y=0); its bottom edge lands exactly at the vinculum's height
            // (vinculumY == degreeHeight), near the LEFT of the tick (above its rising stroke)
            // rather than pushing the whole tick rightward the way a same-line prefix would.
            degreeVisual.setPosition(0, 0);
        }

        float radicandX = startX + tickWidth + contentGap;
        float radicandY = vinculumY + (tickHeight - radicandH);
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

        // Anatomy of the tick, all relative to tickHeight (computed in calculateLayout): start
        // at mid-height, dip down to a low point (the checkmark's notch), then rise sharply to
        // the vinculum, then run the vinculum horizontally to the right edge. Shifted down by
        // vinculumY so there's room above for a floating degree (n-th root only; 0 for √x).
        float bottomY = vinculumY + tickHeight;
        float topY = vinculumY;

        PointF p0 = new PointF(startX, vinculumY + (tickHeight * 0.62f));
        PointF p1 = new PointF(startX + (tickWidth * 0.32f), vinculumY + (tickHeight * 0.82f));
        PointF p2 = new PointF(startX + (tickWidth * 0.6f), bottomY);
        PointF p3 = new PointF(startX + tickWidth, topY);
        PointF p4 = new PointF(b.x, topY);

        PointF[] pointFArr = new PointF[]{p0, p1, p2, p3, p4};

        float fThickness = thickness;
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

    // No getCursorPosition() override -- the default MathVisual behavior (index 0/1 -> just
    // outside this visual's own box) is exactly right for CursorPointer(sqrtNode, 0/1)
    // (Center-before/after the whole sqrt), same as PowerVisual/FractionVisual don't override it
    // either. A previous version of this method unconditionally delegated straight into
    // radicandVisual regardless of index, which was wrong even for plain √x (ignored the
    // Center-before/after case entirely) and became reachable more often once an n-th-root
    // button started actually using the degree slot -- see specs/btn_sqrt.md.

    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        // Mirrors PowerVisual.hitTest's shape: degree (if present) takes priority on its own
        // horizontal span, then radicand, matching reading order (degree, then the radical).
        // Previously this ignored degreeVisual entirely, so tapping the degree box would fall
        // through to the radicand instead.
        if (degreeVisual != null) {
            float degLeft = degreeVisual.HiPER.x;
            float degRight = degLeft + degreeVisual.b.x;
            if (point.x >= degLeft && point.x <= degRight) {
                PointF localPoint = new PointF(point.x - degLeft, point.y - degreeVisual.HiPER.y);
                com.calctastic.sample.hypercal.engine.model.CursorPointer hit = degreeVisual.hitTest(localPoint, basePaint);
                if (hit != null) return hit;
                return new com.calctastic.sample.hypercal.engine.model.CursorPointer(sqrtNode.degree, 0);
            }
        }
        if (radicandVisual != null) {
            PointF localPoint = new PointF(point.x - radicandVisual.HiPER.x, point.y - radicandVisual.HiPER.y);
            com.calctastic.sample.hypercal.engine.model.CursorPointer hit = radicandVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(sqrtNode.radicand, 0);
        }
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(sqrtNode, 0);
    }
}
