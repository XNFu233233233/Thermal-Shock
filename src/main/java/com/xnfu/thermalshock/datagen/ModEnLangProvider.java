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
        // === 1. Blocks & Items ===
        add(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get(), "Simulation Chamber Controller");
        add(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get(), "Simulation Chamber Interface");
        add(ThermalShockBlocks.THERMAL_HEATER.get(), "Thermal Heater");
        add(ThermalShockBlocks.THERMAL_FREEZER.get(), "Thermal Freezer");
        add(ThermalShockBlocks.THERMAL_CONVERTER.get(), "Thermal Converter");
        
        add(ThermalShockItems.MATERIAL_CLUMP.get(), "Material Clump");
        add(ThermalShockItems.SIMULATION_UPGRADE.get(), "Simulation Upgrade");
        add(ThermalShockItems.OVERCLOCK_UPGRADE.get(), "Overclock Upgrade");

        // === 2. Creative Tab ===
        add("itemGroup.thermalshock", "Thermal Shock");

        // === 3. Item Descriptions ===
        add("item.thermalshock.simulation_upgrade.desc", "Controller Component");
        add("item.thermalshock.simulation_upgrade.effect", "Bypasses the 1024-item output limit.");
        add("item.thermalshock.material_clump.empty", "Material Clump (Empty)");
        add("item.thermalshock.material_clump.filled", "Material Clump (%s)");

        // === 4. Item Tooltips (Detailed) ===
        add("tooltip.thermalshock.hold_shift", "Hold [Shift] for details");
        add("tooltip.thermalshock.header", "=== Thermal Shock Component ===");
        add("tooltip.thermalshock.header.mechanic_change", "=== Mode Change: Virtualization ===");
        add("tooltip.thermalshock.header.scaling", "=== Stat Overwrite ===");
        add("tooltip.thermalshock.clump_instruction", "Process in Thermal Shock Chamber");
        
        add("item.thermalshock.simulation_upgrade.detail.virtualize", "- Disables internal physical scanning.");
        add("item.thermalshock.simulation_upgrade.detail.io", "- Enables direct ItemStack processing via Ports.");
        add("item.thermalshock.simulation_upgrade.detail.scaling_rule", "- Volume ignored. Stats based on upgrade count.");
        add("item.thermalshock.simulation_upgrade.detail.batch", "- +4 Processing Batch per upgrade.");
        add("item.thermalshock.simulation_upgrade.detail.exponential", "- Efficiency & Yield scale exponentially.");
        
        add("tooltip.thermalshock.rate_limit", "Input Limit: +%s / -%s H/t");
        add("tooltip.thermalshock.efficiency", "Efficiency: %s");
        add("tooltip.thermalshock.heat_source_rate", "Heat Source: +%s H (Base)");
        add("tooltip.thermalshock.cold_source_rate", "Cold Source: %s H (Base)");
        add("tooltip.thermalshock.catalyst_yield", "Yield Bonus: %s");
        add("tooltip.thermalshock.catalyst_buffer", "Refill: %s pts");
        add("tooltip.thermalshock.type_vent", "Type: Blast Vent");
        add("tooltip.thermalshock.type_access", "Type: Structure Access");
        add("tooltip.thermalshock.locked", "Recipe Locked");
        add("tooltip.thermalshock.unlocked", "Recipe Unlocked");

        // === 5. GUI Labels (Static) ===
        add("gui.thermalshock.label.efficiency", "Efficiency: %s%%");
        add("gui.thermalshock.label.bonus_yield", "Bonus: +%s%%");
        add("gui.thermalshock.label.heat_io", "Rate: %s%s H/t");
        add("gui.thermalshock.label.max_rate", "Limit: +%s / -%s");
        add("gui.thermalshock.mode.overheating", "Overheating Mode");
        add("gui.thermalshock.mode.shock", "Shock Mode");
        add("gui.thermalshock.mode.thermalshock", "Thermal Shock");
        add("gui.thermalshock.label.delta", "Thermal Stress: %s");

        // === 6. GUI Tooltips (Dynamic) ===
        add("gui.thermalshock.tooltip.hold_shift", "[Shift] for details");
        add("gui.thermalshock.tooltip.switching", "Switching in %ss...");
        add("gui.thermalshock.tooltip.switch_mode_title", "Switch Mode");
        add("gui.thermalshock.tooltip.mode_current", "Current:");
        add("gui.thermalshock.tooltip.mode_desc.overheating", "Overheating: Accumulate heat to process.");
        add("gui.thermalshock.tooltip.mode_desc.shock", "Shock: Use Temp Delta (ΔT) to shatter.");
        
        add("gui.thermalshock.tooltip.heat_bar", "Heat Storage");
        add("gui.thermalshock.tooltip.heat_bar.desc", "Energy buffer for operations.");
        add("gui.thermalshock.tooltip.catalyst_bar", "Catalyst Buffer");
        add("gui.thermalshock.tooltip.catalyst_bar.desc", "Consumed for bonus yield.");
        add("gui.thermalshock.tooltip.fluid", "%s: %s / %s mB");

        add("gui.thermalshock.tooltip.delta.title", "Thermal Stress (ΔT)");
        add("gui.thermalshock.tooltip.delta.desc", "Requires: High > %s, Low < %s");
        add("gui.thermalshock.tooltip.efficiency.title", "Structure Efficiency");
        add("gui.thermalshock.tooltip.efficiency.detail", "Reduces catalyst consumption.");
        add("gui.thermalshock.tooltip.yield.title", "Yield Bonus");
        add("gui.thermalshock.tooltip.yield.formula", "Formula: (Structure * (1 + Efficiency) - 1) * 100%");
        add("gui.thermalshock.tooltip.progress.title", "Yield Accumulation");
        add("gui.thermalshock.tooltip.progress.desc", "Accumulation: One extra output is produced when progress reaches 100%.");
        
        add("gui.thermalshock.tooltip.heat_io.title", "Heat Exchange Rate");
        add("gui.thermalshock.tooltip.heat_io.detail", "Net gain/loss per tick.");
        add("gui.thermalshock.tooltip.input.high", "High Input: %s H");
        add("gui.thermalshock.tooltip.input.low", "Low Input: %s H");
        add("gui.thermalshock.tooltip.input.net", "Net Input: %s H");

        // === 7. GUI Buttons & Status ===
        add("gui.thermalshock.btn.generic_clump.title", "Generic Clump Process");
        add("gui.thermalshock.btn.generic_clump.desc1", "Auto-detects clump type.");
        add("gui.thermalshock.btn.generic_clump.desc2", "Check temp requirements.");
        
        add("gui.thermalshock.status.valid", "Structure Valid");
        add("gui.thermalshock.status.invalid", "Structure Invalid");
        add("gui.thermalshock.status.detail.size_casing", "%s (%s)");
        add("gui.thermalshock.status.detail.interior", "Interior: %s blocks");
        add("gui.thermalshock.status.detail.casing", "Limit: +%s / -%s H/t");
        add("gui.thermalshock.status.detail.max_batch", "Batch Limit: %s");
        add("gui.thermalshock.status.help", "Check guide for rules.");

        add("gui.thermalshock.warning.short", "⚠ Output Limit");
        add("gui.thermalshock.warning.detail", "Safety halt if output > 1024.");
        add("gui.thermalshock.warning.solution", "Install [Simulation Upgrade].");

        // === 8. System Messages ===
        add("message.thermalshock.complete", "§aStructure Complete");
        add("message.thermalshock.invalid", "§cStructure error: ");
        add("message.thermalshock.incomplete", "Structure incomplete (Frame required)");
        add("message.thermalshock.multiple_controllers", "Only one controller allowed");
        add("message.thermalshock.blocked_interior", "Interior must be clear");
        add("message.thermalshock.inconsistent_outer_shell", "Inconsistent casing material");
        add("message.thermalshock.missing_vent", "Missing blast vent (1-9 required)");
        add("message.thermalshock.too_many_vents", "Too many blast vents (max 9)");
        add("message.thermalshock.too_many_port", "Too many interfaces (max 16)");
        add("message.thermalshock.too_many_access", "Too many sealed doors (max 4)");
        add("message.thermalshock.port_mode", "Port mode: %s");

        // === 9. Generator GUI ===
        add("gui.thermalshock.source.output", "Output: %s H");
        add("gui.thermalshock.source.target", "Target Heat");
        add("gui.thermalshock.source.set", "Set");
        add("gui.thermalshock.source.energy_input", "Input: %s FE/t");
        add("gui.thermalshock.source.energy_buffer", "Energy Buffer");
        add("gui.thermalshock.source.remaining_time", "Remaining: %s");
        add("gui.thermalshock.source.jei_output", "Output: %s H/t");
        add("gui.thermalshock.converter.heat_label", "Current Heat: %d H");
        add("gui.thermalshock.converter.heat_requirement", "Recipe requires specific heat levels.");

        // === 10. JEI Integration ===
        add("gui.thermalshock.jei.chance.consume", "Consume Chance: %s%%");
        add("gui.thermalshock.jei.chance.output", "Output Chance: %s%%");
        add("gui.thermalshock.jei.label.min_heat_rate", "min > %d H/t");
        add("gui.thermalshock.jei.label.heat_cost", "Heat Cost: %d H");
        add("gui.thermalshock.jei.fe_conversion", "Energy Conversion");
        add("gui.thermalshock.jei.category.fe_to_heat", "FE to Heat");
        add("gui.thermalshock.jei.map.casing", "Structural Casing");
        add("gui.thermalshock.jei.map.catalyst", "Chamber Catalyst");
        add("gui.thermalshock.jei.map.heat_source", "Heat Source");
        add("gui.thermalshock.jei.map.cold_source", "Cold Source");

        // === Simulation Chamber JEI ===
        add("gui.thermalshock.jei.category.overheating", "Simulation: Overheating");
        add("gui.thermalshock.jei.category.shock", "Simulation: Thermal Shock");
        add("gui.thermalshock.jei.category.clump_filling_shock", "Simulation: Clump Filling (Shock)");
        add("gui.thermalshock.jei.category.clump_filling_crafting", "Workbench: Clump Filling (Crafting)");
        add("gui.thermalshock.jei.category.clump_extraction", "Simulation: Clump Extraction");
        add("gui.thermalshock.jei.preview.title", "Supported Categories");
        
        add("jei.thermalshock.slot.block_input", "Block Input");
        add("jei.thermalshock.slot.item_input", "Item Input");
        add("jei.thermalshock.slot.output", "Output Slot");
        add("jei.thermalshock.slot.clump_extraction", "Clump to Extract");
        add("jei.thermalshock.slot.clump_filling", "Empty Clump to Fill");

        add("jei.thermalshock.label.high", "High: >%d");
        add("jei.thermalshock.label.low", "Low: <%d");

        add("gui.thermalshock.tooltip.show_recipes", "Show Recipes (JEI)");
        add("gui.thermalshock.tooltip.show_recipes.desc", "Click to view all supported recipe categories (Hold Shift for preview)");

        // === 11. Jade Integration ===
        add("jade.thermalshock.status", "Status: %s");
        add("jade.thermalshock.mode", "Mode: %s");
        add("jade.thermalshock.heat", "Heat: %d H");
        add("jade.thermalshock.delta", "Delta: %d H");
        add("jade.thermalshock.output", "Output: %d H/t");
        add("jade.thermalshock.net_input", "Net Input: %d H/t");
        add("jade.thermalshock.max_batch", "Max Batch: %d");
        add("jade.thermalshock.recipe_locked", "Recipe Locked: %s");
        add("jade.thermalshock.selected_recipe", "Selected Recipe: ");
        add("jade.thermalshock.volume", "Volume: %d blocks");
        add("jade.thermalshock.ports", "Ports: %d");
        add("jade.thermalshock.energy", "Energy: %s / %s FE");
        add("jade.thermalshock.remaining", "Remaining: %ss");
    }
}
