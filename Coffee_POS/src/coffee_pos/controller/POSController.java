package coffee_pos.controller;

import coffee_pos.DBConnector;
import coffee_pos.UserSession;
import coffee_pos.model.Category;
import coffee_pos.model.OrderItem;
import coffee_pos.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.text.TextAlignment;

public class POSController {

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane productGrid;
    @FXML
    private FlowPane categoryFilterBox;
    @FXML
    private TableView<OrderItem> orderTable;
    @FXML
    private TableColumn<OrderItem, String> nameColumn;
    @FXML
    private TableColumn<OrderItem, Integer> quantityColumn;
    @FXML
    private TableColumn<OrderItem, Double> priceColumn;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label taxLabel;
    @FXML
    private Label totalLabel;

    private ObservableList<Product> products = FXCollections.observableArrayList();
    private ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
    private ObservableList<Category> categories = FXCollections.observableArrayList();
    private Category selectedCategory = null; // To keep track of the current category filter

    private double taxRate; // tax
    private int lastGeneratedWaitingNumber = 0;

    private static int dailyWaitingCounter = 0;
    private static LocalDate lastOrderDate = null;

    private synchronized int getNextWaitingNumber() {
        LocalDate today = LocalDate.now();
        if (lastOrderDate == null || !lastOrderDate.equals(today)) {
            lastOrderDate = today;
            dailyWaitingCounter = 1;
        } else {
            dailyWaitingCounter++;
        }
        return dailyWaitingCounter;
    }

    @FXML
    private void initialize() {
        setupOrderTable();
        loadProducts();
        loadTaxRate();
        loadCategoriesAndDisplayButtons();
        displayProducts(products);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });

        updateTotals();
    }

    private void loadCategoriesAndDisplayButtons() {
        String query = "SELECT * FROM category";
        try (Connection conn = DBConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {

            categories.clear();
            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                ));
            }

            categoryFilterBox.getChildren().clear();

            // Add 'All' button with special styling
            Button allButton = createCategoryButton("All ⭐", true);
            allButton.setOnAction(e -> {
                filterProductsByCategory(null);
                updateActiveButton(allButton);
            });
            categoryFilterBox.getChildren().add(allButton);

            // Store reference to "All" button as initially active
            activeButton = allButton;

            // Add button for each category
            for (Category category : categories) {
                Button categoryButton = createCategoryButton(category.getName(), false);
                categoryButton.setOnAction(e -> {
                    filterProductsByCategory(category);
                    updateActiveButton(categoryButton);
                });
                categoryFilterBox.getChildren().add(categoryButton);
            }

        } catch (SQLException e) {
            showErrorAlert("Failed to load categories: " + e.getMessage());
        }
    }

// Add this field to your class
    private Button activeButton = null;

    private Button createCategoryButton(String text, boolean isAllButton) {
        Button button = new Button(text);

        // Brown theme styling
        String baseStyle = "-fx-background-color: #f0e8d8; "
                + "-fx-border-color: #d4b896; "
                + "-fx-border-width: 1.5; "
                + "-fx-border-radius: 20; "
                + "-fx-background-radius: 20; "
                + "-fx-text-fill: #5d4037; "
                + "-fx-font-weight: bold; "
                + "-fx-font-size: 12px; "
                + "-fx-padding: 8 16 8 16; "
                + "-fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(139,101,75,0.2), 2, 0, 0, 1);";

        String hoverStyle = "-fx-background-color: #e8d5c4; "
                + "-fx-border-color: #c4a484; "
                + "-fx-border-width: 1.5; "
                + "-fx-border-radius: 20; "
                + "-fx-background-radius: 20; "
                + "-fx-text-fill: #4a2c20; "
                + "-fx-font-weight: bold; "
                + "-fx-font-size: 12px; "
                + "-fx-padding: 8 16 8 16; "
                + "-fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(139,101,75,0.3), 3, 0, 0, 1);";

        String activeStyle = "-fx-background-color: #8b654b; "
                + "-fx-border-color: #6d4c35; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 20; "
                + "-fx-background-radius: 20; "
                + "-fx-text-fill: white; "
                + "-fx-font-weight: bold; "
                + "-fx-font-size: 12px; "
                + "-fx-padding: 7 15 7 15; "
                + // Slightly less padding due to thicker border
                "-fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(139,101,75,0.4), 4, 0, 0, 2);";

        // Set initial style based on button type
        if (isAllButton) {
            button.setStyle(activeStyle); // "All" button starts active
        } else {
            button.setStyle(baseStyle);
        }

        // Hover effects (only for inactive buttons)
        button.setOnMouseEntered(e -> {
            if (activeButton != button) {
                button.setStyle(hoverStyle);
            }
        });

        button.setOnMouseExited(e -> {
            if (activeButton != button) {
                button.setStyle(baseStyle);
            }
        });

        // Store the styles as user data for easy access
        button.setUserData(new ButtonStyles(baseStyle, hoverStyle, activeStyle));

        return button;
    }

    private void updateActiveButton(Button newActiveButton) {
        // Reset previous active button
        if (activeButton != null) {
            ButtonStyles styles = (ButtonStyles) activeButton.getUserData();
            activeButton.setStyle(styles.baseStyle);
        }

        // Set new active button
        activeButton = newActiveButton;
        ButtonStyles styles = (ButtonStyles) newActiveButton.getUserData();
        newActiveButton.setStyle(styles.activeStyle);
    }

// Helper class to store button styles
    private static class ButtonStyles {

        final String baseStyle;
        final String hoverStyle;
        final String activeStyle;

        ButtonStyles(String baseStyle, String hoverStyle, String activeStyle) {
            this.baseStyle = baseStyle;
            this.hoverStyle = hoverStyle;
            this.activeStyle = activeStyle;
        }
    }
//    private void loadCategoriesAndDisplayButtons() {
//        String query = "SELECT * FROM category";
//        try (Connection conn = DBConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
//
//            categories.clear();
//            while (rs.next()) {
//                categories.add(new Category(
//                        rs.getInt("id"),
//                        rs.getString("name"),
//                        rs.getString("description")
//                ));
//            }
//
//            categoryFilterBox.getChildren().clear();
//            // Add 'All' button
//            Button allButton = new Button("All");
//            allButton.setOnAction(e -> filterProductsByCategory(null));
//            categoryFilterBox.getChildren().add(allButton);
//
//            // Add button for each category
//            for (Category category : categories) {
//                Button categoryButton = new Button(category.getName());
//                categoryButton.setOnAction(e -> filterProductsByCategory(category));
//                categoryFilterBox.getChildren().add(categoryButton);
//            }
//
//        } catch (SQLException e) {
//            showErrorAlert("Failed to load categories: " + e.getMessage());
//        }
//    }

    private void filterProductsByCategory(Category category) {
        selectedCategory = category;
        filterProducts(searchField.getText()); // Re-apply search filter on the new category
    }

    private void filterProducts(String searchTerm) {
        ObservableList<Product> categoryFilteredList;

        if (selectedCategory == null) {
            categoryFilteredList = products; // No category filter
        } else {
            categoryFilteredList = products.stream()
                    .filter(product -> product.getCategoryId() == selectedCategory.getId())
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
        }

        if (searchTerm == null || searchTerm.isEmpty()) {
            displayProducts(categoryFilteredList);
            return;
        }

        ObservableList<Product> searchFilteredList = categoryFilteredList.stream()
                .filter(product -> product.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        displayProducts(searchFilteredList);
    }

    private void setupOrderTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        orderTable.setItems(orderItems);
    }

    private void loadProducts() {
        products.clear();
        String query = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN category c ON p.category_id = c.id WHERE p.stock > 0";
        try (Connection conn = DBConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                products.add(new Product(
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
            showErrorAlert("Failed to load products: " + e.getMessage());
        }
    }

    private void loadTaxRate() {
        String query = "SELECT rate FROM tax_settings ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                taxRate = rs.getDouble("rate");
            } else {
                taxRate = 0.12;
            }
        } catch (SQLException e) {
            showErrorAlert("Failed to load tax rate: " + e.getMessage());
            taxRate = 0.12;
        }
    }

    private void displayProducts(ObservableList<Product> productsToDisplay) {
        productGrid.getChildren().clear();
        for (Product product : productsToDisplay) {
            VBox productCard = createProductCard(product);
            productGrid.getChildren().add(productCard);
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);

        // Enhanced base styling with shadow and rounded corners - Brown theme
        String baseStyle = "-fx-border-color: #d4b896; "
                + "-fx-border-radius: 8; "
                + "-fx-background-radius: 8; "
                + "-fx-background-color: #faf8f5; "
                + "-fx-padding: 15; "
                + "-fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(139,101,75,0.2), 3, 0, 0, 1);";

        String hoverStyle = "-fx-border-color: #8b654b; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 8; "
                + "-fx-background-radius: 8; "
                + "-fx-background-color: #f5f0e8; "
                + "-fx-padding: 14; "
                + // Adjust padding for thicker border
                "-fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(139,101,75,0.4), 5, 0, 0, 2);";

        card.setStyle(baseStyle);
        card.setPrefWidth(130);
        card.setPrefHeight(160);

        // Image container with background
        VBox imageContainer = new VBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-background-color: #f0e8d8; -fx-background-radius: 6; -fx-padding: 8;");
        imageContainer.setPrefHeight(90);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(70);
        imageView.setFitHeight(70);
        imageView.setPreserveRatio(true);

        try {
            String imagePath = product.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                File file = new File("src/" + imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    imageView.setImage(image);
                } else {
                    setDefaultImage(imageView);
                }
            } else {
                setDefaultImage(imageView);
            }
        } catch (Exception e) {
            setDefaultImage(imageView);
        }

        imageContainer.getChildren().add(imageView);

        // Product name with better typography
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #5d4037;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(110);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextAlignment(TextAlignment.CENTER);

        // Price with enhanced styling
        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #8b654b; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Stock with conditional coloring
        Label stockLabel = new Label("Stock: " + product.getStock());
        String stockColor = product.getStock() > 10 ? "#8b654b"
                : product.getStock() > 0 ? "#bf8f36" : "#d32f2f";
        stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + stockColor + ";");

        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, stockLabel);

        // Enhanced hover effects
        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            // Add slight scale animation effect
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            // Reset scale
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Add pressed effect
        card.setOnMousePressed(e -> {
            card.setStyle(hoverStyle);
            card.setScaleX(0.98);
            card.setScaleY(0.98);
        });

        card.setOnMouseReleased(e -> {
            card.setStyle(hoverStyle);
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });

        card.setOnMouseClicked(event -> addProductToOrder(product));

        return card;
    }

//    private VBox createProductCard(Product product) {
//        VBox card = new VBox(10);
//        card.setAlignment(Pos.CENTER);
//        card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
//        card.setPrefWidth(120);
//        card.setPrefHeight(150);
//
//        ImageView imageView = new ImageView();
//        imageView.setFitWidth(80);
//        imageView.setFitHeight(80);
//        imageView.setPreserveRatio(true);
//
//        try {
//            String imagePath = product.getImage();
//            if (imagePath != null && !imagePath.isEmpty()) {
//                File file = new File("src/" + imagePath);
//                if (file.exists()) {
//                    Image image = new Image(file.toURI().toString());
//                    imageView.setImage(image);
//                } else {
//                    setDefaultImage(imageView);
//                }
//            } else {
//                setDefaultImage(imageView);
//            }
//        } catch (Exception e) {
//            setDefaultImage(imageView);
//        }
//
//        Label nameLabel = new Label(product.getName());
//        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
//        nameLabel.setWrapText(true);
//        nameLabel.setMaxWidth(100);
//
//        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
//        priceLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
//
//        Label stockLabel = new Label("Stock: " + product.getStock());
//        stockLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
//
//        card.getChildren().addAll(imageView, nameLabel, priceLabel, stockLabel);
//
//        card.setOnMouseEntered(e -> card.setStyle("-fx-border-color: #4CAF50; -fx-border-radius: 5; -fx-padding: 10; -fx-cursor: hand; -fx-background-color: #f0f8ff;"));
//        card.setOnMouseExited(e -> card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10; -fx-cursor: hand;"));
//
//        card.setOnMouseClicked(event -> addProductToOrder(product));
//        return card;
//    }
    private void setDefaultImage(ImageView imageView) {
        try {
            File defaultFile = new File("src/coffee_pos/image/product.png");
            if (defaultFile.exists()) {
                Image defaultImage = new Image(defaultFile.toURI().toString());
                imageView.setImage(defaultImage);
            }
        } catch (Exception ex) {
            // If default image also fails, leave imageView empty
        }
    }

    private void addProductToOrder(Product product) {
        if (product.getStock() <= 0) {
            showErrorAlert("Product '" + product.getName() + "' is out of stock.");
            return;
        }

        Optional<OrderItem> existingItem = orderItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst();

        if (existingItem.isPresent()) {
            OrderItem item = existingItem.get();
            if (item.getQuantity() >= product.getStock()) {
                showErrorAlert("Cannot add more. Stock limit reached for '" + product.getName() + "'.");
                return;
            }
            item.setQuantity(item.getQuantity() + 1);
        } else {
            OrderItem newItem = new OrderItem(product);
            orderItems.add(newItem);
        }

        orderTable.refresh();
        updateTotals();
    }

    @FXML
    private void handleRemoveItem() {
        OrderItem selectedItem = orderTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showErrorAlert("Please select an item to remove.");
            return;
        }

        if (selectedItem.getQuantity() > 1) {
            selectedItem.setQuantity(selectedItem.getQuantity() - 1);
            orderTable.refresh();
        } else {
            orderItems.remove(selectedItem);
        }
        updateTotals();
    }

    private void updateTotals() {
        double subtotal = orderItems.stream().mapToDouble(OrderItem::getTotal).sum();
        double tax = subtotal * taxRate;
        double total = subtotal + tax;

        subtotalLabel.setText(String.format("Subtotal: $%.2f", subtotal));
        taxLabel.setText(String.format("Tax (%.0f%%): $%.2f", taxRate * 100, tax));
        totalLabel.setText(String.format("Total: $%.2f", total));
    }

    @FXML
    private void handleInvoiceButton() {
        if (orderItems.isEmpty()) {
            showErrorAlert("The order is empty. Please add items before generating an invoice.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/coffee_pos/view/InvoiceView.fxml"));
            Parent root = loader.load();

            InvoiceController controller = loader.getController();
            controller.setInvoiceText(generateInvoiceText());

            Stage stage = new Stage();
            stage.setTitle("Invoice");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            System.out.println("" + e.getMessage());
            showErrorAlert("Could not open the invoice window: " + e.getMessage());
        }
    }

    @FXML
    private void handlePayButton() {
        if (orderItems.isEmpty()) {
            showErrorAlert("The order is empty. Please add items before payment.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Payment");
        confirmAlert.setHeaderText("Process Payment");
        confirmAlert.setContentText(String.format("Total amount: %s\n\nProceed with payment?", totalLabel.getText()));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.get() != ButtonType.OK) {
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnector.getConnection();
            conn.setAutoCommit(false);

            double subtotal = orderItems.stream().mapToDouble(OrderItem::getTotal).sum();
            double totalAmount = subtotal * (1 + taxRate);

            int waitingNumber = getNextWaitingNumber();
            lastGeneratedWaitingNumber = waitingNumber;

            String insertOrderSQL = "INSERT INTO orders (user_id, total_amount, order_date) VALUES (?, ?, NOW())";
            int orderId;

            try (PreparedStatement pstmtOrder = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setInt(1, UserSession.getInstance().getUserId());
                pstmtOrder.setDouble(2, totalAmount);
                pstmtOrder.executeUpdate();

                try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating order failed, no ID obtained.");
                    }
                }
            }

            String insertItemsSQL = "INSERT INTO order_items (order_id, product_id, quantity, price_per_item) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmtItems = conn.prepareStatement(insertItemsSQL)) {
                for (OrderItem item : orderItems) {
                    pstmtItems.setInt(1, orderId);
                    pstmtItems.setInt(2, item.getProduct().getId());
                    pstmtItems.setInt(3, item.getQuantity());
                    pstmtItems.setDouble(4, item.getPrice());
                    pstmtItems.addBatch();
                }
                pstmtItems.executeBatch();
            }

            String updateStockSQL = "UPDATE products SET stock = stock - ? WHERE id = ?";
            try (PreparedStatement pstmtStock = conn.prepareStatement(updateStockSQL)) {
                for (OrderItem item : orderItems) {
                    pstmtStock.setInt(1, item.getQuantity());
                    pstmtStock.setInt(2, item.getProduct().getId());
                    pstmtStock.addBatch();
                }
                pstmtStock.executeBatch();
            }

            conn.commit();

            showInfoAlert("Payment processed successfully!\nOrder ID: " + orderId + "\nWaiting Number: " + lastGeneratedWaitingNumber);

            handleInvoiceButton();

            clearOrder();
            loadProducts();
            displayProducts(products);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    showErrorAlert("Database error on rollback: " + ex.getMessage());
                }
            }
            showErrorAlert("Transaction failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    showErrorAlert("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleClearButton() {
        if (orderItems.isEmpty()) {
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Clear Order");
        confirmAlert.setHeaderText("Clear All Items");
        confirmAlert.setContentText("Are you sure you want to clear all items from the order?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.get() == ButtonType.OK) {
            clearOrder();
        }
    }

    private void clearOrder() {
        orderItems.clear();
        updateTotals();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
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

    private String generateInvoiceText() {
        StringBuilder invoiceText = new StringBuilder();
        invoiceText.append("****************************************\n");
        invoiceText.append("*           COFFEE POS INVOICE         *\n");
        invoiceText.append("****************************************\n");
        invoiceText.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        if (lastGeneratedWaitingNumber > 0) {
            invoiceText.append("Waiting Number: ").append(lastGeneratedWaitingNumber).append("\n");
        }
        invoiceText.append("----------------------------------------\n");
        // Fixed: Removed space between % and -20s
        invoiceText.append(String.format("%-20s %8s %10s\n", "Item", "Qty", "Price"));
        invoiceText.append("----------------------------------------\n");

        double subtotal = 0;
        for (OrderItem item : orderItems) {
            double itemTotal = item.getPrice() * item.getQuantity();
            subtotal += itemTotal;
            // Fixed: Removed space between % and -20s
            invoiceText.append(String.format("%-20s %8d $%9.2f\n",
                    item.getProductName(),
                    item.getQuantity(),
                    itemTotal));
        }

        double tax = subtotal * taxRate;
        double total = subtotal + tax;

        invoiceText.append("----------------------------------------\n");
        invoiceText.append(String.format("Subtotal: %22.2f\n", subtotal));
        invoiceText.append(String.format("Tax (%.0f%%): %22.2f\n", taxRate * 100, tax));
        invoiceText.append(String.format("Total: %25.2f\n", total));
        invoiceText.append("****************************************\n");
        invoiceText.append("       Thank you for visiting!    \n");
        invoiceText.append("****************************************\n");

        return invoiceText.toString();
    }

//    private String generateInvoiceText() {
//        StringBuilder invoiceText = new StringBuilder();
//        invoiceText.append("****************************************\n");
//        invoiceText.append("*           COFFEE POS INVOICE         *\n");
//        invoiceText.append("****************************************\n");
//        invoiceText.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
//        if (lastGeneratedWaitingNumber > 0) {
//            invoiceText.append("Waiting Number: ").append(lastGeneratedWaitingNumber).append("\n");
//        }
//        invoiceText.append("----------------------------------------\n");
//        invoiceText.append(String.format("% -20s %8s %10s\n", "Item", "Qty", "Price"));
//        invoiceText.append("----------------------------------------\n");
//
//        double total = 0;
//        for (OrderItem item : orderItems) {
//            double itemTotal = item.getPrice() * item.getQuantity();
//            total += itemTotal;
//            invoiceText.append(String.format("% -20s %8d %10.2f\n", 
//                item.getProductName(), 
//                item.getQuantity(), 
//                itemTotal));
//        }
//
//        invoiceText.append("----------------------------------------\n");
//        invoiceText.append(String.format("Total: %27.2f\n", total));
//        invoiceText.append("****************************************\n");
//        invoiceText.append("       Thank you for your business!    \n");
//        invoiceText.append("****************************************\n");
//
//        return invoiceText.toString();
//    }
}
