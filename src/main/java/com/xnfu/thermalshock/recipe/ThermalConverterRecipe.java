package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ThermalConverterRecipe implements Recipe<ConverterRecipeInput> {

    // === 内部数据结构 ===
    public record InputItem(Ingredient ingredient, int count, float consumeChance) {
        public static final Codec<InputItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(InputItem::ingredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(InputItem::count),
                Codec.FLOAT.optionalFieldOf("consume_chance", 1.0f).forGetter(InputItem::consumeChance)
        ).apply(i, InputItem::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, InputItem> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, InputItem::ingredient,
                ByteBufCodecs.VAR_INT, InputItem::count,
                ByteBufCodecs.FLOAT, InputItem::consumeChance,
                InputItem::new
        );
    }

    public record InputFluid(FluidStack fluid, float consumeChance) {
        public static final Codec<InputFluid> CODEC = RecordCodecBuilder.create(i -> i.group(
                FluidStack.CODEC.fieldOf("fluid").forGetter(InputFluid::fluid),
                Codec.FLOAT.optionalFieldOf("consume_chance", 1.0f).forGetter(InputFluid::consumeChance)
        ).apply(i, InputFluid::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, InputFluid> STREAM_CODEC = StreamCodec.composite(
                FluidStack.STREAM_CODEC, InputFluid::fluid,
                ByteBufCodecs.FLOAT, InputFluid::consumeChance,
                InputFluid::new
        );
    }

    public record OutputItem(ItemStack stack, float chance) {
        public static final Codec<OutputItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("item").forGetter(OutputItem::stack),
                Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(OutputItem::chance)
        ).apply(i, OutputItem::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, OutputItem> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, OutputItem::stack,
                ByteBufCodecs.FLOAT, OutputItem::chance,
                OutputItem::new
        );
    }

    public record OutputFluid(FluidStack fluid, float chance) {
        public static final Codec<OutputFluid> CODEC = RecordCodecBuilder.create(i -> i.group(
                FluidStack.CODEC.fieldOf("fluid").forGetter(OutputFluid::fluid),
                Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(OutputFluid::chance)
        ).apply(i, OutputFluid::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, OutputFluid> STREAM_CODEC = StreamCodec.composite(
                FluidStack.STREAM_CODEC, OutputFluid::fluid,
                ByteBufCodecs.FLOAT, OutputFluid::chance,
                OutputFluid::new
        );
    }

    // === 辅助 Record：解决 StreamCodec 参数过多的问题 ===
    private record RecipeParams(int processTime, int minHeat, int maxHeat) {}

    // === 主类字段 ===
    private final List<InputItem> itemInputs;
    private final List<InputFluid> fluidInputs;
    private final List<OutputItem> itemOutputs;
    private final List<OutputFluid> fluidOutputs;
    private final int processTime;
    private final int minHeat;
    private final int maxHeat;

    public ThermalConverterRecipe(List<InputItem> itemInputs, List<InputFluid> fluidInputs,
                                  List<OutputItem> itemOutputs, List<OutputFluid> fluidOutputs,
                                  int processTime, int minHeat, int maxHeat) {
        this.itemInputs = itemInputs;
        this.fluidInputs = fluidInputs;
        this.itemOutputs = itemOutputs;
        this.fluidOutputs = fluidOutputs;
        this.processTime = processTime;
        this.minHeat = minHeat;
        this.maxHeat = maxHeat;
    }

    // 辅助构造：用于 StreamCodec 还原
    private ThermalConverterRecipe(List<InputItem> itemInputs, List<InputFluid> fluidInputs,
                                   List<OutputItem> itemOutputs, List<OutputFluid> fluidOutputs,
                                   RecipeParams params) {
        this(itemInputs, fluidInputs, itemOutputs, fluidOutputs, params.processTime, params.minHeat, params.maxHeat);
    }

    // 辅助 Getter：用于 StreamCodec 提取参数
    private RecipeParams getParams() {
        return new RecipeParams(processTime, minHeat, maxHeat);
    }

    @Override
    public boolean matches(ConverterRecipeInput input, Level level) {
        if (!itemInputs.isEmpty()) {
            InputItem req = itemInputs.get(0);
            ItemStack slotStack = input.getItem(0);
            if (slotStack.isEmpty() || !req.ingredient.test(slotStack) || slotStack.getCount() < req.count) {
                return false;
            }
        }
        if (!fluidInputs.isEmpty()) {
            InputFluid req = fluidInputs.get(0);
            FluidStack tankStack = input.fluid();
            if (tankStack.isEmpty() || !tankStack.is(req.fluid.getFluid()) || tankStack.getAmount() < req.fluid.getAmount()) {
                return false;
            }
        }
        return !itemInputs.isEmpty() || !fluidInputs.isEmpty();
    }

    @Override
    public ItemStack assemble(ConverterRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return itemOutputs.isEmpty() ? ItemStack.EMPTY : itemOutputs.get(0).stack();
    }

    // Getters
    public List<InputItem> getItemInputs() { return itemInputs; }
    public List<InputFluid> getFluidInputs() { return fluidInputs; }
    public List<OutputItem> getItemOutputs() { return itemOutputs; }
    public List<OutputFluid> getFluidOutputs() { return fluidOutputs; }
    public int getProcessTime() { return processTime; }
    public int getMinHeat() { return minHeat; }
    public int getMaxHeat() { return maxHeat; }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() { return ThermalShockRecipes.CONVERTER_SERIALIZER.get(); }
    @Override
    public @NotNull RecipeType<?> getType() { return ThermalShockRecipes.CONVERTER_TYPE.get(); }

    // === Serializer ===
    public static class Serializer implements RecipeSerializer<ThermalConverterRecipe> {
        public static final MapCodec<ThermalConverterRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                InputItem.CODEC.listOf().optionalFieldOf("item_inputs", List.of()).forGetter(ThermalConverterRecipe::getItemInputs),
                InputFluid.CODEC.listOf().optionalFieldOf("fluid_inputs", List.of()).forGetter(ThermalConverterRecipe::getFluidInputs),
                OutputItem.CODEC.listOf().optionalFieldOf("item_outputs", List.of()).forGetter(ThermalConverterRecipe::getItemOutputs),
                OutputFluid.CODEC.listOf().optionalFieldOf("fluid_outputs", List.of()).forGetter(ThermalConverterRecipe::getFluidOutputs),
                Codec.INT.fieldOf("process_time").forGetter(ThermalConverterRecipe::getProcessTime),
                Codec.INT.optionalFieldOf("min_heat", Integer.MIN_VALUE).forGetter(ThermalConverterRecipe::getMinHeat),
                Codec.INT.optionalFieldOf("max_heat", Integer.MAX_VALUE).forGetter(ThermalConverterRecipe::getMaxHeat)
        ).apply(inst, ThermalConverterRecipe::new));

        // 拆分 StreamCodec：
        // Part 1: 3 个整数参数
        private static final StreamCodec<ByteBuf, RecipeParams> PARAMS_STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RecipeParams::processTime,
                ByteBufCodecs.VAR_INT, RecipeParams::minHeat,
                ByteBufCodecs.VAR_INT, RecipeParams::maxHeat,
                RecipeParams::new
        );

        // Part 2: 组合列表和参数 (4个列表 + 1个 Params对象 = 5个参数，符合 composite 限制)
        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalConverterRecipe> STREAM_CODEC = StreamCodec.composite(
                InputItem.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalConverterRecipe::getItemInputs,
                InputFluid.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalConverterRecipe::getFluidInputs,
                OutputItem.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalConverterRecipe::getItemOutputs,
                OutputFluid.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalConverterRecipe::getFluidOutputs,
                PARAMS_STREAM_CODEC.cast(), ThermalConverterRecipe::getParams,
                ThermalConverterRecipe::new
        );

        @Override public @NotNull MapCodec<ThermalConverterRecipe> codec() { return CODEC; }
        @Override public @NotNull StreamCodec<RegistryFriendlyByteBuf, ThermalConverterRecipe> streamCodec() { return STREAM_CODEC; }
    }
}