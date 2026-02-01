package com.xnfu.thermalshock.util;

import net.minecraft.ChatFormatting;

/**
 * 模组常量池。
 * 统一管理颜色、布局、同步频率等数值。
 */
public class ModConstants {
    
    // === 颜色 (Jade & GUI) ===
    public static final int COLOR_HEAT = 0xFFDDAA00;      // 橙色 (热量)
    public static final int COLOR_DELTA = 0xFF00FFFF;     // 青色 (温差)
    public static final int COLOR_EFFICIENCY = 0xFF00AA00; // 绿色 (效率)
    public static final int COLOR_NET_INPUT = 0xFFFFFF00;  // 黄色 (净输入)
    public static final int COLOR_LOCKED = 0xFFFF0000;     // 红色 (锁定)
    public static final int COLOR_UNLOCKED = 0xFFAAAAAA;   // 灰色 (未锁定)
    
    // === 机器状态颜色 ===
    public static final ChatFormatting STYLE_VALID = ChatFormatting.GREEN;
    public static final ChatFormatting STYLE_INVALID = ChatFormatting.RED;
    public static final ChatFormatting STYLE_MODE = ChatFormatting.AQUA;

    // === GUI 布局 ===
    public static final int GUI_CHAMBER_WIDTH = 178;
    public static final int GUI_CHAMBER_HEIGHT = 202;
    
    // === 同步控制 ===
    public static final int SYNC_INTERVAL_STATIC = 40; // 静态数据同步间隔 (2秒)
}
