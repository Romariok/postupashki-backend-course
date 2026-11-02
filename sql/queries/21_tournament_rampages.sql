-- 21. Турнирные выступления с KDA выше шестикратного среднего.
-- KDA = (kills + assists)::numeric / GREATEST(deaths, 1); порог — строго больше шести
-- средних KDA по всей match_players; только матчи турниров; kda округлить до 2 знаков.
--
-- Колонки: match_id, tournament, player, hero, kda
-- Порядок: kda по убыванию, при равенстве match_id, затем slot
-- Строк:   60

with k as (
    select match_id, slot, player_id, hero_id,
    (kills + assists)::numeric / greatest(deaths, 1) kda
    from match_players
)
select k.match_id, t.name tournament, p.nickname player, h.name hero, round(k.kda, 2) kda
from k
join matches m on m.match_id=k.match_id
join tournaments t on t.tournament_id=m.tournament_id
join players p on p.player_id=k.player_id
join heroes h on h.hero_id=k.hero_id
where k.kda > 6 * (select avg(kda) from k)
order by kda desc, match_id asc, k.slot asc
limit 60

