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

public class ThermalShockRecipe extends AbstractSimulationRecipe {
    private final int minHotTemp;
    private final int maxColdTemp;
    private final int requiredDelta;

    public ThermalShockRecipe(List<SimulationIngredient> inputs, ItemStack result, int minHotTemp, int maxColdTemp, int requiredDelta) {
        super(inputs, result);
        this.minHotTemp = minHotTemp;
        this.maxColdTemp = maxColdTemp;
        this.requiredDelta = requiredDelta;
    }

    @Override
    public MachineMode getMachineMode() {
        return MachineMode.THERMAL_SHOCK;
    }

    public int getMinHotTemp() { return minHotTemp; }
    public int getMaxColdTemp() { return maxColdTemp; }
    public int getRequiredDelta() { return requiredDelta; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.THERMAL_SHOCK_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ThermalShockRecipes.THERMAL_SHOCK_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ThermalShockRecipe> {
        public static final MapCodec<ThermalShockRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SimulationIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(ThermalShockRecipe::getSimulationIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(ThermalShockRecipe::getResultStack),
                Codec.INT.fieldOf("min_hot").forGetter(ThermalShockRecipe::getMinHotTemp),
                Codec.INT.fieldOf("max_cold").forGetter(ThermalShockRecipe::getMaxColdTemp),
                Codec.INT.fieldOf("delta").forGetter(ThermalShockRecipe::getRequiredDelta)
        ).apply(inst, ThermalShockRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalShockRecipe> STREAM_CODEC = StreamCodec.composite(
                SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalShockRecipe::getSimulationIngredients,
                ItemStack.STREAM_CODEC, ThermalShockRecipe::getResultStack,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getMinHotTemp,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getMaxColdTemp,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getRequiredDelta,
                ThermalShockRecipe::new
        );

        @Override public MapCodec<ThermalShockRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ThermalShockRecipe> streamCodec() { return STREAM_CODEC; }
    }
}