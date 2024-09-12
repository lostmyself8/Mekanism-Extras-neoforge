package com.jerry.mekextras.common.block;

import com.jerry.mekextras.common.tile.TileEntityLargeCapRadioactiveWasteBarrel;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.util.WorldUtils;
import mekanism.common.util.text.TextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class BlockLargeCapRadioactiveWasteBarrel extends BlockTile.BlockTileModel<TileEntityLargeCapRadioactiveWasteBarrel, BlockTypeTile<TileEntityLargeCapRadioactiveWasteBarrel>> {
    public BlockLargeCapRadioactiveWasteBarrel(BlockTypeTile<TileEntityLargeCapRadioactiveWasteBarrel> type) {
        super(type, properties -> properties.mapColor(MapColor.COLOR_BLACK));
    }

    @NotNull
    @Override
    @Deprecated
    public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
                                 @NotNull BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        TileEntityLargeCapRadioactiveWasteBarrel tile = WorldUtils.getTileEntity(TileEntityLargeCapRadioactiveWasteBarrel.class, world, pos);
        if (tile == null) {
            return InteractionResult.PASS;
        } else if (!world.isClientSide()) {
            GasStack stored = tile.getGas();
            Component text;
            if (stored.isEmpty()) {
                text = MekanismLang.NO_GAS.translateColored(EnumColor.GRAY);
            } else {
                text = MekanismLang.STORED_MB_PERCENTAGE.translateColored(EnumColor.ORANGE, EnumColor.ORANGE, stored, EnumColor.GRAY,
                        TextUtils.format(stored.getAmount()), TextUtils.getPercent(tile.getGasScale()));
            }
            player.sendSystemMessage(text);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
