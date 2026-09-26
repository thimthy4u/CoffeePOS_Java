-- SQL statements from Java files and existing SQL files

-- LoginController.java
SELECT u.id, u.password, r.role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.username = ?

-- OrderController.java
SELECT id, order_date, total_amount FROM orders ORDER BY order_date DESC
SELECT id, order_date, total_amount FROM orders WHERE user_id = ? ORDER BY order_date DESC
SELECT p.name, oi.quantity, oi.price_per_item FROM order_items oi JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?
SELECT o.id, o.order_date, o.total_amount, p.name, oi.quantity, oi.price_per_item FROM orders o JOIN order_items oi ON o.id = oi.order_id JOIN products p ON oi.product_id = p.id ORDER BY o.id, p.name
SELECT o.id, o.order_date, o.total_amount, p.name, oi.quantity, oi.price_per_item FROM orders o JOIN order_items oi ON o.id = oi.order_id JOIN products p ON oi.product_id = p.id WHERE o.user_id = ? ORDER BY o.id, p.name
INSERT INTO orders (user_id, total_amount, order_date) VALUES (?, ?, NOW())
INSERT INTO order_items (order_id, product_id, quantity, price_per_item) VALUES (?, ?, ?, ?)
UPDATE products SET stock = stock - ? WHERE id = ?

-- POSController.java (unique queries only)
SELECT * FROM products WHERE stock > 0
SELECT rate FROM tax_settings ORDER BY id DESC LIMIT 1

-- ProductController.java
SELECT * FROM products
INSERT INTO products (name, price, stock, image) VALUES (?, ?, ?, ?)
UPDATE products SET name = ?, price = ?, stock = ?, image = ? WHERE id = ?
DELETE FROM products WHERE id = ?

-- RegisterController.java
SELECT id FROM users WHERE username = ?
INSERT INTO users (username, password, role_id) VALUES (?, ?, ?)

-- ReportController.java
SELECT p.name, SUM(oi.quantity) as quantity_sold, SUM(oi.quantity * oi.price_per_item) as total_price FROM order_items oi JOIN products p ON oi.product_id = p.id JOIN orders o ON oi.order_id = o.id WHERE DATE(o.order_date) BETWEEN ? AND ?
SELECT p.name, SUM(oi.quantity) as quantity_sold, SUM(oi.quantity * oi.price_per_item) as total_price FROM order_items oi JOIN products p ON oi.product_id = p.id JOIN orders o ON oi.order_id = o.id WHERE DATE(o.order_date) BETWEEN ? AND ? AND o.user_id = ? GROUP BY p.name ORDER BY total_price DESC

-- SettingsController.java
UPDATE tax_settings SET rate = ? WHERE id = (SELECT id FROM tax_settings ORDER BY id DESC LIMIT 1)

-- UserManagementController.java
SELECT id, role_name FROM roles
SELECT u.id, u.username, u.full_name, u.role_id, r.role_name FROM users u JOIN roles r ON u.role_id = r.id ORDER BY u.id
SELECT id, shift_type, shift_date FROM staff_shifts WHERE user_id = ? ORDER BY shift_date DESC
INSERT INTO users (username, password, full_name, role_id) VALUES (?, ?, ?, ?)
UPDATE users SET username = ?, full_name = ?, role_id = ? WHERE id = ?
UPDATE users SET username = ?, full_name = ?, role_id = ?, password = ? WHERE id = ?
DELETE FROM users WHERE id = ?
INSERT INTO staff_shifts (user_id, shift_type, shift_date) VALUES (?, ?, ?)
DELETE FROM staff_shifts WHERE id = ?

-- From shift_migration.sql
CREATE TABLE staff_shifts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    shift_type VARCHAR(50) NOT NULL,
    shift_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- From tax_migration.sql
CREATE TABLE tax_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rate DECIMAL(5, 4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tax_settings (rate) VALUES (0.12);
