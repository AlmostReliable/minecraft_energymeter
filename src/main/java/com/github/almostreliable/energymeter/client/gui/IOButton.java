package com.github.almostreliable.energymeter.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Tuple;

import com.github.almostreliable.energymeter.network.IOUpdatePacket;
import com.github.almostreliable.energymeter.network.PacketHandler;
import com.github.almostreliable.energymeter.util.GuiUtils.TooltipBuilder;
import com.github.almostreliable.energymeter.util.TextUtils;
import com.github.almostreliable.energymeter.util.TypeEnums.BLOCK_SIDE;
import com.github.almostreliable.energymeter.util.TypeEnums.IO_SETTING;
import com.github.almostreliable.energymeter.util.TypeEnums.TRANSLATE_TYPE;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

import static com.github.almostreliable.energymeter.core.Constants.IO_MODE_ID;
import static com.github.almostreliable.energymeter.core.Constants.IO_SIDE_ID;
import static com.github.almostreliable.energymeter.core.Constants.SIDE_CONFIG_ID;

final class IOButton extends GenericButton {

    private static final String TEXTURE = "io";
    private static final int POS_X = 141;
    private static final int POS_Y = 5;
    private static final int TEXTURE_WIDTH = 34;
    private static final int TEXTURE_HEIGHT = 34;
    private static final int BUTTON_SIZE = 17;
    private static final int ZONE_SIZE = 18;
    private static final int OVERLAY_SIZE = 17;
    private final BLOCK_SIDE side;
    private TooltipBuilder tooltip;
    private IO_SETTING setting;

    private IOButton(MeterScreen screen, BLOCK_SIDE side) {
        super(screen, POS_X + getButtonPos(side).getA(), POS_Y + getButtonPos(side).getB(), BUTTON_SIZE, BUTTON_SIZE);
        this.side = side;
        setting = container.getEntity().getSideConfig().get(side);
        tooltip = setupTooltip();
    }

    /**
     * Creates an io button for each passed in block side.
     *
     * @param screen the screen to create the buttons for
     * @param sides  the sides for which the buttons should be created
     * @return a list of all buttons created
     */
    static List<IOButton> create(MeterScreen screen, BLOCK_SIDE... sides) {
        return Arrays
            .stream(sides)
            .filter(side -> side != BLOCK_SIDE.FRONT)
            .map(side -> new IOButton(screen, side))
            .toList();
    }

    /**
     * Returns the x and y positions for the texture depending on the block side.
     *
     * @param side the block side to get the positions for
     * @return the x and y position for the BLOCK_SIDE
     */
    private static Tuple<Integer, Integer> getButtonPos(BLOCK_SIDE side) {
        return switch (side) {
            case TOP -> new Tuple<>(ZONE_SIZE, 0);
            case LEFT -> new Tuple<>(0, ZONE_SIZE);
            case RIGHT -> new Tuple<>(ZONE_SIZE * 2, ZONE_SIZE);
            case BOTTOM -> new Tuple<>(ZONE_SIZE, ZONE_SIZE * 2);
            case BACK -> new Tuple<>(ZONE_SIZE * 2, ZONE_SIZE * 2);
            case FRONT -> new Tuple<>(0, 0);
        };
    }

    @Override
    public void onClick(double mX, double mY) {
        if (isHovered) changeMode(Screen.hasShiftDown());
        super.onClick(mX, mY);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mX, int mY, float partial) {
        super.renderWidget(guiGraphics, mX, mY, partial);
        renderIOOverlay(guiGraphics);
    }

    @Override
    protected void clickHandler() {
        PacketDistributor.sendToServer(new IOUpdatePacket(side, setting));
        tooltip = setupTooltip();
    }

    @Override
    protected String getTexture() {
        return TEXTURE;
    }

    @Override
    protected int getTextureWidth() {
        return TEXTURE_WIDTH;
    }

    @Override
    protected int getTextureHeight() {
        return TEXTURE_HEIGHT;
    }

    @Override
    protected TooltipBuilder getTooltipBuilder() {
        return tooltip;
    }

    private TooltipBuilder setupTooltip() {
        return TooltipBuilder.builder()
            // header
            .addHeader(SIDE_CONFIG_ID).addBlankLine()
            // block side
            .addComponent(TextUtils
                .translate(TRANSLATE_TYPE.TOOLTIP, IO_SIDE_ID, ChatFormatting.GREEN)
                .append(TextUtils.colorize(": ", ChatFormatting.GREEN))
                .append(TextUtils.translate(
                    TRANSLATE_TYPE.BLOCK_SIDE,
                    side.toString().toLowerCase(),
                    ChatFormatting.WHITE
                )))
            // current mode
            .addComponent(TextUtils
                .translate(TRANSLATE_TYPE.TOOLTIP, IO_MODE_ID, ChatFormatting.GREEN)
                .append(TextUtils.colorize(": ", ChatFormatting.GREEN))
                .append(TextUtils.translate(
                    TRANSLATE_TYPE.IO_SETTING,
                    setting.toString().toLowerCase(),
                    ChatFormatting.WHITE
                ))).addBlankLine()
            // action
            .addClickAction("action_1").addShiftClickAction("action_2");
    }

    /**
     * Renders the I/O overlay for the button depending on its io setting.
     *
     * @param stack the pose stack for the render call
     */
    private void renderIOOverlay(GuiGraphics guiGraphics) {
        var textureOffset = (setting.ordinal() - 1) * OVERLAY_SIZE;
        if (textureOffset >= 0) {
            guiGraphics.blit(
                TextUtils.getRL("textures/gui/buttons/" + getTexture() + ".png"),
                getX(),
                getY(),
                BUTTON_SIZE,
                textureOffset,
                OVERLAY_SIZE,
                OVERLAY_SIZE,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
            );
        }
    }

    /**
     * Changes the mode of a block side depending on its current io setting.
     *
     * @param reset whether the field should be reset to OFF
     */
    private void changeMode(boolean reset) {
        if (reset) {
            setting = IO_SETTING.OFF;
            return;
        }

        var sideConfig = container.getEntity().getSideConfig();

        setting = switch (setting) {
            case OFF -> sideConfig.hasInput() ? IO_SETTING.OUT : IO_SETTING.IN;
            case IN -> sideConfig.hasMaxOutputs() ? IO_SETTING.OFF : IO_SETTING.OUT;
            case OUT -> IO_SETTING.OFF;
        };
    }
}
