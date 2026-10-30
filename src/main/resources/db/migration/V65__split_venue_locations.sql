CREATE TEMPORARY TABLE location_region (
    code VARCHAR(8) NOT NULL,
    name VARCHAR(64) NOT NULL,
    country VARCHAR(64) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO location_region (code, name, country) VALUES
    ('AL', 'Alabama', 'United States'),
    ('AK', 'Alaska', 'United States'),
    ('AZ', 'Arizona', 'United States'),
    ('AR', 'Arkansas', 'United States'),
    ('CA', 'California', 'United States'),
    ('CO', 'Colorado', 'United States'),
    ('CT', 'Connecticut', 'United States'),
    ('DE', 'Delaware', 'United States'),
    ('DC', 'District of Columbia', 'United States'),
    ('FL', 'Florida', 'United States'),
    ('GA', 'Georgia', 'United States'),
    ('HI', 'Hawaii', 'United States'),
    ('ID', 'Idaho', 'United States'),
    ('IL', 'Illinois', 'United States'),
    ('IN', 'Indiana', 'United States'),
    ('IA', 'Iowa', 'United States'),
    ('KS', 'Kansas', 'United States'),
    ('KY', 'Kentucky', 'United States'),
    ('LA', 'Louisiana', 'United States'),
    ('ME', 'Maine', 'United States'),
    ('MD', 'Maryland', 'United States'),
    ('MA', 'Massachusetts', 'United States'),
    ('MI', 'Michigan', 'United States'),
    ('MN', 'Minnesota', 'United States'),
    ('MS', 'Mississippi', 'United States'),
    ('MO', 'Missouri', 'United States'),
    ('MT', 'Montana', 'United States'),
    ('NE', 'Nebraska', 'United States'),
    ('NV', 'Nevada', 'United States'),
    ('NH', 'New Hampshire', 'United States'),
    ('NJ', 'New Jersey', 'United States'),
    ('NM', 'New Mexico', 'United States'),
    ('NY', 'New York', 'United States'),
    ('NC', 'North Carolina', 'United States'),
    ('ND', 'North Dakota', 'United States'),
    ('OH', 'Ohio', 'United States'),
    ('OK', 'Oklahoma', 'United States'),
    ('OR', 'Oregon', 'United States'),
    ('PA', 'Pennsylvania', 'United States'),
    ('RI', 'Rhode Island', 'United States');

INSERT INTO location_region (code, name, country) VALUES
    ('SC', 'South Carolina', 'United States'),
    ('SD', 'South Dakota', 'United States'),
    ('TN', 'Tennessee', 'United States'),
    ('TX', 'Texas', 'United States'),
    ('UT', 'Utah', 'United States'),
    ('VT', 'Vermont', 'United States'),
    ('VA', 'Virginia', 'United States'),
    ('WA', 'Washington', 'United States'),
    ('WV', 'West Virginia', 'United States'),
    ('WI', 'Wisconsin', 'United States'),
    ('WY', 'Wyoming', 'United States'),
    ('AB', 'Alberta', 'Canada'),
    ('BC', 'British Columbia', 'Canada'),
    ('MB', 'Manitoba', 'Canada'),
    ('NB', 'New Brunswick', 'Canada'),
    ('NL', 'Newfoundland and Labrador', 'Canada'),
    ('NS', 'Nova Scotia', 'Canada'),
    ('NT', 'Northwest Territories', 'Canada'),
    ('NU', 'Nunavut', 'Canada'),
    ('ON', 'Ontario', 'Canada'),
    ('PE', 'Prince Edward Island', 'Canada'),
    ('QC', 'Quebec', 'Canada'),
    ('SK', 'Saskatchewan', 'Canada'),
    ('YT', 'Yukon', 'Canada');

CREATE TEMPORARY TABLE location_country (
    alias VARCHAR(64) NOT NULL,
    country VARCHAR(64) NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO location_country (alias, country) VALUES
    ('United States', 'United States'),
    ('Afghanistan', 'Afghanistan'),
    ('Åland Islands', 'Åland Islands'),
    ('Albania', 'Albania'),
    ('Algeria', 'Algeria'),
    ('American Samoa', 'American Samoa'),
    ('Andorra', 'Andorra'),
    ('Angola', 'Angola'),
    ('Anguilla', 'Anguilla'),
    ('Antarctica', 'Antarctica'),
    ('Antigua & Barbuda', 'Antigua & Barbuda'),
    ('Argentina', 'Argentina'),
    ('Armenia', 'Armenia'),
    ('Aruba', 'Aruba'),
    ('Australia', 'Australia'),
    ('Austria', 'Austria'),
    ('Azerbaijan', 'Azerbaijan'),
    ('Bahamas', 'Bahamas'),
    ('Bahrain', 'Bahrain'),
    ('Bangladesh', 'Bangladesh'),
    ('Barbados', 'Barbados'),
    ('Belarus', 'Belarus'),
    ('Belgium', 'Belgium'),
    ('Belize', 'Belize'),
    ('Benin', 'Benin'),
    ('Bermuda', 'Bermuda'),
    ('Bhutan', 'Bhutan'),
    ('Bolivia', 'Bolivia'),
    ('Bosnia & Herzegovina', 'Bosnia & Herzegovina'),
    ('Botswana', 'Botswana'),
    ('Bouvet Island', 'Bouvet Island'),
    ('Brazil', 'Brazil'),
    ('British Indian Ocean Territory', 'British Indian Ocean Territory'),
    ('British Virgin Islands', 'British Virgin Islands'),
    ('Brunei', 'Brunei'),
    ('Bulgaria', 'Bulgaria'),
    ('Burkina Faso', 'Burkina Faso'),
    ('Burundi', 'Burundi'),
    ('Cambodia', 'Cambodia'),
    ('Cameroon', 'Cameroon');

INSERT INTO location_country (alias, country) VALUES
    ('Canada', 'Canada'),
    ('Cape Verde', 'Cape Verde'),
    ('Caribbean Netherlands', 'Caribbean Netherlands'),
    ('Cayman Islands', 'Cayman Islands'),
    ('Central African Republic', 'Central African Republic'),
    ('Chad', 'Chad'),
    ('Chile', 'Chile'),
    ('China', 'China'),
    ('Christmas Island', 'Christmas Island'),
    ('Cocos (Keeling) Islands', 'Cocos (Keeling) Islands'),
    ('Colombia', 'Colombia'),
    ('Comoros', 'Comoros'),
    ('Congo - Brazzaville', 'Congo - Brazzaville'),
    ('Congo - Kinshasa', 'Congo - Kinshasa'),
    ('Cook Islands', 'Cook Islands'),
    ('Costa Rica', 'Costa Rica'),
    ('Côte d’Ivoire', 'Côte d’Ivoire'),
    ('Croatia', 'Croatia'),
    ('Cuba', 'Cuba'),
    ('Curaçao', 'Curaçao'),
    ('Cyprus', 'Cyprus'),
    ('Czechia', 'Czechia'),
    ('Denmark', 'Denmark'),
    ('Djibouti', 'Djibouti'),
    ('Dominica', 'Dominica'),
    ('Dominican Republic', 'Dominican Republic'),
    ('Ecuador', 'Ecuador'),
    ('Egypt', 'Egypt'),
    ('El Salvador', 'El Salvador'),
    ('Equatorial Guinea', 'Equatorial Guinea'),
    ('Eritrea', 'Eritrea'),
    ('Estonia', 'Estonia'),
    ('Eswatini', 'Eswatini'),
    ('Ethiopia', 'Ethiopia'),
    ('Falkland Islands', 'Falkland Islands'),
    ('Faroe Islands', 'Faroe Islands'),
    ('Fiji', 'Fiji'),
    ('Finland', 'Finland'),
    ('France', 'France'),
    ('French Guiana', 'French Guiana');

INSERT INTO location_country (alias, country) VALUES
    ('French Polynesia', 'French Polynesia'),
    ('French Southern Territories', 'French Southern Territories'),
    ('Gabon', 'Gabon'),
    ('Gambia', 'Gambia'),
    ('Germany', 'Germany'),
    ('Ghana', 'Ghana'),
    ('Gibraltar', 'Gibraltar'),
    ('Greece', 'Greece'),
    ('Greenland', 'Greenland'),
    ('Grenada', 'Grenada'),
    ('Guadeloupe', 'Guadeloupe'),
    ('Guam', 'Guam'),
    ('Guatemala', 'Guatemala'),
    ('Guernsey', 'Guernsey'),
    ('Guinea', 'Guinea'),
    ('Guinea-Bissau', 'Guinea-Bissau'),
    ('Guyana', 'Guyana'),
    ('Haiti', 'Haiti'),
    ('Heard & McDonald Islands', 'Heard & McDonald Islands'),
    ('Honduras', 'Honduras'),
    ('Hong Kong SAR China', 'Hong Kong SAR China'),
    ('Hungary', 'Hungary'),
    ('Iceland', 'Iceland'),
    ('India', 'India'),
    ('Indonesia', 'Indonesia'),
    ('Iran', 'Iran'),
    ('Iraq', 'Iraq'),
    ('Ireland', 'Ireland'),
    ('Isle of Man', 'Isle of Man'),
    ('Israel', 'Israel'),
    ('Italy', 'Italy'),
    ('Jamaica', 'Jamaica'),
    ('Japan', 'Japan'),
    ('Jersey', 'Jersey'),
    ('Jordan', 'Jordan'),
    ('Kazakhstan', 'Kazakhstan'),
    ('Kenya', 'Kenya'),
    ('Kiribati', 'Kiribati'),
    ('Kosovo', 'Kosovo'),
    ('Kuwait', 'Kuwait');

INSERT INTO location_country (alias, country) VALUES
    ('Kyrgyzstan', 'Kyrgyzstan'),
    ('Laos', 'Laos'),
    ('Latvia', 'Latvia'),
    ('Lebanon', 'Lebanon'),
    ('Lesotho', 'Lesotho'),
    ('Liberia', 'Liberia'),
    ('Libya', 'Libya'),
    ('Liechtenstein', 'Liechtenstein'),
    ('Lithuania', 'Lithuania'),
    ('Luxembourg', 'Luxembourg'),
    ('Macao SAR China', 'Macao SAR China'),
    ('Madagascar', 'Madagascar'),
    ('Malawi', 'Malawi'),
    ('Malaysia', 'Malaysia'),
    ('Maldives', 'Maldives'),
    ('Mali', 'Mali'),
    ('Malta', 'Malta'),
    ('Marshall Islands', 'Marshall Islands'),
    ('Martinique', 'Martinique'),
    ('Mauritania', 'Mauritania'),
    ('Mauritius', 'Mauritius'),
    ('Mayotte', 'Mayotte'),
    ('Mexico', 'Mexico'),
    ('Micronesia', 'Micronesia'),
    ('Moldova', 'Moldova'),
    ('Monaco', 'Monaco'),
    ('Mongolia', 'Mongolia'),
    ('Montenegro', 'Montenegro'),
    ('Montserrat', 'Montserrat'),
    ('Morocco', 'Morocco'),
    ('Mozambique', 'Mozambique'),
    ('Myanmar (Burma)', 'Myanmar (Burma)'),
    ('Namibia', 'Namibia'),
    ('Nauru', 'Nauru'),
    ('Nepal', 'Nepal'),
    ('Netherlands', 'Netherlands'),
    ('New Caledonia', 'New Caledonia'),
    ('New Zealand', 'New Zealand'),
    ('Nicaragua', 'Nicaragua'),
    ('Niger', 'Niger');

INSERT INTO location_country (alias, country) VALUES
    ('Nigeria', 'Nigeria'),
    ('Niue', 'Niue'),
    ('Norfolk Island', 'Norfolk Island'),
    ('North Korea', 'North Korea'),
    ('North Macedonia', 'North Macedonia'),
    ('Northern Mariana Islands', 'Northern Mariana Islands'),
    ('Norway', 'Norway'),
    ('Oman', 'Oman'),
    ('Pakistan', 'Pakistan'),
    ('Palau', 'Palau'),
    ('Palestinian Territories', 'Palestinian Territories'),
    ('Panama', 'Panama'),
    ('Papua New Guinea', 'Papua New Guinea'),
    ('Paraguay', 'Paraguay'),
    ('Peru', 'Peru'),
    ('Philippines', 'Philippines'),
    ('Pitcairn Islands', 'Pitcairn Islands'),
    ('Poland', 'Poland'),
    ('Portugal', 'Portugal'),
    ('Puerto Rico', 'Puerto Rico'),
    ('Qatar', 'Qatar'),
    ('Réunion', 'Réunion'),
    ('Romania', 'Romania'),
    ('Russia', 'Russia'),
    ('Rwanda', 'Rwanda'),
    ('Samoa', 'Samoa'),
    ('San Marino', 'San Marino'),
    ('São Tomé & Príncipe', 'São Tomé & Príncipe'),
    ('Saudi Arabia', 'Saudi Arabia'),
    ('Senegal', 'Senegal'),
    ('Serbia', 'Serbia'),
    ('Seychelles', 'Seychelles'),
    ('Sierra Leone', 'Sierra Leone'),
    ('Singapore', 'Singapore'),
    ('Sint Maarten', 'Sint Maarten'),
    ('Slovakia', 'Slovakia'),
    ('Slovenia', 'Slovenia'),
    ('Solomon Islands', 'Solomon Islands'),
    ('Somalia', 'Somalia'),
    ('South Africa', 'South Africa');

INSERT INTO location_country (alias, country) VALUES
    ('South Georgia & South Sandwich Islands', 'South Georgia & South Sandwich Islands'),
    ('South Korea', 'South Korea'),
    ('South Sudan', 'South Sudan'),
    ('Spain', 'Spain'),
    ('Sri Lanka', 'Sri Lanka'),
    ('St. Barthélemy', 'St. Barthélemy'),
    ('St. Helena', 'St. Helena'),
    ('St. Kitts & Nevis', 'St. Kitts & Nevis'),
    ('St. Lucia', 'St. Lucia'),
    ('St. Martin', 'St. Martin'),
    ('St. Pierre & Miquelon', 'St. Pierre & Miquelon'),
    ('St. Vincent & Grenadines', 'St. Vincent & Grenadines'),
    ('Sudan', 'Sudan'),
    ('Suriname', 'Suriname'),
    ('Svalbard & Jan Mayen', 'Svalbard & Jan Mayen'),
    ('Sweden', 'Sweden'),
    ('Switzerland', 'Switzerland'),
    ('Syria', 'Syria'),
    ('Taiwan', 'Taiwan'),
    ('Tajikistan', 'Tajikistan'),
    ('Tanzania', 'Tanzania'),
    ('Thailand', 'Thailand'),
    ('Timor-Leste', 'Timor-Leste'),
    ('Togo', 'Togo'),
    ('Tokelau', 'Tokelau'),
    ('Tonga', 'Tonga'),
    ('Trinidad & Tobago', 'Trinidad & Tobago'),
    ('Tunisia', 'Tunisia'),
    ('Türkiye', 'Türkiye'),
    ('Turkmenistan', 'Turkmenistan'),
    ('Turks & Caicos Islands', 'Turks & Caicos Islands'),
    ('Tuvalu', 'Tuvalu'),
    ('U.S. Outlying Islands', 'U.S. Outlying Islands'),
    ('U.S. Virgin Islands', 'U.S. Virgin Islands'),
    ('Uganda', 'Uganda'),
    ('Ukraine', 'Ukraine'),
    ('United Arab Emirates', 'United Arab Emirates'),
    ('United Kingdom', 'United Kingdom'),
    ('Uruguay', 'Uruguay'),
    ('Uzbekistan', 'Uzbekistan');

INSERT INTO location_country (alias, country) VALUES
    ('Vanuatu', 'Vanuatu'),
    ('Vatican City', 'Vatican City'),
    ('Venezuela', 'Venezuela'),
    ('Vietnam', 'Vietnam'),
    ('Wallis & Futuna', 'Wallis & Futuna'),
    ('Western Sahara', 'Western Sahara'),
    ('Yemen', 'Yemen'),
    ('Zambia', 'Zambia'),
    ('Zimbabwe', 'Zimbabwe'),
    ('USA', 'United States'),
    ('US', 'United States'),
    ('U.S.', 'United States'),
    ('U.S.A.', 'United States'),
    ('United States of America', 'United States'),
    ('UK', 'United Kingdom'),
    ('England', 'United Kingdom'),
    ('Scotland', 'United Kingdom'),
    ('Wales', 'United Kingdom'),
    ('Northern Ireland', 'United Kingdom'),
    ('Great Britain', 'United Kingdom'),
    ('UAE', 'United Arab Emirates'),
    ('Turkey', 'Türkiye'),
    ('Ivory Coast', 'Côte d’Ivoire'),
    ('Czech Republic', 'Czechia'),
    ('Holland', 'Netherlands'),
    ('Korea', 'South Korea'),
    ('Republic of Ireland', 'Ireland');

CREATE TEMPORARY TABLE venue_split (
    old_name VARCHAR(255) NOT NULL,
    rest VARCHAR(255) NOT NULL,
    segment VARCHAR(255) NULL,
    head VARCHAR(255) NULL,
    city VARCHAR(255) NULL,
    state VARCHAR(8) NULL,
    country VARCHAR(64) NULL,
    has_twin TINYINT(1) NOT NULL DEFAULT 0
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO venue_split (old_name, rest)
SELECT name, name FROM venue WHERE name LIKE '%,%';

UPDATE venue_split
SET segment = TRIM(SUBSTRING_INDEX(rest, ',', -1)),
    head = TRIM(LEFT(rest, CHAR_LENGTH(rest) - CHAR_LENGTH(SUBSTRING_INDEX(rest, ',', -1)) - 1))
WHERE rest LIKE '%,%';

UPDATE venue_split s
JOIN location_country c ON c.alias = s.segment
SET s.country = c.country, s.rest = s.head
WHERE s.rest LIKE '%,%';

UPDATE venue_split
SET segment = TRIM(SUBSTRING_INDEX(rest, ',', -1)),
    head = TRIM(LEFT(rest, CHAR_LENGTH(rest) - CHAR_LENGTH(SUBSTRING_INDEX(rest, ',', -1)) - 1))
WHERE rest LIKE '%,%';

UPDATE venue_split s
JOIN location_region r ON r.code = s.segment OR r.name = s.segment
SET s.state = r.code, s.country = COALESCE(s.country, r.country), s.rest = s.head
WHERE s.rest LIKE '%,%';

UPDATE venue_split
SET segment = TRIM(SUBSTRING_INDEX(rest, ',', -1)),
    head = TRIM(LEFT(rest, CHAR_LENGTH(rest) - CHAR_LENGTH(SUBSTRING_INDEX(rest, ',', -1)) - 1))
WHERE rest LIKE '%,%';

UPDATE venue_split s
JOIN location_region r ON s.segment LIKE CONCAT('% ', r.name)
SET s.state = r.code,
    s.country = COALESCE(s.country, r.country),
    s.city = TRIM(LEFT(s.segment, CHAR_LENGTH(s.segment) - CHAR_LENGTH(r.name))),
    s.rest = s.head
WHERE s.rest LIKE '%,%' AND s.state IS NULL AND r.name LIKE '% %';

UPDATE venue_split
SET segment = TRIM(SUBSTRING_INDEX(rest, ',', -1)),
    head = TRIM(LEFT(rest, CHAR_LENGTH(rest) - CHAR_LENGTH(SUBSTRING_INDEX(rest, ',', -1)) - 1))
WHERE rest LIKE '%,%';

UPDATE venue_split s
JOIN location_region r ON s.segment LIKE CONCAT('% ', r.name)
SET s.state = r.code,
    s.country = COALESCE(s.country, r.country),
    s.city = TRIM(LEFT(s.segment, CHAR_LENGTH(s.segment) - CHAR_LENGTH(r.name))),
    s.rest = s.head
WHERE s.rest LIKE '%,%' AND s.state IS NULL AND r.name NOT LIKE '% %';

UPDATE venue_split
SET segment = TRIM(SUBSTRING_INDEX(rest, ',', -1)),
    head = TRIM(LEFT(rest, CHAR_LENGTH(rest) - CHAR_LENGTH(SUBSTRING_INDEX(rest, ',', -1)) - 1))
WHERE rest LIKE '%,%';

UPDATE venue_split
SET city = segment, rest = head
WHERE rest LIKE '%,%' AND city IS NULL;

UPDATE venue_split s
JOIN venue v ON v.name = s.rest
SET s.has_twin = 1;

CREATE TEMPORARY TABLE venue_split_keeper DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
SELECT rest, MIN(old_name) AS old_name FROM venue_split GROUP BY rest;

UPDATE venue_split s
JOIN venue_split_keeper k ON k.rest = s.rest
SET s.has_twin = 1
WHERE s.old_name <> k.old_name;

UPDATE team x
JOIN venue_split s ON x.stadium = s.old_name
SET x.stadium = s.rest;

UPDATE game x
JOIN venue_split s ON x.venue = s.old_name
SET x.venue = s.rest;

UPDATE game_presentation x
JOIN venue_split s ON x.venue = s.old_name
SET x.venue = s.rest;

UPDATE schedule x
JOIN venue_split s ON x.venue = s.old_name
SET x.venue = s.rest;

UPDATE conference x
JOIN venue_split s ON x.championship_venue = s.old_name
SET x.championship_venue = s.rest;

UPDATE bowl x
JOIN venue_split s ON x.last_venue = s.old_name
SET x.last_venue = s.rest;

UPDATE bowl_field x
JOIN venue_split s ON x.venue = s.old_name
SET x.venue = s.rest;

UPDATE playoff_field x
JOIN venue_split s ON x.venue = s.old_name
SET x.venue = s.rest;

UPDATE venue v
JOIN venue_split s ON v.name = s.rest
SET v.city = s.city, v.state = s.state, v.country = COALESCE(s.country, 'United States')
WHERE (v.city IS NULL OR v.city = '') AND (v.state IS NULL OR v.state = '');

DELETE v FROM venue v
JOIN venue_split s ON v.name = s.old_name
WHERE s.has_twin = 1;

UPDATE venue v
JOIN venue_split s ON v.name = s.old_name
SET v.name = s.rest, v.city = s.city, v.state = s.state, v.country = COALESCE(s.country, 'United States')
WHERE s.has_twin = 0;

UPDATE venue v
JOIN location_region r ON r.code = UPPER(TRIM(v.state)) OR r.name = TRIM(v.state)
SET v.state = r.code, v.country = r.country;

DROP TEMPORARY TABLE venue_split_keeper;
DROP TEMPORARY TABLE venue_split;
DROP TEMPORARY TABLE location_country;
DROP TEMPORARY TABLE location_region;
