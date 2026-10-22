package com.kn8.core.vfx;

/**
 * Tamanho do efeito pelo tamanho de quem o faz (Miguel, 2026-10-22: "um kaiju grande nao deve ter ataques pequenos ou
 * com efeitos pequenos"). As receitas e os efeitos comuns sao feitos para um corpo da altura de referencia (um
 * humano, {@code [fx] referenceHeight}); o fator do corpo e altura / referencia, preso entre {@code MIN} e
 * {@code MAX}. Tamanho, alcance e quantidade crescem por expoentes diferentes (config): a quantidade cresce menos
 * para o efeito grande nao virar uma nuvem que esconde o combate (diretriz §6).
 */
public final class VfxScale {

    public static final float MIN = 0.5F;
    public static final float MAX = 14.0F;

    private VfxScale() {
    }

    /** Fator do corpo: altura / referencia, entre MIN e MAX; altura desconhecida (0) = 1. */
    public static float body(float height, float referenceHeight) {
        if (height <= 0.0F || referenceHeight <= 0.0F) {
            return 1.0F;
        }
        return Math.max(MIN, Math.min(MAX, height / referenceHeight));
    }

    /** Multiplicador de uma grandeza (tamanho, alcance, quantidade) pelo fator do corpo e o expoente dela. */
    public static float factor(float body, double exponent) {
        return (float) Math.pow(body, exponent);
    }
}
