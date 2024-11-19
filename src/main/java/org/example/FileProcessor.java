package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileProcessor {

    public static List<ESPRecord> readESPFile(String filePath) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            Iterator<String> iterator = lines.iterator();
            if (!iterator.hasNext()) {
                return Collections.emptyList();
            }

            // Read the header row
            String headerLine = iterator.next();
            String[] headers = headerLine.split(";");
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            // Get the indices for the required columns
            int serialNumberIndex = headerMap.get("Serial Number");
            int amountIndex = headerMap.get("Amount");
            int serviceFeeIndex = headerMap.get("Flixbus Service Fee");
            int supplierMarginIndex = headerMap.get("Supplier Margin (Inc. Tax)");

            // Process the remaining lines
            List<ESPRecord> records = new ArrayList<>();
            while (iterator.hasNext()) {
                String line = iterator.next();
                String[] parts = line.split(";");
                if (parts.length > Math.max(serviceFeeIndex, supplierMarginIndex)) {
                    double value = parts.length > serviceFeeIndex ? Double.parseDouble(parts[serviceFeeIndex]) : Double.parseDouble(parts[supplierMarginIndex]);
                    records.add(new ESPRecord(parts[serialNumberIndex], Double.parseDouble(parts[amountIndex]), value, Double.parseDouble(parts[supplierMarginIndex])));
                }
            }
            return records;
        }
    }


    static List<Record> readFlixBusFile(String filePath) throws IOException {
        List<Record> records = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0 || containsTotal(row)) { // Skip header and total rows
                    continue;
                }
                Record record = parseFlixBusRow(row);
                records.add(record);
            }
        }
        return records;
    }

    private static boolean containsTotal(Row row) {
        for (Cell cell : row) {
            if (getCellValue(cell).contains("Total")|| getCellValue(cell).contains("Summe")) {
                return true;
            }
        }
        return false;
    }

    private static Record parseFlixBusRow(Row row) {
        String bookingNumber = getCellValue(row.getCell(3));
        String tripServices = getCellValue(row.getCell(10));
        String totalAmountStr = getCellValue(row.getCell(14));
        String cashStr = getCellValue(row.getCell(14));
        String voucherStr = getCellValue(row.getCell(15));
        String paymentType = getCellValue(row.getCell(7));
        String comGrossStr = getCellValue(row.getCell(16));



        double totalAmount = totalAmountStr.isEmpty() ? 0.0 : Double.parseDouble(totalAmountStr);
        double cash = cashStr.isEmpty() ? 0.0 : Double.parseDouble(cashStr);
        double voucher = voucherStr.isEmpty() ? 0.0 : Double.parseDouble(voucherStr);
        double comGross = comGrossStr.isEmpty() ? 0.0 : Double.parseDouble(comGrossStr);

        if ("Cash, Voucher".equals(paymentType) || "Credit card, Voucher".equals(paymentType)) {
            return new VoucherFlixBusRecord(bookingNumber, tripServices,voucher,paymentType,comGross,totalAmount);// Skip records with paymentType "Cash, Voucher" or "Credit Card, Voucher"
        }
        if ("PlatformFee".equals(tripServices)) {
            return new FeeRecord(bookingNumber, cash);
        } else {
            return new FlixBusRecord(bookingNumber, tripServices, cash, voucher, paymentType, comGross, totalAmount);
        }

    }

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public static String determineFileType(String filePath) {
        if (filePath.endsWith(".csv")) {
            return "CSV";
        } else if (filePath.endsWith(".xlsx")) {
            return "EXCEL";
        } else {
            return "UNKNOWN";
        }
    }
}