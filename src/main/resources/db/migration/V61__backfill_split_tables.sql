INSERT INTO `platform_transport` (`game_id`, `home_platform`, `home_platform_id`, `away_platform`, `away_platform_id`, `request_message_id`, `last_message_timestamp`)
SELECT `game_id`, `home_platform`, `home_platform_id`, `away_platform`, `away_platform_id`, `request_message_id`, `last_message_timestamp`
FROM `game`
ON DUPLICATE KEY UPDATE
    `home_platform` = VALUES(`home_platform`),
    `home_platform_id` = VALUES(`home_platform_id`),
    `away_platform` = VALUES(`away_platform`),
    `away_platform_id` = VALUES(`away_platform_id`),
    `request_message_id` = VALUES(`request_message_id`),
    `last_message_timestamp` = VALUES(`last_message_timestamp`);

INSERT INTO `game_presentation` (`game_id`, `tv_channel`, `postseason_game_logo`, `postseason_game_name`, `neutral_site`, `venue`)
SELECT `game_id`, `tv_channel`, `postseason_game_logo`, `postseason_game_name`, `neutral_site`, `venue`
FROM `game`
ON DUPLICATE KEY UPDATE
    `tv_channel` = VALUES(`tv_channel`),
    `postseason_game_logo` = VALUES(`postseason_game_logo`),
    `postseason_game_name` = VALUES(`postseason_game_name`),
    `neutral_site` = VALUES(`neutral_site`),
    `venue` = VALUES(`venue`);

INSERT INTO `game_record_snapshot` (`game_id`, `home_team_rank`, `away_team_rank`, `home_wins`, `home_losses`, `away_wins`, `away_losses`)
SELECT `game_id`, `home_team_rank`, `away_team_rank`, `home_wins`, `home_losses`, `away_wins`, `away_losses`
FROM `game`
ON DUPLICATE KEY UPDATE
    `home_team_rank` = VALUES(`home_team_rank`),
    `away_team_rank` = VALUES(`away_team_rank`),
    `home_wins` = VALUES(`home_wins`),
    `home_losses` = VALUES(`home_losses`),
    `away_wins` = VALUES(`away_wins`),
    `away_losses` = VALUES(`away_losses`);

INSERT INTO `game_coin_toss` (`game_id`, `coin_toss_winner`, `coin_toss_choice`, `overtime_coin_toss_winner`, `overtime_coin_toss_choice`)
SELECT `game_id`, `coin_toss_winner`, `coin_toss_choice`, `overtime_coin_toss_winner`, `overtime_coin_toss_choice`
FROM `game`
ON DUPLICATE KEY UPDATE
    `coin_toss_winner` = VALUES(`coin_toss_winner`),
    `coin_toss_choice` = VALUES(`coin_toss_choice`),
    `overtime_coin_toss_winner` = VALUES(`overtime_coin_toss_winner`),
    `overtime_coin_toss_choice` = VALUES(`overtime_coin_toss_choice`);

INSERT INTO `game_vegas_odds` (`game_id`, `home_vegas_spread`, `away_vegas_spread`)
SELECT `game_id`, `home_vegas_spread`, `away_vegas_spread`
FROM `game`
ON DUPLICATE KEY UPDATE
    `home_vegas_spread` = VALUES(`home_vegas_spread`),
    `away_vegas_spread` = VALUES(`away_vegas_spread`);

INSERT INTO `user_credential` (`user_id`, `email`, `hashed_email`, `password`, `salt`, `api_key_hash`, `reset_token`, `reset_token_expiration`)
SELECT `id`, `email`, `hashed_email`, `password`, `salt`, `api_key_hash`, `reset_token`, `reset_token_expiration`
FROM `user`
ON DUPLICATE KEY UPDATE
    `email` = VALUES(`email`),
    `hashed_email` = VALUES(`hashed_email`),
    `password` = VALUES(`password`),
    `salt` = VALUES(`salt`),
    `api_key_hash` = VALUES(`api_key_hash`),
    `reset_token` = VALUES(`reset_token`),
    `reset_token_expiration` = VALUES(`reset_token_expiration`);

INSERT INTO `user_preferences` (`user_id`, `delay_of_game_warning_opt_out`, `scouting_report_hidden_columns`)
SELECT `id`, `delay_of_game_warning_opt_out`, `scouting_report_hidden_columns`
FROM `user`
ON DUPLICATE KEY UPDATE
    `delay_of_game_warning_opt_out` = VALUES(`delay_of_game_warning_opt_out`),
    `scouting_report_hidden_columns` = VALUES(`scouting_report_hidden_columns`);
