// server_scripts/thermalshock_fuels.js

ServerEvents.recipes(event => {
    
    // 加热器燃料: 烈焰粉
    // 燃烧 400 ticks (20秒)，每 tick 提供 300 点热量
    event.recipes.thermalshock.thermal_fuel('minecraft:blaze_powder', 400, 300)
    .id('kubejs:blaze_powder_fuel');

    // 冷却器燃料: 冰块
    // 燃烧 1200 ticks (60秒)，每 tick 提供 -200 点热量 (制冷)
    event.recipes.thermalshock.thermal_fuel('minecraft:ice', 1200, -200)
    .id('kubejs:ice_coolant');

});
