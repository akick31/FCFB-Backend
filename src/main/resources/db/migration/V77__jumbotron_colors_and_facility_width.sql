ALTER TABLE team_field
    ADD COLUMN jumbotron_color VARCHAR(16) NULL,
    ADD COLUMN right_jumbotron_color VARCHAR(16) NULL,
    ADD COLUMN topper_color VARCHAR(16) NULL,
    ADD COLUMN right_topper_color VARCHAR(16) NULL,
    ADD COLUMN facility_full_width BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN right_facility_full_width BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE game_field
    ADD COLUMN jumbotron_color VARCHAR(16) NULL,
    ADD COLUMN right_jumbotron_color VARCHAR(16) NULL,
    ADD COLUMN topper_color VARCHAR(16) NULL,
    ADD COLUMN right_topper_color VARCHAR(16) NULL,
    ADD COLUMN facility_full_width BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN right_facility_full_width BOOLEAN NOT NULL DEFAULT FALSE;
