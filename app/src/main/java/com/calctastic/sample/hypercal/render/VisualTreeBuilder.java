package com.calctastic.sample.hypercal.render;

import com.calctastic.sample.hypercal.engine.model.ExpressionNode;
import com.calctastic.sample.hypercal.engine.model.FractionNode;
import com.calctastic.sample.hypercal.engine.model.NumberNode;
import com.calctastic.sample.hypercal.engine.model.OperatorNode;
import com.calctastic.sample.hypercal.engine.model.ParenthesisNode;
import com.calctastic.sample.hypercal.engine.model.PowerNode;
import com.calctastic.sample.hypercal.engine.model.SequenceNode;
import com.calctastic.sample.hypercal.engine.model.SqrtNode;

/**
 * Builds the MathVisual tree hierarchy from the ExpressionNode AST.
 * Faithful to HiPER Calc android.core.PH.java.
 */
public class VisualTreeBuilder {

    public static MathVisual buildVisualTree(ExpressionNode node) {
        if (node == null) {
            return null;
        }

        if (node instanceof NumberNode) {
            return new NumberVisual((NumberNode) node);
        }

        if (node instanceof com.calctastic.sample.hypercal.engine.model.EmptyNode) {
            return new PlaceholderVisual((com.calctastic.sample.hypercal.engine.model.EmptyNode) node);
        }

        if (node instanceof OperatorNode) {
            return new OperatorVisual((OperatorNode) node);
        }

        if (node instanceof SequenceNode) {
            SequenceNode seq = (SequenceNode) node;
            SequenceVisual visual = new SequenceVisual(seq);
            for (int i = 0; i < seq.getChildCount(); i++) {
                ExpressionNode child = seq.getChild(i);
                MathVisual childVisual = buildVisualTree(child);
                if (childVisual != null) {
                    visual.addChild(childVisual);
                }
            }
            return visual;
        }

        if (node instanceof SqrtNode) {
            SqrtNode sqrt = (SqrtNode) node;
            SqrtVisual visual = new SqrtVisual(sqrt);
            if (sqrt.radicand != null) {
                visual.radicandVisual = buildVisualTree(sqrt.radicand);
                if (visual.radicandVisual != null) visual.radicandVisual.parent = visual;
            }
            if (sqrt.degree != null) {
                visual.degreeVisual = buildVisualTree(sqrt.degree);
                if (visual.degreeVisual != null) visual.degreeVisual.parent = visual;
            }
            return visual;
        }

        if (node instanceof FractionNode) {
            FractionNode frac = (FractionNode) node;
            FractionVisual visual = new FractionVisual(frac);
            if (frac.integerPart != null) {
                visual.integerVisual = buildVisualTree(frac.integerPart);
                if (visual.integerVisual != null) visual.integerVisual.parent = visual;
            }
            if (frac.numerator != null) {
                visual.numeratorVisual = buildVisualTree(frac.numerator);
                if (visual.numeratorVisual != null) visual.numeratorVisual.parent = visual;
            }
            if (frac.denominator != null) {
                visual.denominatorVisual = buildVisualTree(frac.denominator);
                if (visual.denominatorVisual != null) visual.denominatorVisual.parent = visual;
            }
            return visual;
        }

        if (node instanceof PowerNode) {
            PowerNode pow = (PowerNode) node;
            PowerVisual visual = new PowerVisual(pow);
            if (pow.base != null) {
                visual.baseVisual = buildVisualTree(pow.base);
                if (visual.baseVisual != null) visual.baseVisual.parent = visual;
            }
            if (pow.exponent != null) {
                visual.exponentVisual = buildVisualTree(pow.exponent);
                if (visual.exponentVisual != null) visual.exponentVisual.parent = visual;
            }
            return visual;
        }

        if (node instanceof ParenthesisNode) {
            ParenthesisNode paren = (ParenthesisNode) node;
            ParenthesisVisual visual = new ParenthesisVisual(paren);
            if (paren.content != null) {
                visual.insideVisual = buildVisualTree(paren.content);
                if (visual.insideVisual != null) visual.insideVisual.parent = visual;
            }
            return visual;
        }

        return null;
    }
}
