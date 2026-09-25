package com.calctastic.sample.ui;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class VerticalListTextView extends AppCompatTextView {
    public VerticalListTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public final boolean canScrollHorizontally(int i2) {
        return false;
    }
}
