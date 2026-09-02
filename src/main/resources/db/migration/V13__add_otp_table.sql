CREATE TABLE IF NOT EXISTS otps (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                   identifier VARCHAR(50) NOT NULL,
                                   hashed_code VARCHAR(255) NOT NULL,
                                   purpose VARCHAR(50) NOT NULL,

                                   attempts INT NOT NULL DEFAULT 0,
                                   used BOOLEAN NOT NULL DEFAULT FALSE,

                                   expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);