// src/main/java/com/kn8/client/vfx/PaletteLayers.java
package com.kn8.client.vfx;

import com.kn8.common.data.def.VfxProfileDef;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.vfx.VfxBudget;
import com.kn8.core.vfx.VfxPatterns.Pattern;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * VFX-1: camada da paleta e do material por cima dos efeitos genericos, enquanto a especie/personagem nao tem a
 * receita propria (VFX-2 a VFX-9). Primeiro ganho da diretriz para todos: a mordida da aranha solta lascas pretas e
 * amarelas de quitina, a do Primigenius poeira clara do couro, a do cogumelo esporos; o raio do Honju brilha
 * dourado e o do revivido roxo. As formas mudam pelo material, nao so a cor.
 */
final class PaletteLayers {

    /**
     * Pedacos do material maiores e mais numerosos que as particulas vanilla (conferido em jogo: com o tamanho das
     * vanilla a cor da especie sumia no meio do branco do impacto).
     */
    private static final float MATERIAL_SIZE = 2.0F;
    private static final float MATERIAL_COUNT = 1.5F;

    private PaletteLayers() {
    }

    static void play(ClientLevel level, ResourceLocation effect, VfxProfileDef profile, Vec3 at, Vec3 direction,
            float intensity, KaijuVfx.Body body, VfxBudget budget) {
        Vec3 flat = KaijuVfx.horizontal(direction);
        float size = (float) Math.sqrt(Math.max(0.1F, intensity));
        if (effect.equals(VfxService.IMPACT)) {
            material(level, profile, body, at, flat, intensity, 1.0F, budget);
        } else if (effect.equals(VfxService.SHOCKWAVE)) {
            layer(level, profile, body, "ring", "accent", Pattern.BURST, 2, 2.6F * size, 0.0, 0.0,
                    14, at.add(0, 0.15, 0), flat, budget);
            material(level, profile, body, at, flat, intensity, 1.5F, budget);
        } else if (effect.equals(VfxService.DUST) || effect.equals(VfxService.GROUND_CRACK)) {
            layer(level, profile, body, "ring", "primary", Pattern.BURST, 1, 1.6F * size, 0.0, 0.0,
                    12, at.add(0, 0.1, 0), flat, budget);
            material(level, profile, body, at, flat, intensity, 0.7F, budget);
        } else if (effect.equals(VfxService.SLASH)) {
            layer(level, profile, body, "arc", "accent", Pattern.ARC, 5, 0.35F * size, 0.15,
                    0.9 * size, 5, at, flat, budget);
            material(level, profile, body, at, flat, intensity, 0.5F, budget);
        } else if (effect.equals(VfxService.ROAR)) {
            layer(level, profile, body, "streak", "secondary", Pattern.LINE, 8, 0.3F * size, 0.8,
                    2.0 * size, 8, at, flat, budget);
        } else if (effect.equals(VfxService.ENERGY_CHARGE)) {
            layer(level, profile, body, "glow", "core", Pattern.CONVERGE, 10, 0.25F, 0.1,
                    2.5 * intensity, 18, at, flat, budget);
            layer(level, profile, body, "glow", "accent", Pattern.CONVERGE, 8, 0.35F, 0.08,
                    2.5 * intensity, 22, at, flat, budget);
        } else if (effect.equals(VfxService.ENERGY_BURST)) {
            layer(level, profile, body, "glow", "core", Pattern.BURST, 8, 0.5F * size, 0.35, 0.6, 8,
                    at, flat, budget);
            layer(level, profile, body, "ring", "accent", Pattern.BURST, 2, 1.8F * size, 0.0, 0.0, 10,
                    at, flat, budget);
            material(level, profile, body, at, flat, intensity, 1.0F, budget);
        } else if (effect.equals(VfxService.WEAPON_FIRE)) {
            layer(level, profile, body, "glow", "core", Pattern.BURST, 1, 0.3F, 0.0, 0.0, 3, at, flat,
                    budget);
        }
    }

    /** Pedacos do corpo pelo material: quitina lasca, fungo solta esporo, revivido solta vapor, energia brilha. */
    private static void material(ClientLevel level, VfxProfileDef profile, KaijuVfx.Body body, Vec3 at, Vec3 flat,
            float intensity, float amount, VfxBudget budget) {
        float k = intensity * amount * MATERIAL_COUNT;
        float size = (float) Math.sqrt(Math.max(0.1F, intensity)) * MATERIAL_SIZE;
        switch (profile.material()) {
            case "chitin" -> {
                layer(level, profile, body, "shard", "primary", Pattern.BURST, Math.round(6 * k),
                        0.14F * size, 0.35, 0.5, 20, at, flat, budget);
                layer(level, profile, body, "shard", "accent", Pattern.BURST, Math.round(3 * k),
                        0.12F * size, 0.35, 0.5, 20, at, flat, budget);
            }
            case "fungus" -> {
                layer(level, profile, body, "spore", "secondary", Pattern.RISE, Math.round(8 * k),
                        0.3F * size, 0.03, 1.2 * size, 40, at, flat, budget);
                layer(level, profile, body, "spore", "accent", Pattern.RISE, Math.round(3 * k),
                        0.22F * size, 0.04, 1.0 * size, 40, at, flat, budget);
            }
            case "revived" -> {
                layer(level, profile, body, "spore", "accent", Pattern.RISE, Math.round(5 * k),
                        0.4F * size, 0.05, 1.0 * size, 30, at, flat, budget);
                layer(level, profile, body, "shard", "primary", Pattern.BURST, Math.round(3 * k),
                        0.14F * size, 0.3, 0.5, 20, at, flat, budget);
            }
            case "energy" -> {
                layer(level, profile, body, "glow", "accent", Pattern.BURST, Math.round(5 * k),
                        0.25F * size, 0.25, 0.4, 10, at, flat, budget);
                layer(level, profile, body, "glow", "core", Pattern.BURST, Math.round(2 * k),
                        0.18F * size, 0.15, 0.2, 8, at, flat, budget);
            }
            case "ice" -> {
                layer(level, profile, body, "shard", "core", Pattern.BURST, Math.round(6 * k),
                        0.12F * size, 0.3, 0.4, 18, at, flat, budget);
                layer(level, profile, body, "spore", "primary", Pattern.RISE, Math.round(4 * k),
                        0.35F * size, 0.02, 0.8 * size, 24, at, flat, budget);
            }
            case "suit", "metal" -> {
                layer(level, profile, body, "streak", "core", Pattern.BURST, Math.round(3 * k),
                        0.08F, 0.4, 0.2, 4, at, flat, budget);
                layer(level, profile, body, "shard", "secondary", Pattern.BURST, Math.round(2 * k),
                        0.08F, 0.25, 0.3, 12, at, flat, budget);
            }
            default -> {
                // Carne, couro, escamas: poeira da cor do corpo e alguns pedacos do destaque.
                layer(level, profile, body, "spore", "primary", Pattern.BURST, Math.round(4 * k),
                        0.35F * size, 0.08, 0.6, 16, at, flat, budget);
                layer(level, profile, body, "shard", "secondary", Pattern.BURST, Math.round(3 * k),
                        0.12F * size, 0.3, 0.5, 18, at, flat, budget);
            }
        }
    }

    /** VFX-2: tamanho, quantidade e alcance crescem com o corpo de quem fez o efeito. */
    private static void layer(ClientLevel level, VfxProfileDef profile, KaijuVfx.Body body, String type,
            String color, Pattern pattern, int count, float size, double speed, double spread, int lifetime, Vec3 at,
            Vec3 flat, VfxBudget budget) {
        int rgb = profile.palette().resolve(color);
        if (rgb >= 0 && count > 0) {
            KaijuVfx.emit(level, type, rgb, pattern, Math.round(count * body.count()), size * body.size(),
                    speed * Math.min(body.spread(), 3.0F), spread * body.spread(), lifetime, at, flat, budget);
        }
    }
}
