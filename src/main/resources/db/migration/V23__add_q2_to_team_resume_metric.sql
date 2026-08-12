ALTER TABLE `team_resume_metric`
  ADD COLUMN `q2_wins` int(11) NOT NULL DEFAULT 0 AFTER `q1_losses`,
  ADD COLUMN `q2_losses` int(11) NOT NULL DEFAULT 0 AFTER `q2_wins`;
