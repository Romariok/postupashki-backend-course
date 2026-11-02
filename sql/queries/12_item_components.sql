-- 12. Сборные предметы внутри сборных предметов.
-- Пары «предмет — непосредственный компонент», где компонент сам встречается
-- в item_recipe.item_id; каждая пара один раз, component_cost — цена штуки.
--
-- Колонки: item_name, component_name, quantity, component_cost
-- Порядок: item_name по возрастанию, внутри предмета component_name по возрастанию
-- Строк:   36

select i.name item_name, ii.name component_name, ir.quantity, ii.cost component_cost
from items i
join 
(select * from item_recipe ir_temp1 where exists 
   (select 1 from item_recipe ir_temp2 where ir_temp2.item_id=ir_temp1.component_id)
) ir 
on i.item_id=ir.item_id
left join items ii on ir.component_id=ii.item_id
order by item_name asc, component_name asc
limit 36