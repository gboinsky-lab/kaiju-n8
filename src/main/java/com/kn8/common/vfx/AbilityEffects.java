// src/main/java/com/kn8/common/vfx/AbilityEffects.java
package com.kn8.common.vfx;

import com.kn8.common.data.def.AbilityDef;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Efeitos de uma habilidade no tick de impacto (mesmo tick do dano): os {@code vfx} do JSON, o tremor de camera
 * ({@code camera_shake}, enviado junto com o primeiro efeito) e o som ({@code sound}).
 */
public final class AbilityEffects {

    private static final float SOUND_VOLUME = 2.0F;

    private AbilityEffects() {
    }

    public static void play(Entity source, AbilityDef ability, Vec3 where) {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }
        float yaw = source instanceof LivingEntity living ? living.yBodyRot
                : source.getYRot();
        Vec3 forward = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
        boolean first = true;
        for (AbilityDef.Vfx vfx : ability.vfx()) {
            VfxService.play(level, vfx.effect(), where, forward, vfx.intensity(),
                    first ? ability.cameraShake() : 0.0F);
            first = false;
        }
        ability.sound().flatMap(BuiltInRegistries.SOUND_EVENT::getOptional).ifPresent(sound ->
                level.playSound(null, where.x, where.y, where.z, sound, SoundSource.HOSTILE, SOUND_VOLUME, 1.0F));
    }
}
