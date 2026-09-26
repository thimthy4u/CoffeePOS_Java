package coffee_pos.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import java.util.Date;

public class SalesReportItem {
    private final SimpleStringProperty productName;
    private final SimpleIntegerProperty quantity;
    private final SimpleDoubleProperty totalPrice;
    private final SimpleObjectProperty<Date> saleDate;
    private final SimpleStringProperty cashierName;

    public SalesReportItem(String productName, int quantity, double totalPrice, Date saleDate, String cashierName) {
        this.productName = new SimpleStringProperty(productName);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.totalPrice = new SimpleDoubleProperty(totalPrice);
        this.saleDate = new SimpleObjectProperty<>(saleDate);
        this.cashierName = new SimpleStringProperty(cashierName);
    }

    public String getProductName() {
        return productName.get();
    }

    public SimpleStringProperty productNameProperty() {
        return productName;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public SimpleIntegerProperty quantityProperty() {
        return quantity;
    }

    public double getTotalPrice() {
        return totalPrice.get();
    }

    public SimpleDoubleProperty totalPriceProperty() {
        return totalPrice;
    }

    public Date getSaleDate() {
        return saleDate.get();
    }

    public SimpleObjectProperty<Date> saleDateProperty() {
        return saleDate;
    }

    public String getCashierName() {
        return cashierName.get();
    }

    public SimpleStringProperty cashierNameProperty() {
        return cashierName;
    }
}
