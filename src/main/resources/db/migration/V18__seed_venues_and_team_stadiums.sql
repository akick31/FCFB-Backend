-- Seed data only (no schema changes). Team stadiums and venue catalogue rows sourced from
-- per-team verified web searches (current stadium name, city, state, capacity as of Aug 2026),
-- not a single bulk page scrape. FCS teams are not seeded here (no reliable FBS-only source
-- covers them) and will have NULL stadium until a future pass adds one.

INSERT INTO `venue` (`name`, `city`, `state`, `capacity`) VALUES
  ('Falcon Stadium', 'Colorado Springs', 'CO', 39441),
  ('InfoCision Stadium', 'Akron', 'OH', 30000),
  ('Bryant-Denny Stadium', 'Tuscaloosa', 'AL', 100077),
  ('Casino Del Sol Stadium', 'Tucson', 'AZ', 50782),
  ('Mountain America Stadium', 'Tempe', 'AZ', 53599),
  ('Donald W. Reynolds Razorback Stadium', 'Fayetteville', 'AR', 76212),
  ('Centennial Bank Stadium', 'Jonesboro', 'AR', 30406),
  ('Michie Stadium', 'West Point', 'NY', 36000),
  ('Jordan-Hare Stadium', 'Auburn', 'AL', 88043),
  ('LaVell Edwards Stadium', 'Provo', 'UT', 62073),
  ('Scheumann Stadium', 'Muncie', 'IN', 22500),
  ('McLane Stadium', 'Waco', 'TX', 45140),
  ('Albertsons Stadium', 'Boise', 'ID', 36387),
  ('Doyt L. Perry Stadium', 'Bowling Green', 'OH', 24000),
  ('Broadview Stadium', 'Amherst', 'NY', 25013),
  ('California Memorial Stadium', 'Berkeley', 'CA', 63186),
  ('Kelly/Shorts Stadium', 'Mount Pleasant', 'MI', 30255),
  ('Jerry Richardson Stadium', 'Charlotte', 'NC', 15314),
  ('Nippert Stadium', 'Cincinnati', 'OH', 38088),
  ('Memorial Stadium', 'Clemson', 'SC', 81500),
  ('Brooks Stadium', 'Conway', 'SC', 21000),
  ('Folsom Field', 'Boulder', 'CO', 50183),
  ('Canvas Stadium', 'Fort Collins', 'CO', 36500),
  ('Pratt & Whitney Stadium at Rentschler Field', 'East Hartford', 'CT', 40000),
  ('Wallace Wade Stadium', 'Durham', 'NC', 35018),
  ('Rynearson Stadium', 'Ypsilanti', 'MI', 30200),
  ('Ben Hill Griffin Stadium', 'Gainesville', 'FL', 88548),
  ('Doak Campbell Stadium', 'Tallahassee', 'FL', 67277),
  ('Sanford Stadium', 'Athens', 'GA', 93033),
  ('Allen E. Paulson Stadium', 'Statesboro', 'GA', 25000),
  ('Center Parc Stadium', 'Atlanta', 'GA', 24333),
  ('Bobby Dodd Stadium', 'Atlanta', 'GA', 51913),
  ('Clarence T.C. Ching Athletics Complex', 'Honolulu', 'HI', 15194),
  ('Gies Memorial Stadium', 'Champaign', 'IL', 60670),
  ('Memorial Stadium (Merchants Bank Field at Memorial Stadium)', 'Bloomington', 'IN', 53524),
  ('Jack Trice Stadium', 'Ames', 'IA', 61500),
  ('David Booth Kansas Memorial Stadium', 'Lawrence', 'KS', 41525),
  ('Bill Snyder Family Stadium', 'Manhattan', 'KS', 50000),
  ('Fifth Third Stadium', 'Kennesaw', 'GA', 10200),
  ('Dix Stadium', 'Kent', 'OH', 25319),
  ('Tiger Stadium', 'Baton Rouge', 'LA', 102321),
  ('Joe Aillet Stadium', 'Ruston', 'LA', 28562),
  ('L&N Federal Credit Union Stadium', 'Louisville', 'KY', 60800),
  ('SECU Stadium', 'College Park', 'MD', 46185),
  ('Warren McGuirk Alumni Stadium', 'Amherst', 'MA', 17000),
  ('Hard Rock Stadium', 'Miami Gardens', 'FL', 65326),
  ('Yager Stadium', 'Oxford', 'OH', 24286),
  ('Michigan Stadium', 'Ann Arbor', 'MI', 107601),
  ('Spartan Stadium', 'East Lansing', 'MI', 74866),
  ('Huntington Bank Stadium', 'Minneapolis', 'MN', 50805),
  ('Navy-Marine Corps Memorial Stadium', 'Annapolis', 'MD', 34000),
  ('University Stadium', 'Albuquerque', 'NM', 39224),
  ('Kenan Memorial Stadium', 'Chapel Hill', 'NC', 50500),
  ('DATCU Stadium', 'Denton', 'TX', 30100),
  ('Huskie Stadium', 'DeKalb', 'IL', 28211),
  ('Ryan Field', 'Evanston', 'IL', 35000),
  ('Notre Dame Stadium', 'Notre Dame', 'IN', 77622),
  ('Peden Stadium', 'Athens', 'OH', 24000),
  ('Ohio Stadium', 'Columbus', 'OH', 102780),
  ('Gaylord Family Oklahoma Memorial Stadium', 'Norman', 'OK', 80126),
  ('Boone Pickens Stadium', 'Stillwater', 'OK', 52305),
  ('Autzen Stadium', 'Eugene', 'OR', 54000),
  ('Reser Stadium', 'Corvallis', 'OR', 35548),
  ('Beaver Stadium', 'University Park', 'PA', 106572),
  ('Acrisure Stadium', 'Pittsburgh', 'PA', 51416),
  ('Ross-Ade Stadium', 'West Lafayette', 'IN', 61441),
  ('SHI Stadium', 'Piscataway', 'NJ', 52454),
  ('Gerald J. Ford Stadium', 'Dallas', 'TX', 33200),
  ('Bowers Stadium', 'Huntsville', 'TX', 14000),
  ('Snapdragon Stadium', 'San Diego', 'CA', 35000),
  ('CEFCU Stadium', 'San Jose', 'CA', 18203),
  ('Williams-Brice Stadium', 'Columbia', 'SC', 77559),
  ('M.M. Roberts Stadium', 'Hattiesburg', 'MS', 36000),
  ('Stanford Stadium', 'Stanford', 'CA', 50424),
  ('JMA Wireless Dome', 'Syracuse', 'NY', 42784),
  ('Amon G. Carter Stadium', 'Fort Worth', 'TX', 47000),
  ('Neyland Stadium', 'Knoxville', 'TN', 101915),
  ('Darrell K Royal-Texas Memorial Stadium', 'Austin', 'TX', 100119),
  ('Kyle Field', 'College Station', 'TX', 102733),
  ('Galaxy Stadium', 'Lubbock', 'TX', 60229),
  ('Glass Bowl', 'Toledo', 'OH', 26248),
  ('Veterans Memorial Stadium', 'Troy', 'AL', 30470),
  ('Yulman Stadium', 'New Orleans', 'LA', 30000),
  ('Rose Bowl', 'Pasadena', 'CA', 91136),
  ('Allegiant Stadium', 'Las Vegas', 'NV', 65000),
  ('Los Angeles Memorial Coliseum', 'Los Angeles', 'CA', 77500),
  ('Raymond James Stadium', 'Tampa', 'FL', 69218),
  ('Alamodome', 'San Antonio', 'TX', 36582),
  ('Rice-Eccles Stadium', 'Salt Lake City', 'UT', 51444),
  ('Merlin Olsen Field at Maverik Stadium', 'Logan', 'UT', 25513),
  ('FirstBank Stadium', 'Nashville', 'TN', 35000),
  ('Scott Stadium', 'Charlottesville', 'VA', 61500),
  ('Lane Stadium', 'Blacksburg', 'VA', 65632),
  ('Allegacy Federal Credit Union Stadium', 'Winston-Salem', 'NC', 31500),
  ('Alaska Airlines Field at Husky Stadium', 'Seattle', 'WA', 70138),
  ('Gesa Field at Martin Stadium', 'Pullman', 'WA', 32952),
  ('Milan Puskar Stadium', 'Morgantown', 'WV', 60000),
  ('Waldo Stadium', 'Kalamazoo', 'MI', 30200),
  ('Camp Randall Stadium', 'Madison', 'WI', 76057),
  ('Jonah Field at War Memorial Stadium', 'Laramie', 'WY', 29811),
  ('Mercedes-Benz Stadium', 'Atlanta', 'GA', 71000),
  ('Caesars Superdome', 'New Orleans', 'LA', 73208),
  ('AT&T Stadium', 'Arlington', 'TX', 80000),
  ('Camping World Stadium', 'Orlando', 'FL', 60219),
  ('Bank of America Stadium', 'Charlotte', 'NC', 75037),
  ('State Farm Stadium', 'Glendale', 'AZ', 63400),
  ('NRG Stadium', 'Houston', 'TX', 72220),
  ('Lucas Oil Stadium', 'Indianapolis', 'IN', 67000),
  ('Ford Field', 'Detroit', 'MI', 65000),
  ('SoFi Stadium', 'Inglewood', 'CA', 70240),
  ('Nissan Stadium', 'Nashville', 'TN', 69143),
  ('EverBank Stadium', 'Jacksonville', 'FL', 67838),
  ('Yankee Stadium', 'Bronx', 'NY', 54251),
  ('Independence Stadium', 'Shreveport', 'LA', 50000)
ON DUPLICATE KEY UPDATE city = VALUES(city), state = VALUES(state), capacity = VALUES(capacity);

UPDATE `team` SET `stadium` = 'Falcon Stadium' WHERE `name` = 'Air Force';
UPDATE `team` SET `stadium` = 'InfoCision Stadium' WHERE `name` = 'Akron';
UPDATE `team` SET `stadium` = 'Bryant-Denny Stadium' WHERE `name` = 'Alabama';
UPDATE `team` SET `stadium` = 'Casino Del Sol Stadium' WHERE `name` = 'Arizona';
UPDATE `team` SET `stadium` = 'Mountain America Stadium' WHERE `name` = 'Arizona State';
UPDATE `team` SET `stadium` = 'Donald W. Reynolds Razorback Stadium' WHERE `name` = 'Arkansas';
UPDATE `team` SET `stadium` = 'Centennial Bank Stadium' WHERE `name` = 'Arkansas State';
UPDATE `team` SET `stadium` = 'Michie Stadium' WHERE `name` = 'Army';
UPDATE `team` SET `stadium` = 'Jordan-Hare Stadium' WHERE `name` = 'Auburn';
UPDATE `team` SET `stadium` = 'LaVell Edwards Stadium' WHERE `name` = 'BYU';
UPDATE `team` SET `stadium` = 'Scheumann Stadium' WHERE `name` = 'Ball State';
UPDATE `team` SET `stadium` = 'McLane Stadium' WHERE `name` = 'Baylor';
UPDATE `team` SET `stadium` = 'Albertsons Stadium' WHERE `name` = 'Boise State';
UPDATE `team` SET `stadium` = 'Doyt L. Perry Stadium' WHERE `name` = 'Bowling Green';
UPDATE `team` SET `stadium` = 'Broadview Stadium' WHERE `name` = 'Buffalo';
UPDATE `team` SET `stadium` = 'California Memorial Stadium' WHERE `name` = 'California';
UPDATE `team` SET `stadium` = 'Kelly/Shorts Stadium' WHERE `name` = 'Central Michigan';
UPDATE `team` SET `stadium` = 'Jerry Richardson Stadium' WHERE `name` = 'Charlotte';
UPDATE `team` SET `stadium` = 'Nippert Stadium' WHERE `name` = 'Cincinnati';
UPDATE `team` SET `stadium` = 'Memorial Stadium' WHERE `name` = 'Clemson';
UPDATE `team` SET `stadium` = 'Brooks Stadium' WHERE `name` = 'Coastal Carolina';
UPDATE `team` SET `stadium` = 'Folsom Field' WHERE `name` = 'Colorado';
UPDATE `team` SET `stadium` = 'Canvas Stadium' WHERE `name` = 'Colorado State';
UPDATE `team` SET `stadium` = 'Pratt & Whitney Stadium at Rentschler Field' WHERE `name` = 'Connecticut';
UPDATE `team` SET `stadium` = 'Wallace Wade Stadium' WHERE `name` = 'Duke';
UPDATE `team` SET `stadium` = 'Rynearson Stadium' WHERE `name` = 'Eastern Michigan';
UPDATE `team` SET `stadium` = 'Ben Hill Griffin Stadium' WHERE `name` = 'Florida';
UPDATE `team` SET `stadium` = 'Doak Campbell Stadium' WHERE `name` = 'Florida State';
UPDATE `team` SET `stadium` = 'Sanford Stadium' WHERE `name` = 'Georgia';
UPDATE `team` SET `stadium` = 'Allen E. Paulson Stadium' WHERE `name` = 'Georgia Southern';
UPDATE `team` SET `stadium` = 'Center Parc Stadium' WHERE `name` = 'Georgia State';
UPDATE `team` SET `stadium` = 'Bobby Dodd Stadium' WHERE `name` = 'Georgia Tech';
UPDATE `team` SET `stadium` = 'Clarence T.C. Ching Athletics Complex' WHERE `name` = 'Hawaii';
UPDATE `team` SET `stadium` = 'Gies Memorial Stadium' WHERE `name` = 'Illinois';
UPDATE `team` SET `stadium` = 'Memorial Stadium (Merchants Bank Field at Memorial Stadium)' WHERE `name` = 'Indiana';
UPDATE `team` SET `stadium` = 'Jack Trice Stadium' WHERE `name` = 'Iowa State';
UPDATE `team` SET `stadium` = 'David Booth Kansas Memorial Stadium' WHERE `name` = 'Kansas';
UPDATE `team` SET `stadium` = 'Bill Snyder Family Stadium' WHERE `name` = 'Kansas State';
UPDATE `team` SET `stadium` = 'Fifth Third Stadium' WHERE `name` = 'Kennesaw State';
UPDATE `team` SET `stadium` = 'Dix Stadium' WHERE `name` = 'Kent State';
UPDATE `team` SET `stadium` = 'Tiger Stadium' WHERE `name` = 'LSU';
UPDATE `team` SET `stadium` = 'Joe Aillet Stadium' WHERE `name` = 'Louisiana Tech';
UPDATE `team` SET `stadium` = 'L&N Federal Credit Union Stadium' WHERE `name` = 'Louisville';
UPDATE `team` SET `stadium` = 'SECU Stadium' WHERE `name` = 'Maryland';
UPDATE `team` SET `stadium` = 'Warren McGuirk Alumni Stadium' WHERE `name` = 'Massachusetts';
UPDATE `team` SET `stadium` = 'Hard Rock Stadium' WHERE `name` = 'Miami';
UPDATE `team` SET `stadium` = 'Yager Stadium' WHERE `name` = 'Miami, OH';
UPDATE `team` SET `stadium` = 'Michigan Stadium' WHERE `name` = 'Michigan';
UPDATE `team` SET `stadium` = 'Spartan Stadium' WHERE `name` = 'Michigan State';
UPDATE `team` SET `stadium` = 'Huntington Bank Stadium' WHERE `name` = 'Minnesota';
UPDATE `team` SET `stadium` = 'Navy-Marine Corps Memorial Stadium' WHERE `name` = 'Navy';
UPDATE `team` SET `stadium` = 'University Stadium' WHERE `name` = 'New Mexico';
UPDATE `team` SET `stadium` = 'Kenan Memorial Stadium' WHERE `name` = 'North Carolina';
UPDATE `team` SET `stadium` = 'DATCU Stadium' WHERE `name` = 'North Texas';
UPDATE `team` SET `stadium` = 'Huskie Stadium' WHERE `name` = 'Northern Illinois';
UPDATE `team` SET `stadium` = 'Ryan Field' WHERE `name` = 'Northwestern';
UPDATE `team` SET `stadium` = 'Notre Dame Stadium' WHERE `name` = 'Notre Dame';
UPDATE `team` SET `stadium` = 'Peden Stadium' WHERE `name` = 'Ohio';
UPDATE `team` SET `stadium` = 'Ohio Stadium' WHERE `name` = 'Ohio State';
UPDATE `team` SET `stadium` = 'Gaylord Family Oklahoma Memorial Stadium' WHERE `name` = 'Oklahoma';
UPDATE `team` SET `stadium` = 'Boone Pickens Stadium' WHERE `name` = 'Oklahoma State';
UPDATE `team` SET `stadium` = 'Autzen Stadium' WHERE `name` = 'Oregon';
UPDATE `team` SET `stadium` = 'Reser Stadium' WHERE `name` = 'Oregon State';
UPDATE `team` SET `stadium` = 'Beaver Stadium' WHERE `name` = 'Penn State';
UPDATE `team` SET `stadium` = 'Acrisure Stadium' WHERE `name` = 'Pittsburgh';
UPDATE `team` SET `stadium` = 'Ross-Ade Stadium' WHERE `name` = 'Purdue';
UPDATE `team` SET `stadium` = 'SHI Stadium' WHERE `name` = 'Rutgers';
UPDATE `team` SET `stadium` = 'Gerald J. Ford Stadium' WHERE `name` = 'SMU';
UPDATE `team` SET `stadium` = 'Bowers Stadium' WHERE `name` = 'Sam Houston State';
UPDATE `team` SET `stadium` = 'Snapdragon Stadium' WHERE `name` = 'San Diego State';
UPDATE `team` SET `stadium` = 'CEFCU Stadium' WHERE `name` = 'San Jose State';
UPDATE `team` SET `stadium` = 'Williams-Brice Stadium' WHERE `name` = 'South Carolina';
UPDATE `team` SET `stadium` = 'M.M. Roberts Stadium' WHERE `name` = 'Southern Miss';
UPDATE `team` SET `stadium` = 'Stanford Stadium' WHERE `name` = 'Stanford';
UPDATE `team` SET `stadium` = 'JMA Wireless Dome' WHERE `name` = 'Syracuse';
UPDATE `team` SET `stadium` = 'Amon G. Carter Stadium' WHERE `name` = 'TCU';
UPDATE `team` SET `stadium` = 'Neyland Stadium' WHERE `name` = 'Tennessee';
UPDATE `team` SET `stadium` = 'Darrell K Royal-Texas Memorial Stadium' WHERE `name` = 'Texas';
UPDATE `team` SET `stadium` = 'Kyle Field' WHERE `name` = 'Texas A&M';
UPDATE `team` SET `stadium` = 'Galaxy Stadium' WHERE `name` = 'Texas Tech';
UPDATE `team` SET `stadium` = 'Glass Bowl' WHERE `name` = 'Toledo';
UPDATE `team` SET `stadium` = 'Veterans Memorial Stadium' WHERE `name` = 'Troy';
UPDATE `team` SET `stadium` = 'Yulman Stadium' WHERE `name` = 'Tulane';
UPDATE `team` SET `stadium` = 'Rose Bowl' WHERE `name` = 'UCLA';
UPDATE `team` SET `stadium` = 'Allegiant Stadium' WHERE `name` = 'UNLV';
UPDATE `team` SET `stadium` = 'Los Angeles Memorial Coliseum' WHERE `name` = 'USC';
UPDATE `team` SET `stadium` = 'Raymond James Stadium' WHERE `name` = 'USF';
UPDATE `team` SET `stadium` = 'Alamodome' WHERE `name` = 'UTSA';
UPDATE `team` SET `stadium` = 'Rice-Eccles Stadium' WHERE `name` = 'Utah';
UPDATE `team` SET `stadium` = 'Merlin Olsen Field at Maverik Stadium' WHERE `name` = 'Utah State';
UPDATE `team` SET `stadium` = 'FirstBank Stadium' WHERE `name` = 'Vanderbilt';
UPDATE `team` SET `stadium` = 'Scott Stadium' WHERE `name` = 'Virginia';
UPDATE `team` SET `stadium` = 'Lane Stadium' WHERE `name` = 'Virginia Tech';
UPDATE `team` SET `stadium` = 'Allegacy Federal Credit Union Stadium' WHERE `name` = 'Wake Forest';
UPDATE `team` SET `stadium` = 'Alaska Airlines Field at Husky Stadium' WHERE `name` = 'Washington';
UPDATE `team` SET `stadium` = 'Gesa Field at Martin Stadium' WHERE `name` = 'Washington State';
UPDATE `team` SET `stadium` = 'Milan Puskar Stadium' WHERE `name` = 'West Virginia';
UPDATE `team` SET `stadium` = 'Waldo Stadium' WHERE `name` = 'Western Michigan';
UPDATE `team` SET `stadium` = 'Camp Randall Stadium' WHERE `name` = 'Wisconsin';
UPDATE `team` SET `stadium` = 'Jonah Field at War Memorial Stadium' WHERE `name` = 'Wyoming';
