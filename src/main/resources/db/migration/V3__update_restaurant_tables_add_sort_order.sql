ALTER TABLE restaurant_tables ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;
UPDATE restaurant_tables SET sort_order = id::INTEGER;