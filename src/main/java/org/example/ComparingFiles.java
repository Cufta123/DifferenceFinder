package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public class ComparingFiles {
    public static void compareFiles(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords) {
        // Combine FlixBus records with the same serial number
        Map<String, FlixBusRecord> combinedFlixBusRecords = new HashMap<>();
        for (FlixBusRecord record : flixbusRecords) {
            combinedFlixBusRecords.merge(record.getBookingNumber(), record, (existing, newRecord) ->
                    new FlixBusRecord(
                            existing.getBookingNumber(),
                            existing.getTripServices(),
                            existing.getCash() + newRecord.getCash(),
                            existing.getVoucher(),
                            existing.getPaymentType() // Assuming paymentType is the same for combined records
                    ));
        }

        // Convert the map values to a list
        List<FlixBusRecord> combinedFlixbusList = new ArrayList<>(combinedFlixBusRecords.values());

        // Sort both lists by their serial numbers
        espRecords.sort(Comparator.comparing(ESPRecord::getSerialNumber));
        combinedFlixbusList.sort(Comparator.comparing(FlixBusRecord::getBookingNumber));

        // List to store unmatched FlixBus records
        List<FlixBusRecord> unmatchedFlixbusList = new ArrayList<>();

        double espTotalAmount = espRecords.stream().mapToDouble(ESPRecord::getAmount).sum();
        double flixbusTotalCash = combinedFlixbusList.stream().mapToDouble(FlixBusRecord::getCash).sum();

        System.out.printf("ESP summary: %.2f, Flixbus summary: %.2f, Difference: %.2f%n",
                espTotalAmount, flixbusTotalCash, espTotalAmount - flixbusTotalCash);

        System.out.println("ESP Serial Number | ESP Amount | FlixBus Serial Number | FlixBus Cash Amount | Payment Type | Match");
        int maxSize = Math.max(espRecords.size(), combinedFlixbusList.size());
        for (int i = 0; i < maxSize; i++) {
            String espSerial = i < espRecords.size() ? formatSerialNumber(espRecords.get(i).getSerialNumber()) : "N/A";
            String espAmount = i < espRecords.size() ? String.format("%.2f", espRecords.get(i).getAmount()) : "N/A";
            String flixbusSerial = i < combinedFlixbusList.size() ? formatSerialNumber(combinedFlixbusList.get(i).getBookingNumber()) : "N/A";
            String flixbusCash = i < combinedFlixbusList.size() ? String.format("%.2f", combinedFlixbusList.get(i).getCash()) : "N/A";
            String paymentType = i < combinedFlixbusList.size() ? combinedFlixbusList.get(i).getPaymentType() : "N/A";
            String match = espSerial.equals(flixbusSerial) ? "Yes" : "Not the same";

            if (!espSerial.equals(flixbusSerial) && i < combinedFlixbusList.size()) {
                unmatchedFlixbusList.add(combinedFlixbusList.remove(i));
                i--; // Adjust the index after removal
            } else if (!espSerial.equals("N/A") || !flixbusSerial.equals("N/A")) {
             //   System.out.printf("%s | %s | %s | %s | %s | %s%n", espSerial, espAmount, flixbusSerial, flixbusCash, paymentType, match);
            }
        }

        // Print unmatched FlixBus records at the bottom
        for (FlixBusRecord record : unmatchedFlixbusList) {
            String flixbusSerial = formatSerialNumber(record.getBookingNumber());
            String flixbusCash = String.format("%.2f", record.getCash());
            String paymentType = record.getPaymentType();
            System.out.printf("N/A | N/A | %s | %s | %s | Not the same%n", flixbusSerial, flixbusCash, paymentType);
        }
    }
    public static void printServiceFee(List<ESPRecord> espRecords, List<FlixBusRecord> feeRecords) {
        System.out.println("ESP Serial Number | FlixBus Booking Number | Trip Services | Cash | Voucher | Payment Type | Match");

        // Combine FlixBus records with the same booking number
        Map<String, FlixBusRecord> combinedFlixBusRecords = new HashMap<>();
        for (FlixBusRecord record : feeRecords) {
            combinedFlixBusRecords.merge(record.getBookingNumber(), record, (existing, newRecord) ->
                    new FlixBusRecord(
                            existing.getBookingNumber(),
                            existing.getTripServices(),
                            existing.getCash() + newRecord.getCash(),
                            existing.getVoucher(),
                            existing.getPaymentType() // Assuming paymentType is the same for combined records
                    ));
        }

        // Convert the map values to a list
        List<FlixBusRecord> combinedFlixbusList = new ArrayList<>(combinedFlixBusRecords.values());

        // Sort both lists by their serial numbers
        espRecords.sort(Comparator.comparing(ESPRecord::getSerialNumber));
        combinedFlixbusList.sort(Comparator.comparing(FlixBusRecord::getBookingNumber));

        // List to store unmatched FlixBus records
        List<FlixBusRecord> unmatchedFlixbusList = new ArrayList<>();

        int maxSize = Math.max(espRecords.size(), combinedFlixbusList.size());
        for (int i = 0; i < maxSize; i++) {
            String espSerial = i < espRecords.size() ? formatSerialNumber(espRecords.get(i).getSerialNumber()) : "N/A";
            String flixbusSerial = i < combinedFlixbusList.size() ? formatSerialNumber(combinedFlixbusList.get(i).getBookingNumber()) : "N/A";
            String tripServices = i < combinedFlixbusList.size() ? combinedFlixbusList.get(i).getTripServices() : "N/A";
            double cash = i < combinedFlixbusList.size() ? combinedFlixbusList.get(i).getCash() : 0.0;
            double voucher = i < combinedFlixbusList.size() ? combinedFlixbusList.get(i).getVoucher() : 0.0;
            String paymentType = i < combinedFlixbusList.size() ? combinedFlixbusList.get(i).getPaymentType() : "N/A";
            String match = espSerial.equals(flixbusSerial) ? "Yes" : "No";

            if (!espSerial.equals(flixbusSerial) && i < combinedFlixbusList.size()) {
                unmatchedFlixbusList.add(combinedFlixbusList.remove(i));
                i--; // Adjust the index after removal
            } else if (!espSerial.equals("N/A") || !flixbusSerial.equals("N/A")) {
               // System.out.printf("%s | %s | %s | %.2f | %.2f | %s | %s%n", espSerial, flixbusSerial, tripServices, cash, voucher, paymentType, match);
            }
        }

        // Print unmatched FlixBus records at the bottom
        for (FlixBusRecord record : unmatchedFlixbusList) {
            String flixbusSerial = formatSerialNumber(record.getBookingNumber());
            String tripServices = record.getTripServices();
            double cash = record.getCash();
            double voucher = record.getVoucher();
            String paymentType = record.getPaymentType();
            System.out.printf("N/A | %s | %s | %.2f | %.2f | %s | No%n", flixbusSerial, tripServices, cash, voucher, paymentType);
        }
    }

    private static String formatSerialNumber(String serialNumber) {
        try {
            return new java.math.BigDecimal(serialNumber).toPlainString();
        } catch (NumberFormatException e) {
            return serialNumber;
        }
    }
}