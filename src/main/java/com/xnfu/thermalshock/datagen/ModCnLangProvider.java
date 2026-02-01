package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModCnLangProvider extends LanguageProvider {

    public ModCnLangProvider(PackOutput output) {
        super(output, ThermalShock.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // === 1. 方块与物品 ===
        add(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get(), "热冲击模拟室控制器");
        add(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get(), "模拟室接口");
        add(ThermalShockBlocks.THERMAL_HEATER.get(), "热能发生器");
        add(ThermalShockBlocks.THERMAL_FREEZER.get(), "冷能发生器");
        add(ThermalShockBlocks.THERMAL_CONVERTER.get(), "热力转换器");
        
        add(ThermalShockItems.MATERIAL_CLUMP.get(), "物质团块");
        add(ThermalShockItems.SIMULATION_UPGRADE.get(), "模拟升级");
        add(ThermalShockItems.OVERCLOCK_UPGRADE.get(), "超频升级");
        add("item.thermalshock.overclock_upgrade.desc", "热力转换器专用升级");
        add("item.thermalshock.overclock_upgrade.speed", "- 每个升级缩短 50% 运行时间 (乘法叠加)");
        add("item.thermalshock.overclock_upgrade.batch", "- 集齐4个时：解锁 4x 并行处理能力");

        // === 2. 创造模式页签 ===
        add("itemGroup.thermalshock", "热冲击 (Thermal Shock)");

        // === 3. 物品描述 ===
        add("item.thermalshock.simulation_upgrade.desc", "控制器专用升级组件");
        add("item.thermalshock.simulation_upgrade.effect", "解除单次 1024 物品产出的安全限制。");
        add("item.thermalshock.material_clump.empty", "物质团块 (空)");
        add("item.thermalshock.material_clump.filled", "物质团块 (%s)");

        // === 4. 物品工具提示 (详细) ===
        add("tooltip.thermalshock.hold_shift", "按住 [Shift] 查看详情");
        add("tooltip.thermalshock.header", "=== 热冲击组件 ===");
        add("tooltip.thermalshock.header.mechanic_change", "=== 机制变更：虚拟化 ===");
        add("tooltip.thermalshock.header.scaling", "=== 数值重写 ===");
        add("tooltip.thermalshock.clump_instruction", "放入热冲击模拟室进行加工");
        
        add("item.thermalshock.simulation_upgrade.detail.virtualize", "- 禁用内部物理实体与方块扫描。");
        add("item.thermalshock.simulation_upgrade.detail.io", "- 启用基于接口的直接物品处理。");
        add("item.thermalshock.simulation_upgrade.detail.scaling_rule", "- 忽略结构尺寸，属性取决于升级数量。");
        add("item.thermalshock.simulation_upgrade.detail.batch", "- 每个升级提供 +4 批处理量。");
        add("item.thermalshock.simulation_upgrade.detail.exponential", "- 效率与产量呈指数级增长。");
        
        add("tooltip.thermalshock.rate_limit", "输入上限: +%s / -%s 热/刻");
        add("tooltip.thermalshock.efficiency", "基础效率: %s");
        add("tooltip.thermalshock.heat_source_rate", "高温热源: +%s H (基础值)");
        add("tooltip.thermalshock.cold_source_rate", "低温热源: %s H (基础值)");
        add("tooltip.thermalshock.catalyst_yield", "产量加成: %s");
        add("tooltip.thermalshock.catalyst_buffer", "补充点数: %s");
        add("tooltip.thermalshock.type_vent", "组件: 散热排气口");
        add("tooltip.thermalshock.type_access", "组件: 结构密封门");
        add("tooltip.thermalshock.locked", "配方已锁定");
        add("tooltip.thermalshock.unlocked", "配方未锁定");

        // === 5. GUI 标签 (静态文本) ===
        add("gui.thermalshock.label.efficiency", "结构效率: %s%%");
        add("gui.thermalshock.label.bonus_yield", "产量加成: +%s%%");
        add("gui.thermalshock.label.heat_io", "速率: %s%s H/t");
        add("gui.thermalshock.label.max_rate", "输入限制: +%s / -%s");
        add("gui.thermalshock.mode.overheating", "过热模式");
        add("gui.thermalshock.mode.thermal_shock", "热冲击模式");
        add("gui.thermalshock.mode.shock", "热冲击模式");
        add("gui.thermalshock.mode.thermalshock", "热冲击模式");
        add("gui.thermalshock.label.delta", "热应力 (ΔT): %s H");

        // === 6. GUI 工具提示 (动态信息) ===
        add("gui.thermalshock.tooltip.hold_shift", "按住 [Shift] 查看详情");
        add("gui.thermalshock.tooltip.switching", "模式切换中: %s秒...");
        add("gui.thermalshock.tooltip.switch_mode_title", "切换机器模式");
        add("gui.thermalshock.tooltip.mode_current", "当前模式:");
        add("gui.thermalshock.tooltip.mode_desc.overheating", "过热模式: 积累热量以进行加工。");
        add("gui.thermalshock.tooltip.mode_desc.shock", "热冲击模式: 利用温差(ΔT)粉碎物质。");
        
        add("gui.thermalshock.tooltip.heat_bar", "热量存储");
        add("gui.thermalshock.tooltip.heat_bar.desc", "机器运行所需的热能缓存。");
        add("gui.thermalshock.tooltip.catalyst_bar", "催化剂缓存");
        add("gui.thermalshock.tooltip.catalyst_bar.desc", "消耗以增加产量的物质。");
        add("gui.thermalshock.tooltip.fluid", "%s: %s / %s mB");

        add("gui.thermalshock.tooltip.delta.title", "热应力 (ΔT)");
        add("gui.thermalshock.tooltip.delta.desc", "需满足: 高温 > %s, 低温 < %s");
        add("gui.thermalshock.tooltip.efficiency.title", "结构基础效率");
        add("gui.thermalshock.tooltip.efficiency.detail", "效率越高，催化剂消耗越少。");
        add("gui.thermalshock.tooltip.yield.title", "产量加成");
        add("gui.thermalshock.tooltip.yield.formula", "公式: (结构加成 * (1 + 催化效率) - 1) * 100%");
        add("gui.thermalshock.tooltip.progress.title", "产量积累");
        add("gui.thermalshock.tooltip.progress.desc", "产量加成进度: 当进度达到100%时，额外产出一个成品。");
        
        add("gui.thermalshock.tooltip.heat_io.title", "热量交换速率");
        add("gui.thermalshock.tooltip.heat_io.detail", "受热源输入和配方消耗共同影响。");
        add("gui.thermalshock.tooltip.input.high", "高温源输入: %s H");
        add("gui.thermalshock.tooltip.input.low", "低温源输入: %s H");
        add("gui.thermalshock.tooltip.input.net", "净热量输入: %s H");

        // === 7. GUI 按钮与状态 ===
        add("gui.thermalshock.btn.generic_clump.title", "通用团块处理");
        add("gui.thermalshock.btn.generic_clump.desc1", "自动识别内部的团块类型。");
        add("gui.thermalshock.btn.generic_clump.desc2", "请确保温度满足对应需求。");
        
        add("gui.thermalshock.status.valid", "结构完整");
        add("gui.thermalshock.status.invalid", "结构无效");
        add("gui.thermalshock.status.detail.size_casing", "%s (%s)");
        add("gui.thermalshock.status.detail.interior", "内部容积: %s 格");
        add("gui.thermalshock.status.detail.casing", "输入限制: +%s / -%s H/t");
        add("gui.thermalshock.status.detail.max_batch", "单次批处理: %s");
        add("gui.thermalshock.status.help", "请检查结构搭建指南。");

        add("gui.thermalshock.warning.short", "⚠ 产出上限");
        add("gui.thermalshock.warning.detail", "安全机制: 单次产出 > 1024 将强制停机。");
        add("gui.thermalshock.warning.solution", "请安装 [模拟升级] 以解锁限制。");

        // === 8. 系统提示信息 ===
        add("message.thermalshock.complete", "§a结构完整");
        add("message.thermalshock.invalid", "§c结构错误: ");
        add("message.thermalshock.incomplete", "未检测到有效结构 (需完整框架)");
        add("message.thermalshock.multiple_controllers", "只允许一个控制器");
        add("message.thermalshock.blocked_interior", "内部空间受阻");
        add("message.thermalshock.inconsistent_outer_shell", "外壳材质不统一");
        add("message.thermalshock.missing_vent", "缺少排气口 (需 1-9 个)");
        add("message.thermalshock.too_many_vents", "排气口过多 (最多 9 个)");
        add("message.thermalshock.too_many_port", "接口过多 (最多 16 个)");
        add("message.thermalshock.too_many_access", "密封门过多 (最多 4 个)");
        add("message.thermalshock.port_mode", "端口模式: %s");

        // === 9. 发生器 GUI ===
        add("gui.thermalshock.source.output", "输出: %s H");
        add("gui.thermalshock.source.jei_output", "输出: %s H/t");
        add("gui.thermalshock.source.target", "目标温度");
        add("gui.thermalshock.source.set", "设定");
        add("gui.thermalshock.source.energy_input", "输入: %s FE/t");
        add("gui.thermalshock.source.energy_buffer", "能量缓存");
        add("gui.thermalshock.source.duration", "持续时间: %ss");
        add("gui.thermalshock.source.remaining_time", "剩余时间: %ss");
        add("gui.thermalshock.converter.heat_requirement", "运行配方需要热量条件，需要保证机器热量满足配方运行条件。");
        add("gui.thermalshock.jei.label.min_heat_rate", "所需热量速率: >%d H/t");
        add("gui.thermalshock.jei.label.heat_cost", "消耗热量: %d H");
        add("gui.thermalshock.jei.chance.consume", "消耗概率: %d%%");
        add("gui.thermalshock.jei.chance.output", "产出概率: %d%%");
        add("gui.thermalshock.jei.fe_conversion", "能量 -> 热量 转换比例");
        add("gui.thermalshock.jei.category.fe_to_heat", "能量转换");
        add("gui.thermalshock.jei.map.casing", "结构外壳属性");
        add("gui.thermalshock.jei.map.catalyst", "模拟室催化剂属性");
        add("gui.thermalshock.jei.map.heat_source", "热源数据");
        add("gui.thermalshock.jei.map.cold_source", "冷源数据");

        // === 10. JEI 模拟室分类 ===
        add("gui.thermalshock.jei.category.overheating", "模拟室: 过热加工");
        add("gui.thermalshock.jei.category.shock", "模拟室: 热冲击加工");
        add("gui.thermalshock.jei.category.clump_filling_shock", "模拟室: 物质填充 (热冲击)");
        add("gui.thermalshock.jei.category.clump_filling_crafting", "工作台: 团块充填 (手工)");
        add("gui.thermalshock.jei.category.clump_extraction", "模拟室: 团块提取");
        add("gui.thermalshock.jei.preview.title", "支持的配方类型");

        add("jei.thermalshock.slot.block_input", "方块输入");
        add("jei.thermalshock.slot.item_input", "物品输入");
        add("jei.thermalshock.slot.output", "产出槽位");
        add("jei.thermalshock.slot.clump_extraction", "待提取团块");
        add("jei.thermalshock.slot.clump_filling", "待填充空团块");

        add("jei.thermalshock.label.high", "高温: >%d");
        add("jei.thermalshock.label.low", "低温: <%d");

        add("recipe.thermalshock.future_req", "提取需求: >%d H/t");
        add("recipe.thermalshock.future_cost", "提取消耗: %d H");

        add("gui.thermalshock.tooltip.show_recipes", "查看配方 (JEI)");
        add("gui.thermalshock.tooltip.show_recipes.desc", "支持的配方类型：");

        // === 11. Jade 适配 ===
        add("jade.thermalshock.status", "状态: ");
        add("jade.thermalshock.mode", "模式: ");
        add("jade.thermalshock.heat", "热量: %d H");
        add("jade.thermalshock.delta", "热应力: %d H");
        add("jade.thermalshock.input_header", "热量输入:");
        add("jade.thermalshock.high_input_label", "           +%d H");
        add("jade.thermalshock.low_input_label", "           %d H");
        add("jade.thermalshock.output", "输出: %d H/t");
        add("jade.thermalshock.net_input", "净输入率: %d H/t");
        add("jade.thermalshock.max_batch", "最大批次: %d");
        add("jade.thermalshock.recipe_locked", "配方锁定: ");
        add("jade.thermalshock.volume", "内部容积: %d 格");
        add("jade.thermalshock.ports", "接口数量: %d");
        add("jade.thermalshock.energy", "能量: %s / %s FE");
        add("jade.thermalshock.remaining", "剩余时间: %s秒");
    }
}
