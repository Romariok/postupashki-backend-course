-- 18. Призовые турниров, до которых добрались игроки команды.
-- Турнир в зачёт один раз, если игрок сыграл его матч в составе: joined_at <=
-- started_at::date < left_at (пустой left_at — сейчас); команды без турниров не выводятся.
--
-- Колонки: team, tag, tournaments, prize_total
-- Порядок: prize_total по убыванию, при равенстве team по возрастанию
-- Строк:   22

select t.name team, tag, tournaments, prize_total
from teams t
join (
    select team_id, count(*) tournaments, sum(prize_pool) prize_total
    from (
        select distinct tm.team_id, tr.tournament_id, tr.prize_pool
        from team_members tm
        join match_players mp on mp.player_id=tm.player_id
        join matches m on m.match_id=mp.match_id
        join tournaments tr on tr.tournament_id=m.tournament_id
        where tm.joined_at <= m.started_at::date
        and (tm.left_at is null or m.started_at::date < tm.left_at)
    ) tt
    group by team_id
) x on x.team_id=t.team_id
order by prize_total desc, team asc
limit 22

