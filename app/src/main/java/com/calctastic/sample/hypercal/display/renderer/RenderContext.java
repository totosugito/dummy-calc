package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.display.model.MathToken;

import java.util.List;

/**
 * Context yang dibagikan saat merender pohon token ekspresi (mirip C0150dI di HiPER).
 */
public class RenderContext {
    public Paint textPaint;
    public Paint mathAccentPaint;
    public Paint placeholderBoxPaint;
    public float density;

    // State kursor
    public int fractionFocusIndex = -1;
    public boolean fractionInDenominator = false;
    public int fractionSubCursor = 0;

    // General Container State (Fraction, Sqrt, Power)
    public int containerFocusIndex = -1;
    public boolean containerInSecondary = false;
    public int containerSubCursor = 0;

    public PointF cursorDrawPosition = new PointF();
    public float cursorHeight = 0f;

    // Hitbox pecahan & container
    public List<FractionHitBox> hitBoxes;
    public TokenRendererRegistry registry;

    public static class FractionHitBox {
        public int tokenIndex;
        public android.graphics.RectF numBox = new android.graphics.RectF();
        public android.graphics.RectF denBox = new android.graphics.RectF();
        public float numStartX;
        public float numWidth;
        public float denStartX;
        public float denWidth;
    }

    public static class ContainerHitBox {
        public int tokenIndex;
        public boolean isSecondary;
        public android.graphics.RectF bounds = new android.graphics.RectF();
        public float contentStartX;
        public float contentWidth;
    }
    public List<ContainerHitBox> containerHitBoxes;
}
