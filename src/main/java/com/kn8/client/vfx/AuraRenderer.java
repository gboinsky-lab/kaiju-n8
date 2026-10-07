// src/main/java/com/kn8/client/vfx/AuraRenderer.java
package com.kn8.client.vfx;

import org.joml.Vector3f;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AuraDef;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Aura de poder (0.5, ideia do Miguel): desenhada por cada cliente, todo tick, em volta de quem tem a % publica
 * ({@code kn8:release_visual}) acima do {@code min_release} da aura dele ({@code kn8:aura} -> {@code aura/<id>.json}).
 * Nao ha pacote por tick: o servidor so muda os dois valores publicos quando eles mudam. Mais forte quanto maior a %
 * (e a % sobe sozinha com a vida baixa, no servidor). Quantidade de particulas segue {@code fx.particleLevel}.
 *
 * <p>Estilo {@code lightning} (referencia do Miguel, aura roxa): raios em arco colados ao corpo, po da cor principal
 * subindo, faiscas claras saindo e, perto de 100%, um anel no chao.</p>
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class AuraRenderer {

    /** Distancia maxima (ao quadrado) em que a aura e desenhada. */
    private static final double MAX_DISTANCE_SQR = 48.0 * 48.0;
    private static final int ARC_SEGMENTS = 8;
    private static final int ARC_POINTS_PER_SEGMENT = 3;
    private static final float ARC_SCALE = 0.55F;
    private static final float DUST_SCALE = 0.9F;
    private static final float SPARK_SCALE = 0.45F;
    private static final double ARC_JITTER = 0.09;
    /** A partir desta intensidade aparece o anel no chao. */
    private static final float RING_FROM = 0.7F;

    private AuraRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || minecraft.getCameraEntity() == null) {
            return;
        }
        Vec3 camera = minecraft.getCameraEntity().position();
        float amount = VfxEffects.amount();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isInvisible()
                    || living.distanceToSqr(camera) > MAX_DISTANCE_SQR) {
                continue;
            }
            Integer release = living.getExistingDataOrNull(KN8Attachments.RELEASE_VISUAL);
            if (release == null || release <= 0) {
                continue;
            }
            // Em primeira pessoa a propria aura cobriria a tela: so os outros (e voce em terceira pessoa).
            if (living == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
                continue;
            }
            ResourceLocation auraId = living.getExistingDataOrNull(KN8Attachments.AURA);
            AuraDef aura = KN8Data.AURA.get(auraId == null ? KN8Attachments.DEFAULT_AURA : auraId, true)
                    .orElse(null);
            if (aura == null || release < aura.minRelease()) {
                continue;
            }
            draw(level, living, aura, aura.intensity(release), amount);
        }
    }

    /** Desenha um tick da aura (publico para a previa de comando e para os soldados especiais). */
    public static void draw(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity, float amount) {
        switch (aura.style()) {
            case LIGHTNING -> lightning(level, entity, aura, intensity, amount);
            case FLAME -> flame(level, entity, aura, intensity, amount);
            case SPARKS -> sparks(level, entity, aura, intensity, amount);
        }
    }

    // --- estilos -----------------------------------------------------------------------------------------------

    private static void lightning(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity,
            float amount) {
        RandomSource random = level.random;
        rising(level, entity, aura, intensity, amount, DUST_SCALE);
        // Raios em arco: mais frequentes quanto mais forte a aura.
        if (random.nextFloat() < (0.25F + 0.6F * intensity) * amount) {
            arc(level, entity, aura, random);
        }
        if (intensity > 0.5F && random.nextFloat() < intensity * amount) {
            arc(level, entity, aura, random);
        }
        sparks(level, entity, aura, intensity * 0.6F, amount);
        if (intensity >= RING_FROM && level.getGameTime() % 4 == 0) {
            ring(level, entity, aura, amount);
        }
    }

    private static void sparks(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity, float amount) {
        RandomSource random = level.random;
        int count = Math.max(1, Math.round(3 * intensity * amount));
        DustColorTransitionOptions spark = new DustColorTransitionOptions(rgb(aura.secondary()), rgb(aura.color()),
                SPARK_SCALE);
        for (int i = 0; i < count; i++) {
            Vec3 at = around(entity, aura, random, 1.0);
            level.addParticle(spark, at.x, at.y, at.z, 0, 0.05, 0);
        }
        if (aura.style() == AuraDef.Style.SPARKS && random.nextFloat() < intensity * amount) {
            Vec3 at = around(entity, aura, random, 0.9);
            level.addParticle(intensity > 0.75F ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK, at.x, at.y,
                    at.z, 0, 0.03, 0);
        }
    }

    private static void flame(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity, float amount) {
        rising(level, entity, aura, intensity * 1.6F, amount, DUST_SCALE * 1.3F);
        sparks(level, entity, aura, intensity * 0.5F, amount);
        if (intensity >= RING_FROM && level.getGameTime() % 4 == 0) {
            ring(level, entity, aura, amount);
        }
    }

    // --- pecas ---------------------------------------------------------------------------------------------------

    /** Po da cor principal subindo em volta do corpo. */
    private static void rising(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity, float amount,
            float scale) {
        RandomSource random = level.random;
        int count = Math.max(1, Math.round(4 * intensity * amount * aura.size()));
        DustColorTransitionOptions dust = new DustColorTransitionOptions(rgb(aura.color()), rgb(aura.secondary()),
                scale);
        for (int i = 0; i < count; i++) {
            Vec3 at = around(entity, aura, random, 1.0);
            level.addParticle(dust, at.x, at.y, at.z, 0, 0.1, 0);
        }
    }

    /**
     * Um raio: linha quebrada que contorna o corpo por 60-140 graus, subindo ou descendo, feita de pontos claros
     * (cor secundaria) com um nucleo da cor principal.
     */
    private static void arc(ClientLevel level, LivingEntity entity, AuraDef aura, RandomSource random) {
        double radius = radius(entity, aura) * (0.85 + random.nextDouble() * 0.3);
        double angle = random.nextDouble() * Math.PI * 2;
        double sweep = Math.toRadians(60 + random.nextDouble() * 80) * (random.nextBoolean() ? 1 : -1);
        double height = entity.getBbHeight();
        double y = entity.getY() + height * (0.15 + random.nextDouble() * 0.75);
        double climb = (random.nextDouble() - 0.5) * height * 0.6;
        DustParticleOptions bright = new DustParticleOptions(rgb(aura.secondary()), ARC_SCALE);
        DustParticleOptions core = new DustParticleOptions(rgb(aura.color()), ARC_SCALE * 0.8F);
        Vec3 previous = null;
        for (int step = 0; step <= ARC_SEGMENTS; step++) {
            double t = step / (double) ARC_SEGMENTS;
            double a = angle + sweep * t;
            Vec3 point = new Vec3(entity.getX() + Math.cos(a) * radius + jitter(random),
                    y + climb * t + jitter(random), entity.getZ() + Math.sin(a) * radius + jitter(random));
            if (previous != null) {
                for (int i = 0; i < ARC_POINTS_PER_SEGMENT; i++) {
                    Vec3 p = previous.lerp(point, i / (double) ARC_POINTS_PER_SEGMENT);
                    level.addParticle(i == 0 ? core : bright, p.x, p.y, p.z, 0, 0, 0);
                }
            }
            previous = point;
        }
    }

    /** Anel da cor principal no chao, perto da % maxima. */
    private static void ring(ClientLevel level, LivingEntity entity, AuraDef aura, float amount) {
        int points = Math.max(8, Math.round(20 * amount));
        double radius = radius(entity, aura) * 1.8;
        DustParticleOptions dust = new DustParticleOptions(rgb(aura.color()), DUST_SCALE);
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.addParticle(dust, entity.getX() + Math.cos(a) * radius, entity.getY() + 0.1,
                    entity.getZ() + Math.sin(a) * radius, 0, 0, 0);
        }
    }

    private static Vec3 around(LivingEntity entity, AuraDef aura, RandomSource random, double spread) {
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = radius(entity, aura) * spread * (0.6 + random.nextDouble() * 0.5);
        return new Vec3(entity.getX() + Mth.cos((float) angle) * radius,
                entity.getY() + random.nextDouble() * entity.getBbHeight(),
                entity.getZ() + Mth.sin((float) angle) * radius);
    }

    private static double radius(LivingEntity entity, AuraDef aura) {
        return (entity.getBbWidth() * 0.5 + 0.35) * aura.size();
    }

    private static double jitter(RandomSource random) {
        return (random.nextDouble() - 0.5) * 2 * ARC_JITTER;
    }

    private static Vector3f rgb(int color) {
        return new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }
}
