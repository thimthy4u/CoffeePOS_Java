package coffee_pos.model;

public class OrderItem {
    private Product product;
    private int quantity;

    public OrderItem(Product product) {
        this.product = product;
        this.quantity = 1;
    }

    public OrderItem(String productName, int quantity, double price) {
        // This constructor is for the OrderController, which doesn't have access to the full Product object
        this.product = new Product(0, productName, price, 0, "", 0, "");
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductName() {
        return product.getName();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return product.getPrice();
    }

    public double getTotal() {
        return product.getPrice() * quantity;
    }
}
