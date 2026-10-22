// src/main/java/com/kn8/client/vfx/VfxEffects.java
package com.kn8.client.vfx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.common.config.ClientConfig;
import com.kn8.common.data.def.VfxProfileDef;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.common.registry.KN8Particles;
import com.kn8.common.vfx.VfxKeyframes;
import com.kn8.common.vfx.VfxS2C;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.vfx.VfxBudget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;

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
    private static final float GENERIC_WITH_PROFILE = 0.5F;
    private static final float GENERIC_SCALE_CAP = 4.0F;
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
        EFFECTS.put(VfxService.ENERGY_CHARGE, VfxEffects::energyCharge);
        EFFECTS.put(VfxService.ENERGY_BURST, VfxEffects::energyBurst);
        EFFECTS.put(VfxService.WEB, VfxEffects::web);
        EFFECTS.put(VfxService.TRANSFORM, VfxEffects::transform);
        EFFECTS.put(VfxService.REGENERATE, VfxEffects::regenerate);
    }

    private VfxEffects() {
    }

    /** Chamado no setup do cliente. */
    public static void install() {
        KN8ClientHooks.register(VfxS2C.TYPE, VfxEffects::onVfx);
        VfxKeyframes.install(VfxEffects::onKeyframe);
        // VFX-2: camadas com atraso (sequencias como a transformacao do Kafka).
        NeoForge.EVENT_BUS.addListener(KaijuVfx::tick);
    }

    /** VFX-1: particulas proprias do KaijuVFX (uma fabrica por silhueta). */
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KN8Particles.ARC.get(), sprites -> new VfxParticle.Provider(sprites,
                VfxParticle.Kind.ARC));
        event.registerSpriteSet(KN8Particles.RING.get(), sprites -> new VfxParticle.Provider(sprites,
                VfxParticle.Kind.RING));
        event.registerSpriteSet(KN8Particles.SHARD.get(), sprites -> new VfxParticle.Provider(sprites,
                VfxParticle.Kind.SHARD));
        event.registerSpriteSet(KN8Particles.STREAK.get(), sprites -> new VfxParticle.Provider(sprites,
                VfxParticle.Kind.STREAK));
        event.registerSpriteSet(KN8Particles.GLOW.get(), sprites -> new VfxParticle.Provider(sprites,
                VfxParticle.Kind.GLOW));
        event.registerSpriteSet(KN8Particles.SPORE.get(), sprites -> new VfxParticle.Provider(sprites,
                VfxParticle.Kind.SPORE));
        event.registerSpriteSet(KN8Particles.PULSE.get(), sprites -> new VfxParticle.Provider(sprites,
                VfxParticle.Kind.PULSE));
    }

    /**
     * VFX-1: marca {@code particle_effects} de uma animacao GeckoLib: a receita {@code effect} do perfil visual da
     * entidade, saindo do {@code locator}. Sem receita, nada (a marca so vale para quem a tem).
     */
    private static void onKeyframe(Entity entity, String effect, String locator) {
        if (!(entity.level() instanceof ClientLevel level)) {
            return;
        }
        Vec3 at = locate(entity, locator);
        if (!KaijuVfx.inRange(at)) {
            return;
        }
        KaijuVfx.profile(VfxService.profileOf(entity)).ifPresent(profile -> profile.recipe(effect).ifPresent(
                recipe -> KaijuVfx.playRecipe(level, profile, recipe, at, forward(entity), 1.0F,
                        KaijuVfx.Body.of(entity.getBbHeight()), KaijuVfx.budget(), KaijuVfx.GenericEffect.NONE)));
    }

    /**
     * Origem pela caixa do corpo [SUPOSICAO ate a VFX-2: a posicao real do osso vem depois]: cabeca/boca na frente
     * no alto, pes na frente no chao, maos/arma na frente a meia altura (esquerda/direita para o lado).
     */
    static Vec3 locate(Entity entity, String locator) {
        Vec3 forward = forward(entity);
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        Vec3 front = entity.position().add(forward.scale(entity.getBbWidth() * 0.5));
        double height = entity.getBbHeight();
        return switch (locator) {
            case "head", "mouth", "eyes" -> front.add(0, height * 0.85, 0);
            case "ground", "feet" -> front.add(0, 0.1, 0);
            case "item_left", "left_hand" -> front.add(side.scale(entity.getBbWidth() * 0.4)).add(0, height * 0.6, 0);
            case "item_right", "right_hand" ->
                    front.add(side.scale(-entity.getBbWidth() * 0.4)).add(0, height * 0.6, 0);
            default -> front.add(0, height * 0.6, 0);
        };
    }

    private static Vec3 forward(Entity entity) {
        float yaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
        return new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
    }

    /**
     * VFX-1: com o perfil visual de quem fez o efeito, a receita da habilidade/tecnica (ou do proprio efeito)
     * substitui o generico; sem receita, o generico continua e ganha uma camada com a paleta e o material da
     * especie/personagem. Longe da camera ({@code [fx] effectDistance}) so o tremor.
     */
    private static void onVfx(VfxS2C payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (KaijuVfx.inRange(payload.position())) {
            draw(level, payload);
        }
        if (payload.shake() > 0) {
            CameraShake.add(payload.shake(), payload.position());
        }
    }

    private static void draw(ClientLevel level, VfxS2C payload) {
        Optional<VfxProfileDef> profile = KaijuVfx.profile(payload.source().profile());
        String path = payload.effect().getPath();
        String key = payload.source().key().isEmpty() ? path : payload.source().key();
        // VFX-3: golpe a golpe ("yae_uchi/3"): a receita do golpe, senao a da tecnica, senao a do efeito.
        String technique = key.contains("/") ? key.substring(0, key.indexOf('/')) : key;
        Optional<List<VfxProfileDef.Layer>> recipe = profile.flatMap(def -> def.recipe(key)
                .or(() -> def.recipe(technique)).or(() -> def.recipe(path)));
        VfxBudget budget = KaijuVfx.budget();
        // VFX-2: o efeito cresce com o corpo de quem o fez (kaiju grande, efeito grande).
        KaijuVfx.Body body = KaijuVfx.Body.of(payload.source().height());
        Effect effect = EFFECTS.get(payload.effect());
        // Os efeitos comuns crescem pelo alcance, ate um teto (a quantidade deles tambem sobe com a intensidade).
        float growth = Math.min(body.spread(), GENERIC_SCALE_CAP);
        if (recipe.isPresent()) {
            KaijuVfx.playRecipe(level, profile.get(), recipe.get(), payload.position(), payload.direction(),
                    payload.intensity(), body, budget, effect == null ? KaijuVfx.GenericEffect.NONE
                            : intensity -> effect.play(level, payload.position(), payload.direction(),
                                    intensity * growth, amount()));
            return;
        }
        if (effect != null) {
            // Com perfil, o generico (branco/cinza vanilla) fica pela metade: a cor e o material da especie mandam.
            float generic = profile.isPresent() ? GENERIC_WITH_PROFILE : 1.0F;
            effect.play(level, payload.position(), payload.direction(), payload.intensity() * growth,
                    amount() * generic);
        }
        profile.ifPresent(def -> PaletteLayers.play(level, payload.effect(), def, payload.position(),
                payload.direction(), payload.intensity(), body, budget));
    }

    static float amount() {
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

    /**
     * 0.6: aviso do raio de energia: faiscas vindo de uma esfera em volta do ponto ate ele (a boca do kaiju), durante
     * o preparo. A intensidade e o raio da esfera.
     */
    private static void energyCharge(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        RandomSource random = level.random;
        int count = Math.max(6, Math.round(28 * amount));
        double radius = 2.5 * intensity;
        for (int i = 0; i < count; i++) {
            Vec3 offset = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                    .normalize().scale(radius);
            Vec3 from = at.add(offset);
            Vec3 speed = offset.scale(-0.08);
            level.addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK, from.x, from.y,
                    from.z, speed.x, speed.y, speed.z);
        }
    }

    /** 0.6: explosao de energia: estouro, anel de faiscas e fumaca. */
    private static void energyBurst(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        level.addParticle(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 0, 0, 0);
        burst(level, ParticleTypes.END_ROD, at, count(intensity * 1.5F, amount), 0.8, 0.5);
        burst(level, ParticleTypes.ELECTRIC_SPARK, at, count(intensity, amount), 1.5, 0.4);
        burst(level, ParticleTypes.LARGE_SMOKE, at, count(intensity * 0.5F, amount), 1.5, 0.05);
    }

    /** 0.6: teia: fios brancos espalhados no ponto atingido. */
    private static void web(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        ItemParticleOption cobweb = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB));
        burst(level, cobweb, at, count(intensity * 1.5F, amount), 1.2, 0.15);
        burst(level, ParticleTypes.WHITE_ASH, at, count(intensity * 2.0F, amount), 1.5, 0.05);
    }

    /** VFX-2: troca de forma sem receita: estouro e coluna de fumaca (o que o servidor fazia antes). */
    private static void transform(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.0, at.z, 0, 0, 0);
        burst(level, ParticleTypes.LARGE_SMOKE, at.add(0, 1.0, 0), count(intensity * 2.5F, amount), 1.2, 0.08);
    }

    /** VFX-2: regeneracao sem receita: brilhos verdes em volta do corpo. */
    private static void regenerate(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        burst(level, ParticleTypes.HAPPY_VILLAGER, at, count(intensity * 0.5F, amount), 1.2, 0.02);
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
