// src/main/java/com/kn8/client/vfx/VfxEffects.java
package com.kn8.client.vfx;

import java.util.HashMap;
import java.util.Map;

import com.kn8.common.config.ClientConfig;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.common.vfx.VfxS2C;
import com.kn8.common.vfx.VfxService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Registro de efeitos visuais no cliente (Etapa D): cada id do {@link VfxService} vira particulas. Nada aqui decide
 * jogo; so desenha o que o servidor mandou. A quantidade de particulas segue {@code fx.particleLevel}.
 */
public final class VfxEffects {

    /** Desenho de um efeito: nivel, posicao, direcao, intensidade e fator de quantidade. */
    @FunctionalInterface
    interface Effect {
        void play(ClientLevel level, Vec3 position, Vec3 direction, float intensity, float amount);
    }

    private static final Map<ResourceLocation, Effect> EFFECTS = new HashMap<>();
    private static final int BASE_COUNT = 12;
    private static final int MUZZLE_FLAMES = 3;
    private static final double MUZZLE_FLAME_SPEED = 0.04;
    private static final float LOW = 0.35F;
    private static final float MEDIUM = 0.7F;
    private static final float HIGH = 1.0F;
    private static final int CRACK_RAYS = 9;
    private static final double CRACK_STEP = 0.35;

    static {
        EFFECTS.put(VfxService.IMPACT, VfxEffects::impact);
        EFFECTS.put(VfxService.SHOCKWAVE, VfxEffects::shockwave);
        EFFECTS.put(VfxService.DUST, VfxEffects::dust);
        EFFECTS.put(VfxService.SLASH, VfxEffects::slash);
        EFFECTS.put(VfxService.WEAPON_FIRE, VfxEffects::weaponFire);
        EFFECTS.put(VfxService.ROAR, VfxEffects::roar);
        EFFECTS.put(VfxService.SUIT_RELEASE, VfxEffects::suitRelease);
        EFFECTS.put(VfxService.OVERHEAT, VfxEffects::overheat);
        EFFECTS.put(VfxService.GROUND_CRACK, VfxEffects::groundCrack);
    }

    private VfxEffects() {
    }

    /** Chamado no setup do cliente. */
    public static void install() {
        KN8ClientHooks.register(VfxS2C.TYPE, VfxEffects::onVfx);
    }

    private static void onVfx(VfxS2C payload) {
        ClientLevel level = Minecraft.getInstance().level;
        Effect effect = EFFECTS.get(payload.effect());
        if (level == null || effect == null) {
            return;
        }
        effect.play(level, payload.position(), payload.direction(), payload.intensity(), amount());
        if (payload.shake() > 0) {
            CameraShake.add(payload.shake(), payload.position());
        }
    }

    private static float amount() {
        return switch (ClientConfig.PARTICLE_LEVEL.get()) {
            case LOW -> LOW;
            case MEDIUM -> MEDIUM;
            case HIGH -> HIGH;
        };
    }

    private static int count(float intensity, float amount) {
        return Math.max(1, Math.round(BASE_COUNT * intensity * amount));
    }

    private static void burst(ClientLevel level, ParticleOptions particle, Vec3 at, int count, double spread,
            double speed) {
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            level.addParticle(particle, at.x + (random.nextDouble() - 0.5) * spread,
                    at.y + (random.nextDouble() - 0.5) * spread, at.z + (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * speed, random.nextDouble() * speed,
                    (random.nextDouble() - 0.5) * speed);
        }
    }

    // --- efeitos ------------------------------------------------------------------------------------------

    private static void impact(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        burst(level, ParticleTypes.CRIT, at, count(intensity, amount), 0.6, 0.4);
        burst(level, ParticleTypes.POOF, at, count(intensity * 0.5F, amount), 0.4, 0.1);
    }

    /** Anel de fumaca que se espalha pelo chao, a partir do ponto de impacto. */
    private static void shockwave(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        int points = Math.max(8, Math.round(32 * intensity * amount));
        double speed = 0.35 * intensity;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            level.addParticle(ParticleTypes.CLOUD, at.x, at.y + 0.2, at.z, vx, 0.02, vz);
            level.addParticle(ParticleTypes.POOF, at.x, at.y + 0.1, at.z, vx * 0.7, 0.0, vz * 0.7);
        }
    }

    private static void dust(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        burst(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at, Math.max(1, count(intensity * 0.3F, amount)), 2.5, 0.05);
        burst(level, ParticleTypes.POOF, at, count(intensity, amount), 2.0, 0.15);
    }

    private static void slash(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0, 0, 0);
        burst(level, ParticleTypes.CRIT, at, count(intensity * 0.5F, amount), 0.5, 0.2);
    }

    /**
     * Clarao e fumaca na boca do cano (o rastro do tiro ja sai do servidor). Sem o FLASH vanilla: ele e o clarao
     * de fogos de artificio (uma bola branca de varios blocos) e cobria o atirador em jogo; aqui, chamas pequenas
     * saindo para a frente do cano.
     */
    private static void weaponFire(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        for (int i = 0; i < MUZZLE_FLAMES; i++) {
            Vec3 velocity = dir.scale(MUZZLE_FLAME_SPEED * (i + 1));
            level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
        }
        burst(level, ParticleTypes.SMOKE, at.add(dir.scale(0.2)), count(intensity * 0.4F, amount), 0.15, 0.05);
    }

    /** Ondas de ar saindo da boca do kaiju. */
    private static void roar(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        int points = Math.max(6, Math.round(20 * intensity * amount));
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                Vec3 offset = new Vec3(Math.cos(angle), Math.sin(angle), 0).scale(0.4 * ring);
                level.addParticle(ParticleTypes.POOF, at.x + offset.x, at.y + offset.y, at.z,
                        dir.x * 0.3 * ring, 0, dir.z * 0.3 * ring);
            }
        }
    }

    /** Aura do traje: mais faiscas quanto maior a % liberada (intensidade 0-1). */
    private static void suitRelease(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        int count = Math.max(1, Math.round(10 * intensity * amount));
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double height = random.nextDouble() * 1.8;
            level.addParticle(intensity > 0.75F ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                    at.x + Mth.cos((float) angle) * 0.5, at.y + height, at.z + Mth.sin((float) angle) * 0.5,
                    0, 0.03, 0);
        }
    }

    /**
     * 0.5: rachaduras em raios pelo chao (lascas do proprio bloco de baixo) e pedrinhas saltando; a intensidade e o
     * comprimento das rachaduras em blocos.
     */
    private static void groundCrack(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        RandomSource random = level.random;
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) {
            ground = Blocks.STONE.defaultBlockState();
        }
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        int rays = Math.max(5, Math.round(CRACK_RAYS * amount));
        for (int ray = 0; ray < rays; ray++) {
            double angle = 2 * Math.PI * ray / rays + random.nextDouble() * 0.4;
            double x = at.x;
            double z = at.z;
            // Cada raio anda em passos curtos e entorta um pouco, como uma rachadura.
            for (double travelled = 0; travelled < intensity; travelled += CRACK_STEP) {
                angle += (random.nextDouble() - 0.5) * 0.5;
                x += Math.cos(angle) * CRACK_STEP;
                z += Math.sin(angle) * CRACK_STEP;
                level.addParticle(debris, x, at.y + 0.05, z, 0, 0.08 + random.nextDouble() * 0.12, 0);
            }
        }
        burst(level, debris, at.add(0, 0.3, 0), count(intensity * 0.6F, amount), 1.5, 0.6);
    }

    /** Traje superaquecido: fumaca e pequenas chamas saindo do jogador. */
    private static void overheat(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        burst(level, ParticleTypes.SMOKE, at.add(0, 1.0, 0), count(intensity * 0.5F, amount), 0.6, 0.05);
        if (intensity > 0.6F) {
            burst(level, ParticleTypes.SMALL_FLAME, at.add(0, 1.0, 0), Math.max(1, count(intensity * 0.2F, amount)),
                    0.5, 0.02);
        }
    }
}
