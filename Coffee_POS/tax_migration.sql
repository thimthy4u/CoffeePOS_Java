CREATE TABLE tax_settings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    rate DECIMAL(5, 2) NOT NULL
);

INSERT INTO tax_settings (rate) VALUES (0.12);
