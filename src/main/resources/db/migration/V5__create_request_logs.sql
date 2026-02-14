CREATE TABLE request_logs (
                              id BIGSERIAL PRIMARY KEY,
                              gateway_id UUID NOT NULL,
                              route_id UUID,
                              target_id UUID,
                              method VARCHAR(10) NOT NULL,
                              path VARCHAR(500) NOT NULL,
                              status_code INTEGER NOT NULL,
                              latency_ms INTEGER NOT NULL,
                              error_message TEXT,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_request_logs_gateway_id ON request_logs(gateway_id, created_at DESC);
CREATE INDEX idx_request_logs_route_id ON request_logs(route_id, created_at DESC);
CREATE INDEX idx_request_logs_created_at ON request_logs(created_at DESC);
CREATE INDEX idx_request_logs_status ON request_logs(status_code, created_at DESC);

-- Partition by month for better performance (optional, for production scale)
-- This is commented out for simplicity, but recommended for production
-- CREATE TABLE request_logs_2024_01 PARTITION OF request_logs
-- FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');