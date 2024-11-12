package org.example;

public record ESPRecord(String serialNumber, double amount, double serviceFee, double suplierMargin) {

    public double getTotalAmount() {
        return amount + serviceFee;
    }
}