package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public class ThermalShockFillingRecipe extends ThermalShockRecipe {

    private final Holder<Item> targetItem;
    private final int count;
    private final ClumpInfo cachedInfo;

    public ThermalShockFillingRecipe(List<SimulationIngredient> inputs, Holder<Item> targetItem, int count, int minHot, int maxCold, int delta) {
        super(inputs, new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get()), minHot, maxCold, delta);
        this.targetItem = targetItem;
        this.count = count;
        this.cachedInfo = new ClumpInfo(targetItem, count);
    }

    @Override
    public ItemStack assemble(SimulationRecipeInput input, HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        stack.set(ThermalShockDataComponents.TARGET_OUTPUT, cachedInfo);
        return stack;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        stack.set(ThermalShockDataComponents.TARGET_OUTPUT, cachedInfo);
        return stack;
    }

    public Holder<Item> getTargetItem() { return targetItem; }
    public int getCount() { return count; }

    @Override
    public boolean matches(SimulationRecipeInput input, net.minecraft.world.level.Level level) {
        if (!input.primary().is(ThermalShockItems.MATERIAL_CLUMP.get())) return false;
        return super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.THERMAL_SHOCK_FILLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ThermalShockRecipes.THERMAL_SHOCK_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ThermalShockFillingRecipe> {
        public static final MapCodec<ThermalShockFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SimulationIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(ThermalShockRecipe::getSimulationIngredients),
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("target_item").forGetter(ThermalShockFillingRecipe::getTargetItem),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ThermalShockFillingRecipe::getCount),
                Codec.INT.optionalFieldOf("min_hot", Integer.MIN_VALUE).forGetter(ThermalShockRecipe::getMinHotTemp),
                Codec.INT.optionalFieldOf("max_cold", Integer.MAX_VALUE).forGetter(ThermalShockRecipe::getMaxColdTemp),
                Codec.INT.fieldOf("delta").forGetter(ThermalShockRecipe::getRequiredDelta)
        ).apply(inst, ThermalShockFillingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> STREAM_CODEC = StreamCodec.composite(
                SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalShockRecipe::getSimulationIngredients,
                ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.ITEM), ThermalShockFillingRecipe::getTargetItem,
                ByteBufCodecs.VAR_INT, ThermalShockFillingRecipe::getCount,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getMinHotTemp,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getMaxColdTemp,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getRequiredDelta,
                ThermalShockFillingRecipe::new
        );

        @Override public MapCodec<ThermalShockFillingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
