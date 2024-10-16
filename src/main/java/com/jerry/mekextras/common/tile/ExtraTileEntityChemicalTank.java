package com.jerry.mekextras.common.tile;

import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.capabilities.chemical.ExtraChemicalTankChemicalTank;
import com.jerry.mekextras.common.registry.ExtraDataComponents;
import com.jerry.mekextras.common.tier.CTTier;
import com.jerry.mekextras.common.upgrade.ExtraChemicalTankUpgradeData;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import mekanism.api.*;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.math.MathUtils;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.ILangEntry;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.chemical.ChemicalTankChemicalTank;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.SyntheticComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IHasGasMode;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.IntFunction;

public class ExtraTileEntityChemicalTank extends TileEntityConfigurableMachine implements IHasGasMode {

    @SyntheticComputerMethod(getter = "getDumpingMode", getterDescription = "Get the current Dumping configuration")
    public GasMode dumping = GasMode.IDLE;

    private IChemicalTank chemicalTank;
    private CTTier tier;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDrainItem", docPlaceholder = "drain slot")
    ChemicalInventorySlot drainSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getFillItem", docPlaceholder = "fill slot")
    ChemicalInventorySlot fillSlot;

    public ExtraTileEntityChemicalTank(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        configComponent.setupIOConfig(TransmissionType.ITEM, drainSlot, fillSlot, RelativeSide.FRONT, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, getChemicalTank(), RelativeSide.FRONT);
        ejectorComponent = new TileComponentEjector(this, () -> tier.getOutput());
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL)
                .setCanEject(type -> canFunction() && (dumping != ExtraTileEntityChemicalTank.GasMode.DUMPING));
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        tier = ExtraAttribute.getAdvanceTier(getBlockType(), CTTier.class);
    }

    @Override
    public @Nullable IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(chemicalTank = ExtraChemicalTankChemicalTank.create(tier, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(drainSlot = ChemicalInventorySlot.drain(chemicalTank, listener, 16, 16));
        builder.addSlot(fillSlot = ChemicalInventorySlot.fill(chemicalTank, listener, 16, 48));
        drainSlot.setSlotType(ContainerSlotType.OUTPUT);
        drainSlot.setSlotOverlay(SlotOverlay.PLUS);
        fillSlot.setSlotType(ContainerSlotType.INPUT);
        fillSlot.setSlotOverlay(SlotOverlay.MINUS);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        drainSlot.drainTank();
        fillSlot.fillTank();
        if (dumping != ExtraTileEntityChemicalTank.GasMode.IDLE) {
            if (dumping == ExtraTileEntityChemicalTank.GasMode.DUMPING) {
                chemicalTank.shrinkStack(tier.getStorage() / 400, Action.EXECUTE);
            } else {//dumping == GasMode.DUMPING_EXCESS
                long target = MathUtils.clampToLong(chemicalTank.getCapacity() * MekanismConfig.general.dumpExcessKeepRatio.get());
                long stored = chemicalTank.getStored();
                if (target < stored) {
                    //Dump excess that we need to get to the target (capping at our eject rate for how much we can dump at once)
                    chemicalTank.shrinkStack(Math.min(stored - target, tier.getOutput()), Action.EXECUTE);
                }
            }
        }
        return sendUpdatePacket;
    }

    @Override
    public void nextMode(int tank) {
        if (tank == 0) {
            dumping = dumping.getNext();
            markForSave();
        }
    }

    @Override
    public boolean shouldDumpRadiation() {
        return true;
    }

    @Override
    public int getRedstoneLevel() {
        IChemicalTank currentTank = getCurrentTank();
        return MekanismUtils.redstoneLevelFromContents(currentTank.getStored(), currentTank.getCapacity());
    }

    @Override
    protected boolean makesComparatorDirty(ContainerType<?, ?, ?> type) {
        return type == ContainerType.CHEMICAL;
    }

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerChemicalTankWrapper.class, methodNames = {"getStored", "getCapacity", "getNeeded", "getFilledPercentage"}, docPlaceholder = "tank")
    IChemicalTank getCurrentTank() {
        return chemicalTank;
    }

    public CTTier getTier() {
        return tier;
    }

    public IChemicalTank getChemicalTank() {
        return chemicalTank;
    }

    @Override
    public void parseUpgradeData(HolderLookup.Provider provider, @NotNull IUpgradeData upgradeData) {
        if (upgradeData instanceof ExtraChemicalTankUpgradeData data) {
            redstone = data.redstone;
            setControlType(data.controlType);
            drainSlot.setStack(data.drainSlot.getStack());
            fillSlot.setStack(data.fillSlot.getStack());
            dumping = data.dumping;
            getChemicalTank().setStack(data.storedChemical);
            for (ITileComponent component : getComponents()) {
                component.read(data.components, provider);
            }
        } else {
            super.parseUpgradeData(provider, upgradeData);
        }
    }

    @NotNull
    @Override
    public ExtraChemicalTankUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new ExtraChemicalTankUpgradeData(provider, redstone, getControlType(), drainSlot, fillSlot, dumping, getChemicalTank().getStack(), getComponents());
    }

    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.writeSustainedData(provider, dataMap);
        NBTUtils.writeEnum(dataMap, SerializationConstants.DUMP_MODE, dumping);
    }

    @Override
    public void readSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.readSustainedData(provider, dataMap);
        NBTUtils.setEnumIfPresent(dataMap, SerializationConstants.DUMP_MODE, GasMode.BY_ID, mode -> dumping = mode);
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ExtraDataComponents.DUMP_MODE, dumping);
    }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        dumping = input.getOrDefault(ExtraDataComponents.DUMP_MODE, dumping);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(ExtraTileEntityChemicalTank.GasMode.BY_ID, ExtraTileEntityChemicalTank.GasMode.IDLE, () -> dumping, value -> dumping = value));
    }

    //Methods relating to IComputerTile
    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Set the Dumping mode of the tank")
    void setDumpingMode(ExtraTileEntityChemicalTank.GasMode mode) throws ComputerException {
        validateSecurityIsPublic();
        if (dumping != mode) {
            dumping = mode;
            markForSave();
        }
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Advance the Dumping mode to the next configuration in the list")
    void incrementDumpingMode() throws ComputerException {
        validateSecurityIsPublic();
        nextMode(0);
    }

    @ComputerMethod(requiresPublicSecurity = true, methodDescription = "Descend the Dumping mode to the previous configuration in the list")
    void decrementDumpingMode() throws ComputerException {
        validateSecurityIsPublic();
        dumping = dumping.getPrevious();
        markForSave();
    }
    //End methods IComputerTile

    @NothingNullByDefault
    public enum GasMode implements IIncrementalEnum<GasMode>, IHasTextComponent, StringRepresentable {
        IDLE(MekanismLang.IDLE),
        DUMPING_EXCESS(MekanismLang.DUMPING_EXCESS),
        DUMPING(MekanismLang.DUMPING);

        public static final Codec<ExtraTileEntityChemicalTank.GasMode> CODEC = StringRepresentable.fromEnum(ExtraTileEntityChemicalTank.GasMode::values);
        public static final IntFunction<ExtraTileEntityChemicalTank.GasMode> BY_ID = ByIdMap.continuous(ExtraTileEntityChemicalTank.GasMode::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, ExtraTileEntityChemicalTank.GasMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ExtraTileEntityChemicalTank.GasMode::ordinal);

        private final String serializedName;
        private final ILangEntry langEntry;

        GasMode(ILangEntry langEntry) {
            this.serializedName = name().toLowerCase(Locale.ROOT);
            this.langEntry = langEntry;
        }

        @Override
        public Component getTextComponent() {
            return langEntry.translate();
        }

        @Override
        public ExtraTileEntityChemicalTank.GasMode byIndex(int index) {
            return BY_ID.apply(index);
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
