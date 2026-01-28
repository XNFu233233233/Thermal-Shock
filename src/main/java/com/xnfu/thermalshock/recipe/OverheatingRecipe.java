package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public class OverheatingRecipe extends AbstractSimulationRecipe {
    private final int minTemp;
    private final int heatCost;

    public OverheatingRecipe(List<SimulationIngredient> inputs, ItemStack result, int minTemp, int heatCost) {
        super(inputs, result);
        this.minTemp = minTemp;
        this.heatCost = heatCost;
    }

    @Override
    public MachineMode getMachineMode() {
        return MachineMode.OVERHEATING;
    }

    public int getMinTemp() { return minTemp; }
    public int getHeatCost() { return heatCost; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.OVERHEATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ThermalShockRecipes.OVERHEATING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<OverheatingRecipe> {
        public static final MapCodec<OverheatingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SimulationIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(OverheatingRecipe::getSimulationIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(OverheatingRecipe::getResultStack),
                Codec.INT.fieldOf("min_temp").forGetter(OverheatingRecipe::getMinTemp),
                Codec.INT.fieldOf("heat_cost").forGetter(OverheatingRecipe::getHeatCost)
        ).apply(inst, OverheatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, OverheatingRecipe> STREAM_CODEC = StreamCodec.composite(
                SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), OverheatingRecipe::getSimulationIngredients,
                ItemStack.STREAM_CODEC, OverheatingRecipe::getResultStack,
                ByteBufCodecs.VAR_INT, OverheatingRecipe::getMinTemp,
                ByteBufCodecs.VAR_INT, OverheatingRecipe::getHeatCost,
                OverheatingRecipe::new
        );

        @Override public MapCodec<OverheatingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, OverheatingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}