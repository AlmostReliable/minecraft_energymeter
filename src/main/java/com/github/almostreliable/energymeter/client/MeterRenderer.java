package com.github.almostreliable.energymeter.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import com.github.almostreliable.energymeter.meter.MeterBlock;
import com.github.almostreliable.energymeter.meter.MeterBlockEntity;
import com.github.almostreliable.energymeter.util.TextUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MeterRenderer implements BlockEntityRenderer<MeterBlockEntity> {

    private static final float[] ANGLE = {0, 0, 0, 180, 90, -90};
    private static final float PIXEL_SIZE = .3f / 16;
    private static final float OFFSET = 0.001f;
    private static final int MAX_DISTANCE = 30;
    private static final float HALF = .5f;
    private static final int FULL_BRIGHTNESS = 0x00F0_0000;
    private final Font font;

    public MeterRenderer(Context context) {
        font = context.getFont();
    }

    private static Vector3f getFacingVector(Direction facing) {
        if (facing.ordinal() < 2) {
            // up or down
            return new Vector3f(HALF, facing == Direction.UP ? 1 + OFFSET : -OFFSET, HALF);
        }
        if (facing.ordinal() < 4) {
            // north or south
            return new Vector3f(HALF, HALF, facing == Direction.NORTH ? -OFFSET : 1 + OFFSET);
        }
        // west or east
        return new Vector3f(facing == Direction.WEST ? -OFFSET : 1 + OFFSET, HALF, HALF);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void render(
        MeterBlockEntity entity, float partial, PoseStack stack, MultiBufferSource buffer, int packedLight, int packedOverlay
    ) {
        // turn off display if player is too far away
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity.getBlockPos().distSqr(player.blockPosition()) > Math.pow(MAX_DISTANCE, 2)) return;

        // resolve the facing side and resolve the vector used for positioning
        var facing = entity.getBlockState().getValue(MeterBlock.FACING);
        var bottom = entity.getBlockState().getValue(MeterBlock.BOTTOM);
        var vector = getFacingVector(facing);

        stack.pushPose();

        // move and rotate the position according to the facing
        stack.translate(vector.x(), vector.y(), vector.z());
        if (facing == bottom) {
            stack.mulPose(new Quaternionf().rotateXYZ(
                0,
                ANGLE[facing.ordinal()] * Mth.DEG_TO_RAD,
                180 * Mth.DEG_TO_RAD
            ));
        } else {
            stack.mulPose(Axis.ZN.rotationDegrees(180));
            stack.mulPose(Axis.XN.rotationDegrees(facing == Direction.UP ? 90 : -90));
            stack.mulPose(Axis.ZN.rotationDegrees(
                facing == Direction.DOWN ? (180 - ANGLE[bottom.ordinal()]) : ANGLE[bottom.ordinal()]));
        }

        // scale the stack so the text fits on the screen
        stack.scale(PIXEL_SIZE, PIXEL_SIZE, 0);

        // format the current flow rate and draw it according to its size, so it's centered
        var text = TextUtils.formatEnergy(entity.getTransferRate(), false);
        var flowRate = text.getA();
        var unit = text.getB() + "/t";
        // flow rate
        drawText(flowRate, -font.lineHeight - OFFSET, stack, buffer);
        // unit
        drawText(unit, OFFSET, stack, buffer);

        stack.popPose();
    }

    @SuppressWarnings("DataFlowIssue")
    private void drawText(String text, float y, PoseStack stack, MultiBufferSource buffer) {
        font.drawInBatch(
            text,
            font.width(text) / -2f,
            y,
            ChatFormatting.WHITE.getColor(),
            false,
            stack.last().pose(),
            buffer,
            Font.DisplayMode.NORMAL,
            0,
            FULL_BRIGHTNESS
        );
    }
}
