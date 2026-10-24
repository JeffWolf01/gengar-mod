package com.example.gengar.client;

import com.example.gengar.GengarMod;
import com.example.gengar.entity.GengarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GengarModel extends GeoModel<GengarEntity> {
    @Override
    public ResourceLocation getModelResource(GengarEntity animatable) {
        return new ResourceLocation(GengarMod.MOD_ID, "geo/gengar.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GengarEntity animatable) {
        return new ResourceLocation(GengarMod.MOD_ID, "textures/entity/gengar.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GengarEntity animatable) {
        return new ResourceLocation(GengarMod.MOD_ID, "animations/gengar.animation.json");
    }
}
