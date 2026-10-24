package com.example.gengar.registry;

import com.example.gengar.GengarMod;
import com.example.gengar.entity.GengarEntity;
import com.example.gengar.entity.ShadowBallEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GengarMod.MOD_ID);

    public static final RegistryObject<EntityType<GengarEntity>> GENGAR = ENTITIES.register("gengar",
            () -> EntityType.Builder.of(GengarEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 1.3F).clientTrackingRange(10).build("gengar"));

    public static final RegistryObject<EntityType<ShadowBallEntity>> SHADOW_BALL = ENTITIES.register("shadow_ball",
            () -> EntityType.Builder.<ShadowBallEntity>of(ShadowBallEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(10).fireImmune().build("shadow_ball"));
}
