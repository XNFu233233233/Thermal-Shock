package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.*;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.registries = registries;
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Safe way to get registries in NeoForge 1.21.1 Datagen
        HolderLookup.Provider lookup = this.registries.join();
        HolderGetter<Item> itemGetter = lookup.lookupOrThrow(Registries.ITEM);

        RecipeOutput testOutput = output.withConditions(net.neoforged.neoforge.common.conditions.FalseCondition.INSTANCE);

        // 1. Overheating
        buildOverheating(testOutput, "iron_nuggets_processing", Ingredient.of(Items.IRON_NUGGET), 3, new ItemStack(Items.IRON_NUGGET), 200, 50, false);
        buildOverheating(testOutput, "log_to_charcoal", Ingredient.of(net.minecraft.tags.ItemTags.LOGS), 1, new ItemStack(Items.CHARCOAL, 2), 100, 200, true);
        buildOverheating(testOutput, "wet_sponge_drying", Ingredient.of(Blocks.WET_SPONGE), 1, new ItemStack(Blocks.SPONGE), 100, 500, true);
        buildOverheating(testOutput, "lava_to_obsidian", Ingredient.of(Items.LAVA_BUCKET), 1, new ItemStack(Blocks.OBSIDIAN), 1000, 5000, true);

        // 2. Thermal Shock
        buildShock(testOutput, "dirt_to_clay", Ingredient.of(Blocks.DIRT), 1, new ItemStack(Items.CLAY_BALL, 4), 100, 10, 150, true);
        buildShock(testOutput, "cobble_to_gravel", Ingredient.of(Blocks.COBBLESTONE), 1, new ItemStack(Blocks.GRAVEL), 200, 0, 300, true);
        buildShock(testOutput, "water_to_ice", Ingredient.of(Items.WATER_BUCKET), 1, new ItemStack(Blocks.ICE), 50, -50, 100, true);
        buildShock(testOutput, "glass_cutting", Ingredient.of(Blocks.GLASS), 1, new ItemStack(Blocks.GLASS_PANE, 4), 150, 20, 200, false);

        // 3. Clump Filling
        buildFillingCrafting(testOutput, "iron_filling_crafting", Ingredient.of(Items.IRON_ORE), getHolder(itemGetter, Items.IRON_INGOT));
        buildFillingCrafting(testOutput, "gold_filling_crafting", Ingredient.of(Items.GOLD_ORE), getHolder(itemGetter, Items.GOLD_INGOT));
        buildFillingShock(testOutput, "iron_nugget_filling", Ingredient.of(Items.IRON_BARS), getHolder(itemGetter, Items.IRON_NUGGET), 100, 10, 150);

        // 4. Clump Processing
        buildClumpProcessing(testOutput, "extract_iron_ingot", getHolder(itemGetter, Items.IRON_INGOT), 200, 1000);
        buildClumpProcessing(testOutput, "extract_gold_ingot", getHolder(itemGetter, Items.GOLD_INGOT), 500, 2000);
        buildClumpProcessing(testOutput, "extract_iron_nugget", getHolder(itemGetter, Items.IRON_NUGGET), 100, 500);

        // 5. Fuel
        buildFuel(testOutput, "fuel_coal", Ingredient.of(Items.COAL), 1600, 200);
        buildFuel(testOutput, "fuel_blaze_rod", Ingredient.of(Items.BLAZE_ROD), 2400, 500);
        buildFuel(testOutput, "fuel_lava_bucket", Ingredient.of(Items.LAVA_BUCKET), 20000, 1000);
        buildFuel(testOutput, "fuel_magma_cream", Ingredient.of(Items.MAGMA_CREAM), 1200, 300);
        buildFuel(testOutput, "coolant_snowball", Ingredient.of(Items.SNOWBALL), 200, -50);
        buildFuel(testOutput, "coolant_ice", Ingredient.of(Items.ICE), 1200, -200);
        buildFuel(testOutput, "coolant_packed_ice", Ingredient.of(Blocks.PACKED_ICE), 4800, -350);
        buildFuel(testOutput, "coolant_blue_ice", Ingredient.of(Blocks.BLUE_ICE), 19200, -500);

        // 6. Converter
        ThermalConverterRecipeBuilder.create().inputItem(Ingredient.of(Blocks.COBBLESTONE), 1, 1.0f).outputFluid(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 250), 1.0f).minHeat(500).time(200).save(testOutput, loc("converter/melt_cobble"));
        ThermalConverterRecipeBuilder.create().inputFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1.0f).outputItem(new ItemStack(Blocks.ICE), 1.0f).maxHeat(0).time(100).save(testOutput, loc("converter/freeze_water"));
        ThermalConverterRecipeBuilder.create().inputItem(Ingredient.of(Blocks.WET_SPONGE), 1, 1.0f).outputItem(new ItemStack(Blocks.SPONGE), 1.0f).outputFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1.0f).minHeat(50).time(60).save(testOutput, loc("converter/dry_sponge"));
        ThermalConverterRecipeBuilder.create().inputItem(Ingredient.of(Blocks.GRAVEL), 1, 1.0f).outputItem(new ItemStack(Blocks.SAND), 1.0f).outputItem(new ItemStack(Items.IRON_NUGGET), 0.1f).minHeat(200).time(80).save(testOutput, loc("converter/sift_gravel"));
    }

    private Holder<Item> getHolder(HolderGetter<Item> getter, Item item) {
        return getter.getOrThrow(BuiltInRegistries.ITEM.getResourceKey(item).get());
    }

    private ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "dev_test/" + path);
    }

    private void buildOverheating(RecipeOutput output, String name, Ingredient input, int count, ItemStack result, int minHeat, int cost, boolean isBlock) {
        var builder = ThermalShockRecipeBuilder.overheating(result);
        for(int i=0; i<count; i++) {
            if (isBlock) builder.inputBlock(input);
            else builder.inputItem(input);
        }
        builder.setOverheatingParams(minHeat, cost).save(output, loc("overheating/" + name));
    }

    private void buildShock(RecipeOutput output, String name, Ingredient input, int count, ItemStack result, int minHot, int maxCold, int delta, boolean isBlock) {
        var builder = ThermalShockRecipeBuilder.thermalShock(result);
        for(int i=0; i<count; i++) {
            if (isBlock) builder.inputBlock(input);
            else builder.inputItem(input);
        }
        builder.setThermalShockParams(minHot, maxCold, delta).save(output, loc("shock/" + name));
    }

    private void buildFillingCrafting(RecipeOutput output, String name, Ingredient input, Holder<Item> target) {
        ClumpFillingRecipe recipe = new ClumpFillingRecipe(
                "clump_filling",
                CraftingBookCategory.MISC,
                ShapedRecipePattern.of(
                        Map.of('I', input, 'C', Ingredient.of(ThermalShockItems.MATERIAL_CLUMP.get())),
                        List.of("I", "C")
                ),
                target
        );
        output.accept(loc("filling/" + name), recipe, null);
    }

    private void buildFillingShock(RecipeOutput output, String name, Ingredient input, Holder<Item> target, int minHot, int maxCold, int delta) {
        new ThermalShockFillingRecipeBuilder(input, target, 1, minHot, maxCold, delta)
                .save(output, loc("filling/" + name));
    }

    private void buildClumpProcessing(RecipeOutput output, String name, Holder<Item> target, int minHeat, int cost) {
        ClumpProcessingRecipe recipe = new ClumpProcessingRecipe(
                List.of(new SimulationIngredient(Ingredient.of(ThermalShockItems.MATERIAL_CLUMP.get()), RecipeSourceType.ITEM)),
                target, minHeat, cost
        );
        output.accept(loc("extraction/" + name), recipe, null);
    }

    private void buildFuel(RecipeOutput output, String name, Ingredient input, int time, int rate) {
        ThermalFuelRecipeBuilder.fuel(input, time, rate).save(output, loc("fuel/" + name));
    }
}
