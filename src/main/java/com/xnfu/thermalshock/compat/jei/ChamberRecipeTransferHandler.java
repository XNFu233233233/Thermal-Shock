package com.xnfu.thermalshock.compat.jei;

import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import com.xnfu.thermalshock.client.gui.SimulationChamberMenu;
import com.xnfu.thermalshock.network.PacketSelectRecipe;
import com.xnfu.thermalshock.recipe.AbstractSimulationRecipe;
import com.xnfu.thermalshock.util.RecipeCache;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ChamberRecipeTransferHandler<T extends AbstractSimulationRecipe> implements IRecipeTransferHandler<SimulationChamberMenu, T> {

    private final Class<SimulationChamberMenu> containerClass;
    private final Optional<MenuType<SimulationChamberMenu>> menuType;
    private final RecipeType<T> recipeType;

    public ChamberRecipeTransferHandler(Class<SimulationChamberMenu> containerClass, MenuType<SimulationChamberMenu> menuType, RecipeType<T> recipeType) {
        this.containerClass = containerClass;
        this.menuType = Optional.of(menuType);
        this.recipeType = recipeType;
    }

    @Override
    public Class<? extends SimulationChamberMenu> getContainerClass() {
        return containerClass;
    }

    @Override
    public Optional<MenuType<SimulationChamberMenu>> getMenuType() {
        return menuType;
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(SimulationChamberMenu container, T recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        SimulationChamberBlockEntity be = container.getBlockEntity();
        MachineMode machineMode = be.getMachineMode();
        MachineMode recipeMode = recipe.getMachineMode();

        // 1. 检查模式是否匹配
        if (machineMode != recipeMode) {
            return () -> IRecipeTransferError.Type.USER_FACING;
        }

        // 2. 执行转移 (选择配方)
        if (doTransfer) {
            ResourceLocation id;
            // [特殊处理] 如果是团块提取配方，统一重定向到“通用处理”ID
            if (recipe instanceof com.xnfu.thermalshock.recipe.ClumpProcessingRecipe) {
                id = ResourceLocation.fromNamespaceAndPath("thermalshock", "generic_clump_processing");
            } else {
                id = findRecipeId(recipe);
            }
            
            if (id != null) {
                PacketDistributor.sendToServer(new PacketSelectRecipe(be.getBlockPos(), id));
            }
        }

        return null;
    }

    private ResourceLocation findRecipeId(T recipe) {
        // 使用 RecipeCache 搜索
        for (MachineMode mode : MachineMode.values()) {
            for (RecipeHolder<? extends AbstractSimulationRecipe> holder : RecipeCache.getRecipes(mode)) {
                if (holder.value() == recipe) {
                    return holder.id();
                }
            }
        }
        return null;
    }
}
