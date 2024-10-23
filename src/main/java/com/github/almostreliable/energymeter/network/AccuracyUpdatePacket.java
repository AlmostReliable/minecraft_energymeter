package com.github.almostreliable.energymeter.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.github.almostreliable.energymeter.core.Constants.SYNC_FLAGS;
import com.github.almostreliable.energymeter.meter.MeterMenu;
import com.github.almostreliable.energymeter.network.ClientToServerPacket;
import com.github.almostreliable.energymeter.util.TypeEnums.TEXT_BOX;

import javax.annotation.Nullable;

public class AccuracyUpdatePacket extends ClientToServerPacket<AccuracyUpdatePacket> {

    private TEXT_BOX identifier;
    private int value;

    public AccuracyUpdatePacket(TEXT_BOX identifier, int value) {
        this.identifier = identifier;
        this.value = value;
    }

    public AccuracyUpdatePacket() {}

    @Override
    public void encode(AccuracyUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.identifier.ordinal());
        buffer.writeInt(packet.value);
    }

    @Override
    public AccuracyUpdatePacket decode(FriendlyByteBuf buffer) {
        return new AccuracyUpdatePacket(TEXT_BOX.values()[buffer.readInt()], buffer.readInt());
    }

    @Override
    public void handlePacket(AccuracyUpdatePacket packet, @Nullable ServerPlayer player) {
        if (player != null && player.containerMenu instanceof MeterMenu) {
            var entity = ((MeterMenu) player.containerMenu).getEntity();
            var level = entity.getLevel();
            if (level == null || !level.isLoaded(entity.getBlockPos())) return;
            if (packet.identifier == TEXT_BOX.INTERVAL) {
                entity.setInterval(packet.value);
                entity.syncData(SYNC_FLAGS.INTERVAL);
            } else if (packet.identifier == TEXT_BOX.THRESHOLD) {
                entity.setThreshold(packet.value);
                entity.syncData(SYNC_FLAGS.THRESHOLD);
            }
            entity.setChanged();
        }
    }
}
