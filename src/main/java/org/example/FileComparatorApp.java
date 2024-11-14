package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileComparatorApp extends Application {
    private static final Logger logger = Logger.getLogger(FileComparatorApp.class.getName());

    private TextField file1PathField;
    private TextField file2PathField;
    private TextArea resultArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("File Comparator");

        Label file1Label = new Label("File 1:");
        file1PathField = new TextField();
        Button file1Button = new Button("Browse...");
        file1Button.setOnAction(e -> chooseFile(file1PathField));

        Label file2Label = new Label("File 2:");
        file2PathField = new TextField();
        Button file2Button = new Button("Browse...");
        file2Button.setOnAction(e -> chooseFile(file2PathField));

        Button compareButton = new Button("Compare");
        compareButton.setOnAction(e -> compareFiles());

        resultArea = new TextArea();
        resultArea.setEditable(false);

        VBox vbox = new VBox(10, file1Label, file1PathField, file1Button, file2Label, file2PathField, file2Button, compareButton, resultArea);
        vbox.setPrefSize(500, 500); // Set preferred size for VBox
        VBox.setVgrow(resultArea, Priority.ALWAYS); // Allow TextArea to grow

        Scene scene = new Scene(vbox, 700, 700); // Increase the size of the Scene

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void chooseFile(TextField textField) {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            textField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void compareFiles() {
        String file1Path = file1PathField.getText();
        String file2Path = file2PathField.getText();

        try {
            List<ESPRecord> espRecords = null;
            List<Record> flixbusRecords = null;
            List<Record> flixbusFeeRecords = null;
            List<Record> voucherFlixbusRecords = null;
            String fileType1 = FileProcessor.determineFileType(file1Path);
            String fileType2 = FileProcessor.determineFileType(file2Path);

            if ("CSV".equals(fileType1)) {
                espRecords = FileProcessor.readESPFile(file1Path);
            } else if ("EXCEL".equals(fileType1)) {
                voucherFlixbusRecords = FileProcessor.readFlixBusFile(file1Path);
                flixbusRecords = FileProcessor.readFlixBusFile(file1Path);
                flixbusFeeRecords = FileProcessor.readFlixBusFile(file1Path);
            }

            if ("CSV".equals(fileType2)) {
                espRecords = FileProcessor.readESPFile(file2Path);
            } else if ("EXCEL".equals(fileType2)) {
                voucherFlixbusRecords = FileProcessor.readFlixBusFile(file2Path);
                flixbusRecords = FileProcessor.readFlixBusFile(file2Path);
                flixbusFeeRecords = FileProcessor.readFlixBusFile(file2Path);
            }

            if (espRecords != null && flixbusRecords != null) {
                String comparisonResult = ComparingFiles.compareFiles(espRecords, flixbusRecords, flixbusFeeRecords, voucherFlixbusRecords);
                String serviceFeeResult = ComparingFiles.printServiceFee(espRecords, flixbusFeeRecords);
                resultArea.setText(comparisonResult + "\n" + serviceFeeResult);
            } else {
                resultArea.setText("Please provide one ESP CSV file and one FlixBus Excel file.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred while comparing files.", e);
            resultArea.setText("An error occurred while comparing files.");
        }
    }
}