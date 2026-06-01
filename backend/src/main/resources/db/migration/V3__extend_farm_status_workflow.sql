ALTER TABLE farms DROP CONSTRAINT IF EXISTS farms_status_check;

ALTER TABLE farms
    ADD CONSTRAINT farms_status_check
    CHECK (status IN (
        'PENDING_APPROVAL',
        'PENDING_DEACTIVATION',
        'PENDING_REACTIVATION',
        'ACTIVE',
        'INACTIVE',
        'REJECTED'
    ));
