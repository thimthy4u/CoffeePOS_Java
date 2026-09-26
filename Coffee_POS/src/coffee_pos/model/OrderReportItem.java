package coffee_pos.model;

import java.time.LocalDateTime;

public class OrderReportItem {
    private final int orderId;
    private final LocalDateTime orderDate;
    private final double totalAmount;
    private final String username;

    public OrderReportItem(int orderId, LocalDateTime orderDate, double totalAmount, String username) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.username = username;
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

    public String getUsername() {
        return username;
    }
}
