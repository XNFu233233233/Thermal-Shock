package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.*;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
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

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // [修改] 为所有配方添加 neoforge:false 条件，使其默认不加载 (作为开发测试用)
        RecipeOutput testOutput = output.withConditions(net.neoforged.neoforge.common.conditions.FalseCondition.INSTANCE);

        // =================================================================
        // 1. 过热模式 (Overheating) - 积累热量
        // Path: recipes/dev_test/overheating/
        // =================================================================

        // 1.1 Item -> Item (基础金属加工)
        // 铁粒 -> 铁粒 (处理测试: 3个输入 -> 1个输出)
        buildOverheating(testOutput, "iron_nuggets_processing",
                Ingredient.of(Items.IRON_NUGGET), 3,
                new ItemStack(Items.IRON_NUGGET),
                200, 50, false);

        // 1.2 Block -> Item (烧制)
        // 原木 -> 木炭 (需要 100度, 消耗 200H)
        buildOverheating(testOutput, "log_to_charcoal",
                Ingredient.of(net.minecraft.tags.ItemTags.LOGS), 1,
                new ItemStack(Items.CHARCOAL, 2), // 产量加倍奖励
                100, 200, true); // true = Block Input

        // 1.3 Block -> Block (转化)
        // 湿海绵 -> 干海绵
        buildOverheating(testOutput, "wet_sponge_drying",
                Ingredient.of(Blocks.WET_SPONGE), 1,
                new ItemStack(Blocks.SPONGE),
                100, 500, true);

        // 1.4 Fluid Block -> Block (流体参与 - 物理模式检测流体方块，虚拟模式检测 Port流体)
        // 岩浆 -> 黑曜石 (需要极高热量蒸发杂质?) - 假设逻辑
        // 这里用 Bucket 表示流体原料
        buildOverheating(testOutput, "lava_to_obsidian",
                Ingredient.of(Items.LAVA_BUCKET), 1,
                new ItemStack(Blocks.OBSIDIAN),
                1000, 5000, true); // Block input = LAVA BLOCK

        // =================================================================
        // 2. 热冲击模式 (Thermal Shock) - 温差炸裂
        // Path: recipes/dev_test/shock/
        // =================================================================

        // 2.1 Block -> Item (粉碎)
        // 泥土 -> 粘土球
        buildShock(testOutput, "dirt_to_clay",
                Ingredient.of(Blocks.DIRT), 1,
                new ItemStack(Items.CLAY_BALL, 4),
                100, 10, 150, true);

        // 圆石 -> 沙砾
        buildShock(testOutput, "cobble_to_gravel",
                Ingredient.of(Blocks.COBBLESTONE), 1,
                new ItemStack(Blocks.GRAVEL),
                200, 0, 300, true);

        // 2.2 Fluid Block -> Item (流体炸裂)
        // 水 -> 冰 (利用温差快速结晶?)
        // 逻辑: 高温 50, 低温 -50, 温差 100. 输入: 水方块. 输出: 冰
        buildShock(testOutput, "water_to_ice",
                Ingredient.of(Items.WATER_BUCKET), 1,
                new ItemStack(Blocks.ICE),
                50, -50, 100, true);

        // 2.3 Item -> Item (精密加工)
        // 玻璃 -> 玻璃板 (利用热冲击切割)
        buildShock(testOutput, "glass_cutting",
                Ingredient.of(Blocks.GLASS), 1,
                new ItemStack(Blocks.GLASS_PANE, 4),
                150, 20, 200, false); // Item Input

        // =================================================================
        // 3. 团块填充 (Clump Filling) - 制作数据团块
        // Path: recipes/dev_test/filling/
        // =================================================================

        // 3.1 工作台合成 (Crafting Table)
        // 铁矿 -> 铁锭团块
        buildFillingCrafting(testOutput, "iron_filling_crafting",
                Ingredient.of(Items.IRON_ORE),
                new ItemStack(Items.IRON_INGOT), 200, 1000);

        // 金矿 -> 金锭团块
        buildFillingCrafting(testOutput, "gold_filling_crafting",
                Ingredient.of(Items.GOLD_ORE),
                new ItemStack(Items.GOLD_INGOT), 500, 2000);

        // 3.2 机器热冲击填充 (Machine Shock Filling)
        // 铁栏杆 -> 铁粒团块
        buildFillingShock(testOutput, "iron_nugget_filling",
                Ingredient.of(Items.IRON_BARS),
                new ItemStack(Items.IRON_NUGGET),
                100, 10, 150, // 机器运行条件
                100, 500);    // 团块后续属性

        // =================================================================
        // 4. 团块提取 (Clump Processing) - 将团块还原为物品
        // Path: recipes/dev_test/extraction/
        // =================================================================
        // 注意: 这里需要为每种可能的团块生成对应的提取配方

        // 铁锭团块提取
        buildClumpProcessing(testOutput, "extract_iron_ingot",
                new ItemStack(Items.IRON_INGOT), 200, 1000);

        // 金锭团块提取
        buildClumpProcessing(testOutput, "extract_gold_ingot",
                new ItemStack(Items.GOLD_INGOT), 500, 2000);

        // 铁粒团块提取
        buildClumpProcessing(testOutput, "extract_iron_nugget",
                new ItemStack(Items.IRON_NUGGET), 100, 500);

        // =================================================================
        // 5. 热力燃料 (Fuels & Coolants)
        // Path: recipes/dev_test/fuel/
        // =================================================================

        // 5.1 Heaters (Positive Heat)
        buildFuel(testOutput, "fuel_coal", Ingredient.of(Items.COAL), 1600, 200);
        buildFuel(testOutput, "fuel_blaze_rod", Ingredient.of(Items.BLAZE_ROD), 2400, 500);
        buildFuel(testOutput, "fuel_lava_bucket", Ingredient.of(Items.LAVA_BUCKET), 20000, 1000);
        buildFuel(testOutput, "fuel_magma_cream", Ingredient.of(Items.MAGMA_CREAM), 1200, 300);

        // 5.2 Freezers (Negative Heat)
        buildFuel(testOutput, "coolant_snowball", Ingredient.of(Items.SNOWBALL), 200, -50);
        buildFuel(testOutput, "coolant_ice", Ingredient.of(Items.ICE), 1200, -200);
        buildFuel(testOutput, "coolant_packed_ice", Ingredient.of(Blocks.PACKED_ICE), 4800, -350);
        buildFuel(testOutput, "coolant_blue_ice", Ingredient.of(Blocks.BLUE_ICE), 19200, -500);

        // =================================================================
        // 6. 热力转换器 (Thermal Converter)
        // Path: recipes/dev_test/converter/
        // =================================================================

        // 6.1 Item -> Fluid (Melting)
        // 圆石 -> 岩浆
        ThermalConverterRecipeBuilder.create()
                .inputItem(Ingredient.of(Blocks.COBBLESTONE), 1, 1.0f)
                .outputFluid(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 250), 1.0f)
                .minHeat(500).time(200)
                .save(testOutput, loc("converter/melt_cobble"));

        // 6.2 Fluid -> Item (Freezing)
        // 水 -> 冰
        ThermalConverterRecipeBuilder.create()
                .inputFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1.0f)
                .outputItem(new ItemStack(Blocks.ICE), 1.0f)
                .maxHeat(0).time(100)
                .save(testOutput, loc("converter/freeze_water"));

        // 6.3 Item -> Item + Fluid (Drying)
        // 湿海绵 -> 干海绵 + 水
        ThermalConverterRecipeBuilder.create()
                .inputItem(Ingredient.of(Blocks.WET_SPONGE), 1, 1.0f)
                .outputItem(new ItemStack(Blocks.SPONGE), 1.0f)
                .outputFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1.0f)
                .minHeat(50).time(60)
                .save(testOutput, loc("converter/dry_sponge"));

        // 6.4 Item -> Item + Item (Byproducts)
        // 沙砾 -> 沙子 + 铁粒(10%)
        ThermalConverterRecipeBuilder.create()
                .inputItem(Ingredient.of(Blocks.GRAVEL), 1, 1.0f)
                .outputItem(new ItemStack(Blocks.SAND), 1.0f)
                .outputItem(new ItemStack(Items.IRON_NUGGET), 0.1f)
                .minHeat(200).time(80)
                .save(testOutput, loc("converter/sift_gravel"));
    }

    // === Helpers ===

    private ResourceLocation loc(String path) {
        // [修改] 增加 dev_test/ 前缀
        return ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "dev_test/" + path);
    }

    private void buildOverheating(RecipeOutput output, String name, Ingredient input, int count, ItemStack result, int minHeat, int cost, boolean isBlock) {
        var builder = ThermalShockRecipeBuilder.overheating(result);
        for(int i=0; i<count; i++) {
            if (isBlock) builder.inputBlock(input);
            else builder.inputItem(input);
        }
        builder.setOverheatingParams(minHeat, cost)
               .save(output, loc("overheating/" + name));
    }

    private void buildShock(RecipeOutput output, String name, Ingredient input, int count, ItemStack result, int minHot, int maxCold, int delta, boolean isBlock) {
        var builder = ThermalShockRecipeBuilder.thermalShock(result);
        for(int i=0; i<count; i++) {
            if (isBlock) builder.inputBlock(input);
            else builder.inputItem(input);
        }
        builder.setThermalShockParams(minHot, maxCold, delta)
               .save(output, loc("shock/" + name));
    }

    private void buildFillingCrafting(RecipeOutput output, String name, Ingredient input, ItemStack clumpResult, int clumpMin, int clumpCost) {
        ClumpFillingRecipe recipe = new ClumpFillingRecipe(
                "clump_filling",
                CraftingBookCategory.MISC,
                ShapedRecipePattern.of(
                        Map.of('I', input, 'C', Ingredient.of(ThermalShockItems.MATERIAL_CLUMP.get())),
                        List.of("I", "C")
                ),
                clumpResult, clumpMin, clumpCost
        );
        output.accept(loc("filling/" + name), recipe, null);
    }

    private void buildFillingShock(RecipeOutput output, String name, Ingredient input, ItemStack clumpResult, int minHot, int maxCold, int delta, int clumpMin, int clumpCost) {
        new ThermalShockFillingRecipeBuilder(input, clumpResult, minHot, maxCold, delta)
                .clumpParams(clumpMin, clumpCost)
                .save(output, loc("filling/" + name));
    }

    private void buildClumpProcessing(RecipeOutput output, String name, ItemStack result, int minHeat, int cost) {
        ClumpProcessingRecipe recipe = new ClumpProcessingRecipe(
                List.of(new SimulationIngredient(Ingredient.of(ThermalShockItems.MATERIAL_CLUMP.get()), RecipeSourceType.ITEM)),
                result, minHeat, cost
        );
        output.accept(loc("extraction/" + name), recipe, null);
    }

    private void buildFuel(RecipeOutput output, String name, Ingredient input, int time, int rate) {
        ThermalFuelRecipeBuilder.fuel(input, time, rate)
                .save(output, loc("fuel/" + name));
    }
}
