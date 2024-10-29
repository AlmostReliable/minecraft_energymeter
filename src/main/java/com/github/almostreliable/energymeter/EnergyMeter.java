package com.github.almostreliable.energymeter;

import com.almostreliable.energymeter.ModConstants;

import com.github.almostreliable.energymeter.core.Registration;
import com.github.almostreliable.energymeter.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ModConstants.MOD_ID)
public final class EnergyMeter {

    public static final Logger LOGGER = LogUtils.getLogger();

    public EnergyMeter(IEventBus modEventBus) {
        Registration.init(modEventBus);
        PacketHandler.init(modEventBus);
    }
}
