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
        // 1. 方块与物品
        add(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get(), "热冲击模拟室");
        add(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get(), "模拟室接口");
        add(ThermalShockBlocks.THERMAL_HEATER.get(), "热能发生器");
        add(ThermalShockBlocks.THERMAL_FREEZER.get(), "冷能发生器");
        add(ThermalShockBlocks.THERMAL_CONVERTER.get(), "热力转换器");
        add(ThermalShockItems.MATERIAL_CLUMP.get(), "物质团块");
        add(ThermalShockItems.SIMULATION_UPGRADE.get(), "模拟升级");

        // 模拟升级相关
        add("item.thermalshock.simulation_upgrade.desc", "控制器专用升级组件");
        add("item.thermalshock.simulation_upgrade.effect", "解除 1024 物品产出的安全限制。");

        add("tooltip.thermalshock.header.mechanic_change", "=== 机制变更：虚拟化 ===");
        add("item.thermalshock.simulation_upgrade.detail.virtualize", "- 禁用内部物理实体与方块扫描。");
        add("item.thermalshock.simulation_upgrade.detail.io", "- 启用基于接口(Port)的直接物品处理。");

        add("tooltip.thermalshock.header.scaling", "=== 数值重写 ===");
        add("item.thermalshock.simulation_upgrade.detail.scaling_rule", "- 忽略结构尺寸，属性仅取决于升级数量。");
        add("item.thermalshock.simulation_upgrade.detail.batch", "- 每个升级提供 +4 批处理量。");
        add("item.thermalshock.simulation_upgrade.detail.exponential", "- 效率与产量呈指数级增长。");

        // 物质团块相关
        add("item.thermalshock.material_clump.empty", "物质团块 (空)");
        add("item.thermalshock.material_clump.filled", "物质团块 (%s)");

        // 创造模式页签
        add("itemGroup.thermalshock", "热冲击 (Thermal Shock)");

        // GUI 标签 (主界面文本)
        add("gui.thermalshock.label.efficiency", "结构效率: %s%%");
        add("gui.thermalshock.label.bonus_yield", "产量加成: +%s%% (进度 %s%%)");
        add("gui.thermalshock.label.heat_io", "速率: %s%s 热/刻");
        add("gui.thermalshock.label.max_temp", "耐温上限: %s°C");

        // GUI 工具提示 (悬停信息)
        add("gui.thermalshock.tooltip.hold_shift", "按住 [Shift] 查看详情");

        // 通用按钮
        add("gui.thermalshock.btn.generic_clump.title", "通用团块处理");
        add("gui.thermalshock.btn.generic_clump.desc1", "自动识别内部的团块类型。");
        add("gui.thermalshock.btn.generic_clump.desc2", "请确保温度满足对应需求。");

        // 模式说明
        add("gui.thermalshock.tooltip.mode.overheating", "过热模式");
        add("gui.thermalshock.tooltip.mode.overheating.desc", "通过积累热量来加工物品。");
        add("gui.thermalshock.tooltip.mode.shock", "热冲击模式");
        add("gui.thermalshock.tooltip.mode.shock.desc", "利用瞬时温差(ΔT)粉碎或重组物质。");
        // 模式切换
        add("gui.thermalshock.tooltip.switch_mode_title", "切换机器模式");
        add("gui.thermalshock.tooltip.switching", "模式切换中: %s秒...");
        add("gui.thermalshock.tooltip.mode_current", "当前模式:");
        add("gui.thermalshock.mode.overheating", "过热模式");
        add("gui.thermalshock.mode.thermalshock", "热冲击模式");

        // 模式描述 (拆分)
        add("gui.thermalshock.tooltip.mode_desc.overheating", "过热模式: 积累热量以进行加工。");
        add("gui.thermalshock.tooltip.mode_desc.shock", "热冲击模式: 利用温差(ΔT)粉碎物质。");

        // 对号图标详情
        add("gui.thermalshock.status.detail.size_casing", "%s (%s)");
        add("gui.thermalshock.status.detail.interior", "内部空间: %s 格");
        add("gui.thermalshock.status.detail.casing", "耐温范围: %s°C ~ %s°C");
        add("gui.thermalshock.status.detail.max_batch", "批处理上限: %s");
        add("gui.thermalshock.status.help", "请检查结构搭建指南。");


        // 热冲击模式详细说明
        add("gui.thermalshock.tooltip.delta.title", "热应力 (ΔT)");
        add("gui.thermalshock.tooltip.delta.desc", "高温源与低温源的温度差值。");
        add("gui.thermalshock.tooltip.delta.detail", "数值必须超过配方需求才能运行。");

        // 效率提示
        add("gui.thermalshock.tooltip.efficiency.title", "结构基础效率");
        add("gui.thermalshock.tooltip.efficiency.desc", "由外壳材质决定 (如: 金/铜)。");
        add("gui.thermalshock.tooltip.efficiency.detail", "效率越高，催化剂消耗越少。");

        // 产量提示
        add("gui.thermalshock.tooltip.yield.title", "额外产量加成");
        add("gui.thermalshock.tooltip.yield.desc", "由催化剂提供的额外产出概率。");
        add("gui.thermalshock.tooltip.yield.detail", "进度: 距离产出下一个额外物品的百分比。");
        add("gui.thermalshock.warning.short", "⚠ 产出上限");
        add("gui.thermalshock.warning.detail", "安全机制: 单次产出 > 1024 物品将强制停机。");
        add("gui.thermalshock.warning.solution", "请安装 [模拟升级] 以解锁限制。");
        add("gui.thermalshock.tooltip.yield.formula", "公式: 基础 x 结构倍率 x (1+催化)");

        // 进度提示
        add("gui.thermalshock.tooltip.progress.title", "产量加成积累");
        add("gui.thermalshock.tooltip.progress.detail", "距离产生额外物品: %s%%");

        // 热量速率提示
        add("gui.thermalshock.tooltip.heat_io.title", "热量交换速率");
        add("gui.thermalshock.tooltip.heat_io.desc", "每刻(Tick)的热量净增减。");
        add("gui.thermalshock.tooltip.heat_io.detail", "受热源输入和配方消耗共同影响。");
        add("gui.thermalshock.tooltip.delta_t.desc", "需满足: 高温源 > %s, 低温源 < %s");
        add("gui.thermalshock.tooltip.input.high", "高温源输入: %s H");
        add("gui.thermalshock.tooltip.input.low", "低温源输入: %s H");
        add("gui.thermalshock.tooltip.input.net", "净热量输入: %s H");

        // 耐温提示
        add("gui.thermalshock.tooltip.max_temp.title", "结构耐温极限");
        add("gui.thermalshock.tooltip.max_temp.desc", "结构安全运行的最高温度。");
        add("gui.thermalshock.tooltip.max_temp.detail", "由外壳材质决定 (如: 黑曜石耐高温)。");

        // 进度条与图标提示
        add("gui.thermalshock.tooltip.heat_bar", "热量存储");
        add("gui.thermalshock.tooltip.heat_bar.desc", "机器运行所需的热能缓存。");

        add("gui.thermalshock.tooltip.catalyst_bar", "催化剂缓存");
        add("gui.thermalshock.tooltip.catalyst_bar.desc", "消耗以增加产量的物质。");

        // 状态图标 (对号/叉号)
        add("gui.thermalshock.status.valid", "结构完整");
        add("gui.thermalshock.status.invalid", "结构无效");
        add("gui.thermalshock.status.volume", "内部容积: %s 格");
        add("gui.thermalshock.status.last_batch", "单次批处理: %s");

        // 通用物品提示
        add("tooltip.thermalshock.clump_instruction", "放入热冲击模拟室进行加工");

        // GUI流体
        add("gui.thermalshock.tooltip.fluid", "%s: %s / %s mB");

        // 通用物品提示 (Shift 信息)
        add("tooltip.thermalshock.hold_shift", "按住 [Shift] 查看热力属性");
        add("tooltip.thermalshock.header", "=== 热冲击模拟室组件 ===");
        add("tooltip.thermalshock.temp_range", "耐温: %s°C ~ %s°C");
        add("tooltip.thermalshock.efficiency", "基础效率: %s");
        add("tooltip.thermalshock.heat_source_rate", "高温热源: +%s H (基础值)");
        add("tooltip.thermalshock.cold_source_rate", "低温热源: %s H (基础值)");
        add("tooltip.thermalshock.catalyst_yield", "产量加成: %s");
        add("tooltip.thermalshock.catalyst_buffer", "补充点数: %s");
        add("tooltip.thermalshock.type_vent", "组件: 散热排气口");
        add("tooltip.thermalshock.type_access", "组件: 结构密封门");

        // 锁定按钮
        add("tooltip.thermalshock.locked", "配方已锁定");
        add("tooltip.thermalshock.unlocked", "配方未锁定");

        // 系统提示信息 (多方块反馈)
        add("message.thermalshock.complete", "§a结构完整");
        add("message.thermalshock.invalid", "§c结构错误: ");
        add("message.thermalshock.incomplete", "未检测到有效结构 (需完整框架)");
        add("message.thermalshock.multiple_controllers", "只允许一个控制器");
        add("message.thermalshock.blocked_interior", "内部空间受阻 (必须清空或放置有效组件)");
        add("message.thermalshock.inconsistent_outer_shell", "外壳材质不统一");
        add("message.thermalshock.missing_vent", "缺少排气口 (需 1-9 个)");
        add("message.thermalshock.too_many_vents", "排气口过多 (最多 9 个)");
        add("message.thermalshock.too_many_port", "接口过多 (最多 16 个)");
        add("message.thermalshock.too_many_access", "密封门过多 (最多 4 个)");
    }
}