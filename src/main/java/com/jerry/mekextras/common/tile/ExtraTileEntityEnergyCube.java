package com.jerry.mekextras.common.tile;

import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.capabilities.energy.ExtraEnergyCubeEnergyContainer;
import com.jerry.mekextras.common.tier.ECTier;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.ISlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.upgrade.EnergyCubeUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

public class ExtraTileEntityEnergyCube extends TileEntityConfigurableMachine {

    public static final ModelProperty<CubeSideState[]> SIDE_STATE_PROPERTY = new ModelProperty<>();

    /**
     * This Energy Cube's tier.
     */
    private ECTier tier;
    private float prevScale;

    private ExtraEnergyCubeEnergyContainer energyContainer;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getChargeItem", docPlaceholder = "charge slot")
    EnergyInventorySlot chargeSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDischargeItem", docPlaceholder = "discharge slot")
    EnergyInventorySlot dischargeSlot;

    public ExtraTileEntityEnergyCube(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        configComponent.setupIOConfig(TransmissionType.ITEM, chargeSlot, dischargeSlot, RelativeSide.FRONT, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.FRONT).setEjecting(true);
        ejectorComponent = new TileComponentEjector(this, () -> tier.getOutput());
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY).setCanEject(type -> canFunction());
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        tier = ExtraAttribute.getAdvanceTier(getBlockType(), ECTier.class);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = ExtraEnergyCubeEnergyContainer.create(tier, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(dischargeSlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 17, 35));
        builder.addSlot(chargeSlot = EnergyInventorySlot.drain(energyContainer, listener, 143, 35));
        dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
        chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    public ECTier getAdvanceTier() {
        return tier;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();
        float newScale = MekanismUtils.getScale(prevScale, energyContainer);
        if (newScale != prevScale) {
            prevScale = newScale;
            sendUpdatePacket = true;
        }
        return sendUpdatePacket;
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(energyContainer.getEnergy(), energyContainer.getMaxEnergy());
    }

    @Override
    protected boolean makesComparatorDirty(ContainerType<?, ?, ?> type) {
        return type == ContainerType.ENERGY;
    }

    @Override
    public void parseUpgradeData(HolderLookup.Provider provider, @NotNull IUpgradeData upgradeData) {
        if (upgradeData instanceof EnergyCubeUpgradeData data) {
            redstone = data.redstone;
            setControlType(data.controlType);
            getEnergyContainer().setEnergy(data.energyContainer.getEnergy());
            chargeSlot.setStack(data.chargeSlot.getStack());
            //Copy the contents using NBT so that if it is not actually valid due to a reload we don't crash
            dischargeSlot.deserializeNBT(provider, data.dischargeSlot.serializeNBT(provider));
            for (ITileComponent component : getComponents()) {
                component.read(data.components, provider);
            }
        } else {
            super.parseUpgradeData(provider, upgradeData);
        }
    }

    public ExtraEnergyCubeEnergyContainer getEnergyContainer() {
        return energyContainer;
    }

    @NotNull
    @Override
    public EnergyCubeUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new EnergyCubeUpgradeData(provider, redstone, getControlType(), getEnergyContainer(), chargeSlot, dischargeSlot, getComponents());
    }

    public float getEnergyScale() {
        return prevScale;
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        updateTag.putFloat(SerializationConstants.SCALE, prevScale);
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        ConfigInfo config = getConfig().getConfig(TransmissionType.ENERGY);
        DataType[] currentConfig = new DataType[EnumUtils.SIDES.length];
        if (config != null) {
            for (RelativeSide side : EnumUtils.SIDES) {
                currentConfig[side.ordinal()] = config.getDataType(side);
            }
        }
        super.handleUpdateTag(tag, provider);
        NBTUtils.setFloatIfPresent(tag, SerializationConstants.SCALE, scale -> prevScale = scale);
        if (config != null) {
            for (RelativeSide side : EnumUtils.SIDES) {
                if (currentConfig[side.ordinal()] != config.getDataType(side)) {
                    //Only update the model data if at least one side had the config change
                    updateModelData();
                    break;
                }
            }
        }
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        ConfigInfo config = getConfig().getConfig(TransmissionType.ENERGY);
        if (config == null) {//Should not happen but validate it anyway
            return super.getModelData();
        }
        CubeSideState[] sideStates = new CubeSideState[EnumUtils.SIDES.length];
        for (RelativeSide side : EnumUtils.SIDES) {
            CubeSideState state = CubeSideState.INACTIVE;
            ISlotInfo slotInfo = config.getSlotInfo(side);
            if (slotInfo != null) {
                if (slotInfo.canOutput()) {
                    state = CubeSideState.ACTIVE_LIT;
                } else if (slotInfo.canInput()) {
                    state = CubeSideState.ACTIVE_UNLIT;
                }
            }
            sideStates[side.ordinal()] = state;
        }
        return ModelData.builder().with(SIDE_STATE_PROPERTY, sideStates).build();
    }

    public enum CubeSideState {
        ACTIVE_LIT,
        ACTIVE_UNLIT,
        INACTIVE
    }
}
