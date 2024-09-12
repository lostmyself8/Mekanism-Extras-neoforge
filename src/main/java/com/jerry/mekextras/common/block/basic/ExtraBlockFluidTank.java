package com.jerry.mekextras.common.block.basic;

import com.jerry.mekextras.common.tile.ExtraTileEntityFluidTank;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.resource.BlockResourceInfo;
import mekanism.common.util.FluidUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class ExtraBlockFluidTank extends BlockTile.BlockTileModel<ExtraTileEntityFluidTank, Machine<ExtraTileEntityFluidTank>> {
    public ExtraBlockFluidTank(Machine<ExtraTileEntityFluidTank> type) {
        super(type, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor()));
    }

    @Override
    public int getLightEmission(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        int ambientLight = super.getLightEmission(state, world, pos);
        if (ambientLight == 15) {
            //If we are already at the max light value don't bother looking up the tile to see if it has a fluid that gives off light
            return ambientLight;
        }
        ExtraTileEntityFluidTank tile = WorldUtils.getTileEntity(ExtraTileEntityFluidTank.class, world, pos);
        if (tile != null) {
            FluidStack fluid = tile.fluidTank.getFluid();
            if (!fluid.isEmpty()) {
                ambientLight = Math.max(ambientLight, fluid.getFluidType().getLightLevel(fluid));
            }
        }
        return ambientLight;
    }

    @NotNull
    @Override
    @Deprecated
    public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
                                 @NotNull BlockHitResult hit) {
        ExtraTileEntityFluidTank tile = WorldUtils.getTileEntity(ExtraTileEntityFluidTank.class, world, pos, true);
        if (tile == null) {
            return InteractionResult.PASS;
        } else if (world.isClientSide) {
            return genericClientActivated(player, hand, tile);
        }
        InteractionResult wrenchResult = tile.tryWrench(state, player, hand, hit).getInteractionResult();
        if (wrenchResult != InteractionResult.PASS) {
            return wrenchResult;
        }
        //Handle filling fluid tank
        if (!player.isShiftKeyDown()) {
            if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, world, pos, tile)) {
                return InteractionResult.FAIL;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && FluidUtils.handleTankInteraction(player, hand, stack, tile.fluidTank)) {
                player.getInventory().setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return tile.openGui(player);
    }
}
