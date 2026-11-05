package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SuitDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.registry.KN8Items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * EQUIP-1 (modelos do Miguel de 2026-11-05): armaduras Numbers 4 e 6 novas para o jogador (com a aura de cada
 * personagem, fabricadas na bancada e liberadas pela pesquisa das armas numeradas) e katana longa do Soshiro e do
 * Soichiro (arma de lamina com especial, no tamanho real). A malha 3D (capacete, cauda, asas, manoplas) e so do
 * cliente: conferida em jogo.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EquipGameTests {

    private EquipGameTests() {
    }

    @GameTest(template = "empty_3x3")
    public static void numberedArmorsAreCraftableSuitsWithTheirAura(GameTestHelper helper) {
        List<Item> suits = List.of(KN8Items.NUMBERS4_SUIT.get(), KN8Items.NUMBERS6_SUIT.get(),
                KN8Items.NUMBERS10_SUIT.get());
        List<String> auras = List.of("kikoru_no4_wind", "reno_no6_ice", "hoshina_no10_sync");
        var research = KN8Data.RESEARCH.server().get(KN8Constants.id("numbered_weapon_theory"));
        helper.assertTrue(research != null, "Sem a pesquisa das armas numeradas");
        for (int i = 0; i < suits.size(); i++) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(suits.get(i));
            SuitDef suit = KN8Data.SUIT.server().get(id);
            helper.assertTrue(suit != null && suit.armor() > 0, "Traje sem dados: " + id);
            helper.assertTrue(suit.aura().map(KN8Constants.id(auras.get(i))::equals).orElse(false),
                    "Aura do traje " + id + ": " + suit.aura());
            helper.assertTrue(KN8Data.AURA.server().containsKey(suit.aura().get()), "Aura inexistente: " + id);
            helper.assertTrue(KN8Data.WORKBENCH.server().values().stream().anyMatch(r -> r.result().equals(id)),
                    "Sem receita na bancada: " + id);
            helper.assertTrue(research.unlocks().contains(id), "Pesquisa nao libera " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty_3x3")
    public static void katanaIsARealSizeBladeWithSpecial(GameTestHelper helper) {
        ResourceLocation id = KN8Constants.id("katana");
        WeaponDef katana = KN8Data.WEAPON.server().get(id);
        helper.assertTrue(katana != null, "Katana sem dados");
        helper.assertTrue(katana.style() == WeaponDef.Style.BLADE && katana.special().isPresent()
                && katana.profile().map(KN8Constants.id("sword")::equals).orElse(false), "Katana: " + katana);
        WeaponDef sword = KN8Data.WEAPON.server().get(KN8Constants.id("sword"));
        helper.assertTrue(katana.baseDamage() > sword.baseDamage() && katana.reach() > sword.reach(),
                "Katana mais longa e forte que a espada comum");
        helper.assertTrue(new ItemStack(KN8Items.KATANA.get()).is(
                TagKey.create(Registries.ITEM, KN8Constants.id("real_size_weapons"))), "Katana fora do tamanho real");
        helper.assertTrue(KN8Data.WORKBENCH.server().values().stream().anyMatch(r -> r.result().equals(id)),
                "Katana sem receita");
        helper.assertTrue(KN8Data.RANK.server().get(KN8Constants.id("vice_capitao")).unlocks().contains(id),
                "Katana sem patente");
        helper.succeed();
    }
}
