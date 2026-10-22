// src/main/java/com/kn8/client/vfx/KaijuVfx.java
package com.kn8.client.vfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.kn8.common.config.ClientConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.VfxProfileDef;
import com.kn8.common.registry.KN8Particles;
import com.kn8.common.vfx.VfxParticleType;
import com.kn8.core.vfx.VfxBudget;
import com.kn8.core.vfx.VfxColors;
import com.kn8.core.vfx.VfxPatterns;
import com.kn8.core.vfx.VfxScale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * KaijuVFX (VFX-1, diretriz do Miguel): o que todo efeito do cliente usa. Perfil visual da entidade (paleta,
 * material, receitas), teto de particulas por efeito ({@code [fx] maxParticlesPerEffect} x {@code particleLevel}),
 * distancia de desenho ({@code [fx] effectDistance}) e claroes reduzidos ({@code [fx] reduceFlashes}). Os efeitos
 * de cada personagem ficam nos dados ({@code vfx_profile/<id>.json}); aqui so a maquina. VFX-2: tudo cresce com o
 * corpo de quem faz o efeito ({@link Body}) e as camadas podem vir em sequencia ({@code delay}).
 */
public final class KaijuVfx {

    /** Claroes reduzidos: brilho menor e sem branco puro (diretriz §6). */
    private static final float FLASH_SIZE = 0.7F;
    private static final float FLASH_WHITE_CAP = 0.78F;
    /** Teto de camadas esperando o atraso (efeitos longos de muitos personagens ao mesmo tempo). */
    private static final int MAX_PENDING = 256;

    /** Camada agendada: dispara quando {@code ticks} chega a 0 (no tick do cliente). */
    private record Pending(Runnable emit, int[] ticks) {
    }

    /** So o tick do cliente mexe nesta fila; esvazia ao sair do mundo. */
    private static final List<Pending> PENDING = new ArrayList<>();

    private KaijuVfx() {
    }

    /**
     * Tamanho do corpo de quem fez o efeito (VFX-2, Miguel: kaiju grande nao tem efeito pequeno): multiplicadores de
     * tamanho, alcance e quantidade das particulas pela altura, contra a altura de referencia.
     */
    public record Body(float size, float spread, float count) {
        public static final Body REFERENCE = new Body(1.0F, 1.0F, 1.0F);

        public static Body of(float height) {
            float body = VfxScale.body(height, ClientConfig.REFERENCE_HEIGHT.get().floatValue());
            return new Body(VfxScale.factor(body, ClientConfig.SCALE_SIZE_EXPONENT.get()),
                    VfxScale.factor(body, ClientConfig.SCALE_SPREAD_EXPONENT.get()),
                    VfxScale.factor(body, ClientConfig.SCALE_COUNT_EXPONENT.get()));
        }
    }

    /** Liga a fila de camadas com atraso ao tick do cliente. */
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().isPaused()) {
            if (Minecraft.getInstance().level == null) {
                PENDING.clear();
            }
            return;
        }
        List<Runnable> due = new ArrayList<>();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext();) {
            Pending pending = it.next();
            if (--pending.ticks()[0] <= 0) {
                due.add(pending.emit());
                it.remove();
            }
        }
        due.forEach(Runnable::run);
    }

    public static Optional<VfxProfileDef> profile(Optional<ResourceLocation> id) {
        return id.flatMap(key -> KN8Data.VFX_PROFILE.get(key, true));
    }

    /** Longe demais da camera (ou sem mundo): nao desenha. */
    public static boolean inRange(Vec3 at) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        double max = ClientConfig.EFFECT_DISTANCE.get();
        return minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(at) <= max * max;
    }

    public static VfxBudget budget() {
        return new VfxBudget(ClientConfig.MAX_PARTICLES_PER_EFFECT.get());
    }

    /**
     * Desenha uma receita do perfil: cada camada com a sua silhueta, cor da paleta e padrao, agora ou depois do
     * {@code delay}. A intensidade do servidor multiplica a quantidade e o alcance (o tamanho cresce mais devagar,
     * pela raiz); o corpo multiplica tamanho, alcance, deslocamentos e quantidade. Camada {@code generic} chama
     * {@code generic} com a intensidade dela.
     */
    public static void playRecipe(ClientLevel level, VfxProfileDef profile, List<VfxProfileDef.Layer> layers,
            Vec3 at, Vec3 direction, float intensity, Body body, VfxBudget budget, GenericEffect generic) {
        float size = (float) Math.sqrt(Math.max(0.1F, intensity));
        Vec3 flat = horizontal(direction);
        for (VfxProfileDef.Layer layer : layers) {
            Runnable emit;
            if (layer.generic()) {
                emit = () -> generic.play(intensity * layer.size());
            } else {
                VfxParticleType type = shape(layer.shape());
                int color = profile.palette().resolve(layer.color());
                if (type == null || color < 0) {
                    continue;
                }
                Vec3 origin = at.add(flat.scale(layer.forward() * body.spread())).add(0, layer.up() * body.spread(), 0);
                int count = Math.round(layer.count() * intensity * body.count());
                emit = () -> emit(level, type, color, VfxPatterns.Pattern.of(layer.pattern()), count,
                        layer.size() * size * body.size(), layer.speed() * Math.min(body.spread(), 3.0F),
                        layer.spread() * intensity * body.spread(), layer.lifetime(), origin, flat, budget);
            }
            if (layer.delay() <= 0) {
                emit.run();
            } else if (PENDING.size() < MAX_PENDING) {
                PENDING.add(new Pending(emit, new int[] {layer.delay()}));
            }
        }
    }

    /** Efeito comum do evento, tocado por uma camada {@code generic} da receita. */
    @FunctionalInterface
    public interface GenericEffect {
        GenericEffect NONE = intensity -> { };

        void play(float intensity);
    }

    /** Uma camada: {@code count} particulas pelo padrao, dentro do teto, na qualidade do cliente. */
    public static void emit(ClientLevel level, VfxParticleType type, int color, VfxPatterns.Pattern pattern, int count,
            float size, double speed, double spread, int lifetime, Vec3 at, Vec3 direction, VfxBudget budget) {
        int granted = budget.take(count, VfxEffects.amount());
        if (granted <= 0) {
            return;
        }
        boolean bright = type == KN8Particles.GLOW.get() || type == KN8Particles.ARC.get()
                || type == KN8Particles.STREAK.get() || type == KN8Particles.PULSE.get();
        if (bright && ClientConfig.REDUCE_FLASHES.get()) {
            size *= FLASH_SIZE;
            color = capWhite(color);
        }
        RandomSource random = level.random;
        for (int i = 0; i < granted; i++) {
            double[] p = VfxPatterns.spawn(pattern, i, granted, direction.x, direction.z, spread, speed,
                    random::nextDouble);
            level.addParticle(type.with(color, size, lifetime), at.x + p[0], at.y + p[1], at.z + p[2], p[3], p[4],
                    p[5]);
        }
    }

    public static VfxParticleType shape(String name) {
        var holder = KN8Particles.shapes().get(name);
        return holder == null ? null : holder.get();
    }

    private static int capWhite(int rgb) {
        float max = Math.max(VfxColors.red(rgb), Math.max(VfxColors.green(rgb), VfxColors.blue(rgb)));
        float min = Math.min(VfxColors.red(rgb), Math.min(VfxColors.green(rgb), VfxColors.blue(rgb)));
        // Quase branco: escurece ate o teto mantendo o tom.
        return min > FLASH_WHITE_CAP ? VfxColors.shade(rgb, -(1.0F - FLASH_WHITE_CAP / max)) : rgb;
    }

    static Vec3 horizontal(Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
    }
}
