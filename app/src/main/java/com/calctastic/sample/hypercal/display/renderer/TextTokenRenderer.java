package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.calctastic.sample.hypercal.display.model.MathToken;
import com.calctastic.sample.hypercal.display.view.HiPerThemeColors;

/**
 * TextTokenRenderer:
 * Mengimplementasikan rendering angka dan operator biasa berdasarkan C0329vH.java.
 */
public class TextTokenRenderer implements MathTokenRenderer {

    @Override
    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        if (token == null || token.text == null) return 0f;
        ctx.textPaint.setTextSize(textSize);

        if (token.type == MathToken.Type.OPERATOR) {
            return ctx.textPaint.measureText(" " + token.text + " ");
        } else if (token.type == MathToken.Type.PAREN_GROUP) {
            float innerW = 0f;
            for (MathToken c : token.children) {
                innerW += ctx.registry.measureWidth(c, textSize, ctx);
            }
            return innerW + ctx.textPaint.measureText("()") + (4f * ctx.density);
        }
        return ctx.textPaint.measureText(token.text);
    }

    @Override
    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        ctx.textPaint.setTextSize(textSize);

        if (token.type == MathToken.Type.OPERATOR) {
            ctx.textPaint.setColor(HiPerThemeColors.COLOR_MATH_ACCENT);
            String display = " " + token.text + " ";
            float w = ctx.textPaint.measureText(display);
            Paint.FontMetrics fm = ctx.textPaint.getFontMetrics();
            float textY = baselineY - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(display, x, textY, ctx.textPaint);
            return x + w;
        }

        if (token.type == MathToken.Type.PAREN_GROUP) {
            ctx.textPaint.setColor(HiPerThemeColors.COLOR_EXPRESSION_TEXT);
            Paint.FontMetrics fm = ctx.textPaint.getFontMetrics();
            float textY = baselineY - (fm.ascent + fm.descent) / 2f;

            canvas.drawText("(", x, textY, ctx.textPaint);
            float curX = x + ctx.textPaint.measureText("(") + (2f * ctx.density);
            for (MathToken child : token.children) {
                curX = ctx.registry.draw(canvas, child, curX, baselineY, textSize, -1, ctx);
            }
            canvas.drawText(")", curX, textY, ctx.textPaint);
            return curX + ctx.textPaint.measureText(")") + (2f * ctx.density);
        }

        // Teks biasa / Angka
        ctx.textPaint.setColor(HiPerThemeColors.COLOR_EXPRESSION_TEXT);
        String display = token.text != null ? token.text : "";
        float w = ctx.textPaint.measureText(display);
        Paint.FontMetrics fm = ctx.textPaint.getFontMetrics();
        float textY = baselineY - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(display, x, textY, ctx.textPaint);
        return x + w;
    }
}
