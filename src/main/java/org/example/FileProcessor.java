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

public class FileProcessor {
    public static List<ESPRecord> readESPFile(String filePath) throws IOException {
        return Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)
                .skip(1) // Skip header
                .map(line -> line.split(";")) // Split by semicolon
                .filter(parts -> parts.length >= 13) // Ensure there are at least 13 columns
                .map(parts -> new ESPRecord(parts[5], Double.parseDouble(parts[11]), Double.parseDouble(parts[17]), Double.parseDouble(parts[16])))
                .collect(Collectors.toList());
    }

    public static List<FlixBusRecord> readFlixBusFileFee(String filePath) throws IOException {
        return readFlixBusFile(filePath, true);
    }

    public static List<FlixBusRecord> readFlixbusFile(String filePath) throws IOException {
        return readFlixBusFile(filePath, false);
    }

    private static List<FlixBusRecord> readFlixBusFile(String filePath, boolean isFeeFile) throws IOException {
        List<FlixBusRecord> records = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0 || containsTotal(row)) { // Skip header and total rows
                    continue;
                }
                FlixBusRecord record = parseFlixBusRow(row, isFeeFile);
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


    private static FlixBusRecord parseFlixBusRow(Row row, boolean isFeeFile) {
        String bookingNumber = getCellValue(row.getCell(3));
        String tripServices = getCellValue(row.getCell(10));
        if (isFeeFile && !"PlatformFee".equals(tripServices)) {
            return null; // Skip non-fee records in fee file
        }
        if (!isFeeFile && "PlatformFee".equals(tripServices)) {
            return null; // Skip fee records in non-fee file
        }
        String cashStr = getCellValue(row.getCell(14));
        String voucherStr = getCellValue(row.getCell(15));
        String paymentType = getCellValue(row.getCell(7));
        String com_grossStr= getCellValue(row.getCell(16));

        if ("Cash, Voucher".equals(paymentType) || "Credit card, Voucher".equals(paymentType)) {
            return null; // Skip records with paymentType "Cash, Voucher" or "Credit Card, Voucher"
        }

        double cash = cashStr.isEmpty() ? 0.0 : Double.parseDouble(cashStr);
        double voucher = voucherStr.isEmpty() ? 0.0 : Double.parseDouble(voucherStr);
        double com_gross = com_grossStr.isEmpty() ? 0.0 : Double.parseDouble(com_grossStr);


        return new FlixBusRecord(bookingNumber, tripServices, cash, voucher, paymentType, com_gross);
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
}