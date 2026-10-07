// src/main/java/com/kn8/common/data/def/WeaponDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 *
 * <p>{@code special} (0.5, opcional): ataque especial da arma (tecla R), com recarga e custo proprios. E das armas
 * especiais: o jogador usa, e os soldados especiais vao usar o mesmo resolvedor ({@code SpecialAttacks}).</p>
 */
public record WeaponDef(ResourceLocation item, float baseDamage, float reach, Style style,
        Map<String, Action> actions, List<Float> combo, Map<String, ResourceLocation> sounds,
        Optional<Special> special) {

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

    /** Tipos de ataque especial. Cada tipo novo ganha um resolvedor em {@code SpecialAttacks}. */
    public enum SpecialType implements StringRepresentable {
        /** Golpe no chao a frente: onda em area que fere, empurra e atordoa (machado). */
        GROUND_SLAM("ground_slam");

        public static final Codec<SpecialType> CODEC = StringRepresentable.fromEnum(SpecialType::values);

        private final String serializedName;

        SpecialType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /**
     * Ataque especial. {@code id} da o nome ({@code kn8.weapon.special.<id>}); o dano e
     * {@code base_damage x multiplier} (com o Release de quem usa); o centro da area fica {@code forward} blocos a
     * frente e atinge quem tem a borda da hitbox a ate {@code radius}. {@code stagger_ticks} so vale em Yoju (como o
     * parry). {@code vfx}, {@code camera_shake} e {@code sound} saem no tick de impacto.
     */
    public record Special(String id, SpecialType type, float multiplier, int durationTicks, int impactTick,
            float radius, float forward, float staminaCost, float heatCost, int cooldownTicks, float knockback,
            int staggerTicks, float cameraShake, List<AbilityDef.Vfx> vfx, Optional<ResourceLocation> sound) {
        public static final Codec<Special> CODEC = RecordCodecBuilder.<Special>create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Special::id),
                SpecialType.CODEC.fieldOf("type").forGetter(Special::type),
                DefCodecs.MULTIPLIER.fieldOf("multiplier").forGetter(Special::multiplier),
                Codec.intRange(1, 200).fieldOf("duration_ticks").forGetter(Special::durationTicks),
                Codec.intRange(0, 199).fieldOf("impact_tick").forGetter(Special::impactTick),
                Codec.floatRange(0.5F, 32.0F).fieldOf("radius").forGetter(Special::radius),
                Codec.floatRange(0.0F, 16.0F).optionalFieldOf("forward", 0.0F).forGetter(Special::forward),
                Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("stamina_cost", 0.0F).forGetter(Special::staminaCost),
                Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("heat_cost", 0.0F).forGetter(Special::heatCost),
                Codec.intRange(0, 72000).fieldOf("cooldown_ticks").forGetter(Special::cooldownTicks),
                Codec.floatRange(0.0F, 10.0F).optionalFieldOf("knockback", 0.0F).forGetter(Special::knockback),
                Codec.intRange(0, 400).optionalFieldOf("stagger_ticks", 0).forGetter(Special::staggerTicks),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("camera_shake", 0.0F).forGetter(Special::cameraShake),
                AbilityDef.Vfx.CODEC.listOf().optionalFieldOf("vfx", List.of()).forGetter(Special::vfx),
                ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(Special::sound)
        ).apply(i, Special::new)).validate(special -> special.impactTick() < special.durationTicks()
                ? DataResult.success(special)
                : DataResult.error(() -> "special.impact_tick " + special.impactTick()
                        + " precisa ser menor que duration_ticks " + special.durationTicks()));
    }

    public static final Codec<WeaponDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(WeaponDef::item),
            Codec.floatRange(0.0F, 1000.0F).fieldOf("base_damage").forGetter(WeaponDef::baseDamage),
            Codec.floatRange(0.5F, 256.0F).fieldOf("reach").forGetter(WeaponDef::reach),
            Style.CODEC.fieldOf("style").forGetter(WeaponDef::style),
            Codec.unboundedMap(Codec.STRING, Action.CODEC).fieldOf("actions").forGetter(WeaponDef::actions),
            DefCodecs.MULTIPLIER.listOf().optionalFieldOf("combo", List.of()).forGetter(WeaponDef::combo),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("sounds", Map.of())
                    .forGetter(WeaponDef::sounds),
            Special.CODEC.optionalFieldOf("special").forGetter(WeaponDef::special)
    ).apply(i, WeaponDef::new));
}
