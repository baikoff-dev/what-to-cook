ALTER TABLE recipes
    ADD COLUMN status_before_archive VARCHAR(30);

UPDATE recipes
SET status_before_archive = 'UNTESTED'
WHERE status = 'ARCHIVED';

ALTER TABLE recipes
    ADD CONSTRAINT recipes_status_before_archive_check CHECK (
        (status = 'ARCHIVED'
            AND status_before_archive IN ('UNTESTED', 'TESTED', 'NEEDS_CHANGES'))
        OR
        (status <> 'ARCHIVED' AND status_before_archive IS NULL)
    );
