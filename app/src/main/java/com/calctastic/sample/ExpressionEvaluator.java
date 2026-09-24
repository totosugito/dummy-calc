package com.calctastic.sample;

/**
 * Expression evaluation engine for the sample calculator.
 * Ported from SimpleCalculatorActivity; angle/hyperbolic state mirrors ScientificData
 * (AngleUnit + hyperbolic flag from the decompiled original).
 */
class ExpressionEvaluator {

    /** AngleUnit: DEG / RAD / GRD */
    String angleUnit = "DEG";
    /** ScientificData.hyperbolic */
    boolean hyperbolic = false;

    static boolean isExpressionComplete(String s) {
        if (s == null || s.isEmpty()) return false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth < 0) return false;
        }
        if (depth != 0) return false;
        char last = s.charAt(s.length() - 1);
        if (last == '+' || last == '-' || last == '−' || last == '×' || last == '*'
                || last == '÷' || last == '/' || last == '^' || last == '(') {
            return false;
        }
        // trailing incomplete function name (sin without paren yet)
        String t = s.trim();
        for (String fn : CalcTokens.AUTO_PAREN_FUNCTIONS) {
            if (t.endsWith(fn) && t.length() == fn.length()) return false;
        }
        return true;
    }

    String evaluateExpression(String raw) {
        try {
            String expr = raw.replaceAll("×", "*")
                             .replaceAll("÷", "/")
                             .replaceAll("−", "-")
                             .replaceAll("π", "(" + Double.toString(Math.PI) + ")")
                             .replaceAll("²", "^2")
                             .trim();
            if (expr.isEmpty()) return raw;

            // Constant e (standalone, not inside identifiers) — CONST_E plain "e"
            expr = expr.replaceAll("(?<![A-Za-z0-9.])e(?![A-Za-z0-9])", Double.toString(Math.E));

            // √(x) prefix (SQRT / NTH_ROOT plain "√")
            expr = evalSqrtCalls(expr);

            // Function calls: innermost name(...) first — ordinals 68–86 auto-paren set
            expr = evalFunctionCalls(expr);

            // Ensure binary operators are space-separated for token pass
            expr = normalizeOpSpaces(expr);

            // Postfix / compact tokens
            expr = evalPostfix(expr);

            // Powers a^b
            expr = evalPowers(expr);

            // Percent: 50% → 0.5 (FloatingPoint case 93 movePointLeft(2))
            expr = evalPercent(expr);

            // Word operators: mod, nPr, nCr, Δ%
            expr = evalWordOps(expr);

            expr = normalizeOpSpaces(expr);

            // Fraction check (entire expr)
            if (expr.matches("^(\\d+/)?\\d+/\\d+$")) {
                String[] parts = expr.split("/");
                if (parts.length == 2) {
                    long n = Long.parseLong(parts[0]);
                    long d = Long.parseLong(parts[1]);
                    if (d != 0) {
                        if (n % d == 0) return String.valueOf(n / d);
                        return n + "/" + d;
                    }
                } else if (parts.length == 3) {
                    long w = Long.parseLong(parts[0]);
                    long n = Long.parseLong(parts[1]);
                    long d = Long.parseLong(parts[2]);
                    if (d != 0) {
                        return w + "/" + n + "/" + d;
                    }
                }
            }

            String[] tokens = expr.trim().split("\\s+");
            if (tokens.length == 1) {
                double v = parseNumberOrFraction(tokens[0]);
                return formatNumber(v);
            }
            if (tokens.length >= 3) {
                double a = parseNumberOrFraction(tokens[0]);
                int idx = 1;
                while (idx + 1 < tokens.length) {
                    String op = tokens[idx];
                    double b = parseNumberOrFraction(tokens[idx + 1]);
                    if ("+".equals(op)) a = a + b;
                    else if ("-".equals(op)) a = a - b;
                    else if ("*".equals(op)) a = a * b;
                    else if ("/".equals(op)) a = (b != 0) ? a / b : Double.NaN;
                    else if ("^".equals(op)) a = Math.pow(a, b);
                    idx += 2;
                }
                return formatNumber(a);
            }
            return expr;
        } catch (Exception e) {
            return raw;
        }
    }

    static String formatNumber(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return String.valueOf(v);
        if (v == (long) v && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    /** √(x) → sqrt — SQRT/NTH_ROOT use plain "√". */
    private String evalSqrtCalls(String expr) {
        boolean progress = true;
        while (progress && expr.contains("√(")) {
            progress = false;
            int start = expr.indexOf("√(");
            int depth = 0;
            int end = -1;
            for (int i = start + 1; i < expr.length(); i++) {
                char c = expr.charAt(i);
                if (c == '(') depth++;
                else if (c == ')') {
                    depth--;
                    if (depth == 0) { end = i; break; }
                }
            }
            if (end <= start) break;
            String inner = expr.substring(start + 2, end);
            if (inner.indexOf('(') >= 0) {
                // evaluate inner first on next loop via function pass; just recurse once
                expr = expr.substring(0, start + 2) + evalSqrtCalls(inner) + expr.substring(end);
                progress = true;
                continue;
            }
            try {
                double v = parseNumberOrFraction(evalPowers(inner.trim()));
                expr = expr.substring(0, start) + formatNumber(Math.sqrt(v)) + expr.substring(end + 1);
                progress = true;
            } catch (Exception e) {
                break;
            }
        }
        return expr;
    }

    /** Space-separate binary + - * / for the token evaluator (keep leading unary minus). */
    static String normalizeOpSpaces(String expr) {
        StringBuilder sb = new StringBuilder(expr.trim());
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '+' || c == '*' || c == '/') {
                if (i > 0 && sb.charAt(i - 1) != ' ') {
                    sb.insert(i, ' ');
                    i++;
                }
                if (i + 1 < sb.length() && sb.charAt(i + 1) != ' ') {
                    sb.insert(i + 1, ' ');
                    i++;
                }
            } else if (c == '-') {
                // binary minus only (not unary after op/start/open)
                if (i > 0) {
                    char p = sb.charAt(i - 1);
                    boolean unary = (p == '(' || p == '+' || p == '-' || p == '*' || p == '/' || p == ' ');
                    // also " - " already spaced word ops
                    if (!unary && p != ' ') {
                        if (sb.charAt(i - 1) != ' ') {
                            sb.insert(i, ' ');
                            i++;
                        }
                        if (i + 1 < sb.length() && sb.charAt(i + 1) != ' ') {
                            sb.insert(i + 1, ' ');
                            i++;
                        }
                    }
                }
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Evaluate func(args) where args has no nested '('.
     * Angle units follow AngleUnit: convert to rad for sin/cos/tan, from rad for inverses.
     * Hyperbolic mode (hyperbolic) uses sinh/cosh/tanh (FloatingPoint cases 73–75 z2 branch).
     */
    private String evalFunctionCalls(String expr) {
        boolean progress = true;
        while (progress) {
            progress = false;
            int open = -1;
            // find innermost '('
            for (int i = expr.length() - 1; i >= 0; i--) {
                if (expr.charAt(i) == '(') { open = i; break; }
            }
            if (open < 0) break;
            int close = expr.indexOf(')', open);
            if (close < 0) break;
            // ensure no nested ( between open and close
            if (expr.substring(open + 1, close).indexOf('(') >= 0) {
                // shift search left of this open
                String left = expr.substring(0, open);
                int nested = left.lastIndexOf('(');
                if (nested < 0) break;
                open = nested;
                close = expr.indexOf(')', open);
                if (close < 0) break;
                if (expr.substring(open + 1, close).indexOf('(') >= 0) continue;
            }
            String inner = expr.substring(open + 1, close).trim();
            // function name before '('
            int nameStart = open;
            while (nameStart > 0 && (Character.isLetter(expr.charAt(nameStart - 1)))) nameStart--;
            String name = expr.substring(nameStart, open);

            if (name.isEmpty()) {
                // bare (expr) → evaluate inner if pure number context later; strip one level if fully wrapped
                if (nameStart == 0 && close == expr.length() - 1) {
                    expr = inner;
                    progress = true;
                }
                continue;
            }

            double arg;
            try {
                // inner may still contain ^ or spaces
                String innerEval = evalPowers(inner.replaceAll("π", Double.toString(Math.PI)));
                arg = parseNumberOrFraction(innerEval.trim());
            } catch (Exception e) {
                break;
            }

            Double result = applyFunction(name, arg);
            if (result == null) break;
            expr = expr.substring(0, nameStart) + formatNumber(result) + expr.substring(close + 1);
            progress = true;
        }
        return expr;
    }

    /** Map function name → value. Returns null if unknown/incomplete. */
    private Double applyFunction(String name, double x) {
        switch (name) {
            case "sin":
                return hyperbolic ? Math.sinh(x) : Math.sin(toRadians(x));
            case "cos":
                return hyperbolic ? Math.cosh(x) : Math.cos(toRadians(x));
            case "tan":
                return hyperbolic ? Math.tanh(x) : Math.tan(toRadians(x));
            case "asin": {
                if (hyperbolic) return asinh(x);
                double r = Math.asin(x);
                return fromRadians(r);
            }
            case "acos": {
                if (hyperbolic) return acosh(x);
                double r = Math.acos(x);
                return fromRadians(r);
            }
            case "atan": {
                if (hyperbolic) return atanh(x);
                double r = Math.atan(x);
                return fromRadians(r);
            }
            case "ln":
                return x > 0 ? Math.log(x) : Double.NaN;
            case "log":
                return x > 0 ? Math.log10(x) : Double.NaN;
            case "abs":
                return Math.abs(x);
            case "ceil":
                return Math.ceil(x);
            case "floor":
                return Math.floor(x);
            case "arg":
                // COMPLEX_ARGUMENT on real: 0 or π (AngleUnit.e from rad)
                return x >= 0 ? 0 : fromRadians(Math.PI);
            case "re":
                return x;      // COMPLEX_REAL
            case "im":
                return 0.0;    // COMPLEX_IMAGINARY → 0 for real
            case "conj":
                return x;      // COMPLEX_CONJUGATE on real
            default:
                return null;
        }
    }

    /** AngleUnit.n(): DEG=180, RAD=π, GRD=200 — convert display unit → radians. */
    private double toRadians(double v) {
        if ("DEG".equals(angleUnit)) return Math.toRadians(v);
        if ("GRD".equals(angleUnit)) return v * Math.PI / 200.0;
        return v; // RAD
    }

    /** Inverse: radians → current AngleUnit. */
    private double fromRadians(double rad) {
        if ("DEG".equals(angleUnit)) return Math.toDegrees(rad);
        if ("GRD".equals(angleUnit)) return rad * 200.0 / Math.PI;
        return rad;
    }

    static double asinh(double x) { return Math.log(x + Math.sqrt(x * x + 1)); }
    static double acosh(double x) { return Math.log(x + Math.sqrt(x * x - 1)); }
    static double atanh(double x) { return 0.5 * Math.log((1 + x) / (1 - x)); }

    /** Postfix ! ² and bare √x. */
    private String evalPostfix(String expr) {
        // factorial: digits!
        while (true) {
            int i = expr.lastIndexOf('!');
            if (i <= 0) break;
            int s = i - 1;
            while (s >= 0 && (Character.isDigit(expr.charAt(s)) || expr.charAt(s) == '.')) s--;
            s++;
            if (s >= i) break;
            double n = parseNumberOrFraction(expr.substring(s, i));
            if (n < 0 || n != Math.floor(n) || n > 170) break;
            long f = 1;
            for (long k = 2; k <= (long) n; k++) f *= k;
            expr = expr.substring(0, s) + f + expr.substring(i + 1);
        }
        return expr;
    }

    String evalPowers(String expr) {
        while (expr.contains("^")) {
            int caretIdx = expr.indexOf('^');
            int baseStart = caretIdx - 1;
            while (baseStart >= 0 && (Character.isDigit(expr.charAt(baseStart)) || expr.charAt(baseStart) == '.' || expr.charAt(baseStart) == '-')) {
                baseStart--;
            }
            baseStart++;
            int expEnd = caretIdx + 1;
            boolean neg = expEnd < expr.length() && expr.charAt(expEnd) == '-';
            if (neg) expEnd++;
            while (expEnd < expr.length() && (Character.isDigit(expr.charAt(expEnd)) || expr.charAt(expEnd) == '.')) expEnd++;
            if (baseStart < caretIdx && expEnd > caretIdx + 1) {
                double baseVal = Double.parseDouble(expr.substring(baseStart, caretIdx));
                double expVal = Double.parseDouble(expr.substring(caretIdx + 1, expEnd));
                expr = expr.substring(0, baseStart) + formatNumber(Math.pow(baseVal, expVal)) + expr.substring(expEnd);
            } else {
                break;
            }
        }
        return expr;
    }

    /** `50%` → `0.5` (FloatingPoint PERCENT movePointLeft(2)). */
    private String evalPercent(String expr) {
        StringBuilder sb = new StringBuilder(expr);
        int i = 0;
        while (i < sb.length()) {
            if (sb.charAt(i) == '%') {
                int s = i - 1;
                while (s >= 0 && (Character.isDigit(sb.charAt(s)) || sb.charAt(s) == '.')) s--;
                s++;
                if (s < i) {
                    double v = Double.parseDouble(sb.substring(s, i));
                    String rep = formatNumber(v / 100.0);
                    sb.replace(s, i + 1, rep);
                    i = s + rep.length();
                    continue;
                }
            }
            i++;
        }
        return sb.toString();
    }

    /** Word ops with surrounding spaces: mod, nPr, nCr, Δ% — left-to-right. */
    private String evalWordOps(String expr) {
        String[] ops = { " mod ", " nPr ", " nCr ", " Δ% " };
        for (String op : ops) {
            while (expr.contains(op)) {
                int mid = expr.indexOf(op);
                // left operand
                int le = mid;
                while (le > 0 && !isOpBoundary(expr.charAt(le - 1))) le--;
                // right operand
                int rs = mid + op.length();
                int re = rs;
                while (re < expr.length() && !isOpBoundary(expr.charAt(re))) re++;
                if (le >= mid || rs >= re) break;
                double a = parseNumberOrFraction(expr.substring(le, mid).trim());
                double b = parseNumberOrFraction(expr.substring(rs, re).trim());
                double r;
                if (" mod ".equals(op)) r = (b != 0) ? Math.IEEEremainder(a, b) : Double.NaN;
                else if (" nPr ".equals(op)) r = nPr((int) a, (int) b);
                else if (" nCr ".equals(op)) r = nCr((int) a, (int) b);
                else r = a * (b / 100.0); // Δ% rough: a * b%
                String rep = formatNumber(r);
                expr = expr.substring(0, le) + rep + expr.substring(re);
            }
        }
        return expr;
    }

    static boolean isOpBoundary(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')';
    }

    static double nPr(int n, int r) {
        if (r < 0 || r > n) return Double.NaN;
        double p = 1;
        for (int i = 0; i < r; i++) p *= (n - i);
        return p;
    }

    static double nCr(int n, int r) {
        if (r < 0 || r > n) return Double.NaN;
        return nPr(n, r) / fact(r);
    }

    static double fact(int n) {
        double f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }

    double parseNumberOrFraction(String token) {
        if (token.contains("/")) {
            String[] parts = token.split("/");
            if (parts.length == 2) {
                double n = Double.parseDouble(parts[0]);
                double d = Double.parseDouble(parts[1]);
                return d != 0 ? n / d : 0;
            } else if (parts.length == 3) {
                double w = Double.parseDouble(parts[0]);
                double n = Double.parseDouble(parts[1]);
                double d = Double.parseDouble(parts[2]);
                return d != 0 ? w + (n / d) : 0;
            }
        }
        return Double.parseDouble(token);
    }

}
