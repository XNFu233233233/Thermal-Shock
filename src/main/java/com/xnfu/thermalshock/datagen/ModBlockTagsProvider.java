package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.registries.ThermalShockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ThermalShock.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 1. 生成 VENTS (排气口)
        // 包括：铁栏杆, 栅栏
        this.tag(ThermalShockTags.VENTS)
                .add(Blocks.IRON_BARS)
                .addTag(BlockTags.FENCES);

        // 2. 生成 CASING_ACCESS (结构上面的门/接口)
        // 直接引用原版的标签，这样所有木门、铁门、栅栏门都自动生效
        this.tag(ThermalShockTags.CASING_ACCESS)
                .addTag(BlockTags.DOORS)        // 所有门
                .addTag(BlockTags.TRAPDOORS)    // 所有活板门
                .addTag(BlockTags.FENCE_GATES); // 所有栅栏门
    }
}