// src/main/java/com/kn8/common/combat/WeaponIndex.java
package com.kn8.common.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.WeaponDef;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Item na mao -> definicao da arma (campo {@code item} do JSON). Reconstruido quando a foto de dados muda
 * ({@code /reload}); cada lado (servidor/cliente) tem o seu, porque cada um tem a sua foto.
 */
public final class WeaponIndex {

    private static final WeaponIndex SERVER = new WeaponIndex(false);
    private static final WeaponIndex CLIENT = new WeaponIndex(true);

    private final boolean clientSide;
    private int version = -1;
    private Map<ResourceLocation, WeaponDef> byItem = Map.of();

    private WeaponIndex(boolean clientSide) {
        this.clientSide = clientSide;
    }

    public static Optional<WeaponDef> find(ItemStack stack, boolean clientSide) {
        return (clientSide ? CLIENT : SERVER).lookup(stack);
    }

    private synchronized Optional<WeaponDef> lookup(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        int current = KN8Data.WEAPON.version(clientSide);
        if (current != version) {
            Map<ResourceLocation, WeaponDef> rebuilt = new HashMap<>();
            KN8Data.WEAPON.forSide(clientSide).values().forEach(def -> rebuilt.put(def.item(), def));
            byItem = rebuilt;
            version = current;
        }
        return Optional.ofNullable(byItem.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }
}
