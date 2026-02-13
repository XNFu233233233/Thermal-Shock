package com.xnfu.thermalshock.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xnfu.thermalshock.ThermalShock;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.component.RecipeValidationContext;
import dev.latvian.mods.kubejs.recipe.schema.postprocessing.RecipePostProcessor;
import dev.latvian.mods.kubejs.recipe.schema.postprocessing.RecipePostProcessorType;
import dev.latvian.mods.kubejs.error.RecipeComponentException;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.MapCodec;

public enum SimulationPostProcessor implements RecipePostProcessor {
    INSTANCE;

    public static final RecipePostProcessorType<SimulationPostProcessor> TYPE = new RecipePostProcessorType<>(
        ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "simulation_processor"),
        ctx -> MapCodec.unit(INSTANCE)
    );

    @Override
    public RecipePostProcessorType<?> type() {
        return TYPE;
    }

    @Override
    public void process(RecipeValidationContext ctx, KubeRecipe recipe) {
        JsonObject json = recipe.json;
        
        boolean isFilling = json.has("target_result");
        boolean isProcessing = json.has("target_content");

        // 安全获取数组，如果不存在则视为空
        JsonArray itemIn = json.has("item_inputs") ? json.getAsJsonArray("item_inputs") : new JsonArray();
        JsonArray blockIn = json.has("block_inputs") ? json.getAsJsonArray("block_inputs") : new JsonArray();

        // 基础校验：非团块配方必须有输入
        if (!isFilling && !isProcessing && itemIn.isEmpty() && blockIn.isEmpty()) {
            throw new RecipeComponentException("Recipe needs at least one item or block input!", null, null).source(recipe.sourceLine);
        }

        JsonArray ingredients = new JsonArray();
        
        // 转换物品
        itemIn.forEach(e -> {
            JsonObject entry = new JsonObject();
            entry.add("value", e);
            entry.addProperty("type", "item");
            ingredients.add(entry);
        });

        // 转换方块
        blockIn.forEach(e -> {
            JsonObject entry = new JsonObject();
            JsonObject val = new JsonObject();
            if (e.isJsonPrimitive()) {
                val.addProperty("block", e.getAsString());
            } else if (e.isJsonObject()) {
                var obj = e.getAsJsonObject();
                if (obj.has("block")) {
                    val.addProperty("block", obj.get("block").getAsString());
                } else if (obj.has("item")) {
                    val.addProperty("block", obj.get("item").getAsString());
                } else {
                    val = obj;
                }
            }
            entry.add("value", val);
            entry.addProperty("type", "block");
            ingredients.add(entry);
        });

        // 填充/提取配方自动补全
        if (isFilling || isProcessing) {
            JsonObject clumpEntry = new JsonObject();
            JsonObject clumpVal = new JsonObject();
            clumpVal.addProperty("item", "thermalshock:material_clump");
            clumpEntry.add("value", clumpVal);
            clumpEntry.addProperty("type", "item");
            ingredients.add(clumpEntry);
        }

        // 强制写入 ingredients，防止 "No key ingredients" 错误
        json.add("ingredients", ingredients);
        
        // 清理临时键
        json.remove("item_inputs");
        json.remove("block_inputs");
    }
}
