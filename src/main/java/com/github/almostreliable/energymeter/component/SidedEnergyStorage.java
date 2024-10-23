package com.github.almostreliable.energymeter.component;

import net.minecraft.core.Direction;

import com.github.almostreliable.energymeter.meter.MeterBlockEntity;
import com.github.almostreliable.energymeter.util.TypeEnums.IO_SETTING;
import com.github.almostreliable.energymeter.util.TypeEnums.MODE;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Arrays;
import java.util.List;

public record SidedEnergyStorage(MeterBlockEntity parent, Direction side) implements IEnergyStorage {

    /**
     * Creates a new instance for each direction.
     * Each of them will have the passed in parent and the direction linked to it.
     *
     * @param parent the parent entity
     * @return a list of all created instances
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static List<LazyOptional<SidedEnergyStorage>> create(MeterBlockEntity parent) {
        return Arrays
            .stream(Direction.values())
            .map(direction -> LazyOptional.of(() -> new SidedEnergyStorage(parent, direction)))
            .toList();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        return parent.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return 0;
    }

    @Override
    public boolean canExtract() {
        return parent.getSideConfig().get(side) == IO_SETTING.OUT && parent.getSideConfig().hasInput();
    }

    @Override
    public boolean canReceive() {
        if (parent.getMode() == MODE.CONSUMER) {
            return parent.getSideConfig().get(side) == IO_SETTING.IN;
        }
        return parent.getSideConfig().get(side) == IO_SETTING.IN && parent.getSideConfig().hasOutput();
    }
}
