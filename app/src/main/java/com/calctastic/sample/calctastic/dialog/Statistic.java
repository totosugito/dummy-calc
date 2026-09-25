package com.calctastic.sample.dialog;

/** Statistics labels — port from com.calctastic.calculator.statistics.Statistic */
public enum Statistic {
    QUANTITY("Quantity", "n"),
    MINIMUM("Minimum", "↓"),
    MAXIMUM("Maximum", "↑"),
    RANGE("Range", "↕"),
    MEDIAN("Median", "x̃"),
    MEAN("Mean (Average)", "x̅"),
    MEAN_SQUARED("Mean Squared", "x̅²"),
    GEOMETRIC_MEAN("Geometric Mean", "G"),
    SUM("Sum", "Σx"),
    SUM_OF_SQUARES("Sum of Squares", "Σx²"),
    SUM_OF_SQUARES_VARIANCE("Sum of Squares of Variance", "SS"),
    SAMPLE_VARIANCE("Sample Variance", "s²"),
    SAMPLE_STD_DEV("Sample Standard Deviation", "s"),
    POPULATION_VARIANCE("Population Variance", "σ²"),
    POPULATION_STD_DEV("Population Standard Deviation", "σ");

    private final String description;
    private final String symbol;

    Statistic(String description, String symbol) {
        this.description = description;
        this.symbol = symbol;
    }

    public String getDescription() { return description; }
    public String getSymbol() { return symbol; }
}
