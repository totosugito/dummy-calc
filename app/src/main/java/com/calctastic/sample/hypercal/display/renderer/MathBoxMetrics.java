package com.calctastic.sample.hypercal.display.renderer;

/**
 * MathBoxMetrics:
 * Representasi dimensi kotak visual elemen matematika persis seperti di AbstractC0335wD HiPER.
 * - width (b.x): lebar total
 * - height (b.y): tinggi total
 * - ascent (m): jarak dari batas atas bounding box ke baseline teks
 * - descent: b.y - m (jarak dari baseline teks ke batas bawah)
 */
public class MathBoxMetrics {
    public float width;
    public float height;
    public float ascent;

    public MathBoxMetrics() {
        this(0f, 0f, 0f);
    }

    public MathBoxMetrics(float width, float height, float ascent) {
        this.width = width;
        this.height = height;
        this.ascent = ascent;
    }

    public float getDescent() {
        return height - ascent;
    }
}
