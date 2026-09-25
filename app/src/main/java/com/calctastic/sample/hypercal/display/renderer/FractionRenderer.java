package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.display.model.MathToken;

/**
 * FractionRenderer:
 * Mengimplementasikan rendering pecahan berdasarkan android.core.Qg.java.
 * Menangani pembilang, garis pembagi pecahan (fraction bar), penyebut, dan kotak slot kosong.
 */
public class FractionRenderer implements MathTokenRenderer {

    @Override
    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        float subSize = textSize * 0.8f;
        ctx.textPaint.setTextSize(subSize);

        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);

        float numW = 0f;
        for (MathToken c : token.children) {
            numW += ctx.registry.measureWidth(c, subSize, ctx);
        }
        if (numW == 0f) numW = emptyBoxW;

        float denW = 0f;
        for (MathToken c : token.secondaryChildren) {
            denW += ctx.registry.measureWidth(c, subSize, ctx);
        }
        if (denW == 0f) denW = emptyBoxW;

        float contentW = Math.max(numW, denW);
        return contentW + (16f * ctx.density);
    }

    @Override
    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        // Skala font pembilang & penyebut 0.8x (Qg.java line 37, 61)
        float subSize = textSize * 0.8f;
        ctx.textPaint.setTextSize(subSize);

        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);
        float emptyBoxH = PlaceholderBoxRenderer.measureEmptyBoxHeight(ctx.textPaint, ctx.density);

        float numW = 0f;
        for (MathToken c : token.children) {
            numW += ctx.registry.measureWidth(c, subSize, ctx);
        }
        float denW = 0f;
        for (MathToken c : token.secondaryChildren) {
            denW += ctx.registry.measureWidth(c, subSize, ctx);
        }

        float actualNumW = numW > 0 ? numW : emptyBoxW;
        float actualDenW = denW > 0 ? denW : emptyBoxW;

        // Lebar pecahan (Qg.java line 113)
        float contentW = Math.max(actualNumW, actualDenW);
        float paddingX = 8f * ctx.density;
        float fracW = contentW + (paddingX * 2f);

        // Fraction Bar (Qg.java line 121, 298)
        float spaceW = ctx.textPaint.measureText(" ");
        float barThickness = Math.max(2.2f * ctx.density, spaceW * 0.45f);
        float lineY = baselineY;
        float gapY = emptyBoxH * 0.75f;

        // Gambar Garis Pecahan
        canvas.drawRect(x, lineY - (barThickness / 2f), x + fracW, lineY + (barThickness / 2f), ctx.mathAccentPaint);

        boolean isThisFracFocused = (ctx.fractionFocusIndex == tokenIndex);

        // --- 1. PEMBILANG (KOTAK ATAS) ---
        float numY = lineY - gapY;
        float numStartX = x + (fracW - actualNumW) / 2f;

        if (token.children.isEmpty()) {
            float boxTop = numY - (emptyBoxH / 2f);
            RectF boxRect = new RectF(numStartX, boxTop, numStartX + emptyBoxW, boxTop + emptyBoxH);
            boolean isFocused = isThisFracFocused && !ctx.fractionInDenominator;
            PlaceholderBoxRenderer.drawPlaceholderBox(canvas, boxRect, isFocused, ctx);

            if (isFocused) {
                ctx.cursorDrawPosition.set(boxRect.centerX(), boxRect.centerY());
                ctx.cursorHeight = emptyBoxH * 0.6f;
            }
        } else {
            float numCurX = numStartX;
            if (isThisFracFocused && !ctx.fractionInDenominator && ctx.fractionSubCursor == 0) {
                ctx.cursorDrawPosition.set(numCurX, numY);
                ctx.cursorHeight = subSize * 1.1f;
            }
            for (int ci = 0; ci < token.children.size(); ci++) {
                MathToken c = token.children.get(ci);
                numCurX = ctx.registry.draw(canvas, c, numCurX, numY, subSize, -1, ctx);
                if (isThisFracFocused && !ctx.fractionInDenominator && ctx.fractionSubCursor == ci + 1) {
                    ctx.cursorDrawPosition.set(numCurX, numY);
                    ctx.cursorHeight = subSize * 1.1f;
                }
            }
        }

        // --- 2. PENYEBUT (KOTAK BAWAH) ---
        float denY = lineY + gapY;
        float denStartX = x + (fracW - actualDenW) / 2f;

        if (token.secondaryChildren.isEmpty()) {
            float boxTop = denY - (emptyBoxH / 2f);
            RectF boxRect = new RectF(denStartX, boxTop, denStartX + emptyBoxW, boxTop + emptyBoxH);
            boolean isFocused = isThisFracFocused && ctx.fractionInDenominator;
            PlaceholderBoxRenderer.drawPlaceholderBox(canvas, boxRect, isFocused, ctx);

            if (isFocused) {
                // Posisi tepat di tengah box dengan padding vertikal (kursor lebih kecil dari box)
                ctx.cursorDrawPosition.set(boxRect.centerX(), boxRect.centerY());
                ctx.cursorHeight = emptyBoxH * 0.6f;
            }
        } else {
            float denCurX = denStartX;
            if (isThisFracFocused && ctx.fractionInDenominator && ctx.fractionSubCursor == 0) {
                ctx.cursorDrawPosition.set(denCurX, denY);
                ctx.cursorHeight = subSize * 1.1f;
            }
            for (int ci = 0; ci < token.secondaryChildren.size(); ci++) {
                MathToken c = token.secondaryChildren.get(ci);
                denCurX = ctx.registry.draw(canvas, c, denCurX, denY, subSize, -1, ctx);
                if (isThisFracFocused && ctx.fractionInDenominator && ctx.fractionSubCursor == ci + 1) {
                    ctx.cursorDrawPosition.set(denCurX, denY);
                    ctx.cursorHeight = subSize * 1.1f;
                }
            }
        }

        // Catat HitBox untuk deteksi Tap (Qg.java line 180-200)
        if (ctx.hitBoxes != null) {
            RenderContext.FractionHitBox hitBox = new RenderContext.FractionHitBox();
            hitBox.tokenIndex = tokenIndex;
            float touchPad = 8f * ctx.density;
            hitBox.numBox.set(x - touchPad, lineY - (gapY * 2.2f), x + fracW + touchPad, lineY);
            hitBox.denBox.set(x - touchPad, lineY, x + fracW + touchPad, lineY + (gapY * 2.2f));
            hitBox.numStartX = numStartX;
            hitBox.numWidth = actualNumW;
            hitBox.denStartX = denStartX;
            hitBox.denWidth = actualDenW;
            ctx.hitBoxes.add(hitBox);
        }

        return x + fracW + (4f * ctx.density);
    }
}
