package com.github.almostreliable.energymeter.compat.cct;

import com.github.almostreliable.energymeter.compat.ICapabilityAdapter;
import com.github.almostreliable.energymeter.meter.MeterBlockEntity;
import dan200.computercraft.shared.Capabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class PeripheralAdapter implements ICapabilityAdapter<MeterPeripheral> {

    private final LazyOptional<MeterPeripheral> lazyAdapter;

    public PeripheralAdapter(MeterBlockEntity entity) {
        lazyAdapter = LazyOptional.of(() -> new MeterPeripheral(entity));
    }

    @Override
    public boolean isCapability(Capability<?> cap) {
        return cap.equals(Capabilities.CAPABILITY_PERIPHERAL);
    }

    @Override
    public LazyOptional<MeterPeripheral> getLazyAdapter() {
        return lazyAdapter;
    }
}
