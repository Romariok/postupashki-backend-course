-- 08. Сколько в команде игроков и сколько среди них стримеров.
-- Считаются только незакрытые периоды; команда без состава остаётся
-- в ответе с нулями, стримеры без player_id не учитываются.
--
-- Колонки: name, tag, players_now, streamers_now
-- Порядок: players_now по убыванию, при равенстве name по возрастанию
-- Строк:   24

select
name, tag, count(tm.player_id) players_now, count(s.streamer_id) streamers_now 
from teams t
left join team_members tm on t.team_id=tm.team_id and tm.left_at is NULL
left join streamers s on s.player_id=tm.player_id
group by tag, name
order by players_now desc, name asc
limit 24