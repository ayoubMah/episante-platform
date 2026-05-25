CREATE TABLE IF NOT EXISTS kpi_appointments (
    id                      SERIAL PRIMARY KEY,
    calendar_year           INTEGER NOT NULL,
    calendar_month          INTEGER NOT NULL,
    calendar_year_month     VARCHAR(7) NOT NULL,
    month_start_date        DATE,
    patient_age_group       VARCHAR(50),
    patient_gender          VARCHAR(10),
    complexity_level        INTEGER,
    severity_label          VARCHAR(50),
    reason_label            VARCHAR(255),
    appointment_region      VARCHAR(100),
    total_appointments      BIGINT,
    total_duration_minutes  BIGINT,
    avg_duration_minutes    DOUBLE PRECISION,
    last_batch_id           VARCHAR(50),
    load_timestamp          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kpi_year ON kpi_appointments(calendar_year);
CREATE INDEX IF NOT EXISTS idx_kpi_region ON kpi_appointments(appointment_region);
CREATE INDEX IF NOT EXISTS idx_kpi_severity ON kpi_appointments(severity_label);
