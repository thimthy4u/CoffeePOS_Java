
-- This script adds CRM features to the 'java_pos' database.
-- Execute this in your 'java_pos' MySQL database.

-- 1. Create the 'customers' table
-- This table will store customer information.
CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Add 'customer_id' to the 'orders' table
-- This creates a relationship between an order and a customer.
ALTER TABLE orders
ADD COLUMN customer_id INT NULL,
ADD CONSTRAINT fk_customer_id
FOREIGN KEY (customer_id) REFERENCES customers(id);

