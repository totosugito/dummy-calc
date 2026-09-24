package com.calctastic.sample;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class SimpleCalculatorActivity extends Activity {

    public static class HistoryEntry {
        public String expression;
        public String result;
        public String symbol;

        public HistoryEntry(String expr, String res, String sym) {
            this.expression = expr;
            this.result = res;
            this.symbol = sym;
        }
    }

    private ListView mListView;
    private DisplayAdapter mAdapter;
    private final List<HistoryEntry> mHistory = new ArrayList<>();
    
    private final StringBuilder mCurrentInput = new StringBuilder();
    private int mCursorIndex = 0;
    private String mLiveResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_calculator);

        mListView = findViewById(R.id.calc_screen_list);
        mAdapter = new DisplayAdapter();
        mListView.setAdapter(mAdapter);

        // Preload sample history with thousand and decimal separators formatted
        String hist1Expr = NumberFormatHelper.formatEquation("128456 × 4", 0).taggedText;
        String hist1Res = NumberFormatHelper.formatEquation("513824", 0).taggedText;
        mHistory.add(new HistoryEntry(hist1Expr, hist1Res, "="));

        String hist2Expr = NumberFormatHelper.formatEquation("√(144000) + 2500.75", 0).taggedText;
        String hist2Res = NumberFormatHelper.formatEquation("2879.41", 0).taggedText;
        mHistory.add(new HistoryEntry(hist2Expr, hist2Res, "="));

        setupKeypad();
        updateDisplay();
    }

    private void setupKeypad() {
        int[] allButtons = {
            // Row 1: Memory
            R.id.btn_mem_plus, R.id.btn_mem_minus, R.id.btn_mem_clear, R.id.btn_mem_save, R.id.btn_mem_recall,
            // Row 2: Functions: a/b, √x, %, (, )
            R.id.btn_fraction, R.id.btn_sqrt, R.id.btn_percent, R.id.btn_paren_open, R.id.btn_paren_close,
            // Row 3: Auxiliary & Cursors: 1/x, x², yˣ, ◀, ▶
            R.id.btn_reciprocal, R.id.btn_square, R.id.btn_power, R.id.btn_cursor_left, R.id.btn_cursor_right,
            // Row 4: 7, 8, 9, DEL, CLR
            R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_delete, R.id.btn_clear,
            // Row 5: 4, 5, 6, ×, ÷
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_mul, R.id.btn_div,
            // Row 6: 1, 2, 3, +, −
            R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_add, R.id.btn_sub,
            // Row 7: 0, ., ±, =
            R.id.btn_0, R.id.btn_dot, R.id.btn_negate, R.id.btn_equals
        };

        for (int id : allButtons) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setFocusable(false);
                btn.setFocusableInTouchMode(false);
            }
        }

        // Memory buttons: As requested, ignore click events (just display buttons)
        int[] memButtons = {
            R.id.btn_mem_plus, R.id.btn_mem_minus, R.id.btn_mem_clear, R.id.btn_mem_save, R.id.btn_mem_recall
        };
        for (int id : memButtons) {
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    // Click ignored as requested
                });
            }
        }

        // Number buttons: 0 - 9 and dot
        int[] numButtons = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_dot
        };
        for (int id : numButtons) {
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> insertText(((Button) v).getText().toString()));
            }
        }

        // Binary Operator buttons (wrapped in spaces for equation parsing)
        int[] opButtons = {
            R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div
        };
        for (int id : opButtons) {
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> insertText(" " + ((Button) v).getText().toString() + " "));
            }
        }

        // Power (y^x)
        Button btnPower = findViewById(R.id.btn_power);
        if (btnPower != null) {
            btnPower.setOnClickListener(v -> insertText("^"));
        }

        // Fraction button: a/b (inserts "/" without spaces for fraction format)
        Button btnFraction = findViewById(R.id.btn_fraction);
        if (btnFraction != null) {
            btnFraction.setOnClickListener(v -> insertText("/"));
        }

        // Parentheses, Percent
        Button btnParenOpen = findViewById(R.id.btn_paren_open);
        if (btnParenOpen != null) {
            btnParenOpen.setOnClickListener(v -> insertText("("));
        }

        Button btnParenClose = findViewById(R.id.btn_paren_close);
        if (btnParenClose != null) {
            btnParenClose.setOnClickListener(v -> insertText(")"));
        }

        Button btnPercent = findViewById(R.id.btn_percent);
        if (btnPercent != null) {
            btnPercent.setOnClickListener(v -> insertText("%"));
        }

        // Sqrt (√x): inserts "√("
        Button btnSqrt = findViewById(R.id.btn_sqrt);
        if (btnSqrt != null) {
            btnSqrt.setOnClickListener(v -> insertText("√("));
        }

        // Square: inserts "²"
        Button btnSquare = findViewById(R.id.btn_square);
        if (btnSquare != null) {
            btnSquare.setOnClickListener(v -> insertText("²"));
        }

        // Reciprocal: inserts "1 / "
        Button btnReciprocal = findViewById(R.id.btn_reciprocal);
        if (btnReciprocal != null) {
            btnReciprocal.setOnClickListener(v -> insertText("1 / "));
        }

        // Cursor navigation buttons: ◀ and ▶
        Button btnCursorLeft = findViewById(R.id.btn_cursor_left);
        if (btnCursorLeft != null) {
            btnCursorLeft.setOnClickListener(v -> {
                if (mCursorIndex > 0) {
                    mCursorIndex--;
                    updateDisplay();
                }
            });
        }

        Button btnCursorRight = findViewById(R.id.btn_cursor_right);
        if (btnCursorRight != null) {
            btnCursorRight.setOnClickListener(v -> {
                if (mCursorIndex < mCurrentInput.length()) {
                    mCursorIndex++;
                    updateDisplay();
                }
            });
        }

        // Negate: ±
        Button btnNegate = findViewById(R.id.btn_negate);
        if (btnNegate != null) {
            btnNegate.setOnClickListener(v -> {
                // If cursor is at start or after operator, insert "-"
                insertText("-");
            });
        }

        // Clear
        Button btnClear = findViewById(R.id.btn_clear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                mCurrentInput.setLength(0);
                mCursorIndex = 0;
                mLiveResult = "";
                updateDisplay();
            });
        }

        // Delete (Backspace)
        Button btnDelete = findViewById(R.id.btn_delete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (mCursorIndex > 0 && mCurrentInput.length() > 0) {
                    mCurrentInput.deleteCharAt(mCursorIndex - 1);
                    mCursorIndex--;
                    evaluateLive();
                    updateDisplay();
                }
            });
        }

        // Equals
        Button btnEquals = findViewById(R.id.btn_equals);
        if (btnEquals != null) {
            btnEquals.setOnClickListener(v -> {
                if (mCurrentInput.length() > 0) {
                    String res = evaluateExpression(mCurrentInput.toString());
                    String formattedInput = NumberFormatHelper.formatEquation(mCurrentInput.toString().trim(), 0).taggedText;
                    String formattedRes = NumberFormatHelper.formatEquation(res, 0).taggedText;
                    mHistory.add(new HistoryEntry(formattedInput, formattedRes, "="));
                    mCurrentInput.setLength(0);
                    mCursorIndex = 0;
                    mLiveResult = "";
                    updateDisplay();
                    mListView.setSelection(mAdapter.getCount() - 1);
                }
            });
        }
    }

    private void insertText(String text) {
        mCurrentInput.insert(mCursorIndex, text);
        mCursorIndex += text.length();
        evaluateLive();
        updateDisplay();
    }

    private void evaluateLive() {
        if (mCurrentInput.length() == 0) {
            mLiveResult = "";
            return;
        }
        try {
            String eval = evaluateExpression(mCurrentInput.toString());
            if (eval != null && !eval.isEmpty() && !eval.equals(mCurrentInput.toString().trim())) {
                String formattedLive = NumberFormatHelper.formatEquation(eval, 0).taggedText;
                mLiveResult = "<o>=</o> " + formattedLive;
            } else {
                mLiveResult = "";
            }
        } catch (Exception e) {
            mLiveResult = "";
        }
    }

    private String evaluateExpression(String raw) {
        try {
            String expr = raw.replaceAll("×", "*")
                             .replaceAll("÷", "/")
                             .replaceAll("−", "-")
                             .replaceAll("π", String.valueOf(Math.PI))
                             .replaceAll("²", "^2")
                             .trim();

            // Evaluate simple square root pattern √(x)
            while (expr.contains("√(")) {
                int start = expr.indexOf("√(");
                int end = expr.indexOf(")", start);
                if (end > start) {
                    String inner = expr.substring(start + 2, end).trim();
                    double val = Double.parseDouble(inner);
                    expr = expr.substring(0, start) + Math.sqrt(val) + expr.substring(end + 1);
                } else {
                    break;
                }
            }

            // Check if entire expr is a single fraction: whole/numer/denom or numer/denom
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
                        long totalNum = (w * d) + n;
                        return w + "/" + n + "/" + d;
                    }
                }
            }

            // Evaluate compact power patterns like a^b (e.g. 2^3 or 5^2)
            while (expr.contains("^")) {
                int caretIdx = expr.indexOf("^");
                // find base left of caret
                int baseStart = caretIdx - 1;
                while (baseStart >= 0 && (Character.isDigit(expr.charAt(baseStart)) || expr.charAt(baseStart) == '.')) {
                    baseStart--;
                }
                baseStart++;

                // find exp right of caret
                int expEnd = caretIdx + 1;
                if (expEnd < expr.length() && expr.charAt(expEnd) == '-') expEnd++;
                while (expEnd < expr.length() && (Character.isDigit(expr.charAt(expEnd)) || expr.charAt(expEnd) == '.')) {
                    expEnd++;
                }

                if (baseStart < caretIdx && expEnd > caretIdx + 1) {
                    double baseVal = Double.parseDouble(expr.substring(baseStart, caretIdx));
                    double expVal = Double.parseDouble(expr.substring(caretIdx + 1, expEnd));
                    double powRes = Math.pow(baseVal, expVal);
                    String powResStr = (powRes == (long) powRes) ? String.valueOf((long) powRes) : String.valueOf(powRes);
                    expr = expr.substring(0, baseStart) + powResStr + expr.substring(expEnd);
                } else {
                    break;
                }
            }

            // Simple tokens evaluation or multi-step
            String[] tokens = expr.split("\\s+");
            if (tokens.length >= 3) {
                double a = parseNumberOrFraction(tokens[0]);
                int idx = 1;
                while (idx + 1 < tokens.length) {
                    String op = tokens[idx];
                    double b = parseNumberOrFraction(tokens[idx + 1]);
                    if ("+".equals(op)) a = a + b;
                    else if ("-".equals(op)) a = a - b;
                    else if ("*".equals(op)) a = a * b;
                    else if ("/".equals(op)) a = (b != 0) ? a / b : 0;
                    else if ("^".equals(op)) a = Math.pow(a, b);
                    idx += 2;
                }
                return (a == (long) a) ? String.format("%d", (long) a) : String.format("%s", a);
            }
            return expr;
        } catch (Exception e) {
            return raw;
        }
    }

    private double parseNumberOrFraction(String token) {
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

    private void updateDisplay() {
        mAdapter.notifyDataSetChanged();
    }

    private class DisplayAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mHistory.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            if (position < mHistory.size()) {
                return mHistory.get(position);
            }
            return mCurrentInput.toString();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.history_dialog_list_item, parent, false);
            }

            TextView symbolView = view.findViewById(R.id.history_symbol);
            TextView dividerView = view.findViewById(R.id.history_divider);
            TextView historyCalc = view.findViewById(R.id.history_calculation);
            VerticalListEditText currentCalc = view.findViewById(R.id.current_calculation);
            TextView resultView = view.findViewById(R.id.history_result);

            symbolView.setTypeface(CalcTypefaceHelper.getSymbolAndLabelFont(parent.getContext()));
            dividerView.setTypeface(CalcTypefaceHelper.getSymbolAndLabelFont(parent.getContext()));
            historyCalc.setTypeface(CalcTypefaceHelper.getScreenCalculationFont(parent.getContext()));
            currentCalc.setTypeface(CalcTypefaceHelper.getScreenCalculationFont(parent.getContext()));
            resultView.setTypeface(CalcTypefaceHelper.getScreenCalculationFont(parent.getContext()));

            boolean isActiveLine = (position == mHistory.size());

            if (isActiveLine) {
                dividerView.setVisibility(View.VISIBLE);
                historyCalc.setVisibility(View.GONE);
                currentCalc.setVisibility(View.VISIBLE);

                // Format live expression with thousand and decimal separators, plus colored symbols
                NumberFormatHelper.FormattedResult formatted = 
                        NumberFormatHelper.formatEquation(mCurrentInput.toString(), mCursorIndex);

                // Apply CalcSpannableFormatter to get colored operators and parentheses
                SpannableStringBuilder coloredSpannable = CalcSpannableFormatter.format(formatted.taggedText);
                currentCalc.setText(coloredSpannable);
                currentCalc.setCursorVisible(true);

                // Set orange cursor drawable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    GradientDrawable cursor = new GradientDrawable();
                    cursor.setShape(GradientDrawable.RECTANGLE);
                    cursor.setColor(Color.parseColor("#FF9800"));
                    cursor.setSize(6, 48);
                    currentCalc.setTextCursorDrawable(cursor);
                }

                if (formatted.cursorPosition <= coloredSpannable.length()) {
                    currentCalc.setSelection(formatted.cursorPosition);
                }

                currentCalc.post(() -> {
                    currentCalc.requestFocus();
                });

                // Tap to reposition cursor, mapped via printed sizes
                currentCalc.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_DOWN) {
                        currentCalc.requestFocus();
                        int offset = currentCalc.getOffsetForPosition(event.getX(), event.getY());
                        int rawIdx = NumberFormatHelper.mapOffsetToRawIndex(formatted.printedSizes, offset);
                        if (rawIdx >= 0 && rawIdx <= mCurrentInput.length()) {
                            mCursorIndex = rawIdx;
                            updateDisplay();
                        }
                    }
                    return true;
                });

                resultView.setText(CalcSpannableFormatter.format(mLiveResult));
            } else {
                dividerView.setVisibility(View.GONE);
                currentCalc.setVisibility(View.GONE);
                historyCalc.setVisibility(View.VISIBLE);

                HistoryEntry entry = mHistory.get(position);
                symbolView.setText(entry.symbol);
                historyCalc.setText(CalcSpannableFormatter.format(entry.expression));
                resultView.setText(CalcSpannableFormatter.format(entry.result));
                currentCalc.setOnTouchListener(null);
            }

            return view;
        }
    }
}
