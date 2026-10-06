// src/main/java/com/kn8/client/combat/CombatFeedback.java
package com.kn8.client.combat;

import java.util.Locale;

import com.kn8.common.combat.CombatAction;
import com.kn8.common.combat.CombatResult;
import com.kn8.common.combat.CombatStateS2C;
import com.kn8.common.network.KN8ClientHooks;

import net.minecraft.network.chat.Component;

/**
 * Ultima resposta de combate recebida (M10b), para a HUD: passo do combo e um aviso curto ("PARRY!", "CRITICO!",
 * "sem stamina", "guarda quebrada"). Estado so de exibicao deste cliente; o servidor e quem decide tudo.
 */
public final class CombatFeedback {

    private static final long MESSAGE_MILLIS = 1200;
    private static final long COMBO_MILLIS = 1500;

    private static CombatResult lastResult = CombatResult.OK;
    private static long lastResultAt;
    private static int comboStep = -1;
    private static int comboLength;
    private static long comboAt;

    private CombatFeedback() {
    }

    /** Chamado no setup do cliente. */
    public static void install() {
        KN8ClientHooks.register(CombatStateS2C.TYPE, CombatFeedback::onState);
    }

    private static void onState(CombatStateS2C payload) {
        long now = System.currentTimeMillis();
        CombatResult result = CombatResult.byIndex(payload.result());
        if (payload.action() == CombatAction.LIGHT.ordinal() && payload.comboStep() >= 0) {
            comboStep = payload.comboStep();
            comboLength = payload.comboLength();
            comboAt = now;
        }
        if (result != CombatResult.OK) {
            lastResult = result;
            lastResultAt = now;
        }
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
