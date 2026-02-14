CREATE TABLE routes (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        gateway_id UUID NOT NULL REFERENCES gateways(id) ON DELETE CASCADE,
                        path VARCHAR(255) NOT NULL,
                        method VARCHAR(10) NOT NULL,
                        timeout_ms INTEGER NOT NULL DEFAULT 30000,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT unique_gateway_path_method UNIQUE(gateway_id, path, method),
                        CONSTRAINT valid_method CHECK (method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'))
);

CREATE INDEX idx_routes_gateway_id ON routes(gateway_id);
CREATE INDEX idx_routes_lookup ON routes(gateway_id, path, method);

CREATE TRIGGER update_routes_updated_at BEFORE UPDATE ON routes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();