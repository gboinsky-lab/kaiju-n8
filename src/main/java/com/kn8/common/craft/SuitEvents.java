package com.kn8.common.craft;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SuitDef;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/** Trajes (0.2, Etapa 3): armadura e resistencia do traje lidas do {@code suit/<id>.json} (id = id do item). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class SuitEvents {

    private static final ResourceLocation ARMOR = KN8Constants.id("suit_armor");
    private static final ResourceLocation TOUGHNESS = KN8Constants.id("suit_toughness");

    private SuitEvents() {
    }

    /** Definicao do traje deste item (dados do lado certo: o tooltip do cliente tambem usa). */
    public static Optional<SuitDef> suit(ItemStack stack, boolean clientSide) {
        if (!(stack.getItem() instanceof ArmorItem)) {
            return Optional.empty();
        }
        return KN8Data.SUIT.get(BuiltInRegistries.ITEM.getKey(stack.getItem()), clientSide);
    }

    /** Traje vestido no peito (ou vazio). */
    public static Optional<SuitDef> worn(LivingEntity entity) {
        return suit(entity.getItemBySlot(EquipmentSlot.CHEST), entity.level().isClientSide());
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        // Este evento roda nos dois lados; os dados de traje sao sincronizados, entao basta tentar os dois.
        Optional<SuitDef> def = suit(event.getItemStack(), false).or(() -> suit(event.getItemStack(), true));
        def.ifPresent(suit -> {
            event.addModifier(Attributes.ARMOR, new AttributeModifier(ARMOR, suit.armor(),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
            if (suit.toughness() > 0) {
                event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS, suit.toughness(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
            }
        });
    }
}
