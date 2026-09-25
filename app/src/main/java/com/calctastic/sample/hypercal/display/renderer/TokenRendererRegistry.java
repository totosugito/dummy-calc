package com.calctastic.sample.hypercal.display.renderer;

import android.graphics.Canvas;
import com.calctastic.sample.hypercal.display.model.MathToken;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry yang memetakan tipe token matematika ke renderernya masing-masing (mirip PH.java di HiPER).
 */
public class TokenRendererRegistry {

    private final Map<MathToken.Type, MathTokenRenderer> renderers = new EnumMap<>(MathToken.Type.class);
    private final TextTokenRenderer defaultTextRenderer = new TextTokenRenderer();

    public TokenRendererRegistry() {
        renderers.put(MathToken.Type.NUMBER, defaultTextRenderer);
        renderers.put(MathToken.Type.OPERATOR, defaultTextRenderer);
        renderers.put(MathToken.Type.PAREN_GROUP, defaultTextRenderer);
        renderers.put(MathToken.Type.FRACTION, new FractionRenderer());
        renderers.put(MathToken.Type.SQRT, new SqrtRenderer());
        renderers.put(MathToken.Type.POWER, new PowerRenderer());
    }

    public MathTokenRenderer getRenderer(MathToken.Type type) {
        MathTokenRenderer renderer = renderers.get(type);
        return renderer != null ? renderer : defaultTextRenderer;
    }

    public float measureWidth(MathToken token, float textSize, RenderContext ctx) {
        if (token == null) return 0f;
        return getRenderer(token.type).measureWidth(token, textSize, ctx);
    }

    public MathBoxMetrics measureMetrics(MathToken token, float textSize, RenderContext ctx) {
        if (token == null) return new MathBoxMetrics();
        return getRenderer(token.type).measureMetrics(token, textSize, ctx);
    }

    public float draw(Canvas canvas, MathToken token, float x, float baselineY, float textSize, int tokenIndex, RenderContext ctx) {
        if (token == null) return x;
        return getRenderer(token.type).draw(canvas, token, x, baselineY, textSize, tokenIndex, ctx);
    }
}
