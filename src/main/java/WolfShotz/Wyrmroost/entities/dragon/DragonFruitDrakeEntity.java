package WolfShotz.Wyrmroost.entities.dragon;

import WolfShotz.Wyrmroost.WRConfig;
import WolfShotz.Wyrmroost.client.animation.Animation;
import WolfShotz.Wyrmroost.entities.dragon.helpers.goals.ControlledAttackGoal;
import WolfShotz.Wyrmroost.entities.dragon.helpers.goals.DragonBreedGoal;
import WolfShotz.Wyrmroost.entities.dragon.helpers.goals.MoveToHomeGoal;
import WolfShotz.Wyrmroost.entities.dragonegg.DragonEggProperties;
import WolfShotz.Wyrmroost.entities.util.CommonGoalWrappers;
import WolfShotz.Wyrmroost.entities.util.EntityDataEntry;
import WolfShotz.Wyrmroost.registry.WREntities;
import com.google.common.collect.Lists;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED;

@SuppressWarnings("deprecation")
public class DragonFruitDrakeEntity extends AbstractDragonEntity implements IShearable
{
    public static final Animation STAND_ANIMATION = new Animation(15);
    public static final Animation SIT_ANIMATION = new Animation(15);
    public static final Animation BITE_ANIMATION = new Animation(15);

    private int shearCooldownTime;
    private int napTime;

    public DragonFruitDrakeEntity(EntityType<? extends DragonFruitDrakeEntity> dragon, World world)
    {
        super(dragon, world);

        SLEEP_ANIMATION = new Animation(15);
        WAKE_ANIMATION = new Animation(15);

        addDataEntry("ShearTimer", EntityDataEntry.INTEGER, () -> shearCooldownTime, v -> shearCooldownTime = v);
        addDataEntry("Gender", EntityDataEntry.BOOLEAN, GENDER, getRNG().nextBoolean());
    }

    public static void setSpawnConditions()
    {
        BiomeDictionary.getBiomes(BiomeDictionary.Type.JUNGLE)
                .stream()
                .forEach(b -> b.getSpawns(EntityClassification.MONSTER).add(new Biome.SpawnListEntry(WREntities.DRAGON_FRUIT_DRAKE.get(), 8, 2, 4)));
        EntitySpawnPlacementRegistry.register(
                WREntities.DRAGON_FRUIT_DRAKE.get(),
                EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING,
                ((a, b, c, d, e) -> true));
    }

//    public static boolean canSpawnHere(EntityType<DragonFruitDrakeEntity> type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random)
//    {
//        World world = worldIn.getWorld();
//
//        return world.getDimension() instanceof OverworldDimension && WorldCapability.isPortalTriggered(world);
//    }

    // ================================
    //           Entity NBT
    // ================================

    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        goalSelector.addGoal(3, new MoveToHomeGoal(this));
        goalSelector.addGoal(4, new ControlledAttackGoal(this, 1.1, false, 1.5d, AbstractDragonEntity::performGenericAttack));
        goalSelector.addGoal(5, new DragonBreedGoal(this, false, true));
        goalSelector.addGoal(7, CommonGoalWrappers.followOwner(this, 1.2d, 12f, 3f));
        goalSelector.addGoal(8, CommonGoalWrappers.followParent(this, 1));
        goalSelector.addGoal(9, new WaterAvoidingRandomWalkingGoal(this, 1));
        goalSelector.addGoal(10, CommonGoalWrappers.lookAt(this, 7f));
        goalSelector.addGoal(11, new LookRandomlyGoal(this));
        goalSelector.addGoal(6, new TemptGoal(this, 1, false, Ingredient.fromItems(Items.APPLE))
        {
            @Override
            public boolean shouldExecute() { return !isTamed() && isChild() && super.shouldExecute(); }
        });

        targetSelector.addGoal(1, new HurtByTargetGoal(this).setCallsForHelp(DragonFruitDrakeEntity.class));
        targetSelector.addGoal(2, CommonGoalWrappers.nonTamedTarget(this, PlayerEntity.class, false));
    }

    @Override
    protected void registerAttributes()
    {
        super.registerAttributes();
        getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.232524f);
        getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20d);
        getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3d);
    }

    @Override
    public void setSit(boolean sitting)
    {
        if (isSitting() == sitting) return;
        super.setSit(sitting);
        if (sitting) setAnimation(SIT_ANIMATION);
        else setAnimation(STAND_ANIMATION);
    }

    @Override
    public boolean processInteract(PlayerEntity player, Hand hand, ItemStack stack)
    {
        if (super.processInteract(player, hand, stack)) return true;
        if (stack.getItem() == Items.SHEARS)
            return true; // Shears return false on entity interactions. bad, but workaround for it.

        if (!isTamed() && isChild() && isFoodItem(stack))
        {
            tame(getRNG().nextInt(5) == 0, player);
            eat(stack);

            return true;
        }

        if (isOwner(player) && !isChild() && !player.isSneaking())
        {
            setSit(false);
            player.startRiding(this);

            return true;
        }

        if (isOwner(player) && player.isSneaking())
        {
            setSit(!isSitting());

            return true;
        }

        return false;
    }

    @Override
    public void tick()
    {
        super.tick();

        setSprinting(getAttackTarget() != null);

        if (shearCooldownTime > 0) --shearCooldownTime;

        if (getAnimation() == BITE_ANIMATION && animationTick == 7) attackInFront(0);
    }

    @Override
    public void travel(Vec3d vec3d)
    {
        if (isSprinting()) vec3d.mul(2f, 0, 2f);

        if (!isBeingRidden())
        {
            super.travel(vec3d);
            return;
        }

        // We're being ridden, follow rider controls
        LivingEntity rider = (LivingEntity) getControllingPassenger();
        if (canPassengerSteer())
        {
            float f = rider.moveForward, s = rider.moveStrafing;
            float speed = (float) (getAttribute(MOVEMENT_SPEED).getValue());
            boolean moving = (f != 0 || s != 0);
            Vec3d target = new Vec3d(s, vec3d.y, f);

            setAIMoveSpeed(speed / 2);
            super.travel(target);
            if (moving || getAnimation() == BITE_ANIMATION)
            {
                prevRotationYaw = rotationYaw = rider.rotationYaw;
                rotationPitch = rider.rotationPitch * 0.5f;
                setRotation(rotationYaw, rotationPitch);
                renderYawOffset = rotationYaw;
                rotationYawHead = renderYawOffset;
            }
        }
    }

//    @Override
//    public boolean isNotColliding(IWorldReader worldIn)
//    {
//        if (worldIn.checkNoEntityCollision(this) && !worldIn.containsAnyLiquid(getBoundingBox()))
//        {
//            BlockPos blockpos = new BlockPos(posX, getBoundingBox().minY, posZ);
//            return blockpos.getY() < worldIn.getSeaLevel();
//            if (blockpos.getY() < worldIn.getSeaLevel()) return false;
//
//            BlockState blockstate = worldIn.getBlockState(blockpos.down());
//            Block block = blockstate.getBlock();
//            return block == Blocks.GRASS_BLOCK || blockstate.isIn(BlockTags.LEAVES);
//        }
//
//        return false;
//    }

    @Override
    public boolean canSpawn(IWorld worldIn, SpawnReason spawnReasonIn) { return true; }

    @Nullable
    @Override
    public ILivingEntityData onInitialSpawn(IWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag)
    {
        if (getRNG().nextDouble() <= WRConfig.dfdBabyChance) setGrowingAge(getEggProperties().getGrowthTime());

        return super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean canBeSteered() { return true; }

    @Override
    protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) { return 1.8f; }

    @Override
    public double getMountedYOffset() { return super.getMountedYOffset() + 0.1d; }

    @Override
    public boolean isShearable(@Nonnull ItemStack item, IWorldReader world, BlockPos pos)
    {
        return shearCooldownTime <= 0;
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nonnull ItemStack item, IWorld world, BlockPos pos, int fortune)
    {
        playSound(SoundEvents.ENTITY_MOOSHROOM_SHEAR, 1f, 1f);
        shearCooldownTime = 12000;
        return Lists.newArrayList(new ItemStack(Items.APPLE, 1));
    }

    @Override // These bois are lazy, can sleep during the day
    public void handleSleep()
    {
        if (isSleeping() && --napTime <= 0 && world.isDaytime() && getRNG().nextInt(375) == 0)
        {
            setSleeping(false);
            return;
        }
        if (isSleeping() || (isChild() && world.isDaytime())) return;
        if (--sleepCooldown > 0) return;
        if (isTamed() && !isSitting()) return;
//        if (!(getHomePos().isPresent() && isWithinHomeDistanceFromPosition())) return;
        if (!isIdling()) return;
        int sleepChance = world.isDaytime()? 450 : 300; // neps
        if (getRNG().nextInt(sleepChance) == 0)
        {
            setSleeping(true);
            this.napTime = 150 * getRNG().nextInt(9);
        }
    }

    @Override
    public void performGenericAttack() { swingArm(Hand.MAIN_HAND); }

    @Override
    public void swingArm(Hand hand)
    {
        super.swingArm(hand);
        setAnimation(BITE_ANIMATION);
    }

    @Override
    public boolean canFly() { return false; }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_COW_AMBIENT; }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) { return SoundEvents.ENTITY_COW_HURT; }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_COW_DEATH; }

    @Override
    protected float getSoundPitch() { return super.getSoundPitch() * 0.5f; }

    @Override
    public Collection<Item> getFoodItems()
    {
        List<Item> foods = Tags.Items.CROPS.getAllElements().stream().filter(i -> i.getItem() != Items.NETHER_WART).collect(Collectors.toList());
        Collections.addAll(foods, Items.APPLE, Items.SWEET_BERRIES);
        return foods;
    }

    //    @Override
//    public boolean isBreedingItem(ItemStack stack)
//    {
//        return stack.getItem() == WRItems.DRAGON_FRUIT.get();
//    }
    @Override
    public boolean isBreedingItem(ItemStack stack) { return stack.getItem() == Items.APPLE; }

    @Override
    public DragonEggProperties createEggProperties() { return new DragonEggProperties(0.45f, 0.75f, 9600); }

    @Override
    public Animation[] getAnimations() { return new Animation[] {NO_ANIMATION, SLEEP_ANIMATION, WAKE_ANIMATION, STAND_ANIMATION, SIT_ANIMATION}; }
}
