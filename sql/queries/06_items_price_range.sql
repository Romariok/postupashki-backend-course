-- 06. Предметы от 2000 до 3000 золота, кроме свитков рецептов.
-- cost от 2000 до 3000 включительно, строки с is_recipe = true исключаются.
--
-- Колонки: name, cost
-- Порядок: cost по убыванию, при равенстве name по возрастанию
-- Строк:   29

select name, cost from items WHERE
cost >= 2000 and cost <= 3000 and is_recipe = false
order by cost desc, name asc limit 29