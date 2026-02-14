# KubeJS 集成指南

热冲击 (Thermal Shock) 深度集成了 KubeJS。本指南详细说明了所有 JS 配方方法的传参规范、链式方法及其极致语法特性。

---

## 1. 核心参数规则
为了保持脚本简洁，我们采用了 **“精简构造函数 + 链式调用”** 的设计模式：
*   **必填项**：放在括号 `()` 中。如果不需要某项参数（如方块输入），必须传入空数组 `[]` 作为占位符。
*   **选填项**：使用 `.方法名()` 追加。如果不调用，则使用模组默认值。
*   **顺序规则**：基础数据（产物、输入）在前，数值数据（如热应力）在后。

---

## 2. 核心物理量定义
在配置涉及温度的配方时，需理解以下数值及其判定的物理意义：

### A. 对于模拟室 (Chamber)
*   **高温源速率 (`High Temp Rate`)**：周围所有正向热源（如岩浆、热能发生器）的总输入速率。数值为**正数**。
*   **低温源速率 (`Low Temp Rate`)**：周围所有负向冷源（如冰、冷能发生器）的总输入速率。数值为**负数**。
*   **热应力 (`ΔT`)**：加工启动的**核心门槛**。计算公式：`ΔT = 高温速率 - 低温速率`。

### B. 对于热力转换器 (Converter)
*   **净热量输入速率 (`Net Heat Rate`)**：周围所有能源提供的热量与冷量的代数和。数值可正可负。

---

## 3. 过热加工 (`overheating`)
利用持续的高温熔融或转化物品。

### 位置参数 (构造函数)
| 位置 | 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `result` | ItemStack | **是** | 产出物品。支持 `3x item` 缩写。 |
| 2 | `item_inputs` | Array | **是** | 物品输入列表。若无则填 `[]`。 |
| 3 | `block_inputs` | Array | **是** | 方块输入列表。支持**流体方块**（如 `minecraft:water`）。 |

### 可用的链式方法
| 方法名 | 参数类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `.minHeat(val)` | Integer | `0` | 启动所需的最小净热量输入速率 (H/t)。 |
| `.heatCost(val)` | Integer | `100` | 加工该配方所需的累计总热量 (H)。 |

### 示例
```js
// 2.1 基础熔炼 (使用默认热量门槛)
event.recipes.thermalshock.overheating('minecraft:charcoal', ['#minecraft:logs'], [])

// 2.2 高级加工 (指定高热量需求与高消耗)
event.recipes.thermalshock.overheating('9x iron_nugget', ['iron_ingot'], [])
     .minHeat(500)
     .heatCost(2000)

// 2.3 涉及流体方块的加工 (如：干化)
// 注意：若安装 [模拟升级]，block_inputs 中的流体方块会自动转为从接口流体槽抽取 1000mB 流体。
event.recipes.thermalshock.overheating('sponge', [], ['minecraft:water'])
     .heatCost(50)
```

---

## 4. 热冲击 (`thermal_shock`)
利用极高与极低热源产生的热应力破碎物质。

### 位置参数 (构造函数)
| 位置 | 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `result` | ItemStack | **是** | 产出物品。 |
| 2 | `item_inputs` | Array | **是** | 物品输入列表。 |
| 3 | `block_inputs` | Array | **是** | 方块输入列表。 |
| 4 | `delta` | Integer | **是** | **热应力 (ΔT)**。启动所需的最小速率差。 |

### 可用的链式方法
| 方法名 | 参数类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `.minHot(val)` | Integer | 不检查 | 所需的高温源最小输入速率 (H/t)。 |
| `.maxCold(val)` | Integer | 不检查 | 所需的低温源最大输入速率 (H/t)。数值应为负数或极小值。 |

### 示例
```js
// 3.1 基础破碎
event.recipes.thermalshock.thermal_shock('sand', ['gravel'], [], 50)

// 3.2 极端环境破碎 (限制环境必须极热或极冷)
event.recipes.thermalshock.thermal_shock('obsidian', [], ['minecraft:lava'], 800)
     .minHot(1000)
     .maxCold(0)
```

---

## 5. 团块填充 (热冲击) (`thermal_shock_filling`)
通过热冲击将产物数据编码进物质团块。
*注：系统会自动在输入中追加 `thermalshock:material_clump`。*

### 位置参数 (构造函数)
| 位置 | 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `target_item` | String | **是** | 最终编码进团块的产物物品 ID。 |
| 2 | `item_inputs` | Array | **是** | 物品输入列表。 |
| 3 | `block_inputs` | Array | **是** | 方块输入列表。 |
| 4 | `delta` | Integer | **是** | **热应力 (ΔT)**。填充所需的最小温差。 |

### 可用的链式方法
| 方法名 | 参数类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `.targetCount(val)`| Integer | `1` | 最终从团块中提取出的产物数量。 |
| `.minHot(val)` | Integer | 不限制 | 对应的高温源输入速率要求。 |
| `.maxCold(val)` | Integer | 不限制 | 对应的低温源输入速率要求。 |

### 示例
```js
// 4.1 基础填充
event.recipes.thermalshock.thermal_shock_filling('iron_ingot', ['iron_ore'], [], 700)

// 4.2 批量填充 (设置提取时的产出数量)
event.recipes.thermalshock.thermal_shock_filling('gold_nugget', ['raw_gold'], [], 500)
     .targetCount(4)
```

---

## 6. 团块提取 (`clump_processing`)
从已填充的团块中还原物品。
*注：系统会自动在输入中追加带数据的 `thermalshock:material_clump`。*

### 位置参数 (构造函数)
| 位置 | 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `result` | ItemStack | **是** | 最终产出的物品堆叠。 |
| 2 | `target_item` | String | **是** | 只有编码了此 ID 的团块才会被处理。 |
| 3 | `item_inputs` | Array | **是** | 附加物品消耗。 |
| 4 | `block_inputs` | Array | **是** | 附加方块消耗。 |

### 可用的链式方法
| 方法名 | 参数类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `.minHeat(val)` | Integer | `0` | 提取所需的最小净热量输入速率。 |
| `.heatCost(val)` | Integer | `100` | 提取所需的累计总热量. |

### 示例
```js
// 5.1 常规提取 (仅需团块)
event.recipes.thermalshock.clump_processing('gold_ingot', 'gold_ingot', [], [])

// 5.2 强化提取 (需要额外材料且热量消耗极高)
event.recipes.thermalshock.clump_processing('netherite_ingot', 'netherite_ingot', ['gold_ingot'], [])
     .minHeat(300)
     .heatCost(5000)
```

---

## 7. 热力转换器 (`thermal_converter`)
单方块精密转换，支持多物品/流体和概率。

### 位置参数 (构造函数)
| 位置 | 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `item_outputs` | Map/Array | **是** | 物品输出。推荐映射写法：`{ "id": 概率 }`。 |
| 2 | `item_inputs` | Map/Array | **是** | 物品输入。推荐映射写法：`{ "id": 消耗概率 }`。 |
| 3 | `process_time` | Integer | **是** | 加工耗时 (ticks)。默认 20。 |

### 可用的链式方法
| 方法名 | 参数类型 | 说明 |
| :--- | :--- | :--- |
| `.fluidInput(val)` | String/Map | 添加流体输入。支持 `1000x water` 格式。 |
| `.fluidOutput(val)`| String/Map | 添加流体输出。支持 `100x lava` 格式。 |
| `.chance(val)` | Double | 为**最近一次**通过链式方法添加的项目设置概率。 |
| `.minHeat(val)` | Integer | 设置工作的最小净热量输入速率。 |
| `.maxHeat(val)` | Integer | 设置工作的最大净热量输入速率。 |

### 示例
```js
// 6.1 极致丝滑的映射写法 (多输出 + 概率)
event.recipes.thermalshock.thermal_converter(
    { "gold_ingot": 1.0, "2x gold_nugget": 0.5 }, 
    "raw_gold", 
    200
)

// 6.2 链式流体与概率追踪
event.recipes.thermalshock.thermal_converter("obsidian", "cobblestone", 100)
     .fluidInput("1000x water").chance(0.8) // 0.8 概率应用于 water
     .fluidOutput("100x lava").chance(0.1)  // 0.1 概率应用于 lava
     .minHeat(500)
```

---

## 8. 热力燃料 (`thermal_fuel`)
定义发热/发冷源的燃料属性。

### 位置参数 (构造函数)
| 位置 | 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `ingredient` | Ingredient | **是** | 燃料物品或标签。 |
| 2 | `burn_time` | Integer | **是** | 燃烧持续时间 (ticks)。 |
| 3 | `heat_rate` | Integer | **是** | 产生/吸收热量的速率 (H/t)。正热负冷。 |

### 示例
```js
event.recipes.thermalshock.thermal_fuel('coal', 1600, 20) // 产生热量
event.recipes.thermalshock.thermal_fuel('blue_ice', 4800, -100) // 产生冷量
```

---

## 9. Data Maps 集成
用于动态定义方块和物品的核心热力属性。**必须在 `server_scripts` 中监听。**

### A. 结构外壳 (`registerCasing`)
定义方块作为模拟室外壳时的物理属性与通量限制。
*   `event.add(block, maxHeat, maxCold, efficiency)`
    *   `maxHeat`: **高温输入限制 (H/t)**。外壳允许通过的最大热量输入速率。
    *   `maxCold`: **低温输入限制 (H/t)**。外壳允许通过的最大冷量输入速率（绝对值）。
    *   `efficiency`: **结构效率 (1.0 为基准)**。影响**催化剂消耗率**。效率越高，单位产出消耗的催化剂点数越少。

```js
ThermalShockEvents.registerCasing(event => {
    event.add('minecraft:obsidian', 1000, -500, 0.5)
})
```

### B. 模拟催化剂 (`registerCatalyst`)
`event.add(item, bonus, buffer)`
*   `bonus`: 产量增益系数 (0.5 = +50%)。
*   `buffer`: 该物品提供的催化剂缓存点数。

### C. 热力源属性
定义方块在机器附近时的输出功率。

*   **热源 (`registerHeatSource`)**：`event.add(block, rate)` (正数)
*   **冷源 (`registerColdSource`)**：`event.add(block, rate)`
    *   **注意**：`rate` 需填入**正数**（表示冷却功率），代码逻辑会自动按减法处理。

```js
ThermalShockEvents.registerColdSource(event => {
    // 赋予冰块制冷能力：填入 50，机器实际收到 -50 H/t
    event.add('minecraft:ice', 50) 
})
```

---

## 10. 辅助功能总结
*   **占位符**：构造函数中不使用的参数位置必须填充 `[]`。
*   **缩写支持**：字符串支持 `3x item` 和 `1000x fluid` 格式。
*   **模拟升级特性**：在模拟室中，`block_inputs` 若为流体方块，在安装升级后将自动支持接口流体自动化。
