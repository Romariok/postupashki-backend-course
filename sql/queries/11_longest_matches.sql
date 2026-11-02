-- 11. Самые затяжные матчи и турниры, на которых они сыграны.
-- duration_sec >= 4700 включительно; матчи вне турнира тоже выводятся, у них
-- tournament_name пуст; match_date — дата started_at по UTC.
--
-- Колонки: match_id, match_date, duration_sec, tournament_name
-- Порядок: duration_sec по убыванию, при равенстве match_id по возрастанию
-- Строк:   56

select match_id, m.started_at::date match_date,
duration_sec, t.name tournament_name
from matches m
left join tournaments t on t.tournament_id=m.tournament_id
where duration_sec >=4700
order by duration_sec desc, match_id asc
limit 56