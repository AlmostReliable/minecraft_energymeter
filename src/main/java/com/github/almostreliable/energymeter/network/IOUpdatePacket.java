package com.github.almostreliable.energymeter.network;

import com.github.almostreliable.energymeter.core.Constants.SYNC_FLAGS;
import com.github.almostreliable.energymeter.meter.MeterMenu;
import com.github.almostreliable.energymeter.util.TypeEnums.BLOCK_SIDE;
import com.github.almostreliable.energymeter.util.TypeEnums.IO_SETTING;
import com.github.almostreliable.energymeter.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record IOUpdatePacket(BLOCK_SIDE side, IO_SETTING setting) implements CustomPacketPayload {
    static final Type<IOUpdatePacket> TYPE = new Type<>(Utils.getRL("io_update"));
    static final StreamCodec<ByteBuf, IOUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.side.ordinal(),
            ByteBufCodecs.VAR_INT, p -> p.setting.ordinal(),
            IOUpdatePacket::new
    );

    public IOUpdatePacket(Integer sideIndex, Integer settingsIndex) {
        this(BLOCK_SIDE.values()[sideIndex], IO_SETTING.values()[settingsIndex]);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(IOUpdatePacket payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof MeterMenu menu) {
            var entity = menu.getEntity();
            var level = entity.getLevel();
            if (level == null || !level.isLoaded(entity.getBlockPos())) return;
            entity.getSideConfig().set(payload.side, payload.setting);
            entity.updateNeighbors();
            entity.updateCache(entity.getSideConfig().getDirectionFromSide(payload.side));
            entity.syncData(SYNC_FLAGS.SIDE_CONFIG);
            entity.setChanged();
        }
    }
}
