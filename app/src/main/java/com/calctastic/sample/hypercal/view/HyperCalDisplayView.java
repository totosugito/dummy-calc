package com.calctastic.sample.hypercal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.res.ResourcesCompat;
import com.calctastic.sample.R;
import com.calctastic.sample.hypercal.engine.model.CursorPointer;
import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import com.calctastic.sample.hypercal.render.MathVisual;
import com.calctastic.sample.hypercal.render.VisualTreeBuilder;

/**
 * Custom View for rendering mathematical expressions on Canvas.
 * 100% FAITHFUL TO HiPER Calc android.core.BE.java, UF.java & AbstractC0335wD.java:
 *
 * Implements:
 * - Font configuration matching AbstractC0060Ig.java & C0255nf.java:
 *   Typeface: Typeface.create("Arial", Typeface.NORMAL) (AbstractC0293re.java line 135)
 *   Text Size: screenScale * 14.0f, screenScale from C0332vh.onMeasure (see updatePaintSize)
 * - Visual tree layout calculation and baseline alignment
 * - Touch hit-testing matching BE.java onSingleTapUp & AbstractC0335wD.HiPER(PointF):
 *   Transforms touch point to visual root coordinates and locates exact target node & char offset
 * - Cursor rectangle geometry matching AbstractC0335wD.mo360HiPER() & UF.k(Canvas):
 *   cursorWidth = paint.measureText(" ") * 0.35f (ZD.Sc)
 *   cursorRect = [cx - width/2, top, cx + width/2, bottom]
 * - Blinking cursor handler and invalidation
 */
public class HyperCalDisplayView extends View {

    public interface OnCursorChangedListener {
        void onCursorChanged(CursorPointer pointer);
    }

    private SequenceNode expressionRoot;
    private CursorPointer cursorPointer;
    private OnCursorChangedListener cursorChangedListener;

    private Paint textPaint;
    private Paint cursorPaint;

    // Cache latest root visual and layout offset for accurate touch coordinate mapping
    private MathVisual currentRootVisual;
    private float lastStartX = 0.0f;
    private float lastStartY = 0.0f;

    private boolean cursorVisible = true;
    private final Handler blinkHandler = new Handler(Looper.getMainLooper());
    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            cursorVisible = !cursorVisible;
            invalidate();
            blinkHandler.postDelayed(this, 500);
        }
    };

    private GestureDetector gestureDetector;

    // HiPER Calc nominal base size from AbstractC0293re.java line 144: "100" -> 14.0f
    private static final float NOMINAL_BASE_SIZE = 14.0f;
    // HiPER keypad reference size in design units: C0341wd.mo344HiPER() with AbstractC0060Ig theme values
    //   x = max(("41" + "39_F") * 5, ("33" + "30_F") * 4) + "6" + "8" = max(255, 256) + 8 = 264
    //   y = ("42_F" + "40_F") * 3 + ("34_F" + "31_F") * 5 + "7" + "9" + "11" = 90 + 190 + 5 = 285
    // C0332vh.java line 772 then adds 2 * "1" to both axes.
    private static final float REF_KEYPAD_WIDTH = 266.0f;
    private static final float REF_KEYPAD_HEIGHT = 287.0f;
    // Theme padding "95" (AbstractC0060Ig line 58) and UF.E = 3.6 editor lines (UF.java line 78)
    private static final float DISPLAY_PADDING = 4.0f;
    private static final float EDITOR_LINES = 3.6f;

    public HyperCalDisplayView(Context context) {
        super(context);
        init();
    }

    public HyperCalDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HyperCalDisplayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /** Line height (-ascent + descent) of the Arial font at a nominal design size (scale 1.0). */
    private static float lineHeight(Typeface typeface, float size) {
        Paint p = new Paint();
        p.setTypeface(typeface);
        p.setTextSize(size);
        return -p.ascent() + p.descent();
    }

    /**
     * Reference display height M at scale 1.0, after GestureDetectorOnGestureListenerC0122aI.G():
     * 2 * "95" padding + header line ("102" = 8) + UF.d() (3.6 lines of "100" = 14)
     * + result line ("103" = 15) + status line ("102" = 8) + "95".
     * The header rect (m283HiPER) is approximated by one "102" line.
     */
    private static float referenceDisplayHeight(Typeface typeface) {
        return 2.0f * DISPLAY_PADDING
                + lineHeight(typeface, 8.0f)
                + EDITOR_LINES * lineHeight(typeface, NOMINAL_BASE_SIZE)
                + lineHeight(typeface, 15.0f)
                + lineHeight(typeface, 8.0f) + DISPLAY_PADDING;
    }

    /**
     * Dynamic screen scale faithful to C0332vh.onMeasure (lines 797-811) and Tg.HiPER(V.HiPER):
     *   f  = point.x / pointF.x
     *   f2 = point.y / (pointF.y + M), capped at 1.2f * f
     * Tg.HiPER(V.HiPER) returns f2 (field H), which AbstractC0335wD.k() multiplies with D.
     * point is the whole calculator area (activity content), not just this view.
     */
    private void updatePaintSize() {
        if (textPaint == null) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int areaWidth = dm.widthPixels;
        int areaHeight = dm.heightPixels;
        View content = getRootView() != null ? getRootView().findViewById(android.R.id.content) : null;
        if (content != null && content.getWidth() > 0 && content.getHeight() > 0) {
            areaWidth = content.getWidth();
            areaHeight = content.getHeight();
        }

        float f = areaWidth / REF_KEYPAD_WIDTH;
        float f2 = areaHeight / (REF_KEYPAD_HEIGHT + referenceDisplayHeight(textPaint.getTypeface()));
        float f3 = 1.2f * f;
        if (f2 > f3) {
            f2 = f3;
        }
        float screenScale = f2;

        // HiPER AbstractC0293re.java line 527: textSize = k() * c0215jD.c, k() = Tg.HiPER(V) * D
        textPaint.setTextSize(screenScale * NOMINAL_BASE_SIZE);
    }

    private void init() {
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);

        // HiPER Calc AbstractC0293re.java line 135: DC.HiPER("\\\u0014t\u0007q") -> "Arial"
        textPaint.setTypeface(Typeface.create("Arial", Typeface.NORMAL));

        updatePaintSize();

        // Cursor paint matching HiPER UF.k(Canvas): Style.FILL
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint.setColor(0xFF2196F3); // HiPER Cyan/Blue accent cursor
        cursorPaint.setStyle(Paint.Style.FILL);

        // Initial default expression
        expressionRoot = new SequenceNode();
        cursorPointer = new CursorPointer(expressionRoot, 0);

        // Setup touch detector matching BE.java lines 602-645 & 881-912
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                handleTapAt(e.getX(), e.getY());
                return true;
            }
        });

        blinkHandler.postDelayed(blinkRunnable, 500);
    }

    public void setOnCursorChangedListener(OnCursorChangedListener listener) {
        this.cursorChangedListener = listener;
    }

    public SequenceNode getExpressionRoot() {
        return expressionRoot;
    }

    public void setExpressionRoot(SequenceNode root) {
        this.expressionRoot = root;
        invalidate();
    }

    public CursorPointer getCursorPointer() {
        return cursorPointer;
    }

    public void setCursorPointer(CursorPointer pointer) {
        this.cursorPointer = pointer;
        if (pointer != null && pointer.node != null) {
            pointer.node.setCursorPosition(pointer.position);
        }
        cursorVisible = true;
        blinkHandler.removeCallbacks(blinkRunnable);
        blinkHandler.postDelayed(blinkRunnable, 500);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Hit testing tap coordinates matching BE.java lines 881-912:
     * PointF pointFL = L(new PointF(motionEvent.getX(), motionEvent.getY()));
     * C0157eA c0157eAHiPER = abstractC0335wD.HiPER(pointFL, false, false);
     */
    private void handleTapAt(float touchX, float touchY) {
        if (currentRootVisual == null) {
            return;
        }

        // Convert touch coordinates to visual root local coordinate space
        PointF localPoint = new PointF(touchX - lastStartX, touchY - lastStartY);

        CursorPointer hitPointer = currentRootVisual.hitTest(localPoint, textPaint);
        if (hitPointer != null) {
            setCursorPointer(hitPointer);
            if (cursorChangedListener != null) {
                cursorChangedListener.onCursorChanged(hitPointer);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) {
            updatePaintSize();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blinkHandler.removeCallbacks(blinkRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (expressionRoot == null) {
            return;
        }

        // Build visual tree
        currentRootVisual = VisualTreeBuilder.buildVisualTree(expressionRoot);
        if (currentRootVisual == null) {
            return;
        }

        // Calculate layout
        currentRootVisual.calculateLayout(textPaint);

        // Position on Canvas (HiPER padding & baseline calculation)
        float paddingLeft = 32f;
        float paddingRight = 32f;
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float startX = paddingLeft;
        if (currentRootVisual.b.x + paddingLeft + paddingRight < viewWidth) {
            startX = paddingLeft;
        } else {
            // Scroll to end if expression overflows display width
            startX = viewWidth - paddingRight - currentRootVisual.b.x;
        }

        // Baseline placement faithful to UF.java lines 452-474:
        // f3 = (-paint.ascent()) * 1.6f; baseline = max(f3, root.m),
        // pulled up when the part below the baseline would overflow the display height.
        float f3 = (-textPaint.ascent()) * 1.6f;
        float baselineY = Math.max(f3, currentRootVisual.m);
        float belowBaseline = currentRootVisual.b.y - currentRootVisual.m;
        if (belowBaseline > viewHeight - f3) {
            baselineY = Math.max(currentRootVisual.m, viewHeight - belowBaseline);
        }
        float startY = baselineY - currentRootVisual.m;

        lastStartX = startX;
        lastStartY = startY;

        currentRootVisual.setPosition(startX, startY);

        // Draw visual expression
        canvas.save();
        canvas.translate(startX, startY);
        currentRootVisual.draw(canvas, textPaint);
        canvas.restore();

        // Draw cursor caret if visible matching AbstractC0335wD.mo360HiPER() & UF.k(Canvas)
        if (cursorVisible && cursorPointer != null && cursorPointer.node != null) {
            RectF cursorRect = null;
            MathVisual targetVisual = findVisualForNode(currentRootVisual, cursorPointer.node);

            if (targetVisual != null) {
                // Exact calculation matching AbstractC0335wD.mo360HiPER()
                RectF localRect = targetVisual.getCursorRect(cursorPointer.position, textPaint);
                if (localRect != null) {
                    cursorRect = new RectF(localRect);
                    // Accumulate parent offsets up to root (Faithful to UF.m248L lines 367-382)
                    MathVisual curr = targetVisual;
                    while (curr != null) {
                        cursorRect.offset(curr.HiPER.x, curr.HiPER.y);
                        curr = curr.parent;
                    }
                }
            } else {
                float cx = startX + currentRootVisual.b.x;
                float cyBaseline = startY + currentRootVisual.m;
                float halfWidth = Math.max(3.0f, textPaint.measureText(" ") * 0.35f) / 2.0f;
                cursorRect = new RectF(cx - halfWidth, cyBaseline - (-textPaint.ascent()), cx + halfWidth, cyBaseline + textPaint.descent());
            }

            if (cursorRect != null) {
                // Filled rectangle cursor (UF.java line 643: canvas.drawRect(left, top, right, bottom, paint))
                canvas.drawRect(cursorRect, cursorPaint);
            }
        }
    }

    /**
     * Cursor target for ▲ / ▼, faithful to Qg.HiPER(PointF, Df) (down) and Qg.E(PointF, Df) (up):
     * walks up from the cursor to the nearest fraction where the cursor sits in the numerator (down)
     * or denominator (up), then hit-tests the opposite slot at the cursor x clamped by
     * ZD.ab (0.2f) * density on each side.
     */
    public CursorPointer findVerticalCursorTarget(boolean down) {
        if (currentRootVisual == null || cursorPointer == null || cursorPointer.node == null) {
            return null;
        }
        MathVisual curr = findVisualForNode(currentRootVisual, cursorPointer.node);
        if (curr == null) {
            return null;
        }
        PointF p = curr.getCursorPosition(cursorPointer.position, textPaint);
        if (p == null) {
            return null;
        }
        float x = p.x;
        float margin = 0.2f * getResources().getDisplayMetrics().density;
        while (curr.parent != null) {
            x += curr.HiPER.x; // now in parent coordinates
            MathVisual parent = curr.parent;
            if (parent instanceof com.calctastic.sample.hypercal.render.FractionVisual) {
                com.calctastic.sample.hypercal.render.FractionVisual frac =
                        (com.calctastic.sample.hypercal.render.FractionVisual) parent;
                boolean inNumerator = curr == frac.numeratorVisual;
                if (down == inNumerator) {
                    MathVisual slot = down ? frac.denominatorVisual : frac.numeratorVisual;
                    if (slot == null) {
                        return null;
                    }
                    float cx = Math.max(margin + 1.0f, Math.min(x, (frac.b.x - margin) - 1.0f));
                    float localX = Math.max(0.0f, Math.min(cx - slot.HiPER.x, slot.b.x));
                    return slot.hitTest(new PointF(localX, slot.b.y / 2.0f), textPaint);
                }
            }
            curr = parent;
        }
        return null;
    }

    private MathVisual findVisualForNode(MathVisual root, ExpressionNode target) {
        if (root == null || target == null) return null;
        if (root.modelNode == target) return root;

        if (root instanceof com.calctastic.sample.hypercal.render.SequenceVisual) {
            com.calctastic.sample.hypercal.render.SequenceVisual seq = (com.calctastic.sample.hypercal.render.SequenceVisual) root;
            for (MathVisual child : seq.children) {
                MathVisual found = findVisualForNode(child, target);
                if (found != null) return found;
            }
        } else if (root instanceof com.calctastic.sample.hypercal.render.SqrtVisual) {
            return findVisualForNode(((com.calctastic.sample.hypercal.render.SqrtVisual) root).radicandVisual, target);
        } else if (root instanceof com.calctastic.sample.hypercal.render.FractionVisual) {
            com.calctastic.sample.hypercal.render.FractionVisual frac = (com.calctastic.sample.hypercal.render.FractionVisual) root;
            MathVisual f = findVisualForNode(frac.numeratorVisual, target);
            if (f != null) return f;
            return findVisualForNode(frac.denominatorVisual, target);
        } else if (root instanceof com.calctastic.sample.hypercal.render.PowerVisual) {
            com.calctastic.sample.hypercal.render.PowerVisual pow = (com.calctastic.sample.hypercal.render.PowerVisual) root;
            MathVisual f = findVisualForNode(pow.baseVisual, target);
            if (f != null) return f;
            return findVisualForNode(pow.exponentVisual, target);
        } else if (root instanceof com.calctastic.sample.hypercal.render.ParenthesisVisual) {
            return findVisualForNode(((com.calctastic.sample.hypercal.render.ParenthesisVisual) root).insideVisual, target);
        }

        return null;
    }
}
