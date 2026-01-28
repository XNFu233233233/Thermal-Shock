package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ThermalShockTags {
    // 1. 结构外壳：其实可以用上面的 DataMap 判断，但有个 Tag 方便做配方或者快速筛选
    public static final TagKey<Block> CASINGS = create("casings");

    // 2. 排气口：原版活板门、铁栏杆等
    public static final TagKey<Block> VENTS = create("vents");

    // 3. 允许内部通过的门：门、栅栏门、活板门
    // 这个标签用于告诉结构："如果结构内部出现了这个方块，不要报错，它是合法的空气替代品"
    public static final TagKey<Block> CASING_ACCESS = create("casing_access");

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, name));
    }
}
