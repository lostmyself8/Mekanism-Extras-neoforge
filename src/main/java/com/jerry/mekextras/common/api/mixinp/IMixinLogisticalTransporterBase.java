package com.jerry.mekextras.common.api.mixinp;

import mekanism.common.content.transporter.TransporterStack;
import net.minecraft.core.BlockPos;

public interface IMixinLogisticalTransporterBase {
    void mekanismExtras$getEntering(TransporterStack stack, int progress);

    boolean mekanismExtras$getRecalculate(int stackId, TransporterStack stack, BlockPos from);
}
