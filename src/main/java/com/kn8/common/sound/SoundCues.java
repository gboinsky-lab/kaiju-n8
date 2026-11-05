package com.kn8.common.sound;

import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SoundProfileDef;
import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * SONS: toca o som de cada etapa pelo {@code sound_profile} (preparo e golpe/disparo). Quem chama e o servidor, no
 * mesmo ponto do efeito visual da etapa (inicio do preparo, tick do golpe do JSON), entao som e desenho chegam
 * juntos. Sem perfil ou sem a etapa devolve {@code false} e quem chamou toca o som de antes.
 */
public final class SoundCues {

    /** Perfil das habilidades de todos os kaiju (a especie pode ter o proprio, que vem antes). */
    public static final ResourceLocation KAIJU = KN8Constants.id("kaiju");

    private SoundCues() {
    }

    public enum Stage {
        WINDUP,
        RELEASE
    }

    /**
     * Tecnica de soldado especial (perfil ativo: a forma/traje de agora). Devolve {@code true} se a tecnica tem perfil,
     * mesmo sem som nesta etapa: o preparo sem som fica em silencio (antes toda tecnica comecava com o corte de espada).
     */
    public static boolean technique(Entity source, ResourceLocation profile, String technique, Stage stage,
            float volume) {
        Optional<SoundProfileDef.Cue> cue = Optional.ofNullable(KN8Data.SOUND_PROFILE.server().get(profile))
                .flatMap(def -> def.technique(technique));
        cue.ifPresent(c -> play(source, c, stage, SoundSource.NEUTRAL, volume, 1.0F));
        return cue.isPresent();
    }

    /** Habilidade de kaiju: perfil da especie e depois o geral; o tom segue a voz do tamanho dele. */
    public static boolean ability(KaijuEntity kaiju, String ability, Stage stage, float volume) {
        Optional<SoundProfileDef.Cue> cue = Optional.ofNullable(KN8Data.SOUND_PROFILE.server().get(kaiju.kaijuId()))
                .flatMap(def -> def.ability(ability))
                .filter(c -> sound(c, stage).isPresent())
                .or(() -> Optional.ofNullable(KN8Data.SOUND_PROFILE.server().get(KAIJU))
                        .flatMap(def -> def.ability(ability)));
        return cue.map(c -> play(kaiju, c, stage, SoundSource.HOSTILE, volume, kaiju.getVoicePitch()))
                .orElse(false);
    }

    /** AJUSTES-2: laco de voo do tiro da tecnica (perfil ativo de quem atirou), se o perfil tiver. */
    public static Optional<ResourceLocation> techniqueTravel(ResourceLocation profile, String technique) {
        return Optional.ofNullable(KN8Data.SOUND_PROFILE.server().get(profile))
                .flatMap(def -> def.technique(technique)).flatMap(SoundProfileDef.Cue::travel);
    }

    /** AJUSTES-2: laco de voo do projetil da habilidade (perfil da especie e depois o geral). */
    public static Optional<ResourceLocation> abilityTravel(KaijuEntity kaiju, String ability) {
        return Optional.ofNullable(KN8Data.SOUND_PROFILE.server().get(kaiju.kaijuId()))
                .flatMap(def -> def.ability(ability)).flatMap(SoundProfileDef.Cue::travel)
                .or(() -> Optional.ofNullable(KN8Data.SOUND_PROFILE.server().get(KAIJU))
                        .flatMap(def -> def.ability(ability)).flatMap(SoundProfileDef.Cue::travel));
    }

    private static Optional<ResourceLocation> sound(SoundProfileDef.Cue cue, Stage stage) {
        return stage == Stage.WINDUP ? cue.windup() : cue.release();
    }

    private static boolean play(Entity source, SoundProfileDef.Cue cue, Stage stage, SoundSource category,
            float volume, float pitch) {
        Optional<ResourceLocation> id = sound(cue, stage);
        if (id.isEmpty() || source.level().isClientSide()) {
            return false;
        }
        source.level().playSound(null, source.getX(), source.getEyeY(), source.getZ(), event(id.get()), category,
                volume * cue.volume(), pitch * cue.pitch());
        return true;
    }

    /** Id de som do JSON: o registrado ou um evento de alcance variavel com o id (sons so do sounds.json). */
    public static SoundEvent event(ResourceLocation id) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
    }
}
