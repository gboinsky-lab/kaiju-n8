package com.kn8.common.registry;

import com.kn8.KN8Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sons do mod (0.2, Etapa 1). Os arquivos .ogg sao gerados por sintese em {@code tools/audio/gen_sounds.py} e
 * listados em {@code assets/kn8/sounds.json} (variacoes, alcance e legenda); trocar um som e so substituir o .ogg.
 */
public final class KN8Sounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, KN8Constants.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> KAIJU_ROAR = register("kaiju.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAIJU_AMBIENT = register("kaiju.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAIJU_HURT = register("kaiju.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAIJU_DEATH = register("kaiju.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAIJU_STEP = register("kaiju.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAIJU_SLAM = register("kaiju.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAIJU_BITE = register("kaiju.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_SHOT = register("weapon.rifle_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_SHOT = register("weapon.pistol_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADE_SWING = register("weapon.blade_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADE_HEAVY = register("weapon.blade_heavy");
    // 0.2: um som por arma branca (o JSON da arma aponta para eles em "sounds").
    public static final DeferredHolder<SoundEvent, SoundEvent> KNIFE_SWING = register("weapon.knife_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNIFE_HEAVY = register("weapon.knife_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNIFE_HIT = register("weapon.knife_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWORD_SWING = register("weapon.sword_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWORD_HEAVY = register("weapon.sword_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWORD_HIT = register("weapon.sword_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> AXE_SWING = register("weapon.axe_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> AXE_HEAVY = register("weapon.axe_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> AXE_HIT = register("weapon.axe_hit");
    /** 0.5: ataque especial do machado (Golpe Sismico). */
    public static final DeferredHolder<SoundEvent, SoundEvent> AXE_SPECIAL = register("weapon.axe_special");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY = register("weapon.parry");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUARD_BREAK = register("weapon.guard_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> DASH = register("suit.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERHEAT_ALARM = register("suit.overheat_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISMANTLE = register("carcass.dismantle");
    /** Sirene dos alertas de invasao (Etapa 7); registrada desde ja para o datapack poder usar. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SIREN = register("alert.siren");

    private KN8Sounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(KN8Constants.id(name)));
    }
}
