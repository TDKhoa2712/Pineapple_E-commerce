-- Create composite index on inventory_batches for expiry date and status (useful for expiration checking)
CREATE INDEX idx_inventory_batches_expiry_status ON inventory_batches (expiry_date, status);

-- Create single column index on orders(user_id) for foreign key joins and deletion efficiency
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- Create composite index on products for category and status (crucial for product category filtering in e-commerce catalog)
CREATE INDEX idx_products_category_status ON products (category_id, status);
