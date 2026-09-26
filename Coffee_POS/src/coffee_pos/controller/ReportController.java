package coffee_pos.controller;

import coffee_pos.DBConnector;
import coffee_pos.UserSession;
import coffee_pos.model.SalesReportItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date; // Use java.util.Date for general date objects

public class ReportController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label filter;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private VBox salesReportBox;
    @FXML
    private TableView<SalesReportItem> reportTableView; // Corrected name
    @FXML
    private TableColumn<SalesReportItem, String> productNameColumn; // Corrected name
    @FXML
    private TableColumn<SalesReportItem, Integer> quantityColumn; // Corrected name
    @FXML
    private TableColumn<SalesReportItem, Double> totalPriceColumn; // Corrected name
    @FXML
    private TableColumn<SalesReportItem, java.util.Date> saleDateColumn; // Explicitly use java.util.Date
    @FXML
    private TableColumn<SalesReportItem, String> cashierNameColumn; // Added @FXML
    @FXML
    private Label totalSalesLabel;
    @FXML
    private ComboBox<String> cashierFilterComboBox;

    private final ObservableList<SalesReportItem> salesReportData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        System.out.println("ReportController: initialize called.");
        System.out.println("User role: " + UserSession.getInstance().getRole());
        setupTableColumns();
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusMonths(1));

        titleLabel.setText("Sales Report");

        // Populate cashier filter combobox if admin
        if (UserSession.getInstance().getRole().equalsIgnoreCase("admin")) {
            populateCashierFilter();
            cashierFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                handleGenerateReport();
            });
        } else {
            cashierFilterComboBox.setVisible(false);
            cashierFilterComboBox.setManaged(false);
            filter.setVisible(false);
        }

        handleGenerateReport();
    }

    private void populateCashierFilter() {
        ObservableList<String> cashiers = FXCollections.observableArrayList();
        cashiers.add("All Cashiers"); // Option to view all cashiers

        String query = "SELECT u.username FROM users u JOIN roles r ON u.role_id = r.id WHERE r.role_name = 'Cashier' OR r.role_name = 'Admin'";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cashiers.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            showErrorAlert("Database error fetching cashiers: " + e.getMessage());
            e.printStackTrace();
        }
        cashierFilterComboBox.setItems(cashiers);
        cashierFilterComboBox.getSelectionModel().selectFirst(); // Select 'All Cashiers' by default
    }





    private void setupTableColumns() {
        // Explicitly get columns from the TableView
        TableColumn<SalesReportItem, String> productNameColumn = (TableColumn<SalesReportItem, String>) reportTableView.getColumns().get(0);
        TableColumn<SalesReportItem, Integer> quantityColumn = (TableColumn<SalesReportItem, Integer>) reportTableView.getColumns().get(1);
        TableColumn<SalesReportItem, Double> totalPriceColumn = (TableColumn<SalesReportItem, Double>) reportTableView.getColumns().get(2);
        TableColumn<SalesReportItem, java.util.Date> saleDateColumn = (TableColumn<SalesReportItem, java.util.Date>) reportTableView.getColumns().get(3);
        TableColumn<SalesReportItem, String> cashierNameColumn = (TableColumn<SalesReportItem, String>) reportTableView.getColumns().get(4);

        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        totalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        saleDateColumn.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        cashierNameColumn.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        reportTableView.setItems(salesReportData);
    }

    @FXML
    private void handleGenerateReport() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showErrorAlert("Please select both a start and end date.");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showErrorAlert("Start date cannot be after end date.");
            return;
        }

        loadSalesReportData(startDate, endDate);
    }



    private void loadSalesReportData(LocalDate startDate, LocalDate endDate) {
        salesReportData.clear(); // Clear previous data
        double totalSales = 0.0; // Initialize total sales

        try (Connection conn = DBConnector.getConnection()) {
            String selectedCashier = cashierFilterComboBox.getSelectionModel().getSelectedItem();
            boolean isAdmin = UserSession.getInstance().getRole().equalsIgnoreCase("admin");

            StringBuilder queryBuilder = new StringBuilder("SELECT p.name AS product_name, oi.quantity, oi.price_per_item * oi.quantity AS total_price, o.order_date, u.username AS cashier_name ");
            queryBuilder.append("FROM order_items oi ");
            queryBuilder.append("JOIN products p ON oi.product_id = p.id ");
            queryBuilder.append("JOIN orders o ON oi.order_id = o.id ");
            queryBuilder.append("JOIN users u ON o.user_id = u.id ");

            boolean filterByCashier = isAdmin && selectedCashier != null && !selectedCashier.equals("All Cashiers");

            if (filterByCashier) {
                queryBuilder.append("WHERE u.username = ? AND o.order_date BETWEEN ? AND ?");
            } else if (isAdmin) {
                queryBuilder.append("WHERE o.order_date BETWEEN ? AND ?");
            } else {
                queryBuilder.append("WHERE o.user_id = ? AND o.order_date BETWEEN ? AND ?");
            }

            PreparedStatement ps = conn.prepareStatement(queryBuilder.toString());
            int paramIndex = 1;

            if (filterByCashier) {
                ps.setString(paramIndex++, selectedCashier);
            } else if (!isAdmin) {
                ps.setInt(paramIndex++, UserSession.getInstance().getUserId());
            }

            ps.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            ps.setDate(paramIndex++, java.sql.Date.valueOf(endDate));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String productName = rs.getString("product_name");
                int quantity = rs.getInt("quantity");
                double totalPrice = rs.getDouble("total_price");
                java.util.Date saleDate = rs.getDate("order_date"); // Use java.util.Date
                String cashierName = rs.getString("cashier_name");
                salesReportData.add(new SalesReportItem(productName, quantity, totalPrice, saleDate, cashierName));
                totalSales += totalPrice;
            }
        } catch (SQLException e) {
            showErrorAlert("Database error: " + e.getMessage());
            e.printStackTrace();
        }

        totalSalesLabel.setText(String.format("Total Sales for Period: $%.2f", totalSales));
        if(totalSales > 0){
            totalSalesLabel.setVisible(true);
        }else{
            totalSalesLabel.setVisible(false);
        }
    }

    @FXML
    private void handleExportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report as CSV");
        fileChooser.setInitialFileName("sales_report.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(titleLabel.getScene().getWindow());

        if (file != null) {
            try {
                exportSalesReport(file);
                showInfoAlert("Report exported successfully to " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Failed to export data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }



    private void exportSalesReport(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("Product Name,Quantity Sold,Total Price,Cashier Name");
            for (SalesReportItem item : salesReportData) {
                writer.printf("%s,%d,%.2f,%s\n",
                        item.getProductName(),
                        item.getQuantity(),
                        item.getTotalPrice(),
                        item.getCashierName());
            }
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Report Error");
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