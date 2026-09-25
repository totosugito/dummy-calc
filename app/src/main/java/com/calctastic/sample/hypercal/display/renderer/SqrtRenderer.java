package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.display.model.MathToken;

/**
 * SqrtRenderer:
 * Porting 100% dari kode asli HiPER (android.core.C0102Vh.java).
 *
 * Mengimplementasikan:
 * 1. mo63HiPER() -> Pengukuran ascent (m) dan height (b.y) otomatis berdasarkan anak di dalam akar
 * 2. m() & d() -> Ketebalan dan lebar kait akar proporsional ukuran font dan tinggi konten
 * 3. HiPER(Canvas canvas) -> Perhitungan 5 titik kontrol geometri radical (pointF3 s/d pointF7)
 *    dan poligon path generator asli HiPER
 * 4. QA / C0357yG -> Kotak placeholder jika slot akar masih kosong
 */
public class SqrtRenderer implements MathTokenRenderer {

    @Override
    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        return measureMetrics(token, textSize, ctx).width;
    }

    /**
     * Implementasi mo63HiPER() dari C0102Vh.java line 324-379.
     * Mengukur tinggi total (b.y), ascent (m), dan lebar (b.x).
     */
    @Override
    public MathBoxMetrics measureMetrics(MathToken token, float textSize, RenderContext ctx) {
        ctx.textPaint.setTextSize(textSize);
        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);
        float emptyBoxH = PlaceholderBoxRenderer.measureEmptyBoxHeight(ctx.textPaint, ctx.density);

        // HiPER mo63HiPER: float fMeasureText = paint.measureText(" ") * 0.6f;
        float fMeasureText = ctx.textPaint.measureText(" ") * 0.6f;

        float innerW = 0f;
        float innerH = 0f;
        float innerAscent = 0f;

        if (token.children.isEmpty()) {
            innerW = emptyBoxW;
            innerH = emptyBoxH;
            innerAscent = emptyBoxH * 0.5f;
        } else {
            for (MathToken c : token.children) {
                MathBoxMetrics m = ctx.registry.measureMetrics(c, textSize, ctx);
                innerW += m.width;
                innerH = Math.max(innerH, m.height);
                innerAscent = Math.max(innerAscent, m.ascent);
            }
        }

        // C0102Vh.java line 372-378:
        // this.m = f + fMeasureText;
        // pointF.y = K() + fMeasureText;
        // float fD2 = d();
        // pointF.x = f2 + fD2 + (pointF.y * 0.1f);
        float rootAscent = innerAscent + fMeasureText;
        float rootHeight = innerH + fMeasureText;

        float fM = calcM(ctx.textPaint, rootHeight);
        float fD2 = calcD(ctx.textPaint, fM);

        float rootWidth = innerW + fD2 + (rootHeight * 0.1f);
        return new MathBoxMetrics(rootWidth, rootHeight, rootAscent);
    }

    /**
     * Hitung konstanta ketebalan m() dari C0102Vh.java line 310-320.
     */
    private float calcM(Paint paint, float rootHeight) {
        float fM = -paint.ascent() + paint.descent();
        float fMeasureText = (paint.measureText(" ") * 0.6f) + fM;
        float f = (((rootHeight - fMeasureText) * 0.15f) / fMeasureText) + 1.0f;
        return Math.min(1.6f, Math.max(1.0f, f)) * (paint.measureText(" ") / 10.0f);
    }

    /**
     * Hitung jarak horizontal kait radical d() dari C0102Vh.java line 291-296.
     */
    private float calcD(Paint paint, float fM) {
        float spaceW = paint.measureText(" ");
        return ((fM - (spaceW / 10.0f)) * 22.0f) + (2.7f * spaceW);
    }

    @Override
    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        ctx.textPaint.setTextSize(textSize);
        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);
        float emptyBoxH = PlaceholderBoxRenderer.measureEmptyBoxHeight(ctx.textPaint, ctx.density);

        float innerW = 0f;
        float innerH = 0f;
        float innerAscent = 0f;

        if (token.children.isEmpty()) {
            innerW = emptyBoxW;
            innerH = emptyBoxH;
            innerAscent = emptyBoxH * 0.5f;
        } else {
            for (MathToken c : token.children) {
                MathBoxMetrics m = ctx.registry.measureMetrics(c, textSize, ctx);
                innerW += m.width;
                innerH = Math.max(innerH, m.height);
                innerAscent = Math.max(innerAscent, m.ascent);
            }
        }

        float fMeasureText = ctx.textPaint.measureText(" ") * 0.6f;
        float rootAscent = innerAscent + fMeasureText;
        float rootHeight = innerH + fMeasureText;

        // Koordinat batas atas (topY) dan batas bawah (botY) dari baseline
        float topBoundY = baselineY - rootAscent;

        float fK = innerH;
        float fM = calcM(ctx.textPaint, rootHeight);
        float fD = calcD(ctx.textPaint, fM);

        // Nilai skala garis ketebalan HiPER C0102Vh.java line 65-68
        float fK2 = textSize * 0.12f;
        float f4 = 2.0f;
        float f5 = topBoundY + (1.5f * fK2) + (fK2 / 2.0f); // Garis horizontal atap akar
        float fB = topBoundY + rootHeight; // Titik terbawah kait akar

        // Titik awal kait radical (C0102Vh line 64)
        float f3 = topBoundY + (fK * 0.5f) + fMeasureText;
        float f2 = x;

        // 5 Titik Kontrol Radical HiPER C0102Vh.java line 69-76
        PointF pointF3 = new PointF(f2, f3);
        PointF pointF4 = new PointF((5.0f * fM) + f2, f3 - (2.0f * fM));
        PointF pointF5 = new PointF((12.0f * fM) + f2, fB);
        PointF pointF6 = new PointF((fM * 22.0f) + f2, f5);
        PointF pointF7 = new PointF(x + fD + innerW + (rootHeight * 0.1f), f5);

        PointF[] pointFArr = {pointF3, pointF4, pointF5, pointF6, pointF7};
        float[] fArr = {fK2, 2.0f * fK2, fK2, fK2};

        // Algoritma penghasil poligon radical C0102Vh.java line 78-245
        float[] fArr2 = new float[4];
        float[] fArr3 = new float[4];
        float[] fArr4 = new float[4];
        float[] fArr5 = new float[4];

        for (int i6 = 0; i6 < 4; i6++) {
            PointF pNext = pointFArr[i6 + 1];
            PointF pCur = pointFArr[i6];
            float dx = pNext.x - pCur.x;
            float dy = pNext.y - pCur.y;
            float dist = (float) Math.sqrt((dy * dy) + (dx * dx));
            if (dist == 0f) dist = 0.0001f;
            fArr2[i6] = dx / dist;
            fArr3[i6] = dy / dist;
            fArr4[i6] = ((-dy / dist) * fArr[i6]) / f4;
            fArr5[i6] = ((dx / dist) * fArr[i6]) / f4;
        }

        Path path = new Path();
        path.setFillType(Path.FillType.WINDING);

        for (int i10 = 0; i10 < 4; i10++) {
            PointF p0 = pointFArr[i10];
            PointF p1 = pointFArr[i10 + 1];
            float ox = fArr4[i10];
            float oy = fArr5[i10];

            float[] segX = { p0.x + ox, p1.x + ox, p1.x - ox, p0.x - ox };
            float[] segY = { p0.y + oy, p1.y + oy, p1.y - oy, p0.y - oy };
            appendPolygon(path, segX, segY);
        }

        // Sambungan antar ruas garis C0102Vh line 146-245
        for (int i14 = 1; i14 < 4; i14++) {
            PointF pCorner = pointFArr[i14];
            int prev = i14 - 1;
            float sign = ((fArr2[prev] * fArr3[i14]) - (fArr3[prev] * fArr2[i14]) > 0.0f) ? -1.0f : 1.0f;
            float f21 = pCorner.x;
            float f22 = (fArr4[prev] * sign) + f21;
            float f23 = pCorner.y;
            float f24 = (fArr5[prev] * sign) + f23;
            float f25 = (fArr4[i14] * sign) + f21;
            float f26 = (sign * fArr5[i14]) + f23;
            float fMaxCorner = Math.max(fArr[prev], fArr[i14]);

            float dCross = (fArr2[prev] * fArr3[i14]) - (fArr3[prev] * fArr2[i14]);
            PointF intersect = null;
            if (Math.abs(dCross) >= 0.01f) {
                float t = (((f25 - f22) * fArr3[i14]) - ((f26 - f24) * fArr2[i14])) / dCross;
                float ix = (fArr2[prev] * t) + f22;
                float iy = (t * fArr3[prev]) + f24;
                float diffX = ix - pCorner.x;
                float diffY = iy - pCorner.y;
                if ((diffX * diffX) + (diffY * diffY) <= fMaxCorner * fMaxCorner) {
                    intersect = new PointF(ix, iy);
                }
            }

            if (intersect != null) {
                float[] jX = { pCorner.x, f22, intersect.x, f25 };
                float[] jY = { pCorner.y, f24, intersect.y, f26 };
                appendPolygon(path, jX, jY);
            } else {
                float[] jX = { pCorner.x, f22, f25 };
                float[] jY = { pCorner.y, f24, f26 };
                appendPolygon(path, jX, jY);
            }
        }

        Paint fillPaint = new Paint(ctx.mathAccentPaint);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        canvas.drawPath(path, fillPaint);

        // Posisi mulai konten anak di dalam akar (C0102Vh line 358 & 376)
        float childStartX = x + fD;
        float childBaselineY = topBoundY + fMeasureText + innerAscent;

        boolean isThisFocused = (ctx.targetFocusToken != null ? (ctx.targetFocusToken == token) : (ctx.containerFocusIndex == tokenIndex)) && !ctx.containerInSecondary;

        if (token.children.isEmpty()) {
            // Gambar kotak slot kosong
            float marginBelowBar = 2.5f * ctx.density;
            float boxTop = f5 + marginBelowBar;
            RectF boxRect = new RectF(childStartX, boxTop, childStartX + emptyBoxW, boxTop + emptyBoxH);
            PlaceholderBoxRenderer.drawPlaceholderBox(canvas, boxRect, isThisFocused, ctx);

            if (isThisFocused) {
                ctx.cursorDrawPosition.set(boxRect.centerX(), boxRect.centerY());
                ctx.cursorHeight = emptyBoxH * 0.6f;
            }
        } else {
            if (isThisFocused && ctx.containerSubCursor == 0) {
                ctx.cursorDrawPosition.set(childStartX, childBaselineY);
                ctx.cursorHeight = innerH;
            }

            float curX = childStartX;
            for (int ci = 0; ci < token.children.size(); ci++) {
                MathToken child = token.children.get(ci);
                curX = ctx.registry.draw(canvas, child, curX, childBaselineY, textSize, -1, ctx);
                if (isThisFocused && ctx.containerSubCursor == ci + 1) {
                    ctx.cursorDrawPosition.set(curX, childBaselineY);
                    ctx.cursorHeight = innerH;
                }
            }
        }

        // Catat HitBox untuk deteksi tap selection
        if (ctx.containerHitBoxes != null) {
            RenderContext.ContainerHitBox hitBox = new RenderContext.ContainerHitBox();
            hitBox.tokenIndex = tokenIndex;
            hitBox.targetToken = token;
            hitBox.isSecondary = false;
            hitBox.bounds.set(x, topBoundY, childStartX + innerW + (rootHeight * 0.1f), topBoundY + rootHeight);
            hitBox.contentStartX = childStartX;
            hitBox.contentWidth = innerW;
            ctx.containerHitBoxes.add(hitBox);
        }

        return childStartX + innerW + (rootHeight * 0.1f);
    }

    /**
     * Helper poligon C0102Vh.java line 381-404: HiPER(Path path, float[] fArr, float[] fArr2)
     */
    private static void appendPolygon(Path path, float[] xs, float[] ys) {
        int length = xs.length;
        float area = 0.0f;
        for (int i = 0; i < length; i++) {
            int next = (i + 1) % length;
            area += (xs[i] * ys[next]) - (xs[next] * ys[i]);
        }
        if (area >= 0.0f) {
            path.moveTo(xs[0], ys[0]);
            for (int i = 1; i < length; i++) {
                path.lineTo(xs[i], ys[i]);
            }
        } else {
            path.moveTo(xs[length - 1], ys[length - 1]);
            for (int i = length - 2; i >= 0; i--) {
                path.lineTo(xs[i], ys[i]);
            }
        }
        path.close();
    }
}
