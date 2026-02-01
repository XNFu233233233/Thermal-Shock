# Thermal Shock - KubeJS Integration Wiki

Thermal Shock provides native integration for KubeJS in NeoForge 1.21.1. This allows modpack authors to define custom recipes and modify mod data using JavaScript.

## 1. Recipe Schemas

You can use `event.recipes.thermalshock` in your `ServerEvents.recipes` scripts.

### 1.1 Overheating Processing (过热模式)
Processes ingredients based on heat input rate.

**Syntax:**
```javascript
event.recipes.thermalshock.overheating(ingredients, result, min_temp, heat_cost)
```

*   `ingredients`: Array of `SimulationIngredient` (See Section 2).
*   `result`: Resulting item (Item ID or ItemStack).
*   `min_temp`: Minimum heat input rate required (H/t).
*   `heat_cost`: Heat consumed per operation (H).

---

### 1.2 Thermal Shock (热冲击模式)
Shathers ingredients using temperature difference (ΔT).

**Syntax:**
```javascript
event.recipes.thermalshock.thermal_shock(ingredients, result, min_hot, max_cold, delta)
```

*   `min_hot`: Minimum high temperature required (H).
*   `max_cold`: Maximum cold temperature required (H, usually negative).
*   `delta`: Minimum temperature difference (ΔT) required.

---

### 1.3 Material Clump Filling (物质填充 - 热冲击)
Creates a Material Clump with specific processing requirements.

**Syntax:**
```javascript
event.recipes.thermalshock.thermal_shock_filling(ingredients, target_result, min_hot, max_cold, delta, clump_min_temp, clump_heat_cost)
```

*   `clump_min_temp`: The `min_temp` requirement stored in the resulting Clump.
*   `clump_heat_cost`: The `heat_cost` stored in the resulting Clump.

---

### 1.4 Thermal Fuel (热源燃料)
Defines how items act as fuel for Heaters/Freezers.

**Syntax:**
```javascript
event.recipes.thermalshock.thermal_fuel(ingredient, burn_time, heat_rate)
```

*   `heat_rate`: Positive for Heaters, negative for Freezers.

---

## 2. Simulation Ingredients

Every recipe in the Simulation Chamber requires specifying the source type:
- `type: 'item'`: Matches an Item Entity or Port Item Slot.
- `type: 'block'`: Matches a Block in the chamber or a Port Fluid (if it's a bucket).

**Example:**
```javascript
{ value: 'minecraft:iron_ingot', type: 'item' }
```

---

## 3. Data Maps Integration

Thermal Shock uses NeoForge Data Maps. You can modify them via `ServerEvents.highPriorityData`:

### 3.1 Chamber Casings
Define custom blocks as structure casings.

**Path**: `thermalshock:data_maps/block/chamber_casing.json`

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

---

## 4. Item Components (NBT)

Material Clumps store their data in `thermalshock:target_output` component.

```javascript
Item.of('thermalshock:material_clump').withComponent('thermalshock:target_output', {
    result: { id: 'minecraft:iron_ingot', count: 1 },
    min_temp: 200,
    heat_cost: 1000
})
```
