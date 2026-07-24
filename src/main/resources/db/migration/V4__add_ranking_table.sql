CREATE TABLE `ranking` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `season` int(11) NOT NULL,
  `week` int(11) NOT NULL,
  `poll_type` varchar(255) NOT NULL,
  `poll_rank` int(11) NOT NULL,
  `team_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ranking_slot` (`season`,`week`,`poll_type`,`poll_rank`),
  KEY `idx_ranking_lookup` (`season`,`week`,`poll_type`),
  KEY `idx_ranking_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
