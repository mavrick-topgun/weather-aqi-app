-- Locations table for saved user locations
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    timezone VARCHAR(100) DEFAULT 'auto',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_locations_lat_lon UNIQUE (latitude, longitude)
);

-- Daily metrics table for historical tracking
CREATE TABLE daily_metrics (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    recommendation VARCHAR(50) NOT NULL,
    aqi_value INTEGER,
    pm25 DECIMAL(6, 2),
    ozone DECIMAL(6, 2),
    temperature_max DECIMAL(5, 2),
    temperature_min DECIMAL(5, 2),
    precipitation DECIMAL(6, 2),
    wind_speed DECIMAL(5, 2),
    uv_index DECIMAL(4, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_metrics_location_date UNIQUE (location_id, date)
);

-- Indexes for common queries
CREATE INDEX idx_daily_metrics_location_id ON daily_metrics(location_id);
CREATE INDEX idx_daily_metrics_date ON daily_metrics(date);
CREATE INDEX idx_daily_metrics_location_date ON daily_metrics(location_id, date DESC);
