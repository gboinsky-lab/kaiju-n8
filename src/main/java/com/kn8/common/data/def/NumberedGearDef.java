package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Equipamento numerado do jogador ({@code data/<ns>/kn8/numbered_gear/<id>.json}, PM-11; Plano Mestre secao 11;
 * Miguel 2026-10-23: "todas as armas numeradas podem ser usadas pelo jogador, com requisitos"). O item continua sendo
 * a arma do {@code weapon/*.json}; aqui ficam so os requisitos e o dominio:
 * <ul>
 *   <li>{@code rank}, {@code research} e {@code min_limit} (limite pessoal de Release em %): conferidos no servidor a
 *   cada golpe; sem eles a arma nao ativa (o item continua no inventario, nada se perde);</li>
 *   <li>{@code mastery}: XP de dominio por nivel (o primeiro e 0); o dominio sobe com o dano causado em kaiju com a
 *   propria arma ({@code xp_per_damage});</li>
 *   <li>{@code effect_start}: fracao do dano no nivel 0, chegando a 1,0 no ultimo nivel (curva de aprendizado;
 *   adaptacao de gameplay, nao canon);</li>
 *   <li>{@code instability_heat}: calor extra do especial no nivel 0, caindo a 0 no ultimo (estabilidade);</li>
 *   <li>{@code perfect_dodge_bonus_ticks}: janela a mais da esquiva perfeita no ultimo nivel (Numbers 1: a obra diz
 *   que as retinas do traje preveem o movimento; o numero e adaptacao).</li>
 * </ul>
 */
public record NumberedGearDef(ResourceLocation item, int number, Optional<ResourceLocation> rank,
        Optional<ResourceLocation> research, int minLimit, List<Integer> mastery, double xpPerDamage,
        double effectStart, double instabilityHeat, int perfectDodgeBonusTicks, int order) {

    public static final Codec<NumberedGearDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(NumberedGearDef::item),
            Codec.intRange(1, 99).fieldOf("number").forGetter(NumberedGearDef::number),
            ResourceLocation.CODEC.optionalFieldOf("rank").forGetter(NumberedGearDef::rank),
            ResourceLocation.CODEC.optionalFieldOf("research").forGetter(NumberedGearDef::research),
            Codec.intRange(0, 100).optionalFieldOf("min_limit", 0).forGetter(NumberedGearDef::minLimit),
            Codec.intRange(0, 10_000_000).listOf().optionalFieldOf("mastery", List.of(0))
                    .forGetter(NumberedGearDef::mastery),
            Codec.doubleRange(0.0, 100.0).optionalFieldOf("xp_per_damage", 1.0).forGetter(NumberedGearDef::xpPerDamage),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("effect_start", 1.0).forGetter(NumberedGearDef::effectStart),
            Codec.doubleRange(0.0, 100.0).optionalFieldOf("instability_heat", 0.0)
                    .forGetter(NumberedGearDef::instabilityHeat),
            Codec.intRange(0, 40).optionalFieldOf("perfect_dodge_bonus_ticks", 0)
                    .forGetter(NumberedGearDef::perfectDodgeBonusTicks),
            Codec.INT.optionalFieldOf("order", 0).forGetter(NumberedGearDef::order)
    ).apply(i, NumberedGearDef::new));

    /** Nivel de dominio (0 = recem-equipado) para o XP dado. */
    public int level(int xp) {
        int level = 0;
        for (int i = 1; i < mastery.size(); i++) {
            if (xp >= mastery.get(i)) {
                level = i;
            }
        }
        return level;
    }

    public int maxLevel() {
        return Math.max(0, mastery.size() - 1);
    }

    /** Fracao do caminho ate o dominio completo (0 no nivel 0, 1 no ultimo). */
    public double progress(int xp) {
        return maxLevel() == 0 ? 1.0 : (double) level(xp) / maxLevel();
    }
}
