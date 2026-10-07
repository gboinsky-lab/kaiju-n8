// src/main/java/com/kn8/common/data/def/WeaponDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Arma ({@code data/<ns>/kn8/weapon/<id>.json}). Cada acao tem multiplicador, duracao e tick de impacto: e o
 * contrato da {@code ActionTimeline} validado no PT7 (GDD secao 9).
 *
 * <p>{@code sounds} (0.2, opcional): id do som por momento ({@code swing} golpe leve, {@code heavy} golpe pesado,
 * {@code hit} acerto, {@code shot} disparo). Sem a chave, o combate usa o som generico; qualquer id serve, ate um
 * som de resource pack que o mod nao registra.</p>
 *
 * <p>A patente que libera a arma fica so nos {@code unlocks} das patentes (0.3: o antigo {@code required_rank}
 * daqui divergia deles e foi removido; campo extra num JSON antigo e ignorado).</p>
 */
public record WeaponDef(ResourceLocation item, float baseDamage, float reach, Style style,
        Map<String, Action> actions, List<Float> combo, Map<String, ResourceLocation> sounds) {

    /** Familia da arma (decide animacoes e regras de combate). */
    public enum Style implements StringRepresentable {
        BLADE("blade"),
        HEAVY("heavy"),
        FIREARM("firearm"),
        CANNON("cannon");

        public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);

        private final String serializedName;

        Style(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** Uma acao (leve, pesado, carregado...). {@code impact_tick} deve ser menor que {@code duration_ticks}. */
    public record Action(float multiplier, int durationTicks, int impactTick) {
        public static final Codec<Action> CODEC = RecordCodecBuilder.<Action>create(i -> i.group(
                DefCodecs.MULTIPLIER.fieldOf("multiplier").forGetter(Action::multiplier),
                Codec.intRange(1, 200).fieldOf("duration_ticks").forGetter(Action::durationTicks),
                Codec.intRange(-1, 199).fieldOf("impact_tick").forGetter(Action::impactTick)
        ).apply(i, Action::new)).validate(action -> action.impactTick() < action.durationTicks()
                ? DataResult.success(action)
                : DataResult.error(() -> "impact_tick " + action.impactTick()
                        + " precisa ser menor que duration_ticks " + action.durationTicks()));
    }

    public static final Codec<WeaponDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(WeaponDef::item),
            Codec.floatRange(0.0F, 1000.0F).fieldOf("base_damage").forGetter(WeaponDef::baseDamage),
            Codec.floatRange(0.5F, 256.0F).fieldOf("reach").forGetter(WeaponDef::reach),
            Style.CODEC.fieldOf("style").forGetter(WeaponDef::style),
            Codec.unboundedMap(Codec.STRING, Action.CODEC).fieldOf("actions").forGetter(WeaponDef::actions),
            DefCodecs.MULTIPLIER.listOf().optionalFieldOf("combo", List.of()).forGetter(WeaponDef::combo),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("sounds", Map.of())
                    .forGetter(WeaponDef::sounds)
    ).apply(i, WeaponDef::new));
}
