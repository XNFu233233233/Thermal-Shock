package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.ClumpFillingRecipe;
import com.xnfu.thermalshock.recipe.ClumpProcessingRecipe;
import com.xnfu.thermalshock.recipe.ClumpShockRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {

        // =================================================================
        // 1. [基础过热] 湿海绵 -> 干海绵
        // =================================================================
        // 模式: 红色 (Overheating)
        // 逻辑: 放置湿海绵方块，加热到 100度，消耗 500 热量 -> 变成干海绵
        ThermalShockRecipeBuilder.overheating(Blocks.SPONGE.asItem(), 1)
                .inputBlock(Ingredient.of(Blocks.WET_SPONGE)) // 指定为方块输入
                .setOverheatingParams(100, 500)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "overheating_sponge_dry"));

        ThermalShockRecipeBuilder.overheating(Blocks.SPONGE.asItem(), 1)
                .inputBlock(Ingredient.of(Blocks.ICE)) // 指定为方块输入
                .setOverheatingParams(100, 10)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "overheating_sponge_dry1"));

        // =================================================================
        // 2. [基础热冲击] 泥土 -> 粘土球
        // =================================================================
        // 模式: 青色 (Thermal Shock)
        // 逻辑: 放置泥土方块，制造 150度温差 -> 炸裂成粘土球
        ThermalShockRecipeBuilder.thermalShock(Items.CLAY_BALL, 4)
                .inputBlock(Ingredient.of(Blocks.DIRT)) // 指定为方块输入
                .setThermalShockParams(100, 10, 150)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "shock_dirt_to_clay"));

        // =================================================================
        // 3. [Clump 填装 - 工作台] 铁矿石 -> 铁锭数据
        // =================================================================
        // 模式: 工作台有序/无序合成
        // 逻辑: 铁矿石 + 空Clump -> 带有铁锭数据的Clump
        // 参数: 设定后续处理需要 200度, 1000热量
        ResourceLocation ironFillId = ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "clump_filling_iron");
        ClumpFillingRecipe ironFillRecipe = new ClumpFillingRecipe(
                "clump_filling",
                CraftingBookCategory.MISC,
                List.of(Ingredient.of(Items.IRON_ORE)), // 原料
                new ItemStack(Items.IRON_INGOT),        // 目标产物 (Clump内的数据)
                200,                                    // 后续过热需求: 200度
                1000                                    // 后续过热需求: 1000热量
        );
        output.accept(ironFillId, ironFillRecipe, null);

        // =================================================================
        // 4. [Clump 填装 - 机器热冲击] 铁栏杆 -> 铁粒数据
        // =================================================================
        // 模式: 青色 (Thermal Shock)
        // 逻辑: 丢入铁栏杆(物品) + 丢入空Clump(物品) -> 产出带有铁粒数据的Clump
        // 运行条件: High>200, Cold<10, Delta>150
        new ThermalShockFillingRecipeBuilder(
                Ingredient.of(Items.IRON_BARS),      // 输入原料 (会自动检测为物品)
                new ItemStack(Items.IRON_NUGGET),    // 目标产物 (Clump内的数据)
                100, 10, 150                         // 机器运行条件
        ).save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "shock_filling_iron_nugget"));

        // [新增] 铁锭 Clump 处理配方 (Iron Clump -> Iron Ingot)
        // 这是一个手写 JSON 风格的生成，对应 ClumpProcessingRecipe
        ClumpProcessingRecipe ironRecipe = new ClumpProcessingRecipe(
                new ItemStack(Items.IRON_INGOT), // 目标内容
                200,  // 需要 200 度
                1000  // 消耗 1000 热量
        );
        output.accept(
                ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "clump_process_iron"),
                ironRecipe,
                null
        );

        // [新增] 金锭 Clump (需要更高温度)
        ClumpProcessingRecipe goldRecipe = new ClumpProcessingRecipe(
                new ItemStack(Items.GOLD_INGOT),
                500,  // 需要 500 度
                2000  // 消耗 2000 热量
        );
        output.accept(
                ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "clump_process_gold"),
                goldRecipe,
                null
        );


        // === 5. 燃料配方 (Heater) ===
        // 煤炭: 1600 ticks, +200 heat
        ThermalFuelRecipeBuilder.fuel(Ingredient.of(Items.COAL), 1600, 200)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "fuel_coal"));

        // 烈焰棒: 2400 ticks, +500 heat
        ThermalFuelRecipeBuilder.fuel(Ingredient.of(Items.BLAZE_ROD), 2400, 500)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "fuel_blaze_rod"));

        // 熔岩桶: 20000 ticks, +1000 heat (注意：这里还没处理返还空桶逻辑，暂且作为一次性消耗)
        ThermalFuelRecipeBuilder.fuel(Ingredient.of(Items.LAVA_BUCKET), 20000, 1000)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "fuel_lava"));

        // === 6. 冷媒配方 (Freezer) ===
        // 雪球: 200 ticks, -50 heat
        ThermalFuelRecipeBuilder.fuel(Ingredient.of(Items.SNOWBALL), 200, -50)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "coolant_snowball"));

        // 冰: 1200 ticks, -200 heat
        ThermalFuelRecipeBuilder.fuel(Ingredient.of(Items.ICE), 1200, -200)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "coolant_ice"));

        // 蓝冰: 19200 ticks, -500 heat
        ThermalFuelRecipeBuilder.fuel(Ingredient.of(Items.BLUE_ICE), 19200, -500)
                .save(output, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "coolant_blue_ice"));
    }
}