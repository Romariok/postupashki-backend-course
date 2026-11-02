-- 30. Стоимость сборки из базовых составляющих против собственной цены предмета.
-- Берутся предметы со строками в item_recipe, сборка раскрывается до базовых (тех, у кого
-- строк в item_recipe нет, свитки тоже), количества по пути перемножаются; diff = parts_cost - cost.
--
-- Колонки: name, cost, parts_cost, base_parts, diff
-- Порядок: diff по убыванию, при равенстве name по возрастанию
-- Строк:   77

with recursive parts as (
    select item_id root_id, component_id, quantity qty
    from item_recipe
    union all
    select p.root_id, ir.component_id, p.qty * ir.quantity
    from parts p
    join item_recipe ir on ir.item_id=p.component_id
), base as (
    select p.root_id, sum(c.cost * p.qty) parts_cost, sum(p.qty) base_parts
    from parts p
    join items c on c.item_id=p.component_id
    where not exists (select 1 from item_recipe ir where ir.item_id=p.component_id)
    group by p.root_id
)
select i.name, i.cost, b.parts_cost, b.base_parts, b.parts_cost - i.cost diff
from base b
join items i on i.item_id=b.root_id
order by diff desc, name asc
limit 77

