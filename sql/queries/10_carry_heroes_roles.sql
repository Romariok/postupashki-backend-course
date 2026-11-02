-- 10. Все роли героев, которые умеют быть керри.
-- Только герои с ролью 'carry'; у каждого выводятся все его роли, по строке на роль.
--
-- Колонки: name, primary_attr, attack_type, role
-- Порядок: name по возрастанию, внутри героя role по возрастанию
-- Строк:   47


select name, primary_attr, attack_type, hr.role
from heroes h
join hero_roles hr on h.hero_id=hr.hero_id and hr.hero_id in (select hero_id from hero_roles where role = 'carry')
order by name asc, hr.role asc
limit 47