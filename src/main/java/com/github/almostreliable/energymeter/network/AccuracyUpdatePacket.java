package com.github.almostreliable.energymeter.network;

import com.github.almostreliable.energymeter.core.Constants.SYNC_FLAGS;
import com.github.almostreliable.energymeter.meter.MeterMenu;
import com.github.almostreliable.energymeter.util.TypeEnums.TEXT_BOX;
import com.github.almostreliable.energymeter.util.Utils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AccuracyUpdatePacket(TEXT_BOX identifier, int value) implements CustomPacketPayload {
    static final Type<AccuracyUpdatePacket> TYPE = new Type<>(Utils.getRL("accuracy_update"));
    static final StreamCodec<FriendlyByteBuf, AccuracyUpdatePacket> STREAM_CODEC = CustomPacketPayload.codec(
            AccuracyUpdatePacket::encode,
            AccuracyUpdatePacket::new);

    public AccuracyUpdatePacket(FriendlyByteBuf buffer) {
        this(TEXT_BOX.values()[buffer.readInt()], buffer.readInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(identifier.ordinal());
        buffer.writeInt(value);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AccuracyUpdatePacket payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            var entity = ((MeterMenu) player.containerMenu).getEntity();
            var level = entity.getLevel();
            if (level == null || !level.isLoaded(entity.getBlockPos())) return;
            if (payload.identifier == TEXT_BOX.INTERVAL) {
                entity.setInterval(payload.value);
                entity.syncData(SYNC_FLAGS.INTERVAL);
            } else if (payload.identifier == TEXT_BOX.THRESHOLD) {
                entity.setThreshold(payload.value);
                entity.syncData(SYNC_FLAGS.THRESHOLD);
            }
            entity.setChanged();
        }
    }
}
