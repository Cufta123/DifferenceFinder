package org.example;

public class ESPRecord {
    private String serialNumber;
    private double amount;
    private double serviceFee;
    private double suplierMargin;

    public ESPRecord(String serialNumber, double amount, double serviceFee, double suplierMargin) {
        this.serialNumber = serialNumber;
        this.amount = amount;
        this.serviceFee = serviceFee;
        this.suplierMargin = suplierMargin;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public double getAmount() {
        return amount;
    }

    public double getSuplierMargin() {
        return suplierMargin;
    }

    public double getServiceFee() {
        return serviceFee;
    }

    public double getTotalAmount() {
        return amount + serviceFee;
    }
}