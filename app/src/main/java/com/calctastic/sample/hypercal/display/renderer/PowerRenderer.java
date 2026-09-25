package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.display.model.MathToken;

/**
 * PowerRenderer:
 * Mengimplementasikan rendering pangkat/eksponen berdasarkan android.core.C0349xH.java dan QA.java.
 * Menampilkan kotak slot kosong (placeholder box) pada eksponen saat eksponen belum diisi.
 */
public class PowerRenderer implements MathTokenRenderer {

    @Override
    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        float baseW = 0f;
        for (MathToken c : token.children) {
            baseW += ctx.registry.measureWidth(c, textSize, ctx);
        }

        float expSize = textSize * 0.7f;
        ctx.textPaint.setTextSize(expSize);
        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);

        float expW = 0f;
        for (MathToken c : token.secondaryChildren) {
            expW += ctx.registry.measureWidth(c, expSize, ctx);
        }
        if (expW == 0f) {
            expW = emptyBoxW;
        }

        return baseW + expW + (4f * ctx.density);
    }

    @Override
    public MathBoxMetrics measureMetrics(MathToken token, float textSize, RenderContext ctx) {
        float baseW = 0f;
        float baseH = 0f;
        float baseAscent = 0f;
        for (MathToken c : token.children) {
            MathBoxMetrics m = ctx.registry.measureMetrics(c, textSize, ctx);
            baseW += m.width;
            baseH = Math.max(baseH, m.height);
            baseAscent = Math.max(baseAscent, m.ascent);
        }
        if (baseH == 0f) {
            ctx.textPaint.setTextSize(textSize);
            Paint.FontMetrics fm = ctx.textPaint.getFontMetrics();
            baseAscent = -fm.ascent;
            baseH = -fm.ascent + fm.descent;
        }

        float expSize = textSize * 0.7f;
        ctx.textPaint.setTextSize(expSize);
        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);
        float emptyBoxH = PlaceholderBoxRenderer.measureEmptyBoxHeight(ctx.textPaint, ctx.density);

        float expW = 0f;
        float expH = 0f;
        float expAscent = 0f;
        if (token.secondaryChildren.isEmpty()) {
            expW = emptyBoxW;
            expH = emptyBoxH;
            expAscent = emptyBoxH / 2f;
        } else {
            for (MathToken c : token.secondaryChildren) {
                MathBoxMetrics m = ctx.registry.measureMetrics(c, expSize, ctx);
                expW += m.width;
                expH = Math.max(expH, m.height);
                expAscent = Math.max(expAscent, m.ascent);
            }
        }

        // C0311tf.java line 83-106
        float f5 = 0.5f * baseAscent;
        float totalAscent;
        float totalH;
        if (f5 > expAscent) {
            totalAscent = baseAscent;
            totalH = Math.max(baseH, (expH - expAscent) + f5);
        } else {
            totalAscent = (baseAscent + expAscent) - f5;
            float f7 = (baseH + expAscent) - f5;
            totalH = Math.max(f7, expH);
        }
        float totalW = baseW + expW + (4f * ctx.density);
        return new MathBoxMetrics(totalW, totalH, totalAscent);
    }

    @Override
    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        float curX = x;
        for (MathToken c : token.children) {
            curX = ctx.registry.draw(canvas, c, curX, baselineY, textSize, -1, ctx);
        }

        float expStartX = curX;
        float expSize = textSize * 0.7f;
        ctx.textPaint.setTextSize(expSize);
        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);
        float emptyBoxH = PlaceholderBoxRenderer.measureEmptyBoxHeight(ctx.textPaint, ctx.density);

        float expY = baselineY - (textSize * 0.45f);
        boolean isExpFocused = (ctx.targetFocusToken != null ? (ctx.targetFocusToken == token) : (ctx.containerFocusIndex == tokenIndex)) && ctx.containerInSecondary;

        if (token.secondaryChildren.isEmpty()) {
            // Render kotak slot kosong pada eksponen pangkat (C0357yG.java)
            float boxTop = expY - (emptyBoxH * 0.65f);
            RectF boxRect = new RectF(curX, boxTop, curX + emptyBoxW, boxTop + emptyBoxH);
            PlaceholderBoxRenderer.drawPlaceholderBox(canvas, boxRect, isExpFocused, ctx);

            if (isExpFocused) {
                // Posisi tepat di tengah box dengan padding vertikal (kursor lebih kecil dari box)
                ctx.cursorDrawPosition.set(boxRect.centerX(), boxRect.centerY());
                ctx.cursorHeight = emptyBoxH * 0.6f;
            }
            curX += emptyBoxW;
        } else {
            if (isExpFocused && ctx.containerSubCursor == 0) {
                ctx.cursorDrawPosition.set(curX, expY);
                ctx.cursorHeight = expSize * 1.1f;
            }
            for (int ci = 0; ci < token.secondaryChildren.size(); ci++) {
                MathToken expChild = token.secondaryChildren.get(ci);
                curX = ctx.registry.draw(canvas, expChild, curX, expY, expSize, -1, ctx);
                if (isExpFocused && ctx.containerSubCursor == ci + 1) {
                    ctx.cursorDrawPosition.set(curX, expY);
                    ctx.cursorHeight = expSize * 1.1f;
                }
            }
        }

        // Catat HitBox eksponen
        if (ctx.containerHitBoxes != null) {
            RenderContext.ContainerHitBox hitBox = new RenderContext.ContainerHitBox();
            hitBox.tokenIndex = tokenIndex;
            hitBox.targetToken = token;
            hitBox.isSecondary = true;
            hitBox.bounds.set(expStartX, expY - emptyBoxH, curX, expY + (emptyBoxH * 0.5f));
            hitBox.contentStartX = expStartX;
            hitBox.contentWidth = curX - expStartX;
            ctx.containerHitBoxes.add(hitBox);
        }

        return curX + (2f * ctx.density);
    }
}
