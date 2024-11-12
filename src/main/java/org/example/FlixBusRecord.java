package org.example;
public class FlixBusRecord implements  Record{
    private String bookingNumber;
    private String tripServices;
    private double cash;
    private double voucher;
    private String paymentType;
    private double comm_gross;
    private double totalAmount;

    // Constructor
    public FlixBusRecord(String bookingNumber, String tripServices, double cash, double voucher, String paymentType, double comm_gross, double totalAmount) {
        this.bookingNumber = bookingNumber;
        this.tripServices = tripServices;
        this.cash = cash;
        this.voucher = voucher;
        this.paymentType = paymentType;
        this.comm_gross = comm_gross;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getBookingNumber() {
        return bookingNumber;
    }

    public String getTripServices() {
        return tripServices;
    }

    public double getCash() {
        return cash;
    }

    public double getVoucher() {
        return voucher;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public double getComm_gross() {
        return comm_gross;
    }
    public double getTotalAmount() {
        return totalAmount;
    }

}