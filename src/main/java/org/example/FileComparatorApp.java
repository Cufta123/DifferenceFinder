// FileComparatorApp.java
package org.example;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileComparatorApp extends Application {
    private static final Logger logger = Logger.getLogger(FileComparatorApp.class.getName());

    private TextField file1PathField;
    private TextField file2PathField;
    private TextArea resultArea;
    private ProgressBar progressBar;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("File Comparator");
        primaryStage.getIcons().add(new Image("file:src/main/resources/icon-1.png"));

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

        progressBar = new ProgressBar();
        progressBar.setVisible(false);

        VBox vbox = new VBox(10, file1Label, file1PathField, file1Button, file2Label, file2PathField, file2Button, compareButton, progressBar, resultArea);
        vbox.setPrefSize(500, 600); // Set preferred size for VBox
        VBox.setVgrow(resultArea, Priority.ALWAYS); // Allow TextArea to grow

        Scene scene = new Scene(vbox, 900, 800); // Increase the size of the Scene
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm()); // Apply CSS

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

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
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
                        String serviceFeeResult = ComparingFiles.printServiceFee(espRecords, flixbusFeeRecords, flixbusRecords, voucherFlixbusRecords);
                        updateMessage(comparisonResult + "\n" + serviceFeeResult);
                    } else {
                        updateMessage("Please provide one ESP CSV file and one FlixBus Excel file.");
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "An error occurred while comparing files.", e);
                    updateMessage("An error occurred while comparing files.");
                }
                return null;
            }
        };

        task.setOnRunning(e -> progressBar.setVisible(true));
        task.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            resultArea.setText(task.getMessage());
        });
        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            resultArea.setText("An error occurred while comparing files.");
        });

        new Thread(task).start();
    }
}