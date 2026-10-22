// src/main/java/com/kn8/client/vfx/AuraRenderer.java
package com.kn8.client.vfx;

import java.util.ArrayList;
import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.client.vfx.draw.Drawn;
import com.kn8.client.vfx.draw.DrawnRenderer;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.AuraDef;
import com.kn8.core.vfx.VfxColors;
import com.kn8.common.registry.KN8Attachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
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
 * (e a % sobe sozinha com a vida baixa, no servidor). Quantidade segue {@code fx.particleLevel}.
 *
 * <p>VFX-R2 (Miguel: "nao quero particulas, quero efeitos como raios ou aura"): tudo desenhado ({@link Drawn}).
 * Estilo {@code lightning}: raios em arco colados ao corpo, labaredas da cor principal subindo, tracos claros e, perto
 * de 100%, um anel no chao; {@code flame}: labaredas maiores; {@code sparks}: tracos subindo. Os raios do bloco
 * {@code bolts} (VFX-4A) saem do corpo ou correm rente ao traje como raios continuos.</p>
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class AuraRenderer {

    /** Distancia maxima (ao quadrado) em que a aura e desenhada. */
    private static final double MAX_DISTANCE_SQR = 48.0 * 48.0;
    private static final int ARC_SEGMENTS = 8;
    private static final double ARC_JITTER = 0.09;
    /** VFX-4A: nos de cada raio. */
    private static final int BOLT_SEGMENTS = 6;
    /** Vida de um raio desenhado (ticks): tremula e e trocado por outro, como uma descarga. */
    private static final int BOLT_LIFE = 3;
    private static final int WISP_LIFE = 10;
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
        aura.bolts().ifPresent(bolts -> bolts(level, entity, bolts, intensity, amount));
    }

    // --- raios (VFX-4A) ------------------------------------------------------------------------------------------

    /**
     * Descargas saindo do corpo (Miguel: "sair raios igual no anime"): quantos por tick pela taxa do JSON, mais com o
     * Release; cada um em zigue-zague para fora ou correndo rente ao corpo, com galhos.
     */
    private static void bolts(ClientLevel level, LivingEntity entity, AuraDef.Bolts bolts, float intensity,
            float amount) {
        RandomSource random = level.random;
        double expected = bolts.rate() / 20.0 * (0.25 + 0.75 * intensity) * amount;
        int count = (int) expected + (random.nextDouble() < expected - (int) expected ? 1 : 0);
        if (count <= 0) {
            return;
        }
        float body = KaijuVfx.Body.of(entity.getBbHeight()).size();
        for (int i = 0; i < count; i++) {
            bolt(entity, bolts, body, random);
        }
    }

    private static void bolt(LivingEntity entity, AuraDef.Bolts bolts, float body, RandomSource random) {
        double height = entity.getBbHeight();
        double radius = entity.getBbWidth() * 0.5 * 0.9;
        double angle = random.nextDouble() * Math.PI * 2;
        double y = entity.getY() + height * (0.1 + random.nextDouble() * 0.85);
        Vec3 start = new Vec3(entity.getX() + Math.cos(angle) * radius, y, entity.getZ() + Math.sin(angle) * radius);
        List<Vec3> nodes = new ArrayList<>();
        nodes.add(start);
        if (random.nextFloat() < bolts.crawl()) {
            // Correndo pelo corpo: as linhas do traje acendendo.
            double turn = (0.5 + random.nextDouble() * 0.9) * (random.nextBoolean() ? 1 : -1);
            double climb = (random.nextDouble() - 0.5) * height * 0.5;
            for (int s = 1; s <= BOLT_SEGMENTS; s++) {
                double t = s / (double) BOLT_SEGMENTS;
                double a = angle + turn * t;
                double r = radius * (1.02 + random.nextDouble() * 0.08);
                nodes.add(new Vec3(entity.getX() + Math.cos(a) * r, y + climb * t + jitter(random) * body,
                        entity.getZ() + Math.sin(a) * r));
            }
        } else {
            double length = bolts.length() * height * (0.6 + random.nextDouble() * 0.6);
            Vec3 out = new Vec3(Math.cos(angle), (random.nextDouble() - 0.35) * 0.9, Math.sin(angle)).normalize();
            zigzag(nodes, start, out, length, BOLT_SEGMENTS, random);
        }
        List<List<Vec3>> paths = new ArrayList<>();
        paths.add(nodes);
        // Galhos: um pedaco curto saindo de um no do meio.
        for (int n = 1; n < nodes.size() - 1; n++) {
            if (random.nextFloat() < bolts.branches() / BOLT_SEGMENTS * 2.0F) {
                List<Vec3> branch = new ArrayList<>();
                branch.add(nodes.get(n));
                Vec3 dir = nodes.get(n).subtract(nodes.get(n - 1)).normalize()
                        .add(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                        .normalize();
                zigzag(branch, nodes.get(n), dir, nodes.get(n).distanceTo(start) * 0.5 + 0.2, 3, random);
                paths.add(branch);
            }
        }
        DrawnRenderer.add(new Drawn.Bolt(paths, bolts.width() * body, bolts.color(), bolts.core(),
                BOLT_LIFE + random.nextInt(2)));
    }

    private static void zigzag(List<Vec3> nodes, Vec3 start, Vec3 dir, double length, int segments,
            RandomSource random) {
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();
        double amplitude = length * 0.16;
        for (int s = 1; s <= segments; s++) {
            double t = s / (double) segments;
            Vec3 base = start.add(dir.scale(length * t));
            nodes.add(base.add(side.scale((random.nextDouble() - 0.5) * 2 * amplitude))
                    .add(up.scale((random.nextDouble() - 0.5) * 2 * amplitude)));
        }
    }

    // --- estilos -----------------------------------------------------------------------------------------------

    private static void lightning(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity,
            float amount) {
        RandomSource random = level.random;
        rising(level, entity, aura, intensity, amount, 1.0F);
        // Raios em arco: mais frequentes quanto mais forte a aura.
        if (random.nextFloat() < (0.25F + 0.6F * intensity) * amount) {
            arc(entity, aura, random);
        }
        if (intensity > 0.5F && random.nextFloat() < intensity * amount) {
            arc(entity, aura, random);
        }
        sparks(level, entity, aura, intensity * 0.6F, amount);
        if (intensity >= RING_FROM && level.getGameTime() % 4 == 0) {
            ring(entity, aura);
        }
    }

    /** Tracos claros subindo rente ao corpo (sem as faiscas brancas do vanilla). */
    private static void sparks(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity, float amount) {
        RandomSource random = level.random;
        if (random.nextFloat() > Math.min(1.0F, 1.5F * intensity * amount)) {
            return;
        }
        List<Vec3> starts = new ArrayList<>();
        List<Vec3> velocities = new ArrayList<>();
        int count = Math.max(1, Math.round(2 * intensity * amount));
        for (int i = 0; i < count; i++) {
            starts.add(around(entity, aura, random, 1.0));
            velocities.add(new Vec3(0, 0.12 + random.nextDouble() * 0.1, 0));
        }
        double scale = KaijuVfx.Body.of(entity.getBbHeight()).size();
        DrawnRenderer.add(new Drawn.Sparks(starts, velocities, 0.25 * scale, 0.04 * scale, 0.0, aura.secondary(),
                -1, 6));
    }

    private static void flame(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity, float amount) {
        rising(level, entity, aura, intensity * 1.6F, amount, 1.4F);
        sparks(level, entity, aura, intensity * 0.5F, amount);
        if (intensity >= RING_FROM && level.getGameTime() % 4 == 0) {
            ring(entity, aura);
        }
    }

    // --- pecas ---------------------------------------------------------------------------------------------------

    /** Labaredas da cor principal subindo em volta do corpo (a aura "queimando"). */
    private static void rising(ClientLevel level, LivingEntity entity, AuraDef aura, float intensity, float amount,
            float scale) {
        RandomSource random = level.random;
        double expected = 1.2 * intensity * amount * aura.size();
        int count = (int) expected + (random.nextDouble() < expected - (int) expected ? 1 : 0);
        double body = KaijuVfx.Body.of(entity.getBbHeight()).size();
        for (int i = 0; i < count; i++) {
            Vec3 at = around(entity, aura, random, 1.0);
            at = new Vec3(at.x, entity.getY() + random.nextDouble() * entity.getBbHeight() * 0.7, at.z);
            DrawnRenderer.add(new Drawn.Wisp(at, new Vec3(0, 1, 0), entity.getBbHeight() * 0.35 * scale,
                    0.12 * scale * body, VfxColors.shade(aura.color(), -0.1F), WISP_LIFE + random.nextInt(4),
                    random.nextDouble() * 6.0));
        }
    }

    /**
     * Um raio: linha quebrada que contorna o corpo por 60-140 graus, subindo ou descendo, na cor secundaria com o
     * miolo da principal.
     */
    private static void arc(LivingEntity entity, AuraDef aura, RandomSource random) {
        double radius = radius(entity, aura) * (0.85 + random.nextDouble() * 0.3);
        double angle = random.nextDouble() * Math.PI * 2;
        double sweep = Math.toRadians(60 + random.nextDouble() * 80) * (random.nextBoolean() ? 1 : -1);
        double height = entity.getBbHeight();
        double y = entity.getY() + height * (0.15 + random.nextDouble() * 0.75);
        double climb = (random.nextDouble() - 0.5) * height * 0.6;
        List<Vec3> nodes = new ArrayList<>();
        for (int step = 0; step <= ARC_SEGMENTS; step++) {
            double t = step / (double) ARC_SEGMENTS;
            double a = angle + sweep * t;
            nodes.add(new Vec3(entity.getX() + Math.cos(a) * radius + jitter(random),
                    y + climb * t + jitter(random), entity.getZ() + Math.sin(a) * radius + jitter(random)));
        }
        double scale = KaijuVfx.Body.of(entity.getBbHeight()).size();
        DrawnRenderer.add(new Drawn.Bolt(List.of(nodes), 0.07 * scale, aura.secondary(), aura.color(), BOLT_LIFE));
    }

    /** Anel da cor principal no chao, perto da % maxima. */
    private static void ring(LivingEntity entity, AuraDef aura) {
        double radius = radius(entity, aura) * 1.8;
        DrawnRenderer.add(new Drawn.Wave(entity.position().add(0, 0.06, 0), radius, radius * 0.15, aura.color(), 8));
    }

    private static Vec3 around(LivingEntity entity, AuraDef aura, RandomSource random, double spread) {
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = radius(entity, aura) * spread * (0.6 + random.nextDouble() * 0.5);
        return new Vec3(entity.getX() + Math.cos(angle) * radius,
                entity.getY() + random.nextDouble() * entity.getBbHeight(),
                entity.getZ() + Math.sin(angle) * radius);
    }

    private static double radius(LivingEntity entity, AuraDef aura) {
        return (entity.getBbWidth() * 0.5 + 0.35) * aura.size();
    }

    private static double jitter(RandomSource random) {
        return (random.nextDouble() - 0.5) * 2 * ARC_JITTER;
    }
}
