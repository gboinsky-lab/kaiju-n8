// src/main/java/com/kn8/common/anim/AnimationBridge.java
package com.kn8.common.anim;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.core.KN8Ids;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ponte UNICA de animacao do lado do servidor (M9). Regra da Fase 4: animacao e consequencia do estado decidido no
 * servidor, nunca a causa; keyframes nao carregam logica.
 * <ul>
 *   <li>Jogadores: Player Animation Library (camada {@code kn8:combat}, prioridade 2000, provada no PT6), acionada
 *   por {@link AnimTriggerS2C} para o proprio jogador e para quem o ve.</li>
 *   <li>Kaiju: controllers GeckoLib (provados no PT3), acionados por {@code triggerAnim} do servidor.</li>
 * </ul>
 */
public final class AnimationBridge {

    /** Animacoes do jogador da 0.1 ({@code assets/kn8/player_animations/combat.json}). */
    public static final ResourceLocation PLAYER_LIGHT = player("light");
    public static final ResourceLocation PLAYER_HEAVY = player("heavy");
    public static final ResourceLocation PLAYER_BLOCK = player("block");
    public static final ResourceLocation PLAYER_DODGE = player("dodge");
    /** M10b: disparo do rifle (mira e coice). */
    public static final ResourceLocation PLAYER_SHOOT = player("shoot");
    /** 0.1-B: dash e postura de carga (segura ate soltar). */
    public static final ResourceLocation PLAYER_DASH = player("dash");
    public static final ResourceLocation PLAYER_CHARGE = player("charge");
    public static final List<ResourceLocation> PLAYER_ANIMATIONS =
            List.of(PLAYER_LIGHT, PLAYER_HEAVY, PLAYER_BLOCK, PLAYER_DODGE, PLAYER_SHOOT, PLAYER_DASH, PLAYER_CHARGE);

    private AnimationBridge() {
    }

    private static ResourceLocation player(String name) {
        return KN8Constants.id(KN8Ids.animationName("player", "action", name));
    }

    /**
     * Animacao do jogador para uma acao de uma arma: {@code kn8:player.<item>.<acao>} (ex.: player.sword.light).
     * Cada arma tem a sua, gerada a partir dos tempos do JSON dela (tools/art/gen_player_animations.py), para o pico
     * do golpe cair no tick de impacto daquela arma.
     */
    public static ResourceLocation weaponAction(ResourceLocation weaponItem, String action) {
        return KN8Constants.id(KN8Ids.animationName("player", weaponItem.getPath(), action));
    }

    /**
     * 0.5.0-D: animacao do perfil da familia da arma: {@code kn8:player.<perfil>.<acao>} (draw, guard, reload,
     * stance), gerada por tools/art/gen_player_animations.py a partir do {@code weapon_profile/<id>.json}.
     */
    public static ResourceLocation profileAction(ResourceLocation profile, String action) {
        return KN8Constants.id(KN8Ids.animationName("player", profile.getPath(), action));
    }

    /** Toca uma animacao PAL no jogador, para ele e para todos que o rastreiam. */
    public static void playPlayer(ServerPlayer player, ResourceLocation animation) {
        playPlayer(player, animation, 1.0F);
    }

    /** 0.5.0-D7: idem, na velocidade da acao no servidor (duracao do JSON / duracao real). */
    public static void playPlayer(ServerPlayer player, ResourceLocation animation, float speed) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new AnimTriggerS2C(player.getId(), animation, player.level().getGameTime(), false, speed));
    }

    /** Para a animacao da camada de combate (ex.: soltar o bloqueio). */
    public static void stopPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new AnimTriggerS2C(player.getId(), PLAYER_BLOCK, player.level().getGameTime(), true, 1.0F));
    }

    /** Dispara uma animacao GeckoLib num kaiju (controller + nome registrados na especie). */
    public static void playKaiju(KaijuEntity kaiju, String controller, String animation) {
        kaiju.triggerAnim(controller, animation);
    }
}
