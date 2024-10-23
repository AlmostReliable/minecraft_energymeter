package com.github.almostreliable.energymeter.core;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;

import com.github.almostreliable.energymeter.meter.MeterBlock;
import com.github.almostreliable.energymeter.meter.MeterBlockEntity;
import com.github.almostreliable.energymeter.meter.MeterMenu;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;

import static com.github.almostreliable.energymeter.core.Constants.METER_ID;

public final class Setup {

    private Setup() {}

    public static final class Entities {

        private static final DeferredRegister<BlockEntityType<?>> REGISTRY
            = createRegistry(ForgeRegistries.BLOCK_ENTITY_TYPES);

        private Entities() {}

        @SuppressWarnings("SameParameterValue")
        private static <E extends MeterBlockEntity, B extends MeterBlock> RegistryObject<BlockEntityType<E>> register(
            String id, RegistryObject<B> block, BlockEntitySupplier<E> entity
        ) {
            // noinspection ConstantConditions
            return REGISTRY.register(id, () -> Builder.of(entity, block.get()).build(null));
        }

        public static final RegistryObject<BlockEntityType<MeterBlockEntity>> METER = register(
            METER_ID,
            Blocks.METER,
            MeterBlockEntity::new
        );
    }

    public static final class Containers {

        private static final DeferredRegister<MenuType<?>> REGISTRY = createRegistry(ForgeRegistries.MENU_TYPES);

        private Containers() {}

        @SuppressWarnings("SameParameterValue")
        private static <C extends MeterMenu> RegistryObject<MenuType<C>> register(
            String id, BiFunction<? super MeterBlockEntity, ? super Integer, ? extends C> constructor
        ) {
            return REGISTRY.register(id, () -> IForgeMenuType.create((containerID, inventory, data) -> {
                var entity = (MeterBlockEntity) inventory.player.level().getBlockEntity(data.readBlockPos());
                return constructor.apply(entity, containerID);
            }));
        }

        public static final RegistryObject<MenuType<MeterMenu>> METER = register(METER_ID, MeterMenu::new);
    }
}
