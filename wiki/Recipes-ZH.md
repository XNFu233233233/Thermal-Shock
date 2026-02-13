# 自定义配方参考

本指南详细介绍了“热冲击”模组中所有的自定义配方类型及其完整 JSON 结构。

---

## 1. 过热加工 (`thermalshock:overheating`)
利用持续的高温熔融或转化物品/方块。

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。支持 `item` 或 `block` 类型。最多 3 项物品 + 3 项方块。 |
| `result` | 对象 | 是 | - | 产出物品。包含 `id` 和 `count`。 |
| `min_heat` | 整数 | 否 | **0** | 启动所需的最小热量速率 (H/t)。 |
| `heat_cost` | 整数 | 否 | **100** | 加工该配方所需的累计总热量 (H)。 |

### 示例
```json
{
  "type": "thermalshock:overheating",
  "ingredients": [
    { "type": "item", "value": { "item": "minecraft:iron_ingot" } }
  ],
  "result": { "id": "minecraft:iron_nugget", "count": 9 },
  "min_heat": 200,
  "heat_cost": 1000
}
```

---

## 2. 热冲击 (`thermalshock:thermal_shock`)
利用极高与极低热源之间的温差 (ΔT) 粉碎物品。

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。支持流体方块。 |
| `result` | 对象 | 是 | - | 产出物品。 |
| `delta` | 整数 | **是** | - | 两者之间的最小温差绝对值 (|高温 - 低温|)。 |
| `min_hot` | 整数 | 否 | **不检查** | 模拟室内所需的最小高温热量值 (H)。 |
| `max_cold` | 整数 | 否 | **不检查** | 模拟室内允许的最大低温热量值 (H)。 |

### 示例
```json
{
  "type": "thermalshock:thermal_shock",
  "ingredients": [
    { "type": "item", "value": { "item": "minecraft:cobblestone" } }
  ],
  "result": { "id": "minecraft:gravel", "count": 1 },
  "delta": 150
}
```

---

## 3. 团块填充机器 (`thermalshock:thermal_shock_filling`)
通过热冲击将产物数据编码进物质团块。**输入中必须包含一个团块物品。**

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。必须包含 `thermalshock:material_clump`。 |
| `target_result` | 对象 | 是 | - | 被注入团块内部的产物信息（提取时产出什么）。 |
| `delta` | 整数 | **是** | - | 填充所需的最小温差。 |
| `min_hot` | 整数 | 否 | **不检查** | 填充所需的最小高温。 |
| `max_cold` | 整数 | 否 | **不检查** | 填充所需的最大低温。 |

### 示例
```json
{
  "type": "thermalshock:thermal_shock_filling",
  "ingredients": [
    { "type": "item", "value": { "item": "thermalshock:material_clump" } },
    { "type": "item", "value": { "item": "minecraft:gold_ore" } }
  ],
  "target_result": { "id": "minecraft:gold_ingot", "count": 1 },
  "delta": 700
}
```

---

## 4. 团块提取 (`thermalshock:clump_processing`)
从已填充的团块中还原物品。

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。必须包含带数据的团块。 |
| `target_content` | 对象 | 是 | - | 只有内部数据与此项匹配的团块才会被处理。 |
| `min_heat` | 整数 | 否 | **0** | 提取该团块所需的最小热量速率。 |
| `heat_cost` | 整数 | 否 | **100** | 提取该团块所需的累计热量。 |

### 示例
```json
{
  "type": "thermalshock:clump_processing",
  "ingredients": [
    { "type": "item", "value": { "item": "thermalshock:material_clump" } }
  ],
  "target_content": { "id": "minecraft:gold_ingot" },
  "min_heat": 300,
  "heat_cost": 2000
}
```

---

## 5. 热力燃料 (`thermalshock:thermal_fuel`)
定义发热/发冷源的燃料属性。

### 属性列表
| 属性 | 类型 | 必须 | 说明 |
| :--- | :--- | :--- | :--- |
| `ingredient` | 对象 | 是 | 燃料物品定义（支持标签）。 |
| `burn_time` | 整数 | 是 | 燃烧持续时间（ticks）。 |
| `heat_rate` | 整数 | 是 | 产生热量速率。正数为加热，负数为冷却。 |

---

## 6. 热力转换器 (`thermalshock:thermal_converter`)
单方块精密转换，支持多流体和概率产出。

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `item_inputs` | 数组 | 否 | - | 输入物品列表。 |
| `fluid_inputs` | 数组 | 否 | - | 输入流体列表。 |
| `item_outputs` | 数组 | 否 | - | 产出物品列表。 |
| `fluid_outputs` | 数组 | 否 | - | 产出流体列表。 |
| `process_time` | 整数 | 否 | **20** | 加工耗时 (ticks)。 |
| `min_heat` | 整数 | 否 | -∞ | 允许工作的最小热量。 |
| `max_heat` | 整数 | 否 | +∞ | 允许工作的最大热量。 |
