ALTER TABLE destinations
    ADD COLUMN image_url VARCHAR(500) NULL AFTER description,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at;

CREATE INDEX idx_destinations_name_country
    ON destinations (name, country);

CREATE INDEX idx_destinations_city_country
    ON destinations (city, country);
