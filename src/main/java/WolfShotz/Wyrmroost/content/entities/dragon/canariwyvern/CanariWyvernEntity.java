package WolfShotz.Wyrmroost.content.entities.dragon.canariwyvern;

import WolfShotz.Wyrmroost.Wyrmroost;
import WolfShotz.Wyrmroost.content.entities.dragon.AbstractDragonEntity;
import WolfShotz.Wyrmroost.content.entities.dragon.canariwyvern.goals.CanariAvoidGoal;
import WolfShotz.Wyrmroost.content.entities.dragonegg.DragonEggProperties;
import WolfShotz.Wyrmroost.content.fluids.CausticWaterFluid;
import WolfShotz.Wyrmroost.util.QuikMaths;
import WolfShotz.Wyrmroost.util.entityutils.PlayerMount;
import WolfShotz.Wyrmroost.util.entityutils.ai.FlyerMoveController;
import WolfShotz.Wyrmroost.util.entityutils.ai.goals.*;
import WolfShotz.Wyrmroost.util.entityutils.client.animation.Animation;
import WolfShotz.Wyrmroost.util.network.NetworkUtils;
import com.google.common.collect.Lists;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.controller.BodyController;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

import static net.minecraft.entity.SharedMonsterAttributes.*;

public class CanariWyvernEntity extends AbstractDragonEntity implements PlayerMount.IShoulderMount
{
    public static final Animation FLAP_WINGS_ANIMATION = new Animation(22);
    public static final Animation PREEN_ANIMATION = new Animation(36);
    public static final Animation THREAT_ANIMATION = new Animation(40);
    public static final Animation ATTACK_ANIMATION = new Animation(15);

    public CanariWyvernEntity(EntityType<? extends AbstractDragonEntity> dragon, World world)
    {
        super(dragon, world);

        shouldFlyThreshold = 2;

        moveController = new FlyerMoveController(this, true);
        lookController = new LookController(this);

        setImmune(CausticWaterFluid.CAUSTIC_WATER);
        setImmune(DamageSource.MAGIC);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        goalSelector.addGoal(3, new MoveToHomeGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new FlyerFollowOwnerGoal(this, 7, 1, 4, true));
        goalSelector.addGoal(6, new DragonBreedGoal(this, true, true));
        goalSelector.addGoal(7, new CanariAvoidGoal(this));
        goalSelector.addGoal(7, new FlyerWanderGoal(this, true));
        goalSelector.addGoal(8, CommonGoalWrappers.lookAt(this, 8f));
        goalSelector.addGoal(9, new LookRandomlyGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new DefendHomeGoal(this));
        targetSelector.addGoal(5, CommonGoalWrappers.nonTamedTarget(this, PlayerEntity.class, false));
        targetSelector.addGoal(4, new HurtByTargetGoal(this)
        {
            @Override
            public boolean shouldExecute() { return !isChild() && super.shouldExecute(); }
        });
    }

    @Override
    protected void registerAttributes()
    {
        super.registerAttributes();

        getAttribute(MAX_HEALTH).setBaseValue(16d);
        getAttribute(MOVEMENT_SPEED).setBaseValue(0.2d);
        getAttribute(FOLLOW_RANGE).setBaseValue(2);
        getAttributes().registerAttribute(ATTACK_DAMAGE).setBaseValue(3d);
        getAttributes().registerAttribute(FLYING_SPEED).setBaseValue(0.3);
    }

    @Override
    protected BodyController createBodyController() { return new BodyController(this); }

    // ================================
    //           Entity NBT
    // ================================

    @Override
    protected void registerData()
    {
        super.registerData();

        dataManager.register(VARIANT, getRNG().nextInt(5));
    }

    @Override
    public void writeAdditional(CompoundNBT nbt)
    {
        super.writeAdditional(nbt);

        nbt.putInt("variant", getVariant());
    }

    @Override
    public void readAdditional(CompoundNBT nbt)
    {
        super.readAdditional(nbt);

        setVariant(nbt.getInt("variant"));
    }

    @Override
    public int getSpecialChances() { return 0; }

    // ================================

    @Override
    public void livingTick()
    {
        super.livingTick();

        if (!isSleeping() && !isFlying() && !isRiding() && noActiveAnimation())
        {
            if (getRNG().nextInt(650) == 0) setAnimation(FLAP_WINGS_ANIMATION);
            else if (getRNG().nextInt(350) == 0) setAnimation(PREEN_ANIMATION);
        }

        if (getAnimation() == FLAP_WINGS_ANIMATION)
        {
            if (animationTick == 5 || animationTick == 12) playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 0.7f, 2, true);
            if (animationTick == 9 && getRNG().nextInt(25) == 0)
                entityDropItem(new ItemStack(Items.FEATHER), 0.5f);
        }
    }

    @Override
    public boolean processInteract(PlayerEntity player, Hand hand, ItemStack stack)
    {
        if (super.processInteract(player, hand, stack)) return true;

        if (isOwner(player))
        {
            if (player.isSneaking())
            {
                setSit(!isSitting());

                return true;
            }

            if (PlayerMount.getShoulderEntityCount(player) < 2)
            {
                setSit(true);
                setFlying(false);
                clearAI();
                startRiding(player, true);

                return true;
            }
        }
        return false;
    }

    @Override
    public void travel(Vec3d vec3d)
    {
        // Flying is controlled entirely in the move helper
        if (!isFlying()) super.travel(vec3d);
    }

    @Override
    public void updateRidden()
    {
        super.updateRidden();

        Entity entity = getRidingEntity();

        if (!entity.isAlive())
        {
            stopRiding();
            return;
        }
        
        if (!(entity instanceof PlayerEntity)) return;
        
        PlayerEntity player = (PlayerEntity) entity;

        if ((player.isSneaking() && !player.abilities.isFlying) || player.getSubmergedHeight() > 1.25 || player.isElytraFlying())
        {
            stopRiding();
            return;
        }
        
        rotationYaw = player.rotationYawHead;
        rotationPitch = player.rotationPitch;
        setRotation(rotationYaw, rotationPitch);
        rotationYawHead = player.rotationYawHead;
        prevRotationYaw = player.rotationYawHead;

        double xOffset = checkShoulderOccupants(player)? -0.35f : 0.35f;

        Vec3d vec3d1 = QuikMaths.calculateYawAngle(player.renderYawOffset, xOffset, 0.1).add(player.posX, 0, player.posZ);
        setPosition(vec3d1.x, player.posY + 1.4, vec3d1.z);
    }

    @Override
    public boolean attackEntityAsMob(Entity entity)
    {
        boolean should = super.attackEntityAsMob(entity);
        if (should && entity instanceof LivingEntity)
            ((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.POISON, 200));
        return should;
    }

    @Override
    protected void setupTamedAI() { getAttribute(FOLLOW_RANGE).setBaseValue(16d); }

    @Override
    public void swingArm(Hand hand)
    {
        super.swingArm(hand);
        NetworkUtils.sendAnimationPacket(this, ATTACK_ANIMATION);
    }

    @Override
    public boolean canBeCollidedWith() { return super.canBeCollidedWith() && !isRiding(); }

    @Override
    public boolean isInvulnerableTo(DamageSource source) { return super.isInvulnerableTo(source) || getRidingEntity() != null; }

    /**
     * Array Containing all of the dragons food items
     */
    @Override
    public List<Item> getFoodItems() { return Lists.newArrayList(Items.SWEET_BERRIES); }

    @Override
    public DragonEggProperties createEggProperties()
    {
        return new DragonEggProperties(0.25f, 0.35f, 6000)
                .setCustomTexture(Wyrmroost.rl("textures/entity/dragon/canari/egg.png"))
                .setConditions(c -> c.world.getBlockState(c.getPosition().down()).getBlock() == Blocks.JUNGLE_LEAVES);
    }

    @Override
    public Animation[] getAnimations()
    {
        return new Animation[] {NO_ANIMATION, SLEEP_ANIMATION, WAKE_ANIMATION, FLAP_WINGS_ANIMATION, PREEN_ANIMATION, THREAT_ANIMATION, ATTACK_ANIMATION};
    }
}
