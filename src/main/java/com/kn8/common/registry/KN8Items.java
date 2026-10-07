// src/main/java/com/kn8/common/registry/KN8Items.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;
import com.kn8.common.combat.WeaponItem;
import com.kn8.common.craft.SupplyItem;
import com.kn8.common.training.TrainingDummyItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
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
    /** 0.2: machado (estilo "heavy": golpe lento e forte; modelo do Meshy, numeros [SUPOSICAO] no JSON). */
    public static final DeferredItem<WeaponItem> AXE =
            ITEMS.register("axe", () -> new WeaponItem(new Item.Properties()));
    /** 0.6-D: espada do Hoshina (uma; ele usa o par). Arma especial: especial = corte a distancia (Kuuchi). */
    public static final DeferredItem<WeaponItem> HOSHINA_SWORD =
            ITEMS.register("hoshina_sword", () -> new WeaponItem(new Item.Properties()));

    /** 0.1-B (M11b): materiais de desmonte (tabelas em data/kn8/kn8/dismantle). */
    public static final DeferredItem<Item> KAIJU_TISSUE = ITEMS.registerSimpleItem("kaiju_tissue");
    public static final DeferredItem<Item> MUSCLE_FIBER = ITEMS.registerSimpleItem("muscle_fiber");
    public static final DeferredItem<Item> CORE_FRAGMENT = ITEMS.registerSimpleItem("core_fragment");
    public static final DeferredItem<Item> INTACT_CORE =
            ITEMS.registerSimpleItem("intact_core", new Item.Properties().stacksTo(1));

    /** 0.1-B: ovo do soldado (variante rifle, nivel normal; outras pelo /kn8 soldier spawn). */
    public static final DeferredItem<DeferredSpawnEggItem> SOLDIER_SPAWN_EGG = ITEMS.register("soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.SOLDIER, 0x1C1F1A, 0x4DD0E1, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HOSHINA_SPAWN_EGG = ITEMS.register("hoshina_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.HOSHINA, 0x1A1A22, 0x9B59D0, new Item.Properties()));

    /** 0.2 (Etapa 2): boneco de treino. */
    public static final DeferredItem<TrainingDummyItem> TRAINING_DUMMY = ITEMS.register("training_dummy",
            () -> new TrainingDummyItem(new Item.Properties().stacksTo(16)));

    /** 0.2 (Etapa 3): bancada, trajes (armadura vem do suit/*.json) e suprimentos. */
    public static final DeferredItem<BlockItem> DEFENSE_WORKBENCH = ITEMS.registerSimpleBlockItem("defense_workbench",
            KN8Blocks.DEFENSE_WORKBENCH);
    public static final DeferredItem<ArmorItem> TRAINING_SUIT = suit("training_suit", KN8ArmorMaterials.TRAINING_SUIT);
    public static final DeferredItem<ArmorItem> MK1 = suit("mk1", KN8ArmorMaterials.MK1);
    public static final DeferredItem<ArmorItem> MK1_REINFORCED = suit("mk1_reinforced",
            KN8ArmorMaterials.MK1_REINFORCED);
    public static final DeferredItem<SupplyItem> SUIT_COOLANT = ITEMS.register("suit_coolant",
            () -> new SupplyItem(SupplyItem.Kind.COOLANT, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<SupplyItem> STAMINA_STIM = ITEMS.register("stamina_stim",
            () -> new SupplyItem(SupplyItem.Kind.STIM, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<SupplyItem> RELEASE_CATALYST = ITEMS.register("release_catalyst",
            () -> new SupplyItem(SupplyItem.Kind.CATALYST, new Item.Properties().stacksTo(8)
                    .rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kn8"))
                    .icon(() -> new ItemStack(COMBAT_KNIFE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(COMBAT_KNIFE.get());
                        output.accept(RIFLE.get());
                        output.accept(PISTOL.get());
                        output.accept(SWORD.get());
                        output.accept(AXE.get());
                        output.accept(HOSHINA_SWORD.get());
                        output.accept(KAIJU_TISSUE.get());
                        output.accept(MUSCLE_FIBER.get());
                        output.accept(CORE_FRAGMENT.get());
                        output.accept(INTACT_CORE.get());
                        output.accept(SOLDIER_SPAWN_EGG.get());
                        output.accept(HOSHINA_SPAWN_EGG.get());
                        output.accept(TRAINING_DUMMY.get());
                        output.accept(DEFENSE_WORKBENCH.get());
                        output.accept(TRAINING_SUIT.get());
                        output.accept(MK1.get());
                        output.accept(MK1_REINFORCED.get());
                        output.accept(SUIT_COOLANT.get());
                        output.accept(STAMINA_STIM.get());
                        output.accept(RELEASE_CATALYST.get());
                    })
                    .build());

    private KN8Items() {
    }

    private static DeferredItem<ArmorItem> suit(String name,
            net.neoforged.neoforge.registries.DeferredHolder<ArmorMaterial, ArmorMaterial> material) {
        return ITEMS.register(name, () -> new ArmorItem(material, ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1).durability(ArmorItem.Type.CHESTPLATE.getDurability(SUIT_DURABILITY))));
    }

    private static final int SUIT_DURABILITY = 25;
}
