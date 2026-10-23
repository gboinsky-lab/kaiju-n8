// src/main/java/com/kn8/common/data/def/KaijuAiDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import java.util.List;
import java.util.Optional;

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
 *   <li>{@code wounds} (PM-2): ferimentos por parte do corpo ({@link Wounds}).</li>
 * </ul>
 */
public record KaijuAiDef(float circleChance, float circleDistance, int circleTicks, float separationRadius,
        float retreatHealth, float retreatDistance, int retreatTicks, int retreatCooldownTicks,
        int recoveryAfterHeavyTicks, float chaseSpeed, float lookSpeed, float rangedBeyond, int rangedPriority,
        Wounds wounds) {

    public static final KaijuAiDef DEFAULT = new KaijuAiDef(0.0F, 3.0F, 30, 0.0F, 0.0F, 10.0F, 40, 300, 0, 1.0F,
            30.0F, 0.0F, 0, Wounds.NONE);

    /**
     * PM-2 (Plano Mestre, secao 4): ferimentos por parte. So partes que a especie tem (nome igual ao {@code parts} do
     * {@code kaiju/<id>.json}; o nucleo tem o sistema dele). {@code max_active} = teto de partes feridas ao mesmo
     * tempo.
     */
    public record Wounds(int maxActive, List<Wound> parts) {

        public static final Wounds NONE = new Wounds(2, List.of());

        public static final Codec<Wounds> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 8).optionalFieldOf("max_active", 2).forGetter(Wounds::maxActive),
                Wound.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(Wounds::parts)
        ).apply(i, Wounds::new));

        public Optional<Wound> of(String part) {
            return parts.stream().filter(wound -> wound.part().equals(part)).findFirst();
        }
    }

    /**
     * Um ferimento: dano acumulado na parte acima de {@code threshold} x vida maxima abre o ferimento por
     * {@code duration_ticks}. Efeitos (todos opcionais): {@code speed_multiplier} (perna), {@code stagger_ticks} ao
     * abrir e {@code stagger_multiplier} nos atordoamentos seguintes (cabeca), {@code disables} = habilidades que nao
     * saem enquanto dura (id da habilidade ou tipo, ex. {@code kn8:leap}), {@code damage_taken} = dano extra na parte
     * ferida (ponto fraco aberto).
     */
    public record Wound(String part, float threshold, int durationTicks, float speedMultiplier, int staggerTicks,
            float staggerMultiplier, List<String> disables, float damageTaken) {

        public static final Codec<Wound> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("part").forGetter(Wound::part),
                Codec.floatRange(0.01F, 1.0F).fieldOf("threshold").forGetter(Wound::threshold),
                DefCodecs.TICKS.optionalFieldOf("duration_ticks", 400).forGetter(Wound::durationTicks),
                Codec.floatRange(0.1F, 1.0F).optionalFieldOf("speed_multiplier", 1.0F)
                        .forGetter(Wound::speedMultiplier),
                DefCodecs.TICKS.optionalFieldOf("stagger_ticks", 0).forGetter(Wound::staggerTicks),
                Codec.floatRange(1.0F, 4.0F).optionalFieldOf("stagger_multiplier", 1.0F)
                        .forGetter(Wound::staggerMultiplier),
                Codec.STRING.listOf().optionalFieldOf("disables", List.of()).forGetter(Wound::disables),
                Codec.floatRange(1.0F, 4.0F).optionalFieldOf("damage_taken", 1.0F).forGetter(Wound::damageTaken)
        ).apply(i, Wound::new));

        /** A habilidade (id ou tipo) esta bloqueada por este ferimento? */
        public boolean blocks(String abilityId, String abilityType) {
            return disables.contains(abilityId) || disables.contains(abilityType);
        }
    }

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
            Codec.intRange(0, 100).optionalFieldOf("ranged_priority", 0).forGetter(KaijuAiDef::rangedPriority),
            Wounds.CODEC.optionalFieldOf("wounds", Wounds.NONE).forGetter(KaijuAiDef::wounds)
    ).apply(i, KaijuAiDef::new));
}
