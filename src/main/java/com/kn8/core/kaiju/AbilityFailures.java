package com.kn8.core.kaiju;

import java.util.HashMap;
import java.util.Map;

/**
 * MOV-6 (plano de mobilidade v3, bug 8: "sem contador de falhas repetidas"): conta as falhas seguidas de cada
 * habilidade de um kaiju. Falha = o golpe saiu e nao feriu ninguem (alvo fora do alcance, esquivou, tiro no vazio);
 * golpe barrado por parede nao conta (o golpe quebra a parede: e assim que o kaiju abre caminho). Ao chegar ao limite
 * a contagem zera e quem chama poe a habilidade de descanso.
 */
public final class AbilityFailures {

    private final Map<String, Integer> streaks = new HashMap<>();

    /**
     * Registra o resultado de um uso. Acerto zera a contagem. Devolve true quando as falhas seguidas chegam a
     * {@code limit} (a contagem zera junto); {@code limit} menor que 1 desliga.
     */
    public boolean record(String ability, boolean failed, int limit) {
        if (!failed || limit < 1) {
            streaks.remove(ability);
            return false;
        }
        int streak = streaks.merge(ability, 1, Integer::sum);
        if (streak >= limit) {
            streaks.remove(ability);
            return true;
        }
        return false;
    }

    /** Falhas seguidas da habilidade agora. */
    public int streak(String ability) {
        return streaks.getOrDefault(ability, 0);
    }

    public void clear() {
        streaks.clear();
    }
}
