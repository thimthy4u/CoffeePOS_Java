package coffee_pos.controller;

import coffee_pos.DBConnector;
import coffee_pos.model.Role;
import coffee_pos.model.Shift;
import coffee_pos.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UserManagementController {

    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, Integer> userIdCol;
    @FXML
    private TableColumn<User, String> usernameCol;
    @FXML
    private TableColumn<User, String> fullNameCol;
    @FXML
    private TableColumn<User, String> roleCol;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField fullNameField;
    @FXML
    private ComboBox<Role> roleComboBox;
    @FXML
    private TableView<Shift> shiftTable;
    @FXML
    private TableColumn<Shift, Integer> shiftIdCol;
    @FXML
    private TableColumn<Shift, String> shiftTypeCol;
    @FXML
    private TableColumn<Shift, LocalDate> shiftDateCol;

    private ObservableList<User> userData = FXCollections.observableArrayList();
    private ObservableList<Role> roleData = FXCollections.observableArrayList();
    private ObservableList<Shift> shiftData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupTableColumns();
        loadRoles();
        loadUsers();

        userTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    populateForm(newValue);
                    loadShifts(newValue);
                });
    }

    private void setupTableColumns() {
        userIdCol.setCellFactory(col -> new TableCell<User, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        userTable.setItems(userData);

        shiftIdCol.setCellFactory(col -> new TableCell<Shift, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        shiftTypeCol.setCellValueFactory(new PropertyValueFactory<>("shiftType"));
        shiftDateCol.setCellValueFactory(new PropertyValueFactory<>("shiftDate"));
        shiftTable.setItems(shiftData);
    }

    private void loadRoles() {
        roleData.clear();
        String query = "SELECT id, role_name FROM roles";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                roleData.add(new Role(rs.getInt("id"), rs.getString("role_name")));
            }
            roleComboBox.setItems(roleData);
        } catch (SQLException e) {
            showErrorAlert("Failed to load roles: " + e.getMessage());
        }
    }

    private void loadUsers() {
        userData.clear();
        String query = "SELECT u.id, u.username, u.full_name, u.role_id, r.role_name FROM users u JOIN roles r ON u.role_id = r.id ORDER BY u.id";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                userData.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getInt("role_id"), rs.getString("role_name")));
            }
        } catch (SQLException e) {
            showErrorAlert("Failed to load users: " + e.getMessage());
        }
    }

    private void loadShifts(User user) {
        shiftData.clear();
        if (user == null) return;

        String query = "SELECT id, shift_type, shift_date FROM staff_shifts WHERE user_id = ? ORDER BY shift_date DESC";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, user.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Date shiftDateSql = rs.getDate("shift_date");
                LocalDate shiftDate = (shiftDateSql != null) ? shiftDateSql.toLocalDate() : null;
                shiftData.add(new Shift(rs.getInt("id"),
                        rs.getString("shift_type"),
                        shiftDate));
            }
        } catch (SQLException e) {
            showErrorAlert("Failed to load shifts: " + e.getMessage());
        }
    }

    private void populateForm(User user) {
        if (user != null) {
            usernameField.setText(user.getUsername());
            fullNameField.setText(user.getFullName());
            passwordField.clear();
            for (Role role : roleComboBox.getItems()) {
                if (role.getId() == user.getRoleId()) {
                    roleComboBox.getSelectionModel().select(role);
                    break;
                }
            }
        } else {
            clearForm();
        }
    }

    @FXML
    private void clearForm() {
        userTable.getSelectionModel().clearSelection();
        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();
        roleComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAddUser() {
        if (!validateInput(true)) return;

        String query = "INSERT INTO users (username, password, full_name, role_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, usernameField.getText());
            pstmt.setString(2, hashPassword(passwordField.getText()));
            pstmt.setString(3, fullNameField.getText());
            pstmt.setInt(4, roleComboBox.getSelectionModel().getSelectedItem().getId());
            pstmt.executeUpdate();
            loadUsers();
            clearForm();
        } catch (SQLException | NoSuchAlgorithmException e) {
            showErrorAlert("Failed to add user: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showErrorAlert("No user selected. Please select a user from the table to update.");
            return;
        }
        if (!validateInput(false)) return;

        String password = passwordField.getText();
        boolean passwordChanged = password != null && !password.trim().isEmpty();

        StringBuilder queryBuilder = new StringBuilder("UPDATE users SET username = ?, full_name = ?, role_id = ?");
        if (passwordChanged) {
            queryBuilder.append(", password = ?");
        }
        queryBuilder.append(" WHERE id = ?");

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
            pstmt.setString(1, usernameField.getText());
            pstmt.setString(2, fullNameField.getText());
            pstmt.setInt(3, roleComboBox.getSelectionModel().getSelectedItem().getId());
            if (passwordChanged) {
                pstmt.setString(4, hashPassword(password));
                pstmt.setInt(5, selectedUser.getId());
            } else {
                pstmt.setInt(4, selectedUser.getId());
            }
            pstmt.executeUpdate();
            loadUsers();
            clearForm();
        } catch (SQLException | NoSuchAlgorithmException e) {
            showErrorAlert("Failed to update user: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showErrorAlert("No user selected. Please select a user to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete User");
        confirmation.setHeaderText("Are you sure you want to delete the user: " + selectedUser.getUsername() + "?");
        confirmation.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "DELETE FROM users WHERE id = ?";
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, selectedUser.getId());
                pstmt.executeUpdate();
                loadUsers();
                clearForm();
            } catch (SQLException e) {
                showErrorAlert("Failed to delete user: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddShift() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showErrorAlert("Please select a user to add a shift for.");
            return;
        }

        Dialog<Shift> dialog = new Dialog<>();
        dialog.setTitle("Add New Shift");
        dialog.setHeaderText("Enter shift details for " + selectedUser.getFullName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> shiftTypeComboBox = new ComboBox<>();
        shiftTypeComboBox.getItems().addAll("Morning", "Afternoon", "Evening", "Night");
        shiftTypeComboBox.getSelectionModel().selectFirst();

        DatePicker shiftDatePicker = new DatePicker();
        shiftDatePicker.setValue(LocalDate.now());

        grid.add(new Label("Shift Type:"), 0, 0);
        grid.add(shiftTypeComboBox, 1, 0);
        grid.add(new Label("Date:"), 0, 1);
        grid.add(shiftDatePicker, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String shiftType = shiftTypeComboBox.getSelectionModel().getSelectedItem();
                LocalDate shiftDate = shiftDatePicker.getValue();
                if (shiftType == null || shiftDate == null) {
                    showErrorAlert("Shift Type and Date cannot be empty.");
                    return null;
                }
                return new Shift(0, shiftType, shiftDate);
            }
            return null;
        });

        Optional<Shift> result = dialog.showAndWait();
        result.ifPresent(shift -> {
            String query = "INSERT INTO staff_shifts (user_id, shift_type, shift_date) VALUES (?, ?, ?)";
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, selectedUser.getId());
                pstmt.setString(2, shift.getShiftType());
                pstmt.setDate(3, Date.valueOf(shift.getShiftDate()));
                pstmt.executeUpdate();
                loadShifts(selectedUser);
            } catch (SQLException e) {
                showErrorAlert("Failed to add shift: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteShift() {
        Shift selectedShift = shiftTable.getSelectionModel().getSelectedItem();
        if (selectedShift == null) {
            showErrorAlert("No shift selected. Please select a shift to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Shift");
        confirmation.setHeaderText("Are you sure you want to delete this shift?");
        confirmation.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "DELETE FROM staff_shifts WHERE id = ?";
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, selectedShift.getShiftId());
                pstmt.executeUpdate();
                loadShifts(userTable.getSelectionModel().getSelectedItem());
            } catch (SQLException e) {
                showErrorAlert("Failed to delete shift: " + e.getMessage());
            }
        }
    }

    private boolean validateInput(boolean isNewUser) {
        if (usernameField.getText().isEmpty() || fullNameField.getText().isEmpty() || roleComboBox.getSelectionModel().getSelectedItem() == null) {
            showErrorAlert("Username, Full Name, and Role cannot be empty.");
            return false;
        }
        if (isNewUser && passwordField.getText().isEmpty()) {
            showErrorAlert("Password is required for new users.");
            return false;
        }
        return true;
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
        alert.setTitle("User Management Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}