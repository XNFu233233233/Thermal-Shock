# Data Maps Reference

Thermal Shock utilizes NeoForge Data Maps to allow flexible configuration of block and item properties.

## 1. Chamber Casings (`thermalshock:chamber_casing`)
Determines which blocks can form the outer shell of a Simulation Chamber.

**JSON Path**: `data/thermalshock/data_maps/block/chamber_casing.json`

| Field | Type | Description |
| :--- | :--- | :--- |
| `max_heat_rate` | Integer | Max H/t the casing can transfer from hot sources. |
| `max_cold_rate` | Integer | Max H/t the casing can transfer from cold sources (absolute). |
| `efficiency` | Float | (Optional) Multiplier for catalyst consumption (Default: 1.0). |

---

## 2. Heat & Cold Sources
Define custom blocks as environmental heat or cold sources.

*   **Heat Sources**: `data/thermalshock/data_maps/block/heat_source.json`
    *   Field: `heat_per_tick` (Integer)
*   **Cold Sources**: `data/thermalshock/data_maps/block/cold_source.json`
    *   Field: `cooling_per_tick` (Integer)

---

## 3. Catalysts (`thermalshock:chamber_catalyst`)
Define items used in the Simulation Chamber's catalyst slot.

**JSON Path**: `data/thermalshock/data_maps/item/chamber_catalyst.json`

| Field | Type | Description |
| :--- | :--- | :--- |
| `bonus_yield` | Float | The additional yield chance (e.g., 0.5 = +50%). |
| `buffer_amount` | Float | Points added to the buffer per consumed item. |