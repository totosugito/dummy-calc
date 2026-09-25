package com.calctastic.sample.calctastic.dialog;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Conversion dialog — port from p010f0/g.java (H(3)).
 * Two-step: category list → unit list. Simplified: pick from-unit then to-unit.
 */
public final class ConversionDialog {

    public interface OnConversionSelected {
        /** @return plain conversion expression to insert, e.g. " to " placeholder */
        void onSelected(String fromUnit, String toUnit);
    }

    private ConversionDialog() {}

    public static void show(Context context, OnConversionSelected listener) {
        final ConversionCategory[] cats = ConversionCategory.values();
        String[] catNames = new String[cats.length];
        for (int i = 0; i < cats.length; i++) {
            catNames[i] = cats[i].getDescription();
        }

        new AlertDialog.Builder(context)
                .setTitle("CONVERT")
                .setItems(catNames, (d1, whichCat) -> {
                    final ConversionCategory cat = cats[whichCat];
                    final String[] units = unitsFor(cat);
                    if (units.length == 0) return;

                    new AlertDialog.Builder(context)
                            .setTitle(cat.getDescription() + " — From")
                            .setItems(units, (d2, fromIdx) -> {
                                final String from = units[fromIdx];
                                new AlertDialog.Builder(context)
                                        .setTitle(cat.getDescription() + " — To")
                                        .setItems(units, (d3, toIdx) -> {
                                            listener.onSelected(from, units[toIdx]);
                                            d3.dismiss();
                                            d2.dismiss();
                                            d1.dismiss();
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String[] unitsFor(ConversionCategory cat) {
        switch (cat) {
            case LENGTH:
                return new String[]{"m", "km", "cm", "mm", "mi", "yd", "ft", "in"};
            case AREA:
                return new String[]{"m²", "km²", "cm²", "ft²", "yd²", "acre", "ha"};
            case VOLUME:
                return new String[]{"m³", "L", "mL", "gal", "qt", "pt", "cup", "fl oz"};
            case MASS:
                return new String[]{"kg", "g", "mg", "lb", "oz", "ton", "t"};
            case TEMPERATURE:
                return new String[]{"°C", "°F", "K"};
            case TIME:
                return new String[]{"s", "ms", "min", "h", "day", "week", "year"};
            case SPEED:
                return new String[]{"m/s", "km/h", "mph", "kn", "ft/s"};
            case PRESSURE:
                return new String[]{"Pa", "kPa", "bar", "atm", "psi", "mmHg"};
            case ENERGY:
                return new String[]{"J", "kJ", "cal", "kcal", "Wh", "kWh", "eV"};
            case POWER:
                return new String[]{"W", "kW", "MW", "hp", "GW"};
            case FORCE:
                return new String[]{"N", "kN", "dyn", "lbf"};
            case FREQUENCY:
                return new String[]{"Hz", "kHz", "MHz", "GHz"};
            default:
                return new String[0];
        }
    }
}
