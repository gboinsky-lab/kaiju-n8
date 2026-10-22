// src/main/java/com/kn8/common/vfx/VfxService.java
package com.kn8.common.vfx;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
    /** 0.6: aviso de raio de energia (particulas convergindo para a boca durante o preparo). */
    public static final ResourceLocation ENERGY_CHARGE = KN8Constants.id("energy_charge");
    /** 0.6: explosao de energia (impacto do raio). */
    public static final ResourceLocation ENERGY_BURST = KN8Constants.id("energy_burst");
    /** 0.6: teia grudando no alvo. */
    public static final ResourceLocation WEB = KN8Constants.id("web");

    /** VFX-2: tecnica de soldado especial (so desenha com a receita do perfil; sem receita, nada muda). */
    public static final ResourceLocation TECHNIQUE = KN8Constants.id("technique");
    /** VFX-2: troca de forma (Kafka <-> Kaiju No. 8); sem receita, fumaca e estouro. */
    public static final ResourceLocation TRANSFORM = KN8Constants.id("transform");
    /** VFX-2: regeneracao (pulso por segundo enquanto cura); sem receita, brilhos verdes. */
    public static final ResourceLocation REGENERATE = KN8Constants.id("regenerate");

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

    /**
     * VFX-1: efeito de alguem ({@code source}): o cliente pinta com a paleta do perfil visual dele e, se o perfil
     * tem receita para {@code key} (habilidade/tecnica; vazio = id do efeito), desenha a receita no lugar do generico.
     */
    public static void play(ServerLevel level, ResourceLocation effect, Vec3 position, Vec3 direction,
            float intensity, float shake, Entity source, String key) {
        if (!ServerConfig.SPEC.isLoaded() || !ServerConfig.VFX_ENABLED.get()) {
            return;
        }
        play(level, effect, position, direction, intensity, shake, profileOf(source), key,
                source == null ? 0.0F : source.getBbHeight());
    }

    /** Com o perfil visual dado direto (comando de previa, efeitos sem entidade). */
    public static void play(ServerLevel level, ResourceLocation effect, Vec3 position, Vec3 direction,
            float intensity, float shake, Optional<ResourceLocation> profile, String key, float height) {
        if (!ServerConfig.SPEC.isLoaded() || !ServerConfig.VFX_ENABLED.get()) {
            return;
        }
        PacketDistributor.sendToPlayersNear(level, null, position.x, position.y, position.z, RANGE,
                new VfxS2C(effect, position, direction, intensity, shake, new VfxS2C.Source(profile, key, height)));
    }

    public static void play(ServerLevel level, ResourceLocation effect, Vec3 position, Vec3 direction,
            float intensity, float shake, Entity source) {
        play(level, effect, position, direction, intensity, shake, source, "");
    }

    /**
     * Perfil visual de uma entidade: kaiju pela especie, soldado especial pelo perfil ativo (a forma muda o efeito),
     * soldado comum {@code kn8:soldier}, jogador {@code kn8:player}; o resto, sem perfil.
     */
    public static Optional<ResourceLocation> profileOf(Entity source) {
        if (source instanceof KaijuEntity kaiju) {
            return Optional.of(kaiju.kaijuId());
        }
        if (source instanceof HoshinaEntity special) {
            return Optional.of(special.activeProfileId());
        }
        if (source instanceof SoldierEntity) {
            return Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()));
        }
        if (source instanceof Player) {
            return Optional.of(KN8Constants.id("player"));
        }
        return Optional.empty();
    }

    public static void play(ServerLevel level, ResourceLocation effect, Vec3 position, float intensity) {
        play(level, effect, position, Vec3.ZERO, intensity, 0.0F);
    }
}
