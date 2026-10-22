// src/main/java/com/kn8/client/vfx/draw/DrawnFactory.java
package com.kn8.client.vfx.draw;

import java.util.ArrayList;
import java.util.List;

import com.kn8.common.config.ClientConfig;
import com.kn8.core.vfx.VfxColors;
import com.kn8.core.vfx.VfxPatterns;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * VFX-R1: transforma uma camada de receita com forma desenhada ({@code bolt}, {@code band}, {@code wave},
 * {@code pulse}, {@code beam}, {@code flash}, {@code smoke}) em efeitos do {@link DrawnRenderer}. Os numeros ja vem
 * multiplicados pela intensidade e pelo corpo de quem fez o efeito (VFX-2). VFX-R2: tambem {@code sparks} (linhas
 * de impacto), {@code crack} (rachadura no chao), {@code wisp} (labareda de energia), e a traducao das camadas antigas
 * de particula ({@link #fromParticle}) para que nenhuma receita solte particula.
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
            case "sparks" -> sparks(pattern, Math.min(count, MAX_LINES), size, speed, spread, lifetime, origin, flat,
                    tilt, 0.0, color, light, random);
            case "crack" -> DrawnRenderer.add(new Drawn.Crack(origin.add(0, 0.05, 0), Math.max(3, count),
                    Math.max(0.5, spread), Math.max(0.08, size), color, edge, lifetime, random));
            case "wisp" -> {
                for (int i = 0; i < Math.min(count, MAX_WISPS); i++) {
                    double[] p = VfxPatterns.spawn(VfxPatterns.Pattern.RISE, i, count, flat.x, flat.z, spread, 0.0,
                            random::nextDouble);
                    DrawnRenderer.add(new Drawn.Wisp(origin.add(p[0], p[1], p[2]), direction(pattern, flat, random)
                            .add(0, pattern == VfxPatterns.Pattern.RISE ? 0 : 0.5, 0), Math.max(0.3,
                            speed * lifetime), Math.max(0.04, size), color, lifetime, random.nextDouble() * 6.0));
                }
            }
            default -> {
            }
        }
    }

    /** Teto de tracos por camada e de raios/labaredas (formas desenhadas pesam mais que um ponto). */
    private static final int MAX_LINES = 18;
    private static final int MAX_BOLTS = 6;
    private static final int MAX_WISPS = 6;
    private static final int MAX_PUFFS = 10;
    /** Cor escura: o arco antigo com essa cor vira o miolo da lamina (sem brilho). */
    private static final float DARK = 0.3F;
    /** Vida minima do corte desenhado: em 5 ticks ele abria e sumia rapido demais para ser lido. */
    private static final int MIN_BAND_LIFE = 9;

    /**
     * VFX-R2: uma camada antiga de particula ({@code arc}, {@code ring}, {@code pulse}, {@code glow}, {@code streak},
     * {@code shard}, {@code spore}) vira forma desenhada no mesmo padrao e na mesma cor: arco = faixa de corte (cor
     * escura = miolo, clara = fio da borda), anel = onda no chao, pulso = anel de ar, brilho convergindo = raios,
     * brilho subindo = labaredas, brilho/risco/lasca = linhas de impacto (lasca com gravidade), esporo = nuvem.
     */
    public static void fromParticle(String particle, int color, VfxPatterns.Pattern pattern, int count, float size,
            double speed, double spread, int lifetime, Vec3 at, Vec3 flat, double tilt, RandomSource random) {
        boolean reduce = ClientConfig.REDUCE_FLASHES.get();
        int light = reduce ? VfxColors.shade(color, 0.15F) : VfxColors.shade(color, 0.6F);
        if (pattern == VfxPatterns.Pattern.ARC && !"spore".equals(particle) && !"shard".equals(particle)) {
            boolean dark = Math.max(VfxColors.red(color), Math.max(VfxColors.green(color), VfxColors.blue(color)))
                    < DARK;
            double radius = Math.max(0.3, spread);
            double width = Math.max(size * 1.5, radius * 0.16);
            // Brilho por cima do miolo escuro: um pouco mais fino, no mesmo raio (lamina preta com o fio da cor).
            DrawnRenderer.add(new Drawn.Band(at, flat, tilt, radius, dark ? width : width * 0.6, dark ? color : -1,
                    dark ? -1 : color, Math.max(MIN_BAND_LIFE, lifetime)));
            return;
        }
        switch (particle) {
            case "ring" -> DrawnRenderer.add(new Drawn.Wave(at.add(0, 0.06, 0), Math.max(0.4, Math.max(size, spread)),
                    Math.max(0.08, Math.max(size, spread) * 0.22), color, Math.max(6, lifetime)));
            case "pulse" -> {
                double radius = Math.max(size, spread) * (reduce ? FLASH_SIZE : 1.0F);
                DrawnRenderer.add(new Drawn.Pulse(at, radius, Math.max(0.05, radius * 0.18), color,
                        Math.max(4, lifetime)));
            }
            case "glow" -> {
                if (pattern == VfxPatterns.Pattern.CONVERGE) {
                    int bolts = Math.max(1, Math.min(MAX_BOLTS, count / 3));
                    for (int i = 0; i < bolts; i++) {
                        double[] p = VfxPatterns.spawn(pattern, i, bolts, flat.x, flat.z, spread, speed,
                                random::nextDouble);
                        Vec3 from = at.add(p[0], p[1], p[2]);
                        DrawnRenderer.add(new Drawn.Bolt(from, at.subtract(from).normalize(), from.distanceTo(at),
                                Math.max(0.03, size * 0.5), color, light, Math.max(3, lifetime / 2), 0.3F,
                                random));
                    }
                } else if (pattern == VfxPatterns.Pattern.RISE) {
                    for (int i = 0; i < Math.min(count, MAX_WISPS); i++) {
                        double[] p = VfxPatterns.spawn(pattern, i, count, flat.x, flat.z, spread, speed,
                                random::nextDouble);
                        DrawnRenderer.add(new Drawn.Wisp(at.add(p[0], p[1], p[2]), new Vec3(0, 1, 0),
                                Math.max(0.3, speed * lifetime), Math.max(0.05, size), color, lifetime,
                                random.nextDouble() * 6.0));
                    }
                } else {
                    sparks(pattern, Math.min(count, MAX_LINES), size, speed, spread, lifetime, at, flat, tilt, 0.0,
                            color, light, random);
                }
            }
            case "streak" -> sparks(pattern, Math.min(count, MAX_LINES), size, speed, spread, lifetime, at, flat,
                    tilt, 0.0, color, light, random);
            case "shard" -> sparks(pattern, Math.min(count, MAX_LINES), size * 1.4F, Math.max(speed, 0.15), spread,
                    Math.max(8, lifetime), at, flat, tilt, 0.04, color, -1, random);
            case "spore" -> {
                int puffs = Math.max(1, Math.min(MAX_PUFFS, count));
                List<Vec3> points = new ArrayList<>();
                List<Vec3> drift = new ArrayList<>();
                for (int i = 0; i < puffs; i++) {
                    double[] p = VfxPatterns.spawn(pattern, i, puffs, flat.x, flat.z, spread, speed, tilt,
                            random::nextDouble);
                    points.add(at.add(p[0], p[1], p[2]));
                    drift.add(new Vec3(p[3] * 0.5, Math.abs(p[4]) * 0.5 + 0.01, p[5] * 0.5));
                }
                DrawnRenderer.add(new Drawn.Smoke(points, drift, Math.max(0.15, size * 1.2), color,
                        Math.max(8, lifetime)));
            }
            default -> {
            }
        }
    }

    /** Linhas de impacto pelo padrao: cada traco nasce onde a particula nasceria e anda na mesma velocidade. */
    public static void sparks(VfxPatterns.Pattern pattern, int count, float size, double speed, double spread,
            int lifetime, Vec3 at, Vec3 flat, double tilt, double gravity, int color, int core, RandomSource random) {
        if (count <= 0) {
            return;
        }
        List<Vec3> starts = new ArrayList<>();
        List<Vec3> velocities = new ArrayList<>();
        double minSpeed = Math.max(0.12, speed);
        for (int i = 0; i < count; i++) {
            double[] p = VfxPatterns.spawn(pattern, i, count, flat.x, flat.z, spread, minSpeed, tilt,
                    random::nextDouble);
            starts.add(at.add(p[0], p[1], p[2]));
            Vec3 v = new Vec3(p[3], p[4], p[5]);
            if (v.lengthSqr() < 1.0E-6) {
                v = direction(VfxPatterns.Pattern.BURST, flat, random).scale(minSpeed);
            }
            velocities.add(v.scale(0.7 + random.nextDouble() * 0.6));
        }
        // Grossas o bastante para ler a 10 blocos (as finas de 0,03 sumiam no teste em jogo).
        DrawnRenderer.add(new Drawn.Sparks(starts, velocities, Math.max(0.35, size * 4.0 + minSpeed * 3.0),
                Math.max(0.07, size * 0.9), gravity, color, core, Math.max(5, lifetime)));
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
