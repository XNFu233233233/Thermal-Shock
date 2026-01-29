package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModEnLangProvider extends LanguageProvider {

    public ModEnLangProvider(PackOutput output) {
        super(output, ThermalShock.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 1. Blocks & Items
        add(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get(), "Thermal Shock Simulation Chamber");
        add(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get(), "Simulation Chamber Interface");
        add(ThermalShockBlocks.THERMAL_HEATER.get(), "Thermal Heater");
        add(ThermalShockBlocks.THERMAL_FREEZER.get(), "Thermal Freezer");
        add(ThermalShockBlocks.THERMAL_CONVERTER.get(), "Thermal Converter");
        add(ThermalShockItems.MATERIAL_CLUMP.get(), "Material Clump");
        add(ThermalShockItems.SIMULATION_UPGRADE.get(), "Simulation Upgrade");

        // 模拟升级相关
        add("item.thermalshock.simulation_upgrade.desc", "Component for Chamber Controller");
        add("item.thermalshock.simulation_upgrade.effect", "Bypasses the 1024-item safety limit.");

        add("tooltip.thermalshock.header.mechanic_change", "=== Mode Change: Virtualization ===");
        add("item.thermalshock.simulation_upgrade.detail.virtualize", "- Disables internal physical entity/block scanning.");
        add("item.thermalshock.simulation_upgrade.detail.io", "- Enables direct ItemStack processing via Ports.");

        add("tooltip.thermalshock.header.scaling", "=== Stat Overwrite ===");
        add("item.thermalshock.simulation_upgrade.detail.scaling_rule", "- Ignores structure size. Stats based on upgrade count.");
        add("item.thermalshock.simulation_upgrade.detail.batch", "- +4 Processing Batch per upgrade.");
        add("item.thermalshock.simulation_upgrade.detail.exponential", "- Efficiency & Yield scale exponentially.");

        // 物质团块相关
        add("item.thermalshock.material_clump.empty", "Material Clump (Empty)");
        add("item.thermalshock.material_clump.filled", "Material Clump (%s)");

        // 2. Creative Tab
        add("itemGroup.thermalshock", "Thermal Shock");

        // 3. GUI Labels (Main Text)
        add("gui.thermalshock.label.efficiency", "Efficiency: %s%%");
        add("gui.thermalshock.label.bonus_yield", "Bonus: +%s%% (Next: %s%%)");
        add("gui.thermalshock.label.heat_io", "Rate: %s%s H/t");
        add("gui.thermalshock.label.max_rate", "Max Rate: +%s / -%s");

        // 4. GUI Tooltips (Hover Info)
        add("gui.thermalshock.tooltip.hold_shift", "[Shift] for details");

        // 通用按钮
        add("gui.thermalshock.btn.generic_clump.title", "Generic Clump Process");
        add("gui.thermalshock.btn.generic_clump.desc1", "Auto-detects clump type inside.");
        add("gui.thermalshock.btn.generic_clump.desc2", "Ensure temperature meets requirement.");

        // 模式说明
        add("gui.thermalshock.tooltip.mode.overheating", "Overheating Mode");
        add("gui.thermalshock.tooltip.mode.overheating.desc", "Accumulates heat to process items.");
        add("gui.thermalshock.tooltip.mode.shock", "Thermal Shock Mode");
        add("gui.thermalshock.tooltip.mode.shock.desc", "Uses temp difference (ΔT) to shatter items.");
        // 模式切换
        add("gui.thermalshock.tooltip.switch_mode_title", "Switch Machine Mode");
        add("gui.thermalshock.tooltip.switching", "Switching in %ss...");
        add("gui.thermalshock.tooltip.mode_current", "Current Mode:");
        add("gui.thermalshock.mode.overheating", "Overheating Mode");
        add("gui.thermalshock.mode.thermalshock", "Thermal Shock Mode");
        // 模式描述 (拆分)
        add("gui.thermalshock.tooltip.mode_desc.overheating", "Overheating: Accumulate heat to process.");
        add("gui.thermalshock.tooltip.mode_desc.shock", "Shock: Use Temp Delta (ΔT) to shatter.");

        // 对号图标详情
        add("gui.thermalshock.status.detail.size_casing", "%s (%s)");
        add("gui.thermalshock.status.detail.interior", "Interior Volume: %s blocks");
        add("gui.thermalshock.status.detail.casing", "Limit: +%s / -%s H/t");
        add("gui.thermalshock.status.detail.max_batch", "Batch Limit: %s items");
        add("gui.thermalshock.status.help", "Check Holo-Guide for structure rules.");

        // 热冲击模式详细说明
        add("gui.thermalshock.tooltip.delta.title", "Thermal Stress (ΔT)");
        add("gui.thermalshock.tooltip.delta.desc", "Difference between High & Low temp.");
        add("gui.thermalshock.tooltip.delta.detail", "Must exceed recipe requirement.");

        // Efficiency Tooltip
        add("gui.thermalshock.tooltip.efficiency.title", "Structure Efficiency");
        add("gui.thermalshock.tooltip.efficiency.desc", "Determined by casing material.");
        add("gui.thermalshock.tooltip.efficiency.detail", "Affects catalyst consumption.");

        // Yield Tooltip
        add("gui.thermalshock.tooltip.yield.title", "Production Bonus");
        add("gui.thermalshock.tooltip.yield.desc", "Extra output chance from catalyst.");
        add("gui.thermalshock.tooltip.yield.detail", "(Next: %): Progress to extra item.");
        add("gui.thermalshock.warning.short", "⚠ Output Limit");
        add("gui.thermalshock.warning.detail", "Safety: Machine halts if output > 1024 items.");
        add("gui.thermalshock.warning.solution", "Install [Simulation Upgrade] to bypass.");
        add("gui.thermalshock.tooltip.yield.formula", "Formula: Base x Structure x (1+Catalyst)");

        // 进度提示
        add("gui.thermalshock.tooltip.progress.title", "Bonus Accumulation");
        add("gui.thermalshock.tooltip.progress.detail", "Progress to next extra item: %s%%");

        // Heat I/O Tooltip
        add("gui.thermalshock.tooltip.heat_io.title", "Heat Exchange Rate");
        add("gui.thermalshock.tooltip.heat_io.desc", "Net heat gain/loss per tick.");
        add("gui.thermalshock.tooltip.heat_io.detail", "Affected by sources & recipe cost.");
        add("gui.thermalshock.tooltip.delta_t.desc", "Requires: High > %s, Low < %s");
        add("gui.thermalshock.tooltip.input.high", "High Input: %s H");
        add("gui.thermalshock.tooltip.input.low", "Low Input: %s H");
        add("gui.thermalshock.tooltip.input.net", "Net Input: %s H");

        // Bars & Icons Tooltips
        add("gui.thermalshock.tooltip.heat_bar", "Heat Storage");
        add("gui.thermalshock.tooltip.heat_bar.desc", "Buffer for crafting operations.");

        add("gui.thermalshock.tooltip.catalyst_bar", "Catalyst Buffer");
        add("gui.thermalshock.tooltip.catalyst_bar.desc", "Consumed for bonus yield.");

        // Status Messages (Checkmark)
        add("gui.thermalshock.status.valid", "Structure Valid");
        add("gui.thermalshock.status.invalid", "Structure Invalid");
        add("gui.thermalshock.status.volume", "Volume: %s blocks");
        add("gui.thermalshock.status.last_batch", "Last Batch: %s");

        // General Item Tooltips
        add("tooltip.thermalshock.clump_instruction", "Process in Thermal Shock Chamber");

        // GUI流体
        add("gui.thermalshock.tooltip.fluid", "%s: %s / %s mB");

        // General Item Tooltips (Shift info on items)
        add("tooltip.thermalshock.hold_shift", "Hold [Shift] for Thermal Info");
        add("tooltip.thermalshock.header", "=== Thermal Shock Component ===");
        add("tooltip.thermalshock.rate_limit", "Input Limit: +%s / -%s H/t");
        add("tooltip.thermalshock.efficiency", "Efficiency: %s");
        add("tooltip.thermalshock.heat_source_rate", "Heat Source: +%s H (Base)");
        add("tooltip.thermalshock.cold_source_rate", "Cold Source: %s H (Base)");
        add("tooltip.thermalshock.catalyst_yield", "Yield Bonus: %s");
        add("tooltip.thermalshock.catalyst_buffer", "Refill: %s pts");
        add("tooltip.thermalshock.type_vent", "Type: Blast Vent");
        add("tooltip.thermalshock.type_access", "Type: Structure Access");

        // 锁定按钮
        add("tooltip.thermalshock.locked", "Recipe Locked");
        add("tooltip.thermalshock.unlocked", "Recipe Unlocked");

        // 6. System Messages
        add("message.thermalshock.complete", "§aStructure Complete");
        add("message.thermalshock.invalid", "§cStructure error: ");
        add("message.thermalshock.incomplete", "Structure incomplete (Frame required)");
        add("message.thermalshock.multiple_controllers", "Only one controller allowed");
        add("message.thermalshock.blocked_interior", "Interior must be clear or contain valid parts");
        add("message.thermalshock.inconsistent_outer_shell", "Inconsistent casing material");
        add("message.thermalshock.missing_vent", "Missing blast vent (1-9 required)");
        add("message.thermalshock.too_many_vents", "Too many blast vents (1-9 allowed)");
        add("message.thermalshock.too_many_port", "Too many interfaces (0-16 allowed)");
        add("message.thermalshock.too_many_access","Too many sealed doors (0-4 allowed)");
        add("message.thermalshock.port_mode", "Port mode changed: %s");
    }
}