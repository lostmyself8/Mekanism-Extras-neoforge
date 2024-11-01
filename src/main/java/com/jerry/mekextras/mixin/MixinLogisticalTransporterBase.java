package com.jerry.mekextras.mixin;

import com.jerry.mekextras.api.mixin.IMixinLogisticalTransporterBase;
import mekanism.common.content.network.InventoryNetwork;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LogisticalTransporterBase.class)
public abstract class MixinLogisticalTransporterBase extends Transmitter<IItemHandler, InventoryNetwork, LogisticalTransporterBase> implements IMixinLogisticalTransporterBase {
    public MixinLogisticalTransporterBase(TileEntityTransmitter transmitterTile, TransmissionType... transmissionTypes) {
        super(transmitterTile, transmissionTypes);
    }

    @Shadow
    abstract void entityEntering(TransporterStack stack, int progress);

    @Shadow
    abstract boolean recalculate(int stackId, TransporterStack stack, BlockPos from);

    @Override
    public void mekanismExtras$getEntity(TransporterStack stack, int progress) {
        entityEntering(stack, progress);
    }

    @Override
    public boolean mekanismExtras$getRecalculate(int stackId, TransporterStack stack, BlockPos from) {
        return recalculate(stackId, stack, from);
    }
}
