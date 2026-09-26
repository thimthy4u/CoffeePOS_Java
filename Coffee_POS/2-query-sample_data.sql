SELECT * FROM products
SELECT * FROM users;
SELECT * FROM orders;
SELECT sum(stock) AS STotal FROM products


-- =================================================================
-- Sample Data
-- =================================================================

-- Insert default roles
INSERT INTO roles (role_name) VALUES ('Admin'), ('Cashier');

-- username : admin
-- password : admin
INSERT INTO users (username, password, full_name, role_id) VALUES
('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'Admin', 1);


-- Insert system permissions
INSERT INTO permissions (permission_name, description) VALUES
('manage_users', 'Can create, edit, and delete user accounts.'),
('manage_products', 'Can add, edit, and delete products.'),
('process_orders', 'Can create and process customer orders.'),
('view_sales_reports', 'Can view sales and shift reports.'),
('manage_roles', 'Can define roles and assign permissions.');

-- Assign permissions to roles
-- Admin gets all permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), -- Admin can manage_users
(1, 2), -- Admin can manage_products
(1, 3), -- Admin can process_orders
(1, 4), -- Admin can view_sales_reports
(1, 5); -- Admin can manage_roles

-- Cashier can process orders and view sales reports
INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 3), -- Cashier can process_orders
(2, 4); -- Cashier can view_sales_reports

SELECT * FROM users;
SELECT * FROM permissions
SELECT * FROM roles
SELECT * FROM role_permissions

 SELECT
      u.username,
      u.full_name,
      r.role_name,
      p.permission_name,
      p.description
  FROM
      users u
  JOIN
      roles r ON u.role_id = r.id
  JOIN
      role_permissions rp ON r.id = rp.role_id
  JOIN
      permissions p ON rp.permission_id = p.id;
      
  INSERT INTO role_permissions (role_id, permission_id)
  SELECT r.id, p.id
  FROM roles r, permissions p
  WHERE r.role_name = 'Cashier' AND p.permission_name = 'view_sales_reports';


INSERT INTO tax_settings (rate) VALUES (0.12);
SELECT * from tax_settings

SELECT u.id, u.username, u.full_name, u.role_id, r.role_name FROM users u JOIN roles r ON u.role_id = r.id ORDER BY u.id

  
SELECT * FROM products
SELECT * FROM products
SELECT * FROM users
SELECT * FROM staff_shifts

SELECT p.name, SUM(oi.quantity) as quantity_sold, SUM(oi.quantity * oi.price_per_item) as total_price
FROM order_items oi
JOIN products p ON oi.product_id = p.id
JOIN orders o ON oi.order_id = o.id
JOIN users u ON o.user_id = u.id
WHERE DATE(o.order_date) BETWEEN '2025-09-20 09:05:00' AND '2025-09-29 09:05:00'
AND u.username = "cashier7"
AND o.user_id = 2
GROUP BY p.name ORDER BY total_price DESC

SELECT COUNT(*) AS productTotal FROM products