package org.example;

public record FlixBusRecord(String bookingNumber, String tripServices, double cash, double voucher, String paymentType,
                            double comm_gross, double totalAmount) implements Record {
    // Constructor

}