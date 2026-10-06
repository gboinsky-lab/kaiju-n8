// src/main/java/com/kn8/common/registry/KN8Items.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;
import com.kn8.common.combat.WeaponItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Itens do mod e a aba criativa (criada junto com o primeiro item, no M10). */
public final class KN8Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KN8Constants.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KN8Constants.MOD_ID);

    /** M10: armas basicas (numeros no JSON da arma; o item so identifica qual arma esta na mao). */
    public static final DeferredItem<WeaponItem> COMBAT_KNIFE =
            ITEMS.register("combat_knife", () -> new WeaponItem(new Item.Properties()));
    public static final DeferredItem<WeaponItem> RIFLE =
            ITEMS.register("rifle", () -> new WeaponItem(new Item.Properties()));
    /** Etapa A: armas com modelo do Meshy (numeros no JSON da arma, marcados como [SUPOSICAO]). */
    public static final DeferredItem<WeaponItem> PISTOL =
            ITEMS.register("pistol", () -> new WeaponItem(new Item.Properties()));
    public static final DeferredItem<WeaponItem> SWORD =
            ITEMS.register("sword", () -> new WeaponItem(new Item.Properties()));

    /** 0.1-B (M11b): materiais de desmonte (tabelas em data/kn8/kn8/dismantle). */
    public static final DeferredItem<Item> KAIJU_TISSUE = ITEMS.registerSimpleItem("kaiju_tissue");
    public static final DeferredItem<Item> MUSCLE_FIBER = ITEMS.registerSimpleItem("muscle_fiber");
    public static final DeferredItem<Item> CORE_FRAGMENT = ITEMS.registerSimpleItem("core_fragment");
    public static final DeferredItem<Item> INTACT_CORE =
            ITEMS.registerSimpleItem("intact_core", new Item.Properties().stacksTo(1));

    /** 0.1-B: ovo do soldado (variante rifle, nivel normal; outras pelo /kn8 soldier spawn). */
    public static final DeferredItem<DeferredSpawnEggItem> SOLDIER_SPAWN_EGG = ITEMS.register("soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.SOLDIER, 0x1C1F1A, 0x4DD0E1, new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kn8"))
                    .icon(() -> new ItemStack(COMBAT_KNIFE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(COMBAT_KNIFE.get());
                        output.accept(RIFLE.get());
                        output.accept(PISTOL.get());
                        output.accept(SWORD.get());
                        output.accept(KAIJU_TISSUE.get());
                        output.accept(MUSCLE_FIBER.get());
                        output.accept(CORE_FRAGMENT.get());
                        output.accept(INTACT_CORE.get());
                        output.accept(SOLDIER_SPAWN_EGG.get());
                    })
                    .build());

    private KN8Items() {
    }
}
