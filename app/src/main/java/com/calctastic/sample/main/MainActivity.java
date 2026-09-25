package com.calctastic.sample.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.calctastic.sample.R;
import com.calctastic.sample.calctastic.CalctasticCalculatorActivity;
import com.calctastic.sample.hypercal.HyperCalActivity;

/** Launcher screen: pick which calculator to open. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button calctastic = findViewById(R.id.btn_calctastic);
        calctastic.setOnClickListener(v ->
                startActivity(new Intent(this, CalctasticCalculatorActivity.class)));

        Button hypercal = findViewById(R.id.btn_hypercal);
        hypercal.setOnClickListener(v ->
                startActivity(new Intent(this, HyperCalActivity.class)));
    }
}
