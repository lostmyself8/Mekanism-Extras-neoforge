package com.jerry.mekanism_extras.common.tile;

import com.jerry.mekanism_extras.common.block.attribute.ExtraAttribute;
import com.jerry.mekanism_extras.common.capabilities.chemical.StackedLargeCapWasteBarrel;
import com.jerry.mekanism_extras.common.tier.RWBTier;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.proxy.ProxyChemicalHandler;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class TileEntityLargeCapRadioactiveWasteBarrel extends TileEntityMekanism implements IConfigurable {
    private long lastProcessTick;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerChemicalTankWrapper.class, methodNames = {"getStored", "getCapacity", "getNeeded",
            "getFilledPercentage"}, docPlaceholder = "barrel")
    StackedLargeCapWasteBarrel chemicalTank;
    @Nullable
    private IChemicalTank belowTank;
    private boolean resolvedBelowTank;
    private int processTicks;
    private RWBTier tier;
    private List<BlockCapabilityCache<IChemicalHandler, @Nullable Direction>> chemicalHandlerBelow = Collections.emptyList();

    public TileEntityLargeCapRadioactiveWasteBarrel(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        delaySupplier = NO_DELAY;
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        tier = ExtraAttribute.getAdvanceTier(getBlockType(), RWBTier.class);
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSide(this::getDirection);
        builder.addTank(chemicalTank = StackedLargeCapWasteBarrel.create(this, listener), RelativeSide.TOP, RelativeSide.BOTTOM);
        return builder.build();
    }

    public RWBTier getTier() {
        return tier;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        if (level != null && level.getGameTime() > lastProcessTick) {
            //If we are not on the same tick do stuff, otherwise ignore it (anti tick accelerator protection)
            lastProcessTick = level.getGameTime();
            if (tier.getDecayAmount() > 0 && !chemicalTank.isEmpty() &&
                    !chemicalTank.getType().is(MekanismAPITags.Chemicals.WASTE_BARREL_DECAY_BLACKLIST) &&
                    ++processTicks >= tier.getProcessTicks()) {
                processTicks = 0;
                chemicalTank.shrinkStack(tier.getDecayAmount(), Action.EXECUTE);
            }
            if (getActive()) {
                if (chemicalHandlerBelow.isEmpty()) {
                    chemicalHandlerBelow = List.of(Capabilities.CHEMICAL.createCache((ServerLevel) level, worldPosition.below(), Direction.UP, ConstantPredicates.ALWAYS_TRUE, () -> {
                        //Reset the tank that we know is below this
                        resolvedBelowTank = false;
                        belowTank = null;
                    }));
                }
                IChemicalTank below = getBelowTank();
                if (below == null) {
                    ChemicalUtil.emit(chemicalHandlerBelow, chemicalTank);
                } else {
                    //If the block below this barrel, is also a barrel. Only emit as much as it might be able to accept.
                    // This prevents it then trying to go up the chain back to this barrel and any ones above it
                    ChemicalUtil.emit(chemicalHandlerBelow, chemicalTank, Math.min(below.getNeeded(), chemicalTank.getCapacity()));
                }
            }
            //Note: We don't need to do any checking here if the packet needs due to capacity changing as we do it
            // in TileentityMekanism after this method is called. And given radioactive waste barrels can only contain
            // radioactive substances the check for radiation scale also will work for syncing capacity for purposes
            // of when the client sneak right-clicks on the barrel
        }
        return sendUpdatePacket;
    }

    @Nullable
    private IChemicalTank getBelowTank() {
        if (!resolvedBelowTank) {
            resolvedBelowTank = true;
            IChemicalHandler belowHandler = chemicalHandlerBelow.getFirst().getCapability();
            if (belowHandler instanceof ProxyChemicalHandler chemicalHandler && chemicalHandler.getInternalHandler() instanceof TileEntityLargeCapRadioactiveWasteBarrel barrel) {
                //Note: We don't need to bother with weak references as these are vertical so will always be in the same chunk
                belowTank = barrel.chemicalTank;
            }
        }
        return belowTank;
    }

    public StackedLargeCapWasteBarrel getGasTank() {
        return chemicalTank;
    }

    public double getGasScale() {
        return chemicalTank.getStored() / (double) chemicalTank.getCapacity();
    }

    public ChemicalStack getGas() {
        return chemicalTank.getStack();
    }

    @Override
    public InteractionResult onSneakRightClick(Player player) {
        if (!isRemote()) {
            setActive(!getActive());
            Level world = getLevel();
            if (world != null) {
                world.playSound(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onRightClick(Player player) {
        return InteractionResult.PASS;
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        updateTag.put(SerializationConstants.CHEMICAL, chemicalTank.serializeNBT(provider));
        updateTag.putInt(SerializationConstants.PROGRESS, processTicks);
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setCompoundIfPresent(tag, SerializationConstants.CHEMICAL, nbt -> chemicalTank.deserializeNBT(provider, nbt));
        NBTUtils.setIntIfPresent(tag, SerializationConstants.PROGRESS, val -> processTicks = val);
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(chemicalTank.getStored(), chemicalTank.getCapacity());
    }

    @Override
    protected boolean makesComparatorDirty(ContainerType<?, ?, ?> type) {
        return type == ContainerType.CHEMICAL;
    }
}
