-- Seed data only (no schema changes). Extends V18's coverage from 100 active FBS teams
-- to all 263 real teams (129 FCS + 34 currently-inactive FBS), plus 15 additional NFL
-- stadiums for the neutral-site venue catalogue. Sourced from per-team verified
-- individual page fetches, not a single bulk page scrape.

-- Disambiguate stadium names that collide across different real-world schools
-- (discovered once the full 263-team dataset was gathered; V18 only had 100 teams
-- so these collisions weren't visible yet). Uses the colloquial full name where one
-- exists (e.g. "Clemson Memorial Stadium", "Faurot Field at Memorial Stadium"),
-- otherwise a "Stadium (School)" suffix.
UPDATE `venue` SET `name` = 'Clemson Memorial Stadium' WHERE `name` = 'Memorial Stadium';
UPDATE `team` SET `stadium` = 'Clemson Memorial Stadium' WHERE `name` = 'Clemson';
UPDATE `venue` SET `name` = 'Boston College Alumni Stadium' WHERE `name` = 'Alumni Stadium';
UPDATE `team` SET `stadium` = 'Boston College Alumni Stadium' WHERE `name` = 'Boston College';

INSERT INTO `venue` (`name`, `city`, `state`, `capacity`) VALUES
  ('Anthony Field at Wildcat Stadium', 'Abilene', 'TX', 12000),
  ('Louis Crews Stadium', 'Normal', 'AL', 21000),
  ('ASU Stadium', 'Montgomery', 'AL', 26500),
  ('Bob Ford Field at Tom & Mary Casey Stadium', 'Albany', 'NY', 8500),
  ('Spinks-Casem Stadium', 'Lorman', 'MS', 22500),
  ('Simmons Bank Field', 'Pine Bluff', 'AR', 16000),
  ('Fortera Stadium', 'Clarksville', 'TN', 10000),
  ('Daytona Stadium', 'Daytona Beach', 'FL', 10000),
  ('Richard Gouse Field at Brown Stadium', 'Providence', 'RI', 20000),
  ('Beirne Stadium', 'Smithfield', 'RI', 5500),
  ('Christy Mathewson-Memorial Stadium', 'Lewisburg', 'PA', 13100),
  ('Bud and Jackie Sellick Bowl', 'Indianapolis', 'IN', 7500),
  ('Mustang Memorial Field at Alex G. Spanos Stadium', 'San Luis Obispo', 'CA', 11075),
  ('Barker-Lane Stadium', 'Buies Creek', 'NC', 5500),
  ('Estes Stadium', 'Conway', 'AR', 12000),
  ('Arute Field', 'New Britain', 'CT', 5500),
  ('Buccaneer Field', 'North Charleston', 'SC', 4000),
  ('Finley Stadium', 'Chattanooga', 'TN', 20668),
  ('Crown Field at Andy Kerr Stadium', 'Hamilton', 'NY', 10221),
  ('Robert K. Kraft Field at Lawrence A. Wien Stadium', 'New York', 'NY', 17000),
  ('Schoellkopf Field', 'Ithaca', 'NY', 25597),
  ('Memorial Field', 'Hanover', 'NH', 15600),
  ('Davidson College Stadium', 'Davidson', 'NC', 5000),
  ('Welcome Stadium', 'Dayton', 'OH', 11000),
  ('Delaware Stadium', 'Newark', 'DE', 18500),
  ('Delaware State Alumni Stadium', 'Dover', 'DE', 7193),
  ('Drake Stadium', 'Des Moines', 'IA', 14557),
  ('Arthur J. Rooney Athletic Field', 'Pittsburgh', 'PA', 2200),
  ('William B. Greene Jr. Stadium', 'Johnson City', 'TN', 7694),
  ('Ernest Hawkins Field at Memorial Stadium', 'Commerce', 'TX', 13500),
  ('O''Brien Field', 'Charleston', 'IL', 10000),
  ('Roy Kidd Stadium', 'Richmond', 'KY', 20000),
  ('Roos Field', 'Cheney', 'WA', 8600),
  ('Rhodes Stadium', 'Elon', 'NC', 14000),
  ('Bragg Memorial Stadium', 'Tallahassee', 'FL', 19633),
  ('Coffey Field', 'Bronx', 'NY', 7000),
  ('Paladin Stadium', 'Greenville', 'SC', 16000),
  ('Ernest W. Spangler Stadium', 'Boiling Springs', 'NC', 9000),
  ('Cooper Field', 'Washington', 'DC', 4418),
  ('Eddie Robinson Stadium', 'Grambling', 'LA', 19600),
  ('Armstrong Stadium', 'Hampton', 'VA', 12000),
  ('Harvard Stadium', 'Boston', 'MA', 25884),
  ('Fitton Field', 'Worcester', 'MA', 23500),
  ('Husky Stadium', 'Houston', 'TX', 5000),
  ('William H. Greene Stadium', 'Washington', 'DC', 10000),
  ('Kibbie Dome', 'Moscow', 'ID', 15250),
  ('ICCU Dome', 'Pocatello', 'ID', 12000),
  ('Hancock Stadium', 'Normal', 'IL', 13391),
  ('Gayle and Tom Benson Stadium', 'San Antonio', 'TX', 6000),
  ('Indiana State Memorial Stadium', 'Terre Haute', 'IN', 12764),
  ('Mississippi Veterans Memorial Stadium', 'Jackson', 'MS', 60492),
  ('Fisher Field at Fisher Stadium', 'Easton', 'PA', 13132),
  ('Provost Umphrey Stadium', 'Beaumont', 'TX', 16000),
  ('Goodman Stadium', 'Bethlehem', 'PA', 16000),
  ('Harlen C. Hunter Stadium', 'St. Charles', 'MO', 7450),
  ('Bethpage Federal Credit Union Stadium', 'Brookville', 'NY', 6000),
  ('Harold Alfond Sports Stadium', 'Orono', 'ME', 10000),
  ('Tenney Stadium at Leonidoff Field', 'Poughkeepsie', 'NY', 5000),
  ('Navarre Stadium', 'Lake Charles', 'LA', 17610),
  ('Five Star Stadium', 'Macon', 'GA', 10200),
  ('Duane Stadium', 'North Andover', 'MA', 4000),
  ('Rice-Totten Stadium', 'Itta Bena', 'MS', 10000),
  ('Robert W. Plaster Stadium', 'Springfield', 'MO', 17500),
  ('Kessler Field', 'West Long Branch', 'NJ', 4200),
  ('Washington-Grizzly Stadium', 'Missoula', 'MT', 25203),
  ('Bobcat Stadium (Montana State)', 'Bozeman', 'MT', 17777),
  ('Phil Simms Stadium', 'Morehead', 'KY', 10000),
  ('Hughes Stadium', 'Baltimore', 'MD', 10000),
  ('Roy Stewart Stadium', 'Murray', 'KY', 16800),
  ('Wildcat Stadium', 'Durham', 'NH', 11015),
  ('Ralph F. DellaCamera Stadium', 'West Haven', 'CT', 5000),
  ('Manning Field at John L. Guidry Stadium', 'Thibodaux', 'LA', 10500),
  ('William "Dick" Price Stadium', 'Norfolk', 'VA', 30000),
  ('Bobby Wallace Field at Bank Independent Stadium', 'Florence', 'AL', 10000),
  ('Truist Stadium', 'Greensboro', 'NC', 21500),
  ('O''Kelly-Riddick Stadium', 'Durham', 'NC', 10000),
  ('Alerus Center', 'Grand Forks', 'ND', 12283),
  ('Fargodome', 'Fargo', 'ND', 18700),
  ('Walkup Skydome', 'Flagstaff', 'AZ', 11230),
  ('Nottingham Field', 'Greeley', 'CO', 8533),
  ('UNI-Dome', 'Cedar Falls', 'IA', 16324),
  ('Harry Turpin Stadium', 'Natchitoches', 'LA', 15971),
  ('Franklin Field', 'Philadelphia', 'PA', 52593),
  ('Hillsboro Stadium', 'Hillsboro', 'OR', 7600),
  ('Panther Stadium at Blackshear Field', 'Prairie View', 'TX', 15000),
  ('Bailey Memorial Stadium', 'Clinton', 'SC', 6500),
  ('Powers Field at Princeton Stadium', 'Princeton', 'NJ', 27773),
  ('Meade Stadium', 'Kingston', 'RI', 6555),
  ('E. Claiborne Robins Stadium', 'Richmond', 'VA', 8217),
  ('Joe Walton Stadium', 'Moon Township', 'PA', 3000),
  ('Fred Anderson Field at Hornet Stadium', 'Sacramento', 'CA', 21195),
  ('Campus Field', 'Fairfield', 'CT', 3334),
  ('DeGol Field', 'Loretto', 'PA', 3500),
  ('Bobby Bowden Field at Pete Hanna Stadium', 'Homewood', 'AL', 6700),
  ('Torero Stadium', 'San Diego', 'CA', 6000),
  ('Oliver C. Dawson Stadium', 'Orangeburg', 'SC', 22000),
  ('DakotaDome', 'Vermillion', 'SD', 9100),
  ('Dana J. Dykhouse Stadium', 'Brookings', 'SD', 19340),
  ('Houck Stadium', 'Cape Girardeau', 'MO', 11015),
  ('Strawberry Stadium', 'Hammond', 'LA', 7408),
  ('A. W. Mumford Stadium', 'Baton Rouge', 'LA', 29000),
  ('Saluki Stadium', 'Carbondale', 'IL', 15000),
  ('Eccles Coliseum', 'Cedar City', 'UT', 8500),
  ('O''Shaughnessy Stadium', 'Saint Paul', 'MN', 5025),
  ('Homer Bryce Stadium', 'Nacogdoches', 'TX', 14575),
  ('Spec Martin Stadium', 'DeLand', 'FL', 6000),
  ('W.B. Mason Stadium', 'Easton', 'MA', 2400),
  ('Kenneth P. LaValle Stadium', 'Stony Brook', 'NY', 12300),
  ('Tarleton State Memorial Stadium', 'Stephenville', 'TX', 24000),
  ('Tucker Stadium', 'Cookeville', 'TN', 16500),
  ('Shell Energy Stadium', 'Houston', 'TX', 22000),
  ('Johnson Hagood Stadium', 'Charleston', 'SC', 11427),
  ('Johnny Unitas Stadium', 'Towson', 'MD', 11198),
  ('UC Davis Health Stadium', 'Davis', 'CA', 10743),
  ('Graham Stadium', 'Martin', 'TN', 7500),
  ('Robert and Janet Vackar Stadium', 'Edinburg', 'TX', 13500),
  ('Greater Zion Stadium', 'St. George', 'UT', 10000),
  ('Alumni Memorial Field at Foster Stadium', 'Lexington', 'VA', 10000),
  ('Brown Field', 'Valparaiso', 'IN', 5000),
  ('Villanova Stadium', 'Villanova', 'PA', 12000),
  ('Wagner College Stadium (Hameline Field)', 'Staten Island', 'NY', 4000),
  ('Stewart Stadium', 'Ogden', 'UT', 17312),
  ('Bob Waters Field at E.J. Whitmire Stadium', 'Cullowhee', 'NC', 13742),
  ('Hanson Field', 'Macomb', 'IL', 16368),
  ('Zable Stadium (Walter J. Zable Stadium at Cary Field)', 'Williamsburg', 'VA', 12672),
  ('Gibbs Stadium', 'Spartanburg', 'SC', 13000),
  ('Yale Bowl', 'New Haven', 'CT', 61446),
  ('Stambaugh Stadium', 'Youngstown', 'OH', 20630),
  ('Kidd Brewer Stadium', 'Boone', 'NC', 30000),
  ('Dowdy-Ficklen Stadium', 'Greenville', 'NC', 50000),
  ('Flagler Credit Union Stadium', 'Boca Raton', 'FL', 29571),
  ('Pitbull Stadium', 'Miami', 'FL', 20000),
  ('Valley Children''s Stadium', 'Fresno', 'CA', 40727),
  ('TDECU Stadium', 'Houston', 'TX', 40000),
  ('Nile Kinnick Stadium', 'Iowa City', 'IA', 69250),
  ('AmFirst Stadium', 'Jacksonville', 'AL', 22500),
  ('Bridgeforth Stadium', 'Harrisonburg', 'VA', 24877),
  ('Kroger Field', 'Lexington', 'KY', 61000),
  ('Williams Stadium', 'Lynchburg', 'VA', 25000),
  ('Cajun Field', 'Lafayette', 'LA', 41426),
  ('Malone Stadium', 'Monroe', 'LA', 30427),
  ('Joan C. Edwards Stadium', 'Huntington', 'WV', 30475),
  ('Simmons Bank Liberty Stadium', 'Memphis', 'TN', 58325),
  ('Johnny "Red" Floyd Stadium', 'Murfreesboro', 'TN', 27303),
  ('Davis Wade Stadium', 'Starkville', 'MS', 60311),
  ('Faurot Field at Memorial Stadium', 'Columbia', 'MO', 62622),
  ('Carter-Finley Stadium', 'Raleigh', 'NC', 56919),
  ('Tom Osborne Field at Memorial Stadium', 'Lincoln', 'NE', 91459),
  ('Mackay Stadium', 'Reno', 'NV', 27000),
  ('Aggie Memorial Stadium', 'Las Cruces', 'NM', 30343),
  ('S.B. Ballard Stadium', 'Norfolk', 'VA', 21944),
  ('Vaught-Hemingway Stadium', 'University', 'MS', 64038),
  ('Rice Stadium', 'Houston', 'TX', 47000),
  ('Hancock Whitney Stadium', 'Mobile', 'AL', 25450),
  ('Lincoln Financial Field', 'Philadelphia', 'PA', 68532),
  ('Bobcat Stadium (Texas State)', 'San Marcos', 'TX', 30000),
  ('Skelly Field at H.A. Chapman Stadium', 'Tulsa', 'OK', 30000),
  ('Protective Stadium', 'Birmingham', 'AL', 47100),
  ('Acrisure Bounce House', 'Orlando', 'FL', 45906),
  ('Sun Bowl', 'El Paso', 'TX', 51500),
  ('Houchens Industries-L.T. Smith Stadium', 'Bowling Green', 'KY', 22000),
  ('M&T Bank Stadium', 'Baltimore', 'MD', 70745),
  ('Highmark Stadium', 'Orchard Park', 'NY', 60108),
  ('Soldier Field', 'Chicago', 'IL', 62500),
  ('Paycor Stadium', 'Cincinnati', 'OH', 65515),
  ('Huntington Bank Field', 'Cleveland', 'OH', 67431),
  ('Empower Field at Mile High', 'Denver', 'CO', 76125),
  ('Lambeau Field', 'Green Bay', 'WI', 81441),
  ('GEHA Field at Arrowhead Stadium', 'Kansas City', 'MO', 76416),
  ('U.S. Bank Stadium', 'Minneapolis', 'MN', 66202),
  ('Gillette Stadium', 'Foxborough', 'MA', 64628),
  ('MetLife Stadium', 'East Rutherford', 'NJ', 82500),
  ('Levi''s Stadium', 'Santa Clara', 'CA', 68500),
  ('Lumen Field', 'Seattle', 'WA', 68740),
  ('Northwest Stadium', 'Landover', 'MD', 64000)
ON DUPLICATE KEY UPDATE city = VALUES(city), state = VALUES(state), capacity = VALUES(capacity);

UPDATE `team` SET `stadium` = 'Anthony Field at Wildcat Stadium' WHERE `name` = 'Abilene Christian';
UPDATE `team` SET `stadium` = 'Louis Crews Stadium' WHERE `name` = 'Alabama A&M';
UPDATE `team` SET `stadium` = 'ASU Stadium' WHERE `name` = 'Alabama State';
UPDATE `team` SET `stadium` = 'Bob Ford Field at Tom & Mary Casey Stadium' WHERE `name` = 'Albany';
UPDATE `team` SET `stadium` = 'Spinks-Casem Stadium' WHERE `name` = 'Alcorn State';
UPDATE `team` SET `stadium` = 'Simmons Bank Field' WHERE `name` = 'Arkansas-Pine Bluff';
UPDATE `team` SET `stadium` = 'Fortera Stadium' WHERE `name` = 'Austin Peay';
UPDATE `team` SET `stadium` = 'Daytona Stadium' WHERE `name` = 'Bethune-Cookman';
UPDATE `team` SET `stadium` = 'Richard Gouse Field at Brown Stadium' WHERE `name` = 'Brown';
UPDATE `team` SET `stadium` = 'Beirne Stadium' WHERE `name` = 'Bryant';
UPDATE `team` SET `stadium` = 'Christy Mathewson-Memorial Stadium' WHERE `name` = 'Bucknell';
UPDATE `team` SET `stadium` = 'Bud and Jackie Sellick Bowl' WHERE `name` = 'Butler';
UPDATE `team` SET `stadium` = 'Mustang Memorial Field at Alex G. Spanos Stadium' WHERE `name` = 'Cal Poly';
UPDATE `team` SET `stadium` = 'Barker-Lane Stadium' WHERE `name` = 'Campbell';
UPDATE `team` SET `stadium` = 'Estes Stadium' WHERE `name` = 'Central Arkansas';
UPDATE `team` SET `stadium` = 'Arute Field' WHERE `name` = 'Central Connecticut';
UPDATE `team` SET `stadium` = 'Buccaneer Field' WHERE `name` = 'Charleston Southern';
UPDATE `team` SET `stadium` = 'Finley Stadium' WHERE `name` = 'Chattanooga';
UPDATE `team` SET `stadium` = 'Crown Field at Andy Kerr Stadium' WHERE `name` = 'Colgate';
UPDATE `team` SET `stadium` = 'Robert K. Kraft Field at Lawrence A. Wien Stadium' WHERE `name` = 'Columbia';
UPDATE `team` SET `stadium` = 'Schoellkopf Field' WHERE `name` = 'Cornell';
UPDATE `team` SET `stadium` = 'Memorial Field' WHERE `name` = 'Dartmouth';
UPDATE `team` SET `stadium` = 'Davidson College Stadium' WHERE `name` = 'Davidson';
UPDATE `team` SET `stadium` = 'Welcome Stadium' WHERE `name` = 'Dayton';
UPDATE `team` SET `stadium` = 'Delaware Stadium' WHERE `name` = 'Delaware';
UPDATE `team` SET `stadium` = 'Delaware State Alumni Stadium' WHERE `name` = 'Delaware State';
UPDATE `team` SET `stadium` = 'Drake Stadium' WHERE `name` = 'Drake';
UPDATE `team` SET `stadium` = 'Arthur J. Rooney Athletic Field' WHERE `name` = 'Duquesne';
UPDATE `team` SET `stadium` = 'William B. Greene Jr. Stadium' WHERE `name` = 'East Tennessee State';
UPDATE `team` SET `stadium` = 'Ernest Hawkins Field at Memorial Stadium' WHERE `name` = 'East Texas A&M';
UPDATE `team` SET `stadium` = 'O''Brien Field' WHERE `name` = 'Eastern Illinois';
UPDATE `team` SET `stadium` = 'Roy Kidd Stadium' WHERE `name` = 'Eastern Kentucky';
UPDATE `team` SET `stadium` = 'Roos Field' WHERE `name` = 'Eastern Washington';
UPDATE `team` SET `stadium` = 'Rhodes Stadium' WHERE `name` = 'Elon';
UPDATE `team` SET `stadium` = 'Bragg Memorial Stadium' WHERE `name` = 'Florida A&M';
UPDATE `team` SET `stadium` = 'Coffey Field' WHERE `name` = 'Fordham';
UPDATE `team` SET `stadium` = 'Paladin Stadium' WHERE `name` = 'Furman';
UPDATE `team` SET `stadium` = 'Ernest W. Spangler Stadium' WHERE `name` = 'Gardner-Webb';
UPDATE `team` SET `stadium` = 'Cooper Field' WHERE `name` = 'Georgetown';
UPDATE `team` SET `stadium` = 'Eddie Robinson Stadium' WHERE `name` = 'Grambling State';
UPDATE `team` SET `stadium` = 'Armstrong Stadium' WHERE `name` = 'Hampton';
UPDATE `team` SET `stadium` = 'Harvard Stadium' WHERE `name` = 'Harvard';
UPDATE `team` SET `stadium` = 'Fitton Field' WHERE `name` = 'Holy Cross';
UPDATE `team` SET `stadium` = 'Husky Stadium' WHERE `name` = 'Houston Christian';
UPDATE `team` SET `stadium` = 'William H. Greene Stadium' WHERE `name` = 'Howard';
UPDATE `team` SET `stadium` = 'Kibbie Dome' WHERE `name` = 'Idaho';
UPDATE `team` SET `stadium` = 'ICCU Dome' WHERE `name` = 'Idaho State';
UPDATE `team` SET `stadium` = 'Hancock Stadium' WHERE `name` = 'Illinois State';
UPDATE `team` SET `stadium` = 'Gayle and Tom Benson Stadium' WHERE `name` = 'Incarnate Word';
UPDATE `team` SET `stadium` = 'Indiana State Memorial Stadium' WHERE `name` = 'Indiana State';
UPDATE `team` SET `stadium` = 'Mississippi Veterans Memorial Stadium' WHERE `name` = 'Jackson State';
UPDATE `team` SET `stadium` = 'Fisher Field at Fisher Stadium' WHERE `name` = 'Lafayette';
UPDATE `team` SET `stadium` = 'Provost Umphrey Stadium' WHERE `name` = 'Lamar';
UPDATE `team` SET `stadium` = 'Goodman Stadium' WHERE `name` = 'Lehigh';
UPDATE `team` SET `stadium` = 'Harlen C. Hunter Stadium' WHERE `name` = 'Lindenwood';
UPDATE `team` SET `stadium` = 'Bethpage Federal Credit Union Stadium' WHERE `name` = 'Long Island';
UPDATE `team` SET `stadium` = 'Harold Alfond Sports Stadium' WHERE `name` = 'Maine';
UPDATE `team` SET `stadium` = 'Tenney Stadium at Leonidoff Field' WHERE `name` = 'Marist';
UPDATE `team` SET `stadium` = 'Navarre Stadium' WHERE `name` = 'McNeese State';
UPDATE `team` SET `stadium` = 'Five Star Stadium' WHERE `name` = 'Mercer';
UPDATE `team` SET `stadium` = 'Duane Stadium' WHERE `name` = 'Merrimack';
UPDATE `team` SET `stadium` = 'Rice-Totten Stadium' WHERE `name` = 'Mississippi Valley State';
UPDATE `team` SET `stadium` = 'Robert W. Plaster Stadium' WHERE `name` = 'Missouri State';
UPDATE `team` SET `stadium` = 'Kessler Field' WHERE `name` = 'Monmouth';
UPDATE `team` SET `stadium` = 'Washington-Grizzly Stadium' WHERE `name` = 'Montana';
UPDATE `team` SET `stadium` = 'Bobcat Stadium (Montana State)' WHERE `name` = 'Montana State';
UPDATE `team` SET `stadium` = 'Phil Simms Stadium' WHERE `name` = 'Morehead State';
UPDATE `team` SET `stadium` = 'Hughes Stadium' WHERE `name` = 'Morgan State';
UPDATE `team` SET `stadium` = 'Roy Stewart Stadium' WHERE `name` = 'Murray State';
UPDATE `team` SET `stadium` = 'Wildcat Stadium' WHERE `name` = 'New Hampshire';
UPDATE `team` SET `stadium` = 'Ralph F. DellaCamera Stadium' WHERE `name` = 'New Haven';
UPDATE `team` SET `stadium` = 'Manning Field at John L. Guidry Stadium' WHERE `name` = 'Nicholls State';
UPDATE `team` SET `stadium` = 'William "Dick" Price Stadium' WHERE `name` = 'Norfolk State';
UPDATE `team` SET `stadium` = 'Bobby Wallace Field at Bank Independent Stadium' WHERE `name` = 'North Alabama';
UPDATE `team` SET `stadium` = 'Truist Stadium' WHERE `name` = 'North Carolina A&T';
UPDATE `team` SET `stadium` = 'O''Kelly-Riddick Stadium' WHERE `name` = 'North Carolina Central';
UPDATE `team` SET `stadium` = 'Alerus Center' WHERE `name` = 'North Dakota';
UPDATE `team` SET `stadium` = 'Fargodome' WHERE `name` = 'North Dakota State';
UPDATE `team` SET `stadium` = 'Walkup Skydome' WHERE `name` = 'Northern Arizona';
UPDATE `team` SET `stadium` = 'Nottingham Field' WHERE `name` = 'Northern Colorado';
UPDATE `team` SET `stadium` = 'UNI-Dome' WHERE `name` = 'Northern Iowa';
UPDATE `team` SET `stadium` = 'Harry Turpin Stadium' WHERE `name` = 'Northwestern State';
UPDATE `team` SET `stadium` = 'Franklin Field' WHERE `name` = 'Penn';
UPDATE `team` SET `stadium` = 'Hillsboro Stadium' WHERE `name` = 'Portland State';
UPDATE `team` SET `stadium` = 'Panther Stadium at Blackshear Field' WHERE `name` = 'Prairie View A&M';
UPDATE `team` SET `stadium` = 'Bailey Memorial Stadium' WHERE `name` = 'Presbyterian';
UPDATE `team` SET `stadium` = 'Powers Field at Princeton Stadium' WHERE `name` = 'Princeton';
UPDATE `team` SET `stadium` = 'Meade Stadium' WHERE `name` = 'Rhode Island';
UPDATE `team` SET `stadium` = 'E. Claiborne Robins Stadium' WHERE `name` = 'Richmond';
UPDATE `team` SET `stadium` = 'Joe Walton Stadium' WHERE `name` = 'Robert Morris';
UPDATE `team` SET `stadium` = 'Fred Anderson Field at Hornet Stadium' WHERE `name` = 'Sacramento State';
UPDATE `team` SET `stadium` = 'Campus Field' WHERE `name` = 'Sacred Heart';
UPDATE `team` SET `stadium` = 'DeGol Field' WHERE `name` = 'Saint Francis';
UPDATE `team` SET `stadium` = 'Bobby Bowden Field at Pete Hanna Stadium' WHERE `name` = 'Samford';
UPDATE `team` SET `stadium` = 'Torero Stadium' WHERE `name` = 'San Diego';
UPDATE `team` SET `stadium` = 'Oliver C. Dawson Stadium' WHERE `name` = 'South Carolina State';
UPDATE `team` SET `stadium` = 'DakotaDome' WHERE `name` = 'South Dakota';
UPDATE `team` SET `stadium` = 'Dana J. Dykhouse Stadium' WHERE `name` = 'South Dakota State';
UPDATE `team` SET `stadium` = 'Houck Stadium' WHERE `name` = 'Southeast Missouri';
UPDATE `team` SET `stadium` = 'Strawberry Stadium' WHERE `name` = 'Southeastern Louisiana';
UPDATE `team` SET `stadium` = 'A. W. Mumford Stadium' WHERE `name` = 'Southern';
UPDATE `team` SET `stadium` = 'Saluki Stadium' WHERE `name` = 'Southern Illinois';
UPDATE `team` SET `stadium` = 'Eccles Coliseum' WHERE `name` = 'Southern Utah';
UPDATE `team` SET `stadium` = 'O''Shaughnessy Stadium' WHERE `name` = 'St. Thomas';
UPDATE `team` SET `stadium` = 'Homer Bryce Stadium' WHERE `name` = 'Stephen F. Austin';
UPDATE `team` SET `stadium` = 'Spec Martin Stadium' WHERE `name` = 'Stetson';
UPDATE `team` SET `stadium` = 'W.B. Mason Stadium' WHERE `name` = 'Stonehill';
UPDATE `team` SET `stadium` = 'Kenneth P. LaValle Stadium' WHERE `name` = 'Stony Brook';
UPDATE `team` SET `stadium` = 'Tarleton State Memorial Stadium' WHERE `name` = 'Tarleton State';
UPDATE `team` SET `stadium` = 'Nissan Stadium' WHERE `name` = 'Tennessee State';
UPDATE `team` SET `stadium` = 'Tucker Stadium' WHERE `name` = 'Tennessee Tech';
UPDATE `team` SET `stadium` = 'Shell Energy Stadium' WHERE `name` = 'Texas Southern';
UPDATE `team` SET `stadium` = 'Johnson Hagood Stadium' WHERE `name` = 'The Citadel';
UPDATE `team` SET `stadium` = 'Johnny Unitas Stadium' WHERE `name` = 'Towson';
UPDATE `team` SET `stadium` = 'UC Davis Health Stadium' WHERE `name` = 'UC Davis';
UPDATE `team` SET `stadium` = 'Graham Stadium' WHERE `name` = 'UT Martin';
UPDATE `team` SET `stadium` = 'Robert and Janet Vackar Stadium' WHERE `name` = 'UTRGV';
UPDATE `team` SET `stadium` = 'Greater Zion Stadium' WHERE `name` = 'Utah Tech';
UPDATE `team` SET `stadium` = 'Alumni Memorial Field at Foster Stadium' WHERE `name` = 'VMI';
UPDATE `team` SET `stadium` = 'Brown Field' WHERE `name` = 'Valparaiso';
UPDATE `team` SET `stadium` = 'Villanova Stadium' WHERE `name` = 'Villanova';
UPDATE `team` SET `stadium` = 'Wagner College Stadium (Hameline Field)' WHERE `name` = 'Wagner';
UPDATE `team` SET `stadium` = 'Stewart Stadium' WHERE `name` = 'Weber State';
UPDATE `team` SET `stadium` = 'Bob Waters Field at E.J. Whitmire Stadium' WHERE `name` = 'Western Carolina';
UPDATE `team` SET `stadium` = 'Hanson Field' WHERE `name` = 'Western Illinois';
UPDATE `team` SET `stadium` = 'Zable Stadium (Walter J. Zable Stadium at Cary Field)' WHERE `name` = 'William & Mary';
UPDATE `team` SET `stadium` = 'Gibbs Stadium' WHERE `name` = 'Wofford';
UPDATE `team` SET `stadium` = 'Yale Bowl' WHERE `name` = 'Yale';
UPDATE `team` SET `stadium` = 'Stambaugh Stadium' WHERE `name` = 'Youngstown State';
UPDATE `team` SET `stadium` = 'Kidd Brewer Stadium' WHERE `name` = 'Appalachian State';
UPDATE `team` SET `stadium` = 'Boston College Alumni Stadium' WHERE `name` = 'Boston College';
UPDATE `team` SET `stadium` = 'Dowdy-Ficklen Stadium' WHERE `name` = 'East Carolina';
UPDATE `team` SET `stadium` = 'Flagler Credit Union Stadium' WHERE `name` = 'FAU';
UPDATE `team` SET `stadium` = 'Pitbull Stadium' WHERE `name` = 'FIU';
UPDATE `team` SET `stadium` = 'Valley Children''s Stadium' WHERE `name` = 'Fresno State';
UPDATE `team` SET `stadium` = 'TDECU Stadium' WHERE `name` = 'Houston';
UPDATE `team` SET `stadium` = 'Nile Kinnick Stadium' WHERE `name` = 'Iowa';
UPDATE `team` SET `stadium` = 'AmFirst Stadium' WHERE `name` = 'Jacksonville State';
UPDATE `team` SET `stadium` = 'Bridgeforth Stadium' WHERE `name` = 'James Madison';
UPDATE `team` SET `stadium` = 'Kroger Field' WHERE `name` = 'Kentucky';
UPDATE `team` SET `stadium` = 'Williams Stadium' WHERE `name` = 'Liberty';
UPDATE `team` SET `stadium` = 'Cajun Field' WHERE `name` = 'Louisiana';
UPDATE `team` SET `stadium` = 'Malone Stadium' WHERE `name` = 'Louisiana-Monroe';
UPDATE `team` SET `stadium` = 'Joan C. Edwards Stadium' WHERE `name` = 'Marshall';
UPDATE `team` SET `stadium` = 'Simmons Bank Liberty Stadium' WHERE `name` = 'Memphis';
UPDATE `team` SET `stadium` = 'Johnny "Red" Floyd Stadium' WHERE `name` = 'Middle Tennessee';
UPDATE `team` SET `stadium` = 'Davis Wade Stadium' WHERE `name` = 'Mississippi State';
UPDATE `team` SET `stadium` = 'Faurot Field at Memorial Stadium' WHERE `name` = 'Missouri';
UPDATE `team` SET `stadium` = 'Carter-Finley Stadium' WHERE `name` = 'NC State';
UPDATE `team` SET `stadium` = 'Tom Osborne Field at Memorial Stadium' WHERE `name` = 'Nebraska';
UPDATE `team` SET `stadium` = 'Mackay Stadium' WHERE `name` = 'Nevada';
UPDATE `team` SET `stadium` = 'Aggie Memorial Stadium' WHERE `name` = 'New Mexico State';
UPDATE `team` SET `stadium` = 'S.B. Ballard Stadium' WHERE `name` = 'Old Dominion';
UPDATE `team` SET `stadium` = 'Vaught-Hemingway Stadium' WHERE `name` = 'Ole Miss';
UPDATE `team` SET `stadium` = 'Rice Stadium' WHERE `name` = 'Rice';
UPDATE `team` SET `stadium` = 'Hancock Whitney Stadium' WHERE `name` = 'South Alabama';
UPDATE `team` SET `stadium` = 'Lincoln Financial Field' WHERE `name` = 'Temple';
UPDATE `team` SET `stadium` = 'Bobcat Stadium (Texas State)' WHERE `name` = 'Texas State';
UPDATE `team` SET `stadium` = 'Skelly Field at H.A. Chapman Stadium' WHERE `name` = 'Tulsa';
UPDATE `team` SET `stadium` = 'Protective Stadium' WHERE `name` = 'UAB';
UPDATE `team` SET `stadium` = 'Acrisure Bounce House' WHERE `name` = 'UCF';
UPDATE `team` SET `stadium` = 'Sun Bowl' WHERE `name` = 'UTEP';
UPDATE `team` SET `stadium` = 'Houchens Industries-L.T. Smith Stadium' WHERE `name` = 'Western Kentucky';
