ALTER TABLE `game_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_count` int NOT NULL DEFAULT 0;

ALTER TABLE `season_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_count` int NOT NULL DEFAULT 0;

ALTER TABLE `postseason_season_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_count` int NOT NULL DEFAULT 0;

ALTER TABLE `conference_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_count` int NOT NULL DEFAULT 0;

ALTER TABLE `postseason_conference_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `punt_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_offensive_play_count` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `opponent_punt_count` int NOT NULL DEFAULT 0;

ALTER TABLE `league_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0;

ALTER TABLE `postseason_league_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0;

ALTER TABLE `playbook_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0;

ALTER TABLE `postseason_playbook_stats`
  ADD COLUMN `offensive_play_yards` int NOT NULL DEFAULT 0,
  ADD COLUMN `offensive_play_count` int NOT NULL DEFAULT 0;
