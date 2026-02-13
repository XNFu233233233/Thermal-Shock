package com.xnfu.thermalshock.compat.kubejs.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import net.minecraft.resources.ResourceLocation;

public class ThermalShockRecipeJS extends KubeRecipe {

    // Must be public no-arg constructor for KubeRecipeFactory
    public ThermalShockRecipeJS() {
    }

    @Override
    public void serialize() {
        super.serialize(); // Populates 'this.json' from valueMap

        // Custom Merge Logic
        if (json.has("item_inputs") || json.has("block_inputs")) {
            boolean isFilling = json.has("target_result");
            boolean isProcessing = json.has("target_content");

            JsonArray itemIn = json.has("item_inputs") ? json.getAsJsonArray("item_inputs") : new JsonArray();
            JsonArray blockIn = json.has("block_inputs") ? json.getAsJsonArray("block_inputs") : new JsonArray();

            JsonArray ingredients = new JsonArray();

            itemIn.forEach(e -> {
                JsonObject entry = new JsonObject();
                entry.add("value", e);
                entry.addProperty("type", "item");
                ingredients.add(entry);
            });

            blockIn.forEach(e -> {
                JsonObject entry = new JsonObject();
                JsonObject val = new JsonObject();
                if (e.isJsonPrimitive()) {
                    val.addProperty("block", e.getAsString());
                } else if (e.isJsonObject()) {
                    var obj = e.getAsJsonObject();
                    if (obj.has("block")) {
                        val.addProperty("block", obj.get("block").getAsString());
                    } else {
                        val = obj;
                    }
                }
                entry.add("value", val);
                entry.addProperty("type", "block");
                ingredients.add(entry);
            });

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
