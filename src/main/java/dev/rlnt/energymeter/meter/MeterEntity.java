package dev.rlnt.energymeter.meter;

import static dev.rlnt.energymeter.core.Constants.*;

import dev.rlnt.energymeter.core.Setup;
import dev.rlnt.energymeter.energy.ISidedEnergy;
import dev.rlnt.energymeter.energy.SidedEnergyStorage;
import dev.rlnt.energymeter.network.SettingUpdatePacket;
import dev.rlnt.energymeter.util.TextUtils;
import dev.rlnt.energymeter.util.TypeEnums.*;
import java.util.*;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants.BlockFlags;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class MeterEntity extends BlockEntity implements MenuProvider, ISidedEnergy {

    private static final int REFRESH_RATE = 5;
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> outputCache = new EnumMap<>(Direction.class);
    private final List<LazyOptional<SidedEnergyStorage>> energyStorage;
    private final SideConfiguration sideConfig;
    private boolean hasValidInput = false;
    private boolean setupDone = false;
    private LazyOptional<IEnergyStorage> inputCache = null;
    private float transferRate = 0;
    private int averageRate = 0;
    private int lastAverageRate = 0;
    private int averageCount = 0;
    private STATUS status = STATUS.DISCONNECTED;
    private NUMBER_MODE numberMode = NUMBER_MODE.SHORT;
    private MODE mode = MODE.TRANSFER;

    public MeterEntity(final BlockPos pos, final BlockState state) {
        super(Setup.Entities.METER_ENTITY.get(), pos, state);
        energyStorage = SidedEnergyStorage.create(this);
        sideConfig = new SideConfiguration(state.getValue(MeterBlock.HORIZONTAL_FACING));
    }

    /**
     * Handles the actual energy transfer process.
     * <p>
     * Automatically checks if the energy to transfer can be accepted by the possible outputs.
     * It will try to equally distribute it.
     *
     * @param energy  the energy to transfer
     * @param outputs the possible outputs
     * @return the accepted amount of energy
     */
    private static int transferEnergy(final int energy, final Map<IEnergyStorage, Integer> outputs) {
        int acceptedEnergy = 0;
        int energyToTransfer = energy;
        while (!outputs.isEmpty() && energyToTransfer >= outputs.size()) {
            final int equalSplit = energyToTransfer / outputs.size();
            final List<IEnergyStorage> outputsToRemove = new ArrayList<>();

            for (final Entry<IEnergyStorage, Integer> output : outputs.entrySet()) {
                int actualSplit = equalSplit;
                if (output.getValue() < equalSplit) {
                    actualSplit = output.getValue();
                    outputsToRemove.add(output.getKey());
                }
                output.getKey().receiveEnergy(actualSplit, false);
                energyToTransfer -= actualSplit;
                acceptedEnergy += actualSplit;
            }

            outputsToRemove.forEach(outputs::remove);
        }

        return acceptedEnergy;
    }

    /**
     * Flips the IO {@link BlockState} value and returns the new {@link BlockState}.
     *
     * @return the {@link BlockState} with the flipped IO value
     */
    private BlockState flipBlockState() {
        final BlockState state = getBlockState();
        return state.setValue(MeterBlock.IO, !state.getValue(MeterBlock.IO));
    }

    /**
     * Updates the cached input and output values depending on the {@link Direction}.
     * This ensures that the current status is always up-to-date.
     *
     * @param direction the {@link Direction} to update the cache for
     */
    public void updateCache(final Direction direction) {
        if (level == null || level.isClientSide) return;

        final IO_SETTING setting = sideConfig.get(direction);
        if (setting == IO_SETTING.IN) {
            hasValidInput = getInputFromCache(direction);
        } else if (setting == IO_SETTING.OUT) {
            getOutputFromCache(direction);
        }
        if (!sideConfig.hasInput()) {
            hasValidInput = false;
            inputCache = null;
        }
    }

    public float getTransferRate() {
        return transferRate;
    }

    public STATUS getStatus() {
        if (status != STATUS.TRANSFERRING) {
            return status;
        } else {
            return mode == MODE.CONSUMER ? STATUS.CONSUMING : STATUS.TRANSFERRING;
        }
    }

    public NUMBER_MODE getNumberMode() {
        return numberMode;
    }

    /**
     * Convenience method used by the {@link SettingUpdatePacket} in order
     * to flip a specific setting after a button click on the client.
     *
     * @param setting the setting to update
     */
    public void updateSetting(final SETTING setting) {
        if (setting == SETTING.NUMBER) {
            numberMode = numberMode == NUMBER_MODE.SHORT ? NUMBER_MODE.LONG : NUMBER_MODE.SHORT;
        } else if (setting == SETTING.MODE) {
            mode = mode == MODE.TRANSFER ? MODE.CONSUMER : MODE.TRANSFER;
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        sideConfig.deserialize(tag.getIntArray(SIDE_CONFIG_ID));
        numberMode = NUMBER_MODE.values()[tag.getInt(NUMBER_MODE_ID)];
        mode = MODE.values()[tag.getInt(MODE_ID)];
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        tag.putIntArray(SIDE_CONFIG_ID, sideConfig.serialize());
        tag.putInt(NUMBER_MODE_ID, numberMode.ordinal());
        tag.putInt(MODE_ID, mode.ordinal());
        return super.save(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new ClientboundBlockEntityDataPacket(worldPosition, -1, getUpdateTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();
        tag.putIntArray(SIDE_CONFIG_ID, sideConfig.serialize());
        tag.putFloat(TRANSFER_RATE_ID, transferRate);
        tag.putInt(STATUS_ID, status.ordinal());
        tag.putInt(NUMBER_MODE_ID, numberMode.ordinal());
        tag.putInt(MODE_ID, mode.ordinal());
        return tag;
    }

    @Override
    public void setRemoved() {
        for (final LazyOptional<SidedEnergyStorage> cap : energyStorage) {
            cap.invalidate();
        }
        super.setRemoved();
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet) {
        handleUpdateTag(packet.getTag());
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        sideConfig.deserialize(tag.getIntArray(SIDE_CONFIG_ID));
        transferRate = tag.getFloat(TRANSFER_RATE_ID);
        status = STATUS.values()[tag.getInt(STATUS_ID)];
        numberMode = NUMBER_MODE.values()[tag.getInt(NUMBER_MODE_ID)];
        mode = MODE.values()[tag.getInt(MODE_ID)];
    }

    @Override
    public int receiveEnergy(final int energy, final boolean simulate) {
        if (level == null || !setupDone) return 0;

        // void the energy if consumer mode is activated
        if (mode == MODE.CONSUMER) {
            if (!simulate) {
                averageRate += energy;
                averageCount++;
            }
            return energy;
        }

        // create a map with all possible outputs and their energy limit
        final Map<IEnergyStorage, Integer> outputs = getPossibleOutputs(energy);
        if (outputs.isEmpty()) return 0;

        // get the maximum energy which could be accepted by all outputs
        final int maximumAccepted = outputs.values().stream().mapToInt(maxEnergy -> maxEnergy).sum();

        // if simulated, just check if the energy fits somewhere
        if (simulate) return Math.min(maximumAccepted, energy);

        // actual energy transfer
        final int acceptedEnergy;
        if (maximumAccepted <= energy) {
            // if maximum accepted energy is less or equal the energy to transfer, fill all outputs with their maximum
            outputs.keySet().forEach(cap -> cap.receiveEnergy(outputs.get(cap), false));
            acceptedEnergy = maximumAccepted;
        } else {
            // otherwise, push the energy to all possible outputs equally
            acceptedEnergy = transferEnergy(energy, outputs);
        }

        // adjust data for calculation in tick method
        averageRate += acceptedEnergy;
        averageCount++;

        return acceptedEnergy;
    }

    @Override
    public SideConfiguration getSideConfig() {
        return sideConfig;
    }

    @Override
    public MODE getMode() {
        return mode;
    }

    /**
     * Checks each output direction whether there is a valid energy capability.
     * It will simulate an energy transfer to this capability to make sure it can
     * accept energy and to retrieve the energy limit.
     *
     * @return a map of all possible outputs with their corresponding energy limit
     */
    private Map<IEnergyStorage, Integer> getPossibleOutputs(final int energy) {
        final Map<IEnergyStorage, Integer> outputs = new HashMap<>();
        for (final Direction direction : Direction.values()) {
            // only consider sides where output mode is enabled
            if (sideConfig.get(direction) != IO_SETTING.OUT) continue;

            // try to get the energy capability from the cache, otherwise store it
            final LazyOptional<IEnergyStorage> target = getOutputFromCache(direction);
            if (target == null) continue;

            // store the maximum amount of energy each possible output can receive
            target.ifPresent(cap -> {
                final int accepted = cap.receiveEnergy(energy, true);
                if (accepted > 0) outputs.put(cap, accepted);
            });
        }
        return outputs;
    }

    /**
     * Initiates a block update to sync data between server and client.
     *
     * @param updateNeighbors if true, it will also update the neighbor blocks and rerender
     */
    public void update(final boolean updateNeighbors) {
        if (level == null || level.isClientSide) return;
        if (updateNeighbors) level.setBlock(
            worldPosition,
            flipBlockState(),
            BlockFlags.NOTIFY_NEIGHBORS | BlockFlags.RERENDER_MAIN_THREAD
        );
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), BlockFlags.BLOCK_UPDATE);
    }

    /**
     * Tries to get the input capability from cache, otherwise it will try to get it.
     * This features a workaround for the mod Pipez since it doesn't expose a Tile Entity
     * on the input pipe and thus a capability provider can't be received.
     *
     * @return True if a valid input was found, false otherwise
     */
    private boolean getInputFromCache(final Direction direction) {
        assert level != null && !level.isClientSide;

        LazyOptional<IEnergyStorage> target = inputCache;
        if (target == null) {
            final ICapabilityProvider provider = level.getBlockEntity(worldPosition.relative(direction));
            if (provider instanceof MeterEntity) return false;
            if (provider == null) {
                final BlockState state = level.getBlockState(worldPosition.relative(direction));
                return (
                    !state.isAir() &&
                    state.getBlock().getRegistryName() != null &&
                    state.getBlock().getRegistryName().getNamespace().equals(PIPEZ_ID)
                );
            } else {
                target = provider.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite());
                inputCache = target;
                target.addListener(self -> inputCache = null);
            }
        }

        return true;
    }

    @Nullable
    private LazyOptional<IEnergyStorage> getOutputFromCache(final Direction direction) {
        assert level != null && !level.isClientSide;

        LazyOptional<IEnergyStorage> target = outputCache.get(direction);
        if (target == null) {
            final ICapabilityProvider provider = level.getBlockEntity(worldPosition.relative(direction));
            if (provider == null || provider instanceof MeterEntity) return null;
            target = provider.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite());
            outputCache.put(direction, target);
            target.addListener(self -> outputCache.put(direction, null));
        }
        return target;
    }

    @Override
    public void invalidateCaps() {
        for (final LazyOptional<SidedEnergyStorage> cap : energyStorage) {
            cap.invalidate();
        }
        super.invalidateCaps();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction direction) {
        if (
            !remove &&
            cap == CapabilityEnergy.ENERGY &&
            direction != null &&
            sideConfig.get(direction) != IO_SETTING.OFF
        ) {
            return energyStorage.get(direction.ordinal()).cast();
        }
        return super.getCapability(cap, direction);
    }

    @Override
    public Component getDisplayName() {
        return TextUtils.translate(TRANSLATE_TYPE.CONTAINER, METER_ID);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int containerID, final Inventory inventory, final Player player) {
        return new MeterContainer(containerID, this);
    }

    /**
     * Updates the connection to the specified value.
     * If it was different from the previous value, it will trigger a block update.
     *
     * @param newStatus the new setting to set
     */
    private void updateConnection(final STATUS newStatus) {
        final STATUS oldStatus = status;
        status = newStatus;
        averageRate = 0;
        averageCount = 0;
        if (oldStatus != newStatus) {
            if (newStatus != STATUS.TRANSFERRING) transferRate = 0;
            update(false);
        }
    }

    /**
     * Checks if the output cache has at least one output which is still valid.
     *
     * @return true if there is at least one valid output, false otherwise
     */
    private boolean hasValidOutput() {
        for (final LazyOptional<IEnergyStorage> cap : outputCache.values()) {
            if (cap != null) return true;
        }
        return false;
    }

    /**
     * Calculates the flow rate depending on the energy received within {@value REFRESH_RATE} ticks.
     * Updates the connection status accordingly.
     */
    private void calculateFlow() {
        if (averageCount != 0) {
            transferRate = (float) averageRate / averageCount;
            update(false);
        }

        if (transferRate > 0) {
            updateConnection(STATUS.TRANSFERRING);
        } else {
            updateConnection(STATUS.CONNECTED);
        }

        lastAverageRate = averageRate;
    }

    void tick() {
        if (level == null || level.isClientSide || level.getGameTime() % REFRESH_RATE != 0) return;

        // initial setup
        if (!setupDone) {
            for (final Direction direction : Direction.values()) {
                if (sideConfig.get(direction) != IO_SETTING.OFF) updateCache(direction);
            }
            setupDone = true;
        }

        // if not properly connected or configured, set to disconnected
        if (
            (mode == MODE.CONSUMER && !hasValidInput) ||
            (mode == MODE.TRANSFER && (!hasValidInput || !sideConfig.hasOutput() || !hasValidOutput()))
        ) {
            updateConnection(STATUS.DISCONNECTED);
            return;
        }

        // if the average rate didn't change, set to connected
        if (averageRate == lastAverageRate) {
            updateConnection(STATUS.CONNECTED);
            return;
        }

        calculateFlow();
    }
}