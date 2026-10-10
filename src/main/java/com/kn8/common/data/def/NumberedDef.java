package com.kn8.common.data.def;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
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
        int massReviveIntervalTicks, Regeneration regeneration, Optional<Transform> transform, List<Absorb> absorb,
        Optional<Hardening> hardening, Optional<Adaptation> adaptation) {

    /**
     * Mudanca de forma (0.6-E, No. 10 pequeno -> gigante; Miguel: "depois de um tempo na batalha"): depois de
     * {@code after_combat_ticks} com alvo, ou com a vida abaixo de {@code health_below} (o que vier antes), vira a
     * especie {@code into} com {@code health_fraction} da vida maxima nova. {@code destruction_radius}: blocos que a
     * forma nova quebra ao surgir (forca da categoria Daikaiju). 0.7-E: {@code message} e a chave do aviso no chat
     * (No. 9 -> forma preta).
     */
    public record Transform(ResourceLocation into, int afterCombatTicks, float healthBelow, float healthFraction,
            float destructionRadius, String message) {
        public static final Codec<Transform> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("into").forGetter(Transform::into),
                DefCodecs.TICKS.fieldOf("after_combat_ticks").forGetter(Transform::afterCombatTicks),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("health_below", 0.5F).forGetter(Transform::healthBelow),
                Codec.floatRange(0.05F, 1.0F).optionalFieldOf("health_fraction", 1.0F)
                        .forGetter(Transform::healthFraction),
                Codec.floatRange(0.0F, 32.0F).optionalFieldOf("destruction_radius", 0.0F)
                        .forGetter(Transform::destructionRadius),
                Codec.STRING.optionalFieldOf("message", "kn8.no10.giant_form").forGetter(Transform::message)
        ).apply(i, Transform::new));
    }

    /**
     * Absorcao (0.7-E, Biblioteca v22 secao 33.2/33.3, formas originais do mod): com a propria vida abaixo de
     * {@code self_health_below}, o No. 9 procura a ate {@code seek_radius} blocos um kaiju da especie
     * {@code species} (vivo com a vida abaixo de {@code target_health_below}, ou a carcaca dele se {@code carcass}),
     * puxa-o com tentaculos durante {@code cast_ticks} e vira a especie {@code into} com a vida cheia. Se o alvo
     * morrer, sumir ou sair de {@code seek_radius} antes do fim, a absorcao e cancelada. 1,0 nos dois limites de vida
     * quer dizer "com qualquer vida".
     */
    public record Absorb(List<ResourceLocation> species, ResourceLocation into, float seekRadius, int castTicks,
            float targetHealthBelow, float selfHealthBelow, boolean carcass, String message) {
        public static final Codec<Absorb> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.listOf().fieldOf("species").forGetter(Absorb::species),
                ResourceLocation.CODEC.fieldOf("into").forGetter(Absorb::into),
                Codec.floatRange(1.0F, 128.0F).optionalFieldOf("seek_radius", 24.0F).forGetter(Absorb::seekRadius),
                DefCodecs.TICKS.optionalFieldOf("cast_ticks", 60).forGetter(Absorb::castTicks),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("target_health_below", 1.0F)
                        .forGetter(Absorb::targetHealthBelow),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("self_health_below", 1.0F)
                        .forGetter(Absorb::selfHealthBelow),
                Codec.BOOL.optionalFieldOf("carcass", true).forGetter(Absorb::carcass),
                Codec.STRING.optionalFieldOf("message", "kn8.no9.absorbed").forGetter(Absorb::message)
        ).apply(i, Absorb::new));
    }

    /**
     * Endurecimento (0.7-E, secao 33.1 "defesa/endurecimento"): ao levar golpe, com {@code chance}, a pele endurece
     * por {@code duration_ticks} e o dano cai {@code reduction} (fracao); depois espera {@code cooldown_ticks}.
     */
    public record Hardening(float chance, float reduction, int durationTicks, int cooldownTicks) {
        public static final Codec<Hardening> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Hardening::chance),
                Codec.floatRange(0.0F, 0.95F).fieldOf("reduction").forGetter(Hardening::reduction),
                DefCodecs.TICKS.fieldOf("duration_ticks").forGetter(Hardening::durationTicks),
                DefCodecs.TICKS.fieldOf("cooldown_ticks").forGetter(Hardening::cooldownTicks)
        ).apply(i, Hardening::new));
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

    /**
     * Analise/adaptacao (balanceamento v1.2, secoes 4.1 e 6 "No. 9 Analysis"): cada golpe recebido do mesmo tipo
     * (tipo de dano + arma ou especie de quem bate) soma uma marca; a partir da segunda, o dano desse tipo cai
     * {@code per_hit} por marca, ate {@code max_reduction}. Sem levar esse tipo por {@code decay_ticks}, perde uma
     * marca: variar o ataque (outra arma, outro golpe, outro atacante) contorna a adaptacao.
     */
    public record Adaptation(float perHit, float maxReduction, int decayTicks) {
        public static final Codec<Adaptation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 0.5F).fieldOf("per_hit").forGetter(Adaptation::perHit),
                Codec.floatRange(0.0F, 0.9F).fieldOf("max_reduction").forGetter(Adaptation::maxReduction),
                DefCodecs.TICKS.optionalFieldOf("decay_ticks", 100).forGetter(Adaptation::decayTicks)
        ).apply(i, Adaptation::new));

        /** Reducao do dano com {@code marks} marcas do mesmo tipo (a primeira nao reduz). */
        public float reductionFor(int marks) {
            return Math.min(maxReduction, Math.max(0, marks - 1) * perHit);
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
            Transform.CODEC.optionalFieldOf("transform").forGetter(NumberedDef::transform),
            Absorb.CODEC.listOf().optionalFieldOf("absorb", List.of()).forGetter(NumberedDef::absorb),
            // O RecordCodecBuilder aceita 16 campos: endurecimento e adaptacao vao juntos num par.
            Codec.mapPair(Hardening.CODEC.optionalFieldOf("hardening"), Adaptation.CODEC.optionalFieldOf("adaptation"))
                    .forGetter(def -> Pair.of(def.hardening(), def.adaptation()))
    ).apply(i, (revive, reviveRadius, reviveCast, reviveCooldown, maxRevived, command, flee, fleeMerit, reviveBoss,
            massRadius, massCast, massInterval, regeneration, transform, absorb, defense) -> new NumberedDef(revive,
            reviveRadius, reviveCast, reviveCooldown, maxRevived, command, flee, fleeMerit, reviveBoss, massRadius,
            massCast, massInterval, regeneration, transform, absorb, defense.getFirst(), defense.getSecond())));
}
