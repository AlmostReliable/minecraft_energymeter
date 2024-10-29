package com.github.almostreliable.energymeter.core;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.almostreliable.energymeter.ModConstants;

import com.github.almostreliable.energymeter.meter.MeterBlock;
import com.github.almostreliable.energymeter.meter.MeterBlockEntity;
import com.github.almostreliable.energymeter.meter.MeterMenu;
import com.github.almostreliable.energymeter.util.Utils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        ModConstants.MOD_ID
    );
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModConstants.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModConstants.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
        Registries.BLOCK_ENTITY_TYPE,
        ModConstants.MOD_ID
    );
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
        Registries.MENU,
        ModConstants.MOD_ID
    );

    public static final DeferredBlock<MeterBlock> METER_BLOCK = Util.make(() -> {
        var block = BLOCKS.registerBlock(
            Constants.METER_ID,
            MeterBlock::new,
            BlockBehaviour.Properties.of().strength(2f).mapColor(MapColor.METAL).sound(SoundType.METAL)
        );
        ITEMS.registerSimpleBlockItem(block);
        return block;
    });

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeterBlockEntity>> METER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
        Constants.METER_ID, () -> BlockEntityType.Builder.of(MeterBlockEntity::new, METER_BLOCK.get()).build(null)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<MeterMenu>> METER_MENU = MENUS.register("meter", () ->
        IMenuTypeExtension.create((wid, inventory, data) -> {
            var entity = (MeterBlockEntity) inventory.player.level().getBlockEntity(data.readBlockPos());
            return new MeterMenu(entity, wid);
        })
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register(
        "tab", () -> CreativeModeTab.builder()
            .title(Utils.translate("itemGroup", "tab"))
            .icon(METER_BLOCK::toStack)
            .noScrollBar()
            .displayItems((features, output) -> output.accept(METER_BLOCK))
            .build()
    );

    private Registration() {}

    public static void init(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }
}
