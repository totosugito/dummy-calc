package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import com.calctastic.sample.hypercal.display.model.MathToken;

/**
 * PowerRenderer:
 * Mengimplementasikan rendering pangkat/eksponen berdasarkan android.core.C0349xH.java.
 */
public class PowerRenderer implements MathTokenRenderer {

    @Override
    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        float baseW = 0f;
        for (MathToken c : token.children) {
            baseW += ctx.registry.measureWidth(c, textSize, ctx);
        }
        float expW = 0f;
        for (MathToken c : token.secondaryChildren) {
            expW += ctx.registry.measureWidth(c, textSize * 0.7f, ctx);
        }
        return baseW + expW + (4f * ctx.density);
    }

    @Override
    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        float curX = x;
        for (MathToken c : token.children) {
            curX = ctx.registry.draw(canvas, c, curX, baselineY, textSize, -1, ctx);
        }

        float expSize = textSize * 0.7f;
        float expY = baselineY - (textSize * 0.5f);
        for (MathToken expChild : token.secondaryChildren) {
            curX = ctx.registry.draw(canvas, expChild, curX, expY, expSize, -1, ctx);
        }

        return curX;
    }
}
