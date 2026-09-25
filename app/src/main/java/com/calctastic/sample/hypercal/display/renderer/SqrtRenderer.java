package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.display.model.MathToken;

/**
 * SqrtRenderer:
 * Mengimplementasikan rendering tanda akar berdasarkan android.core.C0102Vh.java dan C0357yG.java.
 * Jika argumen akar kosong (QA / empty slot), menampilkan kotak slot kosong (placeholder box).
 */
public class SqrtRenderer implements MathTokenRenderer {

    @Override
    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        ctx.textPaint.setTextSize(textSize);
        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);

        float innerW = 0f;
        for (MathToken c : token.children) {
            innerW += ctx.registry.measureWidth(c, textSize, ctx);
        }
        if (innerW == 0f) {
            innerW = emptyBoxW;
        }

        float radicalW = textSize * 0.55f;
        return radicalW + innerW + (10f * ctx.density);
    }

    @Override
    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        ctx.textPaint.setTextSize(textSize);
        float emptyBoxW = PlaceholderBoxRenderer.measureEmptyBoxWidth(ctx.textPaint);
        float emptyBoxH = PlaceholderBoxRenderer.measureEmptyBoxHeight(ctx.textPaint, ctx.density);

        float innerW = 0f;
        for (MathToken c : token.children) {
            innerW += ctx.registry.measureWidth(c, textSize, ctx);
        }
        float actualInnerW = innerW > 0 ? innerW : emptyBoxW;

        float radicalW = textSize * 0.55f;
        float topY = baselineY - (textSize * 0.75f);
        float botY = baselineY + (textSize * 0.45f);
        float midY = baselineY + (textSize * 0.05f);

        // Path simbol akar (radical mark) - C0102Vh.java
        Path rootPath = new Path();
        rootPath.moveTo(x, midY);
        rootPath.lineTo(x + radicalW * 0.35f, botY);
        rootPath.lineTo(x + radicalW, topY);
        rootPath.lineTo(x + radicalW + actualInnerW + (6f * ctx.density), topY);

        Paint strokePaint = new Paint(ctx.mathAccentPaint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Math.max(2f * ctx.density, textSize * 0.055f));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(rootPath, strokePaint);

        float curX = x + radicalW + (4f * ctx.density);
        boolean isThisFocused = (ctx.containerFocusIndex == tokenIndex && !ctx.containerInSecondary);

        if (token.children.isEmpty()) {
            // Gambar kotak slot kosong jika akar belum ada isinya (QA.java & C0357yG.java)
            float boxTop = baselineY - (emptyBoxH * 0.65f);
            RectF boxRect = new RectF(curX, boxTop, curX + emptyBoxW, boxTop + emptyBoxH);
            PlaceholderBoxRenderer.drawPlaceholderBox(canvas, boxRect, isThisFocused, ctx);

            if (isThisFocused) {
                ctx.cursorDrawPosition.set(boxRect.centerX(), baselineY);
                ctx.cursorHeight = emptyBoxH * 0.8f;
            }
        } else {
            // Jika ada isinya, render anak-anaknya dan posisi kursor di dalamnya
            if (isThisFocused && ctx.containerSubCursor == 0) {
                ctx.cursorDrawPosition.set(curX, baselineY);
                ctx.cursorHeight = textSize * 1.1f;
            }

            for (int ci = 0; ci < token.children.size(); ci++) {
                MathToken child = token.children.get(ci);
                curX = ctx.registry.draw(canvas, child, curX, baselineY, textSize, -1, ctx);
                if (isThisFocused && ctx.containerSubCursor == ci + 1) {
                    ctx.cursorDrawPosition.set(curX, baselineY);
                    ctx.cursorHeight = textSize * 1.1f;
                }
            }
        }

        // Catat HitBox untuk tap selection
        if (ctx.containerHitBoxes != null) {
            RenderContext.ContainerHitBox hitBox = new RenderContext.ContainerHitBox();
            hitBox.tokenIndex = tokenIndex;
            hitBox.isSecondary = false;
            hitBox.bounds.set(x + radicalW, topY, x + radicalW + actualInnerW + (6f * ctx.density), botY);
            hitBox.contentStartX = x + radicalW + (4f * ctx.density);
            hitBox.contentWidth = actualInnerW;
            ctx.containerHitBoxes.add(hitBox);
        }

        return x + radicalW + actualInnerW + (10f * ctx.density);
    }
}
