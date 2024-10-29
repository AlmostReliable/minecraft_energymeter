package com.github.almostreliable.energymeter;

import com.almostreliable.energymeter.ModConstants;

import com.github.almostreliable.energymeter.client.MeterRenderer;
import com.github.almostreliable.energymeter.client.gui.MeterScreen;
import com.github.almostreliable.energymeter.core.Registration;
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
        event.register(Registration.METER_MENU.get(), MeterScreen::new);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.METER_BLOCK_ENTITY.get(), MeterRenderer::new);
    }
}
