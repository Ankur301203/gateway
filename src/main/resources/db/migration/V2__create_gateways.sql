CREATE TABLE gateways (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          name VARCHAR(100) NOT NULL,
                          description TEXT,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gateways_user_id ON gateways(user_id);
CREATE INDEX idx_gateways_created_at ON gateways(created_at DESC);

CREATE TRIGGER update_gateways_updated_at BEFORE UPDATE ON gateways
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();