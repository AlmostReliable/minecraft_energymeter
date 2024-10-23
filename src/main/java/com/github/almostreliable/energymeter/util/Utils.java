package com.github.almostreliable.energymeter.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import com.almostreliable.energymeter.ModConstants;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public final class Utils {

    private Utils() {}

    public static ResourceLocation getRL(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, path);
    }

    public static MutableComponent translate(String type, String key, Object... args) {
        return Component.translatable(String.format("%s.%s.%s", type, ModConstants.MOD_ID, key), args);
    }

    public static void addShiftInfoTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal("» ").withStyle(ChatFormatting.AQUA).append(translate(
            "tooltip",
            "shift_for_more",
            InputConstants.getKey("key.keyboard.left.shift").getDisplayName()
        ).withStyle(ChatFormatting.GRAY)));
    }

    public static <T> T cast(Object o, Class<T> clazz) {
        return clazz.cast(o);
    }
}
