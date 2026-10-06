// src/main/java/com/kn8/core/KN8Ids.java
package com.kn8.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Regras de nomes do projeto em Java puro (sem classes do Minecraft), para serem testadas com JUnit.
 *
 * <p>Por que existe no M1: os ids de JSON (M4) e os nomes de animacao (M9) seguem a convencao da Fase 4, secao 2.5.
 * Validar isso num unico lugar evita erros silenciosos, porque o Minecraft rejeita caminhos com maiusculas.</p>
 */
public final class KN8Ids {

    // Mesmo conjunto de caracteres aceito pelo Minecraft no caminho de um ResourceLocation.
    private static final Pattern RESOURCE_PATH = Pattern.compile("[a-z0-9_./-]+");
    // Cada parte do nome de animacao: minusculas, digitos e sublinhado, sem ponto.
    private static final Pattern ANIMATION_PART = Pattern.compile("[a-z0-9_]+");

    private KN8Ids() {
    }

    /** true se {@code path} pode ser usado como caminho de ResourceLocation. */
    public static boolean isValidResourcePath(String path) {
        return path != null && !path.isEmpty() && RESOURCE_PATH.matcher(path).matches();
    }

    /**
     * Monta o nome de animacao na convencao {@code <entidade>.<camada>.<nome>}, ex.: {@code primigenius.action.slam}.
     *
     * @throws IllegalArgumentException se alguma parte estiver vazia ou tiver caractere invalido
     */
    public static String animationName(String entity, String layer, String name) {
        requireAnimationPart(entity, "entity");
        requireAnimationPart(layer, "layer");
        requireAnimationPart(name, "name");
        return entity + "." + layer + "." + name;
    }

    private static void requireAnimationPart(String value, String field) {
        Objects.requireNonNull(value, field);
        if (!ANIMATION_PART.matcher(value).matches()) {
            throw new IllegalArgumentException("Parte de animacao invalida em '" + field + "': '" + value + "'");
        }
    }
}
