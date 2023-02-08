package zaftnotameni.creatania.block.entity.custom.behavior;

import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.CenteredSideValueBoxTransform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import zaftnotameni.creatania.block.entity.custom.ManaMotorBlockEntity;

public class ManaMotorBehavior extends TileEntityBehaviour {
  public static final BehaviourType<TileEntityBehaviour> TYPE = new BehaviourType<>();
  public CenteredSideValueBoxTransform slot;
  public ManaMotorBlockEntity motor;

  public ManaMotorBehavior(ManaMotorBlockEntity pMotor, CenteredSideValueBoxTransform pSlot) {
    super(pMotor);
    this.motor = pMotor;
    this.slot = pSlot;
  }

  @Override
  public void read(CompoundTag nbt, boolean clientPacket) {
    super.read(nbt, clientPacket);
    this.motor.mana = nbt.getInt("mana");
    this.motor.manaCap = nbt.getInt("mana_cap");
    this.motor.active = nbt.getBoolean("active");
  }

  @Override
  public void write(CompoundTag nbt, boolean clientPacket) {
    nbt.putInt("mana", this.motor.mana);
    nbt.putInt("mana_cap", this.motor.manaCap);
    nbt.putBoolean("active", this.motor.active);
    super.write(nbt, clientPacket);
  }

  @Override
  public BehaviourType<?> getType() {
    return TYPE;
  }
}