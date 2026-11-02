-- 22. Стримеры с подписчиками выше среднего по своей площадке.
-- Строго больше среднего по всем стримерам площадки, сам стример в среднее входит;
-- platform_avg до 2 знаков; team — тег действующей команды, иначе NULL.
--
-- Колонки: streamer, platform, followers, platform_avg, team
-- Порядок: followers по убыванию, при равенстве streamer по возрастанию
-- Строк:   16

select s.nickname streamer, p.name platform, s.followers, round(a.avg_followers, 2) platform_avg, t.tag team
from streamers s
join platforms p on p.platform_id=s.platform_id
join (select platform_id, avg(followers) avg_followers from streamers group by platform_id) a
on a.platform_id=s.platform_id
left join team_members tm on tm.player_id=s.player_id and tm.left_at is null
left join teams t on t.team_id=tm.team_id
where s.followers > a.avg_followers
order by followers desc, streamer asc
limit 16

