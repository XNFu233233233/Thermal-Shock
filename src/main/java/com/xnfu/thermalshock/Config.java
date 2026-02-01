package com.xnfu.thermalshock;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue FE_PER_HEAT = BUILDER.defineInRange("fePerHeat", 10, 1, 1000000);
    public static final ModConfigSpec.IntValue SOURCE_MIN_ENERGY = BUILDER.defineInRange("sourceMinEnergy", 10, 0, 100000);
    public static final ModConfigSpec.IntValue MAX_CHAMBER_VENTS = BUILDER.defineInRange("maxChamberVents", 9, 1, 64);
    public static final ModConfigSpec.IntValue MAX_CHAMBER_PORTS = BUILDER.defineInRange("maxChamberPorts", 16, 1, 128);
    public static final ModConfigSpec.IntValue MAX_CHAMBER_ACCESS = BUILDER.defineInRange("maxChamberAccess", 4, 0, 16);
    public static final ModConfigSpec.BooleanValue ENABLE_OUTPUT_LIMIT = BUILDER.define("enableOutputLimit", true);
    public static final ModConfigSpec.IntValue PHYSICAL_OUTPUT_LIMIT = BUILDER.defineInRange("physicalOutputLimit", 1024, 1, 4096);
    public static final ModConfigSpec.DoubleValue CATALYST_BUFFER_CAP = BUILDER.defineInRange("catalystBufferCap", 1000.0, 10.0, 100000.0);
    public static final ModConfigSpec.IntValue CHAMBER_BASE_CAPACITY = BUILDER.defineInRange("chamberBaseCapacity", 10000, 100, 1000000);
    public static final ModConfigSpec.IntValue CHAMBER_VOL_CAPACITY_MULT = BUILDER.defineInRange("chamberVolCapacityMult", 1000, 0, 100000);
    public static final ModConfigSpec.IntValue CHAMBER_UPGRADE_CAPACITY_MULT = BUILDER.defineInRange("chamberUpgradeCapacityMult", 50000, 0, 1000000);
    public static final ModConfigSpec.IntValue TANK_CAPACITY = BUILDER.defineInRange("tankCapacity", 64000, 1000, 1000000);
    public static final ModConfigSpec.IntValue VALIDATION_LIMIT_PER_TICK = BUILDER.defineInRange("validationLimitPerTick", 5, 1, 100);

    static final ModConfigSpec SPEC = BUILDER.build();

    // 缓存变量，初始化为默认值
    public static int fePerHeat = 10;
    public static int sourceMinEnergy = 10;
    public static int maxChamberVents = 9;
    public static int maxChamberPorts = 16;
    public static int maxChamberAccess = 4;
    public static boolean enableOutputLimit = true;
    public static int physicalOutputLimit = 1024;
    public static double catalystBufferCap = 1000.0;
    public static int chamberBaseCapacity = 10000;
    public static int chamberVolCapacityMult = 1000;
    public static int chamberUpgradeCapacityMult = 50000;
    public static int tankCapacity = 64000;
    public static int validationLimitPerTick = 5;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            fePerHeat = FE_PER_HEAT.get();
            sourceMinEnergy = SOURCE_MIN_ENERGY.get();
            maxChamberVents = MAX_CHAMBER_VENTS.get();
            maxChamberPorts = MAX_CHAMBER_PORTS.get();
            maxChamberAccess = MAX_CHAMBER_ACCESS.get();
            enableOutputLimit = ENABLE_OUTPUT_LIMIT.get();
            physicalOutputLimit = PHYSICAL_OUTPUT_LIMIT.get();
            catalystBufferCap = CATALYST_BUFFER_CAP.get();
            chamberBaseCapacity = CHAMBER_BASE_CAPACITY.get();
            chamberVolCapacityMult = CHAMBER_VOL_CAPACITY_MULT.get();
            chamberUpgradeCapacityMult = CHAMBER_UPGRADE_CAPACITY_MULT.get();
            tankCapacity = TANK_CAPACITY.get();
            validationLimitPerTick = VALIDATION_LIMIT_PER_TICK.get();
        }
    }
}