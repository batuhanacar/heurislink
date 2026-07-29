CREATE TABLE url_clicks
(
    id UUID PRIMARY KEY,
    short_url_id UUID NOT NULL,
    clicked_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_url_clicks_short_url
        FOREIGN KEY (short_url_id)
            REFERENCES short_urls(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_url_clicks_short_url_id
    ON url_clicks(short_url_id);

CREATE INDEX idx_url_clicks_clicked_at
    ON url_clicks(clicked_at);