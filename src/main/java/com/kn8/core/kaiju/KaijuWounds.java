package com.kn8.core.kaiju;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * PM-2 (Plano Mestre, secao 4: ferimentos persistentes): dano acumulado por parte do corpo de um kaiju. Ao passar do
 * limiar (fracao da vida maxima) a parte fica ferida por um tempo; ferida, nao acumula mais (o efeito nao cresce a
 * cada golpe) e, ao sarar, a conta recomeca do zero. Teto de partes feridas ao mesmo tempo: com o teto cheio o dano
 * da parte fica parado no limiar ate outra sarar. So contas, sem Minecraft (JUnit).
 */
public final class KaijuWounds {

    private final Map<String, Float> accumulated = new HashMap<>();
    private final Map<String, Long> woundedUntil = new HashMap<>();

    /**
     * Dano {@code damage} na parte {@code part}. Devolve true quando o ferimento abre agora. {@code threshold} menor
     * ou igual a 0 desliga a parte.
     */
    public boolean hit(String part, float damage, float maxHealth, float threshold, int durationTicks, int maxActive,
            long now) {
        if (threshold <= 0.0F || damage <= 0.0F || durationTicks <= 0) {
            return false;
        }
        expire(now);
        if (isWounded(part, now)) {
            return false;
        }
        float limit = threshold * Math.max(1.0F, maxHealth);
        float total = accumulated.getOrDefault(part, 0.0F) + damage;
        if (total < limit) {
            accumulated.put(part, total);
            return false;
        }
        if (active(now) >= maxActive) {
            accumulated.put(part, limit);
            return false;
        }
        accumulated.remove(part);
        woundedUntil.put(part, now + durationTicks);
        return true;
    }

    public boolean isWounded(String part, long now) {
        return woundedUntil.getOrDefault(part, Long.MIN_VALUE) > now;
    }

    /** Partes feridas agora (em ordem, para o log e a barra). */
    public Set<String> wounded(long now) {
        Set<String> parts = new TreeSet<>();
        woundedUntil.forEach((part, until) -> {
            if (until > now) {
                parts.add(part);
            }
        });
        return parts;
    }

    public int active(long now) {
        return wounded(now).size();
    }

    /** Tira os ferimentos que ja sararam; devolve as partes saradas. */
    public List<String> expire(long now) {
        List<String> healed = new ArrayList<>();
        woundedUntil.entrySet().removeIf(entry -> {
            if (entry.getValue() <= now) {
                healed.add(entry.getKey());
                return true;
            }
            return false;
        });
        return healed;
    }

    public float accumulated(String part) {
        return accumulated.getOrDefault(part, 0.0F);
    }

    public long until(String part) {
        return woundedUntil.getOrDefault(part, Long.MIN_VALUE);
    }

    public Map<String, Float> accumulatedAll() {
        return Map.copyOf(accumulated);
    }

    /** Volta o estado salvo no mundo. */
    public void restore(String part, float damage, long until) {
        if (damage > 0.0F) {
            accumulated.put(part, damage);
        }
        if (until != Long.MIN_VALUE) {
            woundedUntil.put(part, until);
        }
    }

    public void clear() {
        accumulated.clear();
        woundedUntil.clear();
    }

    /** Velocidade com varios ferimentos: o mais forte vale, sem multiplicar um pelo outro. */
    public static float speedMultiplier(Collection<Float> factors) {
        float result = 1.0F;
        for (float factor : factors) {
            result = Math.min(result, factor);
        }
        return result;
    }

    /** Atordoamento com varios ferimentos: o maior multiplicador vale (nunca menor que 1). */
    public static float staggerMultiplier(Collection<Float> factors) {
        float result = 1.0F;
        for (float factor : factors) {
            result = Math.max(result, factor);
        }
        return result;
    }
}
