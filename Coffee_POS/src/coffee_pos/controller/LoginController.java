package coffee_pos.controller;

import coffee_pos.DBConnector;
import coffee_pos.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.event.ActionEvent;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        if (statusLabel != null) {
            statusLabel.setVisible(false);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorAlert("Username and password cannot be empty.");
            return;
        }

        String query = "SELECT u.id, u.password, r.role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.username = ?";
        try (Connection conn = DBConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String storedPasswordHash = rs.getString("password");
                String userRole = rs.getString("role_name");
                String enteredPasswordHash = hashPassword(password);

                if (storedPasswordHash.equals(enteredPasswordHash)) {
                    // Login successful
                    UserSession.getInstance().setUserId(userId);
                    UserSession.getInstance().setRole(userRole);
                    showMainApplication();
                } else {
                    showErrorAlert("Invalid username or password.");
                }
            } else {
                showErrorAlert("Invalid username or password.");
            }

        } catch (SQLException | NoSuchAlgorithmException e) {
            showErrorAlert("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coffee_pos/view/RegisterView.fxml"));
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Register New User");
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/coffee_pos/icons/logo.png")));
            stage.show();
            currentStage.close();
        } catch (IOException e) {
            showErrorAlert("Could not open registration form: " + e.getMessage());
        }
    }

    private void showMainApplication() {
        try {
            // Close the login stage
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();

            // Open the main application
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coffee_pos/view/MainView.fxml"));
            Parent root = loader.load();
            Stage mainStage = new Stage();
            mainStage.setTitle("Coffee POS");
            mainStage.setScene(new Scene(root));
            mainStage.getIcons().add(new Image(getClass().getResourceAsStream("/coffee_pos/icons/logo.png")));
            mainStage.show();

        } catch (IOException e) {
            showErrorAlert("Failed to load the main application.");
            System.out.println("Failed to load the main application." + e);
        }
    }

    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    @FXML
    public void handleFieldHover(MouseEvent event) {
        TextField field = (TextField) event.getSource();
        field.setStyle(field.getStyle() + "; -fx-border-color: #007bff; -fx-border-width: 2;");
    }

    @FXML
    public void handleFieldExit(MouseEvent event) {
        TextField field = (TextField) event.getSource();
        String originalStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 12; -fx-font-size: 14;";
        field.setStyle(originalStyle);
    }

    @FXML
    public void handleButtonHover(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (!button.isDisabled()) {
            button.setStyle(button.getStyle() + "; -fx-background-color: linear-gradient(to bottom, #0056b3, #004085);");
        }
    }

    @FXML
    public void handleButtonExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (!button.isDisabled()) {
            button.setStyle("-fx-background-color: linear-gradient(to bottom, #007bff, #0056b3); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16; -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    private void showStatusMessage(String message, String color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(javafx.scene.paint.Color.web(color));
            statusLabel.setVisible(true);
        }
    }

}
