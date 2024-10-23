package com.github.almostreliable.energymeter.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.github.almostreliable.energymeter.core.Constants.SYNC_FLAGS;
import com.github.almostreliable.energymeter.meter.MeterMenu;
import com.github.almostreliable.energymeter.network.ClientToServerPacket;
import com.github.almostreliable.energymeter.util.TypeEnums.BLOCK_SIDE;
import com.github.almostreliable.energymeter.util.TypeEnums.IO_SETTING;

import javax.annotation.Nullable;

public class IOUpdatePacket extends ClientToServerPacket<IOUpdatePacket> {

    private BLOCK_SIDE side;
    private IO_SETTING setting;

    public IOUpdatePacket(BLOCK_SIDE side, IO_SETTING setting) {
        this.side = side;
        this.setting = setting;
    }

    public IOUpdatePacket() {}

    @Override
    public void encode(IOUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.side.ordinal());
        buffer.writeInt(packet.setting.ordinal());
    }

    @Override
    public IOUpdatePacket decode(FriendlyByteBuf buffer) {
        return new IOUpdatePacket(BLOCK_SIDE.values()[buffer.readInt()], IO_SETTING.values()[buffer.readInt()]);
    }

    @Override
    public void handlePacket(IOUpdatePacket packet, @Nullable ServerPlayer player) {
        if (player != null && player.containerMenu instanceof MeterMenu menu) {
            var entity = menu.getEntity();
            var level = entity.getLevel();
            if (level == null || !level.isLoaded(entity.getBlockPos())) return;
            entity.getSideConfig().set(packet.side, packet.setting);
            entity.updateNeighbors();
            entity.updateCache(entity.getSideConfig().getDirectionFromSide(packet.side));
            entity.syncData(SYNC_FLAGS.SIDE_CONFIG);
            entity.setChanged();
        }
    }
}
