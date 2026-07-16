CREATE TABLE reservations (
                              id BIGSERIAL PRIMARY KEY,
                              table_id BIGINT NOT NULL REFERENCES restaurant_tables(id),

    -- Info klienti
                              guest_name VARCHAR(100) NOT NULL,
                              guest_phone VARCHAR(30),
                              guest_count INTEGER NOT NULL CHECK (guest_count >= 1),

    -- Kohore
                              reservation_time TIMESTAMP NOT NULL,

    -- Statusi
                              status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REQUEST',

    -- Kush e krijoi kerkesen (kamarieri)
                              requested_by VARCHAR(100),

    -- Timestamps per status transitions
                              confirmed_at TIMESTAMP,
                              arrived_at TIMESTAMP,
                              no_show_at TIMESTAMP,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_time ON reservations(reservation_time);
CREATE INDEX idx_reservations_table ON reservations(table_id);