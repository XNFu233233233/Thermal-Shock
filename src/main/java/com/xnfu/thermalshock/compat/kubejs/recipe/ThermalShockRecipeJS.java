package com.xnfu.thermalshock.compat.kubejs.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xnfu.thermalshock.compat.kubejs.ThermalShockKJSSchemas;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.rhino.util.RemapForJS;

import java.util.List;
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
        // 1. RAW Converter Components (单数 Key 处理)
        if (key == ThermalShockKJSSchemas.CON_OUT_I) {
            rawItemOutputs = convertToRawJson(value, "item", "chance", true);
            lastListKey = "item_output";
            return this;
        }
        if (key == ThermalShockKJSSchemas.CON_IN_I) {
            rawItemInputs = convertToRawJson(value, "ingredient", "consume_chance", false);
            lastListKey = "item_input";
            return this;
        }
        if (key == ThermalShockKJSSchemas.CON_OUT_F) {
            rawFluidOutputs = convertToRawJson(value, "fluid", "chance", false);
            lastListKey = "fluid_output";
            return this;
        }
        if (key == ThermalShockKJSSchemas.CON_IN_F) {
            rawFluidInputs = convertToRawJson(value, "fluid", "consume_chance", false);
            lastListKey = "fluid_input";
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
            fluidObj.addProperty("id", id);
            fluidObj.addProperty("amount", count == 1 ? 1000 : count);
            obj.add("fluid", fluidObj);
        } else if (isItemOutput) {
            JsonObject stackObj = new JsonObject();
            stackObj.addProperty("id", id);
            stackObj.addProperty("count", count);
            obj.add("item", stackObj);
        } else {
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

    // === 统一链式方法 ===

    @RemapForJS("minHeat")
    public ThermalShockRecipeJS minHeat(int value) {
        setValue(ThermalShockKJSSchemas.MIN_HEAT, value);
        return this;
    }

    @RemapForJS("maxHeat")
    public ThermalShockRecipeJS maxHeat(int value) {
        setValue(ThermalShockKJSSchemas.MAX_HEAT, value);
        return this;
    }

    @RemapForJS("heatCost")
    public ThermalShockRecipeJS heatCost(int value) {
        setValue(ThermalShockKJSSchemas.HEAT_COST, value);
        return this;
    }

    @RemapForJS("maxCold")
    public ThermalShockRecipeJS maxCold(int value) {
        setValue(ThermalShockKJSSchemas.MAX_COLD, value);
        return this;
    }

    @RemapForJS("targetCount")
    public ThermalShockRecipeJS targetCount(int value) {
        setValue(ThermalShockKJSSchemas.TARGET_COUNT, value);
        return this;
    }

    // 手动覆盖流体方法，确保统一为单数
    @RemapForJS("fluidInput")
    public ThermalShockRecipeJS fluidInput(Object from) {
        if (rawFluidInputs == null) rawFluidInputs = new JsonArray();
        JsonArray newEntries = convertToRawJson(from, "fluid", "consume_chance", false);
        newEntries.forEach(rawFluidInputs::add);
        lastListKey = "fluid_input";
        save();
        return this;
    }

    @RemapForJS("fluidOutput")
    public ThermalShockRecipeJS fluidOutput(Object from) {
        if (rawFluidOutputs == null) rawFluidOutputs = new JsonArray();
        JsonArray newEntries = convertToRawJson(from, "fluid", "chance", false);
        newEntries.forEach(rawFluidOutputs::add);
        lastListKey = "fluid_output";
        save();
        return this;
    }

    @RemapForJS("chance")
    public ThermalShockRecipeJS chance(double chance) {
        JsonArray targetArray = null;
        String key = null;

        if ("item_output".equals(lastListKey)) { targetArray = rawItemOutputs; key = "chance"; }
        else if ("item_input".equals(lastListKey)) { targetArray = rawItemInputs; key = "consume_chance"; }
        else if ("fluid_output".equals(lastListKey)) { targetArray = rawFluidOutputs; key = "chance"; }
        else if ("fluid_input".equals(lastListKey)) { targetArray = rawFluidInputs; key = "consume_chance"; }

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

        // 1. 将 RAW JSON 数组映射回复数键 (为了兼容模组 Recipe 类)
        if (rawItemOutputs != null) json.add("item_outputs", rawItemOutputs);
        if (rawItemInputs != null) json.add("item_inputs", rawItemInputs);
        if (rawFluidOutputs != null) json.add("fluid_outputs", rawFluidOutputs);
        if (rawFluidInputs != null) json.add("fluid_inputs", rawFluidInputs);
        
        // 清理单数 Key (避免干扰)
        json.remove("item_output");
        json.remove("item_input");
        json.remove("fluid_output");
        json.remove("fluid_input");

        // 2. 特殊逻辑：Clump Filling 处理
        String typeId = (this.type != null && this.type.id != null) ? this.type.id.toString() : "";
        if (typeId.equals("thermalshock:clump_filling")) {
            if (json.has("result")) {
                var result = json.get("result");
                String targetId = "";
                if (result.isJsonPrimitive()) {
                    targetId = result.getAsString();
                } else if (result.isJsonObject()) {
                    var obj = result.getAsJsonObject();
                    if (obj.has("item")) targetId = obj.get("item").getAsString();
                    else if (obj.has("id")) targetId = obj.get("id").getAsString();
                }
                if (!targetId.isEmpty()) {
                    json.addProperty("target_item", targetId);
                }
            }

            JsonObject keyMap = json.has("key") ? json.getAsJsonObject("key") : new JsonObject();
            if (!keyMap.has("C")) {
                JsonObject clumpIngredient = new JsonObject();
                clumpIngredient.addProperty("item", "thermalshock:material_clump");
                keyMap.add("C", clumpIngredient);
            }
            json.add("key", keyMap);
        }

        // 3. 合并逻辑与兼容性处理
        if (!typeId.isEmpty()) {
            // 兼容性：将 min_heat 映射回 min_hot (如果类型是 thermal_shock)
            if (typeId.equals("thermalshock:thermal_shock") || typeId.equals("thermalshock:thermal_shock_filling")) {
                if (json.has("min_heat")) {
                    json.add("min_hot", json.get("min_heat"));
                    json.remove("min_heat");
                }
            }

            if (!typeId.equals("thermalshock:thermal_converter") && !typeId.equals("thermalshock:clump_filling")) {
                mergeIngredientsToPool();
            }
        }
    }

    private void mergeIngredientsToPool() {
        // 兼容性：检查 item_input 或 item_inputs
        String itemK = json.has("item_input") ? "item_input" : "item_inputs";
        String blockK = json.has("block_input") ? "block_input" : "block_inputs";

        if (json.has(itemK) || json.has(blockK)) {
            JsonArray ingredients = new JsonArray();
            
            if (json.has(itemK) && json.get(itemK).isJsonArray()) {
                json.getAsJsonArray(itemK).forEach(e -> {
                    JsonObject entry = new JsonObject();
                    entry.add("value", e);
                    entry.addProperty("type", "item");
                    ingredients.add(entry);
                });
            }
            
            if (json.has(blockK) && json.get(blockK).isJsonArray()) {
                json.getAsJsonArray(blockK).forEach(e -> {
                    JsonObject entry = new JsonObject();
                    JsonObject val = new JsonObject();
                    if (e.isJsonPrimitive()) val.addProperty("block", e.getAsString());
                    else val = e.getAsJsonObject();
                    entry.add("value", val);
                    entry.addProperty("type", "block");
                    ingredients.add(entry);
                });
            }
            
            // ... (省略 clump 注入逻辑，已在 serialize 之后)
            json.add("ingredients", ingredients);
            json.remove(itemK);
            json.remove(blockK);
        }
    }
}
