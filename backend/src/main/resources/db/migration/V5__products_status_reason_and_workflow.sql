-- Add product status workflow support:
-- - status_reason: store reason for status changes (e.g. pending deactivation)
-- - extend status CHECK to include PENDING_DEACTIVATION

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS status_reason varchar(500);

-- In V1__init_schema.sql the status CHECK constraint was created implicitly.
-- On Postgres it typically becomes: products_status_check
ALTER TABLE products
    DROP CONSTRAINT IF EXISTS products_status_check;

ALTER TABLE products
    ADD CONSTRAINT products_status_check
        CHECK (status in ('ACTIVE','PENDING_DEACTIVATION','INACTIVE','OUT_OF_STOCK'));

