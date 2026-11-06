// src/main/java/com/kn8/common/data/def/HitEffect.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Etapa 7-B (Miguel, 2026-11-06, respostas da {@code docs/ETAPA7_AJUSTES.md}): efeito de quem leva o golpe ou o tiro,
 * servido pelas armas ({@code on_hit} do {@code weapon/<id>.json}) e pelas tecnicas dos especiais ({@code on_hit}
 * do {@code special_soldier}, tecnica -> efeito).
 *
 * <ul>
 *   <li>{@code slow_ticks}/{@code slow_level}: Lentidao (supressao da minigun da Rin: nivel 1 = -15%).</li>
 *   <li>{@code poison_ticks}/{@code poison_per_second}: veneno de verdade (flechas do Haruichi): dano por segundo
 *       que ignora a armadura; em quem nao e kaiju vira o Veneno vanilla.</li>
 *   <li>{@code mark_ticks}/{@code mark_bonus}: marca que faz o kaiju levar mais dano de todos (marcacao do Narumi
 *       +15%, analise da Konomi +8%); a maior marca ativa vale.</li>
 *   <li>{@code next_hit_bonus}/{@code next_hit_ticks}: o proximo golpe de qualquer um no kaiju bate mais (marca do
 *       Ryo +20%).</li>
 * </ul>
 */
public record HitEffect(int slowTicks, int slowLevel, int poisonTicks, float poisonPerSecond, int markTicks,
        float markBonus, float nextHitBonus, int nextHitTicks) {

    public static final Codec<HitEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 600).optionalFieldOf("slow_ticks", 0).forGetter(HitEffect::slowTicks),
            Codec.intRange(1, 7).optionalFieldOf("slow_level", 1).forGetter(HitEffect::slowLevel),
            Codec.intRange(0, 600).optionalFieldOf("poison_ticks", 0).forGetter(HitEffect::poisonTicks),
            Codec.floatRange(0.0F, 200.0F).optionalFieldOf("poison_per_second", 0.0F)
                    .forGetter(HitEffect::poisonPerSecond),
            Codec.intRange(0, 1200).optionalFieldOf("mark_ticks", 0).forGetter(HitEffect::markTicks),
            Codec.floatRange(0.0F, 2.0F).optionalFieldOf("mark_bonus", 0.0F).forGetter(HitEffect::markBonus),
            Codec.floatRange(0.0F, 2.0F).optionalFieldOf("next_hit_bonus", 0.0F).forGetter(HitEffect::nextHitBonus),
            Codec.intRange(0, 1200).optionalFieldOf("next_hit_ticks", 200).forGetter(HitEffect::nextHitTicks)
    ).apply(i, HitEffect::new));

    public static final HitEffect NONE = new HitEffect(0, 1, 0, 0.0F, 0, 0.0F, 0.0F, 200);

    public boolean isEmpty() {
        return slowTicks <= 0 && (poisonTicks <= 0 || poisonPerSecond <= 0.0F) && (markTicks <= 0 || markBonus <= 0.0F)
                && nextHitBonus <= 0.0F;
    }
}
