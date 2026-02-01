# Thermal Shock 开发计划 (NeoForge 1.21.1)

本计划旨在完成 Thermal Shock 模组从旧版本/MDK到 **NeoForge 1.21.1** 标准的全面迁移，并优化 KubeJS 集成。

## 1. 核心目标 (状态更新)

### 1.1 基础架构适配
*   **[已完成] 配置系统 (Config)**: 
    *   将硬编码数值提取至 `Config.java`。
    *   **[修复]** 实现了配置缓存机制，解决了世界加载早期访问配置导致的 `IllegalStateException` 崩溃。
*   **[进行中] 数据组件 (Data Components)**: 
    *   **[待确认]** 确保所有物品数据（尤其是物质团块 Clump）完全弃用 NBT，转向 Data Components。
*   **[进行中] 网络协议 (Networking)**: 
    *   **[待优化]** 将 Packet 处理函数逻辑与数据包类分离，防止服务端加载客户端类导致的潜在崩溃。

### 1.2 KubeJS 21 集成 (Smart-Refactor v3)
*   **[已完成] Schema 注册重构**: 迁移至 `RecipeSchemaRegistry` 和事件驱动模型。
*   **[已完成] 自定义组件**: 手动实现 `SimulationIngredient` 的 `RecipeComponent`，支持 Codec 自动包装。
*   **[已完成] 配方支持**: 
    *   支持 `overheating`, `thermal_shock`, `thermal_shock_filling`, `thermal_fuel`, `thermal_converter`。
    *   **[新增]** 支持 `clump_processing` (别名 `extraction`)。
*   **[已完成] 依赖优化**: 重新配置 `build.gradle`，显式引入 `rhino` 和 `kubejs-neoforge` 最新构建版本。

### 1.3 游戏功能增强
*   **[已完成] JEI 集成**: 
    *   实现了自定义分类和渲染器。
    *   **[修复]** 修复了团块提取配方在 JEI 中无法正确转移（+号）的问题，现已重定向至通用处理逻辑。
*   **[进行中] 多块结构 (Multiblock)**:
    *   **[已完成]** 基础验证逻辑与 Config 绑定。
    *   **[待办]** 多块验证限流优化，防止大规模结构验证导致的 Tick 卡顿。

## 2. 后续任务列表

1.  **[P0] 稳定性维护**: 持续关注 Config 加载顺序，确保缓存机制在所有环境（单机/联机）表现一致。
2.  **[P1] 架构清理**: 
    *   审查 `SimulationChamberBlockEntity` 的 `onLoad` 和 `saveAdditional` 逻辑，确保数据同步使用组件。
    *   移除所有代码中残余的 `stack.getTag()` 调用。
3.  **[P2] 数据映射 (Data Maps)**: 
    *   考虑将机器效率、方块热传导系数等静态数据从 Java 代码移至 `data_maps`。
4.  **[P3] 文档更新**: 同步更新 `Wiki_KubeJS_ZH.md` 以反映 v21 的新 Schema 语法。

## 3. 已知修复日志
*   **2026-02-01**: 修复了 Config 早期访问崩溃。
*   **2026-02-01**: 完成 KubeJS 21 核心集成，支持全系 6 种配方。
*   **2026-02-01**: 移除了过时的 `kubejs.plugins.txt` 注册方式。
