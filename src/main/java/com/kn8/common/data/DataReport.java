// src/main/java/com/kn8/common/data/DataReport.java
package com.kn8.common.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado da ultima validacao cruzada: quantas definicoes de cada tipo ficaram, erros (entrada removida) e
 * avisos (entrada mantida, mas com algo que ainda nao existe, ex.: item que chega em modulo futuro).
 */
public final class DataReport {

    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    void count(String registry, int amount) {
        counts.put(registry, amount);
    }

    void error(String message) {
        errors.add(message);
    }

    void warning(String message) {
        warnings.add(message);
    }

    public Map<String, Integer> counts() {
        return Map.copyOf(counts);
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    /** Linha de resumo para log e comando. */
    public String summary() {
        StringBuilder text = new StringBuilder();
        counts.forEach((name, amount) -> text.append(amount).append(' ').append(name).append(", "));
        text.append(errors.size()).append(" erro(s), ").append(warnings.size()).append(" aviso(s)");
        return text.toString();
    }
}
