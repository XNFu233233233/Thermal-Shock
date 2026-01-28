package com.xnfu.thermalshock.block.entity;

import net.minecraft.util.StringRepresentable;

public enum MachineMode implements StringRepresentable {
    OVERHEATING("overheating", 0xFF4400),   // 红色 (过热)
    THERMAL_SHOCK("thermal_shock", 0x00FFFF); // 青色 (热冲击)

    private final String name;
    private final int color;

    MachineMode(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int getColor() { return color; }

    // 用于 GUI 按钮切换
    public MachineMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}