package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ThermalShock.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // 1. 生成控制器模型
        registerController();

        // 2. 生成接口模型
        registerPort();

        registerSourceBlock(ThermalShockBlocks.THERMAL_HEATER, "thermal_heater");
        registerSourceBlock(ThermalShockBlocks.THERMAL_FREEZER, "thermal_freezer");
        registerSourceBlock(ThermalShockBlocks.THERMAL_CONVERTER, "thermal_converter");
    }

    private void registerController() {
        DeferredBlock<Block> block = ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER;

        ResourceLocation sideTexture = mcLoc("block/bricks");
        ResourceLocation frontOff = modLoc("block/chamber_controller_front_off");

        ModelFile modelOff = models().orientable(block.getId().getPath() + "_off", sideTexture, frontOff, sideTexture);

        horizontalBlock(block.get(), modelOff);

        simpleBlockItem(block.get(), modelOff);
    }

    private void registerPort() {
        DeferredBlock<Block> block = ThermalShockBlocks.SIMULATION_CHAMBER_PORT;

        // 假设接口六个面都是同一个纹理
        ResourceLocation texture = modLoc("block/chamber_port");

        // 生成简单的立方体模型
        ModelFile model = models().cubeAll(block.getId().getPath(), texture);

        // 注册 BlockState 和 物品模型
        simpleBlockWithItem(block.get(), model);
    }

    private void registerSourceBlock(DeferredBlock<Block> block, String name) {
        ResourceLocation frontOff = modLoc("block/" + name + "_front_off");
        ResourceLocation frontOn = modLoc("block/" + name + "_front_on");
        ResourceLocation side = modLoc("block/" + name + "_side");
        ResourceLocation top = modLoc("block/" + name + "_top");

        // 创建 Off 模型
        ModelFile modelOff = models().orientableWithBottom(name + "_off", side, frontOff, top, top);
        // 创建 On 模型
        ModelFile modelOn = models().orientableWithBottom(name + "_on", side, frontOn, top, top);

        // 注册 BlockState (根据 LIT 和 FACING 旋转)
        horizontalBlock(block.get(), state -> {
            boolean lit = state.getValue(BlockStateProperties.LIT);
            return lit ? modelOn : modelOff;
        });

        // 注册 Item 模型 (默认使用 Off 状态)
        simpleBlockItem(block.get(), modelOff);
    }
}