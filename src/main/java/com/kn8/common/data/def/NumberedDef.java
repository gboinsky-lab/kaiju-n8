package com.kn8.common.data.def;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Comportamento extra de um kaiju numerado ({@code data/<ns>/kn8/numbered/<id do kaiju>.json}, 0.2 Etapa 8). O
 * Kaiju No. 9 revive carcacas ({@code revive}: especie da carcaca -> especie revivida), comanda os kaiju por perto
 * (passa o seu alvo) e foge quando a vida cai a {@code flee_health} (fracao), dando {@code flee_merit} a quem
 * estava lutando por perto. 0.3: {@code revive_boss} (especie da carcaca -> chefe de {@code boss/*.json}: o Honju
 * volta como chefe com barra e fases) e a ressurreicao em massa das ondas {@code mass_revive} (raio, gesto mais longo
 * e intervalo entre um kaiju e o proximo, para o exercito levantar em sequencia).
 */
public record NumberedDef(Map<ResourceLocation, ResourceLocation> revive, float reviveRadius, int reviveCastTicks,
        int reviveCooldownTicks, int maxRevivedAlive, float commandRadius, float fleeHealth, int fleeMerit,
        Map<ResourceLocation, ResourceLocation> reviveBoss, float massReviveRadius, int massReviveCastTicks,
        int massReviveIntervalTicks, Regeneration regeneration, Optional<Transform> transform) {

    /**
     * Mudanca de forma (0.6-E, No. 10 pequeno -> gigante; Miguel: "depois de um tempo na batalha"): depois de
     * {@code after_combat_ticks} com alvo, ou com a vida abaixo de {@code health_below} (o que vier antes), vira a
     * especie {@code into} com {@code health_fraction} da vida maxima nova. {@code destruction_radius}: blocos que a
     * forma nova quebra ao surgir (forca da categoria Daikaiju).
     */
    public record Transform(ResourceLocation into, int afterCombatTicks, float healthBelow, float healthFraction,
            float destructionRadius) {
        public static final Codec<Transform> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("into").forGetter(Transform::into),
                DefCodecs.TICKS.fieldOf("after_combat_ticks").forGetter(Transform::afterCombatTicks),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("health_below", 0.5F).forGetter(Transform::healthBelow),
                Codec.floatRange(0.05F, 1.0F).optionalFieldOf("health_fraction", 1.0F)
                        .forGetter(Transform::healthFraction),
                Codec.floatRange(0.0F, 32.0F).optionalFieldOf("destruction_radius", 0.0F)
                        .forGetter(Transform::destructionRadius)
        ).apply(i, Transform::new));
    }

    /**
     * Regeneracao (0.6-D, especificacao do Miguel secao 5.2): com a vida abaixo de {@code below} (fracao), cura
     * {@code per_second} (fracao da vida maxima por segundo); abaixo de {@code fast_below}, {@code fast_per_second}.
     * Para enquanto ele levou golpe ha menos de {@code delay_after_hit_ticks} (pressao constante segura a cura).
     */
    public record Regeneration(float below, float perSecond, float fastBelow, float fastPerSecond,
            int delayAfterHitTicks) {
        public static final Regeneration NONE = new Regeneration(0.0F, 0.0F, 0.0F, 0.0F, 0);
        public static final Codec<Regeneration> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("below").forGetter(Regeneration::below),
                Codec.floatRange(0.0F, 1.0F).fieldOf("per_second").forGetter(Regeneration::perSecond),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("fast_below", 0.0F).forGetter(Regeneration::fastBelow),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("fast_per_second", 0.0F)
                        .forGetter(Regeneration::fastPerSecond),
                DefCodecs.TICKS.optionalFieldOf("delay_after_hit_ticks", 0).forGetter(Regeneration::delayAfterHitTicks)
        ).apply(i, Regeneration::new));

        /** Fracao da vida maxima curada por segundo com a vida em {@code healthFraction} (0 = nao regenera). */
        public float rateAt(float healthFraction) {
            if (fastPerSecond > 0 && healthFraction < fastBelow) {
                return fastPerSecond;
            }
            return healthFraction < below ? perSecond : 0.0F;
        }
    }

    public static final Codec<NumberedDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).optionalFieldOf("revive", Map.of())
                    .forGetter(NumberedDef::revive),
            Codec.floatRange(1.0F, 128.0F).optionalFieldOf("revive_radius", 24.0F).forGetter(NumberedDef::reviveRadius),
            DefCodecs.TICKS.optionalFieldOf("revive_cast_ticks", 60).forGetter(NumberedDef::reviveCastTicks),
            DefCodecs.TICKS.optionalFieldOf("revive_cooldown_ticks", 300).forGetter(NumberedDef::reviveCooldownTicks),
            Codec.intRange(0, 32).optionalFieldOf("max_revived_alive", 3).forGetter(NumberedDef::maxRevivedAlive),
            Codec.floatRange(0.0F, 128.0F).optionalFieldOf("command_radius", 32.0F)
                    .forGetter(NumberedDef::commandRadius),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flee_health", 0.3F).forGetter(NumberedDef::fleeHealth),
            Codec.intRange(0, 1_000_000).optionalFieldOf("flee_merit", 0).forGetter(NumberedDef::fleeMerit),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).optionalFieldOf("revive_boss", Map.of())
                    .forGetter(NumberedDef::reviveBoss),
            Codec.floatRange(1.0F, 256.0F).optionalFieldOf("mass_revive_radius", 72.0F)
                    .forGetter(NumberedDef::massReviveRadius),
            DefCodecs.TICKS.optionalFieldOf("mass_revive_cast_ticks", 100).forGetter(NumberedDef::massReviveCastTicks),
            DefCodecs.TICKS.optionalFieldOf("mass_revive_interval_ticks", 4)
                    .forGetter(NumberedDef::massReviveIntervalTicks),
            Regeneration.CODEC.optionalFieldOf("regeneration", Regeneration.NONE)
                    .forGetter(NumberedDef::regeneration),
            Transform.CODEC.optionalFieldOf("transform").forGetter(NumberedDef::transform)
    ).apply(i, NumberedDef::new));
}
