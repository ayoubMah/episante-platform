CREATE TABLE app_users (
                           id UUID PRIMARY KEY,
                           email VARCHAR(120) NOT NULL UNIQUE,
                           password VARCHAR(255) NOT NULL,
                           role VARCHAR(20) NOT NULL,
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
