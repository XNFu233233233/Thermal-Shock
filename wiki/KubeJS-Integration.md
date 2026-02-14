# KubeJS Integration Guide

Thermal Shock is deeply integrated with KubeJS. This guide details all JS recipe method specifications, chainable methods, and advanced syntax features.

---

## 1. Core Parameter Rules
To keep scripts concise, we use a **"Simplified Constructor + Method Chaining"** design pattern:
*   **Required Arguments**: Placed inside parentheses `()`. If a certain parameter is not needed (e.g., block inputs), you **must** pass an empty array `[]` as a placeholder.
*   **Optional Modifiers**: Added using `.methodName()`. If not called, mod defaults are used.
*   **Order Rules**: Base data (Result, Inputs) first, numerical data (like Thermal Stress) at the end.

---

## 2. Physical Quantity Definitions
When configuring recipes involving temperature, it is essential to understand the following values and their physical logic:

### A. For the Simulation Chamber
*   **High Temp Rate**: The total heat input rate from all connected positive heat sources (e.g., Magma, Thermal Heater). This value is **positive**.
*   **Low Temp Rate**: The total cooling input rate from all connected negative cold sources (e.g., Ice, Thermal Freezer). This value is **negative**.
*   **Thermal Stress (ΔT)**: The **core threshold** for starting a process. Formula: `ΔT = High Rate - Low Rate`.

### B. For the Thermal Converter
*   **Net Heat Rate**: The algebraic sum of energy provided by all connected sources. Can be positive or negative.

---

## 3. Overheating (`overheating`)
Uses sustained high temperature to melt or transform items.

### Positional Arguments (Constructor)
| Pos | Name | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `result` | ItemStack | **Yes** | Final output. Supports `3x item` shorthand. |
| 2 | `item_inputs` | Array | **Yes** | List of item inputs. Use `[]` if none. |
| 3 | `block_inputs` | Array | **Yes** | List of block inputs. Supports **Fluid Blocks** (e.g., `minecraft:water`). |

### Available Chained Methods
| Method | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `.minHeat(val)` | Integer | `0` | Minimum net heat rate (H/t) required to start. |
| `.heatCost(val)` | Integer | `100` | Total heat (H) required to complete the process. |

### Examples
```js
// 3.1 Basic Smelting (Using default heat thresholds)
event.recipes.thermalshock.overheating('minecraft:charcoal', ['#minecraft:logs'], [])

// 3.2 Advanced Processing (High threshold and high cost)
event.recipes.thermalshock.overheating('9x iron_nugget', ['iron_ingot'], [])
     .minHeat(500)
     .heatCost(2000)

// 3.3 Fluid Block Processing
// Note: If a [Simulation Upgrade] is installed, fluid blocks in block_inputs 
// will automatically be pulled from Ports as 1000mB fluids.
event.recipes.thermalshock.overheating('sponge', [], ['minecraft:water'])
     .heatCost(50)
```

---

## 4. Thermal Shock (`thermal_shock`)
Uses thermal stress from extreme hot and cold sources to shatter materials.

### Positional Arguments (Constructor)
| Pos | Name | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `result` | ItemStack | **Yes** | Final output. |
| 2 | `item_inputs` | Array | **Yes** | List of item inputs. |
| 3 | `block_inputs` | Array | **Yes** | List of block inputs. |
| 4 | `delta` | Integer | **Yes** | **Thermal Stress (ΔT)**. Minimum required rate difference. |

### Available Chained Methods
| Method | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `.minHot(val)` | Integer | Ignore | Minimum required rate from High Temp sources (H/t). |
| `.maxCold(val)` | Integer | Ignore | Maximum allowed rate from Low Temp sources (H/t). Should be negative. |

### Examples
```js
// 4.1 Basic Shattering
event.recipes.thermalshock.thermal_shock('sand', ['gravel'], [], 50)

// 4.2 Extreme Environment Shattering (Limited to extreme environments)
event.recipes.thermalshock.thermal_shock('obsidian', [], ['minecraft:lava'], 800)
     .minHot(1000)
     .maxCold(0)
```

---

## 5. Clump Filling (Thermal Shock) (`thermal_shock_filling`)
Encodes product data into a Material Clump via thermal shock.
*Note: The system automatically appends `thermalshock:material_clump` to the inputs.*

### Positional Arguments (Constructor)
| Pos | Name | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `target_item` | String | **Yes** | Product ID to be encoded into the clump. |
| 2 | `item_inputs` | Array | **Yes** | Item inputs. |
| 3 | `block_inputs` | Array | **Yes** | Block inputs. |
| 4 | `delta` | Integer | **Yes** | **Thermal Stress (ΔT)** required for filling. |

### Available Chained Methods
| Method | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `.targetCount(val)`| Integer | `1` | Result quantity produced when the clump is extracted. |
| `.minHot(val)` | Integer | Ignore | High temp rate requirement. |
| `.maxCold(val)` | Integer | Ignore | Low temp rate requirement. |

### Examples
```js
// 5.1 Basic Filling
event.recipes.thermalshock.thermal_shock_filling('iron_ingot', ['iron_ore'], [], 700)

// 5.2 Batch Encoding (Produces 4 results upon extraction)
event.recipes.thermalshock.thermal_shock_filling('gold_nugget', ['raw_gold'], [], 500)
     .targetCount(4)
```

---

## 6. Clump Processing (`clump_processing`)
Extracts items from a filled Material Clump.
*Note: The system automatically appends the filled `thermalshock:material_clump` to the inputs.*

### Positional Arguments (Constructor)
| Pos | Name | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `result` | ItemStack | **Yes** | Final output stack. |
| 2 | `target_item` | String | **Yes** | Only clumps encoded with this ID will be processed. |
| 3 | `item_inputs` | Array | **Yes** | Additional item consumption. |
| 4 | `block_inputs` | Array | **Yes** | Additional block consumption. |

### Available Chained Methods
| Method | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `.minHeat(val)` | Integer | `0` | Minimum net heat rate required. |
| `.heatCost(val)` | Integer | `100` | Total heat cost for extraction. |

### Examples
```js
// 6.1 Basic Extraction (Clump only)
event.recipes.thermalshock.clump_processing('gold_ingot', 'gold_ingot', [], [])

// 6.2 Reinforced Extraction (Requires extra materials and high heat)
event.recipes.thermalshock.clump_processing('netherite_ingot', 'netherite_ingot', ['gold_ingot'], [])
     .minHeat(300)
     .heatCost(5000)
```

---

## 7. Thermal Converter (`thermal_converter`)
A precision machine supporting multiple item/fluid inputs and probabilities.

### Positional Arguments (Constructor)
| Pos | Name | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `item_outputs` | Map/Array | **Yes** | **Pro Syntax**: `{ "id": chance }`. |
| 2 | `item_inputs` | Map/Array | **Yes** | **Pro Syntax**: `{ "id": consume_chance }`. |
| 3 | `process_time` | Integer | **Yes** | Base processing time in ticks. Default 20. |

### Available Chained Methods
| Method | Type | Description |
| :--- | :--- | :--- |
| `.fluidInput(val)` | String/Map | Adds fluid input. Supports `1000x water` shorthand. |
| `.fluidOutput(val)`| String/Map | Adds fluid output. Supports `100x lava` shorthand. |
| `.chance(val)` | Double | **Core**: Sets probability for the *most recently added* item/fluid. |
| `.minHeat(val)` | Integer | Sets the minimum net heat rate limit. |
| `.maxHeat(val)` | Integer | Sets the maximum net heat rate limit. |

### Examples
```js
// 7.1 Pro Mapping Syntax (Multiple outputs + probabilities)
event.recipes.thermalshock.thermal_converter(
    { "gold_ingot": 1.0, "2x gold_nugget": 0.5 }, 
    "raw_gold", 
    200
)

// 7.2 Fluid Chaining and Chance Tracking
event.recipes.thermalshock.thermal_converter("obsidian", "cobblestone", 100)
     .fluidInput("1000x water").chance(0.8) // 0.8 applied to water
     .fluidOutput("100x lava").chance(0.1)  // 0.1 applied to lava
     .minHeat(500)
```

---

## 8. Thermal Fuel (`thermal_fuel`)
Defines fuel properties for heat or cold sources.

### Positional Arguments (Constructor)
| Pos | Name | Type | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `ingredient` | Ingredient | **Yes** | Fuel item or tag. |
| 2 | `burn_time` | Integer | **Yes** | Burn duration in ticks. |
| 3 | `heat_rate` | Integer | **Yes** | Rate (H/t). Positive for heat, negative for cold. |

### Examples
```js
event.recipes.thermalshock.thermal_fuel('coal', 1600, 20) // Produces heat
event.recipes.thermalshock.thermal_fuel('blue_ice', 4800, -100) // Produces cooling
```

---

## 9. Data Maps Integration
Used to dynamically define core thermal properties for blocks and items. **Must be registered in `server_scripts`.**

### A. Structural Casing (`registerCasing`)
Defines the physical properties and input caps of a block when used as a Simulation Chamber casing.
*   `event.add(block, maxHeat, maxCold, efficiency)`
    *   `maxHeat`: **High Temp Input Cap (H/t)**. The maximum heat rate the casing can transfer.
    *   `maxCold`: **Low Temp Input Cap (H/t)**. The maximum cooling rate the casing can transfer (absolute value).
    *   `efficiency`: **Structural Efficiency (Base 1.0)**. Impacts **Catalyst Consumption Rate**. Higher efficiency means fewer catalyst points are consumed per unit produced.

### B. Simulation Catalyst (`registerCatalyst`)
`event.add(item, bonus, buffer)`
*   `bonus`: Yield bonus coefficient (0.5 = +50% yield).
*   `buffer`: The refill points provided by this item.

### C. Heat/Cold Source Rates
Defines the power output of a block when connected to a machine.

*   **Heat Source (`registerHeatSource`)**: `event.add(block, rate)` (Positive)
*   **Cold Source (`registerColdSource`)**: `event.add(block, rate)`
    *   **Note**: Pass a **positive** number (representing cooling power). The code automatically handles the subtraction logic.

```js
ThermalShockEvents.registerColdSource(event => {
    // Giving ice cooling capability: input 50, machine effectively receives -50 H/t
    event.add('minecraft:ice', 50) 
})
```

---

## 10. Summary of Helpers
*   **Placeholders**: Always use `[]` for unused positional constructor arguments.
*   **Shorthand Support**: Strings support `3x item` and `1000x fluid` formats.
*   **Simulation Upgrade Feature**: In the Simulation Chamber, any `block_inputs` that are fluid blocks will automatically support Port fluid automation when the upgrade is installed.
