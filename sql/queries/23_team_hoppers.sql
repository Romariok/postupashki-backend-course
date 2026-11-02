-- 23. Игроки, поигравшие больше чем за одну команду.
-- Считаются различные team_id по всем периодам, строго больше одного; current_team —
-- название команды с пустым left_at, current_team и streamer могут быть NULL.
--
-- Колонки: player, country, teams_count, current_team, streamer
-- Порядок: teams_count по убыванию, при равенстве player по возрастанию
-- Строк:   25

select p.nickname player, p.country, c.teams_count, t.name current_team, s.nickname streamer
from (
    select player_id, count(distinct team_id) teams_count
    from team_members
    group by player_id
    having count(distinct team_id) > 1
) c
join players p on p.player_id=c.player_id
left join team_members tm on tm.player_id=p.player_id and tm.left_at is null
left join teams t on t.team_id=tm.team_id
left join streamers s on s.player_id=p.player_id
order by teams_count desc, player asc
limit 25

