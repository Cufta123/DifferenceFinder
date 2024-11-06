package org.example;

public class ESPRecord {
    private String serialNumber;
    private double amount;
    private double serviceFee;

    public ESPRecord(String serialNumber, double amount, double serviceFee) {
        this.serialNumber = serialNumber;
        this.amount = amount;
        this.serviceFee = serviceFee;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public double getAmount() {
        return amount;
    }

    public double getServiceFee() {
        return serviceFee;
    }

    public double getTotalAmount() {
        return amount + serviceFee;
    }
}