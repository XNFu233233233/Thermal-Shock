# Custom Recipe Reference (JSON)

This guide details all custom recipe types and their complete JSON structures for the Thermal Shock mod.

---

### 📖 Quick Navigation
*   [`overheating`](#overheating)
*   [`thermal_shock`](#thermal_shock)
*   [`thermal_shock_filling`](#thermal_shock_filling)
*   [`clump_processing`](#clump_processing)
*   [`thermal_fuel`](#thermal_fuel)
*   [`thermal_converter`](#thermal_converter)
*   [`clump_filling`](#clump_filling)

---

## overheating
Uses sustained high temperature to melt or transform items/blocks.

### Properties
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | Array | Yes | - | Input list. Supports `item` or `block` types. |
| `result` | Object | Yes | - | Output item. Standard ItemStack format. |
| `min_heat` | Integer | No | `0` | Minimum heat rate (H/t) required to start. |
| `heat_cost` | Integer | No | `100` | Total heat (H) required to complete the recipe. |

### Example
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
Uses the temperature difference (ΔT) between extreme hot and cold sources to shatter materials.

### Properties
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | Array | Yes | - | Input list. Supports `item` or `block` types. |
| `result` | Object | Yes | - | Output item. Standard ItemStack format. |
| `delta` | Integer | **Yes** | - | Minimum absolute temperature difference (|Hot - Cold|). |
| `min_hot` | Integer | No | `-2147483648` | Minimum hot temperature value required. |
| `max_cold` | Integer | No | `2147483647` | Maximum cold temperature value allowed. |

### Example
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
Encodes product data into a Material Clump via thermal shock. **Input must include a clump item.**

### Properties
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | Array | Yes | - | Input list. Must contain `thermalshock:material_clump`. |
| `target_item` | String | Yes | - | The product ID encoded into the clump. |
| `count` | Integer | No | `1` | The quantity of the product encoded. |
| `delta` | Integer | **Yes** | - | Minimum ΔT required for filling. |

### Example
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
Extracts items from a filled Material Clump.

### Properties
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `ingredients` | Array | Yes | - | Input list. Must contain a filled clump. |
| `target_item` | String | Yes | - | Product ID inside the clump to match this recipe. |
| `min_heat` | Integer | No | `0` | Minimum heat rate required for extraction. |
| `heat_cost` | Integer | No | `100` | Total heat cost for extraction. |

### Example
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
Defines fuel properties for heat or cold sources.

### Example
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
A precision machine supporting multiple item/fluid inputs and probabilistic outputs.

### Internal Object Structures

#### Item Input (`item_inputs`)
```json
{
  "ingredient": { "item": "..." },
  "count": 1,
  "consume_chance": 1.0
}
```

#### Item Output (`item_outputs`)
```json
{
  "item": { "id": "...", "count": 1 },
  "chance": 1.0
}
```

#### Fluid Entry (`fluid_inputs` / `fluid_outputs`)
```json
{
  "fluid": { "id": "...", "amount": 1000 },
  "consume_chance": 1.0,
  "chance": 1.0
}
```

### Complex Example
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
Shaped crafting recipe to manually fill a clump at a crafting table.

### Example
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
