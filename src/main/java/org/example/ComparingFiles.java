package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ComparingFiles class provides methods to compare records from ESP and FlixBus files.
 * It combines, sorts, and matches records, and generates summaries and detailed comparison results.
 */
public class ComparingFiles {
    private static final Logger logger = Logger.getLogger(ComparingFiles.class.getName());
    private static final List<MatchedRecord> matchedRecordsList = new ArrayList<>();
    private static final List<FlixBusRecord> unmatchedFlixbusList = new ArrayList<>();
    private static final List<ESPRecord> unmatchedESPList = new ArrayList<>();

    /**
     * Compares records from ESP and FlixBus files and generates a comparison result.
     *
     * @param espRecords List of ESP records.
     * @param flixbusRecords List of FlixBus records.
     * @param feeRecords List of FlixBus fee records.
     * @param voucherFlixBusRecords List of FlixBus voucher records.
     * @return A string containing the comparison result.
     */
    public static String compareFiles(List<ESPRecord> espRecords, List<Record> flixbusRecords, List<Record> feeRecords, List<Record> voucherFlixBusRecords) {
        StringBuilder result = new StringBuilder();
        try {
            List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
            List<FlixBusRecord> combinedFlixbusList = combineFlixBusRecords(flixbusRecords);
            List<VoucherFlixBusRecord> combinedVoucherFlixBusList = combineVoucherFlixBusRecords(voucherFlixBusRecords, espRecords);
            List<FeeRecord> combinedFlixBusFeeRecords = combineFlixBusFeeRecords(feeRecords);

            sortRecords(combinedESPList, combinedFlixbusList);

            result.append(generateSummary(combinedESPList, combinedFlixbusList, combinedFlixBusFeeRecords));
            compareRecords(combinedESPList, combinedFlixbusList, combinedVoucherFlixBusList);
            result.append(formatRecordsList());

        } catch (Exception e) {
            logger.severe("An error occurred while comparing files: " + e.getMessage());
            logger.log(Level.SEVERE, "Exception: ", e);
        }
        return result.toString();
    }

    /**
     * Generates a summary of the combined ESP and FlixBus records.
     *
     * @param combinedESPList List of combined ESP records.
     * @param combinedFlixbusList List of combined FlixBus records.
     * @param combinedFlixBusFeeRecords List of combined FlixBus fee records.
     * @return A string containing the summary.
     */
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
        return String.format("ESP summary:     %.2f  |   Suplier Margin:   %14.2f  |   ESP Total Amount: %10.2f%n" +
                        "Flixbus summary: %.2f  |   Total Comm Gross: %9.2f  |  Flixbus Total Amount: %.2f%n" +
                        "Difference:      %12.2f    |   Difference:       %18.2f  |   Difference: %24.2f%n%n",
                espTotalAmount, suplierMarginTotalAmount, combinedESPListTotalAmount, flixbusTotalCash, totalComm_gross, combinedFlixBusListTotalAmount, absoluteDifference, commGrossSupplierMarginDiff, TotalAmountDifference);
    }

    /**
     * Combines voucher FlixBus records with ESP records.
     *
     * @param voucherFlixBusRecords List of voucher FlixBus records.
     * @param espRecords List of ESP records.
     * @return A list of combined voucher FlixBus records.
     */
    private static List<VoucherFlixBusRecord> combineVoucherFlixBusRecords(List<Record> voucherFlixBusRecords, List<ESPRecord> espRecords) {
        Map<String, ESPRecord> espRecordMap = new HashMap<>();
        for (ESPRecord espRecord : espRecords) {
            espRecordMap.put(formatSerialNumber(espRecord.serialNumber()), espRecord);
        }

        Map<String, VoucherFlixBusRecord> combinedVoucherFlixBusRecords = new HashMap<>();
        for (Record record : voucherFlixBusRecords) {
            if (record instanceof VoucherFlixBusRecord voucherFlixBusRecord) {
                String voucherSerial = formatSerialNumber(voucherFlixBusRecord.bookingNumber());
                if (espRecordMap.containsKey(voucherSerial)) {
                    combinedVoucherFlixBusRecords.merge(voucherFlixBusRecord.bookingNumber(), voucherFlixBusRecord, (existing, newRecord) ->
                            new VoucherFlixBusRecord(
                                    existing.bookingNumber(),
                                    existing.tripServices(),
                                    existing.voucher() + newRecord.voucher(),
                                    existing.comm_gross() + newRecord.comm_gross(),
                                    existing.totalAmount() + newRecord.totalAmount()
                            ));
                }
            }
        }
        return new ArrayList<>(combinedVoucherFlixBusRecords.values());
    }

    /**
     * Combines FlixBus records.
     *
     * @param flixbusRecords List of FlixBus records.
     * @return A list of combined FlixBus records.
     */
    private static List<FlixBusRecord> combineFlixBusRecords(List<Record> flixbusRecords) {
        Map<String, FlixBusRecord> combinedFlixBusRecords = new HashMap<>();
        for (Record record : flixbusRecords) {
            if (record instanceof FlixBusRecord flixBusRecord) {
                combinedFlixBusRecords.merge(flixBusRecord.bookingNumber(), flixBusRecord, (existing, newRecord) ->
                        new FlixBusRecord(
                                existing.bookingNumber(),
                                existing.tripServices(),
                                existing.cash() + newRecord.cash(),
                                existing.voucher() + newRecord.voucher(),
                                existing.comm_gross() + newRecord.comm_gross(),
                                existing.totalAmount() + newRecord.totalAmount()
                        ));
            }
        }
        return new ArrayList<>(combinedFlixBusRecords.values());
    }

    /**
     * Calculates the total amount from FlixBus records and fee records.
     *
     * @param flixbusRecords List of FlixBus records.
     * @param feeRecords List of fee records.
     * @return The total amount.
     */
    public static double getFlixBusTotalAmount(List<FlixBusRecord> flixbusRecords, List<FeeRecord> feeRecords) {
        return flixbusRecords.stream().mapToDouble(FlixBusRecord::totalAmount).sum()
                + feeRecords.stream().mapToDouble(FeeRecord::getFeeAmount).sum();
    }

    /**
     * Combines FlixBus fee records.
     *
     * @param feeRecords List of fee records.
     * @return A list of combined FlixBus fee records.
     */
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

    /**
     * Combines ESP records.
     *
     * @param espRecords List of ESP records.
     * @return A list of combined ESP records.
     */
    private static List<ESPRecord> combineESPRecords(List<ESPRecord> espRecords) {
        Map<String, ESPRecord> combinedESPRecords = new HashMap<>();
        for (ESPRecord record : espRecords) {
            combinedESPRecords.merge(record.serialNumber(), record, (existing, newRecord) ->
                    new ESPRecord(
                            existing.serialNumber(),
                            existing.amount() + newRecord.amount(),
                            existing.suplierMargin() + newRecord.suplierMargin(),
                            existing.suplierMargin() + newRecord.suplierMargin()
                    ));
        }
        return new ArrayList<>(combinedESPRecords.values());
    }

    /**
     * Sorts ESP and FlixBus records by their serial numbers and booking numbers, respectively.
     *
     * @param espRecords List of ESP records.
     * @param records List of FlixBus records.
     */
    private static void sortRecords(List<ESPRecord> espRecords, List<? extends Record> records) {
        espRecords.sort(Comparator.comparing(ESPRecord::serialNumber));
        records.sort(Comparator.comparing(Record::bookingNumber));
    }

    /**
     * Compares ESP and FlixBus records and identifies matched and unmatched records.
     *
     * @param espRecords List of ESP records.
     * @param flixbusRecords List of FlixBus records.
     * @param voucherFlixBusRecords List of voucher FlixBus records.
     */
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

    /**
     * Formats the list of matched and unmatched records into a string.
     *
     * @return A string containing the formatted records list.
     */
    private static String formatRecordsList() {
        StringBuilder result = new StringBuilder();

        boolean hasDifferentPrices = false;
        double epsilon_cash = 0.00001; // Adjusted epsilon for comparison
        double epsilon_comm_gross = 0.0001; // Adjusted epsilon for comparison
        for (MatchedRecord record : matchedRecordsList) {
            ESPRecord espRecord = record.espRecord();
            Record flixBusRecord = record.record();

            if (flixBusRecord instanceof FlixBusRecord flixRecord) {
                if (Math.abs(espRecord.amount() - flixRecord.cash()) > epsilon_cash ||
                        Math.abs(espRecord.suplierMargin() - flixRecord.comm_gross()) > epsilon_comm_gross) {
                    if (!hasDifferentPrices) {
                        result.append("Matched Records with Different Prices:\n");
                        result.append(String.format("%-18s| %-12s | %-10s | %-20s | %-10s | %-10s%n", "ESP Serial", "ESP Amount", "ESP Supp Margin", "Flixbus Booking Number", "Cash", "Flixbus Comm Gross"));
                        hasDifferentPrices = true;
                    }
                    String formattedBookingNumber = formatSerialNumber(flixRecord.bookingNumber());
                    result.append(String.format("%-14s | %-19.2f | %-26.2f | %-30s | %-10.2f | %-10.2f%n",
                            espRecord.serialNumber(), espRecord.amount(), espRecord.suplierMargin(), formattedBookingNumber, flixRecord.cash(), flixRecord.comm_gross()));
                }
            } else if (flixBusRecord instanceof VoucherFlixBusRecord voucherRecord) {
                if (Math.abs(espRecord.amount() - voucherRecord.voucher()) > epsilon_cash ||
                        Math.abs(espRecord.suplierMargin() - voucherRecord.comm_gross()) > epsilon_comm_gross) {
                    if (hasDifferentPrices) {
                        result.append("Matched Records with Different Prices:\n");
                        result.append(String.format("%-18s| %-12s | %-10s | %-20s | %-10s | %-10s%n", "ESP Serial", "ESP Amount", "ESP Supp Margin", "Voucher Booking Number", "Voucher", "Voucher Comm Gross"));
                    }
                    String formattedBookingNumber = formatSerialNumber(voucherRecord.bookingNumber());
                    result.append(String.format("%-13s | %-18.2f | %-25.2f | %-30s | %-10.2f | %-10.2f%n",
                            espRecord.serialNumber(), espRecord.amount(), espRecord.suplierMargin(), formattedBookingNumber, voucherRecord.voucher(), voucherRecord.comm_gross()));
                }
            }
        }

        if (!hasDifferentPrices) {
            result.append("All matched records have the same price.\n");
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
                result.append(String.format("%-20s | %-10.2f | %-10.2f%n", record.serialNumber(), record.amount(), record.suplierMargin()));
            }
        } else {
            result.append("\nNo unmatched ESP records.\n");
        }

        return result.toString();
    }
    /**
     * Prints the service fee comparison between ESP and FlixBus records.
     *
     * @param espRecords List of ESP records.
     * @param feeRecords List of FlixBus fee records.
     * @param flixbusRecords List of FlixBus records.
     * @param voucherFlixBusRecords List of FlixBus voucher records.
     * @return A string containing the service fee comparison result.
     */
    public static String printServiceFee(List<ESPRecord> espRecords, List<Record> feeRecords, List<Record> flixbusRecords, List<Record> voucherFlixBusRecords) {
        StringBuilder result = new StringBuilder();

        List<ESPRecord> combinedESPList = combineESPRecords(espRecords);
        List<FeeRecord> combinedFeeList = combineFlixBusFeeRecords(feeRecords);
        List<FlixBusRecord> combinedFlixBusList = combineFlixBusRecords(flixbusRecords);
        List<VoucherFlixBusRecord> combinedVoucherFlixBusList = combineVoucherFlixBusRecords(voucherFlixBusRecords, combinedESPList);

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

        if (matchedFeeRecordsList.isEmpty()) {
            result.append("No matched fees found combing esp fee with flixbus record and putting feeAmount to 0.00.\n");
            for (FlixBusRecord flixBusRecord : combinedFlixBusList) {
                unmatchedFeeList.add(new FeeRecord(flixBusRecord.bookingNumber(), 0.00));
            }
            for (VoucherFlixBusRecord voucherFlixBusRecord : combinedVoucherFlixBusList) {
                unmatchedFeeList.add(new FeeRecord(voucherFlixBusRecord.bookingNumber(), 0.00));
            }
            combinedFeeList = combineFlixBusFeeRecords(new ArrayList<>(unmatchedFeeList));
            unmatchedFeeList.clear();
            espRecordMap.clear();
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
            unmatchedESPFeeList = new ArrayList<>(espRecordMap.values());
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