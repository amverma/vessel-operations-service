-- Vessel Operations Service Database Schema
-- Mission-Critical Service (P1) - RTO/RPO: 2 minutes

-- Create vessels table
CREATE TABLE vessels (
    id BIGSERIAL PRIMARY KEY,
    vessel_id VARCHAR(255) NOT NULL UNIQUE,
    vessel_name VARCHAR(255) NOT NULL,
    imo_number VARCHAR(50) UNIQUE,
    voyage_number VARCHAR(100) NOT NULL,
    port_of_loading VARCHAR(100) NOT NULL,
    port_of_discharge VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    eta TIMESTAMP,
    etd TIMESTAMP,
    actual_departure_time TIMESTAMP,
    load_list_prepared BOOLEAN DEFAULT FALSE,
    load_list_shared_at TIMESTAMP,
    stowage_plan_complete BOOLEAN DEFAULT FALSE,
    stowage_plan_completed_at TIMESTAMP,
    reconciled BOOLEAN DEFAULT FALSE,
    reconciled_at TIMESTAMP,
    total_containers_planned INTEGER,
    total_containers_loaded INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create loaded_containers table
CREATE TABLE loaded_containers (
    id BIGSERIAL PRIMARY KEY,
    container_id VARCHAR(255) NOT NULL UNIQUE,
    vessel_id VARCHAR(255) NOT NULL,
    booking_reference VARCHAR(100) NOT NULL,
    stowage_position VARCHAR(50),
    weight_kg DECIMAL(10, 2),
    container_type VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    loaded_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    remarks TEXT,
    CONSTRAINT fk_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(vessel_id)
);

-- Create indexes for performance
CREATE INDEX idx_vessels_status ON vessels(status);
CREATE INDEX idx_vessels_voyage_number ON vessels(voyage_number);
CREATE INDEX idx_vessels_port_of_loading ON vessels(port_of_loading);
CREATE INDEX idx_vessels_ready_to_depart ON vessels(load_list_prepared, stowage_plan_complete, reconciled, status);
CREATE INDEX idx_loaded_containers_vessel_id ON loaded_containers(vessel_id);
CREATE INDEX idx_loaded_containers_container_id ON loaded_containers(container_id);
CREATE INDEX idx_loaded_containers_status ON loaded_containers(status);

-- Create audit trigger for vessels table
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_vessels_updated_at BEFORE UPDATE ON vessels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE vessels IS 'Mission-critical table storing vessel information. Vessel unable to depart is P1 incident.';
COMMENT ON TABLE loaded_containers IS 'Stores containers loaded on vessels. Critical for vessel departure tracking.';
COMMENT ON COLUMN vessels.status IS 'Vessel status: SCHEDULED, ARRIVED, LOAD_LIST_PREPARED, STOWAGE_PLAN_COMPLETE, LOADING_IN_PROGRESS, LOADING_COMPLETE, RECONCILED, READY_TO_DEPART, DEPARTED, BLOCKED';
COMMENT ON COLUMN loaded_containers.status IS 'Container load status: LOADED, VERIFIED, DAMAGED, MISSING';

-- Made with Bob
