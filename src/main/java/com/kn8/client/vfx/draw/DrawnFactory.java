// src/main/java/com/kn8/client/vfx/draw/DrawnFactory.java
package com.kn8.client.vfx.draw;

import com.kn8.common.config.ClientConfig;
import com.kn8.core.vfx.VfxColors;
import com.kn8.core.vfx.VfxPatterns;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * VFX-R1: transforma uma camada de receita com forma desenhada ({@code bolt}, {@code band}, {@code wave},
 * {@code pulse}, {@code beam}, {@code flash}, {@code smoke}) em efeitos do {@link DrawnRenderer}. Os numeros ja vem
 * multiplicados pela intensidade e pelo corpo de quem fez o efeito (VFX-2).
 */
public final class DrawnFactory {

    /** Claroes reduzidos ({@code [fx] reduceFlashes}): menores e sem branco puro. */
    private static final float FLASH_SIZE = 0.7F;

    private DrawnFactory() {
    }

    public static void spawn(String shape, int color, int edge, VfxPatterns.Pattern pattern, int count, float size,
            double speed, double spread, int lifetime, Vec3 origin, Vec3 flat, double tilt, RandomSource random) {
        boolean reduce = ClientConfig.REDUCE_FLASHES.get();
        int light = reduce ? VfxColors.shade(color, 0.15F) : VfxColors.shade(color, 0.6F);
        switch (shape) {
            case "bolt" -> {
                for (int i = 0; i < count; i++) {
                    Vec3 dir = direction(pattern, flat, random);
                    Vec3 start = pattern == VfxPatterns.Pattern.CONVERGE ? origin.subtract(dir.scale(spread)) : origin;
                    DrawnRenderer.add(new Drawn.Bolt(start, dir, Math.max(0.3, spread), Math.max(0.02, size), color,
                            light, lifetime, 0.4F, random));
                }
            }
            case "band" -> DrawnRenderer.add(new Drawn.Band(origin, flat, tilt, Math.max(0.2, spread),
                    Math.max(0.04, size), color, edge < 0 ? light : edge, lifetime));
            case "wave" -> DrawnRenderer.add(new Drawn.Wave(origin.add(0, 0.08, 0), Math.max(0.3, spread),
                    Math.max(0.05, size), color, lifetime));
            case "pulse" -> {
                double radius = Math.max(spread, size) * (reduce ? FLASH_SIZE : 1.0F);
                DrawnRenderer.add(new Drawn.Pulse(origin, radius, Math.max(0.06, radius * 0.18), color, lifetime));
            }
            case "beam" -> DrawnRenderer.add(new Drawn.Beam(origin, origin.add(direction(pattern, flat, random)
                    .scale(Math.max(0.3, spread))), Math.max(0.02, size), speed, color, light, lifetime));
            case "flash" -> DrawnRenderer.add(new Drawn.Flash(origin, Math.max(size, spread)
                    * (reduce ? FLASH_SIZE : 1.0F), reduce ? VfxColors.shade(color, -0.2F) : color, lifetime));
            case "smoke" -> DrawnRenderer.add(new Drawn.Smoke(origin, Math.max(1, count), Math.max(0.1, spread),
                    Math.max(0.1, size), speed, color, lifetime, random));
            default -> {
            }
        }
    }

    /** Direcao de um raio/feixe pelo padrao da camada (frente, para fora em leque, para cima, para o centro...). */
    static Vec3 direction(VfxPatterns.Pattern pattern, Vec3 flat, RandomSource random) {
        return switch (pattern) {
            case LINE, ARC -> flat.add((random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.3).normalize();
            case RISE -> new Vec3((random.nextDouble() - 0.5) * 0.5, 1.0, (random.nextDouble() - 0.5) * 0.5)
                    .normalize();
            case RING -> {
                double a = random.nextDouble() * Math.PI * 2;
                yield new Vec3(Math.cos(a), (random.nextDouble() - 0.5) * 0.15, Math.sin(a)).normalize();
            }
            default -> new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                    .normalize();
        };
    }
}
