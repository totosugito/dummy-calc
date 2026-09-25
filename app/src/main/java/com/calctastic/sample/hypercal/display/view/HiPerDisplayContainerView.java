package com.calctastic.sample.hypercal.display.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.calctastic.sample.hypercal.display.model.MathToken;

import java.util.List;

/**
 * HiPerDisplayContainerView:
 * Container komposit display Mode Expression HiPER:
 * 1. HiPerExpressionCanvasView: 2-line rendering (Expression line di atas, Result line di bawah).
 * 2. Baris indikator status di pojok kanan atas (DEG/RAD, FIX, M).
 */
public class HiPerDisplayContainerView extends FrameLayout {

    private HiPerExpressionCanvasView canvasView;
    private TextView modeIndicatorView;

    public HiPerDisplayContainerView(Context context) {
        super(context);
        init(context);
    }

    public HiPerDisplayContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HiPerDisplayContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(HiPerThemeColors.COLOR_DISPLAY_BG);

        float density = getResources().getDisplayMetrics().density;
        int paddingSide = (int) (16 * density);
        int paddingTop = (int) (24 * density);
        int paddingBottom = (int) (12 * density);

        // 1. Tambahkan Custom Canvas Formula View (Mode Expression 2-Line)
        canvasView = new HiPerExpressionCanvasView(context);
        LayoutParams canvasLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        canvasView.setLayoutParams(canvasLp);
        canvasView.setPadding(paddingSide, paddingTop, paddingSide, paddingBottom);
        addView(canvasView);

        // 2. Baris Indikator Mode di Pojok Kanan Atas (DEG, SCI, etc.)
        LinearLayout statusLayout = new LinearLayout(context);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        LayoutParams statusLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        statusLp.gravity = Gravity.TOP | Gravity.RIGHT;
        statusLp.topMargin = (int) (6 * density);
        statusLp.rightMargin = (int) (12 * density);

        modeIndicatorView = new TextView(context);
        modeIndicatorView.setText("DEG");
        modeIndicatorView.setTextSize(11f);
        modeIndicatorView.setTextColor(HiPerThemeColors.COLOR_STATUS_ACTIVE);
        statusLayout.addView(modeIndicatorView);

        addView(statusLayout, statusLp);
    }

    public HiPerExpressionCanvasView getCanvasView() {
        return canvasView;
    }

    public void setAngleMode(String mode) {
        if (modeIndicatorView != null) {
            modeIndicatorView.setText(mode);
        }
    }

    public void setResultPreview(String result) {
        if (canvasView != null) {
            canvasView.setResultText(result);
        }
    }

    // Proxy helper methods
    public void appendToken(MathToken token) {
        canvasView.appendToken(token);
    }

    public void appendText(String text) {
        canvasView.appendText(text);
    }

    public void appendOperator(String op) {
        canvasView.appendOperator(op);
    }

    public void appendFraction() {
        canvasView.appendFraction();
    }

    public void appendSqrt() {
        canvasView.appendSqrt();
    }

    public void appendPower() {
        canvasView.appendPower();
    }

    public void deleteBackward() {
        canvasView.deleteBackward();
    }

    public void clearAll() {
        canvasView.clearAll();
    }

    public void moveCursorLeft() {
        canvasView.moveCursorLeft();
    }

    public void moveCursorRight() {
        canvasView.moveCursorRight();
    }

    public List<MathToken> getTokens() {
        return canvasView.getTokens();
    }
}
