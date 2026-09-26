package coffee_pos.controller;

import coffee_pos.DBConnector;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showErrorAlert("All fields are required.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showErrorAlert("Passwords do not match.");
            return;
        }

        if (userExists(username)) {
            showErrorAlert("Username is already taken.");
            return;
        }

        try {
            String hashedPassword = hashPassword(password);
            saveUser(username, hashedPassword);
            showInfoAlert("Registration successful! You can now log in.");
            closeWindow();
            handleShowLogin();
        } catch (NoSuchAlgorithmException | SQLException e) {
            showErrorAlert("Error during registration: " + e.getMessage());
        }
    }

    private boolean userExists(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            showErrorAlert("Database error: " + e.getMessage());
            return true; // Assume user exists to prevent registration on error
        }
    }

    private void saveUser(String username, String hashedPassword) throws SQLException {
        String query = "INSERT INTO users (username, password, role_id) VALUES (?, ?, ?)";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setInt(3, 2); // Default role to Cashier
            pstmt.executeUpdate();
        }
    }

    @FXML
    private void handleShowLogin() {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coffee_pos/view/LoginView.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Coffee POS - Login");
            loginStage.setScene(new Scene(root));
            loginStage.show();
            closeWindow();
        }catch(IOException e){
            showErrorAlert("Could not open login form: "+ e.getMessage());
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Registration Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registration Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
