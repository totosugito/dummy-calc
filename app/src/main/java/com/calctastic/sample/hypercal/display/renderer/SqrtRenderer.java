package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import com.calctastic.sample.hypercal.display.model.MathToken;

/**
 * SqrtRenderer:
 * Mengimplementasikan rendering tanda akar berdasarkan android.core.C0119aE.java.
 */
public class SqrtRenderer implements MathTokenRenderer {

    @Override
    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        float innerW = 0f;
        for (MathToken c : token.children) {
            innerW += ctx.registry.measureWidth(c, textSize, ctx);
        }
        if (innerW == 0f) innerW = 16f * ctx.density;
        return innerW + (textSize * 0.6f) + (10f * ctx.density);
    }

    @Override
    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        float innerW = 0f;
        for (MathToken c : token.children) {
            innerW += ctx.registry.measureWidth(c, textSize, ctx);
        }
        if (innerW == 0f) innerW = 16f * ctx.density;

        float radicalW = textSize * 0.55f;
        float topY = baselineY - (textSize * 0.75f);
        float botY = baselineY + (textSize * 0.45f);
        float midY = baselineY + (textSize * 0.05f);

        Path rootPath = new Path();
        rootPath.moveTo(x, midY);
        rootPath.lineTo(x + radicalW * 0.35f, botY);
        rootPath.lineTo(x + radicalW, topY);
        rootPath.lineTo(x + radicalW + innerW + (6f * ctx.density), topY);

        Paint strokePaint = new Paint(ctx.mathAccentPaint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Math.max(2f * ctx.density, textSize * 0.055f));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(rootPath, strokePaint);

        float curX = x + radicalW + (4f * ctx.density);
        for (MathToken child : token.children) {
            curX = ctx.registry.draw(canvas, child, curX, baselineY, textSize, -1, ctx);
        }

        return x + radicalW + innerW + (10f * ctx.density);
    }
}
