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
                        "Difference:      %.2f    |   Difference:       %.3f  |   Difference: %.2f%n%n",
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

        boolean hasDifferentPrices = false;
        for (MatchedRecord record : matchedRecordsList) {
            ESPRecord espRecord = record.espRecord();
            FlixBusRecord flixBusRecord = (FlixBusRecord) record.record();
            double epsilon = 0.0001; // Small value to account for floating-point precision
            if (Math.abs(espRecord.amount() - flixBusRecord.cash()) > epsilon) {
                if (!hasDifferentPrices) {
                    result.append("Matched Records with Different Prices:\n");
                    result.append(String.format("%-20s | %-10s | %-20s | %-10s%n", "ESP Serial", "ESP Amount", "Flixbus Booking Number", "Cash"));
                    hasDifferentPrices = true;
                }
                String formattedBookingNumber = formatSerialNumber(flixBusRecord.bookingNumber());
                result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f%n",
                        espRecord.serialNumber(), espRecord.amount(), formattedBookingNumber, flixBusRecord.cash()));
            }
        }

        if (!hasDifferentPrices) {
            result.append("All matched records have the same price.\n");
        }

        if (!unmatchedFlixbusList.isEmpty()) {
            result.append("\nUnmatched FlixBus Records:\n");
            for (FlixBusRecord record : unmatchedFlixbusList) {
                String formattedBookingNumber = formatSerialNumber(record.bookingNumber());
                result.append(String.format("%-20s | %-10.2f%n", formattedBookingNumber, record.cash()));
            }
        } else {
            result.append("\nNo unmatched FlixBus records.\n");
        }

        if (!unmatchedESPList.isEmpty()) {
            result.append("\nUnmatched ESP Records:\n");
            for (ESPRecord record : unmatchedESPList) {
                result.append(String.format("%-20s | %-10.2f%n", record.serialNumber(), record.amount()));
            }
        } else {
            result.append("\nNo unmatched ESP records.\n");
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
        List<ESPRecord> unmatchedESPFeeList = new ArrayList<>();

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

        unmatchedESPFeeList.addAll(espRecordMap.values());

        boolean hasDifferentAmounts = false;
        for (MatchedRecord record : matchedFeeRecordsList) {
            ESPRecord espRecord = record.espRecord();
            FeeRecord feeRecord = (FeeRecord) record.record();
            if (Math.abs(espRecord.serviceFee() - feeRecord.getFeeAmount()) > 0.0001) {
                if (!hasDifferentAmounts) {
                    result.append("\nMatched Fee Records with Different Amounts:\n");
                    hasDifferentAmounts = true;
                }
                String formattedBookingNumber = formatSerialNumber(feeRecord.bookingNumber());
                result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f%n",
                        espRecord.serialNumber(), espRecord.serviceFee(), formattedBookingNumber, feeRecord.getFeeAmount()));
            }
        }

        if (!hasDifferentAmounts) {
            result.append("No matched fees with different amounts.\n");
        }

        if (!unmatchedFeeList.isEmpty()) {
            result.append("\nUnmatched FlixBus Fee Records:\n");
            for (FeeRecord record : unmatchedFeeList) {
                String feeSerial = formatSerialNumber(record.bookingNumber());
                double feeAmount = record.getFeeAmount();
                result.append(String.format("%-21s | %.2f%n", feeSerial, feeAmount));
            }
        } else {
            result.append("\nNo unmatched FlixBus Fee records.\n");
        }

        if (!unmatchedESPFeeList.isEmpty()) {
            result.append("\nUnmatched ESP Fee Records:\n");
            for (ESPRecord record : unmatchedESPFeeList) {
                result.append(String.format("%-20s | %-10.2f%n", record.serialNumber(), record.serviceFee()));
            }
        } else {
            result.append("\nNo unmatched ESP Fee records.\n");
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