// server_scripts/thermalshock_recipes.js

ServerEvents.recipes(event => {
    
    // 1. 过热加工 (Overheating)
    // 将铁锭通过高热输入加工成金锭
    event.recipes.thermalshock.overheating([
        { value: 'minecraft:iron_ingot', type: 'item' }
    ], 'minecraft:gold_ingot', 500, 2000)
    .id('kubejs:iron_to_gold_overheating');

    // 2. 热冲击 (Thermal Shock)
    // 将玻璃粉碎成玻璃板
    event.recipes.thermalshock.thermal_shock([
        { value: 'minecraft:glass', type: 'item' }
    ], '4x minecraft:glass_pane', 150, -50, 200)
    .id('kubejs:glass_shattering');

    // 3. 物质填充 (Shock Filling)
    // 制作一个产出为钻石的团块
    event.recipes.thermalshock.thermal_shock_filling([
        { value: 'minecraft:coal_block', type: 'item' },
        { value: 'thermalshock:material_clump', type: 'item' }
    ], 'minecraft:diamond', 200, 0, 200, 1000, 5000)
    .id('kubejs:diamond_clump_filling');

    // 4. 热力转换器 (Thermal Converter)
    // 圆石熔化成岩浆
    event.recipes.thermalshock.thermal_converter(
        [{ ingredient: { item: 'minecraft:cobblestone' }, count: 1, consume_chance: 1.0 }],
        [], // 无流体输入
        [], // 无物品输出
        [{ fluid: { fluid: 'minecraft:lava', amount: 250 }, chance: 1.0 }],
        200, // 200 ticks
        500, // min_heat
        2000000 // max_heat (无上限)
    ).id('kubejs:cobble_melting');

});
