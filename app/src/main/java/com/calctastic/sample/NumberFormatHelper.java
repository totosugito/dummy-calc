package com.calctastic.sample;

import java.util.ArrayList;
import java.util.List;

public final class NumberFormatHelper {

    // Same defaults as CalcTastic's y0.a
    public static String decimalSeparator = ".";
    public static String groupingSeparator = ",";
    public static boolean groupingEnabled = true;

    public static class FormattedResult {
        public final String formattedText;
        public final String taggedText; // with <o> and <p> for coloring
        public final int cursorPosition;
        public final List<Integer> printedSizes;

        public FormattedResult(String formattedText, String taggedText, int cursorPosition, List<Integer> printedSizes) {
            this.formattedText = formattedText;
            this.taggedText = taggedText;
            this.cursorPosition = cursorPosition;
            this.printedSizes = printedSizes;
        }
    }

    /**
     * Exact digit grouping algorithm decompiled from y0.a.f(...)
     */
    public static String formatNumberWithGrouping(String str, int groupSize, String groupSep, String decSep) {
        if (!groupingEnabled) {
            return str;
        }
        int iIndexOf;
        StringBuilder sb = new StringBuilder(str);
        int i3 = 1;
        if (str.contains(decSep)) {
            iIndexOf = str.indexOf(decSep);
        } else if (str.contains("E")) {
            iIndexOf = str.indexOf("E");
        } else {
            iIndexOf = str.length();
        }
        for (int i4 = iIndexOf - 1; i4 > 0 && sb.charAt(i4 - 1) != '-'; i4--) {
            if (i3 % groupSize == 0) {
                sb.insert(i4, groupSep);
            }
            i3++;
        }
        return sb.toString();
    }

    /**
     * Formats an equation or numeric value, tracking the formatted cursor offset,
     * character sizes, and CalcTastic markup tags (<o> for operators, <p> for parentheses).
     */
    public static FormattedResult formatEquation(String raw, int rawCursorIndex) {
        if (raw == null || raw.isEmpty()) {
            return new FormattedResult("", "", 0, new ArrayList<>());
        }

        StringBuilder plainBuilder = new StringBuilder();
        StringBuilder taggedBuilder = new StringBuilder();
        List<Integer> printedSizes = new ArrayList<>();
        int formattedCursor = 0;
        int rawLen = raw.length();

        int i = 0;
        while (i < rawLen) {
            char ch = raw.charAt(i);

            // Check if this position begins a fraction: [digits/]digits/digits or digits/digits or digits/
            // Note: Division operators are preceded and followed by spaces (" / "), whereas fractions are compact like "7/5" or "1/2/3" or "7/"
            boolean isFractionStart = false;
            int fracEnd = -1;
            int firstSlash = -1;
            int secondSlash = -1;

            if (Character.isDigit(ch)) {
                int j = i;
                int slashes = 0;
                int slash1 = -1, slash2 = -1;
                while (j < rawLen) {
                    char cj = raw.charAt(j);
                    if (Character.isDigit(cj)) {
                        j++;
                    } else if (cj == '/') {
                        slashes++;
                        if (slashes == 1) slash1 = j;
                        else if (slashes == 2) slash2 = j;
                        else break;
                        j++;
                    } else {
                        break;
                    }
                }
                if (slashes == 1 || slashes == 2) {
                    isFractionStart = true;
                    fracEnd = j;
                    firstSlash = slash1;
                    secondSlash = slash2;
                }
            }

            if (isFractionStart) {
                // Parse the fraction components
                String wholePart = null;
                String numerPart = "";
                String denomPart = "";

                int wholeStart = -1, wholeEnd = -1;
                int numerStart = -1, numerEnd = -1;
                int denomStart = -1, denomEnd = -1;

                if (secondSlash != -1) {
                    // Mixed fraction: whole / numer / denom
                    wholeStart = i;
                    wholeEnd = firstSlash;
                    wholePart = raw.substring(wholeStart, wholeEnd);

                    numerStart = firstSlash + 1;
                    numerEnd = secondSlash;
                    numerPart = raw.substring(numerStart, numerEnd);

                    denomStart = secondSlash + 1;
                    denomEnd = fracEnd;
                    denomPart = raw.substring(denomStart, denomEnd);
                } else {
                    // Simple fraction: numer / denom
                    numerStart = i;
                    numerEnd = firstSlash;
                    numerPart = raw.substring(numerStart, numerEnd);

                    denomStart = firstSlash + 1;
                    denomEnd = fracEnd;
                    denomPart = raw.substring(denomStart, denomEnd);
                }

                // Append whole part if present
                if (wholePart != null) {
                    for (int p = 0; p < wholePart.length(); p++) {
                        if (rawCursorIndex == wholeStart + p) formattedCursor = plainBuilder.length();
                        char wc = wholePart.charAt(p);
                        plainBuilder.append(wc);
                        taggedBuilder.append(wc);
                        printedSizes.add(1);
                    }
                    if (rawCursorIndex == wholeEnd) formattedCursor = plainBuilder.length();
                    // Half-width space between whole and fraction
                    plainBuilder.append(" ");
                    taggedBuilder.append("<hw> </hw>");
                    printedSizes.add(1);
                }

                // Numerator (styled with <sup>...</sup>)
                taggedBuilder.append("<sup>");
                if (numerPart.isEmpty()) {
                    taggedBuilder.append("<dim>0</dim>");
                } else {
                    for (int p = 0; p < numerPart.length(); p++) {
                        if (rawCursorIndex == numerStart + p) formattedCursor = plainBuilder.length();
                        char nc = numerPart.charAt(p);
                        plainBuilder.append(nc);
                        taggedBuilder.append(nc);
                        printedSizes.add(1);
                    }
                }
                taggedBuilder.append("</sup>");

                if (rawCursorIndex == firstSlash) formattedCursor = plainBuilder.length();

                // Fraction bar
                plainBuilder.append("/");
                taggedBuilder.append("/");
                printedSizes.add(1);

                if (secondSlash != -1 && rawCursorIndex == secondSlash) formattedCursor = plainBuilder.length();

                // Denominator (styled with <sf>...</sf>)
                taggedBuilder.append("<sf>");
                if (denomPart.isEmpty()) {
                    taggedBuilder.append("<dim>1</dim>");
                } else {
                    for (int p = 0; p < denomPart.length(); p++) {
                        if (rawCursorIndex == denomStart + p) formattedCursor = plainBuilder.length();
                        char dc = denomPart.charAt(p);
                        plainBuilder.append(dc);
                        taggedBuilder.append(dc);
                        printedSizes.add(1);
                    }
                }
                taggedBuilder.append("</sf>");

                if (rawCursorIndex == fracEnd) formattedCursor = plainBuilder.length();

                i = fracEnd;
                continue;
            }

            if (Character.isDigit(ch) || ch == '.') {
                int start = i;
                while (i < rawLen && (Character.isDigit(raw.charAt(i)) || raw.charAt(i) == '.')) {
                    i++;
                }
                String numToken = raw.substring(start, i);

                String preparedToken = numToken;
                if (!decimalSeparator.equals(".")) {
                    preparedToken = preparedToken.replace(".", decimalSeparator);
                }

                String formattedToken = formatNumberWithGrouping(preparedToken, 3, groupingSeparator, decimalSeparator);

                int searchOffset = 0;
                for (int pos = 0; pos < numToken.length(); pos++) {
                    int currentRawIndex = start + pos;
                    if (rawCursorIndex == currentRawIndex) {
                        formattedCursor = plainBuilder.length();
                    }

                    char rawDigit = numToken.charAt(pos);
                    String rawCharStr = (rawDigit == '.' && !decimalSeparator.equals(".")) ? decimalSeparator : String.valueOf(rawDigit);

                    boolean isLast = (pos == numToken.length() - 1);
                    int iIndexOf = formattedToken.indexOf(rawCharStr, searchOffset);
                    int length = iIndexOf < 0 ? 0 : (isLast ? formattedToken.length() : iIndexOf + 1) - searchOffset;

                    printedSizes.add(length);

                    String chunk = formattedToken.substring(searchOffset, searchOffset + length);
                    plainBuilder.append(chunk);
                    taggedBuilder.append(chunk);

                    searchOffset += length;
                }

                if (rawCursorIndex == i) {
                    formattedCursor = plainBuilder.length();
                }
            } else if (ch == '^') {
                // Exponent power operator: format following digits with <sup>...</sup>
                if (rawCursorIndex == i) {
                    formattedCursor = plainBuilder.length();
                }
                printedSizes.add(0); // Caret symbol itself is hidden when formatted as superscript
                i++; // skip '^'

                int expStart = i;
                while (i < rawLen && (Character.isDigit(raw.charAt(i)) || raw.charAt(i) == '-' || raw.charAt(i) == '.')) {
                    i++;
                }
                String expToken = raw.substring(expStart, i);

                taggedBuilder.append("<sup>");
                if (expToken.isEmpty()) {
                    taggedBuilder.append("<dim>□</dim>");
                    plainBuilder.append(" ");
                    printedSizes.add(1);
                    if (rawCursorIndex == expStart) {
                        formattedCursor = plainBuilder.length();
                    }
                } else {
                    for (int pos = 0; pos < expToken.length(); pos++) {
                        int curIdx = expStart + pos;
                        if (rawCursorIndex == curIdx) {
                            formattedCursor = plainBuilder.length();
                        }
                        char digitChar = expToken.charAt(pos);
                        plainBuilder.append(digitChar);
                        taggedBuilder.append(digitChar);
                        printedSizes.add(1);
                    }
                    if (rawCursorIndex == i) {
                        formattedCursor = plainBuilder.length();
                    }
                }
                taggedBuilder.append("</sup>");
            } else if (ch == '²') {
                // Square: format with <sup>2</sup> for consistent beautiful rendering
                if (rawCursorIndex == i) {
                    formattedCursor = plainBuilder.length();
                }
                plainBuilder.append("²");
                taggedBuilder.append("<sup>2</sup>");
                printedSizes.add(1);
                i++;
                if (rawCursorIndex == i) {
                    formattedCursor = plainBuilder.length();
                }
            } else {
                if (rawCursorIndex == i) {
                    formattedCursor = plainBuilder.length();
                }
                plainBuilder.append(ch);
                printedSizes.add(1);

                // Add markup tags like CalcTastic: <o> for operators, <p> for parentheses
                if (ch == '(' || ch == ')') {
                    taggedBuilder.append("<p>").append(ch).append("</p>");
                } else if (ch == '+' || ch == '-' || ch == '−' || ch == '×' || ch == '*' || ch == '÷' || ch == '/' || ch == '√' || ch == '%' || ch == '=') {
                    taggedBuilder.append("<o>").append(ch).append("</o>");
                } else {
                    taggedBuilder.append(ch);
                }
                i++;
            }
        }

        if (rawCursorIndex >= rawLen) {
            formattedCursor = plainBuilder.length();
        }

        return new FormattedResult(plainBuilder.toString(), taggedBuilder.toString(), formattedCursor, printedSizes);
    }

    /**
     * Map tap position on screen back to raw cursor index
     * using the exact loop from CalcTastic's p012g0.f.onSingleTapUp
     */
    public static int mapOffsetToRawIndex(List<Integer> printedSizes, int offsetForPosition) {
        if (printedSizes == null || printedSizes.isEmpty()) {
            return 0;
        }
        int iIntValue;
        int i2 = 0;
        int i3 = 0;
        while (i2 < printedSizes.size() && i3 < offsetForPosition && 
               ((iIntValue = printedSizes.get(i2).intValue() + i3) <= offsetForPosition || 
                offsetForPosition - i3 >= iIntValue - offsetForPosition)) {
            i2++;
            i3 = iIntValue;
        }
        return i2;
    }
}
