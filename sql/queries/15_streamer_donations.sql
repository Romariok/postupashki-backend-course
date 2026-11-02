-- 15. Сколько донатов собрал каждый стример.
-- В ответе все стримеры: без донатов donations = 0 и total_amount = 0.00;
-- team — команда с left_at IS NULL, иначе пусто.
--
-- Колонки: streamer, platform, team, donations, total_amount
-- Порядок: total_amount по убыванию, при равенстве streamer по возрастанию
-- Строк:   40

select s.nickname streamer, p.name platform, t.name team, count(sd.donation_id) donations, sum(amount) total_amount
from streamers s
left join team_members tm on s.player_id=tm.player_id and tm.left_at is null
left join teams t on tm.team_id=t.team_id
join platforms p on p.platform_id=s.platform_id
join streams st on s.streamer_id=st.streamer_id
join stream_donations sd on st.stream_id=sd.stream_id
group by streamer, platform, team
order by total_amount desc, streamer asc
limit 40