package com.example.gengar.item;

import com.example.gengar.entity.GengarEntity;
import com.example.gengar.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GengarPokeballItem extends Item {
    public static final int COOLDOWN_TICKS = 60 * 20; // 60 segundos
    public static final String TAG_ID = "GengarId";

    public GengarPokeballItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        CompoundTag tag = stack.getOrCreateTag();

        // 1) Si ya hay un Gengar fuera de la bola -> recogerlo
        if (tag.hasUUID(TAG_ID)) {
            Entity existing = sl.getEntity(tag.getUUID(TAG_ID));
            if (existing instanceof GengarEntity g && g.isAlive() && g.isOwnedBy(player)) {
                g.saveToBall(stack);
                tag.remove(TAG_ID);
                sl.sendParticles(ParticleTypes.WITCH, g.getX(), g.getY() + 0.6, g.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
                g.stopRiding();
                g.discard();
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
                player.displayClientMessage(Component.translatable("message.gengar.recalled"), true);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            // El Gengar anterior no está cargado: se invoca uno nuevo con los datos guardados.
        }

        // 2) Invocar
        GengarEntity gengar = ModEntities.GENGAR.get().create(sl);
        if (gengar == null) return InteractionResultHolder.fail(stack);
        Vec3 look = player.getLookAngle();
        gengar.moveTo(player.getX() + look.x * 1.5, player.getY(), player.getZ() + look.z * 1.5, player.getYRot() + 180F, 0F);
        gengar.tame(player);
        gengar.loadFromBall(tag);
        sl.addFreshEntity(gengar);
        tag.putUUID(TAG_ID, gengar.getUUID());

        sl.sendParticles(ParticleTypes.WITCH, gengar.getX(), gengar.getY() + 0.6, gengar.getZ(), 25, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.8F);
        player.displayClientMessage(Component.translatable("message.gengar.summoned"), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("GLevel")) {
            tooltip.add(Component.translatable("tooltip.gengar.level", tag.getInt("GLevel")));
            tooltip.add(Component.translatable("tooltip.gengar.friendship", tag.getInt("GFriendship")));
        }
    }
}
