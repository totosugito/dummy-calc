package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.OperatorNode;

/**
 * Visual renderer for operators (+, −, ×, ÷, %, etc.).
 * Faithful implementation of android.core.SG from HiPER Calc.
 */
public class OperatorVisual extends MathVisual {
    public final OperatorNode operatorNode;

    public OperatorVisual(OperatorNode node) {
        super(node);
        this.operatorNode = node;
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        String symbol = " " + operatorNode.getSymbol() + " ";
        b.x = paint.measureText(symbol);
        b.y = -paint.ascent() + paint.descent();
        m = -paint.ascent();
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        Paint paint = new Paint(basePaint);
        paint.setTextSize(basePaint.getTextSize() * D);

        String symbol = " " + operatorNode.getSymbol() + " ";
        canvas.drawText(symbol, 0, m, paint);
    }

    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        if (index == 0) {
            return new PointF(0, m);
        }
        return new PointF(b.x, m);
    }

    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        if (point.x < b.x / 2.0f) {
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(operatorNode, 0);
        }
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(operatorNode, 1);
    }
}
