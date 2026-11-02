-- 04. Матчи 2024 года, продлившиеся дольше часа.
-- duration_sec строго больше 3600; год начала — 2024 по UTC,
-- started_at выводится как лежит в таблице, без приведения к дате.
--
-- Колонки: match_id, started_at, duration_sec, winner
-- Порядок: started_at по возрастанию
-- Строк:   78

select match_id, started_at, duration_sec, winner from matches
where duration_sec > 3600 and started_at > '2024-01-01 00:00:00+00'
order by started_at asc limit 78