// src/main/java/com/kn8/common/combat/WeaponItem.java
package com.kn8.common.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item de arma do kn8 (M10). Nao tem logica de combate propria: os numeros vem do JSON da arma
 * ({@code data/<ns>/kn8/weapon/*.json}, campo {@code item}) e as acoes passam pelo {@link CombatService} no servidor.
 * O ataque vanilla e cancelado enquanto a arma esta na mao (o golpe e o do kn8).
 */
public class WeaponItem extends Item {

    public WeaponItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Nao quebra blocos ao "atacar" com a arma (o clique esquerdo vira golpe leve). */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
