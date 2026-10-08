// src/main/java/com/kn8/common/data/def/WeaponProfileDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Perfil de animacao e manuseio de uma familia de armas ({@code data/<ns>/kn8/weapon_profile/<id>.json}, 0.5.0-D,
 * Biblioteca v21 Prioridade 3: "WeaponAnimationProfile"). A arma aponta para o perfil pelo campo {@code profile}
 * do {@code weapon/<id>.json}; dano e tempos dos golpes continuam no JSON da arma.
 *
 * <ul>
 *   <li>{@code hands}: uma mao, duas maos (machado pesado) ou duas armas (Hoshina);</li>
 *   <li>{@code stance}: postura com a arma na mao (animacao {@code player.<perfil>.stance}, em laco, so bracos e
 *   tronco; {@code false} = pose do vanilla, como a mira das armas de fogo);</li>
 *   <li>{@code draw}: saque ao trocar para a arma: nenhuma acao durante {@code ticks}, animacao
 *   {@code player.<perfil>.draw} e som;</li>
 *   <li>a guarda (bloqueio) usa {@code player.<perfil>.guard};</li>
 *   <li>{@code reload}: armas de fogo: pente de {@code magazine} tiros e recarga por etapas (cada uma com tempo e
 *   som; animacao {@code player.<perfil>.reload}). 0.5.0-D2 (Miguel: sem municao infinita): a recarga troca o pente
 *   da arma por um pente carregado ({@code magazine_item}) da mochila; o pente vazio fica com o jogador e e
 *   recarregado com a municao ({@code ammo_item}). Criativo nao gasta;</li>
 *   <li>{@code recoil}: coice da camera de quem atira (graus para cima, desvio lateral maximo, ticks para
 *   voltar parte dele) e do corpo ({@code body_kick}, multiplica o coice da animacao de tiro);</li>
 *   <li>{@code first_person}: os golpes da PAL tambem aparecem em primeira pessoa (bracos do modelo);</li>
 *   <li>{@code npc}: pose dos bracos do soldado com essa arma (rifle, pistol, blade, unarmed) e quantos pentes
 *   reserva ele leva ({@code spare_magazines}; acabaram, ele passa para a arma de apoio).</li>
 * </ul>
 */
public record WeaponProfileDef(Hands hands, boolean stance, Draw draw, Optional<Reload> reload,
        Optional<Recoil> recoil, boolean firstPerson, Npc npc) {

    public enum Hands implements StringRepresentable {
        ONE("one"),
        TWO("two"),
        DUAL("dual");

        public static final Codec<Hands> CODEC = StringRepresentable.fromEnum(Hands::values);

        private final String serializedName;

        Hands(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public record Draw(int ticks, Optional<ResourceLocation> sound) {
        public static final Draw NONE = new Draw(0, Optional.empty());
        public static final Codec<Draw> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 100).optionalFieldOf("ticks", 0).forGetter(Draw::ticks),
                ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(Draw::sound)
        ).apply(i, Draw::new));
    }

    /** Uma etapa da recarga (soltar pente, colocar, engatilhar...); o som toca no inicio dela. */
    public record Stage(String name, int ticks, Optional<ResourceLocation> sound) {
        public static final Codec<Stage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Stage::name),
                Codec.intRange(1, 200).fieldOf("ticks").forGetter(Stage::ticks),
                ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(Stage::sound)
        ).apply(i, Stage::new));
    }

    public record Reload(int magazine, List<Stage> stages, Optional<ResourceLocation> magazineItem,
            Optional<ResourceLocation> ammoItem) {
        public static final Codec<Reload> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 1000).fieldOf("magazine").forGetter(Reload::magazine),
                Stage.CODEC.listOf().fieldOf("stages").forGetter(Reload::stages),
                ResourceLocation.CODEC.optionalFieldOf("magazine_item").forGetter(Reload::magazineItem),
                ResourceLocation.CODEC.optionalFieldOf("ammo_item").forGetter(Reload::ammoItem)
        ).apply(i, Reload::new));

        public int totalTicks() {
            return stages.stream().mapToInt(Stage::ticks).sum();
        }
    }

    public record Recoil(float pitch, float yaw, int recoverTicks, float bodyKick) {
        public static final Codec<Recoil> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 45.0F).fieldOf("pitch").forGetter(Recoil::pitch),
                Codec.floatRange(0.0F, 45.0F).optionalFieldOf("yaw", 0.0F).forGetter(Recoil::yaw),
                Codec.intRange(0, 100).optionalFieldOf("recover_ticks", 4).forGetter(Recoil::recoverTicks),
                Codec.floatRange(0.0F, 5.0F).optionalFieldOf("body_kick", 1.0F).forGetter(Recoil::bodyKick)
        ).apply(i, Recoil::new));
    }

    public record Npc(String armPose, int spareMagazines) {
        public static final Npc DEFAULT = new Npc("blade", 0);
        public static final Codec<Npc> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("arm_pose", "blade").forGetter(Npc::armPose),
                Codec.intRange(0, 100).optionalFieldOf("spare_magazines", 0).forGetter(Npc::spareMagazines)
        ).apply(i, Npc::new));
    }

    public static final Codec<WeaponProfileDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Hands.CODEC.optionalFieldOf("hands", Hands.ONE).forGetter(WeaponProfileDef::hands),
            Codec.BOOL.optionalFieldOf("stance", true).forGetter(WeaponProfileDef::stance),
            Draw.CODEC.optionalFieldOf("draw", Draw.NONE).forGetter(WeaponProfileDef::draw),
            Reload.CODEC.optionalFieldOf("reload").forGetter(WeaponProfileDef::reload),
            Recoil.CODEC.optionalFieldOf("recoil").forGetter(WeaponProfileDef::recoil),
            Codec.BOOL.optionalFieldOf("first_person", true).forGetter(WeaponProfileDef::firstPerson),
            Npc.CODEC.optionalFieldOf("npc", Npc.DEFAULT).forGetter(WeaponProfileDef::npc)
    ).apply(i, WeaponProfileDef::new));
}
