package WolfShotz.Wyrmroost.content.entities.minutus;

import WolfShotz.Wyrmroost.content.entities.AbstractDragonEntity;
import WolfShotz.Wyrmroost.content.entities.minutus.goals.AttackAboveGoal;
import WolfShotz.Wyrmroost.content.entities.minutus.goals.BurrowGoal;
import WolfShotz.Wyrmroost.content.entities.minutus.goals.RunAwayGoal;
import WolfShotz.Wyrmroost.content.entities.minutus.goals.WalkRandom;
import WolfShotz.Wyrmroost.setup.ItemSetup;
import net.minecraft.block.material.Material;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class MinutusEntity extends AbstractDragonEntity
{
    private static final DataParameter<Boolean> BURROWED = EntityDataManager.createKey(MinutusEntity.class, DataSerializers.BOOLEAN);

    public MinutusEntity(EntityType<? extends MinutusEntity> minutus, World world) {
        super(minutus, world);

        setImmune(DamageSource.IN_WALL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SwimGoal(this));
        goalSelector.addGoal(2, new RunAwayGoal<>(this, LivingEntity.class));
        goalSelector.addGoal(3, new BurrowGoal(this));
        goalSelector.addGoal(4, new AttackAboveGoal(this));
        goalSelector.addGoal(5, new WalkRandom(this));

    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4d);
        getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.4d);
        getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4d);

    }

    // ================================
    //           Entity NBT
    // ================================
    @Override
    protected void registerData() {
        super.registerData();
        dataManager.register(BURROWED, false);
    }

    /** Save Game */
    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putBoolean("Burrowed", isBurrowed());
    }

    /** Load Game */
    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        setBurrowed(compound.getBoolean("Burrowed"));
    }

    public boolean isBurrowed() { return dataManager.get(BURROWED); }
    public void setBurrowed(boolean burrow) { dataManager.set(BURROWED, burrow); }

    /** Set The chances this dragon can be an albino. Set it to 0 to have no chance */
    @Override
    public int getAlbinoChances() { return 25; }

    // ================================


    @Override
    public void livingTick() {
        super.livingTick();
        if (isBurrowed() && world.getBlockState(getPosition().down(1)).getMaterial() == Material.AIR) setBurrowed(false);
    }

    @Override
    public boolean processInteract(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty()) {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putBoolean("isalive", true);
            ItemStack newDrop = new ItemStack(ItemSetup.itemdesertwyrm);
            newDrop.setTag(nbt);

            ItemEntity drop = new ItemEntity(world, posX, posY + 0.5d, posZ, newDrop);
            double d0 = player.posX - this.posX;
            double d1 = player.posY - this.posY;
            double d2 = player.posZ - this.posZ;
            drop.setMotion(d0 * 0.1D, d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * 0.1D);
            world.addEntity(drop);

            remove();

            return true;
        }

        return super.processInteract(player, hand);
    }

    @Override
    public boolean canBePushed() { return !isBurrowed(); }

    @Override
    public boolean canBeCollidedWith() { return !isBurrowed(); }

    @Override
    protected void collideWithEntity(Entity entityIn) { if (!isBurrowed()) super.collideWithEntity(entityIn); }

    @Nullable
    @Override
    public AgeableEntity createChild(AgeableEntity ageableEntity) { return null; }
}
