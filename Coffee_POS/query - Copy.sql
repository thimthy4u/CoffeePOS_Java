-- This script updates the 'products' table to include an image path.
-- Execute this in your 'java_pos' MySQL database.

-- 1. Add the 'image' column to the products table
-- This column will store the file path to the product's image.
ALTER TABLE products
ADD COLUMN image VARCHAR(255) NULL;

-- 2. (Optional) Update existing rows to include a default image path.
-- Replace 'path/to/your/image.png' with the actual path to your images.
-- For example, you might use 'image/espresso.png'.
UPDATE products SET image = 'coffee_pos/image/product.png' WHERE name = 'Espresso';
UPDATE products SET image = 'coffee_pos/image/product.png' WHERE name = 'Latte';
UPDATE products SET image = 'coffee_pos/image/product.png' WHERE name = 'Cappuccino';

