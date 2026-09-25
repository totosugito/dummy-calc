package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import com.calctastic.sample.hypercal.display.model.MathToken;

/**
 * Interface dasar untuk setiap token visualizer (turunan konsep AbstractC0335wD di HiPER).
 */
public interface MathTokenRenderer {
    /**
     * Menghitung lebar token pada ukuran teks tertentu.
     */
    float measureWidth(MathToken token, float textSize, RenderContext ctx);

    /**
     * Menggambar token pada posisi (x, baselineY) dan mengembalikan koordinat X ujung kanan token.
     */
    float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx);
}
