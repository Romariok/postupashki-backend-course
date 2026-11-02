-- 27. Пик онлайна против пика предыдущего эфира того же стримера.
-- Только площадка 'VK Play Live'; prev_peak — пик предыдущего по started_at эфира того
-- же стримера, delta — их разность; первый эфир каждого стримера не выводится.
--
-- Колонки: streamer, followers, started_at, title, peak_viewers, prev_peak, delta
-- Порядок: followers по убыванию, внутри стримера started_at по возрастанию
-- Строк:   592

select s.nickname streamer, s.followers, st.started_at, st.title, st.peak_viewers, st.prev_peak,
st.peak_viewers - st.prev_peak delta
from streamers s
join platforms p on p.platform_id=s.platform_id
join (
    select streamer_id, started_at, title, peak_viewers,
    lag(peak_viewers) over (partition by streamer_id order by started_at) prev_peak
    from streams
) st on st.streamer_id=s.streamer_id
where p.name = 'VK Play Live' and st.prev_peak is not null
order by followers desc, started_at asc
limit 592

