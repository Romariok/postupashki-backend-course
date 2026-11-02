-- 20. Герои, которых на патче банили чаще, чем брали.
-- Патч — matches.patch_id; в ответе пары с bans строго больше picks, diff = bans - picks.
--
-- Колонки: version, hero, bans, picks, diff
-- Порядок: diff по убыванию, затем hero по возрастанию, затем version по возрастанию
-- Строк:   99

with b as (
    select m.patch_id, mb.hero_id, count(*) bans
    from match_bans mb
    join matches m on m.match_id=mb.match_id
    group by m.patch_id, mb.hero_id
), pk as (
    select m.patch_id, mp.hero_id, count(*) picks
    from match_players mp
    join matches m on m.match_id=mp.match_id
    group by m.patch_id, mp.hero_id
)
select p.version, h.name hero, b.bans, coalesce(pk.picks, 0) picks, b.bans - coalesce(pk.picks, 0) diff
from b
left join pk on pk.patch_id=b.patch_id and pk.hero_id=b.hero_id
join patches p on p.patch_id=b.patch_id
join heroes h on h.hero_id=b.hero_id
where b.bans > coalesce(pk.picks, 0)
order by diff desc, hero asc, version asc
limit 99

