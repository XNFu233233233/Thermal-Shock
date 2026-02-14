# 数据映射 (Data Maps) 技术参考

热冲击使用 NeoForge Data Maps 允许整合包作者灵活配置各种方块与物品的属性。

---

## 1. 结构外壳 (`thermalshock:chamber_casing`)
定义哪些方块可以作为模拟室的外壳。

### 属性列表
| 字段 | 类型 | 必须 | 说明 |
| :--- | :--- | :--- | :--- |
| `max_heat_rate` | 整数 | 是 | 该外壳能从正热源传导的最大 H/t。 |
| `max_cold_rate` | 整数 | 是 | 该外壳能从冷源传导的最大 H/t (绝对值)。 |
| `efficiency` | 浮点数 | 否 | 结构效率系数 (默认: 1.0)。值越大消耗越慢 (例如 2.0 代表消耗减半)。 |

### 完整示例
**路径**: `data/thermalshock/data_maps/block/chamber_casing.json`
```json
{
  "values": {
    "minecraft:obsidian": {
      "max_heat_rate": 5000,
      "max_cold_rate": 5000,
      "efficiency": 1.0
    }
  }
}
```

---

## 2. 催化剂 (`thermalshock:chamber_catalyst`)
定义可放入模拟室专用槽位的催化物品。

### 属性列表
| 字段 | 类型 | 必须 | 说明 |
| :--- | :--- | :--- | :--- |
| `bonus_yield` | 浮点数 | 是 | 产量加成（例如 0.5 代表多产出 50%）。 |
| `buffer_amount` | 浮点数 | 否 | 每个物品补充的催化点数 (默认: 10.0)。 |

### 完整示例
**路径**: `data/thermalshock/data_maps/item/chamber_catalyst.json`
```json
{
  "values": {
    "minecraft:glowstone_dust": {
      "bonus_yield": 0.5,
      "buffer_amount": 25.0
    }
  }
}
```

---

## 3. 环境热源 (`thermalshock:heat_source`)
定义自定义方块作为环境热量来源。

### 属性列表
| 字段 | 类型 | 必须 | 说明 |
| :--- | :--- | :--- | :--- |
| `heat_per_tick` | 整数 | 是 | 每 tick 提供的正热量值。 |

### 完整示例
**路径**: `data/thermalshock/data_maps/block/heat_source.json`
```json
{
  "values": {
    "minecraft:magma_block": {
      "heat_per_tick": 5
    }
  }
}
```

---

## 4. 环境冷源 (`thermalshock:cold_source`)
定义自定义方块作为环境冷量来源。

### 属性列表
| 字段 | 类型 | 必须 | 说明 |
| :--- | :--- | :--- | :--- |
| `cooling_per_tick` | 整数 | 是 | 每 tick 提供的冷量值 (绝对值)。 |

### 完整示例
**路径**: `data/thermalshock/data_maps/block/cold_source.json`
```json
{
  "values": {
    "minecraft:packed_ice": {
      "cooling_per_tick": 10
    }
  }
}
```
