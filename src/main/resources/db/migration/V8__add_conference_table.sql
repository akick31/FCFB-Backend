CREATE TABLE IF NOT EXISTS `conference` (
    `code` VARCHAR(50) NOT NULL PRIMARY KEY,
    `label` VARCHAR(100) NOT NULL,
    `logo_url` VARCHAR(500) NULL,
    `logo_url_dark` VARCHAR(500) NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `display_order` INT NOT NULL DEFAULT 0
);

INSERT INTO `conference` (`code`, `label`, `logo_url`, `logo_url_dark`, `active`, `display_order`)
VALUES
    ('SEC', 'SEC', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/8.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/8.png', 1, 0),
    ('BIG_TEN', 'Big Ten', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/5.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/5.png', 1, 1),
    ('ACC', 'ACC', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/1.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/1.png', 1, 2),
    ('BIG_12', 'Big 12', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/4.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/4.png', 1, 3),
    ('PAC_12', 'Pac-12', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/9.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/9.png', 1, 4),
    ('AMERICAN', 'American', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/151.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/151.png', 1, 5),
    ('MOUNTAIN_WEST', 'Mountain West', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/17.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/17.png', 1, 6),
    ('MAC', 'MAC', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/15.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/15.png', 1, 7),
    ('SUN_BELT', 'Sun Belt', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/37.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/37.png', 1, 8),
    ('MISSOURI_VALLEY', 'Missouri Valley', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/21.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/21.png', 1, 9),
    ('COLONIAL', 'Colonial', 'https://images.seeklogo.com/logo-png/49/2/colonial-athletic-association-logo-png_seeklogo-490062.png', NULL, 1, 10),
    ('NEC', 'NEC', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500/25.png', 'https://a.espncdn.com/i/teamlogos/ncaa_conf/500-dark/25.png', 1, 11),
    ('FBS_INDEPENDENT', 'FBS Independent', 'https://logos-world.net/wp-content/uploads/2025/01/Division-I-FBS-Independents-Logo-500x281.png', NULL, 1, 12),
    ('BIG_SKY', 'Big Sky', NULL, NULL, 0, 13),
    ('SOUTHLAND', 'Southland', NULL, NULL, 0, 14),
    ('CUSA', 'Conference USA', NULL, NULL, 0, 15),
    ('OHIO_VALLEY', 'Ohio Valley', NULL, NULL, 0, 16),
    ('PATRIOT', 'Patriot', NULL, NULL, 0, 17),
    ('PIONEER', 'Pioneer', NULL, NULL, 0, 18),
    ('IVY', 'Ivy League', NULL, NULL, 0, 19),
    ('INDEPENDENT', 'Independent', NULL, NULL, 0, 20),
    ('FCS_INDEPENDENT', 'FCS Independent', NULL, NULL, 0, 21),
    ('D2', 'Division II', NULL, NULL, 0, 22),
    ('D3', 'Division III', NULL, NULL, 0, 23),
    ('NAIA', 'NAIA', NULL, NULL, 0, 24),
    ('FAKE_TEAM', 'Fake Team', NULL, NULL, 0, 25)
ON DUPLICATE KEY UPDATE
    `label` = VALUES(`label`);
