package com.calctastic.sample.hypercal.display.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Token representasi simbol/operasi matematika yang dirender pada HiPER Canvas.
 */
public class MathToken {
    public enum Type {
        NUMBER,         // e.g. "123", "0.5"
        OPERATOR,       // e.g. "+", "-", "×", "÷"
        FRACTION,       // numerator, denominator
        SQRT,           // square root radical
        POWER,          // base ^ exponent
        PAREN_GROUP     // ( content )
    }

    public final Type type;
    public String text;
    public List<MathToken> children = new ArrayList<>();
    public List<MathToken> secondaryChildren = new ArrayList<>(); // for fraction denominator / power exponent

    public MathToken(Type type, String text) {
        this.type = type;
        this.text = text;
    }

    public static MathToken number(String num) {
        return new MathToken(Type.NUMBER, num);
    }

    public static MathToken operator(String op) {
        return new MathToken(Type.OPERATOR, op);
    }

    public static MathToken fraction(List<MathToken> numerator, List<MathToken> denominator) {
        MathToken token = new MathToken(Type.FRACTION, "");
        if (numerator != null) token.children.addAll(numerator);
        if (denominator != null) token.secondaryChildren.addAll(denominator);
        return token;
    }

    public static MathToken sqrt(List<MathToken> inner) {
        MathToken token = new MathToken(Type.SQRT, "");
        if (inner != null) token.children.addAll(inner);
        return token;
    }

    public static MathToken power(List<MathToken> base, List<MathToken> exp) {
        MathToken token = new MathToken(Type.POWER, "");
        if (base != null) token.children.addAll(base);
        if (exp != null) token.secondaryChildren.addAll(exp);
        return token;
    }

    public static MathToken paren(List<MathToken> inner) {
        MathToken token = new MathToken(Type.PAREN_GROUP, "");
        if (inner != null) token.children.addAll(inner);
        return token;
    }
}
