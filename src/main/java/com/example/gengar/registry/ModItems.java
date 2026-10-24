package com.example.gengar.registry;

import com.example.gengar.GengarMod;
import com.example.gengar.item.GengarPokeballItem;
import com.example.gengar.item.PurpleExperienceBottleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GengarMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GengarMod.MOD_ID);

    public static final RegistryObject<Item> GENGAR_POKEBALL = ITEMS.register("gengar_pokeball",
            () -> new GengarPokeballItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> PURPLE_EXPERIENCE_BOTTLE = ITEMS.register("purple_experience_bottle",
            () -> new PurpleExperienceBottleItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<CreativeModeTab> GENGAR_TAB = TABS.register("gengar_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gengar"))
                    .icon(() -> new ItemStack(GENGAR_POKEBALL.get()))
                    .displayItems((params, out) -> {
                        out.accept(GENGAR_POKEBALL.get());
                        out.accept(PURPLE_EXPERIENCE_BOTTLE.get());
                    }).build());
}
