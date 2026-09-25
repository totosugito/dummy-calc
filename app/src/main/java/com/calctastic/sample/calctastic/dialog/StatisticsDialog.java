package com.calctastic.sample.calctastic.dialog;

import android.app.AlertDialog;
import android.content.Context;
import java.util.List;

/**
 * Statistics dialog — port from p010f0/o.java (H(6)).
 * Shows stats computed from numeric history entries.
 */
public final class StatisticsDialog {

    public interface StatsProvider {
        /** Returns formatted stat rows "symbol : description : value", empty if no data. */
        List<String> computeStats();
    }

    private StatisticsDialog() {}

    public static void show(Context context, StatsProvider provider) {
        List<String> rows = provider.computeStats();
        if (rows == null || rows.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("STATISTICS")
                    .setMessage("Visible History is Empty")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("STATISTICS")
                .setItems(rows.toArray(new String[0]), null)
                .setNegativeButton("Cancel", null)
                .show();
    }
}
