package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.calctastic.sample.hypercal.display.model.MathToken;
import com.calctastic.sample.hypercal.display.view.HiPerThemeColors;

/**
 * PlaceholderBoxRenderer:
 * Mengimplementasikan visualisasi kotak slot kosong berdasarkan android.core.C0357yG.java & QA.java.
 */
public class PlaceholderBoxRenderer {

    /**
     * Menghitung lebar kotak kosong (C0357yG.java line 72: pointF.x = paint.measureText("0") * 1.2f).
     */
    public static float measureEmptyBoxWidth(Paint paint) {
        return paint.measureText("0") * 1.2f;
    }

    /**
     * Menghitung tinggi kotak kosong (C0357yG.java line 42-46).
     */
    public static float measureEmptyBoxHeight(Paint paint, float density) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (-fm.ascent + (0.9f * density)) + fm.descent;
    }

    /**
     * Menggambar kotak slot kosong (C0357yG.java line 205-218).
     */
    public static void drawPlaceholderBox(Canvas canvas, RectF boxRect, boolean isFocused, RenderContext ctx) {
        float digitZeroW = ctx.textPaint.measureText("0");
        float strokeWidth = Math.max(1.8f * ctx.density, digitZeroW * 0.1f);

        ctx.placeholderBoxPaint.setStrokeWidth(strokeWidth);
        ctx.placeholderBoxPaint.setStyle(Paint.Style.STROKE);
        ctx.placeholderBoxPaint.setColor(isFocused ?
                HiPerThemeColors.COLOR_PLACEHOLDER_ACTIVE_BOX : HiPerThemeColors.COLOR_PLACEHOLDER_BOX);

        canvas.drawRect(boxRect, ctx.placeholderBoxPaint);
    }
}
