// src/main/java/com/kn8/common/data/def/KaijuAiDef.java
package com.kn8.common.data.def;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Perfil tatico de uma especie de kaiju ({@code data/<ns>/kn8/kaiju_ai/<id do kaiju>.json}, IA-4; plano de IA,
 * Parte 10). Separado do {@code kaiju/<id>.json} (o codec dele ja tem 16 campos). Nao muda vida, dano nem armadura,
 * so decisoes; especie sem arquivo luta como antes.
 *
 * <ul>
 *   <li>{@code circle_chance}/{@code circle_distance}/{@code circle_ticks} (Yoju): depois de um golpe, chance de
 *   circular o alvo (a {@code circle_distance} blocos alem do alcance) em vez de grudar nele;</li>
 *   <li>{@code separation_radius} (Yoju em grupo): com outro kaiju a essa distancia, cada um chega por um angulo
 *   diferente (cerca sem empilhar);</li>
 *   <li>{@code retreat_health}/{@code retreat_distance}/{@code retreat_ticks}/{@code retreat_cooldown_ticks}: com a
 *   vida abaixo da fracao, recua um tempo (Yoju diante de alvo forte);</li>
 *   <li>{@code recovery_after_heavy_ticks} (Honju, No. 10): depois de um golpe pesado fica parado esse tempo (janela
 *   de reacao para quem luta com ele);</li>
 *   <li>{@code chase_speed}: multiplicador da velocidade de perseguicao (No. 10: pressao);</li>
 *   <li>{@code look_speed}: graus por tick que gira para o alvo (grandes giram devagar);</li>
 *   <li>{@code ranged_beyond}/{@code ranged_priority}: com o alvo a mais que essa distancia, prioridade extra dos
 *   golpes a distancia (teia da Trichonephila, raio do Honju).</li>
 * </ul>
 */
public record KaijuAiDef(float circleChance, float circleDistance, int circleTicks, float separationRadius,
        float retreatHealth, float retreatDistance, int retreatTicks, int retreatCooldownTicks,
        int recoveryAfterHeavyTicks, float chaseSpeed, float lookSpeed, float rangedBeyond, int rangedPriority) {

    public static final KaijuAiDef DEFAULT = new KaijuAiDef(0.0F, 3.0F, 30, 0.0F, 0.0F, 10.0F, 40, 300, 0, 1.0F,
            30.0F, 0.0F, 0);

    public static final Codec<KaijuAiDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("circle_chance", 0.0F).forGetter(KaijuAiDef::circleChance),
            Codec.floatRange(0.0F, 32.0F).optionalFieldOf("circle_distance", 3.0F)
                    .forGetter(KaijuAiDef::circleDistance),
            DefCodecs.TICKS.optionalFieldOf("circle_ticks", 30).forGetter(KaijuAiDef::circleTicks),
            Codec.floatRange(0.0F, 32.0F).optionalFieldOf("separation_radius", 0.0F)
                    .forGetter(KaijuAiDef::separationRadius),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("retreat_health", 0.0F).forGetter(KaijuAiDef::retreatHealth),
            Codec.floatRange(0.0F, 64.0F).optionalFieldOf("retreat_distance", 10.0F)
                    .forGetter(KaijuAiDef::retreatDistance),
            DefCodecs.TICKS.optionalFieldOf("retreat_ticks", 40).forGetter(KaijuAiDef::retreatTicks),
            DefCodecs.TICKS.optionalFieldOf("retreat_cooldown_ticks", 300).forGetter(KaijuAiDef::retreatCooldownTicks),
            DefCodecs.TICKS.optionalFieldOf("recovery_after_heavy_ticks", 0)
                    .forGetter(KaijuAiDef::recoveryAfterHeavyTicks),
            Codec.floatRange(0.1F, 4.0F).optionalFieldOf("chase_speed", 1.0F).forGetter(KaijuAiDef::chaseSpeed),
            Codec.floatRange(1.0F, 90.0F).optionalFieldOf("look_speed", 30.0F).forGetter(KaijuAiDef::lookSpeed),
            Codec.floatRange(0.0F, 128.0F).optionalFieldOf("ranged_beyond", 0.0F).forGetter(KaijuAiDef::rangedBeyond),
            Codec.intRange(0, 100).optionalFieldOf("ranged_priority", 0).forGetter(KaijuAiDef::rangedPriority)
    ).apply(i, KaijuAiDef::new));
}
