// src/main/java/com/kn8/common/numbered/No10Service.java
package com.kn8.common.numbered;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.NumberedDef;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.vfx.VfxService;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Comportamento extra do Kaiju No. 10 (0.6-E; especificacao do Miguel secao 4): regeneracao (50% / 20%), comando
 * dos kaiju por perto (os Preondactyl atacam o alvo dele) e a forma gigante. Numeros no {@code numbered/<id>.json}.
 */
public final class No10Service {

    private static final int THINK_INTERVAL = 10;
    /** Sem alvo por este tempo, a contagem da batalha recomeca (outra luta). */
    private static final int COMBAT_RESET_TICKS = 200;
    private static final double ANNOUNCE_RADIUS = 128.0;
    private static final int DESTRUCTION_POWER = 4;

    private No10Service() {
    }

    static void tick(KaijuNo10Entity no10) {
        Optional<NumberedDef> found = No9Service.def(no10);
        if (found.isEmpty() || !(no10.level() instanceof ServerLevel level)) {
            return;
        }
        NumberedDef def = found.get();
        No9Service.regenerate(level, no10, def.regeneration());
        LivingEntity target = no10.getTarget();
        if (target != null && target.isAlive()) {
            no10.combatTicks++;
            no10.idleTicks = 0;
        } else if (++no10.idleTicks > COMBAT_RESET_TICKS) {
            no10.combatTicks = 0;
        }
        if (no10.tickCount % THINK_INTERVAL == 0) {
            No9Service.command(level, no10, def.commandRadius());
        }
        def.transform().filter(transform -> !no10.transformed && shouldTransform(no10, transform))
                .ifPresent(transform -> transform(level, no10, transform));
    }

    static boolean shouldTransform(KaijuNo10Entity no10, NumberedDef.Transform transform) {
        if (no10.isUsingAbility()) {
            return false;
        }
        return no10.combatTicks >= transform.afterCombatTicks()
                || no10.getHealth() < no10.getMaxHealth() * transform.healthBelow();
    }

    /**
     * Forma gigante: a especie {@code into} surge no lugar (mesma direcao e alvo, vida pela fracao do JSON), quebra
     * o que estiver em volta e a pequena some. Na invasao, a nova toma o lugar da antiga (nao conta como abate).
     */
    public static Optional<KaijuEntity> transform(ServerLevel level, KaijuEntity no10,
            NumberedDef.Transform transform) {
        // 0.7-E: serve tambem as formas do No. 9 (preta, fundidas); a forma nova nasce ja "transformada".
        markTransformed(no10);
        Optional<KaijuEntity> spawned = KaijuSpawner.spawn(level, transform.into(), no10.blockPosition());
        if (spawned.isEmpty()) {
            KN8Constants.LOGGER.warn("[kn8] Numerado: especie da forma nova inexistente {}", transform.into());
            return spawned;
        }
        KaijuEntity giant = spawned.get();
        giant.setYRot(no10.getYRot());
        giant.setYBodyRot(no10.yBodyRot);
        giant.setHealth(giant.getMaxHealth() * transform.healthFraction());
        if (no10.getTarget() != null) {
            giant.setTarget(no10.getTarget());
        }
        markTransformed(giant);
        Vec3 at = no10.position();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 2.0, at.z, 3, 2.0, 2.0, 2.0, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 4.0, at.z, 80, 4.0, 6.0, 4.0, 0.05);
        VfxService.play(level, VfxService.SHOCKWAVE, at, Vec3.ZERO, 3.0F, 0.8F);
        VfxService.play(level, VfxService.ROAR, at.add(0, giant.getBbHeight() * 0.8, 0), giant.getLookAngle(), 3.0F,
                0.0F);
        level.playSound(null, no10.blockPosition(), KN8Sounds.KAIJU_ROAR.get(), SoundSource.HOSTILE, 8.0F, 0.4F);
        if (transform.destructionRadius() > 0) {
            DestructionService.request(level, at, new AbilityDef.Destruction(transform.destructionRadius(),
                    DESTRUCTION_POWER, false, 0.0F), giant);
        }
        Component message = Component.translatable(transform.message()).withStyle(ChatFormatting.DARK_RED,
                ChatFormatting.BOLD);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                no10.getBoundingBox().inflate(ANNOUNCE_RADIUS))) {
            player.sendSystemMessage(message);
        }
        if (no10.getPersistentData().getBoolean(InvasionService.TAG)) {
            InvasionService.release(level, no10.getUUID());
            InvasionService.join(level, giant);
        }
        no10.discard();
        return spawned;
    }

    private static void markTransformed(KaijuEntity kaiju) {
        if (kaiju instanceof KaijuNo10Entity no10) {
            no10.transformed = true;
        } else if (kaiju instanceof KaijuNo9Entity no9) {
            no9.transformed = true;
        }
    }
}
