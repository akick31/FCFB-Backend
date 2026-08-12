ALTER TABLE `game_stats`
  ADD COLUMN `favored_margin` double DEFAULT NULL,
  ADD COLUMN `upset_margin` double DEFAULT NULL;
