package coffee_pos.controller;

import coffee_pos.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import javafx.scene.image.Image;

public class MainViewController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Button posButton;
    @FXML
    private Button productsButton;
    @FXML
    private Button categoryButton;
    @FXML
    private Button ordersButton;
    @FXML
    private Button reportButton;
    @FXML
    private Button userManagementButton;
    
    @FXML
    private Button settingsButton;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private Button selectedButton = null;

    @FXML
    private void initialize() {
        setupButtonHoverEffects();
        setSelectedButton(posButton);
        System.out.println("MainViewController: initialize called.");
        // Role-based access control
        String userRole = UserSession.getInstance().getRole();
        if (!"Admin".equalsIgnoreCase(userRole)) {
            productsButton.setVisible(false);
            productsButton.setManaged(false);
            categoryButton.setVisible(false);
            categoryButton.setManaged(false);
            userManagementButton.setVisible(false);
            userManagementButton.setManaged(false);
            settingsButton.setVisible(false);
            settingsButton.setManaged(false);
        }

        try {
            // Load the POS view by default
            handlePOS();
        } catch (IOException e) {
            showErrorAlert("Failed to load default view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupButtonHoverEffects() {
        // Setup hover effects for all buttons
        setupButtonHover(posButton, "#2980b9", "#3498db", "#1f618d");
        setupButtonHover(productsButton, "#4a6741", "#34495e", "#27ae60");
        setupButtonHover(categoryButton, "#4a6741", "#34495e", "#27ae60");
        setupButtonHover(ordersButton, "#4a6741", "#34495e", "#27ae60");
        setupButtonHover(reportButton, "#4a6741", "#34495e", "#27ae60");
        setupButtonHover(userManagementButton, "#4a6741", "#34495e", "#27ae60");
        setupButtonHover(settingsButton, "#4a6741", "#34495e", "#27ae60");

        // Special hover for logout button
        setupButtonHover(logoutButton, "#c0392b", "#e74c3c", "#a93226");
    }

    private void setupButtonHover(Button button, String hoverColor, String normalColor, String selectedColor) {
        button.setOnMouseEntered(e -> {
            if (selectedButton != button) {
                button.setStyle(button.getStyle().replaceAll("-fx-background-color: [^;]+",
                        "-fx-background-color: " + hoverColor));
                button.setScaleX(1.05);
                button.setScaleY(1.05);
            }
        });

        button.setOnMouseExited(e -> {
            if (selectedButton != button) {
                button.setStyle(button.getStyle().replaceAll("-fx-background-color: [^;]+",
                        "-fx-background-color: " + normalColor));
                button.setScaleX(1.0);
                button.setScaleY(1.0);
            }
        });

        // Add click handler for selection
        button.setOnMouseClicked(e -> {
            setSelectedButton(button);
        });
    }

    private void setSelectedButton(Button button) {
        // Reset previous selected button
        if (selectedButton != null) {
            resetButtonToNormal(selectedButton);
        }

        // Set new selected button
        selectedButton = button;

        // Apply selected style
        String selectedColor = getSelectedColor(button);
        button.setStyle(button.getStyle().replaceAll("-fx-background-color: [^;]+",
                "-fx-background-color: " + selectedColor));
        button.setStyle(button.getStyle() + "; -fx-border-color: white; -fx-border-width: 2px;");
        button.setScaleX(1.0);
        button.setScaleY(1.0);
    }

    private void resetButtonToNormal(Button button) {
        String normalColor = getNormalColor(button);
        button.setStyle(button.getStyle().replaceAll("-fx-background-color: [^;]+",
                "-fx-background-color: " + normalColor));
        button.setStyle(button.getStyle().replaceAll("; -fx-border-color: [^;]+", ""));
        button.setStyle(button.getStyle().replaceAll("; -fx-border-width: [^;]+", ""));
        button.setScaleX(1.0);
        button.setScaleY(1.0);
    }

    private String getSelectedColor(Button button) {
        if (button == posButton) {
            return "#1f618d";
        }
        if (button == logoutButton) {
            return "#a93226";
        }
        return "#27ae60";
    }

    private String getNormalColor(Button button) {
        if (button == posButton) {
            return "#3498db";
        }
        if (button == logoutButton) {
            return "#e74c3c";
        }
        return "#34495e";
    }

    @FXML
    private void handlePOS() throws IOException {
        System.out.println("MainViewController: handlePOS called.");
        loadView("POSView");
    }

    @FXML
    private void handleProducts() throws IOException {
        System.out.println("MainViewController: handleProducts called.");
        loadView("ProductView");
    }

    @FXML
    private void handleCategory() throws IOException {
        System.out.println("MainViewController: handleCategory called.");
        loadView("CategoryView");
    }

    @FXML
    private void handleOrders() throws IOException {
        System.out.println("MainViewController: handleOrders called.");
        loadView("OrderView");
    }

    @FXML
    private void handleReport() throws IOException {
        System.out.println("MainViewController: handleReport called.");
        loadView("ReportView");
    }

    @FXML
    private void handleUserManagement() throws IOException {
        System.out.println("MainViewController: handleUserManagement called.");
        loadView("UserManagementView");
    }

    @FXML
    private void handleSettings() throws IOException {
        System.out.println("MainViewController: handleSettings called.");
        loadView("SettingsView");
    }

    @FXML
    private void handleLogout() {
        System.out.println("MainViewController: handleLogout called.");
        try {
            // Clean the user session
            UserSession.getInstance().cleanUserSession();

            // Close the main application window
            Stage mainStage = (Stage) contentArea.getScene().getWindow();
            mainStage.close();

            // Open the login window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coffee_pos/view/LoginView.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.getIcons().add(new Image("coffee_pos/icons/logo.png"));
            loginStage.setScene(new Scene(root));
            loginStage.show();

        } catch (IOException e) {
            showErrorAlert("Failed to logout: " + e.getMessage());
        }
    }

    private void loadView(String fxmlFileName) throws IOException {
        System.out.println("MainViewController: loadView called for " + fxmlFileName);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coffee_pos/view/" + fxmlFileName + ".fxml"));
            Parent root = loader.load();
            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            showErrorAlert("Failed to load view: " + fxmlFileName + ".fxml - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
