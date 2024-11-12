package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileProcessor {

    public static List<ESPRecord> readESPFile(String filePath) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            return lines
                    .skip(1) // Skip header
                    .map(line -> line.split(";")) // Split by semicolon
                    .filter(parts -> parts.length > 17) // Ensure there are more than 17 columns
                    .map(parts -> {
                        double value = parts.length > 20 ? Double.parseDouble(parts[20]) : Double.parseDouble(parts[17]);
                        return new ESPRecord(parts[5], Double.parseDouble(parts[11]), value, Double.parseDouble(parts[16]));
                    })
                    .collect(Collectors.toList());
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
                if (record != null) {
                    records.add(record);
                }
            }
        }
        return records;
    }

    private static boolean containsTotal(Row row) {
        for (Cell cell : row) {
            if (getCellValue(cell).contains("Total")) {
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

        if ("Cash, Voucher".equals(paymentType) || "Credit card, Voucher".equals(paymentType)) {
            return null; // Skip records with paymentType "Cash, Voucher" or "Credit Card, Voucher"
        }

        double totalAmount = totalAmountStr.isEmpty() ? 0.0 : Double.parseDouble(totalAmountStr);
        double cash = cashStr.isEmpty() ? 0.0 : Double.parseDouble(cashStr);
        double voucher = voucherStr.isEmpty() ? 0.0 : Double.parseDouble(voucherStr);
        double comGross = comGrossStr.isEmpty() ? 0.0 : Double.parseDouble(comGrossStr);

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