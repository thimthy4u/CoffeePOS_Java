package coffee_pos.controller;

import coffee_pos.DBConnector;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.scene.control.Label;

public class SettingsController {

    @FXML
    private TextField taxRateField;
    @FXML
    private Label taxHelpText;

    @FXML
    private void initialize() {
        loadTaxRate();
    }

    private void loadTaxRate() {
        String query = "SELECT rate FROM tax_settings ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                double rate = rs.getDouble("rate");
                taxRateField.setText(String.valueOf(rate * 100));
                taxHelpText.setText("Current: "+String.valueOf(rate * 100));
            }

        } catch (SQLException e) {
            showErrorAlert("Failed to load tax rate: " + e.getMessage());
        }
    }

    @FXML
    private void handleSetTax() {
        String taxRateText = taxRateField.getText();
        if (taxRateText.isEmpty()) {
            showErrorAlert("Tax rate cannot be empty.");
            return;
        }

        try {
            double taxRate = Double.parseDouble(taxRateText);
            if (taxRate < 0) {
                showErrorAlert("Tax rate cannot be negative.");
                return;
            }

            String query = "UPDATE tax_settings SET rate = ? WHERE id = 1";
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setDouble(1, taxRate / 100.0);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    showInfoAlert("Tax rate updated successfully.");
                    loadTaxRate();
                } else {
                    showErrorAlert("Failed to update tax rate.");
                }

            } catch (SQLException e) {
                showErrorAlert("Database error: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            showErrorAlert("Invalid tax rate. Please enter a valid number.");
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Settings Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
