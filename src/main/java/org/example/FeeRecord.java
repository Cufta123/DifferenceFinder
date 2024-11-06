package org.example;

public class FeeRecord {
    private String serialNumber;
    private double feeAmount;

    public FeeRecord(String serialNumber, double feeAmount) {
        this.serialNumber = serialNumber;
        this.feeAmount = feeAmount;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public double getFeeAmount() {
        return feeAmount;
    }
}