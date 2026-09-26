package coffee_pos.controller;

import coffee_pos.DBConnector;
import coffee_pos.model.Category;
import coffee_pos.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;

public class ProductController {

    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, Integer> idColumn;
    @FXML
    private TableColumn<Product, String> nameColumn;
    @FXML
    private TableColumn<Product, Double> priceColumn;
    @FXML
    private TableColumn<Product, Integer> stockColumn;
    @FXML
    private TableColumn<Product, String> imageColumn;
    @FXML
    private TableColumn<Product, String> categoryColumn;

    @FXML
    private TextField nameField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField stockField;
    @FXML
    private ImageView imageView;
    @FXML
    private ComboBox<Category> categoryComboBox;
    
    @FXML Label recordCountLabel;

    private ObservableList<Product> productData = FXCollections.observableArrayList();
    private ObservableList<Category> categoryData = FXCollections.observableArrayList();
    private String currentImagePath;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
        imageColumn.setCellFactory(param -> new TableCell<>() {
            private final ImageView cellImageView = new ImageView();
            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        File file = new File("src/" + imagePath);
                        Image image = new Image(file.toURI().toString(), 50, 50, true, true);
                        cellImageView.setImage(image);
                        setGraphic(cellImageView);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });

        productTable.setItems(productData);
        loadCategories();
        loadProductsFromDatabase();

        productTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showProductDetails(newValue));
        
        String query = "SELECT * FROM products";
        try (Connection conn = DBConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            int total=1;
            while(rs.next()){
                recordCountLabel.setText(total+" items");
                total=total+1;
            }
        } catch (SQLException e) {
            showErrorAlert("Database error", "Failed to load products: " + e.getMessage());
        }
    }

    private void loadCategories() {
        categoryData.clear();
        String query = "SELECT * FROM category";
        try (Connection conn = DBConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                categoryData.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                ));
            }
            categoryComboBox.setItems(categoryData);
        } catch (SQLException e) {
            showErrorAlert("Database error", "Failed to load categories: " + e.getMessage());
        }
    }

    private void loadProductsFromDatabase() {
        productData.clear();
        String query = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN category c ON p.category_id = c.id";
        try (Connection conn = DBConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                productData.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getString("image"),
                        rs.getInt("category_id"),
                        rs.getString("category_name")
                ));
            }
        } catch (SQLException e) {
            showErrorAlert("Database Error",e.getMessage());
        }
    }

    private void showProductDetails(Product product) {
        if (product != null) {
            nameField.setText(product.getName());
            priceField.setText(Double.toString(product.getPrice()));
            stockField.setText(Integer.toString(product.getStock()));
            currentImagePath = product.getImage();
            try {
                File file = new File("src/" + currentImagePath);
                Image image = new Image(file.toURI().toString());
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.setImage(null);
            }

            // Select category in ComboBox
            for (Category category : categoryComboBox.getItems()) {
                if (category.getId() == product.getCategoryId()) {
                    categoryComboBox.getSelectionModel().select(category);
                    break;
                }
            }

        } else {
            clearFields();
        }
    }
    

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(productTable.getScene().getWindow());

        if (selectedFile != null) {
            try {
                File destDir = new File("src/coffee_pos/image");
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                String fileName = selectedFile.getName();
                File destFile = new File(destDir, fileName);
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                currentImagePath = "coffee_pos/image/" + fileName;

                Image image = new Image(destFile.toURI().toString());
                imageView.setImage(image);

            } catch (IOException e) {
                showErrorAlert("I/O Error","Could not save image file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddButton() {
        if(validateInput()){
            String query = "INSERT INTO products (name, price, stock, image, category_id) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, nameField.getText());
                pstmt.setDouble(2, Double.parseDouble(priceField.getText()));
                pstmt.setInt(3, Integer.parseInt(stockField.getText()));
                pstmt.setString(4, currentImagePath);
                pstmt.setInt(5, categoryComboBox.getSelectionModel().getSelectedItem().getId());
                pstmt.executeUpdate();

                loadProductsFromDatabase();
                clearFields();
            } catch (SQLException e) {
                showErrorAlert("Database Error",e.getMessage());
            } catch (NumberFormatException e) {
                showErrorAlert("Invalid input", "Please check the fields.");
            }
        }
    }

    @FXML
    private void handleUpdateButton() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showErrorAlert("Selection Error", "Please select a product to update.");
            return;
        };

        if(validateInput()){
            String query = "UPDATE products SET name = ?, price = ?, stock = ?, image = ?, category_id = ? WHERE id = ?";
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, nameField.getText());
                pstmt.setDouble(2, Double.parseDouble(priceField.getText()));
                pstmt.setInt(3, Integer.parseInt(stockField.getText()));
                pstmt.setString(4, currentImagePath);
                pstmt.setInt(5, categoryComboBox.getSelectionModel().getSelectedItem().getId());
                pstmt.setInt(6, selectedProduct.getId());
                pstmt.executeUpdate();

                loadProductsFromDatabase();
            } catch (SQLException e) {
                showErrorAlert("Database Error",e.getMessage());
            } catch (NumberFormatException e) {
                showErrorAlert("Invalid input", "Please check the fields.");
            }
        }
    }

    @FXML
    private void handleDeleteButton() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null){
            showErrorAlert("Selection Error", "Please select a product to delete.");
            return;
        }

        String query = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, selectedProduct.getId());
            pstmt.executeUpdate();

            loadProductsFromDatabase();
        } catch (SQLException e) {
            showErrorAlert("Database Error",e.getMessage());
        }
    }

    @FXML
    private void handleClearButton() {
        clearFields();
        productTable.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        nameField.clear();
        priceField.clear();
        stockField.clear();
        currentImagePath = null;
        imageView.setImage(null);
        categoryComboBox.getSelectionModel().clearSelection();
    }

    private boolean validateInput() {
        if (nameField.getText().isEmpty() || priceField.getText().isEmpty() || stockField.getText().isEmpty() || categoryComboBox.getSelectionModel().getSelectedItem() == null) {
            showErrorAlert("Validation Error", "All fields are required.");
            return false;
        }
        try {
            Double.parseDouble(priceField.getText());
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showErrorAlert("Invalid input", "Price and stock must be numbers.");
            return false;
        }
        return true;
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}