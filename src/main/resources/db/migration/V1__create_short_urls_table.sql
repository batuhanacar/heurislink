CREATE TABLE short_urls
(
    id UUID PRIMARY KEY,
    original_url TEXT NOT NULL,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);