package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;

/**
 * Radical / Square Root (and n-th root) visual renderer.
 *
 * A faithful port of HiPER Calc's android.core.C0102Vh's actual TECHNIQUE, confirmed from its
 * decompiled draw method to be manual vector drawing -- NOT a font glyph (there is no
 * `drawText("√", ...)` anywhere in it): it builds a 5-point outline, expands each segment into a
 * filled quad strip using the perpendicular of its own direction vector, adds a small filled
 * triangle at each interior joint (a manual miter join), and fills the resulting `Path` with
 * `Path.FillType.WINDING`. That whole technique -- point-list, perpendicular-normal thickness
 * expansion, per-joint triangle fill -- is copied here exactly, including reusing `addPolygonToPath`
 * (renamed from C0102Vh's own `HiPER(Path, float[], float[])`) unchanged.
 *
 * The five points and their horizontal spacing (5, 12, 22 times a shared unit, `mUnit` below) are
 * also a direct port of C0102Vh's own `m()`/`d()` methods -- unlike the class's SCALE unit, `m()`
 * only reads Paint metrics (`measureText`, `ascent`) and `this.b.y`, so it has no dependency on
 * anything this codebase lacks, and resolves precisely.
 *
 * One single quantity does NOT resolve precisely: the draw method's vinculum-height/thickness
 * base (`float fK2 = k() * 0.9f`) calls a `float k()` declared on the superclass
 * `AbstractC0335wD` (a DIFFERENT method from `C0102Vh`'s own `AbstractC0335wD k()` child-getter --
 * two distinct obfuscated methods the decompiler happened to print with the same name), which
 * resolves to `Tg.HiPER(this.G.i) * this.D * this.G.HiPER` -- a value from HiPER Calc's own
 * theme/style object that this codebase has no equivalent of and cannot know the true magnitude
 * of. Substituting the closest literal guess (`paint.getTextSize()`) produced a wildly broken
 * render (confirmed on-device: a solid blob dwarfing the whole glyph), since that value is clearly
 * far larger than intended.
 *
 * Instead of tuning `fK2` to an arbitrary multiplier, it's set to match {@link FractionVisual}'s
 * OWN already-faithfully-ported bar thickness exactly: `paint.measureText(" ") * 0.3f` (from
 * `FractionVisual`'s doc comment: "Exact bar thickness from Qg.java line 121:
 * AbstractC0335wD.HiPER(paint, 0.3f) = measureText(" ") * 0.3f" -- a DIFFERENT HiPER Calc visual
 * class, but the same underlying `measureText(" ") * constant` idiom for line thickness that
 * recurs across this app's visuals). This isn't a guess: it makes the radical sign's stroke and
 * the fraction bar's stroke the same visual weight by construction, which is both a reasonable
 * design default and grounded in an already-verified piece of the original codebase, rather than
 * an unrelated magnitude picked by eye.
 */
public class SqrtVisual extends MathVisual {

    public final SqrtNode sqrtNode;
    public MathVisual radicandVisual;
    public MathVisual degreeVisual;

    public SqrtVisual(SqrtNode node) {
        super(node);
        this.sqrtNode = node;
    }

    /**
     * Ported from C0102Vh's own {@code m()}: a font-size-based scale unit for the tick's
     * geometry, gently grown (0.15x factor) for taller content and explicitly clamped to
     * {@code [1.0, 1.6]} -- so the tick's own size stays close to normal even under a very tall
     * radicand (e.g. a fraction of a fraction), instead of growing without bound.
     */
    private static float mUnit(Paint paint, float ownHeight) {
        float spaceWidth = paint.measureText(" ");
        float refHeight = (spaceWidth * 0.6f) + (-paint.ascent());
        float growth = 1.0f + (((ownHeight - refHeight) * 0.15f) / refHeight);
        growth = Math.min(1.6f, Math.max(1.0f, growth));
        return growth * (spaceWidth / 10.0f);
    }

    /** Ported from C0102Vh's own {@code d()}: horizontal space reserved before the radicand. */
    private static float tickSpan(Paint paint, float ownHeight) {
        float spaceWidth = paint.measureText(" ");
        return ((mUnit(paint, ownHeight) - (spaceWidth / 10.0f)) * 22.0f) + (2.7f * spaceWidth);
    }

    /** Ported from C0102Vh's own {@code mo63HiPER()} layout method. */
    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float smallMargin = paint.measureText(" ") * 0.6f;

        float radicandBaseline;
        float radicandWidth;
        if (radicandVisual != null) {
            radicandVisual.setScale(D);
            radicandVisual.calculateLayout(basePaint);
            radicandBaseline = radicandVisual.m;
            radicandWidth = radicandVisual.b.x;
        } else {
            radicandBaseline = -paint.ascent();
            radicandWidth = 0.0f;
        }
        float radicandHeight = radicandVisual != null ? radicandVisual.b.y : 0.0f;

        if (degreeVisual != null) {
            degreeVisual.setScale(D * 0.75f);
            degreeVisual.calculateLayout(basePaint);
            float degreeBaseline = degreeVisual.m;
            float reserve = (radicandHeight * 0.4f) + smallMargin;

            if (reserve > degreeBaseline) {
                this.m = radicandBaseline + smallMargin;
                b.y = radicandHeight + smallMargin;
                float span = tickSpan(paint, b.y);
                degreeVisual.setPosition(0.0f, reserve - degreeBaseline);
                if (radicandVisual != null) {
                    radicandVisual.setPosition(degreeVisual.b.x + span, smallMargin);
                }
                b.x = radicandWidth + span + degreeVisual.b.x + (b.y * 0.1f);
            } else {
                this.m = radicandBaseline + degreeBaseline - reserve + smallMargin;
                b.y = radicandHeight + degreeBaseline - reserve + smallMargin;
                float span = tickSpan(paint, b.y);
                degreeVisual.setPosition(0.0f, 0.0f);
                if (radicandVisual != null) {
                    radicandVisual.setPosition(degreeVisual.b.x + span, degreeBaseline - reserve + smallMargin);
                }
                b.x = radicandWidth + span + degreeVisual.b.x + (b.y * 0.1f);
            }
        } else {
            this.m = radicandBaseline + smallMargin;
            b.y = radicandHeight + smallMargin;
            float span = tickSpan(paint, b.y);
            if (radicandVisual != null) {
                radicandVisual.setPosition(span, smallMargin);
            }
            b.x = radicandWidth + span + (b.y * 0.1f);
        }
    }

    /** Ported from C0102Vh's own {@code HiPER(Canvas, String)} draw method -- see class doc. */
    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        float radicandHeight = radicandVisual != null ? radicandVisual.b.y : 0.0f;
        float radicandTopY = radicandVisual != null ? radicandVisual.HiPER.y : 0.0f;
        float fM = mUnit(paint, b.y);
        float f2 = degreeVisual != null ? degreeVisual.b.x - (3.0f * fM) : 0.0f;
        float f3 = (radicandHeight * 0.5f) + radicandTopY;
        // Same thickness as FractionVisual's own bar: paint.measureText(" ") * 0.3f.
        float fK2 = paint.measureText(" ") * 0.3f;
        float f5 = 2.0f * fK2;
        float fB = (((b.y - this.m) / 3.0f) * 2.0f) + this.m;

        PointF p0 = new PointF(f2, f3);
        PointF p1 = new PointF((5.0f * fM) + f2, f3 - (2.0f * fM));
        PointF p2 = new PointF((12.0f * fM) + f2, fB);
        PointF p3 = new PointF((22.0f * fM) + f2, f5);
        PointF p4 = new PointF(b.x, f5);

        PointF[] pointFArr = new PointF[]{p0, p1, p2, p3, p4};

        float f4 = 2.0f;
        float[] fArr = new float[]{fK2, 2.0f * fK2, fK2, fK2};
        float[] fArr2 = new float[4];
        float[] fArr3 = new float[4];
        float[] fArr4 = new float[4];
        float[] fArr5 = new float[4];

        for (int i6 = 0; i6 < 4; i6++) {
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
    // either.

    @Override
    public CursorPointer hitTest(PointF point, Paint basePaint) {
        // Mirrors PowerVisual.hitTest's shape: degree (if present) takes priority on its own
        // horizontal span, then radicand, matching reading order (degree, then the radical).
        if (degreeVisual != null) {
            float degLeft = degreeVisual.HiPER.x;
            float degRight = degLeft + degreeVisual.b.x;
            if (point.x >= degLeft && point.x <= degRight) {
                PointF localPoint = new PointF(point.x - degLeft, point.y - degreeVisual.HiPER.y);
                CursorPointer hit = degreeVisual.hitTest(localPoint, basePaint);
                if (hit != null) return hit;
                return new CursorPointer(sqrtNode.degree, 0);
            }
        }
        if (radicandVisual != null) {
            PointF localPoint = new PointF(point.x - radicandVisual.HiPER.x, point.y - radicandVisual.HiPER.y);
            CursorPointer hit = radicandVisual.hitTest(localPoint, basePaint);
            if (hit != null) return hit;
            return new CursorPointer(sqrtNode.radicand, 0);
        }
        return new CursorPointer(sqrtNode, 0);
    }
}
