package com.calctastic.sample.calctastic.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.calctastic.sample.R;

/** Constants dialog — port from p010f0/d.java (H(2)) */
public final class ConstantsDialog {

    public interface OnConstantSelected {
        void onSelected(PhysicalConstant constant);
    }

    private ConstantsDialog() {}

    public static void show(Context context, OnConstantSelected listener) {
        final PhysicalConstant[] values = PhysicalConstant.values();
        String[] items = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            PhysicalConstant c = values[i];
            items[i] = c.getSymbol() + "  " + c.getDescription() + "\n"
                    + c.getValue() + " " + c.getUnits();
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("CONSTANTS")
                .setItems(items, (d, which) -> {
                    listener.onSelected(values[which]);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            ListView listView = dialog.getListView();
            if (listView != null) {
                listView.setDividerHeight(1);
                listView.setCacheColorHint(0);
            }
        });
        dialog.show();
    }
}
