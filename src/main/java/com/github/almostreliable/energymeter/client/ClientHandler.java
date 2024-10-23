package com.github.almostreliable.energymeter.client;

import com.github.almostreliable.energymeter.client.gui.MeterScreen;
import com.github.almostreliable.energymeter.core.Registration;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * A separate class to make sure it's less likely to be class-loaded.
 */
public class ClientHandler {

    public static void onRegisterMenu(final RegisterMenuScreensEvent event) {
        event.register(Registration.METER.get(), MeterScreen::new);
    }

    public static void registerEntityRenders(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.METER_BLOCK_ENTITY.get(), MeterRenderer::new);
    }
}
