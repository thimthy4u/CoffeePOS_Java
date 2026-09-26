-- Create the category table
CREATE TABLE category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

-- Add the category_id column to the products table
ALTER TABLE products
ADD COLUMN category_id INT;

-- Add a foreign key constraint to the products table
ALTER TABLE products
ADD CONSTRAINT fk_category
FOREIGN KEY (category_id)
REFERENCES category(id);
