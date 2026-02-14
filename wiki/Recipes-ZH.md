# 自定义配方参考 (JSON)

本指南介绍了“热冲击”模组中所有的自定义配方类型。

---

### 📖 快速导航
*   [`overheating`](#overheating)
*   [`thermal_shock`](#thermal_shock)
*   [`thermal_shock_filling`](#thermal_shock_filling)
*   [`clump_processing`](#clump_processing)
*   [`thermal_fuel`](#thermal_fuel)
*   [`thermal_converter`](#thermal_converter)
*   [`clump_filling`](#clump_filling)

---

## overheating
利用持续的高温熔融或转化物品/方块。

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。支持 `item` 或 `block` 类型。 |
| `result` | 对象 | 是 | - | 产出物品。标准 ItemStack 格式。 |
| `min_heat` | 整数 | 否 | `0` | 启动所需的最小热量速率 (H/t)。 |
| `heat_cost` | 整数 | 否 | `100` | 加工该配方所需的累计总热量 (H)。 |

### 示例
```json
{
  "type": "thermalshock:overheating",
  "ingredients": [
    {
      "type": "item",
      "value": {
        "item": "minecraft:iron_ingot"
      }
    }
  ],
  "result": {
    "id": "minecraft:iron_nugget",
    "count": 9
  },
  "min_heat": 200,
  "heat_cost": 1000
}
```

---

## thermal_shock
利用极高与极低热源之间的温差 (ΔT) 粉碎物品。

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。支持 `item` 或 `block` 类型。 |
| `result` | 对象 | 是 | - | 产出物品。标准 ItemStack 格式。 |
| `delta` | 整数 | **是** | - | 两者之间的最小温差绝对值 (|高温 - 低温|)。 |
| `min_hot` | 整数 | 否 | `-2147483648` | 所需最小高温热量值。 |
| `max_cold` | 整数 | 否 | `2147483647` | 允许最大低温热量值。 |

### 示例
```json
{
  "type": "thermalshock:thermal_shock",
  "ingredients": [
    {
      "type": "item",
      "value": {
        "item": "minecraft:cobblestone"
      }
    }
  ],
  "result": {
    "id": "minecraft:gravel",
    "count": 1
  },
  "delta": 150
}
```

---

## thermal_shock_filling
通过热冲击将产物数据编码进物质团块。**输入中必须包含一个团块物品。**

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。必须包含 `thermalshock:material_clump`。 |
| `target_item` | 字符串 | 是 | - | 编码进团块的产物物品 ID。 |
| `count` | 整数 | 否 | `1` | 编码进团块的产出数量。 |
| `delta` | 整数 | **是** | - | 填充所需的最小温差。 |

### 示例
```json
{
  "type": "thermalshock:thermal_shock_filling",
  "ingredients": [
    {
      "type": "item",
      "value": {
        "item": "thermalshock:material_clump"
      }
    },
    {
      "type": "item",
      "value": {
        "item": "minecraft:gold_ore"
      }
    }
  ],
  "target_item": "minecraft:gold_ingot",
  "count": 1,
  "delta": 700
}
```

---

## clump_processing
从已填充的团块中还原物品。

### 属性列表
| 属性 | 类型 | 必须 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | 数组 | 是 | - | 输入列表。必须包含带数据的团块。 |
| `target_item` | 字符串 | 是 | - | 匹配团块内部编码的产物 ID。 |
| `min_heat` | 整数 | 否 | `0` | 提取所需的最小热量速率。 |
| `heat_cost` | 整数 | 否 | `100` | 提取所需的累计热量。 |

### 示例
```json
{
  "type": "thermalshock:clump_processing",
  "ingredients": [
    {
      "type": "item",
      "value": {
        "item": "thermalshock:material_clump"
      }
    }
  ],
  "target_item": "minecraft:gold_ingot",
  "min_heat": 300,
  "heat_cost": 2000
}
```

---

## thermal_fuel
定义发热/发冷源的燃料属性。

### 示例
```json
{
  "type": "thermalshock:thermal_fuel",
  "ingredient": {
    "item": "minecraft:blaze_rod"
  },
  "burn_time": 2400,
  "heat_rate": 500
}
```

---

## thermal_converter
单方块精密转换机器，支持多物品/流体输入与概率产出。

### 内部对象结构规范

#### 输入物品 (`item_inputs`)
```json
{
  "ingredient": { "item": "..." },
  "count": 1,
  "consume_chance": 1.0
}
```

#### 输出物品 (`item_outputs`)
```json
{
  "item": { "id": "...", "count": 1 },
  "chance": 1.0
}
```

#### 流体项 (`fluid_inputs` / `fluid_outputs`)
```json
{
  "fluid": { "id": "...", "amount": 1000 },
  "consume_chance": 1.0,
  "chance": 1.0
}
```

### 综合示例
```json
{
  "type": "thermalshock:thermal_converter",
  "item_inputs": [
    {
      "ingredient": { "item": "minecraft:raw_iron" },
      "count": 1,
      "consume_chance": 1.0
    }
  ],
  "item_outputs": [
    {
      "item": { "id": "minecraft:iron_ingot", "count": 1 },
      "chance": 1.0
    },
    {
      "item": { "id": "minecraft:iron_nugget", "count": 3 },
      "chance": 0.5
    }
  ],
  "fluid_inputs": [
    {
      "fluid": { "id": "minecraft:water", "amount": 1000 },
      "consume_chance": 0.8
    }
  ],
  "min_heat": 500,
  "process_time": 100
}
```

---

## clump_filling
在工作台手工合成带标记的团块。

### 示例
```json
{
  "type": "thermalshock:clump_filling",
  "pattern": [
    "I",
    "C"
  ],
  "key": {
    "I": { "item": "minecraft:iron_ore" },
    "C": { "item": "thermalshock:material_clump" }
  },
  "target_item": "minecraft:iron_ingot"
}
```
