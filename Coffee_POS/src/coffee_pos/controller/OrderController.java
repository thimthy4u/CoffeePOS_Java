package coffee_pos.controller;

import coffee_pos.DBConnector;
import coffee_pos.model.Order;
import coffee_pos.model.OrderItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import javafx.scene.control.Alert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class OrderController {

    @FXML
    private TableView<Order> ordersTable;
    @FXML
    private TableColumn<Order, Integer> orderIdColumn;
    @FXML
    private TableColumn<Order, LocalDateTime> orderDateColumn;
    @FXML
    private TableColumn<Order, Double> orderTotalColumn;
    @FXML
    private TableView<OrderItem> orderItemsTable;
    @FXML
    private TableColumn<OrderItem, String> orderItemNameColumn;
    @FXML
    private TableColumn<OrderItem, Integer> orderItemQuantityColumn;
    @FXML
    private TableColumn<OrderItem, Double> orderItemPriceColumn;

    private ObservableList<Order> orderList = FXCollections.observableArrayList();
    private ObservableList<OrderItem> orderItemList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupOrderTable();
        setupOrderItemsTable();
        loadOrderHistory();

        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadOrderItems(newSelection.getOrderId());
            }
        });
    }

    private void setupOrderTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        orderTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        ordersTable.setItems(orderList);
    }

    private void setupOrderItemsTable() {
        orderItemNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        orderItemQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        orderItemPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        orderItemsTable.setItems(orderItemList);
    }

    private void loadOrderHistory() {
        orderList.clear();
        String currentUserRole = coffee_pos.UserSession.getInstance().getRole();
        int currentUserId = coffee_pos.UserSession.getInstance().getUserId();
        boolean isAdmin = "Admin".equals(currentUserRole);

        String query;
        if (isAdmin) {
            query = "SELECT id, order_date, total_amount FROM orders ORDER BY order_date DESC";
        } else {
            query = "SELECT id, order_date, total_amount FROM orders WHERE user_id = ? ORDER BY order_date DESC";
        }

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            if (!isAdmin) {
                pstmt.setInt(1, currentUserId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orderList.add(new Order(
                            rs.getInt("id"),
                            rs.getTimestamp("order_date").toLocalDateTime(),
                            rs.getDouble("total_amount")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOrderItems(int orderId) {
        orderItemList.clear();
        String query = "SELECT p.name, oi.quantity, oi.price_per_item FROM order_items oi " +
                       "JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orderItemList.add(new OrderItem(
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getDouble("price_per_item")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Detailed Order History as CSV");
        fileChooser.setInitialFileName("detailed_order_history.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(ordersTable.getScene().getWindow());

        if (file != null) {
            String currentUserRole = coffee_pos.UserSession.getInstance().getRole();
            int currentUserId = coffee_pos.UserSession.getInstance().getUserId();
            boolean isAdmin = "Admin".equals(currentUserRole);

            String query;
            if (isAdmin) {
                query = "SELECT o.id, o.order_date, o.total_amount, p.name, oi.quantity, oi.price_per_item " +
                               "FROM orders o " +
                               "JOIN order_items oi ON o.id = oi.order_id " +
                               "JOIN products p ON oi.product_id = p.id " +
                               "ORDER BY o.id, p.name";
            } else {
                query = "SELECT o.id, o.order_date, o.total_amount, p.name, oi.quantity, oi.price_per_item " +
                               "FROM orders o " +
                               "JOIN order_items oi ON o.id = oi.order_id " +
                               "JOIN products p ON oi.product_id = p.id " +
                               "WHERE o.user_id = ? " +
                               "ORDER BY o.id, p.name";
            }

            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                if (!isAdmin) {
                    pstmt.setInt(1, currentUserId);
                }

                try (ResultSet rs = pstmt.executeQuery();
                     PrintWriter writer = new PrintWriter(file)) {

                    // Write header
                    writer.println("OrderID,OrderDate,TotalAmount,ProductName,Quantity,PricePerItem");

                    if (!rs.isBeforeFirst()) { // Check if ResultSet is empty
                        showErrorAlert("There is no data to export.");
                        return;
                    }

                    // Write data rows
                    while (rs.next()) {
                        writer.printf("%d,%s,%.2f,%s,%d,%.2f\n",
                                rs.getInt("id"),
                                rs.getTimestamp("order_date").toLocalDateTime().toString(),
                                rs.getDouble("total_amount"),
                                rs.getString("name"),
                                rs.getInt("quantity"),
                                rs.getDouble("price_per_item"));
                    }

                    showInfoAlert("Detailed order history exported successfully to " + file.getAbsolutePath());
                }

            } catch (SQLException | IOException e) {
                showErrorAlert("Failed to export data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Export Error");
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
