package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.logging.Level;
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
            List<FeeRecord> combinedFlixBusFeeRecords = combineFlixBusFeeRecords(feeRecords);

            sortRecords(combinedESPList, combinedFlixbusList);

            // Generate summary
            result.append(generateSummary(combinedESPList, combinedFlixbusList, combinedFlixBusFeeRecords));

            // Perform matching logic
            compareRecords(combinedESPList, combinedFlixbusList);

            // Matched records list
            result.append(formatRecordsList());


        } catch (Exception e) {
            logger.severe("An error occurred while comparing files: " + e.getMessage());
            logger.log(Level.SEVERE, "Exception: ", e);
        }
        return result.toString();
    }

    private static String generateSummary(List<ESPRecord> combinedESPList, List<FlixBusRecord> combinedFlixbusList, List<FeeRecord> combinedFlixBusFeeRecords) {
        double espTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::amount).sum();
        double suplierMarginTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::suplierMargin).sum();
        double flixbusTotalCash = combinedFlixbusList.stream().mapToDouble(FlixBusRecord::cash).sum();
        double absoluteDifference = Math.abs(espTotalAmount - flixbusTotalCash);
        double totalComm_gross = combinedFlixbusList.stream().mapToDouble(FlixBusRecord::comm_gross).sum();
        double commGrossSupplierMarginDiff = Math.abs(totalComm_gross - suplierMarginTotalAmount);
        double combinedESPListTotalAmount = combinedESPList.stream().mapToDouble(ESPRecord::getTotalAmount).sum();
        double combinedFlixBusListTotalAmount = getFlixBusTotalAmount(combinedFlixbusList, combinedFlixBusFeeRecords);
        double TotalAmountDifference = Math.abs(combinedESPListTotalAmount - combinedFlixBusListTotalAmount);
        return String.format("ESP summary:     %.2f  |   Suplier Margin:   %.3f  |   ESP Total Amount: %.2f%n" +
                        "Flixbus summary: %.2f  |   Total Comm_gross: %.3f |  Flixbus Total Amount: %.2f%n" +
                        "Difference:      %.2f    |   Difference:       %.3f  |   Difference: %.2f%n",
                espTotalAmount, suplierMarginTotalAmount, combinedESPListTotalAmount, flixbusTotalCash, totalComm_gross, combinedFlixBusListTotalAmount, absoluteDifference, commGrossSupplierMarginDiff, TotalAmountDifference);
    }


    private static List<FlixBusRecord> combineFlixBusRecords(List<Record> flixbusRecords) {
        Map<String, FlixBusRecord> combinedFlixBusRecords = new HashMap<>();
        for (Record record : flixbusRecords) {
            if (record instanceof FlixBusRecord flixBusRecord) {
                combinedFlixBusRecords.merge(flixBusRecord.bookingNumber(), flixBusRecord, (existing, newRecord) ->
                        new FlixBusRecord(
                                existing.bookingNumber(),
                                existing.tripServices(),
                                existing.cash() + newRecord.cash(),
                                existing.voucher() + newRecord.voucher(), // Sum up voucher
                                existing.paymentType(),
                                existing.comm_gross() + newRecord.comm_gross(), // Sum up comm_gross
                                existing.totalAmount() + newRecord.totalAmount()
                        ));
            }

        }
        return new ArrayList<>(combinedFlixBusRecords.values());
    }

    public static double getFlixBusTotalAmount(List<FlixBusRecord> flixbusRecords, List<FeeRecord> feeRecords) {
        return flixbusRecords.stream().mapToDouble(FlixBusRecord::totalAmount).sum()
                + feeRecords.stream().mapToDouble(FeeRecord::getFeeAmount).sum();
    }

    private static List<FeeRecord> combineFlixBusFeeRecords(List<Record> feeRecords) {
        Map<String, FeeRecord> combinedFlixBusFeeRecords = new HashMap<>();
        for (Record record : feeRecords) {
            if (record instanceof FeeRecord feeRecord) {
                combinedFlixBusFeeRecords.merge(feeRecord.bookingNumber(), feeRecord, (existing, newRecord) ->
                        new FeeRecord(
                                existing.bookingNumber(),
                                existing.getFeeAmount() + newRecord.getFeeAmount()
                        ));
            }
        }
        return new ArrayList<>(combinedFlixBusFeeRecords.values());
    }


    private static List<ESPRecord> combineESPRecords(List<ESPRecord> espRecords) {
        Map<String, ESPRecord> combinedESPRecords = new HashMap<>();
        for (ESPRecord record : espRecords) {
            combinedESPRecords.merge(record.serialNumber(), record, (existing, newRecord) ->
                    new ESPRecord(
                            existing.serialNumber(),
                            existing.amount() + newRecord.amount(),
                            existing.suplierMargin() + newRecord.suplierMargin(),
                            existing.suplierMargin() + newRecord.suplierMargin() // Correctly sum the suplierMargin
                    ));
        }
        return new ArrayList<>(combinedESPRecords.values());
    }

    private static void sortRecords(List<ESPRecord> espRecords, List<? extends Record> records) {
        espRecords.sort(Comparator.comparing(ESPRecord::serialNumber));
        records.sort(Comparator.comparing(Record::bookingNumber));
    }

    private static void compareRecords(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords) {
        unmatchedFlixbusList.clear();
        unmatchedESPList.clear();
        matchedRecordsList.clear();

        Map<String, ESPRecord> espRecordMap = new HashMap<>();
        for (ESPRecord espRecord : espRecords) {
            espRecordMap.put(formatSerialNumber(espRecord.serialNumber()), espRecord);
        }

        for (FlixBusRecord flixbusRecord : flixbusRecords) {
            String flixbusSerial = formatSerialNumber(flixbusRecord.bookingNumber());
            ESPRecord matchedESPRecord = espRecordMap.get(flixbusSerial);

            if (matchedESPRecord != null) {
                matchedRecordsList.add(new MatchedRecord(matchedESPRecord, flixbusRecord));
                espRecordMap.remove(flixbusSerial);
            } else {
                unmatchedFlixbusList.add(flixbusRecord);
            }
        }

        unmatchedESPList.addAll(espRecordMap.values());
    }


    private static String formatRecordsList() {
        StringBuilder result = new StringBuilder();
        result.append("Matched Records:\n");
        result.append(String.format("%-20s | %-10s | %-20s | %-10s | %-10s%n", "ESP Serial", "ESP Amount", "Booking Number", "Cash", "Cash Equal"));
        for (MatchedRecord record : matchedRecordsList) {
            ESPRecord espRecord = record.espRecord();
            FlixBusRecord flixBusRecord = (FlixBusRecord) record.record();
            String formattedBookingNumber = formatSerialNumber(flixBusRecord.bookingNumber());
            String cashEqual = String.format("%.2f", espRecord.amount()).equals(String.format("%.2f", flixBusRecord.cash())) ? "Equal" : "Not Equal";
            result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f | %-10s%n",
                    espRecord.serialNumber(), espRecord.amount(), formattedBookingNumber, flixBusRecord.cash(), cashEqual));
        }
        result.append("\nUnmatched FlixBus Records:\n");
        for (FlixBusRecord record : unmatchedFlixbusList) {
            String formattedBookingNumber = formatSerialNumber(record.bookingNumber());
            result.append(String.format("%-20s | %-10.2f%n", formattedBookingNumber, record.cash()));
        }
        result.append("\nUnmatched ESP Records:\n");
        for (ESPRecord record : unmatchedESPList) {
            result.append(String.format("%-20s | %-10.2f%n", record.serialNumber(), record.amount()));
        }
        return result.toString();
    }

    public static String printServiceFee(List<ESPRecord> espRecords, List<Record> feeRecords) {
        StringBuilder result = new StringBuilder();

        List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
        List<FeeRecord> combinedFeeList = combineFlixBusFeeRecords(feeRecords);

        sortRecords(combinedESPList, combinedFeeList);

        List<FeeRecord> unmatchedFeeList = new ArrayList<>();
        List<MatchedRecord> matchedFeeRecordsList = new ArrayList<>();

        Map<String, ESPRecord> espRecordMap = new HashMap<>();
        for (ESPRecord espRecord : combinedESPList) {
            espRecordMap.put(formatSerialNumber(espRecord.serialNumber()), espRecord);
        }

        for (FeeRecord feeRecord : combinedFeeList) {
            String feeSerial = formatSerialNumber(feeRecord.bookingNumber());
            ESPRecord matchedESPRecord = espRecordMap.get(feeSerial);

            if (matchedESPRecord != null) {
                matchedFeeRecordsList.add(new MatchedRecord(matchedESPRecord, feeRecord));
                espRecordMap.remove(feeSerial);
            } else {
                unmatchedFeeList.add(feeRecord);
            }
        }


        result.append("\nMatched Fee Records:\n");
        for (MatchedRecord record : matchedFeeRecordsList) {
            ESPRecord espRecord = record.espRecord();
            FeeRecord feeRecord = (FeeRecord) record.record();
            String formattedBookingNumber = formatSerialNumber(feeRecord.bookingNumber());
            result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f%n",
                    espRecord.serialNumber(), espRecord.serviceFee(), formattedBookingNumber, feeRecord.getFeeAmount()));
        }

        result.append("\nUnmatched Fee Records:\n");
        for (FeeRecord record : unmatchedFeeList) {
            String feeSerial = formatSerialNumber(record.bookingNumber());
            double feeAmount = record.getFeeAmount();
            result.append(String.format("%-21s | %.2f%n", feeSerial, feeAmount));
        }

        return result.toString();
    }

    private static String formatSerialNumber(String serialNumber) {
        if (serialNumber == null || serialNumber.isEmpty()) {
            return "N/A";
        }
        try {
            return new java.math.BigDecimal(serialNumber).toPlainString();
        } catch (NumberFormatException e) {
            return serialNumber;
        }
    }
}