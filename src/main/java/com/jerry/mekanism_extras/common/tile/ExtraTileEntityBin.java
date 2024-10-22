package com.jerry.mekanism_extras.common.tile;

import com.jerry.mekanism_extras.common.block.attribute.ExtraAttribute;
import com.jerry.mekanism_extras.common.inventory.slot.ExtraBinInventorySlot;
import com.jerry.mekanism_extras.common.tier.BTier;
import com.jerry.mekanism_extras.common.upgrade.ExtraBinUpgradeData;
import mekanism.api.Action;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.SerializationConstants;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.lib.inventory.HandlerTransitRequest;
import mekanism.common.lib.inventory.TransitRequest;
import mekanism.common.tile.base.CapabilityTileEntity;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.upgrade.IUpgradeData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtraTileEntityBin extends TileEntityMekanism implements IConfigurable {
    @Nullable
    private BlockCapabilityCache<IItemHandler, @Nullable Direction> targetInventory;
    public int addTicks = 0;
    public int removeTicks = 0;
    private int delayTicks;
    private boolean needsSync;
    private BTier tier;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getStored", docPlaceholder = "bin")
    ExtraBinInventorySlot binSlot;
    public ExtraTileEntityBin(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        tier = ExtraAttribute.getAdvanceTier(getBlockType(), BTier.class);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(binSlot = ExtraBinInventorySlot.create(listener, tier));
        return builder.build();
    }

    public BTier getTier() {
        return tier;
    }

    public int getItemCount() {
        return binSlot.getCount();
    }

    public ExtraBinInventorySlot getBinSlot() {
        return binSlot;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        addTicks = Math.max(0, addTicks - 1);
        removeTicks = Math.max(0, removeTicks - 1);
        delayTicks = Math.max(0, delayTicks - 1);
        if (delayTicks == 0) {
            if (getActive()) {
                //Note: We can't just pass "this" and have to instead look up the capability to make sure we respect any sidedness
                // we short circuit looking it up from the world though, and just query the provider we add to the tile directly
                IItemHandler capability = CapabilityTileEntity.ITEM_HANDLER_PROVIDER.getCapability(this, Direction.DOWN);
                HandlerTransitRequest request = new HandlerTransitRequest(capability);
                request.addItem(binSlot.getBottomStack(), 0);
                if (targetInventory == null) {
                    targetInventory = Capabilities.ITEM.createCache((ServerLevel) level, getBlockPos().below(), Direction.UP);
                }
                TransitRequest.TransitResponse response = request.eject(this, targetInventory.getCapability(), 0, LogisticalTransporterBase::getColor);
                if (!response.isEmpty()) {
                    int sendingAmount = response.getSendingAmount();
                    MekanismUtils.logMismatchedStackSize(binSlot.shrinkStack(sendingAmount, Action.EXECUTE), sendingAmount);
                }
                delayTicks = MekanismUtils.TICKS_PER_HALF_SECOND;
            }
        } else {
            delayTicks--;
        }
        if (needsSync) {
            sendUpdatePacket = true;
            needsSync = false;
        }
        return sendUpdatePacket;
    }

    @Override
    public InteractionResult onSneakRightClick(Player player) {
        setActive(!getActive());
        Level world = getLevel();
        if (world != null) {
            world.playSound(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onRightClick(Player player) {
        return InteractionResult.PASS;
    }

    public boolean toggleLock() {
        return setLocked(!binSlot.isLocked());
    }

    public boolean setLocked(boolean isLocked) {
        if (binSlot.setLocked(isLocked)) {
            if (getLevel() != null && !isRemote()) {
                needsSync = true;
                markForSave();
                getLevel().playSound(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public void parseUpgradeData(HolderLookup.Provider provider, @NotNull IUpgradeData upgradeData) {
        if (upgradeData instanceof ExtraBinUpgradeData data) {
            redstone = data.redstone();
            ExtraBinInventorySlot previous = data.binSlot();
            binSlot.setStack(previous.getStack());
            binSlot.setLockStack(previous.getLockStack());
        } else {
            super.parseUpgradeData(provider, upgradeData);
        }
    }

    @NotNull
    @Override
    public ExtraBinUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new ExtraBinUpgradeData(redstone, getBinSlot());
    }

    @Override
    public void onContentsChanged() {
        super.onContentsChanged();
        if (level != null && !isRemote()) {
            needsSync = true;
        }
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        updateTag.put(SerializationConstants.ITEM, binSlot.serializeNBT(provider));
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setCompoundIfPresent(tag, SerializationConstants.ITEM, nbt -> binSlot.deserializeNBT(provider, nbt));
    }

    //Methods relating to IComputerTile
    @ComputerMethod(methodDescription = "Get the maximum number of items the bin can contain.")
    int getCapacity() {
        return binSlot.getLimit(binSlot.getStack());
    }

    @ComputerMethod(methodDescription = "If true, the Bin is locked to a particular item type.")
    boolean isLocked() {
        return binSlot.isLocked();
    }

    @ComputerMethod(methodDescription = "Get the type of item the Bin is locked to (or Air if not locked)")
    ItemStack getLock() {
        return binSlot.getLockStack();
    }

    @ComputerMethod(methodDescription = "Lock the Bin to the currently stored item type. The Bin must not be creative, empty, or already locked")
    void lock() throws ComputerException {
        if (binSlot.isEmpty()) {
            throw new ComputerException("Empty bins cannot be locked!");
        } else if (!setLocked(true)) {
            throw new ComputerException("This bin is already locked!");
        }
    }

    @ComputerMethod(methodDescription = "Unlock the Bin's fixed item type. The Bin must not be creative, or already unlocked")
    void unlock() throws ComputerException {
        if (!setLocked(true)) {
            throw new ComputerException("This bin is not locked!");
        }
    }
    //End methods IComputerTile
}
