-- V10__create_restaurant_tables.sql

CREATE TABLE restaurant_tables (
                                   id BIGSERIAL PRIMARY KEY,
                                   name VARCHAR(50) NOT NULL UNIQUE,
                                   seat_count INTEGER NOT NULL CHECK (seat_count >= 2 AND seat_count <= 20),
                                   section VARCHAR(20) NOT NULL,
                                   status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                                   position_x INTEGER NOT NULL DEFAULT 0,
                                   position_y INTEGER NOT NULL DEFAULT 0,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_restaurant_tables_section ON restaurant_tables(section);
CREATE INDEX idx_restaurant_tables_status ON restaurant_tables(status);