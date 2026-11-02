-- 25. Место стримера-профессионала в рейтинге своей страны.
-- Только стримеры с player_id и действующей записью в team_members; место по mmr
-- по убыванию и country_players считаются по всей players; player — ник из players.
--
-- Колонки: player, country, mmr, country_rank, country_players, team_tag, platform
-- Порядок: country по возрастанию, внутри страны country_rank по возрастанию
-- Строк:   16

select p.nickname player, p.country, p.mmr, p.country_rank, p.country_players, t.tag team_tag, pl.name platform
from (
    select player_id, nickname, country, mmr,
    rank() over (partition by country order by mmr desc) country_rank,
    count(*) over (partition by country) country_players
    from players
) p
join streamers s on s.player_id=p.player_id
join platforms pl on pl.platform_id=s.platform_id
join team_members tm on tm.player_id=p.player_id and tm.left_at is null
join teams t on t.team_id=tm.team_id
order by country asc, country_rank asc
limit 16

