-- 13. Сколько матчей сыграно в каждом году.
-- Год из started_at; tournament_matches — матчи с непустым tournament_id, ноль допустим.
--
-- Колонки: year, matches, tournament_matches
-- Порядок: year по возрастанию
-- Строк:   10

select extract(year from started_at) as year, count(match_id) matches, count(tournament_id) tournament_matches
from matches m
group by year
order by year asc
limit 10