package com.github.almostreliable.energymeter;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import com.almostreliable.energymeter.ModConstants;

import com.github.almostreliable.energymeter.client.MeterRenderer;
import com.github.almostreliable.energymeter.client.gui.MeterScreen;
import com.github.almostreliable.energymeter.core.Registration;
import com.github.almostreliable.energymeter.core.Setup.Containers;
import com.github.almostreliable.energymeter.core.Setup.Entities;
import com.github.almostreliable.energymeter.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(ModConstants.MOD_ID)
public final class EnergyMeter {

    public static final Logger LOGGER = LogUtils.getLogger();

    public EnergyMeter(IEventBus modEventBus) {
        Registration.init(modEventBus);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        PacketHandler.init();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(Containers.METER.get(), MeterScreen::new);
            BlockEntityRenderers.register(Entities.METER.get(), MeterRenderer::new);
        });
    }
}
