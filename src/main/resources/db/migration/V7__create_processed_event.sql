CREATE TABLE IF NOT EXISTS processed_event (
    evento_id VARCHAR(120) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);
