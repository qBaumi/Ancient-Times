package com.delu.ancienttimes.common.entity;

import com.delu.ancienttimes.AncientTimes;
import com.delu.ancienttimes.common.util.NbtUtils;
import com.delu.ancienttimes.registries.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Predicate;

public class Scalemouflis extends Animal implements GeoEntity, NeutralMob {
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");

    public static final RawAnimation ATTACK_AIR = RawAnimation.begin().thenPlay("attack-1-blending");
    public static final RawAnimation ATTACK_WATER = RawAnimation.begin().thenPlay("attack-2-water");

    private static final UniformInt ANGRY_TIMER = TimeUtil.rangeOfSeconds(30, 60);

    public static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Scalemouflis.class, EntityDataSerializers.INT);

    public static AttributeSupplier.Builder createAttributes(){
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40d)
                .add(Attributes.MOVEMENT_SPEED, 0.1d)
                .add(Attributes.ATTACK_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 4.8);
    }
    private int remainingPersistentAngerTime = 0;
    private UUID persistentAngerTarget;
    protected AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public Scalemouflis(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 1F, 1F, false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, Variant.generateRandom(this.level().random).ordinal());
    }

    protected <E extends Scalemouflis> PlayState testAnimController(AnimationState<E> event){ // TODO add attack animations
        if (event.isMoving()) {
            if (this.isInWater()) {
                return event.setAndContinue(SWIM);
            } else {
                return event.setAndContinue(WALK);
            }
        } else {
            return event.setAndContinue(IDLE);
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return ModEntities.SCALEMOUFLIS.get().create(serverLevel);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "scallywag", 10, this::testAnimController));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        //this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.9, false));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.1D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.5D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 7.5f));

        this.targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this, Cod.class, 10, true, true, null));
        //this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return remainingPersistentAngerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int i) {
        remainingPersistentAngerTime = i;
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID uuid) {
        persistentAngerTarget = uuid;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(ANGRY_TIMER.sample(this.level().random));
    }

    public Variant getVariant() {
        return Variant.values()[entityData.get(VARIANT)];
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        NbtUtils.setIfExists(pCompound, "variant", CompoundTag::getInt, this.entityData, VARIANT);
        NbtUtils.setIfExists(pCompound, "persistentAngerTarget", CompoundTag::getUUID, this::setPersistentAngerTarget);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("variant", getVariant().ordinal());
        if (this.getPersistentAngerTarget() != null)
            pCompound.putUUID("persistentAngerTarget", this.getPersistentAngerTarget());
    }

    public void setVariant(Variant variant) {
        this.entityData.set(VARIANT, variant.ordinal());
    }

    public enum Variant{
        VARIANT1(AncientTimes.entityTexture("scalemouflis/scalemouflis_blood_texture.png")),
        VARIANT2(AncientTimes.entityTexture("scalemouflis/scalemouflis_pond_snail_texture.png")),
        VARIANT3(AncientTimes.entityTexture("scalemouflis/scalemouflis_normal_texture.png"));

        private final ResourceLocation texture;

        Variant(ResourceLocation texture) {
            this.texture = texture;
        }

        public ResourceLocation getTexture() {
            return texture;
        }

        public static Variant generateRandom(RandomSource random){
            return Variant.values()[random.nextInt(Variant.values().length)];
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void baseTick() {
        this.setAirSupply(this.getMaxAirSupply());
        super.baseTick();
    }


    @Override
    public int getMaxAirSupply() {
        return 1000; // Duration in ticks (300 = 15 seconds underwater without suffocating)
    }

    @Override
    public boolean isPushedByFluid() {
        return false; // Prevent the entity from being pushed by water currents
    }

    @Override
    public boolean isInWater() {
        return this.level().isWaterAt(this.blockPosition());
    }

    @Override
    public boolean isSwimming() {
        return this.isInWater();
    }
    public MobType getMobType() {
        return MobType.WATER;
    }
    public void travel(Vec3 pTravelVector) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(pTravelVector);
        }

    }

}
