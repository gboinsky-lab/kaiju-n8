package com.kn8.common.sound;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/**
 * AJUSTES-2 (Miguel 2026-11-05: "acrescente os sons de voo"): projetil com um laco de som enquanto voa. O servidor
 * escolhe o som no disparo (perfil de som ou o tipo do tiro) e o grava num dado sincronizado; cada cliente toca o laco
 * preso ao projetil ({@code client/sound/ProjectileFlightSounds}), sem pacote novo.
 */
public interface TravelSoundSource {

    /** Som do voo; vazio = projetil sem laco (bala comum, tiro rapido demais para se ouvir). */
    Optional<ResourceLocation> travelSound();
}
