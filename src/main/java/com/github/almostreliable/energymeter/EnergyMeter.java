package com.github.almostreliable.energymeter;

import com.almostreliable.energymeter.ModConstants;
import com.github.almostreliable.energymeter.client.ClientHandler;
import com.github.almostreliable.energymeter.core.Registration;
import com.github.almostreliable.energymeter.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ModConstants.MOD_ID)
public final class EnergyMeter {

    public static final Logger LOGGER = LogUtils.getLogger();

    public EnergyMeter(IEventBus modEventBus, Dist dist) {
        Registration.init(modEventBus);
        PacketHandler.init(modEventBus);

        if (dist.isClient()) {
            modEventBus.addListener(ClientHandler::onRegisterMenu);
            modEventBus.addListener(ClientHandler::registerEntityRenders);
        }
    }
}
