// src/main/java/com/kn8/common/data/def/SoundProfileDef.java
package com.kn8.common.data.def;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Sons por etapa de um personagem ou da familia de kaiju ({@code data/<ns>/kn8/sound_profile/<id>.json}, SONS,
 * Miguel 2026-10-30: "todos os sons feitos pelo Claude"). O id e o do perfil de soldado especial
 * ({@code special_soldier/<id>}) ou {@code kaiju} (habilidades de todos os kaiju, pelo nome da habilidade). Fica num
 * registro proprio porque o {@code AbilityDef} e o {@code Technique} ja tem 16 campos (limite do codec).
 *
 * <ul>
 *   <li>{@code techniques}/{@code abilities}: por nome ({@code kuuchi}, {@code slam}); a chave {@code *} vale para os
 *   que nao tem a propria. {@code windup} toca no inicio do preparo, {@code release} no tick do golpe/disparo do
 *   JSON (nunca no inicio da animacao);</li>
 *   <li>{@code travel} (AJUSTES-2): laco que segue o projetil da tecnica/habilidade enquanto ele voa (raio,
 *   corte que voa, espiral, granada do canhao, lanca de gelo); toca no cliente, pelo id sincronizado no
 *   projetil;</li>
 *   <li>{@code volume}/{@code pitch}: da etapa (o kaiju ainda multiplica o tom pela voz do tamanho dele).</li>
 * </ul>
 * Sem perfil, sem a etapa ou sem a chave, fica o som de antes (golpe de espada, aviso generico).
 */
public record SoundProfileDef(Map<String, Cue> techniques, Map<String, Cue> abilities) {

    public static final String ANY = "*";

    public record Cue(Optional<ResourceLocation> windup, Optional<ResourceLocation> release, float volume,
            float pitch, Optional<ResourceLocation> travel) {

        public static final Codec<Cue> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("windup").forGetter(Cue::windup),
                ResourceLocation.CODEC.optionalFieldOf("release").forGetter(Cue::release),
                Codec.floatRange(0.0F, 16.0F).optionalFieldOf("volume", 1.0F).forGetter(Cue::volume),
                Codec.floatRange(0.25F, 4.0F).optionalFieldOf("pitch", 1.0F).forGetter(Cue::pitch),
                ResourceLocation.CODEC.optionalFieldOf("travel").forGetter(Cue::travel)
        ).apply(i, Cue::new));
    }

    public static final Codec<SoundProfileDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Cue.CODEC).optionalFieldOf("techniques", Map.of())
                    .forGetter(SoundProfileDef::techniques),
            Codec.unboundedMap(Codec.STRING, Cue.CODEC).optionalFieldOf("abilities", Map.of())
                    .forGetter(SoundProfileDef::abilities)
    ).apply(i, SoundProfileDef::new));

    public Optional<Cue> technique(String name) {
        return Optional.ofNullable(techniques.getOrDefault(name, techniques.get(ANY)));
    }

    public Optional<Cue> ability(String name) {
        return Optional.ofNullable(abilities.getOrDefault(name, abilities.get(ANY)));
    }
}
