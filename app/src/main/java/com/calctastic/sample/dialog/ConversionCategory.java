package com.calctastic.sample.dialog;

/** Unit conversion categories — port from com.calctastic.calculator.conversions */
public enum ConversionCategory {
    LENGTH("Length"),
    AREA("Area"),
    VOLUME("Volume"),
    MASS("Mass"),
    TEMPERATURE("Temperature"),
    TIME("Time"),
    SPEED("Speed"),
    PRESSURE("Pressure"),
    ENERGY("Energy"),
    POWER("Power"),
    FORCE("Force"),
    FREQUENCY("Frequency");

    private final String description;

    ConversionCategory(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
