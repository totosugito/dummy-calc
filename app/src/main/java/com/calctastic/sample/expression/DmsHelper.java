package com.calctastic.sample.expression;

/**
 * Degree-Minute-Second conversion — port from DegreeMinuteSecond (case 103 DMS).
 * Converts a decimal degree value to "D°M'S\"" format (and reverse parse).
 */
public final class DmsHelper {

    private DmsHelper() {}

    /** Convert decimal degrees → "30°30'0\"" style string. */
    public static String toDms(double decimalDeg) {
        boolean neg = decimalDeg < 0;
        double abs = Math.abs(decimalDeg);
        int deg = (int) abs;
        double rem = (abs - deg) * 60;
        int min = (int) rem;
        double sec = (rem - min) * 60;
        // Round seconds; carry if ≥60
        sec = Math.round(sec * 1e6) / 1e6;
        if (sec >= 60.0) {
            sec = 0;
            min++;
        }
        if (min >= 60) {
            min = 0;
            deg++;
        }
        String sign = neg ? "-" : "";
        if (sec == (long) sec) {
            return sign + deg + "°" + min + "'" + (long) sec + "\"";
        }
        return sign + deg + "°" + min + "'" + sec + "\"";
    }

    /**
     * Try convert current expression text to DMS if it parses as a number.
     * Returns null if not convertible.
     */
    public static String tryConvert(String expr) {
        if (expr == null || expr.trim().isEmpty()) return null;
        String t = expr.trim();
        // Already DMS?
        if (t.contains("°") || t.contains("'")) return null;
        try {
            double v = Double.parseDouble(t.replace(",", ""));
            return toDms(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parse "D°M'S\"" or mixed → decimal degrees. Returns NaN on failure. */
    public static double parseDms(String s) {
        if (s == null) return Double.NaN;
        try {
            String t = s.trim();
            boolean neg = t.startsWith("-");
            if (neg || t.startsWith("+")) t = t.substring(1);

            double deg = 0, min = 0, sec = 0;
            if (t.contains("°")) {
                String[] p = t.split("°", 2);
                deg = Double.parseDouble(p[0].isEmpty() ? "0" : p[0]);
                String rest = p[1];
                if (rest.contains("'")) {
                    String[] p2 = rest.split("'", 2);
                    min = Double.parseDouble(p2[0].isEmpty() ? "0" : p2[0]);
                    if (p2.length > 1 && !p2[1].isEmpty()) {
                        sec = Double.parseDouble(p2[1].replace("\"", "").trim());
                    }
                } else if (!rest.isEmpty()) {
                    min = Double.parseDouble(rest.replace("'", "").replace("\"", ""));
                }
            } else {
                deg = Double.parseDouble(t);
            }
            double result = deg + min / 60.0 + sec / 3600.0;
            return neg ? -result : result;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
