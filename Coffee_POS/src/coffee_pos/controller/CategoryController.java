
package coffee_pos.controller;

import coffee_pos.DBConnector;
import coffee_pos.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CategoryController {

    @FXML
    private TableView<Category> categoryTableView;
    @FXML
    private TableColumn<Category, Integer> idColumn;
    @FXML
    private TableColumn<Category, String> nameColumn;
    @FXML
    private TableColumn<Category, String> descriptionColumn;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;

    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryTableView.setItems(categoryList);

        loadCategories();

        categoryTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showCategoryDetails(newValue));
    }

    private void loadCategories() {
        categoryList.clear();
        String query = "SELECT * FROM category";
        try (Connection conn = DBConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                categoryList.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            showErrorAlert("Database error", "Failed to load categories: " + e.getMessage());
        }
    }

    private void showCategoryDetails(Category category) {
        if (category != null) {
            nameField.setText(category.getName());
            descriptionArea.setText(category.getDescription());
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleAddButton() {
        String name = nameField.getText();
        String description = descriptionArea.getText();

        if (name.isEmpty()) {
            showErrorAlert("Validation Error", "Name field cannot be empty.");
            return;
        }

        String query = "INSERT INTO category (name, description) VALUES (?, ?)";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.executeUpdate();

            loadCategories();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Category added successfully.");
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to add category: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateButton() {
        Category selectedCategory = categoryTableView.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            showErrorAlert("Selection Error", "Please select a category to update.");
            return;
        }

        String name = nameField.getText();
        String description = descriptionArea.getText();

        if (name.isEmpty()) {
            showErrorAlert("Validation Error", "Name field cannot be empty.");
            return;
        }

        String query = "UPDATE category SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setInt(3, selectedCategory.getId());
            pstmt.executeUpdate();

            loadCategories();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Category updated successfully.");
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to update category: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteButton() {
        Category selectedCategory = categoryTableView.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            showErrorAlert("Selection Error", "Please select a category to delete.");
            return;
        }

        String query = "DELETE FROM category WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, selectedCategory.getId());
            pstmt.executeUpdate();

            loadCategories();
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Category deleted successfully.");
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to delete category: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearButton() {
        clearFields();
        categoryTableView.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        nameField.clear();
        descriptionArea.clear();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }
}
