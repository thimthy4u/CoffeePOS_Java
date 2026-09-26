package coffee_pos.model;

import java.time.LocalDateTime;

public class Order {
    private int orderId;
    private LocalDateTime orderDate;
    private double totalAmount;

    public Order(int orderId, LocalDateTime orderDate, double totalAmount) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
    }

    public int getOrderId() {
        return orderId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
