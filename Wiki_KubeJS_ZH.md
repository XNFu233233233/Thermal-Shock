# 热冲击 (Thermal Shock) - KubeJS 适配指南

本模组为 NeoForge 1.21.1 提供了原生 KubeJS 支持。允许模组包作者通过 JavaScript 定义自定义配方、燃料并动态修改模组数据。

所有配方类型都支持 **输出在前，输入在后** 的参数顺序，并且可选参数支持 **链式调用**。

## 1. 配方架构 (Recipe Schemas)

在 `ServerEvents.recipes` 脚本中，你可以使用 `event.recipes.thermalshock` 下的方法。

### 1.1 过热加工 (Overheating)
根据热量输入速率处理原料。

**语法：**
```javascript
event.recipes.thermalshock.overheating(output, input)
    .minHeat(100)       // 可选，默认 0
    .heatCost(5000)     // 可选，默认 0
```

*   `output`: 输出物品 (Item ID 或 ItemStack)。
*   `input`: `SimulationIngredient` (详见第 2 节)。
*   `min_heat`: 所需的最小热输入速率 (H/t)。
*   `heat_cost`: 每次操作消耗的热量 (H)。

---

### 1.2 热冲击加工 (Thermal Shock)
利用温差 (ΔT) 粉碎物质。

**语法：**
```javascript
event.recipes.thermalshock.thermal_shock(output, input)
    .minHot(1000)       // 可选
    .maxCold(-500)      // 可选
    .delta(800)         // 可选
```

*   `min_hot`: 所需的最小高温值 (H)。
*   `max_cold`: 所需的最大低温值 (H，通常为负数)。
*   `delta`: 所需的最小温差 (ΔT)。

---

### 1.3 物质填充 - 热冲击 (Material Clump Filling)
创建一个带有特定加工需求的物质团块。

**语法：**
```javascript
event.recipes.thermalshock.thermal_shock_filling(output, input)
    .minHot(1000)
    .maxCold(-500)
    .delta(800)
    .clumpMinHeat(200)    // 团块后续加工需求
    .clumpHeatCost(2000)
```

*   `output`: 最终产物 (团块加工后会得到这个)。
*   `input`: 原料。
*   `clump_min_heat`/`clump_heat_cost`: 存储在结果团块中的数据。

---

### 1.4 团块加工 (Clump Processing)
在过热期间处理物质团块。注意：输入必须是 `thermalshock:material_clump`，但此处定义的 input 会匹配团块的具体 NBT 数据。

**语法：**
```javascript
event.recipes.thermalshock.clump_processing(output, input)
    .minHeat(500)
    .heatCost(10000)
```

*   `output`: 团块内部应该包含的物品（如果团块里装的是这个，配方才匹配）。
*   `input`: 通常是一个包含特定 NBT 的团块，或者简单的 `thermalshock:material_clump`。实际上配方逻辑会检查团块内部是否包含 `output` 指定的物品。

---

### 1.5 热力燃料 (Thermal Fuel)
定义物品作为发生器（加热器/冷却器）燃料的时间和效能。

**语法：**
```javascript
event.recipes.thermalshock.thermal_fuel(input)
    .burnTime(200)      // 可选
    .heatRate(10)       // 可选，正数=热，负数=冷
```

---

### 1.6 热力转换 (Thermal Converter)
多物品/流体转换。

**语法：**
```javascript
event.recipes.thermalshock.thermal_converter(itemOutputs, fluidOutputs, itemInputs, fluidInputs)
    .processTime(200)
    .minHeat(100)
    .maxHeat(5000)
```

*   `itemOutputs`: 物品数组 `['minecraft:apple', 'minecraft:stone']`。
*   `fluidOutputs`: 流体数组 `['100x minecraft:water']`。
*   `itemInputs`: 物品输入数组。
*   `fluidInputs`: 流体输入数组。

---

## 2. 模拟室原料 (Simulation Ingredients)

模拟室中的每个配方都需要指定来源类型：
- `type: 'item'`: 匹配掉落物实体或接口中的物品槽位。
- `type: 'block'`: 匹配模拟室内部的方块或接口中的流体（如果该输入定义为桶）。

**简写格式 (默认 `type: 'item'`):**
可以直接使用物品 ID 或标签字符串：
```javascript
'minecraft:iron_ingot'
'#forge:ingots/iron'
```

**高级格式 (指定 `type`):**
```javascript
{ "value": { "item": "minecraft:iron_ingot" }, "type": "item" }
// 或者用于方块检测
{ "value": { "block": "minecraft:magma_block" }, "type": "block" }
```

---

## 3. 数据映射集成 (Data Maps)

(需要验证 NeoForge 1.21.1 KubeJS 支持)

支持的数据映射类型：
- `thermalshock:chamber_casing` (Block)
- `thermalshock:chamber_catalyst` (Item)
- `thermalshock:heat_source` (Block)
- `thermalshock:cold_source` (Block)

**示例注册：**
```javascript
ServerEvents.registry('neoforge:data_map_type', event => {
   // 如果 KubeJS 尚未自动注册，可能需要手动注册（视版本而定）
})

// 修改数据
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
