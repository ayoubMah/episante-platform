CREATE TABLE appointments (
                              id UUID PRIMARY KEY,
                              doctor_id UUID NOT NULL,
                              patient_id UUID NOT NULL,
                              start_time TIMESTAMP WITH TIME ZONE NOT NULL,
                              end_time TIMESTAMP WITH TIME ZONE NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Optional: Indexing for performance
-- We will often search by doctor or patient, so we need indexes.
CREATE INDEX idx_appointment_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointment_patient ON appointments(patient_id);