CREATE TABLE `ranking_metric` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `season` int(11) NOT NULL,
  `week` int(11) NOT NULL,
  `metric_type` varchar(255) NOT NULL,
  `team_id` int(11) NOT NULL,
  `value` double NOT NULL,
  `wins` int(11) DEFAULT NULL,
  `losses` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ranking_metric_slot` (`season`,`week`,`metric_type`,`team_id`),
  KEY `idx_ranking_metric_lookup` (`season`,`week`,`metric_type`),
  KEY `idx_ranking_metric_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
