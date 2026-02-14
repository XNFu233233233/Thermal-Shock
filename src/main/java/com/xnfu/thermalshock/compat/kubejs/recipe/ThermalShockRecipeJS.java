package com.xnfu.thermalshock.compat.kubejs.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xnfu.thermalshock.compat.kubejs.ThermalShockKJSSchemas;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.rhino.util.RemapForJS;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThermalShockRecipeJS extends KubeRecipe {

    private static final Pattern STACK_PATTERN = Pattern.compile("^(\\d+)[x\\s]*\\s*(.+)$");
    
    private JsonArray rawItemOutputs;
    private JsonArray rawItemInputs;
    private JsonArray rawFluidOutputs;
    private JsonArray rawFluidInputs;
    
    private String lastListKey = null;

    public ThermalShockRecipeJS() {
    }

    @Override
    public <T> KubeRecipe setValue(RecipeKey<T> key, T value) {
        // [核心] 这里的 value 现在是原始的 Map/List/String，因为 Schema 里使用了 RAW_COMPONENT
        if (key == ThermalShockKJSSchemas.CON_OUT_I) {
            rawItemOutputs = convertToRawJson(value, "item", "chance", true);
            lastListKey = "item_outputs";
            return this;
        }
        if (key == ThermalShockKJSSchemas.CON_IN_I) {
            rawItemInputs = convertToRawJson(value, "ingredient", "consume_chance", false);
            lastListKey = "item_inputs";
            return this;
        }
        if (key == ThermalShockKJSSchemas.CON_OUT_F) {
            rawFluidOutputs = convertToRawJson(value, "fluid", "chance", false);
            lastListKey = "fluid_outputs";
            return this;
        }
        if (key == ThermalShockKJSSchemas.CON_IN_F) {
            rawFluidInputs = convertToRawJson(value, "fluid", "consume_chance", false);
            lastListKey = "fluid_inputs";
            return this;
        }

        return super.setValue(key, value);
    }

    private JsonArray convertToRawJson(Object value, String valueKey, String chanceKey, boolean isItemOutput) {
        JsonArray arr = new JsonArray();
        if (value instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                arr.add(parseEntry(String.valueOf(k), ((Number) v).floatValue(), valueKey, chanceKey, isItemOutput));
            });
        } else if (value instanceof Iterable<?> list) {
            list.forEach(e -> {
                arr.add(parseEntry(String.valueOf(e), 1.0f, valueKey, chanceKey, isItemOutput));
            });
        } else {
            arr.add(parseEntry(String.valueOf(value), 1.0f, valueKey, chanceKey, isItemOutput));
        }
        return arr;
    }

    private JsonObject parseEntry(String input, float chance, String valueKey, String chanceKey, boolean isItemOutput) {
        JsonObject obj = new JsonObject();
        Matcher m = STACK_PATTERN.matcher(input);
        
        int count = 1;
        String id = input;
        if (m.find()) {
            count = Integer.parseInt(m.group(1));
            id = m.group(2);
        }

        if (valueKey.equals("fluid")) {
            JsonObject fluidObj = new JsonObject();
            fluidObj.addProperty("id", id); // 使用 id
            fluidObj.addProperty("amount", count == 1 ? 1000 : count); // 默认 1000mB
            obj.add("fluid", fluidObj);
        } else if (isItemOutput) {
            // OutputItem: { "item": { "id": "...", "count": ... }, "chance": ... }
            JsonObject stackObj = new JsonObject();
            stackObj.addProperty("id", id);
            stackObj.addProperty("count", count);
            obj.add("item", stackObj);
        } else {
            // InputItem: { "ingredient": { "item/tag": "..." }, "count": ..., "consume_chance": ... }
            JsonObject ingObj = new JsonObject();
            if (id.startsWith("#")) {
                ingObj.addProperty("tag", id.substring(1));
            } else {
                ingObj.addProperty("item", id);
            }
            obj.add("ingredient", ingObj);
            obj.addProperty("count", count);
        }
        
        obj.addProperty(chanceKey, chance);
        return obj;
    }

    @RemapForJS("minHeat")
    public ThermalShockRecipeJS minHeat(int value) {
        setValue(ThermalShockKJSSchemas.MIN_HEAT, value);
        return this;
    }

    @RemapForJS("heatCost")
    public ThermalShockRecipeJS heatCost(int value) {
        setValue(ThermalShockKJSSchemas.HEAT_COST, value);
        return this;
    }

    @RemapForJS("minHot")
    public ThermalShockRecipeJS minHot(int value) {
        setValue(ThermalShockKJSSchemas.MIN_HOT, value);
        return this;
    }

    @RemapForJS("maxCold")
    public ThermalShockRecipeJS maxCold(int value) {
        setValue(ThermalShockKJSSchemas.MAX_COLD, value);
        return this;
    }

    @RemapForJS("maxHeat")
    public ThermalShockRecipeJS maxHeat(int value) {
        setValue(ThermalShockKJSSchemas.MAX_HEAT, value);
        return this;
    }

    @RemapForJS("targetCount")
    public ThermalShockRecipeJS targetCount(int value) {
        setValue(ThermalShockKJSSchemas.TARGET_COUNT, value);
        return this;
    }

    @RemapForJS("fluidInput")
    public ThermalShockRecipeJS fluidInput(Object from) {
        if (rawFluidInputs == null) rawFluidInputs = new JsonArray();
        JsonArray newEntries = convertToRawJson(from, "fluid", "consume_chance", false);
        newEntries.forEach(rawFluidInputs::add);
        lastListKey = "fluid_inputs";
        save();
        return this;
    }

    @RemapForJS("fluidOutput")
    public ThermalShockRecipeJS fluidOutput(Object from) {
        if (rawFluidOutputs == null) rawFluidOutputs = new JsonArray();
        JsonArray newEntries = convertToRawJson(from, "fluid", "chance", false);
        newEntries.forEach(rawFluidOutputs::add);
        lastListKey = "fluid_outputs";
        save();
        return this;
    }

    @RemapForJS("chance")
    public ThermalShockRecipeJS chance(double chance) {
        JsonArray targetArray = null;
        String key = null;

        if ("item_outputs".equals(lastListKey)) { targetArray = rawItemOutputs; key = "chance"; }
        else if ("item_inputs".equals(lastListKey)) { targetArray = rawItemInputs; key = "consume_chance"; }
        else if ("fluid_outputs".equals(lastListKey)) { targetArray = rawFluidOutputs; key = "chance"; }
        else if ("fluid_inputs".equals(lastListKey)) { targetArray = rawFluidInputs; key = "consume_chance"; }

        if (targetArray != null && targetArray.size() > 0) {
            JsonObject last = targetArray.get(targetArray.size() - 1).getAsJsonObject();
            last.addProperty(key, (float) chance);
            save();
        }
        return this;
    }

    @Override
    public void serialize() {
        super.serialize();

        // 1. Inject Raw JSON Arrays
        if (rawItemOutputs != null) json.add("item_outputs", rawItemOutputs);
        if (rawItemInputs != null) json.add("item_inputs", rawItemInputs);
        if (rawFluidOutputs != null) json.add("fluid_outputs", rawFluidOutputs);
        if (rawFluidInputs != null) json.add("fluid_inputs", rawFluidInputs);

        // 2. Merge Logic for Overheating/Shock (Only if NOT converter)
        if (!json.has("type") || !json.get("type").getAsString().equals("thermalshock:thermal_converter")) {
            if (json.has("item_inputs") || json.has("block_inputs")) {
                JsonArray ingredients = new JsonArray();
                
                if (json.has("item_inputs") && json.get("item_inputs").isJsonArray()) {
                    json.getAsJsonArray("item_inputs").forEach(e -> {
                        JsonObject entry = new JsonObject();
                        entry.add("value", e);
                        entry.addProperty("type", "item");
                        ingredients.add(entry);
                    });
                }
                
                if (json.has("block_inputs") && json.get("block_inputs").isJsonArray()) {
                    json.getAsJsonArray("block_inputs").forEach(e -> {
                        JsonObject entry = new JsonObject();
                        JsonObject val = new JsonObject();
                        if (e.isJsonPrimitive()) val.addProperty("block", e.getAsString());
                        else val = e.getAsJsonObject();
                        entry.add("value", val);
                        entry.addProperty("type", "block");
                        ingredients.add(entry);
                    });
                }
                
                boolean isFilling = json.has("target_item") && !json.has("result");
                boolean isProcessing = json.has("target_item") && json.has("result");
                if (isFilling || isProcessing) {
                    JsonObject clumpEntry = new JsonObject();
                    JsonObject clumpVal = new JsonObject();
                    clumpVal.addProperty("item", "thermalshock:material_clump");
                    clumpEntry.add("value", clumpVal);
                    clumpEntry.addProperty("type", "item");
                    ingredients.add(clumpEntry);
                }

                json.add("ingredients", ingredients);
                json.remove("item_inputs");
                json.remove("block_inputs");
            }
        }
    }
}
