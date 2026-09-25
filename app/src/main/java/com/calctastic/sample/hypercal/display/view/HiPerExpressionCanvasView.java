package com.calctastic.sample.hypercal.display.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.calctastic.sample.hypercal.display.model.MathFormulaDocument;
import com.calctastic.sample.hypercal.display.model.MathToken;
import com.calctastic.sample.hypercal.display.renderer.RenderContext;
import com.calctastic.sample.hypercal.display.renderer.TokenRendererRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * HiPerExpressionCanvasView:
 * View kanvas ekspresi formula 2D HiPER Calculator (mirip C0281qH.java & BE.java).
 * Sangat ramping (~180 baris) karena hanya bertanggung jawab pada:
 * 1. Viewport kanvas & scroll gesture sentuh.
 * 2. Menggambar garis kursor vertikal berkedip.
 * 3. Menghubungkan MathFormulaDocument (data) ke TokenRendererRegistry (AST renderer).
 */
public class HiPerExpressionCanvasView extends View implements MathFormulaDocument.OnDocumentChangeListener {

    private final MathFormulaDocument document = new MathFormulaDocument();
    private final TokenRendererRegistry registry = new TokenRendererRegistry();
    private final List<RenderContext.FractionHitBox> fractionHitBoxes = new ArrayList<>();
    private final RenderContext renderContext = new RenderContext();

    private Paint textPaint;
    private Paint resultTextPaint;
    private Paint mathAccentPaint;
    private Paint placeholderBoxPaint;
    private Paint cursorPaint;

    private float baseTextSize;
    private float scrollOffsetX = 0f;
    private float totalContentWidth = 0f;

    private boolean cursorVisible = true;
    private long lastBlinkTime = 0;
    private final PointF cursorDrawPosition = new PointF();
    private float cursorHeight = 0f;

    private String resultText = "";
    private GestureDetector gestureDetector;

    public HiPerExpressionCanvasView(Context context) {
        super(context);
        init();
    }

    public HiPerExpressionCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HiPerExpressionCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setFocusable(true);
        document.setOnDocumentChangeListener(this);

        float density = getResources().getDisplayMetrics().density;
        baseTextSize = 34f * density;

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(HiPerThemeColors.COLOR_EXPRESSION_TEXT);
        textPaint.setTextSize(baseTextSize);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        resultTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resultTextPaint.setColor(HiPerThemeColors.COLOR_EXPRESSION_TEXT);
        resultTextPaint.setTextSize(baseTextSize * 1.25f);
        resultTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        mathAccentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mathAccentPaint.setColor(HiPerThemeColors.COLOR_MATH_ACCENT);
        mathAccentPaint.setStyle(Paint.Style.FILL);

        placeholderBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        placeholderBoxPaint.setColor(HiPerThemeColors.COLOR_PLACEHOLDER_BOX);
        placeholderBoxPaint.setStyle(Paint.Style.STROKE);
        placeholderBoxPaint.setStrokeWidth(1.8f * density);

        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint.setColor(HiPerThemeColors.COLOR_CURSOR);
        cursorPaint.setStrokeWidth(2.5f * density);
        cursorPaint.setStyle(Paint.Style.STROKE);

        // Siapkan RenderContext
        renderContext.textPaint = textPaint;
        renderContext.mathAccentPaint = mathAccentPaint;
        renderContext.placeholderBoxPaint = placeholderBoxPaint;
        renderContext.density = density;
        renderContext.hitBoxes = fractionHitBoxes;
        renderContext.registry = registry;

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                scrollOffsetX -= distanceX;
                clampScroll();
                invalidate();
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                float startX = getPaddingLeft() + scrollOffsetX;
                document.handleTap(e.getX(), e.getY(), startX, registry, baseTextSize, renderContext, fractionHitBoxes);
                return true;
            }
        });
    }

    @Override
    public void onDocumentChanged() {
        resetCursorBlink();
        ensureCursorVisible();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = gestureDetector.onTouchEvent(event);
        return handled || super.onTouchEvent(event);
    }

    private void clampScroll() {
        float viewWidth = getWidth();
        if (totalContentWidth <= viewWidth) {
            scrollOffsetX = 0f;
        } else {
            float minScroll = viewWidth - totalContentWidth - 48f;
            if (scrollOffsetX < minScroll) scrollOffsetX = minScroll;
            if (scrollOffsetX > 0f) scrollOffsetX = 0f;
        }
    }

    public MathFormulaDocument getDocument() {
        return document;
    }

    public List<MathToken> getTokens() {
        return document.getTokens();
    }

    public void setResultText(String result) {
        this.resultText = result != null ? result : "";
        invalidate();
    }

    // Delegasi operasi formula ke document
    public void appendText(String text) { document.appendText(text); }
    public void appendOperator(String op) { document.appendOperator(op); }
    public void appendToken(MathToken token) { document.appendToken(token); }
    public void appendFraction() { document.appendFraction(); }
    public void appendSqrt() { document.appendSqrt(); }
    public void appendPower() { document.appendPower(); }
    public void deleteBackward() { document.deleteBackward(); }
    public void clearAll() { document.clearAll(); }
    public void moveCursorLeft() { document.moveCursorLeft(); }
    public void moveCursorRight() { document.moveCursorRight(); }

    private void resetCursorBlink() {
        cursorVisible = true;
        lastBlinkTime = SystemClock.uptimeMillis();
    }

    private void ensureCursorVisible() {
        float x = getPaddingLeft();
        List<MathToken> tokens = document.getTokens();
        for (int i = 0; i < document.getCursorIndex() && i < tokens.size(); i++) {
            x += registry.measureWidth(tokens.get(i), baseTextSize, renderContext);
        }

        float screenCursorX = x + scrollOffsetX;
        float viewWidth = getWidth();
        if (viewWidth > 0) {
            if (screenCursorX > viewWidth - 64f) {
                scrollOffsetX = (viewWidth - 64f) - x;
            } else if (screenCursorX < 64f) {
                scrollOffsetX = 64f - x;
                if (scrollOffsetX > 0f) scrollOffsetX = 0f;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewHeight = getHeight();
        int viewWidth = getWidth();
        float density = getResources().getDisplayMetrics().density;

        float exprBaselineY = viewHeight * 0.38f;
        float resultBaselineY = viewHeight * 0.82f;
        float startX = getPaddingLeft() + scrollOffsetX;

        long now = SystemClock.uptimeMillis();
        if (now - lastBlinkTime > 500) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = now;
        }

        fractionHitBoxes.clear();
        float curX = startX;
        cursorDrawPosition.set(curX, exprBaselineY);
        cursorHeight = baseTextSize * 1.15f;

        // Berikan state dokumen ke renderContext
        renderContext.fractionFocusIndex = document.getFractionFocusIndex();
        renderContext.fractionInDenominator = document.isFractionInDenominator();
        renderContext.fractionSubCursor = document.getFractionSubCursor();
        renderContext.cursorDrawPosition.set(curX, exprBaselineY);
        renderContext.cursorHeight = cursorHeight;

        // 1. Gambar Baris Atas: EXPRESSION LINE
        List<MathToken> tokens = document.getTokens();
        int cursorIdx = document.getCursorIndex();
        int fracFocus = document.getFractionFocusIndex();

        for (int i = 0; i < tokens.size(); i++) {
            if (fracFocus < 0 && i == cursorIdx) {
                cursorDrawPosition.set(curX, exprBaselineY);
                cursorHeight = baseTextSize * 1.15f;
            }
            curX = registry.draw(canvas, tokens.get(i), curX, exprBaselineY, baseTextSize, i, renderContext);
        }

        if (fracFocus < 0 && cursorIdx == tokens.size()) {
            cursorDrawPosition.set(curX, exprBaselineY);
            cursorHeight = baseTextSize * 1.15f;
        } else if (fracFocus >= 0) {
            cursorDrawPosition.set(renderContext.cursorDrawPosition);
            cursorHeight = renderContext.cursorHeight;
        }

        totalContentWidth = curX - (getPaddingLeft() + scrollOffsetX);

        // 2. Gambar Kursor Berkedip HiPER (Garis vertikal lurus)
        if (cursorVisible && isEnabled()) {
            float halfH = cursorHeight / 2f;
            float topY = cursorDrawPosition.y - halfH;
            float botY = cursorDrawPosition.y + halfH;
            canvas.drawLine(cursorDrawPosition.x, topY, cursorDrawPosition.x, botY, cursorPaint);
        }

        // 3. Gambar Baris Bawah: RESULT LINE
        if (resultText != null && !resultText.isEmpty()) {
            resultTextPaint.setColor(HiPerThemeColors.COLOR_EXPRESSION_TEXT);
            float resW = resultTextPaint.measureText(resultText);
            float resX = viewWidth - getPaddingRight() - resW - (12f * density);
            canvas.drawText(resultText, resX, resultBaselineY, resultTextPaint);
        }

        postInvalidateDelayed(100);
    }
}
