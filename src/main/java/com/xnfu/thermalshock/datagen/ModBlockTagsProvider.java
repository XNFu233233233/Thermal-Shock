package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.registries.ThermalShockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ThermalShock.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        var blocks = com.xnfu.thermalshock.registries.ThermalShockBlocks.BLOCKS.getEntries();
        var pickaxeTag = this.tag(BlockTags.MINEABLE_WITH_PICKAXE);

        for (var block : blocks) {
            pickaxeTag.add(block.get());
        }

        // 1. 生成 VENTS (排气口)：仅包括铁栏杆和栅栏
        this.tag(ThermalShockTags.VENTS)
                .add(Blocks.IRON_BARS)
                .addTag(BlockTags.FENCES);

        // 2. 生成 CASING_ACCESS (结构访问口)：仅包括门和活板门
        this.tag(ThermalShockTags.CASING_ACCESS)
                .addTag(BlockTags.DOORS)
                .addTag(BlockTags.TRAPDOORS);
    }
}