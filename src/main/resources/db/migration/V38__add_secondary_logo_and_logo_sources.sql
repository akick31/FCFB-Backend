ALTER TABLE team ADD COLUMN secondary_logo VARCHAR(512) DEFAULT NULL;

ALTER TABLE team_uniform_current ADD COLUMN helmet_logo_source VARCHAR(16) NOT NULL DEFAULT 'PRIMARY';

ALTER TABLE team_uniform_history ADD COLUMN helmet_logo_source VARCHAR(16) NOT NULL DEFAULT 'PRIMARY';

ALTER TABLE team_field
    ADD COLUMN midfield_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM',
    ADD COLUMN quarter_logo_source VARCHAR(16) NOT NULL DEFAULT 'CUSTOM';
