package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.logging.Logger;

public class ComparingFiles {
    private static final Logger logger = Logger.getLogger(ComparingFiles.class.getName());
    private static final List<MatchedRecord> matchedRecordsList = new ArrayList<>();
    private static final List<FlixBusRecord> unmatchedFlixbusList = new ArrayList<>(); // Declare unmatchedFlixbusList
    private static final List<ESPRecord> unmatchedESPList = new ArrayList<>(); // Declare unmatchedESPList


    public static String compareFiles(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords) {
        StringBuilder result = new StringBuilder();
        try {
            List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
            List<FlixBusRecord> combinedFlixbusList = combineFlixBusRecords(flixbusRecords);

            sortRecords(combinedESPList, combinedFlixbusList);

            // Generate summary
            result.append(generateSummary(combinedESPList, combinedFlixbusList));

            // Perform matching logic
            compareRecords(combinedESPList, combinedFlixbusList);

            // Matched records list
            result.append(formatMatchedRecordsList());
            // Append unmatched records
            result.append("\nUnmatched FlixBus Records:\n");
            for (FlixBusRecord record : unmatchedFlixbusList) {
                result.append(String.format("%-20s | %-10.2f%n", record.getBookingNumber(), record.getCash()));
            }
            result.append("\nUnmatched ESP Records:\n");
            for (ESPRecord record : unmatchedESPList) {
                result.append(String.format("%-20s | %-10.2f%n", record.getSerialNumber(), record.getAmount()));
            }
        } catch (Exception e) {
            logger.severe("An error occurred while comparing files: " + e.getMessage());
            e.printStackTrace();
        }
        return result.toString();
    }

    private static String generateSummary(List<ESPRecord> combinedESPList, List<FlixBusRecord> combinedFlixbusList) {
        double espTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::getAmount).sum();
        double suplierMarginTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::getSuplierMargin).sum();
        double flixbusTotalCash = combinedFlixbusList.stream().mapToDouble(FlixBusRecord::getCash).sum();
        double absoluteDifference = Math.abs(espTotalAmount - flixbusTotalCash);
        double totalComm_gross = combinedFlixbusList.stream().mapToDouble(FlixBusRecord::getComm_gross).sum();
        double commGrossSupplierMarginDiff = Math.abs(totalComm_gross - suplierMarginTotalAmount);

        return String.format("ESP summary:     %.2f  |   Suplier Margin:   %.3f%n" +
                        "Flixbus summary: %.2f  |   Total Comm_gross: %.3f%n" +
                        "Difference:      %.2f    |   Difference:       %.3f%n%n",
                espTotalAmount, suplierMarginTotalAmount, flixbusTotalCash, totalComm_gross, absoluteDifference, commGrossSupplierMarginDiff);
    }

    public static String printServiceFee(List<ESPRecord> espRecords, List<FlixBusRecord> feeRecords) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("%n%-20s | %-15s | %-10s%n", "FlixBus Booking Number", "Trip Services", "Cash"));

        List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
        List<FlixBusRecord> combinedFlixbusList = combineFlixBusRecords(feeRecords);

        sortRecords(combinedESPList, combinedFlixbusList);

        result.append(printServiceFeeComparison(combinedESPList, combinedFlixbusList));
        return result.toString();
    }

    private static List<FlixBusRecord> combineFlixBusRecords(List<FlixBusRecord> flixbusRecords) {
        Map<String, FlixBusRecord> combinedFlixBusRecords = new HashMap<>();
        for (FlixBusRecord record : flixbusRecords) {
            combinedFlixBusRecords.merge(record.getBookingNumber(), record, (existing, newRecord) ->
                    new FlixBusRecord(
                            existing.getBookingNumber(),
                            existing.getTripServices(),
                            existing.getCash() + newRecord.getCash(),
                            existing.getVoucher() + newRecord.getVoucher(), // Sum up voucher
                            existing.getPaymentType(),
                            existing.getComm_gross() + newRecord.getComm_gross() // Sum up comm_gross
                    ));
        }
        return new ArrayList<>(combinedFlixBusRecords.values());
    }

    private static List<ESPRecord> combineESPRecords(List<ESPRecord> espRecords) {
        Map<String, ESPRecord> combinedESPRecords = new HashMap<>();
        for (ESPRecord record : espRecords) {
            combinedESPRecords.merge(record.getSerialNumber(), record, (existing, newRecord) ->
                    new ESPRecord(
                            existing.getSerialNumber(),
                            existing.getAmount() + newRecord.getAmount(),
                            existing.getSuplierMargin() + newRecord.getSuplierMargin(),
                            existing.getSuplierMargin() + newRecord.getSuplierMargin() // Correctly sum the suplierMargin
                    ));
        }
        return new ArrayList<>(combinedESPRecords.values());
    }

    private static void sortRecords(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords) {
        espRecords.sort(Comparator.comparing(ESPRecord::getSerialNumber));
        flixbusRecords.sort(Comparator.comparing(FlixBusRecord::getBookingNumber));
    }

    private static void compareRecords(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords) {
        unmatchedFlixbusList.clear();
        unmatchedESPList.clear();
        matchedRecordsList.clear();


        int maxSize = Math.max(espRecords.size(), flixbusRecords.size());
        for (int i = 0; i < maxSize; i++) {
            String espSerial = i < espRecords.size() ? formatSerialNumber(espRecords.get(i).getSerialNumber()) : "N/A";
            String espAmount = i < espRecords.size() ? String.format("%.2f", espRecords.get(i).getAmount()) : "N/A";
            String flixbusSerial = i < flixbusRecords.size() ? formatSerialNumber(flixbusRecords.get(i).getBookingNumber()) : "N/A";
            String flixbusCash = i < flixbusRecords.size() ? String.format("%.2f", flixbusRecords.get(i).getCash()) : "N/A";
            String paymentType = i < flixbusRecords.size() ? flixbusRecords.get(i).getPaymentType() : "N/A";
            String match = (espSerial.equals(flixbusSerial) && espAmount.equals(flixbusCash)) ? "Yes" : "No";

            if (!espSerial.equals(flixbusSerial) && i < flixbusRecords.size()) {
                unmatchedFlixbusList.add(flixbusRecords.remove(i));
                i--; // Adjust the index after removal
            } else if (!espSerial.equals("N/A") || !flixbusSerial.equals("N/A")) {
                if (match.equals("Yes")) {
                    matchedRecordsList.add(new MatchedRecord(espRecords.get(i), flixbusRecords.get(i)));

                }
            }
        }
    }

    private static String formatMatchedRecordsList() {
        StringBuilder result = new StringBuilder();
        result.append("Matched Records:\n");
        result.append(String.format("%-20s | %-10s | %-20s | %-10s%n", "ESP Serial", "ESP Amount", "Booking Number", "Cash"));
        for (MatchedRecord record : matchedRecordsList) {
            ESPRecord espRecord = record.getEspRecord();
            FlixBusRecord flixBusRecord = record.getFlixBusRecord();
            String formattedBookingNumber = new java.math.BigDecimal(flixBusRecord.getBookingNumber()).toPlainString();
            result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f%n",
                    espRecord.getSerialNumber(), espRecord.getAmount(), formattedBookingNumber, flixBusRecord.getCash()));
        }
        return result.toString();
    }
    private static String printServiceFeeComparison(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords) {
        StringBuilder result = new StringBuilder();
        List<FlixBusRecord> unmatchedFlixbusList = new ArrayList<>();
        matchedRecordsList.clear();

        int maxSize = Math.max(espRecords.size(), flixbusRecords.size());
        for (int i = 0; i < maxSize; i++) {
            String espSerial = i < espRecords.size() ? formatSerialNumber(espRecords.get(i).getSerialNumber()) : "N/A";
            String flixbusSerial = i < flixbusRecords.size() ? formatSerialNumber(flixbusRecords.get(i).getBookingNumber()) : "N/A";
            String tripServices = i < flixbusRecords.size() ? flixbusRecords.get(i).getTripServices() : "N/A";
            double cash = i < flixbusRecords.size() ? flixbusRecords.get(i).getCash() : 0.0;
            double voucher = i < flixbusRecords.size() ? flixbusRecords.get(i).getVoucher() : 0.0;
            String paymentType = i < flixbusRecords.size() ? flixbusRecords.get(i).getPaymentType() : "N/A";
            String match = espSerial.equals(flixbusSerial) ? "Yes" : "No";

            if (!espSerial.equals(flixbusSerial) && i < flixbusRecords.size()) {
                unmatchedFlixbusList.add(flixbusRecords.remove(i));
                i--; // Adjust the index after removal
            } else if (!espSerial.equals("N/A") || !flixbusSerial.equals("N/A")) {
                if (match.equals("Yes")) {
                    matchedRecordsList.add(new MatchedRecord(espRecords.get(i), flixbusRecords.get(i)));
                }
                result.append(String.format("%s | %s | %s | %.2f | %.2f | %s | %s%n", espSerial, flixbusSerial, tripServices, cash, voucher, paymentType, match));
            }
        }

        for (FlixBusRecord record : unmatchedFlixbusList) {
            String flixbusSerial = formatSerialNumber(record.getBookingNumber());
            String tripServices = record.getTripServices();
            double cash = record.getCash();
            result.append(String.format("%-21s | %-15s | %.2f%n", flixbusSerial, tripServices, cash));
        }
        return result.toString();
    }

    private static String formatSerialNumber(String serialNumber) {
        try {
            return new java.math.BigDecimal(serialNumber).toPlainString();
        } catch (NumberFormatException e) {
            return serialNumber;
        }
    }
}