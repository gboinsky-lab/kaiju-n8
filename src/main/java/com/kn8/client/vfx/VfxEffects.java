// src/main/java/com/kn8/client/vfx/VfxEffects.java
package com.kn8.client.vfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.client.vfx.draw.Drawn;
import com.kn8.client.vfx.draw.DrawnFactory;
import com.kn8.client.vfx.draw.DrawnRenderer;
import com.kn8.common.config.ClientConfig;
import com.kn8.common.data.def.VfxProfileDef;
import com.kn8.common.network.KN8ClientHooks;
import com.kn8.common.registry.KN8Particles;
import com.kn8.common.vfx.VfxKeyframes;
import com.kn8.common.vfx.VfxS2C;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.vfx.VfxBudget;
import com.kn8.core.vfx.VfxColors;
import com.kn8.core.vfx.VfxPatterns.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Registro de efeitos visuais no cliente (Etapa D): cada id do {@link VfxService} vira efeito. Nada aqui decide
 * jogo; so desenha o que o servidor mandou. VFX-R2 (Miguel: "nao quero que nenhum tenha particulas"): todos os
 * efeitos comuns sao formas desenhadas ({@link Drawn}); so os pedacos de bloco da rachadura continuam particulas do
 * vanilla (meio-termo do Miguel). A quantidade segue {@code fx.particleLevel}.
 */
public final class VfxEffects {

    /** Desenho de um efeito: nivel, posicao, direcao, intensidade e fator de quantidade. */
    @FunctionalInterface
    interface Effect {
        void play(ClientLevel level, Vec3 position, Vec3 direction, float intensity, float amount);
    }

    /** VFX-R2: efeitos com medidas exatas do servidor (linha, area): nao crescem com o corpo. */
    @FunctionalInterface
    interface RawEffect {
        void play(ClientLevel level, VfxS2C payload);
    }

    private static final Map<ResourceLocation, Effect> EFFECTS = new HashMap<>();
    private static final Map<ResourceLocation, RawEffect> RAW = new HashMap<>();
    private static final int BASE_COUNT = 12;
    private static final float LOW = 0.35F;
    private static final float MEDIUM = 0.7F;
    private static final float HIGH = 1.0F;
    private static final int CRACK_RAYS = 9;
    private static final float GENERIC_WITH_PROFILE = 0.5F;
    private static final float GENERIC_SCALE_CAP = 4.0F;

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
        EFFECTS.put(VfxService.EXPLOSION, VfxEffects::explosion);
        EFFECTS.put(VfxService.PARRY, VfxEffects::parry);
        EFFECTS.put(VfxService.HARDEN, VfxEffects::harden);
        RAW.put(VfxService.TRACER, VfxEffects::tracer);
        RAW.put(VfxService.TENDRIL, VfxEffects::tendril);
        RAW.put(VfxService.TELEGRAPH, VfxEffects::telegraph);
        RAW.put(VfxService.SMOKE, VfxEffects::smoke);
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
            // VFX-4B: costas (asas da Kikoru No. 4).
            case "back", "wings" -> entity.position().subtract(forward.scale(entity.getBbWidth() * 0.6))
                    .add(0, height * 0.7, 0);
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
        RawEffect raw = RAW.get(payload.effect());
        if (raw != null) {
            raw.play(level, payload);
            return;
        }
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

    // --- cores dos efeitos comuns (sem perfil: ar, poeira, fumaca, fogo) -------------------------------------------

    private static final int AIR = 0xE8F2F6;
    private static final int SPARK = 0xFFF4D6;
    private static final int DUST_COLOR = 0x8C8270;
    private static final int SMOKE_COLOR = 0x4E4C4A;
    private static final int FIRE = 0xFF9A3C;
    private static final int FIRE_CORE = 0xFFF0C0;
    private static final int ENERGY = 0x9FD8FF;
    private static final int CRACK_DARK = 0x2A2420;
    private static final int TENDRIL_RED = 0x8C0A14;

    /** Cor "#rrggbb" na chave do efeito (efeitos sem perfil que mudam de cor); senao a dada. */
    private static int keyColor(VfxS2C payload, int fallback) {
        String key = payload.source().key();
        int at = key.indexOf('#');
        if (at < 0 || key.length() < at + 7) {
            return fallback;
        }
        try {
            return Integer.parseInt(key.substring(at + 1, at + 7), 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static RandomSource random(ClientLevel level) {
        return level.random;
    }

    // --- efeitos ------------------------------------------------------------------------------------------

    /** Golpe: anel de ar e linhas de impacto saindo do ponto. */
    private static void impact(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        double r = 0.5 + 0.3 * intensity;
        DrawnRenderer.add(new Drawn.Pulse(at, r, r * 0.2, AIR, 5));
        DrawnFactory.sparks(Pattern.BURST, Math.min(14, count(intensity * 0.7F, amount)), 0.08F, 0.35, 0.3, 5,
                at, horizontal(dir), 0.0, 0.0, SPARK, 0xFFFFFF, random(level));
    }

    /** Onda de choque: anel no chao abrindo e uma faixa baixa de poeira. */
    private static void shockwave(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        double radius = 3.0 * intensity;
        DrawnRenderer.add(new Drawn.Wave(at.add(0, 0.08, 0), radius, Math.max(0.2, radius * 0.18), AIR, 12));
        ringSmoke(level, at.add(0, 0.2, 0), radius * 0.5, Math.max(0.4, radius * 0.18), DUST_COLOR, 16, amount);
    }

    /** Poeira do pisao/queda: nuvens baixas e um anel pequeno. */
    private static void dust(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        DrawnRenderer.add(new Drawn.Smoke(at.add(0, 0.2, 0), Math.max(2, Math.round(6 * amount)), 1.2 * intensity,
                0.5 * Math.sqrt(intensity) + 0.3, 0.01, DUST_COLOR, 24, random(level)));
        DrawnRenderer.add(new Drawn.Wave(at.add(0, 0.06, 0), 1.5 * intensity, 0.25, DUST_COLOR, 10));
    }

    /** Corte comum: arco claro e duas faiscas. */
    private static void slash(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        Vec3 flat = horizontal(dir);
        double radius = 0.9 + 0.3 * intensity;
        double tilt = (random(level).nextDouble() - 0.5) * 70.0;
        DrawnRenderer.add(new Drawn.Band(at.subtract(flat.scale(radius * 0.6)), flat, tilt, radius, radius * 0.18,
                -1, AIR, 5));
        DrawnFactory.sparks(Pattern.BURST, 4, 0.06F, 0.3, 0.2, 4, at, flat, 0.0, 0.0, SPARK, -1, random(level));
    }

    /**
     * Disparo: clarao curto na boca do cano e um fio de fumaca (o tiro em si e o {@link #tracer}). Sem o FLASH
     * vanilla (uma bola branca de varios blocos que cobria o atirador).
     */
    private static void weaponFire(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        DrawnRenderer.add(new Drawn.Flash(at.add(dir.scale(0.15)), 0.3, FIRE, 2));
        DrawnRenderer.add(new Drawn.Beam(at, at.add(dir.scale(0.5)), 0.12, 0.0, FIRE, FIRE_CORE, 2));
        DrawnRenderer.add(new Drawn.Smoke(at.add(dir.scale(0.3)), 2, 0.1, 0.12, 0.01, SMOKE_COLOR, 12,
                random(level)));
    }

    /** Rugido: tres aneis de ar saindo da boca para a frente, um apos o outro. */
    private static void roar(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        Vec3 forward = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
        for (int ring = 0; ring < 3; ring++) {
            int delay = ring * 3;
            double radius = (0.8 + 0.6 * ring) * intensity;
            Vec3 center = at.add(forward.scale(0.6 * (ring + 1) * intensity));
            KaijuVfx.later(delay, () -> DrawnRenderer.add(new Drawn.Pulse(center, radius, radius * 0.15, AIR, 8,
                    forward)));
        }
    }

    /** Traje liberando poder: raios pequenos em volta (a aura de verdade e o {@link AuraRenderer}). */
    private static void suitRelease(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        RandomSource random = random(level);
        int count = Math.max(1, Math.round(3 * intensity * amount));
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 start = at.add(Math.cos(a) * 0.4, 0.3 + random.nextDouble() * 1.4, Math.sin(a) * 0.4);
            DrawnRenderer.add(new Drawn.Bolt(start, new Vec3(Math.cos(a), random.nextDouble() - 0.3, Math.sin(a))
                    .normalize(), 0.5 + intensity * 0.5, 0.05, ENERGY, 0xFFFFFF, 3, 0.4F, random));
        }
    }

    /**
     * 0.5: rachaduras escuras em raios pelo chao (desenhadas) e pedrinhas do proprio bloco saltando (os pedacos de
     * bloco continuam do vanilla: meio-termo do Miguel); a intensidade e o comprimento das rachaduras em blocos.
     */
    private static void groundCrack(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        RandomSource random = level.random;
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) {
            ground = Blocks.STONE.defaultBlockState();
        }
        int rays = Math.max(5, Math.round(CRACK_RAYS * amount));
        DrawnRenderer.add(new Drawn.Crack(new Vec3(at.x, Math.floor(at.y - 0.5) + 1.02, at.z), rays, intensity,
                0.35, CRACK_DARK, -1, 50, random));
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        for (int i = 0; i < count(intensity * 0.6F, amount); i++) {
            level.addParticle(debris, at.x + (random.nextDouble() - 0.5) * 1.5, at.y + 0.3,
                    at.z + (random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 0.6,
                    random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.6);
        }
    }

    /** 0.6: carga do raio de energia: raios vindo de uma esfera ate o ponto (a boca) e um brilho crescendo. */
    private static void energyCharge(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        RandomSource random = random(level);
        double radius = 2.5 * intensity;
        int bolts = Math.max(2, Math.round(4 * amount));
        for (int i = 0; i < bolts; i++) {
            Vec3 offset = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                    .normalize().scale(radius);
            Vec3 from = at.add(offset);
            DrawnRenderer.add(new Drawn.Bolt(from, offset.scale(-1).normalize(), radius, 0.06, ENERGY, 0xFFFFFF, 4,
                    0.3F, random));
        }
        DrawnRenderer.add(new Drawn.Flash(at, 0.4 * intensity, ENERGY, 6));
    }

    /** 0.6: explosao de energia: clarao, anel de ar, linhas saindo e fumaca desenhada. */
    private static void energyBurst(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        double r = 1.2 * intensity;
        DrawnRenderer.add(new Drawn.Flash(at, r, ENERGY, 5));
        DrawnRenderer.add(new Drawn.Pulse(at, r * 1.6, r * 0.25, 0xFFFFFF, 7));
        DrawnFactory.sparks(Pattern.BURST, Math.min(16, count(intensity, amount)), 0.1F, 0.5, 0.5, 7, at,
                horizontal(dir), 0.0, 0.0, ENERGY, 0xFFFFFF, random(level));
        DrawnRenderer.add(new Drawn.Smoke(at, Math.max(2, Math.round(5 * amount)), r * 0.8, r * 0.5, 0.02,
                SMOKE_COLOR, 26, random(level)));
    }

    /** 0.6: teia: fios brancos esticando do ponto atingido e um veu claro. */
    private static void web(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        DrawnFactory.sparks(Pattern.BURST, Math.min(14, count(intensity, amount)), 0.05F, 0.12, 1.0, 16, at,
                horizontal(dir), 0.0, 0.0, 0xF4F4F0, -1, random(level));
        DrawnRenderer.add(new Drawn.Smoke(at, 3, 0.6 * intensity, 0.5, 0.0, 0xD8D8D0, 20, random(level)));
    }

    /** VFX-2: troca de forma sem receita: clarao, coluna de fumaca e onda no chao. */
    private static void transform(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        double r = 1.2 * intensity;
        DrawnRenderer.add(new Drawn.Flash(at.add(0, 1.0, 0), r, FIRE_CORE, 6));
        DrawnRenderer.add(new Drawn.Smoke(at.add(0, 1.0, 0), Math.max(3, Math.round(8 * amount)), r, r * 0.6,
                0.04, SMOKE_COLOR, 40, random(level)));
        DrawnRenderer.add(new Drawn.Wave(at.add(0, 0.08, 0), r * 2.5, r * 0.3, AIR, 12));
    }

    /** VFX-2: regeneracao sem receita: labaredas verdes entrando no corpo. */
    private static void regenerate(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        RandomSource random = random(level);
        int count = Math.max(2, Math.round(4 * amount));
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 0.8 + 0.6 * intensity;
            Vec3 from = at.add(Math.cos(a) * r, (random.nextDouble() - 0.5) * intensity, Math.sin(a) * r);
            DrawnRenderer.add(new Drawn.Wisp(from, at.subtract(from), r * 0.9, 0.25 * Math.sqrt(intensity),
                    0x7CE87C, 16, random.nextDouble() * 6.0));
        }
    }

    /** Traje superaquecido: fumaca saindo do corpo e, muito quente, labaredas laranja. */
    private static void overheat(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        DrawnRenderer.add(new Drawn.Smoke(at.add(0, 1.0, 0), 2, 0.4, 0.25, 0.03, SMOKE_COLOR, 20, random(level)));
        if (intensity > 0.6F) {
            DrawnRenderer.add(new Drawn.Wisp(at.add(0, 0.8, 0), new Vec3(0, 1, 0), 0.8, 0.12, FIRE, 10,
                    random(level).nextDouble() * 6.0));
        }
    }

    /** VFX-R2: explosao (tiro do canhao, autodestruicao): clarao, onda, linhas de fogo e fumaca escura subindo. */
    private static void explosion(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        double r = Math.max(0.8, intensity);
        RandomSource random = random(level);
        DrawnRenderer.add(new Drawn.Flash(at, r * 1.1, FIRE_CORE, 4));
        DrawnRenderer.add(new Drawn.Flash(at, r * 0.7, FIRE, 9));
        DrawnRenderer.add(new Drawn.Pulse(at, r * 1.8, r * 0.2, AIR, 7));
        DrawnRenderer.add(new Drawn.Wave(at.add(0, 0.08 - Math.min(r, at.y - Math.floor(at.y)), 0), r * 2.0,
                r * 0.35, FIRE, 12));
        DrawnFactory.sparks(Pattern.BURST, Math.min(18, Math.round(10 * amount) + 4), (float) (0.08 * r), 0.45 * r,
                0.3 * r, 8, at, horizontal(dir), 0.0, 0.04, FIRE, FIRE_CORE, random);
        DrawnRenderer.add(new Drawn.Smoke(at, Math.max(4, Math.round(10 * amount)), r * 0.7, r * 0.55, 0.05,
                SMOKE_COLOR, 50, random));
    }

    /** VFX-R2: aparar: clarao dourado curto, anel pequeno e faiscas saindo do choque das laminas. */
    private static void parry(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        DrawnRenderer.add(new Drawn.Flash(at, 0.4 * intensity, 0xFFE08A, 3));
        DrawnRenderer.add(new Drawn.Pulse(at, 0.7 * intensity, 0.08, 0xFFF4D6, 5));
        DrawnFactory.sparks(Pattern.BURST, 10, 0.06F, 0.45, 0.15, 5, at, horizontal(dir), 0.0, 0.05, 0xFFD060,
                0xFFFFFF, random(level));
    }

    /** VFX-R2: pele endurecida do No. 9: casca escura fechando sobre o corpo (anel escuro e lascas). */
    private static void harden(ClientLevel level, Vec3 at, Vec3 dir, float intensity, float amount) {
        DrawnRenderer.add(new Drawn.Pulse(at, intensity * 0.9, intensity * 0.25, 0x5A5A66, 8));
        DrawnFactory.sparks(Pattern.BURST, 8, 0.07F, 0.25, intensity * 0.5, 6, at, horizontal(dir), 0.0, 0.04,
                0x3A3A44, -1, random(level));
    }

    /** VFX-R2: rastro do tiro: risco claro andando da boca do cano ate onde bateu. */
    private static void tracer(ClientLevel level, VfxS2C payload) {
        Vec3 from = payload.position();
        Vec3 to = from.add(payload.direction());
        double length = from.distanceTo(to);
        if (length < 0.1) {
            return;
        }
        DrawnRenderer.add(new Drawn.Beam(from, to, 0.06, Math.min(length, 4.0), keyColor(payload, 0xFFE9A0),
                0xFFFFFF, 3));
    }

    /** VFX-R2: tentaculo/fio do No. 9: faixa grossa e escura, quase reta, tremendo (2 ticks por pulso). */
    private static void tendril(ClientLevel level, VfxS2C payload) {
        Vec3 from = payload.position();
        Vec3 path = payload.direction();
        double length = path.length();
        if (length < 0.1) {
            return;
        }
        int color = keyColor(payload, TENDRIL_RED);
        DrawnRenderer.add(new Drawn.Bolt(from, path.normalize(), length, 0.14 * Math.max(0.6, payload.intensity()),
                color, VfxColors.shade(color, 0.35F), 3, 0.0F, random(level)));
    }

    /**
     * VFX-R2: aviso do golpe pesado: chave "disc|sector|lane#rrggbb@ticks"; centro em {@code position}, raio na
     * intensidade; setor com angulo central e largura em {@code direction.x/y}; faixa com o fim e a meia largura em
     * {@code direction.x/y/z}.
     */
    private static void telegraph(ClientLevel level, VfxS2C payload) {
        String key = payload.source().key();
        int kind = key.startsWith("sector") ? 1 : key.startsWith("lane") ? 2 : 0;
        int life = 8;
        int at = key.indexOf('@');
        if (at >= 0) {
            try {
                life = Integer.parseInt(key.substring(at + 1)) + 2;
            } catch (NumberFormatException ignored) {
                // fica a vida padrao
            }
        }
        Vec3 p = payload.position();
        Vec3 d = payload.direction();
        DrawnRenderer.add(new Drawn.Zone(kind, p.x, p.z, p.y, payload.intensity(), d.x, d.y, d.x, d.y, d.z,
                keyColor(payload, 0xFFD000), life));
    }

    /** VFX-R2: fumaca desenhada (fuga, sumico, invocacao): nuvens subindo na cor da chave. */
    private static void smoke(ClientLevel level, VfxS2C payload) {
        float size = Math.max(0.3F, payload.intensity());
        float amount = amount();
        DrawnRenderer.add(new Drawn.Smoke(payload.position(), Math.max(3, Math.round(10 * amount)), size, size * 0.5,
                0.03, keyColor(payload, SMOKE_COLOR), 40, random(level)));
    }

    /** Fumaca em anel saindo pelo chao (onda de choque). */
    private static void ringSmoke(ClientLevel level, Vec3 at, double radius, double size, int color, int lifetime,
            float amount) {
        int puffs = Math.max(4, Math.round(8 * amount));
        List<Vec3> points = new ArrayList<>();
        List<Vec3> drift = new ArrayList<>();
        for (int i = 0; i < puffs; i++) {
            double a = Math.PI * 2 * i / puffs;
            points.add(at.add(Math.cos(a) * radius * 0.3, 0, Math.sin(a) * radius * 0.3));
            drift.add(new Vec3(Math.cos(a) * radius * 0.05, 0.005, Math.sin(a) * radius * 0.05));
        }
        DrawnRenderer.add(new Drawn.Smoke(points, drift, size, color, lifetime));
    }

    private static Vec3 horizontal(Vec3 direction) {
        return KaijuVfx.horizontal(direction);
    }
}
