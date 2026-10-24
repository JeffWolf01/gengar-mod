package com.example.gengar;

import com.example.gengar.registry.ModEntities;
import com.example.gengar.registry.ModItems;
import com.example.gengar.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GengarMod.MOD_ID)
public class GengarMod {
    public static final String MOD_ID = "gengar";

    public GengarMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.SOUNDS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        // Geckolib 4 se inicializa solo; no hace falta GeckoLib.initialize().
    }
}
