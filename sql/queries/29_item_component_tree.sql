-- 29. Полное дерево сборки Guardian Greaves, Bloodthorn, Harpoon и Hurricane Pike.
-- Строка на каждый путь вниз, сам корень не выводится; lvl с 1, qty — quantity последнего
-- шага, qty_total — произведение quantity по пути, path — имена от корня через ' > '.
--
-- Колонки: root_item, lvl, component, qty, qty_total, path
-- Порядок: root_item по возрастанию, затем path по возрастанию
-- Строк:   34

with recursive tree as (
    select i.name root_item, 1 lvl, c.name component, ir.quantity qty, ir.quantity qty_total,
    i.name || ' > ' || c.name path, ir.component_id
    from items i
    join item_recipe ir on ir.item_id=i.item_id
    join items c on c.item_id=ir.component_id
    where i.name in ('Guardian Greaves', 'Bloodthorn', 'Harpoon', 'Hurricane Pike')
    union all
    select t.root_item, t.lvl + 1, c.name, ir.quantity, t.qty_total * ir.quantity,
    t.path || ' > ' || c.name, ir.component_id
    from tree t
    join item_recipe ir on ir.item_id=t.component_id
    join items c on c.item_id=ir.component_id
)
select root_item, lvl, component, qty, qty_total, path
from tree
order by root_item asc, path asc
limit 34

