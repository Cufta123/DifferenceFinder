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


    public static String compareFiles(List<ESPRecord> espRecords, List<Record> flixbusRecords, List<Record> feeRecords) {
        StringBuilder result = new StringBuilder();
        try {
            List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
            List<FlixBusRecord> combinedFlixbusList = combineFlixBusRecords(flixbusRecords);
            List<FeeRecord> combinedFlixBusFeeRecords = combineFlixBusFeeRecords(flixbusRecords);

            sortRecords(combinedESPList, combinedFlixbusList);

            // Generate summary
            result.append(generateSummary(combinedESPList, combinedFlixbusList, combinedFlixBusFeeRecords));

            // Perform matching logic
            compareRecords(combinedESPList, combinedFlixbusList);

            // Matched records list
            result.append(formatRecordsList());


        } catch (Exception e) {
            logger.severe("An error occurred while comparing files: " + e.getMessage());
            e.printStackTrace();
        }
        return result.toString();
    }

    private static String generateSummary(List<ESPRecord> combinedESPList, List<FlixBusRecord> combinedFlixbusList, List<FeeRecord> combinedFlixBusFeeRecords) {
        double espTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::getAmount).sum();
        double suplierMarginTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::getSuplierMargin).sum();
        double flixbusTotalCash = combinedFlixbusList.stream().mapToDouble(FlixBusRecord::getCash).sum();
        double absoluteDifference = Math.abs(espTotalAmount - flixbusTotalCash);
        double totalComm_gross = combinedFlixbusList.stream().mapToDouble(FlixBusRecord::getComm_gross).sum();
        double commGrossSupplierMarginDiff = Math.abs(totalComm_gross - suplierMarginTotalAmount);
        double combinedESPListTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::getTotalAmount).sum();
        double combinedFlixBusListTotalAmount = getFlixBusTotalAmount(combinedFlixbusList,combinedFlixBusFeeRecords);
       double TotalAmountDifference = Math.abs(combinedESPListTotalAmount - combinedFlixBusListTotalAmount);
        return String.format("ESP summary:     %.2f  |   Suplier Margin:   %.3f  |   ESP Total Amount: %.2f%n" +
                        "Flixbus summary: %.2f  |   Total Comm_gross: %.3f |  Flixbus Total Amount: %.2f%n" +
                        "Difference:      %.2f    |   Difference:       %.3f  |   Difference: %.2f%n",
                espTotalAmount, suplierMarginTotalAmount,combinedESPListTotalAmount, flixbusTotalCash, totalComm_gross,combinedFlixBusListTotalAmount ,absoluteDifference, commGrossSupplierMarginDiff,TotalAmountDifference);
    }
    public static String printServiceFee(List<ESPRecord> espRecords, List<Record> feeRecords) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("%n%-20s | %-15s | %-10s%n", "FlixBus Booking Number", "Trip Services", "Cash"));

        List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
        List<FlixBusRecord> combinedFlixbusList = combineFlixBusRecords(feeRecords);

        sortRecords(combinedESPList, combinedFlixbusList);

        result.append(printServiceFeeComparison(combinedESPList, combinedFlixbusList));
        return result.toString();
    }

    private static List<FlixBusRecord> combineFlixBusRecords(List<Record> flixbusRecords) {
        Map<String, FlixBusRecord> combinedFlixBusRecords = new HashMap<>();
        for (Record record : flixbusRecords) {
            if (record instanceof FlixBusRecord flixBusRecord) {
                combinedFlixBusRecords.merge(flixBusRecord.getBookingNumber(), flixBusRecord, (existing, newRecord) ->
                        new FlixBusRecord(
                                existing.getBookingNumber(),
                                existing.getTripServices(),
                                existing.getCash() + newRecord.getCash(),
                                existing.getVoucher() + newRecord.getVoucher(), // Sum up voucher
                                existing.getPaymentType(),
                                existing.getComm_gross() + newRecord.getComm_gross(), // Sum up comm_gross
                                existing.getTotalAmount() + newRecord.getTotalAmount()
                        ));
            }

        }
        return new ArrayList<>(combinedFlixBusRecords.values());
    }
    public static double getFlixBusTotalAmount(List<FlixBusRecord> flixbusRecords, List<FeeRecord> feeRecords) {
        return flixbusRecords.stream().mapToDouble(FlixBusRecord::getTotalAmount).sum()
                + feeRecords.stream().mapToDouble(FeeRecord::getFeeAmount).sum();
    }
    private static List<FeeRecord> combineFlixBusFeeRecords(List<Record> feeRecords){
        Map<String, FeeRecord> combinedFlixBusFeeRecords = new HashMap<>();
        for (Record record : feeRecords) {
            if (record instanceof FeeRecord feeRecord) {
                combinedFlixBusFeeRecords.merge(feeRecord.getBookingNumber(), feeRecord, (existing, newRecord) ->
                        new FeeRecord(
                                existing.getBookingNumber(),
                                existing.getFeeAmount() + newRecord.getFeeAmount()
                        ));
            }
        }
        return new ArrayList<>(combinedFlixBusFeeRecords.values());
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

    private static String formatRecordsList() {
        StringBuilder result = new StringBuilder();
        result.append("Matched Records:\n");
        result.append(String.format("%-20s | %-10s | %-20s|%-20s | %-10s | %-10s%n", "ESP Serial", "ESP Amount","Total Amount", "Booking Number", "Cash", "Cash Equal"));
        for (MatchedRecord record : matchedRecordsList) {
            ESPRecord espRecord = record.getEspRecord();
            FlixBusRecord flixBusRecord = record.getFlixBusRecord();
            String formattedBookingNumber = new java.math.BigDecimal(flixBusRecord.getBookingNumber()).toPlainString();
            String cashEqual = String.format("%.2f", espRecord.getAmount()).equals(String.format("%.2f", flixBusRecord.getCash())) ? "Equal" : "Not Equal";
            result.append(String.format("%-20s | %-10.2f | %-20.2f |%-20s| %-10.2f | %-10s%n",
                    espRecord.getSerialNumber(), espRecord.getAmount(),espRecord.getTotalAmount() ,formattedBookingNumber, flixBusRecord.getCash(), cashEqual));
        }
        result.append("\nUnmatched FlixBus Records:\n");
        for (FlixBusRecord record : unmatchedFlixbusList) {
            String formattedBookingNumber = new java.math.BigDecimal(record.getBookingNumber()).toPlainString();
            result.append(String.format("%-20s | %-10.2f%n", formattedBookingNumber, record.getCash()));
        }
        result.append("\nUnmatched ESP Records:\n");
        for (ESPRecord record : unmatchedESPList) {
            result.append(String.format("%-20s | %-10.2f%n", record.getSerialNumber(), record.getAmount()));
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