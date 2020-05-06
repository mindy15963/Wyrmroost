package WolfShotz.Wyrmroost.content.entities.dragon;

import WolfShotz.Wyrmroost.client.animation.Animation;
import WolfShotz.Wyrmroost.content.entities.dragon.ai.goals.*;
import WolfShotz.Wyrmroost.content.entities.dragonegg.DragonEggProperties;
import WolfShotz.Wyrmroost.content.io.container.OWDrakeInvContainer;
import WolfShotz.Wyrmroost.content.items.DragonArmorItem;
import WolfShotz.Wyrmroost.registry.WRSounds;
import WolfShotz.Wyrmroost.util.QuikMaths;
import WolfShotz.Wyrmroost.util.network.NetworkUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SaddleItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;

import static net.minecraft.entity.SharedMonsterAttributes.*;

/**
 * Created by WolfShotz 7/10/19 - 22:18
 */
public class OWDrakeEntity extends AbstractDragonEntity
{
    private static final UUID SPRINTING_ID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    private static final AttributeModifier SPRINTING_SPEED_BOOST = (new AttributeModifier(SPRINTING_ID, "Sprinting speed boost", 1.15F, AttributeModifier.Operation.MULTIPLY_TOTAL)).setSaved(false);

    // Dragon Entity Animations
    public static final Animation SIT_ANIMATION = new Animation(15);
    public static final Animation STAND_ANIMATION = new Animation(15);
    public static final Animation GRAZE_ANIMATION = new Animation(35);
    public static final Animation HORN_ATTACK_ANIMATION = new Animation(15);
    public static final Animation ROAR_ANIMATION = new Animation(86);
    public static final Animation TALK_ANIMATION = new Animation(20);

    // Dragon Entity Data
    private static final DataParameter<Boolean> VARIANT_BOOL = EntityDataManager.createKey(OWDrakeEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SADDLED = EntityDataManager.createKey(OWDrakeEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<ItemStack> ARMOR = EntityDataManager.createKey(OWDrakeEntity.class, DataSerializers.ITEMSTACK);

    public OWDrakeEntity(EntityType<? extends OWDrakeEntity> drake, World world)
    {
        super(drake, world);

        SLEEP_ANIMATION = new Animation(20);
        WAKE_ANIMATION = new Animation(15);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        goalSelector.addGoal(4, new MoveToHomeGoal(this));
        goalSelector.addGoal(5, new ControlledAttackGoal(this, 1, true, 2.1, d -> NetworkUtils.sendAnimationPacket(d, HORN_ATTACK_ANIMATION)));
        goalSelector.addGoal(6, CommonGoalWrappers.followOwner(this, 1.2d, 12f, 3f));
        goalSelector.addGoal(7, new DragonBreedGoal(this, false, true));
        goalSelector.addGoal(8, new GrazeGoal(this, 2, GRAZE_ANIMATION));
        goalSelector.addGoal(9, new WaterAvoidingRandomWalkingGoal(this, 1));
        goalSelector.addGoal(10, CommonGoalWrappers.lookAt(this, 10f));
        goalSelector.addGoal(11, new LookRandomlyGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new DefendHomeGoal(this));
        targetSelector.addGoal(5, CommonGoalWrappers.nonTamedTarget(this, PlayerEntity.class, false));
        targetSelector.addGoal(4, new HurtByTargetGoal(this)
        {
            @Override
            public boolean shouldExecute() { return super.shouldExecute() && !isChild(); }
        });
    }

    @Override
    protected void registerAttributes()
    {
        super.registerAttributes();
        
        getAttribute(MAX_HEALTH).setBaseValue(50d);
        getAttribute(MOVEMENT_SPEED).setBaseValue(0.20989d);
        getAttribute(KNOCKBACK_RESISTANCE).setBaseValue(10);
        getAttribute(FOLLOW_RANGE).setBaseValue(20d);
        getAttribute(ATTACK_KNOCKBACK).setBaseValue(3.2d);
        getAttributes().registerAttribute(ATTACK_DAMAGE).setBaseValue(5d);
    }
    
    // ================================
    //           Entity Data
    // ================================
    @Override
    protected void registerData()
    {
        super.registerData();
        dataManager.register(GENDER, getRNG().nextBoolean());
        dataManager.register(VARIANT_BOOL, false);
        dataManager.register(SADDLED, false);
        dataManager.register(ARMOR, ItemStack.EMPTY);
    }
    
    /** Save Game */
    @Override
    public void writeAdditional(CompoundNBT nbt)
    {
        nbt.putBoolean("gender", getGender());
        nbt.putBoolean("variant", getDrakeVariant());
        
        super.writeAdditional(nbt);
    }
    
    /** Load Game */
    @Override
    public void readAdditional(CompoundNBT nbt)
    {
        super.readAdditional(nbt);

        setGender(nbt.getBoolean("gender"));
        setDrakeVariant(nbt.getBoolean("variant"));
        setArmor(invHandler.map(i -> i.getStackInSlot(1).getItem()).orElse(Items.AIR));
        setSaddled(invHandler.map(i -> !i.getStackInSlot(0).isEmpty()).orElse(false));

        // Datafix
        if (nbt.contains("saddled"))
            invHandler.ifPresent(h -> h.setStackInSlot(1, new ItemStack(Items.SADDLE, 1)));
    }

    /**
     * The Variant of the drake.
     * false == Common, true == Savanna. Boolean since we only have 2 different variants
     */
    public boolean getDrakeVariant()
    {
        return dataManager.get(VARIANT_BOOL);
    }

    /**
     * Set the drake variant
     * false == Common, true == Savanna.
     */
    public void setDrakeVariant(boolean variant)
    {
        dataManager.set(VARIANT_BOOL, variant);
    }

    /**
     * Does the drake have a chest?
     */
    public boolean hasChest()
    {
        return invHandler.map(i -> !i.getStackInSlot(2).isEmpty()).orElse(false);
    }

    /**
     * Whether or not the drake is saddled
     */
    public boolean isSaddled()
    {
        return dataManager.get(SADDLED);
    }

    /**
     * Set the drake saddled
     */
    public void setSaddled(boolean flag)
    {
        if (flag) playSound(SoundEvents.ENTITY_HORSE_SADDLE, 1, 1);
        dataManager.set(SADDLED, flag);
    }

    /**
     * Get the armor of the drake
     */
    public DragonArmorItem getArmor()
    {
        return (DragonArmorItem) dataManager.get(ARMOR).getItem();
    }

    /**
     * Set the armor of the drake
     */
    public void setArmor(Item armor)
    {
        DragonArmorItem.setDragonArmored(this, 1);
        if (!(armor instanceof DragonArmorItem)) armor = null;
        dataManager.set(ARMOR, new ItemStack(armor));
        if (armor != null) playSound(SoundEvents.ENTITY_HORSE_ARMOR, 1, 1);
    }

    public boolean isArmored()
    {
        return dataManager.get(ARMOR).getItem() instanceof DragonArmorItem;
    }

    /**
     * Set sprinting switch for Entity.
     */
    public void setSprinting(boolean sprinting)
    {
        if (isSprinting() == sprinting) return;

        IAttributeInstance attribute = getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        
        super.setSprinting(sprinting);
        
        if (attribute.getModifier(SPRINTING_ID) != null) attribute.removeModifier(SPRINTING_SPEED_BOOST);
        if (sprinting) attribute.applyModifier(SPRINTING_SPEED_BOOST);
    }
    
    @Override
    public int getSpecialChances()
    {
        return 100;
    }

    @Override
    public LazyOptional<ItemStackHandler> createInv()
    {
        return LazyOptional.of(() -> new ItemStackHandler(19));
    }
    
    // ================================
    
    @Override
    public void livingTick()
    {
        if (!world.isRemote)
        {
            if ((getAttackTarget() == null || !getAttackTarget().isAlive()) && isAngry()) setAngry(false);
            setSprinting(isAngry());
        }
        
        if (getAnimation() == ROAR_ANIMATION)
        {
            if (getAnimationTick() == 1)
                playSound(WRSounds.OWDRAKE_ROAR.get(), 2.5f, 1f);
            if (getAnimationTick() == 15)
            {
                for (Entity e : getEntitiesNearby(5)) // Dont get too close now ;)
                {
                    if (e instanceof OWDrakeEntity) continue;
                    double angle = (QuikMaths.getAngle(getPosX(), e.getPosX(), getPosZ(), e.getPosZ()) + 90) * Math.PI / 180;
                    double x = 1.2 * (-Math.cos(angle));
                    double z = 1.2 * (-Math.sin(angle));
                    e.addVelocity(x, 0.4d, z);
                }
            }
            if (getAnimationTick() > 15)
            {
                for (Entity e : getEntitiesNearby(20))
                {
                    if (!(e instanceof LivingEntity) || e instanceof OWDrakeEntity) continue;
                    ((LivingEntity) e).addPotionEffect(new EffectInstance(Effects.SLOWNESS, 120));
                }

                if (!isTamed() && !getPassengers().isEmpty())
                {
                    for (Entity e : getPassengers())
                    {
                        e.stopRiding();
                        e.setMotion(QuikMaths.nextPseudoDouble(getRNG()) * 3.5, 0.8, QuikMaths.nextPseudoDouble(getRNG()) * 3.5);
                    }
                }
            }
        }

        if (getAnimation() == HORN_ATTACK_ANIMATION)
        {
            prevRotationYaw = renderYawOffset = rotationYaw = rotationYawHead;
            if (getAnimationTick() == 8)
            {
                playSound(SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 1, 0.5f);
                world.playSound(getPosX(), getPosY(), getPosZ(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.AMBIENT, 1f, 0.5f, false);
                AxisAlignedBB size = getBoundingBox().shrink(0.2);
                AxisAlignedBB aabb = size.offset(QuikMaths.calculateYawAngle(renderYawOffset, 0, size.getXSize() * 1.2));
                attackInAABB(aabb);
            }
        }
        super.livingTick();
    }
    
    @Override
    public boolean processInteract(PlayerEntity player, Hand hand, ItemStack stack)
    {
        if (super.processInteract(player, hand, stack)) return true;

        // If holding a saddle and this is not a child, Saddle up!
        if (stack.getItem() instanceof SaddleItem && !isSaddled() && !isChild()) // instaceof: for custom saddles (if any)
        {
            invHandler.ifPresent(s ->
            {
                s.setStackInSlot(0, stack);
                consumeItemFromStack(player, stack);
            });
            setSaddled(true);
            return true;
        }

        // If Sneaking, Sit
        if (isOwner(player) && player.isSneaking())
        {
            setSit(!isSitting());

            return true;
        }

        // If Saddled and not sneaking, start riding
        if (isSaddled() && !isBreedingItem(stack) && !isChild() && ((!isTamed() && !isInWater()) || isOwner(player)))
        {
            setSit(false);
            if (!world.isRemote) player.startRiding(this);
            setHomePos(Optional.empty());

            return true;
        }

        // If a child, tame it the old fashioned way
        if (isFoodItem(stack) && isChild() && !isTamed())
        {
            tame(getRNG().nextInt(10) == 0, player);
            consumeItemFromStack(player, stack);

            return true;
        }
        
        return false;
    }
    
    /**
     * Called to handle the movement of the entity
     */
    @Override
    public void travel(Vec3d vec3d)
    {
        if (isBeingRidden() && canBeSteered() && isOwner((LivingEntity) getControllingPassenger()))
        {
            LivingEntity rider = (LivingEntity) getControllingPassenger();
            if (canPassengerSteer() && canBeSteered())
            {
                float f = rider.moveForward, s = rider.moveStrafing;
                float speed = (float) (getAttribute(MOVEMENT_SPEED).getValue() * (rider.isSprinting() ? SPRINTING_SPEED_BOOST.getAmount() : 1));
                boolean moving = (f != 0 || s != 0);
                Vec3d target = new Vec3d(s, vec3d.y, f);

                setSprinting(rider.isSprinting());
                setAIMoveSpeed(speed);
                if (rider.isJumping) jumpController.setJumping();
                super.travel(target);
                if (moving || getAnimation() == OWDrakeEntity.HORN_ATTACK_ANIMATION)
                {
                    prevRotationYaw = rotationYaw = rider.rotationYaw;
                    rotationPitch = rider.rotationPitch * 0.5f;
                    setRotation(rotationYaw, rotationPitch);
                    renderYawOffset = rotationYaw;
                    rotationYawHead = renderYawOffset;
                }
                
                return;
            }
        }
        
        super.travel(vec3d);
    }

    @Override
    public void updatePassenger(Entity passenger)
    {
        super.updatePassenger(passenger);

        if (!isTamed() && passenger instanceof LivingEntity)
        {
            int rand = getRNG().nextInt(100);

            if (passenger instanceof PlayerEntity && rand == 0) tame(true, (PlayerEntity) passenger);
            else if (rand % 20 == 0 && getAnimation() != ROAR_ANIMATION && EntityPredicates.CAN_AI_TARGET.test(passenger))
                setAttackTarget((LivingEntity) passenger);
        }
    }

    @Nullable
    @Override
    public ILivingEntityData onInitialSpawn(IWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag)
    {
        Biome biome = worldIn.getBiome(new BlockPos(this));
        Set<Biome> biomes = BiomeDictionary.getBiomes(BiomeDictionary.Type.SAVANNA);

        if (biomes.contains(biome)) setDrakeVariant(true);

        return super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public void handleSleep()
    {
        if (!isSleeping()
                && --sleepCooldown <= 0
                && !world.isDaytime()
                && (!isTamed() || isSitting())
                && !isBeingRidden()
                && getAttackTarget() == null
                && getNavigator().noPath()
                && !isAngry()
                && !isInWaterOrBubbleColumn()
                && !isFlying()
                && getRNG().nextInt(300) == 0) setSleeping(true);
        else if (isSleeping() && world.isDaytime() && getRNG().nextInt(150) == 0) setSleeping(false);
    }

    @Override // Needed because sometimes size conflicts when transitioning from sit - stand, so keep it constant.
    public double getMountedYOffset() { return 1.85; }

    @Override
    public boolean canBeSteered() { return isSaddled() && isTamed(); }

    @Override
    protected boolean canBeRidden(Entity entityIn)
    {
        if (!super.canBeRidden(entityIn)) return false;
        if (isTamed()) return true;
        return getAnimation() != HORN_ATTACK_ANIMATION && getAnimation() != ROAR_ANIMATION;
    }

    @Override
    public boolean canFly() { return false; }

    @Override
    public boolean onLivingFall(float distance, float damageMultiplier)
    {
        return super.onLivingFall(distance - 2, damageMultiplier);
    }

    @Override
    public void setAttackTarget(@Nullable LivingEntity target)
    {
        if (target != null && getAttackTarget() != target)
        {
            setAngry(true);
            if (!isTamed() && getAnimation() != OWDrakeEntity.ROAR_ANIMATION)
                NetworkUtils.sendAnimationPacket(OWDrakeEntity.this, OWDrakeEntity.ROAR_ANIMATION);
        }
        super.setAttackTarget(target);
    }
    
    @Override
    public void eatGrassBonus()
    {
        if (isChild()) addGrowth(60);
        if (getHealth() < getMaxHealth()) heal(4f);
    }
    
    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn)
    {
        if (ticksExisted % 2 == 0) playSound(SoundEvents.ENTITY_COW_STEP, 0.3f, 1);
        
        super.playStepSound(pos, blockIn);
    }
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound()
    {
        return WRSounds.OWDRAKE_IDLE.get();
    }
    
    @Override
    public void playAmbientSound()
    {
        if (!isSleeping())
        {
            if (noActiveAnimation()) setAnimation(TALK_ANIMATION);
            SoundEvent soundevent = getAmbientSound();
            if (soundevent != null) playSound(soundevent, 1, 1);
        }
    }
    
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return WRSounds.OWDRAKE_HURT.get();
    }
    
    @Override
    protected void playHurtSound(DamageSource source)
    {
        if (noActiveAnimation()) setAnimation(TALK_ANIMATION);
        
        super.playHurtSound(source);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() { return WRSounds.OWDRAKE_DEATH.get(); }

    @Override
    public void setSit(boolean sitting)
    {
        if (sitting != isSitting()) setAnimation(sitting? SIT_ANIMATION : STAND_ANIMATION);

        super.setSit(sitting);
    }

    @Override
    public void performGenericAttack() { setAnimation(HORN_ATTACK_ANIMATION); }

    @Override
    protected boolean isMovementBlocked() { return super.isMovementBlocked(); }

    @Override
    public EntitySize getSize(Pose poseIn)
    {
        return (isSitting() || isSleeping())? super.getSize(poseIn).scale(1f, 0.7f) : super.getSize(poseIn);
    }

    @Override
    protected int getExperiencePoints(PlayerEntity player) { return 2 + rand.nextInt(3); }
    
    @Override
    public void setMountCameraAngles(boolean backView)
    {
        if (backView) GlStateManager.translated(0, -0.5d, 0.5d);
        else GlStateManager.translated(0, 0, -3d);
    }

    @Override
    public Collection<Item> getFoodItems()
    {
        return new ArrayList<>(Tags.Items.CROPS_WHEAT.getAllElements());
    }
    
    @Override
    public DragonEggProperties createEggProperties()
    {
        return new DragonEggProperties(0.65f, 1f, 18000);
    }
    
    @Nullable
    @Override
    public Container createMenu(int windowID, PlayerInventory playerInv, PlayerEntity player)
    {
        return new OWDrakeInvContainer(this, playerInv, windowID);
    }
    
    @Override
    public Animation[] getAnimations()
    {
        return new Animation[]{NO_ANIMATION, GRAZE_ANIMATION, HORN_ATTACK_ANIMATION, SIT_ANIMATION, STAND_ANIMATION, SLEEP_ANIMATION, WAKE_ANIMATION, ROAR_ANIMATION};
    }
}