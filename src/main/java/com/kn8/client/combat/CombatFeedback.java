// src/main/java/com/kn8/client/combat/CombatFeedback.java
package com.kn8.client.combat;

import java.util.Locale;

import com.kn8.common.combat.AmmoS2C;
import com.kn8.common.combat.CombatAction;
import com.kn8.common.combat.CombatResult;
import com.kn8.common.combat.CombatStateS2C;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.core.combat.SpecialGeometry;

import net.minecraft.network.chat.Component;

/**
 * Ultima resposta de combate recebida (M10b), para a HUD: passo do combo e um aviso curto ("PARRY!", "CRITICO!",
 * "sem stamina", "guarda quebrada"). Estado so de exibicao deste cliente; o servidor e quem decide tudo.
 */
public final class CombatFeedback {

    private static final long MESSAGE_MILLIS = 1200;
    private static final long COMBO_MILLIS = 1500;
    private static final long MILLIS_PER_TICK = 50;

    private static CombatResult lastResult = CombatResult.OK;
    private static long lastResultAt;
    private static int comboStep = -1;
    private static int comboLength;
    private static long comboAt;
    /** 0.5: quando (relogio do cliente) o ataque especial fica pronto e a recarga total, vindos do servidor. */
    private static long specialReadyAt;
    private static long specialTotalMillis;
    /** 0.5.0-D: pente da arma de fogo na mao (magazine 0 = sem pente) e fim da recarga no relogio do cliente. */
    private static int rounds;
    private static int magazine;
    private static long reloadEndsAt;
    private static long reloadTotalMillis;

    private CombatFeedback() {
    }

    /** Chamado no setup do cliente. */
    public static void install() {
        KN8ClientHooks.register(CombatStateS2C.TYPE, CombatFeedback::onState);
        KN8ClientHooks.register(AmmoS2C.TYPE, CombatFeedback::onAmmo);
    }

    private static void onAmmo(AmmoS2C payload) {
        rounds = payload.rounds();
        magazine = payload.magazine();
        reloadTotalMillis = payload.reloadTotal() * MILLIS_PER_TICK;
        reloadEndsAt = System.currentTimeMillis() + payload.reloadLeft() * MILLIS_PER_TICK;
    }

    /** Tiros no pente e tamanho dele (0 = a arma na mao nao tem pente). */
    public static int rounds() {
        return rounds;
    }

    public static int magazine() {
        return magazine;
    }

    /** Fracao da recarga em andamento (0 a 1), ou -1 se nao esta recarregando. */
    public static float reloadProgress() {
        long remaining = reloadEndsAt - System.currentTimeMillis();
        if (remaining <= 0 || reloadTotalMillis <= 0) {
            return -1.0F;
        }
        return 1.0F - remaining / (float) reloadTotalMillis;
    }

    private static void onState(CombatStateS2C payload) {
        long now = System.currentTimeMillis();
        CombatResult result = CombatResult.byIndex(payload.result());
        if (payload.action() == CombatAction.LIGHT.ordinal() && payload.comboStep() >= 0) {
            comboStep = payload.comboStep();
            comboLength = payload.comboLength();
            comboAt = now;
        }
        if (payload.action() == CombatAction.LIGHT.ordinal()
                && (result == CombatResult.OK || result == CombatResult.SLOWED_NO_STAMINA)) {
            WeaponRecoil.onShot();
        }
        if (payload.action() == CombatAction.SPECIAL.ordinal()) {
            // Na acao SPECIAL os campos do combo levam a recarga em ticks (CombatStateS2C).
            specialReadyAt = now + payload.comboStep() * MILLIS_PER_TICK;
            specialTotalMillis = payload.comboLength() * MILLIS_PER_TICK;
        }
        if (result != CombatResult.OK) {
            lastResult = result;
            lastResultAt = now;
        }
    }

    /** Fracao da recarga do ataque especial (0 = acabou de usar, 1 = pronto). */
    public static float specialProgress() {
        long remaining = specialReadyAt - System.currentTimeMillis();
        return SpecialGeometry.cooldownProgress(remaining, specialTotalMillis);
    }

    /** Segundos que faltam para o especial (arredondado para cima; 0 = pronto). */
    public static int specialSecondsLeft() {
        long remaining = specialReadyAt - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) ((remaining + 999) / 1000);
    }

    /** Linha do combo ("Combo 2/3"), ou null fora da janela. */
    public static Component comboLine() {
        if (comboLength <= 1 || comboStep < 0 || System.currentTimeMillis() - comboAt > COMBO_MILLIS) {
            return null;
        }
        return Component.translatable("kn8.hud.combo", comboStep + 1, comboLength);
    }

    /** Aviso curto do ultimo evento, ou null se ja expirou. */
    public static Component messageLine() {
        if (System.currentTimeMillis() - lastResultAt > MESSAGE_MILLIS) {
            return null;
        }
        return Component.translatable("kn8.hud.combat." + lastResult.name().toLowerCase(Locale.ROOT));
    }

    /** Cor do aviso: vermelho para negado/quebra, dourado para parry/critico. */
    public static int messageColor() {
        return switch (lastResult) {
            case PARRY, CRITICAL -> 0xFFFFD54F;
            default -> 0xFFFF6E6E;
        };
    }
}
