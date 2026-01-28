package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Ingredient;

public record SimulationIngredient(Ingredient ingredient, RecipeSourceType type) {

    // JSON Codec: { "value": { "item": "..." }, "type": "block" }
    public static final Codec<SimulationIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("value").forGetter(SimulationIngredient::ingredient),
            StringRepresentable.fromEnum(RecipeSourceType::values).fieldOf("type").forGetter(SimulationIngredient::type)
    ).apply(instance, SimulationIngredient::new));

    // Network Codec
    public static final StreamCodec<RegistryFriendlyByteBuf, SimulationIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SimulationIngredient::ingredient,
            RecipeSourceType.STREAM_CODEC, SimulationIngredient::type,
            SimulationIngredient::new
    );
}