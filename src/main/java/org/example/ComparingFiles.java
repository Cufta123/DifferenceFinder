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

    public static String compareFiles(List<ESPRecord> espRecords, List<Record> flixbusRecords, List<Record> feeRecords, List<Record> voucherFlixBusRecords) {
        StringBuilder result = new StringBuilder();
        try {
            List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
            List<FlixBusRecord> combinedFlixbusList = combineFlixBusRecords(flixbusRecords);
            List<VoucherFlixBusRecord> combinedVoucherFlixBusList = combineVoucherFlixBusRecords(voucherFlixBusRecords);
            List<FeeRecord> combinedFlixBusFeeRecords = combineFlixBusFeeRecords(feeRecords);

            sortRecords(combinedESPList, combinedFlixbusList);

            // Generate summary
            result.append(generateSummary(combinedESPList, combinedFlixbusList, combinedFlixBusFeeRecords));

            // Perform matching logic
            compareRecords(combinedESPList, combinedFlixbusList, combinedVoucherFlixBusList);

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
    private static List<VoucherFlixBusRecord> combineVoucherFlixBusRecords(List<Record> voucherFlixBusRecords) {
        Map<String, VoucherFlixBusRecord> combinedVoucherFlixBusRecords = new HashMap<>();
        for (Record record : voucherFlixBusRecords) {
            if (record instanceof VoucherFlixBusRecord voucherFlixBusRecord) {
                combinedVoucherFlixBusRecords.merge(voucherFlixBusRecord.bookingNumber(), voucherFlixBusRecord, (existing, newRecord) ->
                        new VoucherFlixBusRecord(
                                existing.bookingNumber(),
                                existing.tripServices(),
                                existing.voucher() + newRecord.voucher(), // Sum up voucher
                                existing.paymentType(),
                                existing.comm_gross() + newRecord.comm_gross(), // Sum up comm_gross
                                existing.totalAmount() + newRecord.totalAmount()
                        ));
            }
        }
        return new ArrayList<>(combinedVoucherFlixBusRecords.values());
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

    private static void compareRecords(List<ESPRecord> espRecords, List<FlixBusRecord> flixbusRecords, List<VoucherFlixBusRecord> voucherFlixBusRecords) {
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

        // Now compare unmatched ESP records with VoucherFlixBusRecords
        Map<String, ESPRecord> unmatchedESPMap = new HashMap<>(espRecordMap);
        for (VoucherFlixBusRecord voucherRecord : voucherFlixBusRecords) {
            String voucherSerial = formatSerialNumber(voucherRecord.bookingNumber());
            ESPRecord matchedESPRecord = unmatchedESPMap.get(voucherSerial);

            if (matchedESPRecord != null) {
                matchedRecordsList.add(new MatchedRecord(matchedESPRecord, voucherRecord));
                unmatchedESPMap.remove(voucherSerial);
            }
        }

        unmatchedESPList.addAll(unmatchedESPMap.values());
    }


    private static String formatRecordsList() {
        StringBuilder result = new StringBuilder();

        boolean hasDifferentPrices = false;
        for (MatchedRecord record : matchedRecordsList) {
            ESPRecord espRecord = record.espRecord();
            Record flixBusRecord = record.record();
            double epsilon = 0.0001; // Small value to account for floating-point precision

            if (flixBusRecord instanceof FlixBusRecord flixRecord) {
                if (Math.abs(espRecord.amount() - flixRecord.cash()) > epsilon) {
                    if (!hasDifferentPrices) {
                        result.append("Matched Records with Different Prices:\n");
                        result.append(String.format("%-18s| %-12s | %-10s | %-10s | %-20s | %-10s | %-10s%n", "ESP Serial", "ESP Amount", "ESP Supp Margin", "ESP Comm Gross", "Flixbus Booking Number", "Cash", "Flixbus Comm Gross"));
                        hasDifferentPrices = true;
                    }
                    String formattedBookingNumber = formatSerialNumber(flixRecord.bookingNumber());
                    result.append(String.format("%-13s | %-18.2f | %-25.2f | %-25.2f | %-30s | %-10.2f | %-10.2f%n",
                            espRecord.serialNumber(), espRecord.amount(), espRecord.suplierMargin(), espRecord.suplierMargin(), formattedBookingNumber, flixRecord.cash(), flixRecord.comm_gross()));
                }
            } else if (flixBusRecord instanceof VoucherFlixBusRecord voucherRecord) {
                if (Math.abs(espRecord.amount() - voucherRecord.voucher()) > epsilon) {
                    if (hasDifferentPrices) {
                        result.append("Matched Records with Different Prices:\n");
                        result.append(String.format("%-18s| %-12s | %-10s | %-10s | %-20s | %-10s | %-10s%n", "ESP Serial", "ESP Amount", "ESP Supp Margin", "ESP Comm Gross", "Voucher Booking Number", "Voucher", "Voucher Comm Gross"));
                    }
                    String formattedBookingNumber = formatSerialNumber(voucherRecord.bookingNumber());
                    result.append(String.format("%-13s | %-18.2f | %-25.2f | %-25.2f | %-30s | %-10.2f | %-10.2f%n",
                            espRecord.serialNumber(), espRecord.amount(), espRecord.suplierMargin(), espRecord.suplierMargin(), formattedBookingNumber, voucherRecord.voucher(), voucherRecord.comm_gross()));
                }
            }
        }

        if (!hasDifferentPrices) {
            result.append("All matched records have the same price.\n");
        }
    // Check if there are any matched voucher records and display them as they affect total amounts
        boolean hasMatchedVoucherRecords = matchedRecordsList.stream().anyMatch(record -> record.record() instanceof VoucherFlixBusRecord);
        if (hasMatchedVoucherRecords) {
            result.append("\nMatched Voucher Records:\n");
            for (MatchedRecord record : matchedRecordsList) {
                if (record.record() instanceof VoucherFlixBusRecord voucherRecord) {
                    String formattedBookingNumber = formatSerialNumber(voucherRecord.bookingNumber());
                    result.append(String.format("%-13s | %-18.2f | %-25.2f | %-25.2f | %-30s | %-10.2f | %-10.2f%n",
                            record.espRecord().serialNumber(), record.espRecord().amount(), record.espRecord().suplierMargin(), record.espRecord().suplierMargin(), formattedBookingNumber, voucherRecord.voucher(), voucherRecord.comm_gross()));
                }
            }
        }

        if (!unmatchedFlixbusList.isEmpty()) {
            result.append("\nUnmatched FlixBus Records:\n");
            for (FlixBusRecord record : unmatchedFlixbusList) {
                String formattedBookingNumber = formatSerialNumber(record.bookingNumber());
                if (!formattedBookingNumber.isEmpty()) {
                    result.append(String.format("%-20s | %-10.2f | %-10.2f%n", formattedBookingNumber, record.cash(), record.comm_gross()));
                }
            }
        } else {
            result.append("\nNo unmatched FlixBus records.\n");
        }

        if (!unmatchedESPList.isEmpty()) {
            result.append("\nUnmatched ESP Records:\n");
            for (ESPRecord record : unmatchedESPList) {
                result.append(String.format("%-20s | %-10.2f | %-10.2f | %-10.2f%n", record.serialNumber(), record.amount(), record.suplierMargin(), record.suplierMargin()));
            }
        } else {
            result.append("\nNo unmatched ESP records.\n");
        }


        return result.toString();
    }

    public static String printServiceFee(List<ESPRecord> espRecords, List<Record> feeRecords, List<Record> flixbusRecords, List<Record> voucherFlixBusRecords) {
        StringBuilder result = new StringBuilder();

        List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
        List<FeeRecord> combinedFeeList = combineFlixBusFeeRecords(feeRecords);
        List<FlixBusRecord> combinedFlixBusList = combineFlixBusRecords(flixbusRecords);
        List<VoucherFlixBusRecord> combinedVoucherFlixBusList = combineVoucherFlixBusRecords(voucherFlixBusRecords);

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

        List<ESPRecord> unmatchedESPFeeList = new ArrayList<>(espRecordMap.values());

        // If there are no FlixBus fee records, match ESP fee records to FlixBus records or VoucherFlixBus records
        if (matchedFeeRecordsList.isEmpty()) {
            result.append("No flixbus fee records found, comparing IDs instead.\n");
            for (ESPRecord espRecord : unmatchedESPFeeList) {
                String espSerial = formatSerialNumber(espRecord.serialNumber());
                for (FlixBusRecord flixBusRecord : combinedFlixBusList) {
                    String flixBusSerial = formatSerialNumber(flixBusRecord.bookingNumber());
                    if (espSerial.equals(flixBusSerial)) {
                        matchedFeeRecordsList.add(new MatchedRecord(espRecord, flixBusRecord));
                        espRecordMap.remove(espSerial); // Ensure the map is updated correctly
                    }
                }
                for (VoucherFlixBusRecord voucherRecord : combinedVoucherFlixBusList) {
                    String voucherSerial = formatSerialNumber(voucherRecord.bookingNumber());
                    if (espSerial.equals(voucherSerial)) {
                        matchedFeeRecordsList.add(new MatchedRecord(espRecord, voucherRecord));
                        espRecordMap.remove(espSerial); // Ensure the map is updated correctly
                    }
                }
            }
            unmatchedESPFeeList.clear();
            unmatchedESPFeeList.addAll(espRecordMap.values());
        }

        boolean hasDifferentAmounts = false;
        for (MatchedRecord record : matchedFeeRecordsList) {
            ESPRecord espRecord = record.espRecord();
            Record feeRecord = record.record();
            if (feeRecord instanceof FeeRecord fee) {
                if (Math.abs(espRecord.serviceFee() - fee.getFeeAmount()) > 0.0001) {
                    if (!hasDifferentAmounts) {
                        result.append("\nMatched Fee Records with Different Amounts:\n");
                        hasDifferentAmounts = true;
                    }
                    String formattedBookingNumber = formatSerialNumber(fee.bookingNumber());
                    result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f%n",
                            espRecord.serialNumber(), espRecord.serviceFee(), formattedBookingNumber, fee.getFeeAmount()));
                }
            } else if (feeRecord instanceof FlixBusRecord flixBus) {
                if (Math.abs(espRecord.serviceFee() - flixBus.cash()) > 0.0001) {
                    if (!hasDifferentAmounts) {
                        result.append("\nMatched Fee Records with Different Amounts:\n");
                        hasDifferentAmounts = true;
                    }
                    String formattedBookingNumber = formatSerialNumber(flixBus.bookingNumber());
                    result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f%n",
                            espRecord.serialNumber(), espRecord.serviceFee(), formattedBookingNumber, flixBus.cash()));
                }
            } else if (feeRecord instanceof VoucherFlixBusRecord voucher) {
                if (Math.abs(espRecord.serviceFee() - voucher.voucher()) > 0.0001) {
                    if (!hasDifferentAmounts) {
                        result.append("\nMatched Fee Records with Different Amounts:\n");
                        hasDifferentAmounts = true;
                    }
                    String formattedBookingNumber = formatSerialNumber(voucher.bookingNumber());
                    result.append(String.format("%-20s | %-10.2f | %-20s | %-10.2f%n",
                            espRecord.serialNumber(), espRecord.serviceFee(), formattedBookingNumber, voucher.voucher()));
                }
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
        try {
            return new java.math.BigDecimal(serialNumber).toPlainString();
        } catch (NumberFormatException e) {
            return serialNumber;
        }
    }
}