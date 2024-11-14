package org.example;

public class SummaryRecord {
    private final String description;
    private final double espValue;
    private final double flixbusValue;
    private final double difference;

    public SummaryRecord(String description, double espValue, double flixbusValue, double difference) {
        this.description = description;
        this.espValue = espValue;
        this.flixbusValue = flixbusValue;
        this.difference = difference;
    }

    public String getDescription() {
        return description;
    }

    public double getEspValue() {
        return espValue;
    }

    public double getFlixbusValue() {
        return flixbusValue;
    }

    public double getDifference() {
        return difference;
    }
}