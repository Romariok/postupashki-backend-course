-- 17. Доля побед светлой стороны по патчам.
-- Патч без единого матча не выводится; radiant_winrate — процент,
-- round(100.0 * radiant_wins / matches, 2).
--
-- Колонки: version, matches, radiant_wins, radiant_winrate
-- Порядок: radiant_winrate (округлённое) по убыванию, при равенстве version по возрастанию
-- Строк:   12

select version, count(m.match_id) matches, count(m.winner) radiant_wins, round(100.0 * count(m.winner) /count(m.match_id), 2) radiant_winrate
from patches p
join (select match_id, patch_id, nullif(winner, 'dire') winner from matches) m on p.patch_id=m.patch_id
group by version
order by radiant_winrate desc, version asc
limit 12