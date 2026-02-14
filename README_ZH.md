# Thermal Shock (热冲击) - NeoForge 1.21.1

[English Version](./README.md)

![AI Driven](https://img.shields.io/badge/Developed%20with-AI-blueviolet)
![NeoForge](https://img.shields.io/badge/Minecraft-1.21.1-orange)
![License](https://img.shields.io/badge/License-MIT-green)

## 🤖 AI 开发声明

**本项目是一个极具实验性的 AI 全流程开发案例：**
本模组 **99%** 的内容均由人工智能生成，涵盖了代码实现、纹理绘制、数值设计及项目文档。

- **核心架构与逻辑：** 由 **Gemini 3 Pro** 及 **Gemini 3 Flash** 驱动构建。
- **本地化与创意命名：** 部分翻译、机器及模组名称由 **DeepSeek** 生成。

### ⚠️ 维护与贡献指南
由于本项目深度依赖 AI 驱动，开发者本人并不具备脱离 AI 独立进行大规模代码重构或底层调试的能力。
- **维护模式：** 所有修复与功能更新均需通过 AI 协同完成。
- **项目周期：** 本模组专为开发者个人的整合包量身定制。我仅会在该整合包开发期间提供有限的维护支持。一旦整合包运行稳定，本项目将**停止后续维护**。
- **贡献建议：** 如果您希望改进此模组或进行长期扩展，**强烈建议您 Fork 本仓库**进行独立开发。

---

## 🌋 模组简介

**Thermal Shock** 是一个专为空岛（Skyblock）生存设计的资源生产模组。它引入了一套独特的热力管理加工体系，玩家通过构建多方块结构并控制热量/冷量的输入，实现资源的自动化转化与产出。

### 核心特性
*   **模拟室 (Simulation Chamber)：** 一个可自定义尺寸（最高 13x13）的多方块结构，支持**物理实体扫描**或通过**虚拟化升级**实现高效处理。
*   **双重加工逻辑：**
    *   **过热模式 (Overheating)：** 侧重于高额热量的持续输入，用于物品的熔融、干燥或重组。
    *   **热冲击模式 (Thermal Shock)：** 侧重于高温与低温之间的差值，通过温差应力实现物质的粉碎与精炼。
*   **物质团块 (Material Clump)：** 模组的核心中间产物，作为动态数据载体存储了产出信息与处理需求。
*   **热力转换器 (Thermal Converter)：** 紧凑的单方块机器，用于处理基础的物品/流体热力转换配方。
*   **KubeJS 原生适配：** 完美支持脚本扩展。详见 [KubeJS 集成指南](./wiki/KubeJS-Integration-ZH.md)。

## 🛠️ 安装要求
*   **Minecraft:** 1.21.1
*   **NeoForge:** 21.1.x 或更高版本
*   **依赖项:** KubeJS (可选，用于自定义配方)

---

## 💖 鸣谢 (Acknowledgments)
*   本项目使用了来自 [unused-textures](https://github.com/malcolmriley/unused-textures) 仓库的优秀材质。
