-- 07. Действующие составы: кто сейчас за кого играет.
-- Только незакрытые периоды (left_at IS NULL); игроки без команды не выводятся.
--
-- Колонки: nickname, country, mmr, team_name, joined_at
-- Порядок: joined_at по возрастанию, при равенстве nickname по возрастанию
-- Строк:   86

select p.nickname, p.country, p.mmr, t.name as team_name, tm.joined_at from players as p
join team_members as tm on p.player_id = tm.player_id
join teams as t on t.team_id = tm.team_id 
where tm.left_at is null
order by tm.joined_at asc, p.nickname asc
limit 86