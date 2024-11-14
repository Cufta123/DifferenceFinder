package org.example;

public class VoucherFlixBusRecord implements Record {
    private final String bookingNumber;
    private final String tripServices;
    private final double voucher;
    private final String paymentType;
    private final double commGross;
    private final double totalAmount;

    public VoucherFlixBusRecord(String bookingNumber, String tripServices,  double voucher, String paymentType, double commGross, double totalAmount) {
        this.bookingNumber = bookingNumber;
        this.tripServices = tripServices;
        this.voucher = voucher;
        this.paymentType = paymentType;
        this.commGross = commGross;
        this.totalAmount = totalAmount;
    }

    @Override
    public String bookingNumber() {
        return bookingNumber;
    }

    public String tripServices() {
        return tripServices;
    }


    public double voucher() {
        return voucher;
    }

    public String paymentType() {
        return paymentType;
    }

    public double comm_gross() {
        return commGross;
    }

    public double totalAmount() {
        return totalAmount;
    }
}
