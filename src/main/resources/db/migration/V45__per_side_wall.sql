ALTER TABLE team_field
    ADD COLUMN right_wall_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN right_wall_logo_source VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_wall_logo_url VARCHAR(512) DEFAULT NULL,
    ADD COLUMN right_wall_text_outline_color VARCHAR(9) DEFAULT NULL;

ALTER TABLE game_field
    ADD COLUMN right_wall_color VARCHAR(9) DEFAULT NULL,
    ADD COLUMN right_wall_logo_source VARCHAR(16) DEFAULT NULL,
    ADD COLUMN right_wall_logo_url VARCHAR(512) DEFAULT NULL,
    ADD COLUMN right_wall_text_outline_color VARCHAR(9) DEFAULT NULL;
