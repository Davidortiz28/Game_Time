ALTER TABLE game_launches ADD COLUMN process_name VARCHAR(260);

UPDATE game_launches l
SET process_name = g.process_name
FROM games g
WHERE l.game_id = g.id AND l.process_name IS NULL;
