-- 09. Кто из стримеров за какую команду выступает сейчас.
-- Только действующие составы; стримеры без player_id не выводятся.
--
-- Колонки: streamer, platform, player, team, tag, followers
-- Порядок: followers по убыванию, при равенстве streamer по возрастанию
-- Строк:   16

select s.nickname streamer, p.name platform, pl.nickname player, t.name team, t.tag, followers
from streamers s
left join team_members tm on s.player_id=tm.player_id and tm.left_at is null
left join teams t on tm.team_id=t.team_id
join platforms p on p.platform_id=s.platform_id
join players pl on pl.player_id=s.player_id
order by followers desc, streamer asc
limit 16
