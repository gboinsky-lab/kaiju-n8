// src/main/java/com/kn8/common/combat/CombatEvents.java
package com.kn8.common.combat;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.core.combat.CombatMath;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Liga o combate do jogador aos eventos do jogo (game bus, servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class CombatEvents {

    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()) {
            CombatService.tick(player);
        }
    }

    /** Com uma arma do kn8 na mao, o golpe e o do kn8: o ataque vanilla (clique em entidade) nao acontece. */
    @SubscribeEvent
    public static void onVanillaAttack(AttackEntityEvent event) {
        if (CombatService.heldWeapon(event.getEntity()).isPresent()) {
            event.setCanceled(true);
        }
    }

    /**
     * Esquiva (invulneravel no inicio), parry (bloqueio recem-iniciado: sem dano, devolve stamina, abre critico) e
     * bloqueio frontal (reduz o dano, custa stamina; sem stamina a guarda quebra).
     * Prioridade alta: roda antes da reducao pela % liberada (M5), que entao se aplica ao que sobrou.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ServerConfig.SPEC.isLoaded()) {
            return;
        }
        if (CombatService.isInvulnerable(player)) {
            event.setCanceled(true);
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!CombatService.isBlocking(player) || attacker == null) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 toAttacker = attacker.position().subtract(player.position());
        if (!CombatMath.inFront(look.x, look.z, toAttacker.x, toAttacker.z)) {
            return;
        }
        if (CombatService.isParry(player)) {
            // Parry (M10b): bloqueio comecou ate poucos ticks antes do golpe -> sem dano nenhum.
            event.setCanceled(true);
            CombatService.parry(player, attacker);
            return;
        }
        if (attacker instanceof KaijuEntity kaiju && kaiju.isDealingHeavyHit()
                && ServerConfig.HEAVY_IGNORES_BLOCK.get()) {
            // Ataque pesado de kaiju (slam, investida): o bloqueio comum nao segura; so parry ou esquiva.
            return;
        }
        CombatMath.Block block = CombatMath.block(event.getAmount(), ServerConfig.BLOCK_DAMAGE_REDUCTION.get(),
                ServerConfig.BLOCK_STAMINA_PER_DAMAGE.get());
        if (PowerService.tryConsumeStamina(player, block.staminaCost())) {
            event.setAmount(block.damageTaken());
        } else {
            CombatService.breakGuard(player);
        }
    }
}
