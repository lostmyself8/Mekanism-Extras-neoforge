package com.jerry.mekextras.common.tile.machine;

import com.jerry.mekextras.common.config.ExtraConfig;
import com.jerry.mekextras.common.registry.ExtraBlocks;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.*;
import mekanism.api.math.FloatingLong;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.FluidTankHelper;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableFluidStack;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.FluidInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.registries.MekanismFluids;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.IFluidBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TileEntityAdvanceElectricPump extends TileEntityMekanism implements IConfigurable {

    private static final int BASE_TICKS_REQUIRED = 19;
    public static final int MAX_FLUID = 10_000_000;
    private static final int BASE_OUTPUT_RATE = 1024;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerFluidTankWrapper.class, methodNames = {"getFluid", "getFluidCapacity", "getFluidNeeded",
            "getFluidFilledPercentage"}, docPlaceholder = "buffer tank")
    public BasicFluidTank fluidTank;

    @NotNull
    private FluidStack activeType = FluidStack.EMPTY;
    public int ticksRequired = BASE_TICKS_REQUIRED;

    public int operatingTicks;
    private boolean usedEnergy = false;
    private int outputRate = BASE_OUTPUT_RATE;

    private final Set<BlockPos> recurringNodes = new ObjectOpenHashSet<>();
    private List<BlockCapabilityCache<IFluidHandler, @Nullable Direction>> fluidHandlerAbove = Collections.emptyList();

    private MachineEnergyContainer<TileEntityAdvanceElectricPump> energyContainer;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getInputItem", docPlaceholder = "input slot")
    FluidInventorySlot inputSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getOutputItem", docPlaceholder = "output slot")
    OutputInventorySlot outputSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;

    public TileEntityAdvanceElectricPump(BlockPos pos, BlockState state) {
        super(ExtraBlocks.ADVANCE_ELECTRIC_PUMP, pos, state);
    }

    @NotNull
    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        FluidTankHelper builder = FluidTankHelper.forSide(this::getDirection);
        builder.addTank(fluidTank = BasicFluidTank.output(MAX_FLUID, listener), RelativeSide.TOP);
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSide(this::getDirection);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener), RelativeSide.BACK);
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(inputSlot = FluidInventorySlot.drain(fluidTank, listener, 28, 20), RelativeSide.TOP);
        builder.addSlot(outputSlot = OutputInventorySlot.at(listener, 28, 51), RelativeSide.BOTTOM);
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 143, 35), RelativeSide.BACK);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        inputSlot.drainTank(outputSlot);
        FloatingLong clientEnergyUsed = FloatingLong.ZERO;
        if (canFunction() && (fluidTank.isEmpty() || estimateIncrementAmount() <= fluidTank.getNeeded())) {
            FloatingLong energyPerTick = energyContainer.getEnergyPerTick();
            if (energyContainer.extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL).equals(energyPerTick)) {
                if (!activeType.isEmpty()) {
                    //If we have an active type of fluid, use energy. This can cause there to be ticks where there isn't actually
                    // anything to suck that use energy, but those will balance out with the first set of ticks where it doesn't
                    // use any energy until it actually picks up the first block
                    clientEnergyUsed = energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
                }
                operatingTicks++;
                if (operatingTicks >= ticksRequired) {
                    operatingTicks = 0;
                    if (suck()) {
                        if (clientEnergyUsed.isZero()) {
                            //If it didn't already have an active type (hasn't used energy this tick), then extract energy
                            clientEnergyUsed = energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
                        }
                    } else {
                        reset();
                    }
                }
            }
        }
        usedEnergy = !clientEnergyUsed.isZero();
        if (!fluidTank.isEmpty()) {
            if (fluidHandlerAbove.isEmpty()) {
                fluidHandlerAbove = List.of(Capabilities.FLUID.createCache((ServerLevel) level, worldPosition.above(), Direction.DOWN));
            }
            FluidUtils.emit(fluidHandlerAbove, fluidTank, outputRate);
        }
        return sendUpdatePacket;
    }

    public int estimateIncrementAmount() {
        return fluidTank.getFluid().is(MekanismFluids.HEAVY_WATER.getFluid()) ? ExtraConfig.extraGeneralConfig.pumpHeavyWaterAmount.get() : FluidType.BUCKET_VOLUME;
    }

    private boolean suck() {
        boolean hasFilter = upgradeComponent.isUpgradeInstalled(Upgrade.FILTER);
        //First see if there are any fluid blocks under the pump - if so, suck and adds the location to the recurring list
        if (suck(worldPosition.relative(Direction.DOWN), hasFilter, true)) {
            return true;
        }
        //Even though we can add to recurring in the above for loop, we always then exit and don't get to here if we did so
        List<BlockPos> tempPumpList = new ArrayList<>(recurringNodes);
        Collections.shuffle(tempPumpList);
        //Finally, go over the recurring list of nodes and see if there is a fluid block available to suck - if not, will iterate around the recurring block, attempt to suck,
        //and then add the adjacent block to the recurring list
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (BlockPos tempPumpPos : tempPumpList) {
            if (suck(tempPumpPos, hasFilter, false)) {
                return true;
            }
            //Add all the blocks surrounding this recurring node to the recurring node list
            for (Direction orientation : EnumUtils.DIRECTIONS) {
                mutable.setWithOffset(tempPumpPos, orientation);
                if (WorldUtils.distanceBetween(worldPosition, mutable) <= MekanismConfig.general.maxPumpRange.get()) {
                    if (suck(mutable, hasFilter, true)) {
                        return true;
                    }
                }
            }
            recurringNodes.remove(tempPumpPos);
        }
        return false;
    }

    private boolean suck(BlockPos pos, boolean hasFilter, boolean addRecurring) {
        //Note: we get the block state from the world so that we can get the proper block in case it is fluid logged
        Optional<BlockState> state = WorldUtils.getBlockState(level, pos);
        if (state.isPresent()) {
            BlockState blockState = state.get();
            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty() && fluidState.isSource()) {
                //Just in case someone does weird things and has a fluid state that is empty and a source
                // only allow collecting from non-empty sources
                Block block = blockState.getBlock();
                if (block instanceof IFluidBlock fluidBlock) {
                    if (validFluid(fluidBlock.drain(level, pos, FluidAction.SIMULATE))) {
                        //Actually drain it
                        suck(fluidBlock.drain(level, pos, FluidAction.EXECUTE), pos, addRecurring);
                        return true;
                    }
                } else if (block instanceof BucketPickup bucketPickup) {
                    Fluid sourceFluid = fluidState.getType();
                    FluidStack fluidStack = getOutput(sourceFluid, hasFilter);
                    if (validFluid(fluidStack)) {
                        //If it can be picked up by a bucket, and we actually want to pick it up, do so to update the fluid type we are doing
                        if (shouldPump(level, sourceFluid)) {
                            //Note we only attempt taking if it is not water, or we want to pump water sources
                            // otherwise we assume the type from the fluid state is correct
                            ItemStack pickedUpStack = bucketPickup.pickupBlock(null, level, pos, blockState);
                            if (pickedUpStack.isEmpty()) {
                                //Couldn't actually pick it up, exit
                                return false;
                            } else if (pickedUpStack.getItem() instanceof BucketItem bucket) {
                                //This isn't the best validation check given it may not return a bucket, but it is good enough for now
                                sourceFluid = bucket.getFluid();
                                //Update the fluid stack in case something somehow changed about the type
                                // making sure that we replace to heavy water if we got heavy water
                                fluidStack = getOutput(sourceFluid, hasFilter);
                                if (!validFluid(fluidStack)) {
                                    Mekanism.logger.warn("Fluid removed without successfully picking up. Fluid {} at {} in {} was valid, but after picking up was {}.",
                                            fluidState.getType(), pos, level, sourceFluid);
                                    return false;
                                }
                            }
                        }
                        suck(fluidStack, pos, addRecurring);
                        return true;
                    }
                }
                //Otherwise, we do not know how to drain from the block, or it is not valid, and we shouldn't take it so don't handle it
            }
        }
        return false;
    }

    private boolean shouldPump(Level level, Fluid sourceFluid) {
        if (!MekanismConfig.general.pumpInfiniteFluidSources.get()) {
            if (sourceFluid == Fluids.WATER) {
                //If we don't pump infinite sources, only pump it if water conversion is turned off
                return !level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
            } else if (sourceFluid == Fluids.LAVA) {
                //If we don't pump infinite sources, only pump it if lava conversion is turned off
                return !level.getGameRules().getBoolean(GameRules.RULE_LAVA_SOURCE_CONVERSION);
            }
        }
        return true;
    }

    private FluidStack getOutput(Fluid sourceFluid, boolean hasFilter) {
        if (sourceFluid == Fluids.WATER) {
            if (hasFilter) {
                //The speed of pumping heavy water
                return MekanismFluids.HEAVY_WATER.getFluidStack(ExtraConfig.extraGeneralConfig.pumpHeavyWaterAmount.get());
            }
            //The speed of pumping water
            return MekanismConfig.general.pumpInfiniteFluidSources.get() ? new FluidStack(sourceFluid, FluidType.BUCKET_VOLUME)
                    : new FluidStack(sourceFluid, FluidType.BUCKET_VOLUME * 100);
        }
        return new FluidStack(sourceFluid, FluidType.BUCKET_VOLUME);
    }

    private void suck(@NotNull FluidStack fluidStack, BlockPos pos, boolean addRecurring) {
        //Size doesn't matter, but we do want to take the NBT into account
        activeType = fluidStack.copyWithAmount(1);
        if (addRecurring) {
            pos = pos.immutable();
            recurringNodes.add(pos);
        }
        int amountOffered = fluidStack.getAmount();
        if (fluidTank.insert(fluidStack, Action.EXECUTE, AutomationType.INTERNAL).getAmount() != amountOffered) {
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
    }

    private boolean validFluid(@NotNull FluidStack fluidStack) {
        if (!fluidStack.isEmpty() && (activeType.isEmpty() || activeType.isFluidEqual(fluidStack))) {
            if (fluidTank.isEmpty()) {
                return true;
            } else if (fluidTank.isFluidEqual(fluidStack)) {
                return fluidStack.getAmount() <= fluidTank.getNeeded();
            }
        }
        return false;
    }

    public void reset() {
        activeType = FluidStack.EMPTY;
        recurringNodes.clear();
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbtTags) {
        super.saveAdditional(nbtTags);
        nbtTags.putInt(NBTConstants.PROGRESS, operatingTicks);
        if (!activeType.isEmpty()) {
            nbtTags.put(NBTConstants.FLUID_STORED, activeType.writeToNBT(new CompoundTag()));
        }
        if (!recurringNodes.isEmpty()) {
            ListTag recurringList = new ListTag();
            for (BlockPos nodePos : recurringNodes) {
                recurringList.add(NbtUtils.writeBlockPos(nodePos));
            }
            nbtTags.put(NBTConstants.RECURRING_NODES, recurringList);
        }
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        operatingTicks = nbt.getInt(NBTConstants.PROGRESS);
        NBTUtils.setFluidStackIfPresent(nbt, NBTConstants.FLUID_STORED, fluid -> activeType = fluid);
        if (nbt.contains(NBTConstants.RECURRING_NODES, Tag.TAG_LIST)) {
            ListTag tagList = nbt.getList(NBTConstants.RECURRING_NODES, Tag.TAG_COMPOUND);
            for (int i = 0; i < tagList.size(); i++) {
                recurringNodes.add(NbtUtils.readBlockPos(tagList.getCompound(i)));
            }
        }
    }

    @Override
    public InteractionResult onSneakRightClick(Player player) {
        reset();
        player.displayClientMessage(MekanismLang.PUMP_RESET.translate(), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onRightClick(Player player) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean supportsMode(RedstoneControl mode) {
        return true;
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS_REQUIRED);
            outputRate = BASE_OUTPUT_RATE * (1 + upgradeComponent.getUpgrades(Upgrade.SPEED));
        }
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(fluidTank.getFluidAmount(), fluidTank.getCapacity());
    }

    @Override
    protected boolean makesComparatorDirty(ContainerType<?, ?, ?> type) {
        return type == ContainerType.FLUID;
    }

    @NotNull
    @Override
    public List<Component> getInfo(@NotNull Upgrade upgrade) {
        return UpgradeUtils.getMultScaledInfo(this, upgrade);
    }

    public MachineEnergyContainer<TileEntityAdvanceElectricPump> getEnergyContainer() {
        return energyContainer;
    }

    public boolean usedEnergy() {
        return usedEnergy;
    }

    @NotNull
    public FluidStack getActiveType() {
        return this.activeType;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableBoolean.create(this::usedEnergy, value -> usedEnergy = value));
        container.track(SyncableFluidStack.create(this::getActiveType, value -> activeType = value));
    }

    //Methods relating to IComputerTile
    @ComputerMethod(nameOverride = "reset", requiresPublicSecurity = true)
    void resetPump() throws ComputerException {
        validateSecurityIsPublic();
        reset();
    }
    //End methods IComputerTile
}
