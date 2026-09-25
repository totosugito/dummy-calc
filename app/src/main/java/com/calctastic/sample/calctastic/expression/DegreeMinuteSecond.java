package com.calctastic.sample.calctastic.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Port of com.calctastic.calculator.numbers.DegreeMinuteSecond — DMS state machine only.
 *
 * Faithful to raw DegreeMinuteSecond.java:
 *   - fields: degreesOnly, degrees, minutes, seconds, dmsString, isNegative
 *   - Z(str) parse → value + DegreeString
 *   - ctor: split decimal → deg/min/sec (÷60, ÷3600, HALF_UP, carry 60)
 *   - Q(103) cycle: FloatingPoint first press + DegreeMinuteSecond unit cycle
 *   - Y() display when dmsString==null; R()/dmsString.string when set
 *   - I() ⇔ dmsString == null
 *
 * FloatingPoint ops replaced with BigDecimal (same math, same rounding).
 * Binary/complex/etc. ops from original omitted — sample evaluator uses doubles.
 */
public final class DegreeMinuteSecond {

    public static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);
    private static final BigDecimal LIMIT = new BigDecimal("9999999999");
    private static final BigDecimal SIXTY = new BigDecimal("60");
    private static final BigDecimal SEC_ROUND = new BigDecimal("0.001"); // M0(3) ≈ 3 dp

    private final BigDecimal degreesOnly; // decimal degrees value
    private final boolean isNegative;
    private final BigInteger degrees;
    private final BigInteger minutes;
    private final BigDecimal seconds;
    private final DegreeString dmsString; // null ⇔ I()
    private final String errorValue;      // null if ok

    public DegreeMinuteSecond(BigDecimal degreesOnly) {
        this(degreesOnly, null, false);
    }

    public DegreeMinuteSecond(BigDecimal degreesOnly, DegreeString degreeString, boolean allowOverflow) {
        this.degreesOnly = degreesOnly;
        this.isNegative = degreesOnly.signum() < 0;
        BigDecimal abs = degreesOnly.abs();
        boolean overflowOk = abs.compareTo(LIMIT) < 0;
        if (!overflowOk && !allowOverflow) {
            throw new ArithmeticException("DMS Overflow Size: " + abs);
        }

        BigInteger deg = abs.toBigInteger();
        BigDecimal rem1 = abs.subtract(new BigDecimal(deg));
        BigDecimal mAll = rem1.multiply(SIXTY);
        BigInteger min = mAll.toBigInteger();
        BigDecimal rem2 = mAll.subtract(new BigDecimal(min));

        // M0(3): round fractional minutes to 3 dp → seconds portion * already in min-fraction
        // Original: rem2 is fraction of a minute; M0(3) rounds it; equals 60 → carry
        // seconds = rem2 * 60 rounded to 3 dp? Original M0(3) on (rem1*60 fraction).
        // floatingPointC1 = fractional part of minutes (0..1); M0(3) rounds to 3 dp;
        // if that == 60? No — equals f2593j (60) is for full-minute scale.
        // Re-read ctor: C0/fraction path gives minutes as integer part of (frac*60);
        // C1 = fraction of that; M0(3) on C1... wait C1 is 0..1 fraction.
        // Actually: floatingPointC1.M0(3) where C1 = frac of minutes (0..1).
        // equals(SIXTY)? That would never be true for 0..1.
        // Looking again: floatingPointC1 = floatingPointC0.r0().C0(f2593j) — frac(min)*60 = seconds!
        // floatingPointM0 = seconds.M0(3) — round seconds to 3 dp
        // if seconds.rounded == 60: seconds=0, minutes++, carry
        BigDecimal sec = rem2.multiply(SIXTY).setScale(3, RoundingMode.HALF_UP);
        if (sec.compareTo(SIXTY) >= 0) {
            sec = BigDecimal.ZERO;
            min = min.add(BigInteger.ONE);
            if (min.equals(BigInteger.valueOf(60))) {
                min = BigInteger.ZERO;
                deg = deg.add(BigInteger.ONE);
            }
        }

        this.degrees = deg;
        this.minutes = min;
        this.seconds = sec;
        this.dmsString = degreeString;
        this.errorValue = overflowOk ? null : "error_overflow";
    }

    public DegreeMinuteSecond(DegreeMinuteSecond other) {
        this.degreesOnly = other.degreesOnly;
        this.isNegative = other.isNegative;
        this.degrees = other.degrees;
        this.minutes = other.minutes;
        this.seconds = other.seconds;
        this.errorValue = other.errorValue;
        this.dmsString = null;
    }

    /** Original Z(str, mathContext, z2) — parse DMS string → value + DegreeString. */
    public static DegreeMinuteSecond Z(String str, boolean z2) {
        DegreeString ds = new DegreeString(str);
        int sign = ds.isNegative ? -1 : 1;
        BigDecimal deg = new BigDecimal(ds.degrees != null ? ds.degrees : "0");
        BigDecimal min = new BigDecimal(ds.minutes != null ? ds.minutes : "0");
        BigDecimal sec = new BigDecimal(ds.seconds != null ? ds.seconds : "0");
        // value = ±(deg + min/60 + sec/3600)
        BigDecimal value = deg
                .add(min.divide(SIXTY, MC))
                .add(sec.divide(new BigDecimal("3600"), MC))
                .multiply(BigDecimal.valueOf(sign));
        if (z2) {
            ds = null;
        }
        return new DegreeMinuteSecond(value, ds, true);
    }

    /**
     * Original FloatingPoint.Q(103) + DegreeMinuteSecond.Q(103) for command DMS (ordinal 103).
     * input: current operand string (plain "7" / "7.15" or DMS "7°" / "7°9'" / …).
     * Returns new operand string, or null if no change (returns this).
     */
    public static String pressDms(String operand) {
        if (operand == null) return null;
        String t = operand.trim();
        if (t.isEmpty()) return null;

        boolean hasDms = t.indexOf('°') >= 0 || t.indexOf('\'') >= 0 || t.indexOf('"') >= 0;

        if (!hasDms) {
            // FloatingPoint.Q(103)
            if (t.equals("-")) {
                return Z("-0°", false).displayEquation();
            }
            BigDecimal v;
            try {
                v = new BigDecimal(t.replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
            // a.p = integer? scale<=0 or rounds DOWN to same
            boolean isInteger = v.signum() == 0 || v.scale() <= 0
                    || v.setScale(0, RoundingMode.DOWN).compareTo(v) == 0;
            if (t.contains("E") || !isInteger) {
                // non-integer → new DegreeMinuteSecond(this) → dmsString=null → Y()
                return new DegreeMinuteSecond(v, null, false).Y();
            }
            // integer → Z(s + "°")
            return Z(t + "°", false).displayEquation();
        }

        // Already DegreeMinuteSecond — Q(103)
        return cycleExisting(t);
    }

    /** DegreeMinuteSecond.Q case 103 on a string that already has DMS markers. */
    private static String cycleExisting(String t) {
        DegreeString ds;
        try {
            ds = new DegreeString(t);
        } catch (RuntimeException e) {
            return null;
        }

        // Reconstruct object with dmsString set (as after first Z())
        DegreeMinuteSecond dms = Z(t, false);

        // --- Q(103) body ---
        if (dms.I()) {
            // dmsString null → degreesOnly plain
            return formatPlain(dms.degreesOnly);
        }
        DegreeString d = dms.dmsString;
        if (d.f()) { // has seconds section
            if (d.sec != null) {
                if (d.min != null) {
                    return null; // return this — full D°M'S" no change
                }
                // replaceFirst(")", deg!=null ? "'" : "°")
                return Z(d.string.replaceFirst("\"", d.deg != null ? "'" : "°"), false).displayEquation();
            }
            // append "
            return Z(d.string + "\"", true).displayEquation();
        }
        if (!d.e()) { // no minutes
            if (d.deg != null) {
                return Z(d.string.replaceFirst("°", "'"), false).displayEquation();
            }
            return null; // this
        }
        // has minutes
        if (d.min != null) {
            return Z(d.string.replaceFirst("'", "\""), false).displayEquation();
        }
        // has minutes number but no ' marker — insert ' (T.a.b)
        String inserted = insertBeforeSeconds(d.string, "'");
        boolean dropDs = !(d.string.contains(".") || d.string.contains("E"));
        return Z(inserted, dropDs).displayEquation();
    }

    /** Original T.a.b(str, "'") — insert marker before seconds if any, else append logic. */
    private static String insertBeforeSeconds(String s, String marker) {
        int i = s.indexOf('"');
        if (i >= 0) {
            return s.substring(0, i) + marker + s.substring(i);
        }
        return s + marker;
    }

    /** Y() — equation display. */
    public String Y() {
        if (!I()) {
            return R();
        }
        if (degrees.signum() == 0 && minutes.signum() == 0 && seconds.signum() == 0) {
            return "0°";
        }
        StringBuilder sb = new StringBuilder(isNegative ? "-" : "");
        if (degrees.signum() > 0) {
            sb.append(degrees).append('°');
        }
        if (minutes.signum() > 0) {
            sb.append(minutes).append('\'');
        }
        if (seconds.signum() > 0) {
            sb.append(formatSeconds()).append('"');
        }
        return sb.toString();
    }

    /** R() — raw dmsString.string or null. */
    public String R() {
        return dmsString != null ? dmsString.string : null;
    }

    public boolean I() {
        return dmsString == null;
    }

    public boolean hasError() {
        return errorValue != null;
    }

    public BigDecimal getDegreesOnly() {
        return degreesOnly;
    }

    /** Equation line text (what sample puts in mCurrentInput). */
    public String displayEquation() {
        if (I()) {
            return Y();
        }
        return dmsString.n(); // plain n() without dim tags for input string
    }

    public String toString() {
        return Y();
    }

    /** Seconds display: up to 3 decimal places stripped (X() ≈ y0.a.a(..., 3, 5, ...)). */
    private String formatSeconds() {
        BigDecimal s = seconds.stripTrailingZeros();
        if (s.scale() < 0) s = s.setScale(0);
        if (s.scale() > 3) s = s.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        if (s.scale() < 0) s = s.setScale(0);
        return s.toPlainString();
    }

    private static String formatPlain(BigDecimal v) {
        if (v.scale() <= 0) {
            return v.toBigInteger().toString();
        }
        return v.stripTrailingZeros().toPlainString();
    }
}
