package org.example;

import java.io.IOException;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
          //  List<ESPRecord> espRecords = FileProcessor.readESPFile("src/main/java/org/example/xlsx_files/TOB_Flixbus_monthly_billing_report_20240901.csv");
           // List<FlixBusRecord> flixbusRecords = FileProcessor.readFlixbusFile("src/main/java/org/example/xlsx_files/Booking-report-125295-Own.Solutions---Tobaccoland-API-08-2024-tobaccoland.xlsx");
           // List<FlixBusRecord> flixbusFeeRecords = FileProcessor.readFlixBusFileFee("src/main/java/org/example/xlsx_files/Booking-report-125295-Own.Solutions---Tobaccoland-API-08-2024-tobaccoland.xlsx");

            List<ESPRecord> espRecords = FileProcessor.readESPFile("src/main/java/org/example/xlsx_files/ESP_Bimedia_Flixbus_weekly_billing_report_20240916.csv");
            List<FlixBusRecord> flixbusRecords = FileProcessor.readFlixbusFile("src/main/java/org/example/xlsx_files/Flixbus-Booking-report-129005-Own.Solutions-Bimedia.xlsx");
            List<FlixBusRecord> flixbusFeeRecords = FileProcessor.readFlixBusFileFee("src/main/java/org/example/xlsx_files/Flixbus-Booking-report-129005-Own.Solutions-Bimedia.xlsx");

            ComparingFiles.compareFiles(espRecords, flixbusRecords);
            ComparingFiles.printServiceFee(espRecords, flixbusFeeRecords); // Pass both lists
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}