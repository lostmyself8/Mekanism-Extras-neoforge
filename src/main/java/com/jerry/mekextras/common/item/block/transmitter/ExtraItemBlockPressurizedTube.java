package com.jerry.mekextras.common.item.block.transmitter;

import com.jerry.mekextras.common.tier.transmitter.TTier;
import com.jerry.mekextras.common.tile.transmitter.ExtraTileEntityPressurizedTube;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.transmitter.BlockSmallTransmitter;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.tier.TubeTier;
import mekanism.common.util.text.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ExtraItemBlockPressurizedTube extends ItemBlockTooltip<BlockSmallTransmitter<ExtraTileEntityPressurizedTube>> {
    public ExtraItemBlockPressurizedTube(BlockSmallTransmitter<ExtraTileEntityPressurizedTube> block) {
        super(block);
    }

    @NotNull
    @Override
    public TubeTier getTier() {
        return Objects.requireNonNull(Attribute.getTier(getBlock(), TubeTier.class));
    }

    @Override
    protected void addDetails(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.addDetails(stack, world, tooltip, flag);
        tooltip.add(MekanismLang.CAPABLE_OF_TRANSFERRING.translateColored(EnumColor.DARK_GRAY));
        tooltip.add(MekanismLang.GASES.translateColored(EnumColor.PURPLE, MekanismLang.MEKANISM));
        tooltip.add(MekanismLang.INFUSE_TYPES.translateColored(EnumColor.PURPLE, MekanismLang.MEKANISM));
        tooltip.add(MekanismLang.PIGMENTS.translateColored(EnumColor.PURPLE, MekanismLang.MEKANISM));
        tooltip.add(MekanismLang.SLURRIES.translateColored(EnumColor.PURPLE, MekanismLang.MEKANISM));
    }

    @Override
    protected void addStats(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.addStats(stack, world, tooltip, flag);
        TubeTier tier = getTier();
        tooltip.add(MekanismLang.CAPACITY_MB_PER_TICK.translateColored(EnumColor.INDIGO, EnumColor.GRAY, TextUtils.format(TTier.getTubeCapacity(tier))));
        tooltip.add(MekanismLang.PUMP_RATE_MB.translateColored(EnumColor.INDIGO, EnumColor.GRAY, TextUtils.format(TTier.getTubePullAmount(tier))));
    }
}
