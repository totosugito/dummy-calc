package com.calctastic.sample.hypercal.render;

import com.calctastic.sample.hypercal.engine.model.ExpressionNode;

/**
 * Looks up the {@link MathVisual} that renders a given {@link ExpressionNode} inside an
 * already-built visual tree (see {@link VisualTreeBuilder}). Used by HyperCalDisplayView to
 * locate the cursor's visual (for drawing the caret and for ▲/▼ navigation) without the View
 * needing to know each visual type's child layout.
 *
 * Moved out of HyperCalDisplayView (2026-09-26) alongside VisualTreeBuilder, which it mirrors --
 * see specs/display_scaling_typography.md for where display-related code now lives.
 */
public final class VisualTree {
    private VisualTree() {
    }

    public static MathVisual find(MathVisual root, ExpressionNode target) {
        if (root == null || target == null) return null;
        if (root.modelNode == target) return root;

        if (root instanceof SequenceVisual) {
            SequenceVisual seq = (SequenceVisual) root;
            for (MathVisual child : seq.children) {
                MathVisual found = find(child, target);
                if (found != null) return found;
            }
        } else if (root instanceof SqrtVisual) {
            // Degree first, matching reading order (and CursorNav.moveLeft/moveRight's own
            // degree-before-radicand slot order) -- was previously only searching radicandVisual,
            // silently unreachable for the degree slot as soon as a real n-th-root button started
            // using SqrtNode.degree (see specs/btn_sqrt.md).
            SqrtVisual sqrt = (SqrtVisual) root;
            MathVisual f = find(sqrt.degreeVisual, target);
            if (f != null) return f;
            return find(sqrt.radicandVisual, target);
        } else if (root instanceof FractionVisual) {
            FractionVisual frac = (FractionVisual) root;
            MathVisual f = find(frac.integerVisual, target);
            if (f != null) return f;
            f = find(frac.numeratorVisual, target);
            if (f != null) return f;
            return find(frac.denominatorVisual, target);
        } else if (root instanceof PowerVisual) {
            PowerVisual pow = (PowerVisual) root;
            MathVisual f = find(pow.baseVisual, target);
            if (f != null) return f;
            return find(pow.exponentVisual, target);
        } else if (root instanceof ParenthesisVisual) {
            return find(((ParenthesisVisual) root).insideVisual, target);
        }

        return null;
    }
}
