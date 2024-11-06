package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Comparator {
    public static void compareFiles(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords) {
        // Combine FlixBus records with the same serial number
        Map<String, FlixBusRecord> combinedFlixBusRecords = new HashMap<>();
        for (FlixBusRecord record : flixbusRecords) {
            combinedFlixBusRecords.merge(record.getBookingNumber(), record, (existing, newRecord) ->
                    new FlixBusRecord(
                            existing.getBookingNumber(),
                            existing.getTripServices(),
                            existing.getCash() + newRecord.getCash(),
                            existing.getPlatformFee(),
                            existing.getVoucher(),
                            existing.getPaymentType() // Assuming paymentType is the same for combined records
                    ));
        }

        // Convert the map values to a list
        List<FlixBusRecord> combinedFlixbusList = new ArrayList<>(combinedFlixBusRecords.values());

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
            System.out.printf("%s | %s | %s | %s | %s | %s%n", espSerial, espAmount, flixbusSerial, flixbusCash, paymentType, match);
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