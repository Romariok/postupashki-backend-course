-- 19. Стримеры, которые ни разу не брали Pudge.
-- Только стримеры с непустым player_id и без единой игры за 'Pudge'; team — тег
-- команды с left_at IS NULL, иначе пусто; games — все строки match_players игрока.
--
-- Колонки: streamer, platform, team, followers, games
-- Порядок: followers по убыванию, при равенстве streamer по возрастанию
-- Строк:   8

select s.nickname streamer, p.name platform, t.tag team, s.followers, count(mp.match_id) games
from streamers s
join platforms p on p.platform_id=s.platform_id
left join team_members tm on tm.player_id=s.player_id and tm.left_at is null
left join teams t on t.team_id=tm.team_id
left join match_players mp on mp.player_id=s.player_id
where s.player_id is not null
and not exists (
    select 1 from match_players mp2
    join heroes h on h.hero_id=mp2.hero_id
    where mp2.player_id=s.player_id and h.name = 'Pudge'
)
group by s.nickname, p.name, t.tag, s.followers
order by followers desc, streamer asc
limit 8

