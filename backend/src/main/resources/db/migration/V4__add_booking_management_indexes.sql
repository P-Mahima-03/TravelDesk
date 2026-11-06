CREATE INDEX idx_bookings_status_created_at
    ON bookings (booking_status, created_at);

CREATE INDEX idx_bookings_created_at
    ON bookings (created_at);
