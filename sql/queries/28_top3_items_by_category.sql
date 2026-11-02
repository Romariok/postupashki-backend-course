-- 28. Три самых дорогих предмета в каждой категории лавки.
-- Категория: 'recipe' при is_recipe, иначе 'assembled' при строках в item_recipe.item_id,
-- иначе 'basic'; place 1-3 по cost убыв., при равной цене по name; purchases — строк match_items, иначе 0.
--
-- Колонки: category, place, name, cost, purchases
-- Порядок: category по возрастанию, затем place по возрастанию
-- Строк:   9

with c as (
    select i.item_id, i.name, i.cost,
    case when i.is_recipe then 'recipe'
         when exists (select 1 from item_recipe ir where ir.item_id=i.item_id) then 'assembled'
         else 'basic' end category
    from items i
), r as (
    select c.*, row_number() over (partition by category order by cost desc, name asc) place
    from c
)
select r.category, r.place, r.name, r.cost, count(mi.item_id) purchases
from r
left join match_items mi on mi.item_id=r.item_id
where r.place <= 3
group by r.category, r.place, r.name, r.cost
order by category asc, place asc
limit 9

