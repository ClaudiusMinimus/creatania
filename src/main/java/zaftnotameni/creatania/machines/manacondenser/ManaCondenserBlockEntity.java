package zaftnotameni.creatania.machines.manacondenser;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.IManaReceiver;
import zaftnotameni.creatania.config.CommonConfig;
import zaftnotameni.creatania.registry.Blocks;
import zaftnotameni.sharedbehaviors.ActiveStateSynchronizerBehavior;
import zaftnotameni.sharedbehaviors.IAmManaMachine;
import zaftnotameni.sharedbehaviors.IAmParticleEmittingMachine;
import zaftnotameni.sharedbehaviors.KineticManaMachine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Consumes SU and adds Corrupted Inert Mana blocks in the inventory below
 *
 * Can be configured to also require mana, but requires none by default
 */
public class ManaCondenserBlockEntity extends KineticTileEntity implements IManaReceiver, IAmManaMachine, IAmParticleEmittingMachine {
  public LazyOptional<IManaReceiver> lazyManaReceiver = LazyOptional.empty();
  public ActiveStateSynchronizerBehavior activeStateSynchronizerBehavior;
  public boolean isFirstTick = true;
  public int mana = 0;
  public boolean firstTick = true;
  public ManaCondenserBlockEntity(BlockEntityType<? extends ManaCondenserBlockEntity> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
    this.setLazyTickRate(CommonConfig.MANA_GENERATOR_LAZY_TICK_RATE.get());
  }
  @Nonnull
  @Override
  public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
    if (cap == BotaniaForgeCapabilities.MANA_RECEIVER) return lazyManaReceiver.cast();
    return super.getCapability(cap, side);
  }
  public int getNormalizedRPM() {
    var min = CommonConfig.MANA_CONDENSER_SU_PER_RPM.get();
    if (Math.abs(this.getSpeed()) < min) return 0;
    return Math.max(1, (int) Math.abs(this.getSpeed()));
  }
  @Override
  public void addBehaviours(List<TileEntityBehaviour> behaviours) {
    this.activeStateSynchronizerBehavior = new ActiveStateSynchronizerBehavior(this);
    behaviours.add(this.activeStateSynchronizerBehavior);
    super.addBehaviours(behaviours);
  }
  public float tickCounter = 0f;
  public void serverTick() {
    this.active = false;
    this.firstTick = false;
    var rpm = this.getNormalizedRPM();
    var maxPossibleRpm = AllConfigs.SERVER.kinetics.maxMotorSpeed.get();
    var percentageOfMaxRpm = Math.max(0.01f, rpm / (float) maxPossibleRpm);

    var requiredMana = getManaConsumptionRate();
    if (this.doesNotMeetRequirementsToCondenseMana(rpm, requiredMana)) {
      return;
    }
    if (requiredMana > 0) this.receiveMana(-requiredMana);

    tickCounter += percentageOfMaxRpm / (Math.max(1, CommonConfig.MANA_CONDENSER_THROTTLE_PER_RPM_BELOW_MAX.get()));
    if (tickCounter > 1f) {
      this.insertCorruptedManaBlockBelow();
      tickCounter = 0;
    }
    this.active = true;
  }
  public boolean doesNotMeetRequirementsToCondenseMana(int rpm, int requiredMana) {
    return this.isOverStressed() || (rpm <= 0) || (this.getBlockPos() == null) || !this.isSpeedRequirementFulfilled() || (this.mana < requiredMana);
  }
  public void insertCorruptedManaBlockBelow() {
    BlockEntity entityBelow = this.level.getBlockEntity(this.worldPosition.below());
    if (entityBelow == null) return;
    entityBelow.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).ifPresent((inventoryBelow) -> {
      ItemStack stack = new ItemStack(Blocks.CORRUPT_MANA_BLOCK.get().asItem());
      for (int i = 0; i < inventoryBelow.getSlots(); i++) {
        if (!inventoryBelow.isItemValid(i, stack)) continue;
        if (!inventoryBelow.insertItem(i, stack, true).isEmpty()) continue;
        inventoryBelow.insertItem(i, stack, false);
        break;
      }
    });
  }
  @Override
  public void tick() {
    super.tick();
    isFirstTick = false;
    if (this.level == null || this.level.isClientSide()) return;
    serverTick();
  }
  public int getManaConsumptionRate() { return getNormalizedRPM() * CommonConfig.MANA_CONDENSER_MANA_PER_TICK_PER_RPM.get(); }

  @Override
  public float calculateStressApplied() {
    float impact = CommonConfig.MANA_CONDENSER_SU_PER_RPM.get() * Math.abs(this.getSpeed());
    this.lastStressApplied = impact;
    return impact;
  }
  @Override
  public float calculateAddedStressCapacity() { return 0f; }
  @Override
  public Level getManaReceiverLevel() { return this.getLevel(); }
  @Override
  public BlockPos getManaReceiverPos() { return this.getBlockPos(); }
  @Override
  public int getCurrentMana() { return this.mana; }
  @Override
  public boolean isFull() { return this.mana > CommonConfig.MANA_CONDENSER_MAX_MANA_STORAGE.get(); }
  @Override
  public void receiveMana(int mana) { this.mana = Math.max(0, this.mana + mana); this.setChanged(); }
  @Override
  public boolean canReceiveManaFromBursts() { return !this.isFull(); }
  @Override
  public void invalidateCaps() {
    super.invalidateCaps();
    this.lazyManaReceiver.invalidate();
  }
  @Override
  public void onLoad() {
    super.onLoad();
    lazyManaReceiver = LazyOptional.of(() -> this);
  }
  public boolean active;
  @Override
  public boolean shouldEmitParticles() { return true; }
  @Override
  public boolean isManaMachineActive() { return this.active; }
  @Override
  public boolean isManaMachineDuct() { return false; }
  @Override
  public int getManaMachineMana() { return this.mana; }
  @Override
  public void setManaMachineLastCapacityProvided(float value) { this.lastCapacityProvided = value; }
  @Override
  public void setManaMachineLastStressImpact(float value) { this.lastStressApplied = value; }
  @Override
  public void updateMana(int value) { this.mana = value; }
  @Override
  public void setManaMachineMana(int value) { this.updateMana(value); }
  @Override
  public int getManaMachineGeneratedSpeed() { return 0; }
  @Override
  public KineticManaMachine getManaMachine() { return null; }
  @Override
  public void updateGeneratedRotation(int i) {}
  @Override
  protected void read(CompoundTag compound, boolean clientPacket) {
    super.read(compound, clientPacket);
    this.active = compound.getBoolean("active");
    this.mana = compound.getInt("mana");
  }
  @Override
  protected void write(CompoundTag compound, boolean clientPacket) {
    compound.putBoolean("active", this.active);
    compound.putInt("mana", this.mana);
    super.write(compound, clientPacket);
  }
}


