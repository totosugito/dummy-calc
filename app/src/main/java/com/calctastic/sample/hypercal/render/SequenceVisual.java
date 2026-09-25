package com.calctastic.sample.hypercal.render;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual renderer for linear horizontal sequence of tokens.
 * Faithful implementation of android.core.C0329vH from HiPER Calc.
 */
public class SequenceVisual extends MathVisual {
    public final SequenceNode sequenceNode;
    public final List<MathVisual> children = new ArrayList<>();

    public SequenceVisual(SequenceNode node) {
        super(node);
        this.sequenceNode = node;
    }

    public void addChild(MathVisual visual) {
        children.add(visual);
        visual.parent = this;
    }

    public void clear() {
        children.clear();
    }

    @Override
    public void calculateLayout(Paint basePaint) {
        float currentX = 0.0f;
        float maxAbove = 0.0f;
        float maxBelow = 0.0f;

        for (MathVisual child : children) {
            child.setScale(this.D);
            child.calculateLayout(basePaint);
            maxAbove = Math.max(maxAbove, child.m);
            maxBelow = Math.max(maxBelow, child.b.y - child.m);
        }

        this.m = maxAbove > 0 ? maxAbove : (-basePaint.ascent());
        float totalHeight = maxAbove + maxBelow;
        if (totalHeight == 0) {
            totalHeight = -basePaint.ascent() + basePaint.descent();
        }

        for (MathVisual child : children) {
            child.setPosition(currentX, this.m - child.m);
            currentX += child.b.x;
        }

        b.x = currentX;
        b.y = totalHeight;
    }

    @Override
    public void draw(Canvas canvas, Paint basePaint) {
        for (MathVisual child : children) {
            canvas.save();
            canvas.translate(child.HiPER.x, child.HiPER.y);
            child.draw(canvas, basePaint);
            canvas.restore();
        }
    }

    @Override
    public PointF getCursorPosition(int index, Paint basePaint) {
        if (children.isEmpty() || index <= 0) {
            return new PointF(0, m);
        }
        if (index >= children.size()) {
            MathVisual last = children.get(children.size() - 1);
            return new PointF(last.HiPER.x + last.b.x, m);
        }
        MathVisual target = children.get(index);
        return new PointF(target.HiPER.x, m);
    }

    /**
     * Exact hit testing logic from HiPER Calc C0329vH.java lines 1521-1580.
     * Evaluates midway boundaries f2 = ((curr.x + curr.w) + next.x) / 2.0f
     * and delegates to the child component.
     */
    @Override
    public com.calctastic.sample.hypercal.engine.model.CursorPointer hitTest(PointF point, Paint basePaint) {
        if (children.isEmpty()) {
            return new com.calctastic.sample.hypercal.engine.model.CursorPointer(sequenceNode, 0);
        }

        float f7 = Float.NEGATIVE_INFINITY;
        int iL = children.size();

        for (int i = 0; i < iL; i++) {
            MathVisual child = children.get(i);
            float f2;
            if (i < iL - 1) {
                MathVisual nextChild = children.get(i + 1);
                f2 = ((child.HiPER.x + child.b.x) + nextChild.HiPER.x) / 2.0f;
            } else {
                f2 = Float.POSITIVE_INFINITY;
            }

            if (point.x >= f7 && point.x < f2) {
                PointF localPoint = new PointF(point.x - child.HiPER.x, point.y - child.HiPER.y);
                com.calctastic.sample.hypercal.engine.model.CursorPointer hit = child.hitTest(localPoint, basePaint);
                if (hit != null) {
                    return hit;
                }
                return new com.calctastic.sample.hypercal.engine.model.CursorPointer(child.modelNode, 0);
            }
            f7 = f2;
        }

        MathVisual lastChild = children.get(children.size() - 1);
        return new com.calctastic.sample.hypercal.engine.model.CursorPointer(lastChild.modelNode, lastChild.modelNode != null ? lastChild.modelNode.getLength() : 0);
    }
}
