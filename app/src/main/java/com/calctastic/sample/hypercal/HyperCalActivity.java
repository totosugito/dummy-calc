package com.calctastic.sample.hypercal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.calctastic.sample.R;
import com.calctastic.sample.hypercal.display.model.MathToken;
import com.calctastic.sample.hypercal.display.view.HiPerDisplayContainerView;

import java.util.List;

/**
 * HyperCalActivity:
 * Activity kalkulator dengan integrasi:
 * 1. Display HiPER Calculator 2D Canvas (Render pecahan bertingkat, akar, kurung, eksponen, dan kursor).
 * 2. Keypad tombol berdesain Calctastic.
 * 3. Skema warna tema HiPER.
 * Bebas dari segala AdView / Ads SDK.
 */
public class HyperCalActivity extends Activity implements View.OnClickListener {

    private HiPerDisplayContainerView displayContainer;
    private Button btnModeToggle;
    private boolean isDegreeMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hypercal);

        displayContainer = findViewById(R.id.hiper_display_container);
        btnModeToggle = findViewById(R.id.btn_mode_toggle);

        if (btnModeToggle != null) {
            btnModeToggle.setOnClickListener(v -> toggleAngleMode());
        }

        setupKeypadListeners();

        // Tampilkan pecahan dengan kotak atas dan bawah secara default
        if (displayContainer != null) {
            displayContainer.appendFraction();
        }
    }

    private void toggleAngleMode() {
        isDegreeMode = !isDegreeMode;
        String modeText = isDegreeMode ? "DEG" : "RAD";
        if (btnModeToggle != null) {
            btnModeToggle.setText(modeText);
        }
        if (displayContainer != null) {
            displayContainer.setAngleMode(modeText);
        }
    }

    private void setupKeypadListeners() {
        int[] buttonIds = new int[]{
                // Numbers
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_dot,
                // Operators
                R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div,
                // Math & 2D Structures
                R.id.btn_fraction, R.id.btn_sqrt, R.id.btn_percent,
                R.id.btn_square, R.id.btn_power, R.id.btn_reciprocal,
                R.id.btn_paren_open, R.id.btn_paren_close,
                // Controls
                R.id.btn_cursor_left, R.id.btn_cursor_right,
                R.id.btn_delete, R.id.btn_clear, R.id.btn_equals,
                // Memory
                R.id.btn_mem_plus, R.id.btn_mem_minus, R.id.btn_mem_clear,
                R.id.btn_mem_save, R.id.btn_mem_recall,
                R.id.btn_negate
        };

        for (int id : buttonIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // 1. Tombol Angka (0-9, dot)
        if (id == R.id.btn_0) displayContainer.appendText("0");
        else if (id == R.id.btn_1) displayContainer.appendText("1");
        else if (id == R.id.btn_2) displayContainer.appendText("2");
        else if (id == R.id.btn_3) displayContainer.appendText("3");
        else if (id == R.id.btn_4) displayContainer.appendText("4");
        else if (id == R.id.btn_5) displayContainer.appendText("5");
        else if (id == R.id.btn_6) displayContainer.appendText("6");
        else if (id == R.id.btn_7) displayContainer.appendText("7");
        else if (id == R.id.btn_8) displayContainer.appendText("8");
        else if (id == R.id.btn_9) displayContainer.appendText("9");
        else if (id == R.id.btn_dot) displayContainer.appendText(".");

        // 2. Tombol Operator Matematika
        else if (id == R.id.btn_add) displayContainer.appendOperator("+");
        else if (id == R.id.btn_sub) displayContainer.appendOperator("−");
        else if (id == R.id.btn_mul) displayContainer.appendOperator("×");
        else if (id == R.id.btn_div) displayContainer.appendOperator("÷");

        // 3. Tombol Fitur Display 2D (Pecahan, Akar, Pangkat)
        else if (id == R.id.btn_fraction) displayContainer.appendFraction();
        else if (id == R.id.btn_sqrt) displayContainer.appendSqrt();
        else if (id == R.id.btn_power) displayContainer.appendPower();
        else if (id == R.id.btn_square) {
            // x² -> tambahkan power kuadrat dengan basis ber-tanda kurung ( ... )²
            displayContainer.appendSquare();
        } else if (id == R.id.btn_percent) displayContainer.appendText("%");
        else if (id == R.id.btn_paren_open) displayContainer.appendText("(");
        else if (id == R.id.btn_paren_close) displayContainer.appendText(")");
        else if (id == R.id.btn_reciprocal) {
            // 1/x -> bentuk pecahan dengan 1 di atas dan kursor di kotak penyebut bawah untuk mengisi x
            displayContainer.appendReciprocal();
        }

        // 4. Tombol Kontrol Kursor & Edit
        else if (id == R.id.btn_delete) displayContainer.deleteBackward();
        else if (id == R.id.btn_clear) displayContainer.clearAll();
        else if (id == R.id.btn_cursor_left) displayContainer.moveCursorLeft();
        else if (id == R.id.btn_cursor_right) displayContainer.moveCursorRight();

        // 5. Tombol Evaluasi (=)
        else if (id == R.id.btn_equals) {
            evaluatePreview();
        }

        // 6. Negate (±)
        else if (id == R.id.btn_negate) {
            displayContainer.appendText("−");
        }
    }

    private void evaluatePreview() {
        List<MathToken> tokens = displayContainer.getTokens();
        if (tokens.isEmpty()) {
            displayContainer.setResultPreview("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (MathToken token : tokens) {
            sb.append(stringifyToken(token));
        }

        displayContainer.setResultPreview("= " + sb.toString());
    }

    private String stringifyToken(MathToken token) {
        if (token == null) return "";
        if (token.type == MathToken.Type.NUMBER || token.type == MathToken.Type.OPERATOR) {
            return token.text != null ? token.text : "";
        }
        if (token.type == MathToken.Type.FRACTION) {
            StringBuilder num = new StringBuilder();
            for (MathToken c : token.children) num.append(stringifyToken(c));
            StringBuilder den = new StringBuilder();
            for (MathToken c : token.secondaryChildren) den.append(stringifyToken(c));
            return "(" + num + "/" + den + ")";
        }
        if (token.type == MathToken.Type.SQRT) {
            StringBuilder inner = new StringBuilder();
            for (MathToken c : token.children) inner.append(stringifyToken(c));
            return "√(" + inner + ")";
        }
        if (token.type == MathToken.Type.POWER) {
            StringBuilder base = new StringBuilder();
            for (MathToken c : token.children) base.append(stringifyToken(c));
            StringBuilder exp = new StringBuilder();
            for (MathToken c : token.secondaryChildren) exp.append(stringifyToken(c));
            return base + "^(" + exp + ")";
        }
        if (token.type == MathToken.Type.PAREN_GROUP) {
            StringBuilder inner = new StringBuilder();
            for (MathToken c : token.children) inner.append(stringifyToken(c));
            return "(" + inner + ")";
        }
        return "";
    }
}
