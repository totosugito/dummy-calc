package com.calctastic.sample.expression;

import java.io.Serializable;

/**
 * Port 1:1 of com.calctastic.calculator.core.DegreeString (raw/.../DegreeString.java).
 * Parses "D°M'S\"" / partial forms into numeric parts + unit markers.
 */
public class DegreeString implements Serializable {
    private static final long serialVersionUID = 8584172300499453423L;

    public final String deg;       // "°" marker or null
    public final String degrees;   // numeric degrees or null
    public final boolean isNegative;
    public final String min;       // "'" marker or null
    public final String minutes;   // numeric minutes or null
    public final String sec;       // "\"" marker or null
    public final String seconds;   // numeric seconds or null
    public final String string;

    public DegreeString(String str) {
        // Original: y0.a.c(str) > 13 → overflow (digit count). Keep soft check.
        if (countDigits(str) > 13) {
            throw new IllegalArgumentException("DMS Overflow Digits: " + str);
        }
        this.string = str;
        boolean neg = str.startsWith("-");
        this.isNegative = neg;
        String s = neg ? str.substring(1) : str;

        int iDeg = s.indexOf('°');
        String degPart = iDeg >= 0 ? s.substring(0, iDeg) : null;
        this.deg = iDeg >= 0 ? s.substring(iDeg, iDeg + 1) : null;
        if (iDeg >= 0) s = s.substring(iDeg + 1);

        int iMin = s.indexOf('\'');
        String minPart = iMin >= 0 ? s.substring(0, iMin) : null;
        this.min = iMin >= 0 ? s.substring(iMin, iMin + 1) : null;
        if (iMin >= 0) s = s.substring(iMin + 1);

        int iSec = s.indexOf('"');
        String secPart = iSec >= 0 ? s.substring(0, iSec) : null;
        this.sec = iSec >= 0 ? s.substring(iSec, iSec + 1) : null;
        if (iSec >= 0) s = s.substring(iSec + 1);

        if (s.length() > 0) {
            if (secPart == null && minPart == null) {
                minPart = s;
            } else {
                if (secPart != null) {
                    throw new IllegalArgumentException("Could not parse DMS from string: " + str);
                }
                secPart = s;
            }
        }

        this.degrees = (degPart == null || degPart.length() == 0) ? null : degPart;
        this.minutes = (minPart == null || minPart.length() == 0) ? null : minPart;
        this.seconds = (secPart != null && secPart.length() != 0) ? secPart : null;
    }

    private static int countDigits(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) n++;
        }
        return n;
    }

    /** Original b() — has degrees (number or ° marker). */
    public final boolean b() {
        return !(this.degrees == null && this.deg == null);
    }

    /** Original e() — has minutes. */
    public final boolean e() {
        return !(this.minutes == null && this.min == null);
    }

    /** Original f() — has seconds. */
    public final boolean f() {
        return !(this.seconds == null && this.sec == null);
    }

    /** Original g() — display with <dim> placeholders for missing parts. */
    public final String g() {
        StringBuilder sb = new StringBuilder(this.isNegative ? "-" : "");
        if (b()) {
            sb.append(this.degrees != null ? this.degrees : "<dim>0</dim>");
            sb.append(this.deg != null ? this.deg : "<dim>°</dim>");
        }
        if (e()) {
            sb.append(this.minutes != null ? this.minutes : "<dim>0</dim>");
            sb.append(this.min != null ? this.min : "<dim>'</dim>");
        }
        if (f()) {
            sb.append(this.seconds != null ? this.seconds : "<dim>0</dim>");
            sb.append(this.sec != null ? this.sec : "<dim>\"</dim>");
        }
        return sb.toString();
    }

    /** Original n() — plain string with 0 defaults for present sections. */
    public final String n() {
        StringBuilder sb = new StringBuilder(this.isNegative ? "-" : "");
        if (b()) {
            sb.append(this.degrees != null ? this.degrees : "0");
            sb.append('°');
        }
        if (e()) {
            sb.append(this.minutes != null ? this.minutes : "0");
            sb.append('\'');
        }
        if (f()) {
            sb.append(this.seconds != null ? this.seconds : "0");
            sb.append('"');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return n();
    }
}
