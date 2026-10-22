package com.kn8.core.vfx;

/**
 * Teto de particulas de um efeito (diretriz KaijuVFX §6: "limites de particulas"): a quantidade pedida vezes o fator
 * de qualidade do cliente ({@code [fx] particleLevel}), nunca acima do que sobra do teto por efeito
 * ({@code [fx] maxParticlesPerEffect}). Cada efeito tem o seu; as camadas gastam do mesmo.
 */
public final class VfxBudget {

    private int left;

    public VfxBudget(int max) {
        this.left = Math.max(0, max);
    }

    /** Quantas particulas a camada pode soltar: pelo menos 1 se pediu alguma e ainda ha teto. */
    public int take(int requested, float quality) {
        if (requested <= 0 || left <= 0) {
            return 0;
        }
        int scaled = Math.max(1, Math.round(requested * Math.max(0.0F, quality)));
        int granted = Math.min(scaled, left);
        left -= granted;
        return granted;
    }

    public int left() {
        return left;
    }
}
