package org.example;

import java.io.IOException;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            List<ESPRecord> espRecords = FileProcessor.readESPFile("src/main/java/org/example/xlsx_files/ESP_Bimedia_Flixbus_weekly_billing_report_20240916.csv");
            List<FlixBusRecord> flixbusRecords = FileProcessor.readFlixbusFile("src/main/java/org/example/xlsx_files/Flixbus-Booking-report-129005-Own.Solutions-Bimedia.xlsx");

            Comparator.compareFiles(espRecords, flixbusRecords);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}