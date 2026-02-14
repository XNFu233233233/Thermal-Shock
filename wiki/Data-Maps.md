# Data Maps Technical Reference

Thermal Shock utilizes NeoForge Data Maps to allow modpack authors to flexibly configure properties for various blocks and items.

---

## 1. Structural Casing (`thermalshock:chamber_casing`)
Defines which blocks can be used as the outer shell of a Simulation Chamber.

### Property List
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `max_heat_rate` | Integer | Yes | Maximum H/t this casing can transfer from positive heat sources. |
| `max_cold_rate` | Integer | Yes | Maximum H/t this casing can transfer from cold sources (absolute value). |
| `efficiency` | Float | No | Structural efficiency coefficient (Default: 1.0). Higher values result in slower consumption (e.g., 2.0 means consumption is halved). |

### Complete Example
**Path**: `data/thermalshock/data_maps/block/chamber_casing.json`
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

## 2. Catalyst (`thermalshock:chamber_catalyst`)
Defines catalyst items that can be placed in the Simulation Chamber's dedicated slot.

### Property List
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `bonus_yield` | Float | Yes | Yield bonus (e.g., 0.5 represents a 50% increase in output). |
| `buffer_amount` | Float | No | Catalyst points added per item consumed (Default: 10.0). |

### Complete Example
**Path**: `data/thermalshock/data_maps/item/chamber_catalyst.json`
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

## 3. Environmental Heat Source (`thermalshock:heat_source`)
Defines custom blocks as environmental heat sources.

### Property List
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `heat_per_tick` | Integer | Yes | Positive heat value provided per tick. |

### Complete Example
**Path**: `data/thermalshock/data_maps/block/heat_source.json`
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

## 4. Environmental Cold Source (`thermalshock:cold_source`)
Defines custom blocks as environmental cold sources.

### Property List
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `cooling_per_tick` | Integer | Yes | Cooling value provided per tick (absolute value). |

### Complete Example
**Path**: `data/thermalshock/data_maps/block/cold_source.json`
```json
{
  "values": {
    "minecraft:packed_ice": {
      "cooling_per_tick": 10
    }
  }
}
```
