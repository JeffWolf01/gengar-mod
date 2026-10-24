package com.example.gengar.client;

import com.example.gengar.entity.GengarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class GengarRenderer extends GeoEntityRenderer<GengarEntity> {
    public GengarRenderer(EntityRendererProvider.Context context) {
        super(context, new GengarModel());
        this.shadowRadius = 0.45F;
        // Ojos rojos brillantes: usa textures/entity/gengar_glowmask.png
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
