-- 24. Турниры, чей призовой фонд больше суммы призовых всех предыдущих.
-- Предыдущие — со строго меньшим started_at, сравнение строгое; у первого турнира
-- prev_total и prev_count нули, не NULL; matches_played — число матчей турнира.
--
-- Колонки: tournament, started_at, prize_pool, prev_total, prev_count, matches_played
-- Порядок: started_at по возрастанию
-- Строк:   2

select t.name tournament, t.started_at, t.prize_pool, prev.total prev_total, prev.cnt prev_count,
(select count(*) from matches m where m.tournament_id=t.tournament_id) matches_played
from tournaments t
cross join lateral (
    select coalesce(sum(o.prize_pool), 0) total, count(*) cnt
    from tournaments o
    where o.started_at < t.started_at
) prev
where t.prize_pool > prev.total
order by t.started_at asc
limit 2

