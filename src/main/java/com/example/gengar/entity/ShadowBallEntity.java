package com.example.gengar.entity;

import com.example.gengar.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.joml.Vector3f;

/** Proyectil de Gengar: Bola Sombra. Sin gravedad, daño mágico, no daña al dueño ni a sus mascotas. */
public class ShadowBallEntity extends Projectile {
    private static final DustParticleOptions DARK_PURPLE = new DustParticleOptions(new Vector3f(0.25F, 0.05F, 0.4F), 1.6F);
    private float damage = 4.0F;

    public ShadowBallEntity(EntityType<? extends ShadowBallEntity> type, Level level) {
        super(type, level);
    }

    public ShadowBallEntity(Level level, LivingEntity shooter, float damage) {
        this(ModEntities.SHADOW_BALL.get(), level);
        setOwner(shooter);
        this.damage = damage;
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            onHit(hit);
        }
        if (isRemoved()) return;

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();

        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(DARK_PURPLE, getRandomX(0.5), getY() + 0.2 + random.nextDouble() * 0.2, getRandomZ(0.5), 0, 0, 0);
            }
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, 0.01, 0);
        } else if (tickCount > 80) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        if (getOwner() instanceof GengarEntity g) {
            if (target instanceof GengarEntity || target == g.getOwner()) return false;
            if (target instanceof TamableAnimal t && t.getOwnerUUID() != null && t.getOwnerUUID().equals(g.getOwnerUUID())) return false;
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        result.getEntity().hurt(damageSources().indirectMagic(this, getOwner()), damage);
        burst();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) burst();
    }

    private void burst() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(DARK_PURPLE, getX(), getY(), getZ(), 25, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
    }
}
