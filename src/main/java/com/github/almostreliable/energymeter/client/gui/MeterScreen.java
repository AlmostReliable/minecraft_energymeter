package com.github.almostreliable.energymeter.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.github.almostreliable.energymeter.meter.MeterMenu;
import com.github.almostreliable.energymeter.util.GuiUtils;
import com.github.almostreliable.energymeter.util.GuiUtils.TooltipBuilder;
import com.github.almostreliable.energymeter.util.TextUtils;
import com.github.almostreliable.energymeter.util.TypeEnums.ACCURACY;
import com.github.almostreliable.energymeter.util.TypeEnums.BLOCK_SIDE;
import com.github.almostreliable.energymeter.util.TypeEnums.MODE;
import com.github.almostreliable.energymeter.util.TypeEnums.NUMBER_MODE;
import com.github.almostreliable.energymeter.util.TypeEnums.SETTING;
import com.github.almostreliable.energymeter.util.TypeEnums.TRANSLATE_TYPE;

import java.util.ArrayList;
import java.util.Collection;

import static com.github.almostreliable.energymeter.core.Constants.ACCURACY_ID;
import static com.github.almostreliable.energymeter.core.Constants.IO_MODE_ID;
import static com.github.almostreliable.energymeter.core.Constants.IO_SCREEN_ID;
import static com.github.almostreliable.energymeter.core.Constants.IO_SIDE_ID;
import static com.github.almostreliable.energymeter.core.Constants.METER_ID;
import static com.github.almostreliable.energymeter.core.Constants.MODE_ID;
import static com.github.almostreliable.energymeter.core.Constants.SIDE_CONFIG_ID;
import static com.github.almostreliable.energymeter.core.Constants.STATUS_ID;
import static com.github.almostreliable.energymeter.core.Constants.TRANSFER_RATE_ID;
import static com.github.almostreliable.energymeter.core.Constants.UI_COLORS;

public class MeterScreen extends AbstractContainerScreen<MeterMenu> {

    private static final ResourceLocation TEXTURE = TextUtils.getRL("textures/gui/meter.png");
    private static final int TEXTURE_WIDTH = 199;
    private static final int TEXTURE_HEIGHT = 129;
    private static final TooltipBuilder TOOLTIP = setupTooltip();
    private final Collection<AbstractWidget> toRender = new ArrayList<>();
    private IntervalBox intervalBox;
    private ThresholdBox thresholdBox;

    @SuppressWarnings("AssignmentToSuperclassField")
    public MeterScreen(MeterMenu container, Inventory inventory, Component name) {
        super(container, inventory, name);
        imageWidth = TEXTURE_WIDTH;
        imageHeight = TEXTURE_HEIGHT;
    }

    private static TooltipBuilder setupTooltip() {
        return TooltipBuilder
            .builder()
            .addHeader(SIDE_CONFIG_ID)
            .addBlankLine()
            .addComponent(TextUtils
                .translate(TRANSLATE_TYPE.TOOLTIP, IO_SIDE_ID, ChatFormatting.GREEN)
                .append(TextUtils.colorize(": ", ChatFormatting.GREEN))
                .append(TextUtils.translate(
                    TRANSLATE_TYPE.BLOCK_SIDE,
                    BLOCK_SIDE.FRONT.toString().toLowerCase(),
                    ChatFormatting.WHITE
                )))
            .addComponent(TextUtils
                .translate(TRANSLATE_TYPE.TOOLTIP, IO_MODE_ID, ChatFormatting.GREEN)
                .append(TextUtils.colorize(": ", ChatFormatting.GREEN))
                .append(TextUtils.translate(TRANSLATE_TYPE.IO_SETTING, IO_SCREEN_ID, ChatFormatting.WHITE)));
    }

    @Override
    protected void init() {
        super.init();

        intervalBox = new IntervalBox(this, font, leftPos + 18, topPos + imageHeight + 5, 42, 8);
        addRenderable(intervalBox);

        thresholdBox = new ThresholdBox(this, font, leftPos + 81, topPos + imageHeight + 5, 42, 8);
        addRenderable(thresholdBox);

        addRenderables(IOButton.create(this, BLOCK_SIDE.values()));
        addRenderable(new SettingButton(this, 136, 64, SETTING.NUMBER));
        addRenderable(new SettingButton(this, 136, 86, SETTING.MODE));
        addRenderable(new SettingButton(this, 136, 108, SETTING.ACCURACY));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mX, int mY, float partial) {
        super.render(guiGraphics, mX, mY, partial);
        renderTooltip(guiGraphics, mX, mY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mX, int mY) {
        // front screen tooltip
        if (isWithinRegion(mX, mY, 159, 16, 23, 16)) {
            guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, TOOLTIP.resolveList(), mX, mY);
            return;
        }
        super.renderTooltip(guiGraphics, mX, mY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pX, int pY) {
        // header
        GuiUtils.renderText(
            guiGraphics,
            11,
            9,
            1.3f,
            TextUtils.translateAsString(TRANSLATE_TYPE.LABEL, METER_ID),
            UI_COLORS.WHITE
        );

        // transfer rate
        GuiUtils.renderText(
            guiGraphics,
            11,
            26,
            1.1f,
            TextUtils.translateAsString(TRANSLATE_TYPE.LABEL, TRANSFER_RATE_ID) + ':',
            UI_COLORS.GRAY
        );
        var formattedFlow = TextUtils.formatEnergy(
            menu.getEntity().getTransferRate(),
            menu.getEntity().getNumberMode() == NUMBER_MODE.LONG
        );
        GuiUtils.renderText(
            guiGraphics,
            16,
            37,
            1.0f,
            String.format("%s %s/t", formattedFlow.getA(), formattedFlow.getB()),
            UI_COLORS.MINT
        );

        // status
        GuiUtils.renderText(
            guiGraphics,
            11,
            50,
            1.1f,
            TextUtils.translateAsString(TRANSLATE_TYPE.LABEL, STATUS_ID) + ':',
            UI_COLORS.GRAY
        );
        GuiUtils.renderText(
            guiGraphics,
            16,
            61,
            1.0f,
            TextUtils.translateAsString(TRANSLATE_TYPE.STATUS, menu.getEntity().getStatus().toString().toLowerCase()),
            getStatusColor()
        );

        // mode
        GuiUtils.renderText(
            guiGraphics,
            11,
            74,
            1.1f,
            TextUtils.translateAsString(TRANSLATE_TYPE.LABEL, MODE_ID) + ':',
            UI_COLORS.GRAY
        );
        GuiUtils.renderText(
            guiGraphics,
            16,
            85,
            1.0f,
            TextUtils.translateAsString(TRANSLATE_TYPE.MODE, menu.getEntity().getMode().toString().toLowerCase()),
            getModeColor()
        );

        // accuracy
        GuiUtils.renderText(
            guiGraphics,
            11,
            98,
            1.1f,
            TextUtils.translateAsString(TRANSLATE_TYPE.LABEL, ACCURACY_ID) + ':',
            UI_COLORS.GRAY
        );
        GuiUtils.renderText(
            guiGraphics,
            16,
            109,
            1.0f,
            TextUtils.translateAsString(
                TRANSLATE_TYPE.ACCURACY,
                menu.getEntity().getAccuracy().toString().toLowerCase()
            ),
            getAccuracyColor()
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partial, int mX, int mY) {
        // background texture
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    private void addRenderable(AbstractWidget widget) {
        addRenderableWidget(widget);
        toRender.add(widget);
    }

    /**
     * Convenience method to add multiple widgets at once.
     *
     * @param widgets the list of widgets to add
     */
    private void addRenderables(Iterable<? extends AbstractWidget> widgets) {
        for (var widget : widgets) {
            addRenderableWidget(widget);
            toRender.add(widget);
        }
    }

    /**
     * Checks if the mouse cursor is within a specified region.
     *
     * @param mX     mouse position on the x-axis
     * @param mY     mouse position on the y-axis
     * @param pX     left position on the x-axis
     * @param width  width to calculate the boundary on the x-axis
     * @param pY     top position on the y-axis
     * @param height height to calculate the boundary on the y-axis
     * @return true if the cursor is within the region, false otherwise
     */
    @SuppressWarnings("SameParameterValue")
    private boolean isWithinRegion(int mX, int mY, int pX, int width, int pY, int height) {
        return mX >= leftPos + pX && mX <= leftPos + pX + width && mY >= topPos + pY && mY <= topPos + pY + height;
    }

    IntervalBox getIntervalBox() {
        return intervalBox;
    }

    ThresholdBox getThresholdBox() {
        return thresholdBox;
    }

    /**
     * Gets a color representing the current status.
     *
     * @return the color of the current status
     */
    private int getStatusColor() {
        var status = menu.getEntity().getStatus();
        return switch (status) {
            case DISCONNECTED -> UI_COLORS.RED;
            case CONNECTED -> UI_COLORS.YELLOW;
            case TRANSFERRING -> UI_COLORS.GREEN;
            case CONSUMING -> UI_COLORS.ROSE;
        };
    }

    /**
     * Gets a color representing the current mode.
     *
     * @return the color of the current mode
     */
    private int getModeColor() {
        return menu.getEntity().getMode() == MODE.CONSUMER ? UI_COLORS.PURPLE : UI_COLORS.BLUE;
    }

    /**
     * Gets a color representing the current accuracy mode.
     *
     * @return the color of the current accuracy mode
     */
    private int getAccuracyColor() {
        return menu.getEntity().getAccuracy() == ACCURACY.EXACT ? UI_COLORS.ORANGE : UI_COLORS.PINK;
    }
}
