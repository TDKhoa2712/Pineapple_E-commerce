-- Migration to add unit column to products table
ALTER TABLE products ADD COLUMN unit VARCHAR(50);

-- Update existing products to have a default unit of 'kg'
UPDATE products SET unit = 'kg' WHERE unit IS NULL;
