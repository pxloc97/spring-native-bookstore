ALTER TABLE bookOld
    ADD COLUMN IF NOT EXISTS publisher VARCHAR(255);

UPDATE bookOld SET publisher = 'Polarsophia' WHERE publisher IS NULL;

ALTER TABLE bookOld
    ALTER COLUMN publisher SET NOT NULL;
