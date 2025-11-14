CREATE TYPE gender AS ENUM ('MALE', 'FEMALE');

CREATE TABLE doctors (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         first_name VARCHAR(80) NOT NULL,
                         last_name VARCHAR(80) NOT NULL,
                         email VARCHAR(120) UNIQUE NOT NULL,
                         phone VARCHAR(30),
                         specialty VARCHAR(80),
                         rpps VARCHAR(30),
                         clinic_address TEXT,
                         created_at TIMESTAMPTZ DEFAULT now(),
                         updated_at TIMESTAMPTZ DEFAULT now()
);
