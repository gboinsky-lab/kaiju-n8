// src/main/java/com/kn8/client/vfx/KaijuVfx.java
package com.kn8.client.vfx;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * KaijuVFX (VFX-1, diretriz do Miguel): o que todo efeito do cliente usa. Perfil visual da entidade (paleta,
 * material, receitas), teto de particulas por efeito ({@code [fx] maxParticlesPerEffect} x {@code particleLevel}),
 * distancia de desenho ({@code [fx] effectDistance}) e claroes reduzidos ({@code [fx] reduceFlashes}). Os efeitos
 * de cada personagem ficam nos dados ({@code vfx_profile/<id>.json}); aqui so a maquina.
 */
public final class KaijuVfx {

    /** Claroes reduzidos: brilho menor e sem branco puro (diretriz §6). */
    private static final float FLASH_SIZE = 0.7F;
    private static final float FLASH_WHITE_CAP = 0.78F;

    private KaijuVfx() {
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
     * Desenha uma receita do perfil: cada camada com a sua silhueta, cor da paleta e padrao. A intensidade do
     * servidor multiplica a quantidade e o alcance (o tamanho cresce mais devagar, pela raiz).
     */
    public static void playRecipe(ClientLevel level, VfxProfileDef profile, List<VfxProfileDef.Layer> layers,
            Vec3 at, Vec3 direction, float intensity, VfxBudget budget) {
        float size = (float) Math.sqrt(Math.max(0.1F, intensity));
        Vec3 flat = horizontal(direction);
        for (VfxProfileDef.Layer layer : layers) {
            VfxParticleType type = shape(layer.shape());
            int color = profile.palette().resolve(layer.color());
            if (type == null || color < 0) {
                continue;
            }
            Vec3 origin = at.add(flat.scale(layer.forward())).add(0, layer.up(), 0);
            emit(level, type, color, VfxPatterns.Pattern.of(layer.pattern()), Math.round(layer.count() * intensity),
                    layer.size() * size, layer.speed(), layer.spread() * intensity, layer.lifetime(), origin, flat,
                    budget);
        }
    }

    /** Uma camada: {@code count} particulas pelo padrao, dentro do teto, na qualidade do cliente. */
    public static void emit(ClientLevel level, VfxParticleType type, int color, VfxPatterns.Pattern pattern, int count,
            float size, double speed, double spread, int lifetime, Vec3 at, Vec3 direction, VfxBudget budget) {
        int granted = budget.take(count, VfxEffects.amount());
        if (granted <= 0) {
            return;
        }
        boolean bright = type == KN8Particles.GLOW.get() || type == KN8Particles.ARC.get()
                || type == KN8Particles.STREAK.get();
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
