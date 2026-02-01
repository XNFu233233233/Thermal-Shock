# KubeJS 脚本集成指南

本模组为 KubeJS 21 提供了深度集成。模组包作者可以通过 JavaScript 定义复杂的配方逻辑和动态数据。

---

## 1. 核心对象：模拟原料 (Simulation Ingredient)

模拟室的所有配方（过热、热冲击、填充、提取）都使用特定的原料对象结构，以区分**物品实体**与**方块/流体**。

### 属性参考
| 属性 | 类型 | 说明 |
| :--- | :--- | :--- |
| `value` | 字符串 | 物品或方块的 ID（例如 `'minecraft:iron_ingot'`）。 |
| `type` | 字符串 | `'item'`: 匹配掉落物实体或接口物品。 <br> `'block'`: 匹配模拟室内的方块或接口流体。 |

---

## 2. 配方参考 (ServerEvents.recipes)

所有配方均通过 `event.recipes.thermalshock` 下的相应方法调用。

### A. 过热加工 (`overheating`)
用于模拟室的过热模式。支持最多 3 个物品项 + 3 个方块项。

**属性参考**
*   `ingredients`: (数组) 包含 `Simulation Ingredient` 对象的列表。
*   `result`: (物品) 产出。
*   `min_heat`: (整数) 启动所需的最小速率 (H/t)。
*   `heat_cost`: (整数) 完成所需的总热量 (H)。

**示例**
```javascript
event.recipes.thermalshock.overheating(
    [{ value: 'minecraft:sand', type: 'item' }],
    'minecraft:glass',
    50, 200
)
```

---

### B. 热冲击 (`thermal_shock`)
用于模拟室的热冲击模式。支持最多 3 个物品项 + 3 个方块项。

**属性参考**
*   `min_hot`: 最小高温要求 (H)。
*   `max_cold`: 最大低温要求 (H)。
*   `delta`: 最小温差要求 (ΔT)。

**示例**
```javascript
event.recipes.thermalshock.thermal_shock(
    [{ value: 'minecraft:cobblestone', type: 'item' }],
    'minecraft:gravel',
    100, -50, 150
)
```

---

### C. 团块填充机器 (`thermal_shock_filling`)
用于生产含有数据的物质团块。**必须在原料中包含一个团块。**

**属性参考**
*   `target_result`: (物品) 注入团块内部的最终产物。
*   `clump_min_heat`: (整数) 写入团块的提取热量速率需求。
*   `clump_heat_cost`: (整数) 写入团块的提取热量总量需求。

**示例**
```javascript
event.recipes.thermalshock.thermal_shock_filling(
    [
        { value: 'thermalshock:material_clump', type: 'item' },
        { value: 'minecraft:iron_ore', type: 'item' }
    ],
    'minecraft:iron_ingot',
    500, -200, 700,
    200, 1000
)
```

---

### D. 团块提取 (`extraction` 或 `clump_processing`)
从已填充的团块中还原物品。**必须包含带数据的团块。**

**属性参考**
*   `target_content`: (物品) 团块内部必须匹配的数据产物。

**示例**
```javascript
event.recipes.thermalshock.extraction(
    [{ value: 'thermalshock:material_clump', type: 'item' }],
    'minecraft:iron_ingot',
    200, 1000
)
```

---

### E. 热力燃料 (`thermal_fuel`)
定义加热器或冷却器的有效燃料。

**属性参考**
*   `ingredient`: (物品) 燃料项目。
*   `burn_time`: (整数) 燃烧刻数。
*   `heat_rate`: (整数) 速率。正数为热源，负数为冷源。

**示例**
```javascript
event.recipes.thermalshock.thermal_fuel('minecraft:blaze_rod', 2400, 50)
```

---

### F. 热力转换器 (`thermal_converter`)
精密单方块加工。

**属性参考**
*   `item_inputs`: (数组) `{ ingredient: 'id', count: n, consume_chance: 1.0 }` 列表。
*   `fluid_inputs`: (数组) `{ fluid: 'id', amount: n, consume_chance: 1.0 }` 列表。
*   `item_outputs`: (数组) `{ item: 'id', chance: 1.0 }` 列表。
*   `fluid_outputs`: (数组) `{ fluid: 'id', amount: n, chance: 1.0 }` 列表。
*   `process_time`: (整数) 时长。
*   `min_heat` / `max_heat`: (整数) 工作的热量区间。

**示例**
```javascript
event.recipes.thermalshock.thermal_converter(
    [{ ingredient: 'minecraft:sand', count: 1, consume_chance: 1.0 }],
    [], [], [],
    100, 100, 500
)
```
