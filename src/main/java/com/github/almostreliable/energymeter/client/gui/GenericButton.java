package com.github.almostreliable.energymeter.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import com.github.almostreliable.energymeter.meter.MeterMenu;
import com.github.almostreliable.energymeter.util.GuiUtils;
import com.github.almostreliable.energymeter.util.TextUtils;

public abstract class GenericButton extends Button {

    final MeterMenu container;
    final MeterScreen screen;

    GenericButton(MeterScreen screen, int pX, int pY, int width, int height) {
        super(
            screen.getGuiLeft() + pX,
            screen.getGuiTop() + pY,
            width,
            height,
            Component.empty(),
            button -> ((GenericButton) button).clickHandler(),
            DEFAULT_NARRATION
        );
        container = screen.getMenu();
        this.screen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mX, int mY, float partial) {
        guiGraphics.blit(
            TextUtils.getRL("textures/gui/buttons/" + getTexture() + ".png"),
            getX(),
            getY(),
            0,
            0,
            width,
            height,
            getTextureWidth(),
            getTextureHeight()
        );
        setTooltip(Tooltip.create(getTooltipBuilder().resolve()));
    }

    /**
     * Handles the functionality which is triggered when clicking the button.
     * <p>
     * Can be overwritten by buttons to resolve individual functionality.
     */
    protected abstract void clickHandler();

    /**
     * Gets the texture file name for the button as {@link String}.
     * The default location points to "textures/gui/buttons".
     *
     * @return the texture file name
     */
    protected abstract String getTexture();

    /**
     * Gets the texture atlas width for the rendering calls.
     *
     * @return the texture atlas width
     */
    protected abstract int getTextureWidth();

    /**
     * Gets the texture atlas height for the rendering calls.
     *
     * @return the texture atlas height
     */
    protected abstract int getTextureHeight();

    protected abstract GuiUtils.TooltipBuilder getTooltipBuilder();
}
