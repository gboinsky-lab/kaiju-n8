package com.kn8.common.registry;

import java.util.EnumMap;
import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Materiais dos trajes (0.2, Etapa 3). Defesa 0 de proposito: armadura e resistencia vem do {@code suit/*.json}
 * (regra 2: numeros em dados), aplicadas por {@code SuitEvents} no lugar dos atributos padrao do item.
 */
public final class KN8ArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, KN8Constants.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRAINING_SUIT = suit("training_suit");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MK1 = suit("mk1");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MK1_REINFORCED = suit("mk1_reinforced");

    private static final int ENCHANTABILITY = 10;

    private KN8ArmorMaterials() {
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> suit(String name) {
        return MATERIALS.register(name, () -> {
            EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                defense.put(type, 0);
            }
            return new ArmorMaterial(defense, ENCHANTABILITY, SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(KN8Items.KAIJU_TISSUE.get()),
                    List.of(new ArmorMaterial.Layer(KN8Constants.id(name))), 0.0F, 0.0F);
        });
    }
}
