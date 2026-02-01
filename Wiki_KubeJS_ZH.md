# 热冲击 (Thermal Shock) - KubeJS 适配指南

本模组为 NeoForge 1.21.1 提供了原生 KubeJS 支持。允许模组包作者通过 JavaScript 定义自定义配方、燃料并动态修改模组数据。

## 1. 配方架构 (Recipe Schemas)

在 `ServerEvents.recipes` 脚本中，你可以使用 `event.recipes.thermalshock` 下的方法。

### 1.1 过热加工 (Overheating)
根据热量输入速率处理原料。

**语法：**
```javascript
event.recipes.thermalshock.overheating(ingredients, result, min_temp, heat_cost)
```

*   `ingredients`: `SimulationIngredient` 数组（详见第 2 节）。
*   `result`: 输出物品 (Item ID 或 ItemStack)。
*   `min_temp`: 所需的最小热输入速率 (H/t)。
*   `heat_cost`: 每次操作消耗的热量 (H)。

---

### 1.2 热冲击加工 (Thermal Shock)
利用温差 (ΔT) 粉碎物质。

**语法：**
```javascript
event.recipes.thermalshock.thermal_shock(ingredients, result, min_hot, max_cold, delta)
```

*   `min_hot`: 所需的最小高温值 (H)。
*   `max_cold`: 所需的最大低温值 (H，通常为负数)。
*   `delta`: 所需的最小温差 (ΔT)。

---

### 1.3 物质填充 - 热冲击 (Material Clump Filling)
创建一个带有特定加工需求的物质团块。

**语法：**
```javascript
event.recipes.thermalshock.thermal_shock_filling(ingredients, target_result, min_hot, max_cold, delta, clump_min_temp, clump_heat_cost)
```

*   `clump_min_temp`: 存储在结果团块中的 `min_temp` 需求。
*   `clump_heat_cost`: 存储在结果团块中的 `heat_cost` 需求。

---

### 1.4 热力燃料 (Thermal Fuel)
定义物品作为发生器（加热器/冷却器）燃料的时间和效能。

**语法：**
```javascript
event.recipes.thermalshock.thermal_fuel(ingredient, burn_time, heat_rate)
```

*   `heat_rate`: 正数代表加热器燃料，负数代表冷却器燃料。

---

## 2. 模拟室原料 (Simulation Ingredients)

模拟室中的每个配方都需要指定来源类型：
- `type: 'item'`: 匹配掉落物实体或接口中的物品槽位。
- `type: 'block'`: 匹配模拟室内部的方块或接口中的流体（如果该输入定义为桶）。

**示例：**
```javascript
{ value: 'minecraft:iron_ingot', type: 'item' }
```

---

## 3. 数据映射集成 (Data Maps)

你可以通过 `ServerEvents.highPriorityData` 修改 Data Map：

### 3.1 结构外壳属性 (Chamber Casings)
定义哪些方块可以作为结构外壳。

**路径**: `thermalshock:data_maps/block/chamber_casing.json`

```javascript
ServerEvents.highPriorityData(event => {
    event.addJson('thermalshock:data_maps/block/chamber_casing.json', {
        values: {
            'minecraft:diamond_block': {
                max_heat_rate: 10000,
                max_cold_rate: 10000,
                efficiency: 2.0
            }
        }
    })
})
```

### 3.2 催化剂属性 (Chamber Catalysts)
**路径**: `thermalshock:data_maps/item/chamber_catalyst.json`

```javascript
{
    bonus_yield: 0.5,    // +50% 产量加成
    buffer_amount: 50.0 // 每个物品补充的缓冲区点数
}
```
