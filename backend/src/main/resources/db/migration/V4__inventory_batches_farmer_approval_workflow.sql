-- Update inventory batch workflow:
-- - Allow farmer-created batches to be PENDING_APPROVAL / REJECTED.
-- - Add rejection_reason so we can show reason to farmer.

ALTER TABLE inventory_batches
    ADD COLUMN IF NOT EXISTS rejection_reason varchar(500);

-- In V1__init_schema.sql the status CHECK constraint was created implicitly.
-- On Postgres it typically becomes: inventory_batches_status_check
ALTER TABLE inventory_batches
    DROP CONSTRAINT IF EXISTS inventory_batches_status_check;

ALTER TABLE inventory_batches
    ADD CONSTRAINT inventory_batches_status_check
        CHECK (status in ('PENDING_APPROVAL','AVAILABLE','REJECTED','SOLD_OUT','EXPIRED'));

