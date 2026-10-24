package com.example.gengar.event;

import com.example.gengar.GengarMod;
import com.example.gengar.client.GengarRenderer;
import com.example.gengar.entity.GengarEntity;
import com.example.gengar.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModEvents {

    /** Bus del mod: atributos de la entidad. */
    @Mod.EventBusSubscriber(modid = GengarMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.GENGAR.get(), GengarEntity.createAttributes().build());
        }
    }

    /** Bus de Forge: XP al eliminar entidades (Método 1 de nivel/amistad). */
    @Mod.EventBusSubscriber(modid = GengarMod.MOD_ID)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onDeath(LivingDeathEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide || victim instanceof GengarEntity || victim instanceof Player) return;

            int xp = Math.max(1, (int) (victim.getMaxHealth() / 2.0F)); // Dragon (200 HP) = 100 XP
            Entity killer = event.getSource().getEntity();

            if (killer instanceof GengarEntity gengar) {
                gengar.addXp(xp);
            } else if (killer instanceof ServerPlayer player) {
                // Si el jugador mata cerca de su Gengar, este también gana experiencia
                for (GengarEntity g : player.level().getEntitiesOfClass(GengarEntity.class,
                        player.getBoundingBox().inflate(32), g -> g.isOwnedBy(player))) {
                    g.addXp(xp);
                }
            }
        }
    }

    /** Bus del mod, solo cliente: renderers. */
    @Mod.EventBusSubscriber(modid = GengarMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientBus {
        @SubscribeEvent
        public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.GENGAR.get(), GengarRenderer::new);
            event.registerEntityRenderer(ModEntities.SHADOW_BALL.get(), NoopRenderer::new);
        }
    }
}
