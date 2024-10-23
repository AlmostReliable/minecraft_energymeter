package com.github.almostreliable.energymeter;

import com.almostreliable.energymeter.ModConstants;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = ModConstants.MOD_ID, dist = Dist.CLIENT)
public final class EnergyMeterClient {

    public EnergyMeterClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerRenderers);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {

    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }
}
