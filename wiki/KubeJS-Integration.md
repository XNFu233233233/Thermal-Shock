# KubeJS Integration Guide

Thermal Shock provides deep integration for KubeJS 21 in Minecraft 1.21.1.

---

## 1. Simulation Ingredients
Simulation Chamber recipes distinguish between items and blocks using a specific object structure:

| Attribute | Type | Req. | Description |
| :--- | :--- | :--- | :--- |
| `value` | String | Yes | The ID of the item or block (e.g., `'minecraft:sand'`). |
| `type` | String | Yes | Either `'item'` (entities/slots) or `'block'` (world blocks/fluids). |

---

## 2. Recipe Syntax (ServerEvents.recipes)

### A. Overheating
```javascript
event.recipes.thermalshock.overheating(ingredients, result, min_heat, heat_cost)
```

### B. Thermal Shock
```javascript
event.recipes.thermalshock.thermal_shock(ingredients, result, min_hot, max_cold, delta)
```

### C. Clump Filling
```javascript
event.recipes.thermalshock.thermal_shock_filling(ingredients, target_result, min_hot, max_cold, delta, clump_min_heat, clump_heat_cost)
```

### D. Clump Processing (Extraction)
```javascript
event.recipes.thermalshock.extraction(ingredients, target_content, min_heat, heat_cost)
```

### E. Thermal Converter
```javascript
event.recipes.thermalshock.thermal_converter(item_inputs, fluid_inputs, item_outputs, fluid_outputs, process_time, min_heat, max_heat)
```

---

## 3. Script Examples

```javascript
ServerEvents.recipes(event => {
    const { thermalshock } = event.recipes

    // Overheating: Sand -> Glass
    thermalshock.overheating(
        [{ value: 'minecraft:sand', type: 'item' }],
        'minecraft:glass', 50, 200
    )

    // Converter: Cobblestone -> Lava
    thermalshock.thermal_converter(
        [{ ingredient: 'minecraft:cobblestone', count: 1, consume_chance: 1.0 }],
        [], [],
        [{ fluid: 'minecraft:lava', amount: 250, chance: 1.0 }],
        200, 500, 2000
    )
})
```

---

## 4. Modifying Data Maps (ServerEvents.highPriorityData)

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