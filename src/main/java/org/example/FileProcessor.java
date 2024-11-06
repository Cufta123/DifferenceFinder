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
        List<ESPRecord> records = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)
                .skip(1) // Skip header
                .map(line -> line.split(";")) // Split by semicolon
                .filter(parts -> parts.length >= 13) // Ensure there are at least 13 columns
                .map(parts -> new ESPRecord(parts[5], Double.parseDouble(parts[11]), Double.parseDouble(parts[17])))
                .collect(Collectors.toList());
        return records;
    }

    public static List<FlixBusRecord> readFlixbusFile(String filePath) throws IOException {
        List<FlixBusRecord> records = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) { // Skip header row
                    continue;
                }
                boolean containsTotal = false;
                for (Cell cell : row) {
                    if (getCellValue(cell).contains("Total")) {
                        containsTotal = true;
                        break;
                    }
                }
                if (containsTotal) {
                    continue;
                }

                String bookingNumber = getCellValue(row.getCell(3));
                String tripServices = getCellValue(row.getCell(10));
                if ("PlatformFee".equals(tripServices)) {
                    continue;
                }
                String cashStr = getCellValue(row.getCell(14));
                String platformFeeStr = getCellValue(row.getCell(12));
                String voucherStr = getCellValue(row.getCell(15)); // Assuming voucher is in column 15
                String paymentType = getCellValue(row.getCell(7)); // Assuming paymentType is in column 16

                if ("Cash, Voucher".equals(paymentType)) {
                    continue; // Skip records with paymentType "Cash, Voucher"
                }

                double cash = cashStr.isEmpty() ? 0.0 : Double.parseDouble(cashStr);
                double platformFee = platformFeeStr.isEmpty() ? 0.0 : Double.parseDouble(platformFeeStr);
                double voucher = voucherStr.isEmpty() ? 0.0 : Double.parseDouble(voucherStr);

                records.add(new FlixBusRecord(bookingNumber, tripServices, cash, platformFee, voucher, paymentType));
            }
        }

        return records;
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