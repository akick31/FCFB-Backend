UPDATE `game_writeup`
SET `message` = '{kicking_team} fails to recover the onside kick.'
WHERE `id` = 803
  AND `scenario` = 'FAILED_ONSIDE'
  AND `play_call` = 'KICKOFF_ONSIDE'
  AND `message` = '{receiving_team} fails to recover the onside kick.';

UPDATE `game_writeup`
SET `message` = 'The kickoff returner coughs it up and {kicking_team} scoops it up and walks into the end zone for a touchdown!'
WHERE `id` = 531
  AND `scenario` = 'TOUCHDOWN'
  AND `play_call` = 'KICKOFF_NORMAL'
  AND `message` = 'The kickoff returner bursts through the coverage and races downfield untouched for a touchdown!';
