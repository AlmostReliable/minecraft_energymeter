package com.github.almostreliable.energymeter.component;

import com.github.almostreliable.energymeter.meter.MeterBlock;
import com.github.almostreliable.energymeter.util.TypeEnums.BLOCK_SIDE;
import com.github.almostreliable.energymeter.util.TypeEnums.IO_SETTING;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SideConfiguration implements INBTSerializable<CompoundTag> {

    private static final int MAX_OUTPUTS = 4;
    private final EnumMap<Direction, IO_SETTING> config = new EnumMap<>(Direction.class);
    private final Direction facing;
    private final Direction bottom;

    public SideConfiguration(BlockState state) {
        facing = state.getValue(MeterBlock.FACING);
        bottom = state.getValue(MeterBlock.BOTTOM);
        for (var direction : Direction.values()) {
            config.put(direction, IO_SETTING.OFF);
        }
    }

    /**
     * Gets the IO setting for the given direction.
     *
     * @param direction the direction to resolve the IO setting from
     * @return the IO setting
     */
    public IO_SETTING get(Direction direction) {
        return config.get(direction);
    }

    /**
     * Gets the IO setting for the given block side.
     *
     * @param side the block side to resolve the IO setting from
     * @return the IO setting
     */
    public IO_SETTING get(BLOCK_SIDE side) {
        return config.get(getDirectionFromSide(side));
    }

    /**
     * Gets the direction from the given block side depending on the facing of the block.
     *
     * @param side the block side to resolve the direction from
     * @return the direction
     */
    public Direction getDirectionFromSide(BLOCK_SIDE side) {
        return facing == bottom ? horizontalConversion(side) : verticalConversion(side);
    }

    /**
     * Sets the given block side to the passed IO setting.
     *
     * @param side    the side on which the setting should be changed
     * @param setting the setting which should be set
     */
    public void set(BLOCK_SIDE side, IO_SETTING setting) {
        config.put(getDirectionFromSide(side), setting);
    }

    /**
     * Checks if the side configuration has an input set somewhere.
     *
     * @return true if there is an input, false otherwise
     */
    public boolean hasInput() {
        return config.containsValue(IO_SETTING.IN);
    }

    /**
     * Checks if the side configuration has an output set somewhere.
     *
     * @return true if there is an output, false otherwise
     */
    public boolean hasOutput() {
        return config.containsValue(IO_SETTING.OUT);
    }

    /**
     * Checks if the side configuration already has the maximum amount of outputs.
     *
     * @return true if there are max outputs, false otherwise
     */
    public boolean hasMaxOutputs() {
        return config.values().stream().filter(setting -> setting == IO_SETTING.OUT).count() == MAX_OUTPUTS;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var nbt = new CompoundTag();
        for (var direction : Direction.values()) {
            nbt.putInt(direction.toString(), config.get(direction).ordinal());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        for (var direction : Direction.values()) {
            config.put(direction, IO_SETTING.values()[nbt.getInt(direction.toString())]);
        }
    }

    /**
     * Converts the current side configuration as string map and returns it.
     *
     * @return the side configuration as string map
     */
    public Map<String, String> asStringMap() {
        Map<String, String> stringMap = new HashMap<>();
        for (var entry : config.entrySet()) {
            stringMap.put(entry.getKey().getName(), entry.getValue().name());
        }
        return stringMap;
    }

    /**
     * Converts the given block side to a direction if the facing direction
     * is any of the 4 compass directions.
     *
     * @param side the block side to convert
     * @return the direction
     */
    private Direction horizontalConversion(BLOCK_SIDE side) {
        return switch (side) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case BACK -> facing.getOpposite();
            default -> facing;
        };
    }

    /**
     * Converts the given block side to a direction if the facing direction
     * is up or down.
     *
     * @param side the block side to convert
     * @return the direction
     */
    private Direction verticalConversion(BLOCK_SIDE side) {
        return switch (side) {
            case TOP -> bottom.getOpposite();
            case BOTTOM -> bottom;
            case LEFT -> facing == Direction.UP ? bottom.getClockWise() : bottom.getCounterClockWise();
            case RIGHT -> facing == Direction.UP ? bottom.getCounterClockWise() : bottom.getClockWise();
            case BACK -> facing.getOpposite();
            default -> facing;
        };
    }
}
