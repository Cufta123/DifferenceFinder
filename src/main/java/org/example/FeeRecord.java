package org.example;

public class FeeRecord implements Record {
    private final String bookingNumber;
    private final double feeAmount;

    public FeeRecord(String serialNumber, double feeAmount) {
        this.bookingNumber = serialNumber;
        this.feeAmount = feeAmount;
    }

    public double getFeeAmount() {
        return feeAmount;
    }

    @Override
    public String bookingNumber() {
        return bookingNumber;
    }
}