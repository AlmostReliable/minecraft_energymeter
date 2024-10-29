package com.github.almostreliable.energymeter.meter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import com.github.almostreliable.energymeter.core.Registration;

import java.util.Objects;

public class MeterMenu extends AbstractContainerMenu {

    private final MeterBlockEntity entity;

    public MeterMenu(MeterBlockEntity entity, int windowID) {
        super(Registration.METER_MENU.get(), windowID);
        this.entity = entity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
            ContainerLevelAccess.create(Objects.requireNonNull(entity.getLevel()), entity.getBlockPos()),
            player,
            entity.getBlockState().getBlock()
        );
    }

    public MeterBlockEntity getEntity() {
        return entity;
    }
}
