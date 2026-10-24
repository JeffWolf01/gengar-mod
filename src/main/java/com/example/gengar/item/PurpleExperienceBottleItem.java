package com.example.gengar.item;

import com.example.gengar.entity.GengarEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

public class PurpleExperienceBottleItem extends Item {
    public static final int LEVELS_PER_BOTTLE = 1;

    public PurpleExperienceBottleItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** Clic derecho sobre Gengar: sube nivel. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof GengarEntity gengar) || !gengar.isOwnedBy(player)) return InteractionResult.PASS;

        if (gengar.getGengarLevel() >= GengarEntity.MAX_LEVEL) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.gengar.max_level"), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        if (player.level() instanceof ServerLevel sl) {
            gengar.addLevels(LEVELS_PER_BOTTLE);
            DustParticleOptions purple = new DustParticleOptions(new Vector3f(0.65F, 0.15F, 0.95F), 1.4F);
            sl.sendParticles(purple, gengar.getX(), gengar.getY() + 0.7, gengar.getZ(), 40, 0.4, 0.6, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.WITCH, gengar.getX(), gengar.getY() + 0.7, gengar.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, gengar.getX(), gengar.getY() + 0.3, gengar.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, gengar.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 1.0F, 0.7F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
