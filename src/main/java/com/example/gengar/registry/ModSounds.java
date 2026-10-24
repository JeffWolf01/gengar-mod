package com.example.gengar.registry;

import com.example.gengar.GengarMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GengarMod.MOD_ID);

    public static final RegistryObject<SoundEvent> AMBIENT_HOWL = reg("ambient_howl");
    public static final RegistryObject<SoundEvent> STEP = reg("step");
    public static final RegistryObject<SoundEvent> REST = reg("rest");
    public static final RegistryObject<SoundEvent> ATTACK_SHADOWBALL = reg("attack_shadowball");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GengarMod.MOD_ID, name)));
    }
}
