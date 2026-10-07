// src/main/java/com/kn8/common/vfx/VfxService.java
package com.kn8.common.vfx;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sistema central de efeitos visuais (Etapa D). Ninguem desenha efeito sozinho: entidades, armas e habilidades
 * chamam {@link #play}; o cliente tem o registro de efeitos (particulas). Ids padrao abaixo.
 */
public final class VfxService {

    public static final ResourceLocation IMPACT = KN8Constants.id("impact");
    public static final ResourceLocation SHOCKWAVE = KN8Constants.id("shockwave");
    public static final ResourceLocation DUST = KN8Constants.id("dust");
    public static final ResourceLocation SLASH = KN8Constants.id("slash");
    public static final ResourceLocation WEAPON_FIRE = KN8Constants.id("weapon_fire");
    public static final ResourceLocation ROAR = KN8Constants.id("roar");
    public static final ResourceLocation SUIT_RELEASE = KN8Constants.id("suit_release");
    public static final ResourceLocation OVERHEAT = KN8Constants.id("overheat");
    /** 0.5: rachaduras no chao saindo do ponto de impacto (ataque especial do machado). */
    public static final ResourceLocation GROUND_CRACK = KN8Constants.id("ground_crack");

    /** Distancia em que os jogadores recebem o efeito. */
    public static final double RANGE = 64.0;

    private VfxService() {
    }

    public static void play(ServerLevel level, ResourceLocation effect, Vec3 position, Vec3 direction,
            float intensity, float shake) {
        if (!ServerConfig.SPEC.isLoaded() || !ServerConfig.VFX_ENABLED.get()) {
            return;
        }
        PacketDistributor.sendToPlayersNear(level, null, position.x, position.y, position.z, RANGE,
                new VfxS2C(effect, position, direction, intensity, shake));
    }

    public static void play(ServerLevel level, ResourceLocation effect, Vec3 position, float intensity) {
        play(level, effect, position, Vec3.ZERO, intensity, 0.0F);
    }
}
