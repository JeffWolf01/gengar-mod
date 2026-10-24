package com.example.gengar.entity;

import com.example.gengar.GengarMod;
import com.example.gengar.item.GengarPokeballItem;
import com.example.gengar.item.PurpleExperienceBottleItem;
import com.example.gengar.registry.ModItems;
import com.example.gengar.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;

public class GengarEntity extends TamableAnimal implements GeoEntity, RangedAttackMob {

    // ---------------------------------------------------------------- constantes
    public static final int MAX_LEVEL = 100;
    public static final int MAX_FRIENDSHIP = 100;

    public static final int MODE_FOLLOW = 0;
    public static final int MODE_HEAD = 1;
    public static final int MODE_GUARD = 2;
    public static final int MODE_SLEEP = 3;

    public static final int GUARD_RADIUS = 10;

    /** Escalado Nivel 1 -> Nivel 100. El máximo iguala al Ender Dragon (200 HP, golpe de 10). */
    private static final double HP_MIN = 20.0, HP_MAX = 200.0;
    private static final double DMG_MIN = 3.0, DMG_MAX = 10.0;      // daño base de Bola Sombra
    private static final double ARMOR_MIN = 0.0, ARMOR_MAX = 15.0;   // "resistencia"
    private static final double TOUGH_MIN = 0.0, TOUGH_MAX = 6.0;

    public static final TagKey<Item> FOODS = TagKey.create(Registries.ITEM, new ResourceLocation(GengarMod.MOD_ID, "foods"));
    public static final TagKey<Item> GIFTS = TagKey.create(Registries.ITEM, new ResourceLocation(GengarMod.MOD_ID, "gifts"));

    // ---------------------------------------------------------------- datos sincronizados
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(GengarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(GengarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FRIENDSHIP =
            SynchedEntityData.defineId(GengarEntity.class, EntityDataSerializers.INT);

    // ---------------------------------------------------------------- solo servidor
    private int xp = 0;
    private int prevMode = MODE_FOLLOW;
    private BlockPos guardPos = null;

    // ---------------------------------------------------------------- animaciones
    private static final String CTRL_MAIN = "main";
    private static final String CTRL_ACTION = "action";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gengar.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.gengar.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.gengar.run");
    private static final RawAnimation SIT_HEAD = RawAnimation.begin().thenLoop("animation.gengar.sit_head");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.gengar.sleep");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.gengar.attack");
    private static final RawAnimation HOWL = RawAnimation.begin().thenPlay("animation.gengar.howl");
    private static final RawAnimation CHEER = RawAnimation.begin().thenPlay("animation.gengar.cheer");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GengarEntity(EntityType<? extends GengarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, HP_MIN)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, DMG_MIN)
                .add(Attributes.ARMOR, ARMOR_MIN)
                .add(Attributes.ARMOR_TOUGHNESS, TOUGH_MIN)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_LEVEL, 1);
        entityData.define(DATA_MODE, MODE_FOLLOW);
        entityData.define(DATA_FRIENDSHIP, 0);
    }

    // ================================================================ IA
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new ShadowBallGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 6.0F, 2.0F, true) {
            @Override public boolean canUse() { return canFollow() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canFollow() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D) {
            @Override public boolean canUse() { return getMode() == MODE_GUARD && super.canUse(); }
        });
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // En modo guardián también ataca monstruos cercanos aunque el dueño no esté peleando
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> getMode() == MODE_GUARD));
    }

    private boolean canFollow() {
        int m = getMode();
        return (m == MODE_FOLLOW || m == MODE_HEAD) && !isPassenger();
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return !(target instanceof GengarEntity) && !(target instanceof Player)
                && !(target instanceof TamableAnimal t && owner.getUUID().equals(t.getOwnerUUID()));
    }

    /** Dispara Bolas Sombra mientras el objetivo esté a la vista. */
    static class ShadowBallGoal extends Goal {
        private final GengarEntity gengar;
        private int cooldown;

        ShadowBallGoal(GengarEntity gengar) {
            this.gengar = gengar;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity t = gengar.getTarget();
            return t != null && t.isAlive() && gengar.getMode() != MODE_SLEEP;
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { cooldown = 10; }
        @Override public void stop() { gengar.getNavigation().stop(); }

        @Override public void tick() {
            LivingEntity t = gengar.getTarget();
            if (t == null) return;
            double d = gengar.distanceToSqr(t);
            boolean see = gengar.getSensing().hasLineOfSight(t);
            gengar.getLookControl().setLookAt(t, 30.0F, 30.0F);

            if (!gengar.isPassenger()) {
                if (d > 100.0D || !see) gengar.getNavigation().moveTo(t, 1.1D);
                else gengar.getNavigation().stop();
            }
            if (--cooldown <= 0 && see && d <= 256.0D) {
                gengar.performRangedAttack(t, 1.0F);
                cooldown = Math.max(10, 40 - gengar.getGengarLevel() / 4);
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ShadowBallEntity ball = new ShadowBallEntity(level(), this, getShadowBallDamage());
        double dx = target.getX() - ball.getX();
        double dy = target.getY(0.5D) - ball.getY();
        double dz = target.getZ() - ball.getZ();
        ball.shoot(dx, dy, dz, 1.3F, 1.0F);
        level().addFreshEntity(ball);
        triggerAnim(CTRL_ACTION, "attack"); // el sonido sale del keyframe de la animación
    }

    public float getShadowBallDamage() {
        float bonus = 1.0F + getFriendship() / 500.0F; // hasta +20 % con amistad máxima
        return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * bonus);
    }

    // ================================================================ niveles / stats
    public int getGengarLevel() { return entityData.get(DATA_LEVEL); }
    public int getFriendship() { return entityData.get(DATA_FRIENDSHIP); }
    public int getMode() { return entityData.get(DATA_MODE); }
    public void setMode(int mode) { entityData.set(DATA_MODE, mode); }

    public static int xpToNext(int level) { return 8 + level * 2; }

    public void addXp(int amount) {
        if (level().isClientSide || amount <= 0 || getGengarLevel() >= MAX_LEVEL) return;
        xp += amount;
        boolean up = false;
        while (getGengarLevel() < MAX_LEVEL && xp >= xpToNext(getGengarLevel())) {
            xp -= xpToNext(getGengarLevel());
            entityData.set(DATA_LEVEL, getGengarLevel() + 1);
            up = true;
        }
        if (getGengarLevel() >= MAX_LEVEL) xp = 0;
        if (up) onLevelUp();
    }

    public void addLevels(int n) {
        if (level().isClientSide || n <= 0) return;
        entityData.set(DATA_LEVEL, Mth.clamp(getGengarLevel() + n, 1, MAX_LEVEL));
        onLevelUp();
    }

    private void onLevelUp() {
        refreshStats(true);
        addFriendship(1);
        cheer();
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 12, 0.4, 0.5, 0.4, 0.05);
        }
    }

    public void addFriendship(int n) {
        int old = getFriendship();
        int now = Mth.clamp(old + n, 0, MAX_FRIENDSHIP);
        entityData.set(DATA_FRIENDSHIP, now);
        if (now > old && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, getRandomX(1.0), getY() + 1.0, getRandomZ(1.0), 3, 0.2, 0.2, 0.2, 0.02);
        }
    }

    public void refreshStats(boolean healGain) {
        double t = (getGengarLevel() - 1) / (double) (MAX_LEVEL - 1);
        float oldMax = getMaxHealth();
        setBase(Attributes.MAX_HEALTH, Mth.lerp(t, HP_MIN, HP_MAX));
        setBase(Attributes.ATTACK_DAMAGE, Mth.lerp(t, DMG_MIN, DMG_MAX));
        setBase(Attributes.ARMOR, Mth.lerp(t, ARMOR_MIN, ARMOR_MAX));
        setBase(Attributes.ARMOR_TOUGHNESS, Mth.lerp(t, TOUGH_MIN, TOUGH_MAX));
        if (healGain && getMaxHealth() > oldMax) heal(getMaxHealth() - oldMax);
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    private void setBase(Attribute attr, double value) {
        AttributeInstance inst = getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    // ================================================================ Pokéball
    public void saveToBall(ItemStack ball) {
        CompoundTag tag = ball.getOrCreateTag();
        tag.putInt("GLevel", getGengarLevel());
        tag.putInt("GXp", xp);
        tag.putInt("GFriendship", getFriendship());
        tag.putFloat("GHealth", getHealth());
    }

    public void loadFromBall(CompoundTag tag) {
        entityData.set(DATA_LEVEL, Mth.clamp(tag.contains("GLevel") ? tag.getInt("GLevel") : 1, 1, MAX_LEVEL));
        xp = tag.getInt("GXp");
        entityData.set(DATA_FRIENDSHIP, Mth.clamp(tag.getInt("GFriendship"), 0, MAX_FRIENDSHIP));
        refreshStats(false);
        float hp = tag.contains("GHealth") ? tag.getFloat("GHealth") : getMaxHealth();
        setHealth(Mth.clamp(hp, 1.0F, getMaxHealth()));
    }

    private ItemStack findBall(ServerPlayer owner) {
        for (ItemStack s : owner.getInventory().items) {
            if (s.getItem() == ModItems.GENGAR_POKEBALL.get() && s.hasTag()
                    && s.getTag().hasUUID(GengarPokeballItem.TAG_ID)
                    && s.getTag().getUUID(GengarPokeballItem.TAG_ID).equals(getUUID())) {
                return s;
            }
        }
        for (ItemStack s : owner.getInventory().offhand) {
            if (s.getItem() == ModItems.GENGAR_POKEBALL.get() && s.hasTag()
                    && s.getTag().hasUUID(GengarPokeballItem.TAG_ID)
                    && s.getTag().getUUID(GengarPokeballItem.TAG_ID).equals(getUUID())) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /** En vez de morir, Gengar vuelve a la Pokéball y esta entra en cooldown de 60 s. */
    @Override
    public void die(DamageSource source) {
        if (level().isClientSide) return;
        if (!(getOwner() instanceof ServerPlayer owner)) {
            setHealth(1.0F); // sin dueño conectado no se pierde: queda con 1 de vida
            return;
        }
        stopRiding();
        setHealth(getMaxHealth()); // vuelve curado
        ItemStack ball = findBall(owner);
        if (ball.isEmpty()) {
            ball = new ItemStack(ModItems.GENGAR_POKEBALL.get());
            saveToBall(ball);
            if (!owner.getInventory().add(ball)) owner.drop(ball, false);
        } else {
            saveToBall(ball);
            ball.getTag().remove(GengarPokeballItem.TAG_ID);
        }
        owner.getCooldowns().addCooldown(ModItems.GENGAR_POKEBALL.get(), GengarPokeballItem.COOLDOWN_TICKS);
        owner.displayClientMessage(Component.translatable("message.gengar.fainted"), true);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.6, getZ(), 25, 0.3, 0.5, 0.3, 0.03);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() == getOwner()) return false;
        return super.hurt(source, amount);
    }

    // ================================================================ interacción
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isOwnedBy(player)) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        boolean client = level().isClientSide;

        if (stack.getItem() instanceof PurpleExperienceBottleItem) return InteractionResult.PASS;

        if (stack.is(FOODS)) {
            if (getHealth() >= getMaxHealth() && getFriendship() >= MAX_FRIENDSHIP) return InteractionResult.PASS;
            if (!client) {
                FoodProperties fp = stack.getItem().getFoodProperties();
                heal(fp != null ? Math.max(2, fp.getNutrition() * 2) : 4);
                addFriendship(2);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.2F);
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (stack.is(GIFTS)) { // Método 3: regalos
            if (!client) {
                addFriendship(5);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                cheer();
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (stack.isEmpty()) {
            if (!client) {
                if (player.isShiftKeyDown()) toggleGuard(player);
                else toggleHead(player);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    private void toggleHead(Player player) {
        if (getMode() == MODE_HEAD) {
            stopRiding();
            setMode(MODE_FOLLOW);
            player.displayClientMessage(Component.translatable("message.gengar.mode.follow"), true);
        } else {
            clearGuard();
            startRiding(player, true);
            setMode(MODE_HEAD);
            player.displayClientMessage(Component.translatable("message.gengar.mode.head"), true);
        }
    }

    private void toggleGuard(Player player) {
        if (getMode() == MODE_GUARD) {
            clearGuard();
            setMode(MODE_FOLLOW);
            player.displayClientMessage(Component.translatable("message.gengar.mode.follow"), true);
            return;
        }
        BlockPos pos = blockPosition();
        if (player instanceof ServerPlayer sp && sp.getRespawnPosition() != null
                && sp.getRespawnDimension() == level().dimension()
                && sp.getRespawnPosition().distSqr(blockPosition()) < 32 * 32) {
            pos = sp.getRespawnPosition(); // cama / spawn cercano
        }
        stopRiding();
        guardPos = pos;
        restrictTo(pos, GUARD_RADIUS);
        setMode(MODE_GUARD);
        getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0D);
        player.displayClientMessage(Component.translatable("message.gengar.mode.guard"), true);
    }

    private void clearGuard() {
        guardPos = null;
        clearRestriction();
    }

    // ================================================================ tick
    @Override
    public void tick() {
        super.tick();

        if (isPassenger() && getVehicle() != null) { // girar con la cabeza del jugador
            float yaw = getVehicle().getYRot();
            setYRot(yaw);
            yBodyRot = yaw;
            setYHeadRot(yaw);
        }
        if (level().isClientSide) return;

        LivingEntity owner = getOwner();

        if (owner instanceof ServerPlayer p && p.level() == level()) {
            // Modo Sueño
            if (p.isSleeping() && getMode() != MODE_SLEEP && getMode() != MODE_GUARD) startSleeping(p);
            else if (!p.isSleeping() && getMode() == MODE_SLEEP) wakeUp();

            // Volver a montarse en la cabeza si se cayó (cambio de dimensión, etc.)
            if (getMode() == MODE_HEAD && !isPassenger() && tickCount % 20 == 0
                    && distanceToSqr(p) < 9.0D && !p.isSleeping()) {
                startRiding(p, true);
            }
        } else if (isPassenger() && (owner == null || !owner.isAlive())) {
            stopRiding();
        }

        // Guardián: regresar si se alejó demasiado
        if (getMode() == MODE_GUARD && guardPos != null && tickCount % 40 == 0
                && guardPos.distSqr(blockPosition()) > (GUARD_RADIUS + 4) * (GUARD_RADIUS + 4)
                && getTarget() == null) {
            getNavigation().moveTo(guardPos.getX() + 0.5, guardPos.getY(), guardPos.getZ() + 0.5, 1.0D);
        }

        // Amistad >= 50: regeneración lenta. Aullido ocasional.
        if (tickCount % 100 == 0 && getFriendship() >= 50 && getHealth() < getMaxHealth()) heal(1.0F);
        if (getMode() != MODE_SLEEP && random.nextInt(2400) == 0) howl();
    }

    private void startSleeping(ServerPlayer p) {
        prevMode = getMode();
        stopRiding();
        BlockPos bed = p.getSleepingPos().orElse(p.blockPosition());
        BlockPos spot = findSpotNear(bed);
        if (spot != null) teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        getNavigation().stop();
        setTarget(null);
        setMode(MODE_SLEEP);
    }

    private void wakeUp() {
        setMode(prevMode == MODE_SLEEP ? MODE_FOLLOW : prevMode);
        playSound(SoundEvents.CAT_PURREOW, 0.5F, 1.0F);
    }

    private BlockPos findSpotNear(BlockPos bed) {
        for (int r = 1; r <= 2; r++) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos pos = bed.relative(dir, r);
                BlockPos below = pos.below();
                boolean floor = !level().getBlockState(below).getCollisionShape(level(), below).isEmpty();
                if (floor && level().noCollision(this, getType().getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                    return pos;
                }
            }
        }
        return null;
    }

    // ================================================================ emociones
    public void howl() { triggerAnim(CTRL_ACTION, "howl"); }
    public void cheer() { triggerAnim(CTRL_ACTION, "cheer"); }

    // ================================================================ misc
    @Override public boolean isImmobile() { return super.isImmobile() || getMode() == MODE_SLEEP; }
    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { /* el paso suena por keyframe */ }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.VEX_HURT; }
    @Override public boolean isFood(ItemStack stack) { return stack.is(FOODS); }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean canAddPassenger(Entity e) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    /** Altura extra sobre el punto de montura del jugador para quedar justo sobre su cabeza. */
    @Override public double getMyRidingOffset() { return 0.45D; }

    // ================================================================ NBT
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GLevel", getGengarLevel());
        tag.putInt("GXp", xp);
        tag.putInt("GFriendship", getFriendship());
        tag.putInt("GMode", getMode());
        tag.putInt("GPrevMode", prevMode);
        if (guardPos != null) tag.putIntArray("GGuard", new int[]{guardPos.getX(), guardPos.getY(), guardPos.getZ()});
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_LEVEL, Mth.clamp(tag.contains("GLevel") ? tag.getInt("GLevel") : 1, 1, MAX_LEVEL));
        xp = tag.getInt("GXp");
        entityData.set(DATA_FRIENDSHIP, Mth.clamp(tag.getInt("GFriendship"), 0, MAX_FRIENDSHIP));
        setMode(tag.getInt("GMode"));
        prevMode = tag.getInt("GPrevMode");
        if (tag.contains("GGuard")) {
            int[] g = tag.getIntArray("GGuard");
            if (g.length == 3) {
                guardPos = new BlockPos(g[0], g[1], g[2]);
                restrictTo(guardPos, GUARD_RADIUS);
            }
        }
        refreshStats(false);
    }

    // ================================================================ Geckolib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GengarEntity>(this, CTRL_MAIN, 5, this::mainPredicate)
                .setSoundKeyframeHandler(this::onSoundKeyframe));
        controllers.add(new AnimationController<GengarEntity>(this, CTRL_ACTION, 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("howl", HOWL)
                .triggerableAnim("cheer", CHEER)
                .setSoundKeyframeHandler(this::onSoundKeyframe));
    }

    private PlayState mainPredicate(AnimationState<GengarEntity> state) {
        if (getMode() == MODE_SLEEP) return state.setAndContinue(SLEEP);
        if (isPassenger()) return state.setAndContinue(SIT_HEAD);
        if (state.isMoving()) {
            boolean fast = getDeltaMovement().horizontalDistanceSqr() > 0.03D;
            return state.setAndContinue(fast ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    /** Sonidos sincronizados con "sound_effects" del archivo de animación (solo cliente). */
    private void onSoundKeyframe(SoundKeyframeEvent<GengarEntity> event) {
        if (!level().isClientSide) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(event.getKeyframeData().getSound()));
        if (sound != null) {
            level().playLocalSound(getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 0.8F, 0.95F + random.nextFloat() * 0.1F, false);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
