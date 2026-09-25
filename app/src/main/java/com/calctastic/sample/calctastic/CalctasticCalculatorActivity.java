package com.calctastic.sample.calctastic;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.calctastic.sample.R;
import com.calctastic.sample.calctastic.dialog.ConstantsDialog;
import com.calctastic.sample.calctastic.dialog.ConversionDialog;
import com.calctastic.sample.calctastic.dialog.Statistic;
import com.calctastic.sample.calctastic.dialog.StatisticsDialog;
import com.calctastic.sample.calctastic.expression.CalcSpannableFormatter;
import com.calctastic.sample.calctastic.expression.CalcTokens;
import com.calctastic.sample.calctastic.expression.DmsHelper;
import com.calctastic.sample.calctastic.expression.ExpressionDecorator;
import com.calctastic.sample.calctastic.expression.ExpressionEvaluator;
import com.calctastic.sample.calctastic.expression.NumberFormatHelper;
import com.calctastic.sample.calctastic.memory.CalculatorMemory;
import com.calctastic.sample.calctastic.ui.CalcFontSizeHelper;
import com.calctastic.sample.calctastic.ui.CalcTypefaceHelper;
import com.calctastic.sample.calctastic.ui.VerticalListEditText;
import java.util.ArrayList;
import java.util.List;

public class CalctasticCalculatorActivity extends Activity {

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

    private View mSimpleKeypad;
    private View mScientificKeypad;
    private Button mModeToggle;
    private boolean mScientificMode = false;
    private boolean mShiftActive = false;
    private boolean mHyperbolic = false;
    private String mAngleUnit = "DEG";   // AngleUnit: DEG / RAD / GRD
    private String mNotation = "FIX";    // DecimalNotation: FIX / SCI / ENG
    private int mDecimalPrecision = 12;  // CalculatorManager.decimalPrecision default
    private final ExpressionEvaluator mEvaluator = new ExpressionEvaluator();
    private final CalculatorMemory mMemory = new CalculatorMemory();

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

        String hist3Expr = NumberFormatHelper.formatEquation("987654 / 3", 0).taggedText;
        String hist3Res = NumberFormatHelper.formatEquation("329218", 0).taggedText;
        mHistory.add(new HistoryEntry(hist3Expr, hist3Res, "="));

        String hist4Expr = NumberFormatHelper.formatEquation("123456789 + 987654321 × 111222333 + 444555666", 0).taggedText;
        String hist4Res = "111,222,333,444,555,666,777,888,999,000,111,222,333";
        mHistory.add(new HistoryEntry(hist4Expr, hist4Res, "="));

        setupModeToggle();
        setupKeypad();
        setupScientificKeypad();
        applyKeypadFonts();
        updateDisplay();
    }

    /**
     * Per-button typefaces from CalculatorCommand.keyboardFont (f.java:31–41):
     * MONO1=roboto_mono, SANS1=inter_medium, SANS2=inter_regular,
     * SERI1=stix_two, SERI2=hepta_slab.
     */
    private void applyKeypadFonts() {
        // MONO1 — ( ) i
        applyFont(R.id.sci_paren_open, "MONO1");
        applyFont(R.id.sci_paren_close, "MONO1");
        applyFont(R.id.sci_i, "MONO1");
        applyFont(R.id.btn_paren_open, "MONO1");
        applyFont(R.id.btn_paren_close, "MONO1");

        // SANS2 — digits
        int[] digits = {
            R.id.sci_0, R.id.sci_1, R.id.sci_2, R.id.sci_3, R.id.sci_4,
            R.id.sci_5, R.id.sci_6, R.id.sci_7, R.id.sci_8, R.id.sci_9,
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };
        for (int id : digits) applyFont(id, "SANS2");

        // SERI1 — π ± + − × ÷ =
        int[] serOp = {
            R.id.sci_pi, R.id.sci_negate, R.id.sci_add, R.id.sci_sub,
            R.id.sci_mul, R.id.sci_div, R.id.sci_equals,
            R.id.btn_negate, R.id.btn_add, R.id.btn_sub, R.id.btn_mul,
            R.id.btn_div, R.id.btn_equals
        };
        for (int id : serOp) applyFont(id, "SERI1");

        // SERI2 — a/b x² yˣ 1/x √ .
        int[] serSlab = {
            R.id.sci_fraction, R.id.sci_square, R.id.sci_power,
            R.id.sci_reciprocal, R.id.sci_dot,
            R.id.btn_fraction, R.id.btn_sqrt, R.id.btn_square,
            R.id.btn_power, R.id.btn_reciprocal, R.id.btn_dot
        };
        for (int id : serSlab) applyFont(id, "SERI2");

        // SANS1 — function/control labels
        int[] sans1 = {
            R.id.sci_shift, R.id.sci_drg, R.id.sci_fse, R.id.sci_ms, R.id.sci_mr,
            R.id.sci_sin, R.id.sci_cos, R.id.sci_tan, R.id.sci_ln, R.id.sci_log,
            R.id.sci_percent, R.id.sci_delete, R.id.sci_clear, R.id.sci_eex,
            R.id.sci_cursor_left, R.id.sci_cursor_right,
            R.id.btn_percent, R.id.btn_delete, R.id.btn_clear,
            R.id.btn_cursor_left, R.id.btn_cursor_right,
            R.id.btn_mem_plus, R.id.btn_mem_minus, R.id.btn_mem_clear,
            R.id.btn_mem_save, R.id.btn_mem_recall
        };
        for (int id : sans1) applyFont(id, "SANS1");
    }

    private void applyFont(int viewId, String fontId) {
        View v = findViewById(viewId);
        if (v instanceof android.widget.TextView) {
            CalcTypefaceHelper.applyFontId((android.widget.TextView) v, fontId, 0);
        }
    }

    private void setupModeToggle() {
        mSimpleKeypad = findViewById(R.id.simple_keypad);
        mScientificKeypad = findViewById(R.id.scientific_keypad);
        mModeToggle = findViewById(R.id.btn_mode_toggle);
        if (mModeToggle != null) {
            mModeToggle.setOnClickListener(v -> {
                mScientificMode = !mScientificMode;
                mShiftActive = false;
                refreshShiftLabels();
                if (mSimpleKeypad != null) {
                    mSimpleKeypad.setVisibility(mScientificMode ? View.GONE : View.VISIBLE);
                }
                if (mScientificKeypad != null) {
                    mScientificKeypad.setVisibility(mScientificMode ? View.VISIBLE : View.GONE);
                }
                mModeToggle.setText(mScientificMode ? "SIM" : "SCI");
            });
        }
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

        // Memory row (simple keypad): same side-effects as original MEMORY_* commands
        Button memPlus = findViewById(R.id.btn_mem_plus);
        if (memPlus != null) memPlus.setOnClickListener(v -> mMemory.memoryPlusMinus(true, mLiveResult, mCurrentInput.toString()));
        Button memMinus = findViewById(R.id.btn_mem_minus);
        if (memMinus != null) memMinus.setOnClickListener(v -> mMemory.memoryPlusMinus(false, mLiveResult, mCurrentInput.toString()));
        Button memClear = findViewById(R.id.btn_mem_clear);
        if (memClear != null) memClear.setOnClickListener(v -> mMemory.value = null);
        Button memSave = findViewById(R.id.btn_mem_save);
        if (memSave != null) memSave.setOnClickListener(v -> mMemory.memorySave(mLiveResult, mCurrentInput.toString()));
        Button memRecall = findViewById(R.id.btn_mem_recall);
        if (memRecall != null) memRecall.setOnClickListener(v -> {
            if (mMemory.value != null && !mMemory.value.isEmpty()) insertText(mMemory.value);
        });

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

        // SQRT plain="√" — ordinal 89, NOT in auto-paren set 68–86
        Button btnSqrt = findViewById(R.id.btn_sqrt);
        if (btnSqrt != null) {
            btnSqrt.setOnClickListener(v -> insertText("√"));
        }

        // SQUARE plain="²" — ordinal 88, no auto "("
        Button btnSquare = findViewById(R.id.btn_square);
        if (btnSquare != null) {
            btnSquare.setOnClickListener(v -> insertText("²"));
        }

        // RECIPROCAL plain="1/" — ordinal 92, no auto "("
        Button btnReciprocal = findViewById(R.id.btn_reciprocal);
        if (btnReciprocal != null) {
            btnReciprocal.setOnClickListener(v -> insertText("1/"));
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

        // Delete (Backspace) — same entry rules as scientific DEL
        Button btnDelete = findViewById(R.id.btn_delete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> backspaceEntry());
        }

        // Equals
        Button btnEquals = findViewById(R.id.btn_equals);
        if (btnEquals != null) {
            btnEquals.setOnClickListener(v -> performEqualsCalculation());
        }
    }

    /**
     * Scientific keypad wiring.
     * Insert strings come from CalculatorCommand.equationStringPlain in the
     * decompiled original (raw/.../CalculatorCommand.java). Shift pairs follow
     * keyboard_portrait_full.xml + SCIENTIFIC_DESIGN.md (SHIFT_BUTTON_* labels).
     */
    private void setupScientificKeypad() {
        int[] allSciButtons = {
            R.id.sci_shift, R.id.sci_i, R.id.sci_drg, R.id.sci_fse, R.id.sci_ms, R.id.sci_mr,
            R.id.sci_fraction, R.id.sci_sin, R.id.sci_cos, R.id.sci_tan, R.id.sci_paren_open, R.id.sci_paren_close,
            R.id.sci_reciprocal, R.id.sci_pi, R.id.sci_ln, R.id.sci_log, R.id.sci_cursor_left, R.id.sci_cursor_right,
            R.id.sci_square, R.id.sci_7, R.id.sci_8, R.id.sci_9, R.id.sci_delete, R.id.sci_clear,
            R.id.sci_power, R.id.sci_4, R.id.sci_5, R.id.sci_6, R.id.sci_mul, R.id.sci_div,
            R.id.sci_percent, R.id.sci_1, R.id.sci_2, R.id.sci_3, R.id.sci_add, R.id.sci_sub,
            R.id.sci_eex, R.id.sci_0, R.id.sci_dot, R.id.sci_negate, R.id.sci_equals
        };
        for (int id : allSciButtons) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setFocusable(false);
                btn.setFocusableInTouchMode(false);
            }
        }

        // KEYBOARD_SHIFT — toggles 2nd/shift mode (no equation string)
        wireSci(R.id.sci_shift, () -> {
            mShiftActive = !mShiftActive;
            refreshShiftLabels();
        });

        // ANGLE_UNIT (DRG): cycle DEG → RAD → GRD (AngleUnit enum in original)
        // HYPERBOLIC (HYP): shift toggles hyperbolic flag on trig labels
        Button btnDrg = findViewById(R.id.sci_drg);
        if (btnDrg != null) {
            btnDrg.setOnClickListener(v -> {
                if (mShiftActive) {
                    mShiftActive = false;
                    mHyperbolic = !mHyperbolic;
                    refreshShiftLabels();
                    return;
                }
                if ("DEG".equals(mAngleUnit)) mAngleUnit = "RAD";
                else if ("RAD".equals(mAngleUnit)) mAngleUnit = "GRD";
                else mAngleUnit = "DEG";
                btnDrg.setText(mAngleUnit);
            });
        }

        // NOTATION (FSE): cycle FIX → SCI → ENG (DecimalNotation.f / .b())
        // PRECISION (shift): label "P:"+decimalPrecision (f.java case 111); dialog later
        Button btnFse = findViewById(R.id.sci_fse);
        if (btnFse != null) {
            btnFse.setOnClickListener(v -> {
                if (mShiftActive) {
                    mShiftActive = false;
                    refreshShiftLabels();
                    // PRECISION dialog placeholder — keep showing P:n then restore on next refresh
                    return;
                }
                if ("FIX".equals(mNotation)) mNotation = "SCI";
                else if ("SCI".equals(mNotation)) mNotation = "ENG";
                else mNotation = "FIX";
                btnFse.setText(mNotation);
            });
        }

        // MEMORY_SAVE stores current operand to register 0 (Calculator.java case 44)
        wireSci(R.id.sci_ms, () -> {
            if (consumeShift()) return;
            mMemory.memorySave(mLiveResult, mCurrentInput.toString());
        });
        wireSci(R.id.sci_mr, () -> {
            if (consumeShift()) return;
            if (mMemory.value != null && !mMemory.value.isEmpty()) {
                insertText(mMemory.value);
            }
        });

        // Numbers + decimal: equationStringPlain == keyboard digit
        int[] sciNumButtons = {
            R.id.sci_0, R.id.sci_1, R.id.sci_2, R.id.sci_3, R.id.sci_4,
            R.id.sci_5, R.id.sci_6, R.id.sci_7, R.id.sci_8, R.id.sci_9,
            R.id.sci_dot
        };
        for (int id : sciNumButtons) {
            final int buttonId = id;
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    String shiftPlain = digitShiftPlain(buttonId);
                    if (shiftPlain != null && mShiftActive) {
                        mShiftActive = false;
                        refreshShiftLabels();
                        if (shiftPlain.isEmpty()) {
                            // CONST (shift 0) / CONV (shift .) dialogs
                            if (buttonId == R.id.sci_0) {
                                showConstantsDialog();
                            } else if (buttonId == R.id.sci_dot) {
                                showConversionDialog();
                            }
                            return;
                        }
                        // ceil/floor/abs/arg/re/im are ordinal 68–86 → auto "("
                        insertFunction(shiftPlain);
                        return;
                    }
                    insertText(((Button) v).getText().toString());
                });
            }
        }

        // Binary operators — plain strings include surrounding spaces
        // Shift +: MEMORY_PLUS — memory[0] += current operand (Calculator.java case 48)
        wireSci(R.id.sci_add, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); mMemory.memoryPlusMinus(true, mLiveResult, mCurrentInput.toString()); return; }
            insertText(" + ");
        });
        // Shift −: MEMORY_MINUS — memory[0] −= current operand (Calculator.java case 49)
        wireSci(R.id.sci_sub, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); mMemory.memoryPlusMinus(false, mLiveResult, mCurrentInput.toString()); return; }
            insertText(" − ");
        });
        // × shift → COMPLEX_CONJUGATE plain="conj" + auto "(" (ordinal 80)
        wireSci(R.id.sci_mul, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertFunction("conj"); return; }
            insertText(" × ");
        });
        wireSci(R.id.sci_div, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText(" mod "); return; } // MODULO
            insertText(" / ");
        });

        // FRACTION plain="/"; shift DMS — convert current number to D°M'S" (case 103)
        wireSci(R.id.sci_fraction, () -> {
            if (mShiftActive) {
                mShiftActive = false;
                refreshShiftLabels();
                convertCurrentToDms();
                return;
            }
            insertText("/");
        });

        // Trig / ln / log: Equation.T() ordinal 68–86 auto-appends "(" after function.
        wireSci(R.id.sci_sin, () -> insertShiftableFunc("sin", "asin"));
        wireSci(R.id.sci_cos, () -> insertShiftableFunc("cos", "acos"));
        wireSci(R.id.sci_tan, () -> insertShiftableFunc("tan", "atan"));

        // COMPLEX_RECT plain="i"; shift COMPLEX_POLAR plain="∠"
        wireSci(R.id.sci_i, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText("∠"); return; }
            insertText("i");
        });

        wireSci(R.id.sci_paren_open, () -> insertText("("));
        wireSci(R.id.sci_paren_close, () -> insertText(")"));

        // RECIPROCAL plain="1/"
        wireSci(R.id.sci_reciprocal, () -> {
            if (consumeShift()) return; // design: no shift on 1/x in portrait_full? label empty
            insertText("1/");
        });
        // CONST_PI plain="π"; shift CONST_E plain="e"
        wireSci(R.id.sci_pi, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText("e"); return; }
            insertText("π");
        });
        // LOG_E plain="ln" + auto "(" (ordinal 71); shift EXP_E plain="e^" — ordinal 90, NO auto "("
        wireSci(R.id.sci_ln, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText("e^"); return; }
            insertFunction("ln");
        });
        // LOG_10 plain="log" + auto "(" (ordinal 72); shift EXP_10 plain="10^" — NO auto "("
        wireSci(R.id.sci_log, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText("10^"); return; }
            insertFunction("log");
        });

        // CURSOR_LEFT / CURSOR_RIGHT; shift HOME / END (no equation string)
        Button sciCursorLeft = findViewById(R.id.sci_cursor_left);
        if (sciCursorLeft != null) {
            sciCursorLeft.setOnClickListener(v -> {
                if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); mCursorIndex = 0; updateDisplay(); return; }
                if (mCursorIndex > 0) { mCursorIndex--; updateDisplay(); }
            });
        }
        Button sciCursorRight = findViewById(R.id.sci_cursor_right);
        if (sciCursorRight != null) {
            sciCursorRight.setOnClickListener(v -> {
                if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); mCursorIndex = mCurrentInput.length(); updateDisplay(); return; }
                if (mCursorIndex < mCurrentInput.length()) { mCursorIndex++; updateDisplay(); }
            });
        }

        // SQUARE plain="²"; shift SQRT plain="√"
        wireSci(R.id.sci_square, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText("√"); return; }
            insertText("²");
        });

        // BACKSPACE; shift MEMORY_CLEAR — clear register 0 (Calculator.java case 46)
        Button sciDelete = findViewById(R.id.sci_delete);
        if (sciDelete != null) {
            sciDelete.setOnClickListener(v -> {
                if (mShiftActive) {
                    mShiftActive = false;
                    refreshShiftLabels();
                    mMemory.value = null; // CalcMemory.e(0)
                    return;
                }
                backspaceEntry();
            });
        }

        // CLEAR; shift CLEAR_SCREEN (clears history too)
        Button sciClear = findViewById(R.id.sci_clear);
        if (sciClear != null) {
            sciClear.setOnClickListener(v -> {
                if (mShiftActive) {
                    mShiftActive = false;
                    refreshShiftLabels();
                    mHistory.clear();
                    updateDisplay();
                    return;
                }
                mCurrentInput.setLength(0);
                mCursorIndex = 0;
                mLiveResult = "";
                updateDisplay();
            });
        }

        // POWER plain="^"; shift NTH_ROOT plain="√"
        wireSci(R.id.sci_power, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText("√"); return; }
            insertText("^");
        });

        // PERCENT plain="%"; shift DELTA_PERCENT plain=" Δ% "
        wireSci(R.id.sci_percent, () -> {
            if (mShiftActive) { mShiftActive = false; refreshShiftLabels(); insertText(" Δ% "); return; }
            insertText("%");
        });

        // EXPONENT (EEX) plain="E"
        wireSci(R.id.sci_eex, () -> {
            if (consumeShift()) return;
            insertText("E");
        });
        // NEGATE plain="-"; shift STATISTIC — dialog (H(6))
        wireSci(R.id.sci_negate, () -> {
            if (mShiftActive) {
                mShiftActive = false;
                refreshShiftLabels();
                showStatisticsDialog();
                return;
            }
            insertText("-");
        });

        // EQUALS — evaluate; shift CONST_RAND — random in [0,1), no plain string
        Button sciEquals = findViewById(R.id.sci_equals);
        if (sciEquals != null) {
            sciEquals.setOnClickListener(v -> {
                if (mShiftActive) {
                    mShiftActive = false;
                    refreshShiftLabels();
                    double r = Math.random();
                    insertText(r == (long) r ? String.valueOf((long) r) : String.valueOf(r));
                    return;
                }
                performEqualsCalculation();
            });
        }
    }

    /** Shift plain strings for digit keys (SCIENTIFIC_DESIGN / g.java shift labels). Empty = dialog, no insert. */
    private String digitShiftPlain(int id) {
        if (!mShiftActive) return null;
        if (id == R.id.sci_7) return "ceil";   // CEILING
        if (id == R.id.sci_8) return "re";     // COMPLEX_REAL
        if (id == R.id.sci_9) return "im";     // COMPLEX_IMAGINARY
        if (id == R.id.sci_4) return "floor";  // FLOOR
        if (id == R.id.sci_5) return "abs";    // ABSOLUTE
        if (id == R.id.sci_6) return "arg";    // COMPLEX_ARGUMENT
        if (id == R.id.sci_1) return "!";      // FACTORIAL
        if (id == R.id.sci_2) return " nPr ";  // NPR
        if (id == R.id.sci_3) return " nCr ";  // NCR
        if (id == R.id.sci_0) return "";       // CONST — dialog
        if (id == R.id.sci_dot) return "";     // CONVERT_UNIT — dialog
        return null;
    }

    private void wireSci(int id, Runnable action) {
        Button btn = findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v -> action.run());
        }
    }

    /** Insert function name + auto "(" — mirrors Equation.T() cases 68–86. */
    private void insertFunction(String plain) {
        insertText(CalcTokens.isAutoParenFunction(plain) ? plain + "(" : plain);
    }

    private void insertShiftableFunc(String mainPlain, String shiftPlain) {
        if (mShiftActive) {
            mShiftActive = false;
            refreshShiftLabels();
            insertFunction(shiftPlain);
        } else {
            insertFunction(mainPlain);
        }
    }

    /**
     * Entry-style backspace (string model of Equation.b() + AlgebraicInputHandler case 27).
     * Deleting "(" that belongs to a function also removes the function name (Equation.b:332–334).
     */
    private void backspaceEntry() {
        if (mCursorIndex <= 0 || mCurrentInput.length() == 0) return;
        int end = mCursorIndex;
        String before = mCurrentInput.substring(0, end);
        int start = end - 1;

        // Multi-char operators are one entry
        for (String op : CalcTokens.OPERATOR_TOKENS) {
            if (before.endsWith(op)) {
                start = end - op.length();
                break;
            }
        }

        // Compact postfix tokens (e^, 10^, 1/, ², √, °, !)
        if (start == end - 1) {
            String other = CalcTokens.longestSuffix(CalcTokens.POSTFIX_TOKENS, before);
            if (other != null) {
                start = end - other.length();
            }
        }

        // Equation.b: if deleting "(", and previous entry is a function → delete function too
        if (start == end - 1 && mCurrentInput.charAt(start) == '(') {
            String prefix = mCurrentInput.substring(0, start);
            String func = CalcTokens.longestSuffix(CalcTokens.AUTO_PAREN_FUNCTIONS, prefix);
            if (func != null) {
                start -= func.length();
            }
        }

        // Deleting a bare function name (no paren yet) removes the whole name
        if (start == end - 1 && Character.isLetter(mCurrentInput.charAt(start))) {
            String func = CalcTokens.longestSuffix(CalcTokens.AUTO_PAREN_FUNCTIONS, before);
            if (func != null && before.length() == func.length()
                    || (func != null && before.charAt(before.length() - func.length() - 1) == ' ')) {
                start = end - func.length();
            }
        }

        if (start < 0) start = 0;
        mCurrentInput.delete(start, end);
        mCursorIndex = start;
        evaluateLive();
        updateDisplay();
    }

    private boolean consumeShift() {
        if (mShiftActive) {
            mShiftActive = false;
            refreshShiftLabels();
            return true;
        }
        return false;
    }

    private void refreshShiftLabels() {
        Button shift = findViewById(R.id.sci_shift);
        if (shift != null) {
            shift.setAlpha(mShiftActive ? 1.0f : 0.7f);
        }

        // f.java: FSE main label is DecimalNotation.b(); when shift shows PRECISION command → "P:"+precision
        Button fse = findViewById(R.id.sci_fse);
        if (fse != null) {
            if (mShiftActive) {
                fse.setText("P:" + mDecimalPrecision);
            } else {
                fse.setText(mNotation);
            }
        }

        // f.java case 52 POWER: zE0 = inputMethod.e() = !algebraic; sample is ALGEBRAIC → "xʸ"
        // (already set in layout; re-assert in case something overwrote it)
        Button power = findViewById(R.id.sci_power);
        if (power != null && !mShiftActive) {
            power.setText("xʸ");
        }

        int[] digits = {
            R.id.sci_0, R.id.sci_1, R.id.sci_2, R.id.sci_3, R.id.sci_4,
            R.id.sci_5, R.id.sci_6, R.id.sci_7, R.id.sci_8, R.id.sci_9
        };
        String[] mainLabels = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        for (int i = 0; i < digits.length; i++) {
            Button b = findViewById(digits[i]);
            if (b != null) {
                b.setText(mainLabels[i]);
            }
        }
        // Trig labels: keyboardText + "h" when hyperbolic (f.java case 73–78)
        int[] trig = { R.id.sci_sin, R.id.sci_cos, R.id.sci_tan };
        String[] trigBase = { "Sin", "Cos", "Tan" };
        for (int i = 0; i < trig.length; i++) {
            Button b = findViewById(trig[i]);
            if (b != null) {
                if (mShiftActive) {
                    // shift labels asin/acos/atan
                    String[] shiftLabels = { "asin", "acos", "atan" };
                    b.setText(shiftLabels[i]);
                } else {
                    b.setText(mHyperbolic ? trigBase[i] + "h" : trigBase[i]);
                }
            }
        }
    }

    private void performEqualsCalculation() {
        if (mCurrentInput.length() > 0) {
            String res = mEvaluator.evaluateExpression(mCurrentInput.toString());
            String formattedInput = ExpressionDecorator.decorateAngleFunctions(
                    NumberFormatHelper.formatEquation(mCurrentInput.toString().trim(), 0).taggedText, mAngleUnit, mHyperbolic);
            String formattedRes = NumberFormatHelper.formatEquation(res, 0).taggedText;
            mHistory.add(new HistoryEntry(formattedInput, formattedRes, "="));
            mCurrentInput.setLength(0);
            mCursorIndex = 0;
            mLiveResult = "";
            updateDisplay();
            mListView.setSelection(mAdapter.getCount() - 1);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_EQUALS) {
                performEqualsCalculation();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void insertText(String text) {
        mCurrentInput.insert(mCursorIndex, text);
        mCursorIndex += text.length();
        evaluateLive();
        updateDisplay();
    }

    /** CONST dialog — insert selected constant value (p010f0/d.java H(2)). */
    private void showConstantsDialog() {
        ConstantsDialog.show(this, constant ->
                insertText(constant.getValue()));
    }

    /** CONV dialog — two-step unit pick; insert source value marker (p010f0/g.java H(3)). */
    private void showConversionDialog() {
        ConversionDialog.show(this, (fromUnit, toUnit) -> {
            // Insert conversion: wrap current operand as " value <from> → <to> "
            // Simplified: insert " <from> → <to> " marker after current expression end
            String marker = " " + fromUnit + " → " + toUnit + " ";
            insertText(marker);
        });
    }

    /** STATS dialog — compute stats from numeric history results (p010f0/o.java H(6)). */
    private void showStatisticsDialog() {
        StatisticsDialog.show(this, () -> {
            java.util.List<Double> nums = new ArrayList<>();
            for (HistoryEntry e : mHistory) {
                try {
                    String r = e.result.replaceAll("</?[^/<>]+>", "").replace(",", "").trim();
                    nums.add(Double.parseDouble(r));
                } catch (Exception ignored) {}
            }
            if (nums.isEmpty()) return null;

            int n = nums.size();
            double min = nums.stream().mapToDouble(d -> d).min().orElse(0);
            double max = nums.stream().mapToDouble(d -> d).max().orElse(0);
            double sum = nums.stream().mapToDouble(d -> d).sum();
            double mean = sum / n;
            double varSum = 0;
            for (double v : nums) varSum += (v - mean) * (v - mean);
            double med;
            java.util.List<Double> sorted = new ArrayList<>(nums);
            java.util.Collections.sort(sorted);
            if (n % 2 == 1) med = sorted.get(n / 2);
            else med = (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;

            java.util.List<String> rows = new ArrayList<>();
            rows.add("n  : Quantity : " + n);
            rows.add("↓  : Minimum : " + min);
            rows.add("↑  : Maximum : " + max);
            rows.add("↕  : Range : " + (max - min));
            rows.add("x̃  : Median : " + med);
            rows.add("x̅  : Mean (Average) : " + mean);
            rows.add("Σx : Sum : " + sum);
            rows.add("s² : Sample Variance : " + (n > 1 ? varSum / (n - 1) : 0));
            rows.add("s  : Sample Standard Deviation : " + (n > 1 ? Math.sqrt(varSum / (n - 1)) : 0));
            rows.add("σ² : Population Variance : " + (varSum / n));
            rows.add("σ  : Population Standard Deviation : " + Math.sqrt(varSum / n));
            return rows;
        });
    }

    /**
     * Shift a/b — DMS on trailing operand only (AlgebraicInputHandler case 103:
     * equation.a() result or equation.M() last entry — NOT whole string).
     * Allows: 5→DMS, + 80→DMS independently in one expression.
     */
    private void convertCurrentToDms() {
        String full = mCurrentInput.toString();
        int end = Math.min(Math.max(mCursorIndex, 0), full.length());
        String updated = DmsHelper.convertTrailing(full, end);
        if (updated != null) {
            int delta = updated.length() - full.length();
            mCurrentInput.setLength(0);
            mCurrentInput.append(updated);
            mCursorIndex = Math.min(end + delta, updated.length());
            evaluateLive();
            updateDisplay();
        }
        // null → no trailing numeric operand (original returns without change)
    }

    private void evaluateLive() {
        mEvaluator.angleUnit = mAngleUnit;
        mEvaluator.hyperbolic = mHyperbolic;

        if (mCurrentInput.length() == 0) {
            mLiveResult = "";
            return;
        }
        // Skip eval while expression is incomplete (unclosed paren / trailing token)
        // — avoids regex-heavy evaluateExpression on every keystroke (perf)
        String raw = mCurrentInput.toString();
        if (!ExpressionEvaluator.isExpressionComplete(raw)) {
            mLiveResult = "";
            return;
        }
        try {
            String eval = mEvaluator.evaluateExpression(raw);
            if (eval != null && !eval.isEmpty() && !eval.equals(raw.trim())) {
                String formattedLive = NumberFormatHelper.formatEquation(eval, 0).taggedText;
                mLiveResult = "<o>=</o> " + formattedLive;
            } else {
                mLiveResult = "";
            }
        } catch (Exception e) {
            mLiveResult = "";
        }
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

            TextView dividerView = view.findViewById(R.id.history_divider);
            TextView historyCalc = view.findViewById(R.id.history_calculation);
            TextView historyResult = view.findViewById(R.id.history_result);
            View activeCalcContainer = view.findViewById(R.id.active_calculation_container);
            VerticalListEditText currentCalc = view.findViewById(R.id.current_calculation);
            TextView activeLiveResult = view.findViewById(R.id.active_live_result);

            dividerView.setTypeface(CalcTypefaceHelper.getSymbolAndLabelFont(parent.getContext()));
            historyCalc.setTypeface(CalcTypefaceHelper.getScreenCalculationFont(parent.getContext()));
            historyResult.setTypeface(CalcTypefaceHelper.getScreenCalculationFont(parent.getContext()));
            currentCalc.setTypeface(CalcTypefaceHelper.getScreenCalculationFont(parent.getContext()));
            activeLiveResult.setTypeface(CalcTypefaceHelper.getScreenCalculationFont(parent.getContext()));

            // Dynamic sizes — FONTSIZE_LIST_DESCRIPTION_HISTORY vs FONTSIZE_LIST_STACK (p018j0/b.java)
            float exprSp = CalcFontSizeHelper.expressionSp(parent.getContext());
            float resultSp = CalcFontSizeHelper.resultSp(parent.getContext());
            historyCalc.setTextSize(exprSp);
            currentCalc.setTextSize(exprSp);
            historyResult.setTextSize(resultSp);
            activeLiveResult.setTextSize(resultSp);
            dividerView.setTextSize(12f);

            boolean isActiveLine = (position == mHistory.size());

            View chevronContainer = view.findViewById(R.id.chevron_container);
            TextView btnChevronUp = view.findViewById(R.id.btn_chevron_up);
            TextView btnChevronDown = view.findViewById(R.id.btn_chevron_down);

            if (isActiveLine) {
                dividerView.setVisibility(View.VISIBLE);
                historyCalc.setVisibility(View.GONE);
                historyResult.setVisibility(View.GONE);
                activeCalcContainer.setVisibility(View.VISIBLE);
                activeLiveResult.setVisibility(View.VISIBLE);

                // Format live expression with thousand and decimal separators, plus colored symbols
                NumberFormatHelper.FormattedResult formatted = 
                        NumberFormatHelper.formatEquation(mCurrentInput.toString(), mCursorIndex);

                // Decorate angle sup after formatEquation; adjust cursor for inserted visible chars
                // (matches Equation.T: cursor after auto "(" → after "sin(" in display "sind(")
                int[] decoratedCursor = { formatted.cursorPosition };
                SpannableStringBuilder coloredSpannable = CalcSpannableFormatter.format(
                        ExpressionDecorator.decorateAngleFunctions(formatted.taggedText, formatted.cursorPosition, decoratedCursor, mAngleUnit, mHyperbolic));
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

                if (decoratedCursor[0] <= coloredSpannable.length()) {
                    currentCalc.setSelection(decoratedCursor[0]);
                }

                currentCalc.post(() -> {
                    currentCalc.requestFocus();
                    android.text.Layout layout = currentCalc.getLayout();
                    if (layout != null) {
                        int lineCount = layout.getLineCount();
                        int totalLines = lineCount;
                        if (currentCalc.getWidth() > 0) {
                            int availWidth = currentCalc.getWidth() - currentCalc.getPaddingLeft() - currentCalc.getPaddingRight();
                            if (availWidth > 0) {
                                android.text.StaticLayout sl = new android.text.StaticLayout(
                                        coloredSpannable,
                                        currentCalc.getPaint(),
                                        availWidth,
                                        android.text.Layout.Alignment.ALIGN_NORMAL,
                                        1.0f, 0.0f, false
                                );
                                totalLines = Math.max(totalLines, sl.getLineCount());
                            }
                        }

                        if (totalLines > 3) {
                            chevronContainer.setVisibility(View.VISIBLE);
                            int curLine = layout.getLineForOffset(currentCalc.getSelectionStart());
                            btnChevronUp.setAlpha(curLine > 0 ? 1.0f : 0.3f);
                            btnChevronDown.setAlpha(curLine < totalLines - 1 ? 1.0f : 0.3f);
                        } else {
                            chevronContainer.setVisibility(View.GONE);
                        }
                    } else {
                        chevronContainer.setVisibility(View.GONE);
                    }
                });

                if (btnChevronUp != null) {
                    btnChevronUp.setOnClickListener(v -> {
                        android.text.Layout layout = currentCalc.getLayout();
                        if (layout != null) {
                            int curLine = layout.getLineForOffset(currentCalc.getSelectionStart());
                            if (curLine > 0) {
                                int targetLine = curLine - 1;
                                float x = layout.getPrimaryHorizontal(currentCalc.getSelectionStart());
                                int newFormattedOffset = layout.getOffsetForHorizontal(targetLine, x);
                                int rawIdx = NumberFormatHelper.mapOffsetToRawIndex(formatted.printedSizes, newFormattedOffset);
                                if (rawIdx >= 0 && rawIdx <= mCurrentInput.length()) {
                                    mCursorIndex = rawIdx;
                                    updateDisplay();
                                }
                            }
                        }
                    });
                }

                if (btnChevronDown != null) {
                    btnChevronDown.setOnClickListener(v -> {
                        android.text.Layout layout = currentCalc.getLayout();
                        if (layout != null) {
                            int lineCount = layout.getLineCount();
                            int curLine = layout.getLineForOffset(currentCalc.getSelectionStart());
                            if (curLine < lineCount - 1) {
                                int targetLine = curLine + 1;
                                float x = layout.getPrimaryHorizontal(currentCalc.getSelectionStart());
                                int newFormattedOffset = layout.getOffsetForHorizontal(targetLine, x);
                                int rawIdx = NumberFormatHelper.mapOffsetToRawIndex(formatted.printedSizes, newFormattedOffset);
                                if (rawIdx >= 0 && rawIdx <= mCurrentInput.length()) {
                                    mCursorIndex = rawIdx;
                                    updateDisplay();
                                }
                            }
                        }
                    });
                }

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

                activeLiveResult.setText(CalcSpannableFormatter.format(mLiveResult));
            } else {
                dividerView.setVisibility(View.GONE);
                activeCalcContainer.setVisibility(View.GONE);
                activeLiveResult.setVisibility(View.GONE);
                chevronContainer.setVisibility(View.GONE);
                historyCalc.setVisibility(View.VISIBLE);
                historyResult.setVisibility(View.VISIBLE);

                HistoryEntry entry = mHistory.get(position);
                historyCalc.setText(CalcSpannableFormatter.format(entry.expression));
                SpannableStringBuilder resultSsb = CalcSpannableFormatter.format("= " + entry.result);
                // "=" prefix → default white, not green result color
                if (resultSsb.length() > 0 && resultSsb.charAt(0) == '=') {
                    resultSsb.setSpan(
                            new android.text.style.ForegroundColorSpan(Color.WHITE),
                            0, 1,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                historyResult.setText(resultSsb);
                currentCalc.setOnTouchListener(null);
            }

            return view;
        }
    }
}
