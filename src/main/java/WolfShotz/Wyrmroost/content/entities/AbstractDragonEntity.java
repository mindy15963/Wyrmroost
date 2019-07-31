package WolfShotz.Wyrmroost.content.entities;

import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED;

/**
 * Created by WolfShotz 7/10/19 - 21:36
 * This is where the magic happens. Here be our Dragons!
 */
public abstract class AbstractDragonEntity extends TameableEntity implements IAnimatedEntity
{
    private int animationTick;
    private Animation animation = NO_ANIMATION;

    private List<String> immunes = new ArrayList<>();

    // Dragon Entity Data
    private static final DataParameter<Boolean> GENDER = EntityDataManager.createKey(AbstractDragonEntity.class, DataSerializers.BOOLEAN);
//    private static final DataParameter<Boolean> ASLEEP = EntityDataManager.createKey(AbstractDragonEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> ALBINO = EntityDataManager.createKey(AbstractDragonEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SADDLED = EntityDataManager.createKey(AbstractDragonEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FLYING = EntityDataManager.createKey(AbstractDragonEntity.class, DataSerializers.BOOLEAN);


    public AbstractDragonEntity(EntityType<? extends AbstractDragonEntity> dragon, World world) {
        super(dragon, world);
        setTamed(false);

        stepHeight = 1;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SwimGoal(this));
//        goalSelector.addGoal(2, new SleepGoal(this));
        goalSelector.addGoal(3, sitGoal = new SitGoal(this));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.2f, 14, 4));
    }

    // ================================
    //           Entity NBT
    // ================================
    @Override
    protected void registerData() {
        super.registerData();
        dataManager.register(GENDER, getRNG().nextBoolean());
//        dataManager.register(ASLEEP, false);
        dataManager.register(ALBINO, getAlbinoChances() != 0 && getRNG().nextInt(getAlbinoChances()) == 0);
        dataManager.register(SADDLED, false);
        dataManager.register(FLYING, false);

    }

    /** Save Game */
    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putBoolean("Gender", getGender());
//        compound.putBoolean("Asleep", isAsleep());
        compound.putBoolean("Albino", isAlbino());
        compound.putBoolean("Saddled", isSaddled());
    }

    /** Load Game */
    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        setGender(compound.getBoolean("Gender"));
//        setAsleep(compound.getBoolean("Asleep"));
        setAlbino(compound.getBoolean("Albino"));
        setSaddled(compound.getBoolean("Saddled"));
    }

    /**
     * Whether or not the dragonEntity is asleep
     * TODO
     */
//    public boolean isAsleep() { return dataManager.get(ASLEEP); }
//    public void setAsleep(boolean sleeping) { dataManager.set(ASLEEP, sleeping); }

    /**
     * Gets the Gender of the dragonEntity.
     * <P>
     * true = Male | false = Female. Anything else is an abomination.
     */
    public boolean getGender() { return dataManager.get(GENDER); }
    public void setGender(boolean sex) { dataManager.set(GENDER, sex); }

    /**
     * Whether or not this dragonEntity is albino. true == isAlbino, false == is not
     */
    public boolean isAlbino() { return dataManager.get(ALBINO); }
    public void setAlbino(boolean albino) { dataManager.set(ALBINO, albino); }
    /**
     * Set The chances this dragon can be an albino.
     * Set it to 0 to have no chance.
     */
    public abstract int getAlbinoChances();

    /**
     * Whether or not the dragon is saddled
     */
    public boolean isSaddled() { return dataManager.get(SADDLED); }
    public void setSaddled(boolean saddled) { dataManager.set(SADDLED, saddled); }

    /**
     * Whether or not the dragon is flying
     */
    public boolean isFlying() { return dataManager.get(FLYING); }
    /**
     * Whether or not the dragon can fly.
     * For ground entities, return false
     */
    public boolean canFly() { return !isChild(); }
    public void setFlying(boolean fly) {
        if (canFly()) {
            dataManager.set(FLYING, fly);
            jump();
        }
    }

    @Override
    public void setSitting(boolean sitting) {
        isJumping = false;
        navigator.clearPath();
        setAttackTarget(null);

        super.setSitting(sitting);
    }

    /**
     *  Whether or not the dragonEntity is pissed or not.
     */
    public boolean isAngry() { return (this.dataManager.get(TAMED) & 2) != 0; }
    public void setAngry(boolean angry) {
        byte b0 = this.dataManager.get(TAMED);

        if (angry) this.dataManager.set(TAMED, (byte) (b0 | 2));
        else this.dataManager.set(TAMED, (byte) (b0 & -3));
    }

    // ================================

    /**
     * Called frequently so the entity can update its state every tick as required.
     */
    @Override
    public void livingTick() {
        boolean canFly = canFly() && getAltitude() > 2;
        if (canFly != isFlying()) setFlying(true);

        super.livingTick();
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick() {
        if (getAnimation() != NO_ANIMATION) {
            ++animationTick;
            if (world.isRemote && animationTick >= animation.getDuration()) setAnimation(NO_ANIMATION);
        }

        super.tick();
    }

    /**
     * Called to handle the movement of the entity
     */
    @Override
    public void travel(Vec3d vec3d) {
        if (isBeingRidden() && canBeSteered() && isTamed()) {
            LivingEntity rider = (LivingEntity) getControllingPassenger();
            if (canPassengerSteer()) {
                float f = rider.moveForward, s = rider.moveStrafing;
                float speed = (float) getAttribute(MOVEMENT_SPEED).getValue() * (rider.isSprinting() ? 2 : 1);
                Vec3d target = new Vec3d(s, vec3d.y, f);

                setSprinting(rider.isSprinting());
                setAIMoveSpeed(speed);
                super.travel(target);
                setRotation(rotationYaw = rider.rotationYaw, rotationPitch);
//              setRotation(ModUtils.limitAngle(rotationYaw, ModUtils.calcAngle(target), 15), rotationPitch); TODO: Smooth Rotations

                return;
            }
        }

        super.travel(vec3d);
    }

    protected double getAltitude() { return posY - world.getHeight(Heightmap.Type.WORLD_SURFACE, (int) posX, (int) posZ); }

    @Override
    protected float getJumpUpwardsMotion() { return canFly() ? 1 : super.getJumpUpwardsMotion(); }

    /**
     * Set a damage source immunity
     */
    protected void setImmune(DamageSource source) { immunes.add(source.getDamageType()); }
    /**
     * Whether or not the dragon is immune to the source or not
     */
    private boolean isImmune(DamageSource source) { return !immunes.isEmpty() && immunes.contains(source.getDamageType()); }
    @Override
    public boolean isInvulnerableTo(DamageSource source) { return super.isInvulnerableTo(source) || isImmune(source); }

    /**
     * Array Containing all of the dragons food items
     */
    protected abstract Item[] getFoodItems();
    /**
     * Whether or not stack is apart of the dragons diet
     */
    protected boolean isFoodItem(ItemStack stack) {
        if (getFoodItems().length == 0) return false;
        return Arrays.stream(getFoodItems()).anyMatch(item -> item == stack.getItem());
    }

    @Override
    public boolean canPassengerSteer() { return getControllingPassenger() != null && canBeSteered(); }
    @Override
    public boolean canBeSteered() { return getControllingPassenger() instanceof LivingEntity && isSaddled(); }
    @Nullable
    public Entity getControllingPassenger() { return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0); }

    // ================================
    //        Entity Animation
    // ================================
    @Override
    public int getAnimationTick() { return animationTick; }

    @Override
    public void setAnimationTick(int tick) { animationTick = tick; }

    @Override
    public Animation getAnimation() { return animation; }

    @Override
    public void setAnimation(Animation animation) {
        this.animation = animation;
        setAnimationTick(0);
    }

    // ================================

}
