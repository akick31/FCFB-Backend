INSERT IGNORE INTO venue (name, city, state, capacity, country) VALUES
    ('Thomas A. Robinson National Stadium', 'Nassau', NULL, 15000, 'Bahamas'),
    ('Cramton Bowl', 'Montgomery', 'AL', 25000, 'United States'),
    ('Toyota Stadium', 'Frisco', 'TX', 20500, 'United States'),
    ('Oracle Park', 'San Francisco', 'CA', 41915, 'United States');

CREATE TEMPORARY TABLE bowl_stadium (
    bowl VARCHAR(255) NOT NULL,
    venue VARCHAR(255) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO bowl_stadium (bowl, venue) VALUES
    ('Arizona Bowl', 'Casino Del Sol Stadium'),
    ('Armed Forces Bowl', 'Amon G. Carter Stadium'),
    ('Bahamas Bowl', 'Thomas A. Robinson National Stadium'),
    ('Cameilia Bowl', 'Cramton Bowl'),
    ('Camellia Bowl', 'Cramton Bowl'),
    ('Citrus Bowl', 'Camping World Stadium'),
    ('Cure Bowl', 'Acrisure Bounce House'),
    ('Dollar General Bowl', 'Hancock Whitney Stadium'),
    ('Duke''s Mayo Bowl', 'Bank of America Stadium'),
    ('First Responders Bowl', 'Gerald J. Ford Stadium'),
    ('Frisco Bowl', 'Toyota Stadium'),
    ('Gator Bowl', 'EverBank Stadium'),
    ('Hawaii Bowl', 'Clarence T.C. Ching Athletics Complex'),
    ('Holiday Bowl', 'Petco Park'),
    ('Independence Bowl', 'Independence Stadium'),
    ('Las Vegas Bowl', 'Allegiant Stadium'),
    ('Liberty Bowl', 'Simmons Bank Liberty Stadium'),
    ('Military Bowl', 'Navy-Marine Corps Memorial Stadium'),
    ('Music City Bowl', 'Nissan Stadium'),
    ('New Mexico Bowl', 'University Stadium'),
    ('New Orleans Bowl', 'Caesars Superdome'),
    ('Outback Bowl', 'Raymond James Stadium'),
    ('Pinstripe Bowl', 'Yankee Stadium'),
    ('Potato Bowl', 'Albertsons Stadium'),
    ('San Francisco Bowl', 'Oracle Park'),
    ('Sun Bowl', 'Sun Bowl'),
    ('Texas Bowl', 'NRG Stadium');

INSERT INTO bowl_field (bowl, wall_text, venue)
SELECT b.name, UPPER(b.name), v.name
FROM bowl b
JOIN bowl_stadium bs ON bs.bowl = b.name
JOIN venue v ON v.name = bs.venue
ON DUPLICATE KEY UPDATE venue = COALESCE(NULLIF(bowl_field.venue, ''), VALUES(venue));

DROP TEMPORARY TABLE bowl_stadium;

CREATE TEMPORARY TABLE championship_stadium (
    code VARCHAR(64) NOT NULL,
    venue VARCHAR(255) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO championship_stadium (code, venue) VALUES
    ('ACC', 'Bank of America Stadium'),
    ('BIG_12', 'AT&T Stadium'),
    ('BIG_TEN', 'Lucas Oil Stadium'),
    ('MAC', 'Ford Field'),
    ('PAC_12', 'Allegiant Stadium'),
    ('SEC', 'Mercedes-Benz Stadium');

UPDATE conference c
JOIN championship_stadium cs ON cs.code = c.code
JOIN venue v ON v.name = cs.venue
SET c.championship_venue = v.name
WHERE c.championship_venue IS NULL OR c.championship_venue = '';

DROP TEMPORARY TABLE championship_stadium;
