package org.example;

public record FlixBusRecord(String bookingNumber, String tripServices, double cash, double voucher,
                            double comm_gross, double totalAmount) implements Record {
    // Constructor

}