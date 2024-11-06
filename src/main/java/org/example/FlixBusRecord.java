package org.example;

public class FlixBusRecord {
    private String bookingNumber;
    private String tripServices;
    private double cash;
    private double platformFee;
    private double voucher;
    private String paymentType; // New field

    public FlixBusRecord(String bookingNumber, String tripServices, double cash, double platformFee, double voucher, String paymentType) {
        this.bookingNumber = bookingNumber;
        this.tripServices = tripServices;
        this.cash = cash;
        this.platformFee = platformFee;
        this.voucher = voucher;
        this.paymentType = paymentType;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public String getTripServices() {
        return tripServices;
    }

    public double getCash() {
        return cash;
    }

    public double getPlatformFee() {
        return platformFee;
    }

    public double getVoucher() {
        return voucher;
    }

    public String getPaymentType() {
        return paymentType;
    }
}