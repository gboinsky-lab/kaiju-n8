// src/main/java/com/kn8/common/registry/KN8Items.java
package com.kn8.common.registry;

import com.kn8.common.support.FieldRadioItem;
import com.kn8.KN8Constants;
import com.kn8.common.combat.MagazineItem;
import com.kn8.common.combat.WeaponItem;
import com.kn8.common.craft.SupplyItem;
import com.kn8.common.trail.TrailScannerItem;
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
    /** 0.7-C: canhao pesado da Mina e baioneta longa do Narumi (armas dos soldados especiais; modelos do Miguel). */
    public static final DeferredItem<WeaponItem> MINA_CANNON =
            ITEMS.register("mina_cannon", () -> new WeaponItem(new Item.Properties()));
    public static final DeferredItem<WeaponItem> NARUMI_BAYONET =
            ITEMS.register("narumi_bayonet", () -> new WeaponItem(new Item.Properties()));
    /** PM-11A: armas numeradas do jogador (requisitos e dominio no numbered_gear/*.json). */
    public static final DeferredItem<WeaponItem> NUMBERS1_BAYONET = ITEMS.register("numbers1_bayonet",
            () -> new WeaponItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<WeaponItem> NUMBERS2_GAUNTLETS = ITEMS.register("numbers2_gauntlets",
            () -> new WeaponItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<WeaponItem> NUMBERS6_RIFLE = ITEMS.register("numbers6_rifle",
            () -> new WeaponItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    /** PM-11B: machado com asas Numbers 4 (plana com Shift no ar). */
    public static final DeferredItem<WeaponItem> NUMBERS4_AXE = ITEMS.register("numbers4_axe",
            () -> new WeaponItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    /** 0.7-F: punhos do Kaiju No. 8 (item invisivel na mao: o dano dos golpes sai do weapon/kaiju_no8_fist.json). */
    public static final DeferredItem<WeaponItem> KAIJU_NO8_FIST =
            ITEMS.register("kaiju_no8_fist", () -> new WeaponItem(new Item.Properties()));
    /** 0.8-A: manoplas Numbers 2 do Isao (item invisivel, as manoplas estao no modelo; weapon/numbers2_gauntlet). */
    public static final DeferredItem<WeaponItem> NUMBERS2_GAUNTLET =
            ITEMS.register("numbers2_gauntlet", () -> new WeaponItem(new Item.Properties()));
    /** MOV-5G: punhos do Isao sem o Numbers 2 (item invisivel; weapon/isao_fist). */
    public static final DeferredItem<WeaponItem> ISAO_FIST =
            ITEMS.register("isao_fist", () -> new WeaponItem(new Item.Properties()));

    /**
     * 0.5.0-D2 (Miguel: sem municao infinita): pentes (guardam os tiros, carregados com a municao) e municao de
     * cada arma de fogo. Capacidade no weapon_profile; receitas na bancada.
     */
    public static final DeferredItem<MagazineItem> RIFLE_MAGAZINE =
            ITEMS.register("rifle_magazine", () -> new MagazineItem(new Item.Properties()));
    public static final DeferredItem<MagazineItem> PISTOL_MAGAZINE =
            ITEMS.register("pistol_magazine", () -> new MagazineItem(new Item.Properties()));
    public static final DeferredItem<Item> RIFLE_AMMO = ITEMS.registerSimpleItem("rifle_ammo");
    public static final DeferredItem<Item> PISTOL_AMMO = ITEMS.registerSimpleItem("pistol_ammo");

    /** 0.1-B (M11b): materiais de desmonte (tabelas em data/kn8/kn8/dismantle). */
    public static final DeferredItem<Item> KAIJU_TISSUE = ITEMS.registerSimpleItem("kaiju_tissue");
    public static final DeferredItem<Item> MUSCLE_FIBER = ITEMS.registerSimpleItem("muscle_fiber");
    public static final DeferredItem<Item> CORE_FRAGMENT = ITEMS.registerSimpleItem("core_fragment");
    public static final DeferredItem<Item> INTACT_CORE =
            ITEMS.registerSimpleItem("intact_core", new Item.Properties().stacksTo(1));

    /** 0.1-B: ovo do soldado (variante rifle, nivel normal; outras pelo /kn8 soldier spawn). */
    public static final DeferredItem<DeferredSpawnEggItem> SOLDIER_SPAWN_EGG = ITEMS.register("soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.SOLDIER, 0x1C1F1A, 0x4DD0E1, new Item.Properties()));
    /** PM-8: oficial de missoes (giver kn8:officer; o de provas pelo NBT kn8_giver). */
    public static final DeferredItem<DeferredSpawnEggItem> DEFENSE_OFFICER_SPAWN_EGG = ITEMS.register(
            "defense_officer_spawn_egg", () -> new DeferredSpawnEggItem(KN8Entities.DEFENSE_OFFICER, 0x1C1F1A,
                    0xE8C547, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CIVILIAN_SPAWN_EGG = ITEMS.register("civilian_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.CIVILIAN, 0x6B8FB5, 0xE0B080, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HOSHINA_SPAWN_EGG = ITEMS.register("hoshina_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.HOSHINA, 0x1A1A22, 0x9B59D0, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HOSHINA_NO10_SPAWN_EGG = ITEMS.register(
            "hoshina_no10_spawn_egg", () -> new DeferredSpawnEggItem(KN8Entities.HOSHINA_NO10, 0x8E1B24, 0x9B59D0,
                    new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> KIKORU_SPAWN_EGG = ITEMS.register("kikoru_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.KIKORU, 0x1A1A1A, 0xF2C94C, new Item.Properties()));
    /** 0.7-C: Reno, Mina e Narumi (soldados especiais). */
    public static final DeferredItem<DeferredSpawnEggItem> RENO_SPAWN_EGG = ITEMS.register("reno_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.RENO, 0x1A1A1A, 0x8FD8FF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MINA_SPAWN_EGG = ITEMS.register("mina_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.MINA, 0x1A1A1A, 0xFF9A3C, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> NARUMI_SPAWN_EGG = ITEMS.register("narumi_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.NARUMI, 0x1A1A1A, 0xFF6FB5, new Item.Properties()));
    /** 0.7-D: formas com as armas numeradas (Kikoru 4, Reno 6, Narumi 1). */
    public static final DeferredItem<DeferredSpawnEggItem> KIKORU_NO4_SPAWN_EGG = ITEMS.register(
            "kikoru_no4_spawn_egg", () -> new DeferredSpawnEggItem(KN8Entities.KIKORU_NO4, 0xE8E8E8, 0x5FD8FF,
                    new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> RENO_NO6_SPAWN_EGG = ITEMS.register(
            "reno_no6_spawn_egg", () -> new DeferredSpawnEggItem(KN8Entities.RENO_NO6, 0x1B3550, 0xA8E6FF,
                    new Item.Properties()));
    /** 0.7-F: Kafka (forma humana) e Kaiju No. 8. */
    public static final DeferredItem<DeferredSpawnEggItem> KAFKA_SPAWN_EGG = ITEMS.register("kafka_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.KAFKA, 0x1A1A1A, 0xE8E8E8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> KAIJU_NO8_SPAWN_EGG = ITEMS.register(
            "kaiju_no8_spawn_egg", () -> new DeferredSpawnEggItem(KN8Entities.KAIJU_NO8, 0x1C1C1C, 0x3FF2E0,
                    new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> NARUMI_NO1_SPAWN_EGG = ITEMS.register(
            "narumi_no1_spawn_egg", () -> new DeferredSpawnEggItem(KN8Entities.NARUMI_NO1, 0x1A1A1A, 0xFFFFFF,
                    new Item.Properties()));
    /** 0.8-A: Isao Shinomiya (Numbers 2). */
    public static final DeferredItem<DeferredSpawnEggItem> ISAO_SPAWN_EGG = ITEMS.register("isao_spawn_egg",
            () -> new DeferredSpawnEggItem(KN8Entities.ISAO, 0x2A2A2A, 0xFFB347, new Item.Properties()));
    /** MOV-5G: Isao Shinomiya sem o Numbers 2. */
    public static final DeferredItem<DeferredSpawnEggItem> ISAO_BASE_SPAWN_EGG = ITEMS.register(
            "isao_base_spawn_egg", () -> new DeferredSpawnEggItem(KN8Entities.ISAO_BASE, 0x2A2A2A, 0xC8C8C8,
                    new Item.Properties()));

    /** 0.2 (Etapa 2): boneco de treino. */
    public static final DeferredItem<TrainingDummyItem> TRAINING_DUMMY = ITEMS.register("training_dummy",
            () -> new TrainingDummyItem(new Item.Properties().stacksTo(16)));

    /** 0.2 (Etapa 3): bancada, trajes (armadura vem do suit/*.json) e suprimentos. */
    public static final DeferredItem<BlockItem> DEFENSE_WORKBENCH = ITEMS.registerSimpleBlockItem("defense_workbench",
            KN8Blocks.DEFENSE_WORKBENCH);
    public static final DeferredItem<BlockItem> TRAINING_CONSOLE = ITEMS.registerSimpleBlockItem("training_console",
            KN8Blocks.TRAINING_CONSOLE);
    public static final DeferredItem<BlockItem> SQUAD_STATION = ITEMS.registerSimpleBlockItem("squad_station",
            KN8Blocks.SQUAD_STATION);
    public static final DeferredItem<BlockItem> RESEARCH_TERMINAL = ITEMS.registerSimpleBlockItem(
            "research_terminal", KN8Blocks.RESEARCH_TERMINAL);
    public static final DeferredItem<BlockItem> BASE_CORE = ITEMS.registerSimpleBlockItem("base_core",
            KN8Blocks.BASE_CORE);
    public static final DeferredItem<BlockItem> WEIGHT_RACK = ITEMS.registerSimpleBlockItem("weight_rack",
            KN8Blocks.WEIGHT_RACK);
    /** PM-9: Rastreador de Kaiju (investiga o rastro de destruicao). */
    public static final DeferredItem<TrailScannerItem> TRAIL_SCANNER = ITEMS.register("trail_scanner",
            () -> new TrailScannerItem(new Item.Properties().stacksTo(1)));
    /** PM-10B: Radio de Campo (apoio militar em missao e invasao). */
    public static final DeferredItem<FieldRadioItem> FIELD_RADIO = ITEMS.register("field_radio",
            () -> new FieldRadioItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ArmorItem> TRAINING_SUIT = suit("training_suit", KN8ArmorMaterials.TRAINING_SUIT);
    public static final DeferredItem<ArmorItem> MK1 = suit("mk1", KN8ArmorMaterials.MK1);
    public static final DeferredItem<ArmorItem> MK1_REINFORCED = suit("mk1_reinforced",
            KN8ArmorMaterials.MK1_REINFORCED);
    public static final DeferredItem<ArmorItem> NUMBERS10_SUIT = suit("numbers10_suit",
            KN8ArmorMaterials.NUMBERS10_SUIT);
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
                        output.accept(MINA_CANNON.get());
                        output.accept(NARUMI_BAYONET.get());
                        output.accept(NUMBERS1_BAYONET.get());
                        output.accept(NUMBERS2_GAUNTLETS.get());
                        output.accept(NUMBERS6_RIFLE.get());
                        output.accept(NUMBERS4_AXE.get());
                        output.accept(full(RIFLE_MAGAZINE.get(), parameters));
                        output.accept(full(PISTOL_MAGAZINE.get(), parameters));
                        output.accept(RIFLE_AMMO.get());
                        output.accept(PISTOL_AMMO.get());
                        output.accept(KAIJU_TISSUE.get());
                        output.accept(MUSCLE_FIBER.get());
                        output.accept(CORE_FRAGMENT.get());
                        output.accept(INTACT_CORE.get());
                        output.accept(SOLDIER_SPAWN_EGG.get());
                        output.accept(DEFENSE_OFFICER_SPAWN_EGG.get());
                        output.accept(CIVILIAN_SPAWN_EGG.get());
                        output.accept(HOSHINA_SPAWN_EGG.get());
                        output.accept(HOSHINA_NO10_SPAWN_EGG.get());
                        output.accept(KIKORU_SPAWN_EGG.get());
                        output.accept(RENO_SPAWN_EGG.get());
                        output.accept(MINA_SPAWN_EGG.get());
                        output.accept(NARUMI_SPAWN_EGG.get());
                        output.accept(KIKORU_NO4_SPAWN_EGG.get());
                        output.accept(RENO_NO6_SPAWN_EGG.get());
                        output.accept(NARUMI_NO1_SPAWN_EGG.get());
                        output.accept(KAFKA_SPAWN_EGG.get());
                        output.accept(KAIJU_NO8_SPAWN_EGG.get());
                        output.accept(ISAO_SPAWN_EGG.get());
                        output.accept(ISAO_BASE_SPAWN_EGG.get());
                        output.accept(TRAINING_DUMMY.get());
                        output.accept(WEIGHT_RACK.get());
                        output.accept(DEFENSE_WORKBENCH.get());
                        output.accept(TRAINING_CONSOLE.get());
                        output.accept(SQUAD_STATION.get());
                        output.accept(RESEARCH_TERMINAL.get());
                        output.accept(BASE_CORE.get());
                        output.accept(TRAIL_SCANNER.get());
                        output.accept(FIELD_RADIO.get());
                        output.accept(TRAINING_SUIT.get());
                        output.accept(MK1.get());
                        output.accept(MK1_REINFORCED.get());
                        output.accept(NUMBERS10_SUIT.get());
                        output.accept(SUIT_COOLANT.get());
                        output.accept(STAMINA_STIM.get());
                        output.accept(RELEASE_CATALYST.get());
                    })
                    .build());

    private KN8Items() {
    }

    /** Pente cheio para a aba criativa (a capacidade vem do perfil da arma, ja carregado no servidor/cliente). */
    private static ItemStack full(MagazineItem item, CreativeModeTab.ItemDisplayParameters parameters) {
        ItemStack stack = new ItemStack(item);
        MagazineItem.setRounds(stack, Math.max(1, com.kn8.common.combat.WeaponHandling.magazineCapacity(item,
                true)));
        return stack;
    }

    private static DeferredItem<ArmorItem> suit(String name,
            net.neoforged.neoforge.registries.DeferredHolder<ArmorMaterial, ArmorMaterial> material) {
        return ITEMS.register(name, () -> new ArmorItem(material, ArmorItem.Type.CHESTPLATE,
                new Item.Properties().stacksTo(1).durability(ArmorItem.Type.CHESTPLATE.getDurability(SUIT_DURABILITY))));
    }

    private static final int SUIT_DURABILITY = 25;
}
