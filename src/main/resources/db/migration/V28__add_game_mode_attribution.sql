ALTER TABLE `game`
    ADD COLUMN `game_mode_set_by` VARCHAR(255) NULL,
    ADD COLUMN `game_mode_set_at` DATETIME NULL;

UPDATE `game_writeup`
SET `message` = CONCAT(`message`, '<br><br>Set by {game_mode_changed_by}.')
WHERE `scenario` = 'CHEW_MODE_ENABLED'
  AND `message` NOT LIKE '%{game_mode_changed_by}%';

INSERT INTO `game_writeup` (`scenario`, `play_call`, `message`)
VALUES (
    'CHEW_MODE_DISABLED',
    NULL,
    '{home_coach} {away_coach}<br><br>Chew mode is off. Plays run off the normal amount of time again.<br><br>Set by {game_mode_changed_by}.'
);
