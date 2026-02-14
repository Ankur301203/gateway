CREATE TABLE route_targets (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
                               target_url VARCHAR(500) NOT NULL,
                               is_active BOOLEAN NOT NULL DEFAULT true,
                               health_status VARCHAR(20) NOT NULL DEFAULT 'unknown',
                               last_health_check TIMESTAMP,
                               consecutive_failures INTEGER NOT NULL DEFAULT 0,
                               weight INTEGER NOT NULL DEFAULT 1,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT valid_health_status CHECK (health_status IN ('healthy', 'unhealthy', 'unknown')),
                               CONSTRAINT valid_weight CHECK (weight > 0)
);

CREATE INDEX idx_route_targets_route_id ON route_targets(route_id);
CREATE INDEX idx_route_targets_health ON route_targets(route_id, is_active, health_status);

CREATE TRIGGER update_route_targets_updated_at BEFORE UPDATE ON route_targets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();