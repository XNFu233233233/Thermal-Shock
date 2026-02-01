# Recipes Reference

This guide provides a detailed breakdown of all custom recipe types in Thermal Shock. Each section includes a full attribute reference and a JSON example.

---

## 1. Basic Processing (Standard Mode)

### A. Overheating (`thermalshock:overheating`)
Used for smelting or converting items using high heat.
*   **Input Limit**: Maximum **3** items.
*   **Attributes**:
    *   `ingredients`: (Array) List of input items. Use `{"type": "item", "value": {"item": "id"}}`.
    *   `result`: (Object) The resulting item. `{"id": "id", "count": n}`.
    *   `min_heat`: (Integer) The minimum heat rate (H/t) required from the environment to start.
    *   `heat_cost`: (Integer) Total accumulated heat (H) required to complete the recipe.

**Example**:
```json
{
  "type": "thermalshock:overheating",
  "ingredients": [{ "type": "item", "value": { "item": "minecraft:sand" } }],
  "result": { "id": "minecraft:glass", "count": 1 },
  "min_heat": 50,
  "heat_cost": 200
}
```

### B. Thermal Shock (`thermalshock:thermal_shock`)
Shatters items using rapid temperature changes (ΔT).
*   **Input Limit**: Maximum **3** items.
*   **Attributes**:
    *   `ingredients`: (Array) Input items.
    *   `result`: (Object) Resulting item.
    *   `min_hot`: (Integer) Minimum high temperature (H) required in the chamber.
    *   `max_cold`: (Integer) Maximum cold temperature (H) allowed (usually negative).
    *   `delta`: (Integer) The required difference between high and low sources (|Hot - Cold|).

**Example**:
```json
{
  "type": "thermalshock:thermal_shock",
  "ingredients": [{ "type": "item", "value": { "item": "minecraft:cobblestone" } }],
  "result": { "id": "minecraft:gravel", "count": 1 },
  "min_hot": 100,
  "max_cold": -50,
  "delta": 150
}
```

---

## 2. Advanced Processing (Data-Driven Clumps)

### A. Clump Filling Machine (`thermalshock:thermal_shock_filling`)
Injects data into a Material Clump using Thermal Shock.
*   **Input Limit**: Maximum **4** items (including the clump).
*   **Attributes**:
    *   `ingredients`: (Array) Inputs. Usually includes an empty clump.
    *   `target_result`: (Object) The item data to be encoded into the clump.
    *   `min_hot`/`max_cold`/`delta`: Standard thermal shock requirements.
    *   `clump_min_heat`: The `min_heat` value written into the new clump.
    *   `clump_heat_cost`: The `heat_cost` value written into the new clump.

### B. Clump Extraction (`thermalshock:clump_processing`)
Restores items from a filled Clump.
*   **Input Limit**: Maximum **4** items.
*   **Attributes**:
    *   `ingredients`: (Array) Inputs. Must include the filled clump.
    *   `target_content`: (Object) The specific item data the clump must contain to match this recipe.
    *   `min_heat`: (Integer) Minimum heat rate for extraction.
    *   `heat_cost`: (Integer) Heat required to finish extraction.

---

## 3. Support Types

### Thermal Fuel (`thermalshock:thermal_fuel`)
*   `ingredient`: The fuel item.
*   `burn_time`: Duration in ticks.
*   `heat_rate`: Output rate (Positive = Heater, Negative = Freezer).

### Thermal Converter (`thermalshock:thermal_converter`)
Complex multi-tank processing.
*   `item_inputs` / `fluid_inputs`: Arrays of input objects.
*   `item_outputs` / `fluid_outputs`: Arrays of output objects with `chance`.
*   `process_time`: Duration in ticks.
