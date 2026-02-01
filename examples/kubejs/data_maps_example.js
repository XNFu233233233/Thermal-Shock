// server_scripts/thermalshock_data_maps.js

ServerEvents.highPriorityData(event => {
    
    // 1. 修改结构外壳属性
    // 将钻石块设为顶级外壳，拥有极高的耐热性和效率
    event.addJson('thermalshock:data_maps/block/chamber_casing.json', {
        values: {
            'minecraft:diamond_block': {
                max_heat_rate: 20000,
                max_cold_rate: 20000,
                efficiency: 2.5
            },
            // 为木板提供基础等级的外壳支持
            '#minecraft:planks': {
                max_heat_rate: 100,
                max_cold_rate: 50,
                efficiency: 0.8
            }
        }
    });

    // 2. 定义热源与冷源
    // 让萤石也能作为热源工作
    event.addJson('thermalshock:data_maps/block/heat_source.json', {
        values: {
            'minecraft:glowstone': {
                heat_per_tick: 10
            }
        }
    });

    // 让雪块成为微弱冷源
    event.addJson('thermalshock:data_maps/block/cold_source.json', {
        values: {
            'minecraft:snow_block': {
                cooling_per_tick: 2
            }
        }
    });

});
