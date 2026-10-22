// src/main/java/com/kn8/client/vfx/KaijuVfx.java
package com.kn8.client.vfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.kn8.common.config.ClientConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.VfxProfileDef;
import com.kn8.client.vfx.draw.DrawnFactory;
import com.kn8.common.registry.KN8Particles;
import com.kn8.core.vfx.VfxBudget;
import com.kn8.core.vfx.VfxColors;
import com.kn8.core.vfx.VfxPatterns;
import com.kn8.core.vfx.VfxScale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
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
            } else if (layer.drawn()) {
                // VFX-R1: forma desenhada (sem particulas).
                int color = profile.palette().resolve(layer.color());
                if (color < 0) {
                    continue;
                }
                int edge = layer.edge().isEmpty() ? profile.palette().resolve("accent")
                        : profile.palette().resolve(layer.edge());
                Vec3 origin = at.add(flat.scale(layer.forward() * body.spread())).add(0, layer.up() * body.spread(), 0);
                int count = Math.max(1, Math.round(layer.count() * intensity * Math.min(body.count(), 2.0F)));
                emit = () -> DrawnFactory.spawn(layer.shape(), color, edge, VfxPatterns.Pattern.of(layer.pattern()),
                        count, layer.size() * size * body.size(), layer.speed() * body.spread(),
                        layer.spread() * intensity * body.spread(), layer.lifetime(), origin, flat, layer.tilt(),
                        level.random);
            } else {
                String type = layer.shape();
                int color = profile.palette().resolve(layer.color());
                if (!KN8Particles.shapes().containsKey(type) || color < 0) {
                    continue;
                }
                Vec3 origin = at.add(flat.scale(layer.forward() * body.spread())).add(0, layer.up() * body.spread(), 0);
                int count = Math.round(layer.count() * intensity * body.count());
                emit = () -> emit(level, type, color, VfxPatterns.Pattern.of(layer.pattern()), count,
                        layer.size() * size * body.size(), layer.speed() * Math.min(body.spread(), 3.0F),
                        layer.spread() * intensity * body.spread(), layer.lifetime(), origin, flat, layer.tilt(),
                        budget);
            }
            if (layer.delay() <= 0) {
                emit.run();
            } else if (PENDING.size() < MAX_PENDING) {
                PENDING.add(new Pending(emit, new int[] {layer.delay()}));
            }
        }
    }

    /** VFX-R2: roda {@code emit} daqui a {@code ticks} ticks (sequencias dos efeitos comuns, como o rugido). */
    public static void later(int ticks, Runnable emit) {
        if (ticks <= 0) {
            emit.run();
        } else if (PENDING.size() < MAX_PENDING) {
            PENDING.add(new Pending(emit, new int[] {ticks}));
        }
    }

    /** Efeito comum do evento, tocado por uma camada {@code generic} da receita. */
    @FunctionalInterface
    public interface GenericEffect {
        GenericEffect NONE = intensity -> { };

        void play(float intensity);
    }

    /**
     * Uma camada: {@code count} pecas pelo padrao, dentro do teto, na qualidade do cliente. VFX-R2 (Miguel: nada de
     * particulas): a silhueta antiga ({@code arc}, {@code ring}, {@code glow}...) vira forma desenhada
     * ({@link DrawnFactory#fromParticle}).
     */
    public static void emit(ClientLevel level, String type, int color, VfxPatterns.Pattern pattern, int count,
            float size, double speed, double spread, int lifetime, Vec3 at, Vec3 direction, VfxBudget budget) {
        emit(level, type, color, pattern, count, size, speed, spread, lifetime, at, direction, 0.0, budget);
    }

    /** VFX-3: com o {@code tilt} do arco (graus em volta da frente). */
    public static void emit(ClientLevel level, String type, int color, VfxPatterns.Pattern pattern, int count,
            float size, double speed, double spread, int lifetime, Vec3 at, Vec3 direction, double tilt,
            VfxBudget budget) {
        int granted = budget.take(count, VfxEffects.amount());
        if (granted <= 0) {
            return;
        }
        boolean bright = "glow".equals(type) || "arc".equals(type) || "streak".equals(type) || "pulse".equals(type);
        if (bright && ClientConfig.REDUCE_FLASHES.get()) {
            size *= FLASH_SIZE;
            color = capWhite(color);
        }
        DrawnFactory.fromParticle(type, color, pattern, granted, size, speed, spread, lifetime, at, direction, tilt,
                level.random);
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
