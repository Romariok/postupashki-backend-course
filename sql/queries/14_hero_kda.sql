-- 14. Средние убийства, смерти и помощь у ходовых героев.
-- Только герои с games >= 420; средние — round(avg(x), 2) по numeric, два знака.
--
-- Колонки: hero, games, avg_kills, avg_deaths, avg_assists
-- Порядок: avg_kills (округлённое) по убыванию, при равенстве hero по возрастанию
-- Строк:   29

select h.name hero, count(mp.match_id) games,
round(avg(mp.kills), 2) avg_kills, round(avg(mp.deaths), 2) avg_deaths, round(avg(mp.assists), 2) avg_assists
from heroes h
join match_players mp on h.hero_id=mp.hero_id
group by hero
having count(mp.match_id) >=420
order by avg_kills desc, hero ASC
limit 29