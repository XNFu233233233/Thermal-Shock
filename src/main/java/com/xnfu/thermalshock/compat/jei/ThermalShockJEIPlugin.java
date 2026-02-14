package com.xnfu.thermalshock.compat.jei;

import com.xnfu.thermalshock.Config;
import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.compat.jei.category.*;
import com.xnfu.thermalshock.recipe.*;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.ArrayList;
import java.util.List;

import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.item.MaterialClumpItem;
import mezz.jei.api.registration.*;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.constants.VanillaTypes;

@JeiPlugin
public class ThermalShockJEIPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "jei_plugin");

    // === Subtype Interpreter ===
    private static class ClumpSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
        @Override
        public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
            var info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
            return info != null ? info : "";
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
            var info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
            if (info == null) return "";
            return info.item().getRegisteredName() + ":" + info.count();
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ThermalShockItems.MATERIAL_CLUMP.get(), new ClumpSubtypeInterpreter());
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        List<ItemStack> clumpVariants = new ArrayList<>();
        var rm = level.getRecipeManager();

        rm.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof ClumpFillingRecipe)
                .forEach(r -> clumpVariants.add(r.getResultItem(level.registryAccess())));

        rm.getAllRecipesFor(ThermalShockRecipes.THERMAL_SHOCK_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof ThermalShockFillingRecipe)
                .forEach(r -> clumpVariants.add(r.getResultItem(level.registryAccess())));

        if (!clumpVariants.isEmpty()) {
            registration.addExtraItemStacks(clumpVariants);
        }
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        // [MODIFIED] Moved dynamic ingredients to registerExtraIngredients
    }

    // === Simulation Chamber RecipeTypes ===
    public static final RecipeType<OverheatingRecipe> TYPE_OVERHEATING = SimulationOverheatingCategory.TYPE;
    public static final RecipeType<ThermalShockRecipe> TYPE_SHOCK = SimulationShockCategory.TYPE;
    public static final RecipeType<ThermalShockFillingRecipe> TYPE_FILLING_SHOCK = ClumpFillingShockCategory.TYPE;
    public static final RecipeType<ClumpFillingRecipe> TYPE_FILLING_CRAFTING = ClumpFillingCraftingCategory.TYPE;
    public static final RecipeType<ClumpProcessingRecipe> TYPE_EXTRACTION = ClumpExtractionCategory.TYPE;

    // === Generator & Converter RecipeTypes ===
    public static final RecipeType<ThermalFuelRecipe> TYPE_HEATER_FUEL = RecipeType.create(ThermalShock.MODID, "heater_fuel", ThermalFuelRecipe.class);
    public static final RecipeType<ThermalFuelRecipe> TYPE_FREEZER_FUEL = RecipeType.create(ThermalShock.MODID, "freezer_fuel", ThermalFuelRecipe.class);
    public static final RecipeType<ThermalConverterRecipe> TYPE_CONVERTER = RecipeType.create(ThermalShock.MODID, "thermal_converter", ThermalConverterRecipe.class);

    // === Data Maps RecipeTypes ===
    public static final RecipeType<DataMapEntries.CasingEntry> TYPE_MAP_CASING = RecipeType.create(ThermalShock.MODID, "map_casing", DataMapEntries.CasingEntry.class);
    public static final RecipeType<DataMapEntries.CatalystEntry> TYPE_MAP_CATALYST = RecipeType.create(ThermalShock.MODID, "map_catalyst", DataMapEntries.CatalystEntry.class);
    public static final RecipeType<DataMapEntries.SourceEntry> TYPE_MAP_HEAT_SOURCE = RecipeType.create(ThermalShock.MODID, "map_heat_source", DataMapEntries.SourceEntry.class);
    public static final RecipeType<DataMapEntries.SourceEntry> TYPE_MAP_COLD_SOURCE = RecipeType.create(ThermalShock.MODID, "map_cold_source", DataMapEntries.SourceEntry.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();

        // 燃料分类
        registration.addRecipeCategories(new ThermalFuelCategory(helper, TYPE_HEATER_FUEL, "block.thermalshock.thermal_heater", 
                new ItemStack(ThermalShockBlocks.THERMAL_HEATER.get()), true));
        registration.addRecipeCategories(new ThermalFuelCategory(helper, TYPE_FREEZER_FUEL, "block.thermalshock.thermal_freezer", 
                new ItemStack(ThermalShockBlocks.THERMAL_FREEZER.get()), false));

        // 转换器
        registration.addRecipeCategories(new ThermalConverterCategory(helper, new ItemStack(ThermalShockBlocks.THERMAL_CONVERTER.get())));

        // Data Maps
        registration.addRecipeCategories(new DataMapEntries.CasingCategory(helper, TYPE_MAP_CASING, new ItemStack(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get())));
        registration.addRecipeCategories(new DataMapEntries.CatalystCategory(helper, TYPE_MAP_CATALYST, new ItemStack(Items.DIAMOND)));
        registration.addRecipeCategories(new DataMapEntries.SourceCategory(helper, TYPE_MAP_HEAT_SOURCE, "gui.thermalshock.jei.map.heat_source", new ItemStack(Blocks.MAGMA_BLOCK)));
        registration.addRecipeCategories(new DataMapEntries.SourceCategory(helper, TYPE_MAP_COLD_SOURCE, "gui.thermalshock.jei.map.cold_source", new ItemStack(Blocks.BLUE_ICE)));

        // 模拟室 (Simulation Chamber)
        ItemStack simIcon = new ItemStack(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get());
        registration.addRecipeCategories(new SimulationOverheatingCategory(helper, simIcon));
        registration.addRecipeCategories(new SimulationShockCategory(helper, simIcon));
        registration.addRecipeCategories(new ClumpFillingShockCategory(helper, simIcon));
        registration.addRecipeCategories(new ClumpFillingCraftingCategory(helper, new ItemStack(Blocks.CRAFTING_TABLE)));
        registration.addRecipeCategories(new ClumpExtractionCategory(helper, simIcon));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var recipeManager = Minecraft.getInstance().level.getRecipeManager();

        // 1. 燃料配方 (分流)
        List<ThermalFuelRecipe> allFuels = recipeManager.getAllRecipesFor(ThermalShockRecipes.THERMAL_FUEL_TYPE.get())
                .stream().map(RecipeHolder::value).toList();
        
        registration.addRecipes(TYPE_HEATER_FUEL,
                allFuels.stream().filter(r -> r.getHeatRate() > 0).toList());
        registration.addRecipes(TYPE_FREEZER_FUEL,
                allFuels.stream().filter(r -> r.getHeatRate() < 0).toList());

        // 2. 转换器配方
        registration.addRecipes(TYPE_CONVERTER,
                recipeManager.getAllRecipesFor(ThermalShockRecipes.CONVERTER_TYPE.get()).stream().map(RecipeHolder::value).toList());

        // 3. Data Maps 加载
        registerDataMapRecipes(registration);

        // 4. 模拟室配方
        registration.addRecipes(TYPE_OVERHEATING, recipeManager.getAllRecipesFor(ThermalShockRecipes.OVERHEATING_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> !(r instanceof ClumpProcessingRecipe)) 
                .toList());

        registration.addRecipes(TYPE_SHOCK, recipeManager.getAllRecipesFor(ThermalShockRecipes.THERMAL_SHOCK_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> !(r instanceof ThermalShockFillingRecipe)) 
                .toList());

        registration.addRecipes(TYPE_FILLING_SHOCK, recipeManager.getAllRecipesFor(ThermalShockRecipes.THERMAL_SHOCK_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> r instanceof ThermalShockFillingRecipe)
                .map(r -> (ThermalShockFillingRecipe)r).toList());

        registration.addRecipes(TYPE_FILLING_CRAFTING, recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                .map(RecipeHolder::value).filter(r -> r instanceof ClumpFillingRecipe)
                .map(r -> (ClumpFillingRecipe)r).toList());

        registration.addRecipes(TYPE_EXTRACTION, recipeManager.getAllRecipesFor(ThermalShockRecipes.OVERHEATING_TYPE.get()).stream()
                .map(RecipeHolder::value).filter(r -> r instanceof ClumpProcessingRecipe)
                .map(r -> (ClumpProcessingRecipe)r).toList());
    }

    private void registerDataMapRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // FE Ratio
        int fePerHeat = 1000;
        try { fePerHeat = Config.fePerHeat; } catch (Exception e) {}

        // Casing
        java.util.Map<com.xnfu.thermalshock.data.CasingData, List<ItemStack>> groupedCasings = new java.util.HashMap<>();
        BuiltInRegistries.BLOCK.holders().forEach(holder -> {
            var data = holder.getData(ThermalShockDataMaps.CASING_PROPERTY);
            if (data != null) {
                groupedCasings.computeIfAbsent(data, k -> new ArrayList<>()).add(new ItemStack(holder.value()));
            }
        });
        List<DataMapEntries.CasingEntry> casings = new ArrayList<>();
        groupedCasings.forEach((data, items) -> casings.add(new DataMapEntries.CasingEntry(items, data)));
        registration.addRecipes(TYPE_MAP_CASING, casings);

        // Catalyst
        java.util.Map<com.xnfu.thermalshock.data.CatalystData, List<ItemStack>> groupedCatalysts = new java.util.HashMap<>();
        BuiltInRegistries.ITEM.holders().forEach(holder -> {
            var data = holder.getData(ThermalShockDataMaps.CATALYST_PROPERTY);
            if (data != null) {
                groupedCatalysts.computeIfAbsent(data, k -> new ArrayList<>()).add(new ItemStack(holder.value()));
            }
        });
        List<DataMapEntries.CatalystEntry> catalysts = new ArrayList<>();
        groupedCatalysts.forEach((data, items) -> catalysts.add(new DataMapEntries.CatalystEntry(items, data)));
        registration.addRecipes(TYPE_MAP_CATALYST, catalysts);

        // Heat Source
        List<DataMapEntries.SourceEntry> heatSources = new ArrayList<>();
        // Add FE Entry: No stacks, 1 Heat, feValue from config
        heatSources.add(new DataMapEntries.SourceEntry(List.of(), java.util.Optional.empty(), 1, fePerHeat, true, true));

        java.util.Map<Integer, List<ItemStack>> groupedHeat = new java.util.HashMap<>();
        BuiltInRegistries.BLOCK.holders().forEach(holder -> {
            var data = holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY);
            if (data != null) {
                var block = holder.value();
                if (block instanceof LiquidBlock liquid) {
                    heatSources.add(new DataMapEntries.SourceEntry(List.of(), java.util.Optional.of(new net.neoforged.neoforge.fluids.FluidStack(liquid.defaultBlockState().getFluidState().getType(), 1000)), data.heatPerTick(), 0, true, false));
                } else {
                    groupedHeat.computeIfAbsent(data.heatPerTick(), k -> new ArrayList<>()).add(new ItemStack(block));
                }
            }
        });
        groupedHeat.forEach((rate, items) -> heatSources.add(new DataMapEntries.SourceEntry(items, java.util.Optional.empty(), rate, 0, true, false)));
        registration.addRecipes(TYPE_MAP_HEAT_SOURCE, heatSources);

        // Cold Source
        List<DataMapEntries.SourceEntry> coldSources = new ArrayList<>();
        // Add FE Entry
        coldSources.add(new DataMapEntries.SourceEntry(List.of(), java.util.Optional.empty(), 1, fePerHeat, false, true));

        java.util.Map<Integer, List<ItemStack>> groupedCold = new java.util.HashMap<>();
        BuiltInRegistries.BLOCK.holders().forEach(holder -> {
            var data = holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
            if (data != null) {
                var block = holder.value();
                if (block instanceof LiquidBlock liquid) {
                    coldSources.add(new DataMapEntries.SourceEntry(List.of(), java.util.Optional.of(new net.neoforged.neoforge.fluids.FluidStack(liquid.defaultBlockState().getFluidState().getType(), 1000)), data.coolingPerTick(), 0, false, false));
                } else {
                    groupedCold.computeIfAbsent(data.coolingPerTick(), k -> new ArrayList<>()).add(new ItemStack(block));
                }
            }
        });
        groupedCold.forEach((rate, items) -> coldSources.add(new DataMapEntries.SourceEntry(items, java.util.Optional.empty(), rate, 0, false, false)));
        registration.addRecipes(TYPE_MAP_COLD_SOURCE, coldSources);
    }


    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(com.xnfu.thermalshock.client.gui.ThermalConverterScreen.class, 66, 36, 24, 16, TYPE_CONVERTER);
        
        registration.addRecipeClickArea(com.xnfu.thermalshock.client.gui.SimulationChamberScreen.class, 
                com.xnfu.thermalshock.client.gui.SimulationChamberScreen.BTN_RECIPE_X, 
                com.xnfu.thermalshock.client.gui.SimulationChamberScreen.BTN_RECIPE_Y, 
                com.xnfu.thermalshock.client.gui.SimulationChamberScreen.BTN_RECIPE_W, 
                com.xnfu.thermalshock.client.gui.SimulationChamberScreen.BTN_RECIPE_H, 
                TYPE_OVERHEATING, TYPE_SHOCK, TYPE_FILLING_SHOCK, TYPE_FILLING_CRAFTING, TYPE_EXTRACTION);

        registration.addGuiContainerHandler(com.xnfu.thermalshock.client.gui.ThermalSourceScreen.class, new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.xnfu.thermalshock.client.gui.ThermalSourceScreen>() {
            @Override
            public java.util.Collection<mezz.jei.api.gui.handlers.IGuiClickableArea> getGuiClickableAreas(com.xnfu.thermalshock.client.gui.ThermalSourceScreen containerScreen, double mouseX, double mouseY) {
                return java.util.List.of(new mezz.jei.api.gui.handlers.IGuiClickableArea() {
                    @Override
                    public net.minecraft.client.renderer.Rect2i getArea() {
                        return new net.minecraft.client.renderer.Rect2i(58, 48, 60, 5);
                    }

                    @Override
                    public void onClick(mezz.jei.api.recipe.IFocusFactory focusFactory, mezz.jei.api.runtime.IRecipesGui recipesGui) {
                        recipesGui.showTypes(java.util.List.of(TYPE_HEATER_FUEL, TYPE_FREEZER_FUEL));
                    }
                });
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        var menuType = com.xnfu.thermalshock.registries.ThermalShockMenus.SIMULATION_CHAMBER_MENU.get();
        
        registration.addRecipeTransferHandler(new ChamberRecipeTransferHandler<>(com.xnfu.thermalshock.client.gui.SimulationChamberMenu.class, menuType, TYPE_OVERHEATING), TYPE_OVERHEATING);
        registration.addRecipeTransferHandler(new ChamberRecipeTransferHandler<>(com.xnfu.thermalshock.client.gui.SimulationChamberMenu.class, menuType, TYPE_SHOCK), TYPE_SHOCK);
        registration.addRecipeTransferHandler(new ChamberRecipeTransferHandler<>(com.xnfu.thermalshock.client.gui.SimulationChamberMenu.class, menuType, TYPE_FILLING_SHOCK), TYPE_FILLING_SHOCK);
        registration.addRecipeTransferHandler(new ChamberRecipeTransferHandler<>(com.xnfu.thermalshock.client.gui.SimulationChamberMenu.class, menuType, TYPE_EXTRACTION), TYPE_EXTRACTION);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ThermalShockBlocks.THERMAL_HEATER.get()), TYPE_HEATER_FUEL);
        registration.addRecipeCatalyst(new ItemStack(ThermalShockBlocks.THERMAL_FREEZER.get()), TYPE_FREEZER_FUEL);
        registration.addRecipeCatalyst(new ItemStack(ThermalShockBlocks.THERMAL_CONVERTER.get()), TYPE_CONVERTER);
        
        registration.addRecipeCatalyst(new ItemStack(ThermalShockBlocks.THERMAL_HEATER.get()), TYPE_MAP_HEAT_SOURCE);
        registration.addRecipeCatalyst(new ItemStack(ThermalShockBlocks.THERMAL_FREEZER.get()), TYPE_MAP_COLD_SOURCE);

        ItemStack simIcon = new ItemStack(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get());
        registration.addRecipeCatalyst(simIcon, TYPE_OVERHEATING);
        registration.addRecipeCatalyst(simIcon, TYPE_SHOCK);
        registration.addRecipeCatalyst(simIcon, TYPE_FILLING_SHOCK);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CRAFTING_TABLE), TYPE_FILLING_CRAFTING);
        registration.addRecipeCatalyst(simIcon, TYPE_EXTRACTION);
    }
}
