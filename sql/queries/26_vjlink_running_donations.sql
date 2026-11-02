-- 26. Нарастающая сумма донатов внутри каждого стрима Вжлинка за 2021 год.
-- Стримы Вжлинка, начавшиеся в 2021 году по UTC, строка на каждый донат; running_total —
-- нарастающая сумма донатов своего стрима по sent_at_sec.
--
-- Колонки: stream_id, title, sent_at_sec, donor, amount, running_total
-- Порядок: stream_id по возрастанию, внутри стрима sent_at_sec по возрастанию
-- Строк:   192

select st.stream_id, st.title, d.sent_at_sec, d.nickname donor, d.amount,
sum(d.amount) over (partition by st.stream_id order by d.sent_at_sec, d.donation_id) running_total
from streams st
join streamers s on s.streamer_id=st.streamer_id
join stream_donations d on d.stream_id=st.stream_id
where s.nickname = 'Вжлинк'
and st.started_at >= '2021-01-01 00:00:00+00' and st.started_at < '2022-01-01 00:00:00+00'
order by stream_id asc, sent_at_sec asc, d.donation_id asc
limit 192

