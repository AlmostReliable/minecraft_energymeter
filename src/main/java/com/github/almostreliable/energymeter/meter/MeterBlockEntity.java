package com.github.almostreliable.energymeter.meter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.github.almostreliable.energymeter.compat.CapabilityAdapterFactory;
import com.github.almostreliable.energymeter.compat.ICapabilityAdapter;
import com.github.almostreliable.energymeter.compat.IMeterEntityObserver;
import com.github.almostreliable.energymeter.compat.cct.MeterPeripheral;
import com.github.almostreliable.energymeter.component.SideConfiguration;
import com.github.almostreliable.energymeter.component.SidedEnergyStorage;
import com.github.almostreliable.energymeter.core.Setup.Entities;
import com.github.almostreliable.energymeter.network.ClientSyncPacket;
import com.github.almostreliable.energymeter.network.PacketHandler;
import com.github.almostreliable.energymeter.network.SettingUpdatePacket;
import com.github.almostreliable.energymeter.util.TextUtils;
import com.github.almostreliable.energymeter.util.TypeEnums.ACCURACY;
import com.github.almostreliable.energymeter.util.TypeEnums.IO_SETTING;
import com.github.almostreliable.energymeter.util.TypeEnums.MODE;
import com.github.almostreliable.energymeter.util.TypeEnums.NUMBER_MODE;
import com.github.almostreliable.energymeter.util.TypeEnums.SETTING;
import com.github.almostreliable.energymeter.util.TypeEnums.STATUS;
import com.github.almostreliable.energymeter.util.TypeEnums.TRANSLATE_TYPE;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.github.almostreliable.energymeter.core.Constants.ACCURACY_ID;
import static com.github.almostreliable.energymeter.core.Constants.INTERVAL_ID;
import static com.github.almostreliable.energymeter.core.Constants.METER_ID;
import static com.github.almostreliable.energymeter.core.Constants.MODE_ID;
import static com.github.almostreliable.energymeter.core.Constants.NUMBER_MODE_ID;
import static com.github.almostreliable.energymeter.core.Constants.PIPEZ_ID;
import static com.github.almostreliable.energymeter.core.Constants.SIDE_CONFIG_ID;
import static com.github.almostreliable.energymeter.core.Constants.STATUS_ID;
import static com.github.almostreliable.energymeter.core.Constants.SYNC_FLAGS;
import static com.github.almostreliable.energymeter.core.Constants.THRESHOLD_ID;
import static com.github.almostreliable.energymeter.core.Constants.TRANSFER_RATE_ID;

public class MeterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int REFRESH_RATE = 5;
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> outputCache = new EnumMap<>(Direction.class);
    private final List<LazyOptional<SidedEnergyStorage>> energyStorage;
    private final SideConfiguration sideConfig;
    private final List<Double> energyRates = Collections.synchronizedList(new ArrayList<>());
    private final Set<IMeterEntityObserver> observers = Collections.synchronizedSet(new HashSet<>());
    @Nullable
    private final ICapabilityAdapter<MeterPeripheral> meterPeripheral;
    private boolean hasValidInput;
    private boolean setupDone;
    private LazyOptional<IEnergyStorage> inputCache;
    private double transferRate;
    private double averageRate;
    private NUMBER_MODE numberMode = NUMBER_MODE.SHORT;
    private STATUS status = STATUS.DISCONNECTED;
    private MODE mode = MODE.TRANSFER;
    private ACCURACY accuracy = ACCURACY.EXACT;
    private int interval = REFRESH_RATE;
    private int threshold = REFRESH_RATE;
    private double zeroThreshold;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public MeterBlockEntity(BlockPos pos, BlockState state) {
        super(Entities.METER.get(), pos, state);
        energyStorage = SidedEnergyStorage.create(this);
        sideConfig = new SideConfiguration(state);
        meterPeripheral = CapabilityAdapterFactory.createMeterPeripheral(this);
    }

    /**
     * Handles the equal energy transfer process.
     * <p>
     * This will try to distribute the energy equally to all possible outputs by rerouting excess
     * energy in case a limit of an output is exceeded.
     *
     * @param energy  the energy to transfer
     * @param outputs the possible outputs
     * @return the accepted amount of energy
     */
    private static int transferEnergy(int energy, Map<? extends IEnergyStorage, Integer> outputs) {
        var acceptedEnergy = 0;
        var energyToTransfer = energy;
        while (!outputs.isEmpty() && energyToTransfer >= outputs.size()) {
            var equalSplit = energyToTransfer / outputs.size();
            Collection<IEnergyStorage> outputsToRemove = new ArrayList<>();

            for (var output : outputs.entrySet()) {
                var actualSplit = equalSplit;
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
     * Convenience method used by the {@link SettingUpdatePacket} in order
     * to flip a specific setting after a button click on the client.
     *
     * @param setting the setting to update
     */
    public void updateSetting(SETTING setting) {
        switch (setting) {
            case NUMBER -> {
                numberMode = numberMode == NUMBER_MODE.SHORT ? NUMBER_MODE.LONG : NUMBER_MODE.SHORT;
                syncData(SYNC_FLAGS.NUMBER_MODE);
            }
            case MODE -> {
                mode = mode == MODE.TRANSFER ? MODE.CONSUMER : MODE.TRANSFER;
                syncData(SYNC_FLAGS.MODE);
            }
            case ACCURACY -> {
                var flags = SYNC_FLAGS.ACCURACY;
                if (accuracy == ACCURACY.EXACT) {
                    accuracy = ACCURACY.INTERVAL;
                } else {
                    accuracy = ACCURACY.EXACT;
                    interval = REFRESH_RATE;
                    flags |= SYNC_FLAGS.INTERVAL;
                }
                syncData(flags);
            }
        }
    }

    /**
     * Syncs data to clients tracking the current with a {@link ClientSyncPacket}.
     * <p>
     * Different flags from the sync flags can be passed to define what should be included
     * in the packet to avoid unnecessary data being sent.
     *
     * @param flags the flags of the data to sync
     */
    public void syncData(int flags) {
        if (level == null || level.isClientSide) return;
        var packet = new ClientSyncPacket(
            worldPosition,
            flags,
            sideConfig,
            transferRate,
            numberMode,
            status,
            mode,
            accuracy,
            interval,
            threshold
        );
        PacketHandler.CHANNEL.send(
            PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
            packet
        );

        for (var observer : observers) {
            observer.onMeterTileChanged(this, flags);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(SIDE_CONFIG_ID)) sideConfig.deserializeNBT(tag.getCompound(SIDE_CONFIG_ID));
        if (tag.contains(NUMBER_MODE_ID)) numberMode = NUMBER_MODE.values()[tag.getInt(NUMBER_MODE_ID)];
        if (tag.contains(MODE_ID)) mode = MODE.values()[tag.getInt(MODE_ID)];
        if (tag.contains(ACCURACY_ID)) accuracy = ACCURACY.values()[tag.getInt(ACCURACY_ID)];
        if (tag.contains(INTERVAL_ID)) interval = tag.getInt(INTERVAL_ID);
        if (tag.contains(THRESHOLD_ID)) threshold = tag.getInt(THRESHOLD_ID);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(SIDE_CONFIG_ID, sideConfig.serializeNBT());
        tag.putInt(NUMBER_MODE_ID, numberMode.ordinal());
        tag.putInt(MODE_ID, mode.ordinal());
        tag.putInt(ACCURACY_ID, accuracy.ordinal());
        tag.putInt(INTERVAL_ID, interval);
        tag.putInt(THRESHOLD_ID, threshold);
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag();
        tag.put(SIDE_CONFIG_ID, sideConfig.serializeNBT());
        tag.putDouble(TRANSFER_RATE_ID, transferRate);
        tag.putInt(STATUS_ID, status.ordinal());
        tag.putInt(NUMBER_MODE_ID, numberMode.ordinal());
        tag.putInt(MODE_ID, mode.ordinal());
        tag.putInt(ACCURACY_ID, accuracy.ordinal());
        tag.putInt(INTERVAL_ID, interval);
        tag.putInt(THRESHOLD_ID, threshold);
        return tag;
    }

    @Override
    public void setRemoved() {
        for (var observer : observers) {
            observer.onMeterTileRemoved(this);
        }
        super.setRemoved();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        sideConfig.deserializeNBT(tag.getCompound(SIDE_CONFIG_ID));
        transferRate = tag.getDouble(TRANSFER_RATE_ID);
        status = STATUS.values()[tag.getInt(STATUS_ID)];
        numberMode = NUMBER_MODE.values()[tag.getInt(NUMBER_MODE_ID)];
        mode = MODE.values()[tag.getInt(MODE_ID)];
        accuracy = ACCURACY.values()[tag.getInt(ACCURACY_ID)];
        interval = tag.getInt(INTERVAL_ID);
        threshold = tag.getInt(THRESHOLD_ID);
    }

    public int receiveEnergy(int energy, boolean simulate) {
        if (level == null || !setupDone) return 0;

        // void the energy if consumer mode is activated
        if (mode == MODE.CONSUMER) {
            if (!simulate) {
                averageRate += energy;
            }
            return energy;
        }

        // create a map with all possible outputs and their energy limit
        var outputs = getPossibleOutputs(energy);
        if (outputs.isEmpty()) return 0;

        // get the maximum energy which could be accepted by all outputs
        var maximumAccepted = outputs.values().stream().mapToInt(maxEnergy -> maxEnergy).sum();

        // if simulated, just check if the energy fits somewhere
        if (simulate) return Math.min(maximumAccepted, energy);

        // actual energy transfer
        int acceptedEnergy;
        if (maximumAccepted <= energy) {
            // if maximum accepted energy is less or equal the energy to transfer, fill all outputs with their maximum
            outputs.forEach((cap, integer) -> cap.receiveEnergy(integer, false));
            acceptedEnergy = maximumAccepted;
        } else {
            // otherwise, push the energy to all possible outputs equally
            acceptedEnergy = transferEnergy(energy, outputs);
        }

        // adjust data for calculation in tick method
        averageRate += acceptedEnergy;

        return acceptedEnergy;
    }

    /**
     * Updates the neighbor blocks of the entity.
     * Can be useful to connect cables.
     */
    public void updateNeighbors() {
        if (level == null || level.isClientSide) return;
        level.setBlock(worldPosition, flipBlockState(), Block.UPDATE_NEIGHBORS | Block.UPDATE_IMMEDIATE);
    }

    @Override
    public void invalidateCaps() {
        for (var cap : energyStorage) {
            cap.invalidate();
        }

        if (meterPeripheral != null) {
            meterPeripheral.getLazyAdapter().invalidate();
        }

        super.invalidateCaps();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction direction) {
        if (!remove) {
            if (cap.equals(ForgeCapabilities.ENERGY) && direction != null &&
                sideConfig.get(direction) != IO_SETTING.OFF) {
                return energyStorage.get(direction.ordinal()).cast();
            }
            if (meterPeripheral != null && meterPeripheral.isCapability(cap)) {
                return meterPeripheral.getLazyAdapter().cast();
            }
        }
        return super.getCapability(cap, direction);
    }

    /**
     * Adds a new observer to the list of observers.
     * <p>
     * Observers are CCT components that are notified about specific events.
     *
     * @param observer the observer to add
     */
    public void subscribe(IMeterEntityObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes an observer from the list of observers.
     * <p>
     * Observers are CCT components that are notified about specific events.
     *
     * @param observer the observer to remove
     */
    public void unsubscribe(IMeterEntityObserver observer) {
        observers.remove(observer);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerID, Inventory inventory, Player player) {
        return new MeterMenu(this, containerID);
    }

    /**
     * Updates the cached input and output values depending on the direction.
     * This ensures that the current status is always up-to-date.
     *
     * @param direction the direction to update the cache for
     */
    public void updateCache(Direction direction) {
        if (level == null || level.isClientSide) return;

        var setting = sideConfig.get(direction);
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

    /**
     * Called each tick server-side.
     */
    void tick() {
        if (level == null || level.isClientSide) return;
        if ((thresholdReached() || intervalReached()) && !energyRates.isEmpty()) calculateTransferRate();
        if (level.getGameTime() % REFRESH_RATE != 0) return;

        // initial setup
        if (!setupDone) {
            for (var direction : Direction.values()) {
                if (sideConfig.get(direction) != IO_SETTING.OFF) updateCache(direction);
            }
            setupDone = true;
        }

        // if not properly connected or configured, set to disconnected
        if ((mode == MODE.CONSUMER && !hasValidInput) ||
            (mode == MODE.TRANSFER && (!hasValidInput || !sideConfig.hasOutput() || !hasValidOutput()))) {
            updateStatus(STATUS.DISCONNECTED);
            return;
        }

        energyRates.add(averageRate);
        averageRate = 0;
        calculateThreshold();

        if (transferRate > 0) {
            updateStatus(STATUS.TRANSFERRING);
        } else {
            updateStatus(STATUS.CONNECTED);
        }
    }

    /**
     * Checks each output direction whether there is a valid energy capability.
     * It will simulate an energy transfer to this capability to make sure it can
     * accept energy and to retrieve the energy limit.
     *
     * @return a map of all possible outputs with their corresponding energy limit
     */
    private Map<IEnergyStorage, Integer> getPossibleOutputs(int energy) {
        Map<IEnergyStorage, Integer> outputs = new HashMap<>();
        for (var direction : Direction.values()) {
            // only consider sides where output mode is enabled
            if (sideConfig.get(direction) != IO_SETTING.OUT) continue;

            // try to get the energy capability from the cache, otherwise store it
            var target = getOutputFromCache(direction);
            if (target == null) continue;

            // store the maximum amount of energy each possible output can receive
            target.ifPresent(cap -> {
                var accepted = cap.receiveEnergy(energy, true);
                if (accepted > 0) outputs.put(cap, accepted);
            });
        }
        return outputs;
    }

    @Nullable
    private LazyOptional<IEnergyStorage> getOutputFromCache(Direction direction) {
        assert level != null && !level.isClientSide;

        var target = outputCache.get(direction);
        if (target == null) {
            ICapabilityProvider provider = level.getBlockEntity(worldPosition.relative(direction));
            if (provider == null || provider instanceof MeterBlockEntity) return null;
            target = provider.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
            outputCache.put(direction, target);
            target.addListener(self -> outputCache.put(direction, null));
        }
        return target;
    }

    /**
     * Flips the IO block state value and returns the new block state.
     * This is a utility method to make neighbor updates possible.
     *
     * @return the block state with the flipped IO value
     */
    private BlockState flipBlockState() {
        var state = getBlockState();
        return state.setValue(MeterBlock.IO, !state.getValue(MeterBlock.IO));
    }

    private boolean thresholdReached() {
        return energyRates.size() * REFRESH_RATE >= threshold && zeroThreshold == 0;
    }

    private boolean intervalReached() {
        assert level != null;
        return level.getGameTime() % interval == 0;
    }

    /**
     * Calculates the flow rate depending on the energy received within the specified interval.
     * Updates the status accordingly.
     */
    private void calculateTransferRate() {
        assert level != null && !level.isClientSide;

        var oldTransferRate = transferRate;
        var average = energyRates.stream().mapToDouble(Double::valueOf).average().orElse(0);
        transferRate = average / REFRESH_RATE;
        if (oldTransferRate != transferRate) syncData(SYNC_FLAGS.TRANSFER_RATE);

        energyRates.clear();
        if (accuracy == ACCURACY.INTERVAL) energyRates.add(average);
    }

    /**
     * Checks if the output cache has at least one output which is still valid.
     *
     * @return true if there is at least one valid output, false otherwise
     */
    private boolean hasValidOutput() {
        return outputCache.values().stream().anyMatch(Objects::nonNull);
    }

    /**
     * Updates the status to the specified value.
     * If it was different from the previous value, it will trigger a client sync.
     *
     * @param newStatus the new setting to set
     */
    private void updateStatus(STATUS newStatus) {
        var oldStatus = status;
        status = newStatus;
        averageRate = 0;
        if (oldStatus != newStatus) {
            var flags = SYNC_FLAGS.STATUS;
            if (newStatus != STATUS.TRANSFERRING) {
                energyRates.clear();
                transferRate = 0;
                flags |= SYNC_FLAGS.TRANSFER_RATE;
            }
            syncData(flags);
        }
    }

    private void calculateThreshold() {
        var skips = Math.max(0, energyRates.size() * REFRESH_RATE - threshold);
        zeroThreshold = energyRates.stream().skip(skips).reduce(0.0, Double::sum);
    }

    /**
     * Tries to get the input capability from cache, otherwise it will try to get it.
     * This features a workaround for the mod Pipez since it doesn't expose a Tile Entity
     * on the input pipe and thus a capability provider can't be received.
     *
     * @return True if a valid input was found, false otherwise
     */
    private boolean getInputFromCache(Direction direction) {
        assert level != null && !level.isClientSide;

        var target = inputCache;
        if (target == null) {
            ICapabilityProvider provider = level.getBlockEntity(worldPosition.relative(direction));
            if (provider instanceof MeterBlockEntity) return false;
            if (provider == null) {
                var state = level.getBlockState(worldPosition.relative(direction));
                // noinspection deprecation
                return !state.isAir() &&
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(PIPEZ_ID);
            }
            target = provider.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
            inputCache = target;
            target.addListener(self -> inputCache = null);
        }

        return true;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public double getTransferRate() {
        return Math.round(transferRate * 1_000.0) / 1_000.0;
    }

    public void setTransferRate(double transferRate) {
        this.transferRate = transferRate;
    }

    public STATUS getStatus() {
        if (status == STATUS.TRANSFERRING) {
            return mode == MODE.CONSUMER ? STATUS.CONSUMING : STATUS.TRANSFERRING;
        }
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public NUMBER_MODE getNumberMode() {
        return numberMode;
    }

    public void setNumberMode(NUMBER_MODE numberMode) {
        this.numberMode = numberMode;
    }

    public ACCURACY getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(ACCURACY accuracy) {
        this.accuracy = accuracy;
    }

    public SideConfiguration getSideConfig() {
        return sideConfig;
    }

    public MODE getMode() {
        return mode;
    }

    public void setMode(MODE mode) {
        this.mode = mode;
    }

    @Override
    public Component getDisplayName() {
        return TextUtils.translate(TRANSLATE_TYPE.CONTAINER, METER_ID);
    }
}
