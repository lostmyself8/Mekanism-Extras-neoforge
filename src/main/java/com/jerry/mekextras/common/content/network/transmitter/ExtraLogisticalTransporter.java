package com.jerry.mekextras.common.content.network.transmitter;

import com.jerry.mekextras.common.tile.transmitter.ExtraTileEntityTransmitter;
import mekanism.api.NBTConstants;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.text.EnumColor;
import mekanism.api.tier.ITier;
import mekanism.common.MekanismLang;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.content.transporter.PathfinderCache;
import mekanism.common.tier.TransporterTier;
import mekanism.common.upgrade.transmitter.LogisticalTransporterUpgradeData;
import mekanism.common.upgrade.transmitter.TransmitterUpgradeData;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.TransporterUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtraLogisticalTransporter extends ExtraLogisticalTransporterBase implements IExtraUpgradeableTransmitter<LogisticalTransporterUpgradeData> {
    @Nullable
    private EnumColor color;
    public ExtraLogisticalTransporter(IBlockProvider blockProvider, ExtraTileEntityTransmitter tile) {
        super(tile, Attribute.getTier(blockProvider, TransporterTier.class));
    }

    @Nullable
    @Override
    public EnumColor getColor() {
        return color;
    }

    public void setColor(@Nullable EnumColor c) {
        color = c;
    }

    @Override
    public InteractionResult onConfigure(Player player, Direction side) {
        setColor(TransporterUtils.increment(getColor()));
        PathfinderCache.onChanged(getTransmitterNetwork());
        getTransmitterTile().sendUpdatePacket();
        EnumColor color = getColor();
        player.displayClientMessage(MekanismLang.TOGGLE_COLOR.translateColored(EnumColor.GRAY, color == null ? MekanismLang.NONE.translateColored(EnumColor.WHITE) : color.getColoredName()), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onRightClick(Player player, Direction side) {
        EnumColor color = getColor();
        player.displayClientMessage(MekanismLang.CURRENT_COLOR.translateColored(EnumColor.GRAY, color == null ? MekanismLang.NONE.translateColored(EnumColor.WHITE) : color.getColoredName()), true);
        return super.onRightClick(player, side);
    }

    @Nullable
    @Override
    public LogisticalTransporterUpgradeData getUpgradeData() {
        return new LogisticalTransporterUpgradeData(redstoneReactive, getConnectionTypesRaw(), getColor(), transit, needsSync, nextId, delay, delayCount);
    }

    @Override
    public boolean dataTypeMatches(@NotNull TransmitterUpgradeData data) {
        return data instanceof LogisticalTransporterUpgradeData;
    }

    @Override
    public void parseUpgradeData(@NotNull LogisticalTransporterUpgradeData data) {
        redstoneReactive = data.redstoneReactive;
        setConnectionTypesRaw(data.connectionTypes);
        setColor(data.color);
        transit.putAll(data.transit);
        needsSync.putAll(data.needsSync);
        nextId = data.nextId;
        delay = data.delay;
        delayCount = data.delayCount;
    }

    @Override
    protected void readFromNBT(CompoundTag nbtTags) {
        super.readFromNBT(nbtTags);
        setColor(NBTUtils.getEnum(nbtTags, NBTConstants.COLOR, TransporterUtils::readColor));
    }

    @Override
    public void writeToNBT(CompoundTag nbtTags) {
        super.writeToNBT(nbtTags);
        if (getColor() != null) {
            NBTUtils.writeEnum(nbtTags, NBTConstants.COLOR, getColor());
        }
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(CompoundTag updateTag) {
        updateTag = super.getReducedUpdateTag(updateTag);
        if (getColor() != null) {
            NBTUtils.writeEnum(updateTag, NBTConstants.COLOR, getColor());
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        super.handleUpdateTag(tag);
        setColor(NBTUtils.getEnum(tag, NBTConstants.COLOR, TransporterUtils::readColor));
    }

    @Override
    public ITier getTier() {
        return tier;
    }
}
