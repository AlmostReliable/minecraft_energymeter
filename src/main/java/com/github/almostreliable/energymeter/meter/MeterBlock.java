package com.github.almostreliable.energymeter.meter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import static com.github.almostreliable.energymeter.core.Constants.PIPEZ_ID;

public class MeterBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final DirectionProperty BOTTOM = DirectionProperty.create("bottom", Plane.HORIZONTAL);

    public MeterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing = context.getNearestLookingDirection().getOpposite();
        var bottom = context.getHorizontalDirection();
        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(BOTTOM, facing == Direction.DOWN ? bottom : bottom.getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(BOTTOM);
    }

    @Override
    public void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block block, BlockPos neighbor, boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, block, neighbor, isMoving);

        // resolve tile entity from block position
        if (!state.hasBlockEntity()) return;
        if (level.getBlockEntity(pos) instanceof MeterBlockEntity entity) {
            // ensure valid neighbor
            var neighborState = level.getBlockState(neighbor);
            var registryName = BuiltInRegistries.BLOCK.getKey(neighborState.getBlock());
            if (!neighborState.isAir() && !neighborState.hasBlockEntity() &&
                !registryName.getNamespace().equals(PIPEZ_ID)) {
                return;
            }

            // resolve direction from neighbor block position
            var vector = neighbor.subtract(pos);
            var direction = Direction.fromDelta(vector.getX(), vector.getY(), vector.getZ());
            if (direction == null) return;

            // update the cache from the direction
            entity.updateCache(direction);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || player.isShiftKeyDown()) return InteractionResult.SUCCESS;

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider entity && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, entity, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MeterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <E extends BlockEntity> BlockEntityTicker<E> getTicker(
        Level level, BlockState state, BlockEntityType<E> entity
    ) {
        if (level.isClientSide) {
            return null;
        }
        return (pLevel, pPos, pState, pEntity) -> {
            if (pEntity instanceof MeterBlockEntity meter) {
                meter.tick();
            }
        };
    }
}
