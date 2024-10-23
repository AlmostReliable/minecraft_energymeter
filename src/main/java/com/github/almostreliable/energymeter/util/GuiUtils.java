package com.github.almostreliable.energymeter.util;

import com.github.almostreliable.energymeter.util.TypeEnums.TRANSLATE_TYPE;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public final class GuiUtils {

    private GuiUtils() {}

    /**
     * Draws a given text at the given position with the given color.
     * <p>
     * This method handles the translation and scaling of the text in order
     * to maintain the same position after the scaling.
     *
     * @param guiGraphics the gui graphics
     * @param x           the x position
     * @param y           the y position
     * @param scale       the scale of the text
     * @param text        the text to draw
     * @param color       the color of the text as decimal
     */
    public static void renderText(GuiGraphics guiGraphics, int x, int y, float scale, String text, int color) {
        PoseStack stack = guiGraphics.pose();
        stack.pushPose();
        stack.translate(x, y, 0);
        stack.scale(scale, scale, 1);
        guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color);
        stack.popPose();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class TooltipBuilder {

        private final List<Component> value = new ArrayList<>();

        private TooltipBuilder() {}

        public static TooltipBuilder builder() {
            return new TooltipBuilder();
        }

        public Component resolve() {
            MutableComponent component = Component.empty();
            for (Component e : value) {
                component = component.append(e).append("\n");
            }
            return component;
        }

        public List<Component> resolveList() {
            return value;
        }

        /**
         * Adds a blank line to the tooltip.
         *
         * @return the instance of the tooltip
         */
        public TooltipBuilder addBlankLine() {
            return addComponent(Component.literal(" "));
        }

        /**
         * Adds a generic component to the tooltip.
         *
         * @param component the component to addComponent
         * @return the instance of the tooltip
         */
        public TooltipBuilder addComponent(Component component) {
            value.add(component);
            return this;
        }

        /**
         * Adds a header component to the tooltip.
         *
         * @param key the key of the translation
         * @return the instance of the tooltip
         */
        public TooltipBuilder addHeader(String key) {
            return addComponent(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, key, ChatFormatting.GOLD));
        }

        /**
         * Adds a description component to the tooltip.
         *
         * @param key the key of the translation
         * @return the instance of the tooltip
         */
        public TooltipBuilder addDescription(String key) {
            return addComponent(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, key, ChatFormatting.WHITE));
        }

        /**
         * Adds a custom action component to the tooltip.
         *
         * @param input the key of the {@link InputConstants}
         * @param key   the key of the translation
         * @return the instance of the tooltip
         */
        public TooltipBuilder addCustomAction(String input, String key) {
            return addComponent(TextUtils
                .colorize("> ", ChatFormatting.GRAY)
                .append(TextUtils.colorize(InputConstants.getKey(input).getDisplayName().getString(),
                    ChatFormatting.AQUA
                ))
                .append(" ")
                .append(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, key, ChatFormatting.GRAY)));
        }

        /**
         * Adds a click action component to the tooltip.
         *
         * @param key the key of the translation
         * @return the instance of the tooltip
         */
        public TooltipBuilder addClickAction(String key) {
            return addComponent(TextUtils
                .colorize("> ", ChatFormatting.GRAY)
                .append(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, "action_click", ChatFormatting.AQUA))
                .append(" ")
                .append(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, key, ChatFormatting.GRAY)));
        }

        /**
         * Adds a hold action component to the tooltip.
         *
         * @param input the key of the {@link InputConstants}
         * @param key   the key of the translation
         * @return the instance of the tooltip
         */
        public TooltipBuilder addHoldAction(String input, String key) {
            return addComponent(TextUtils
                .colorize("> ", ChatFormatting.GRAY)
                .append(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, "action_hold", ChatFormatting.GRAY))
                .append(" ")
                .append(TextUtils.colorize(InputConstants.getKey(input).getDisplayName().getString(),
                    ChatFormatting.AQUA
                ))
                .append(" ")
                .append(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, key, ChatFormatting.GRAY)));
        }

        /**
         * Adds a shift click action component to the tooltip.
         *
         * @param key the key of the translation
         * @return the instance of the tooltip
         */
        public TooltipBuilder addShiftClickAction(String key) {
            return addComponent(TextUtils
                .colorize("> ", ChatFormatting.GRAY)
                .append(TextUtils.colorize(String.format("%s + %s",
                    InputConstants.getKey("key.keyboard.left.shift").getDisplayName().getString(),
                    TextUtils.translateAsString(TRANSLATE_TYPE.TOOLTIP, "action_click")
                ), ChatFormatting.AQUA))
                .append(" ")
                .append(TextUtils.translate(TRANSLATE_TYPE.TOOLTIP, key, ChatFormatting.GRAY)));
        }
    }
}
