package com.kn8.client.sound;

import java.util.concurrent.CompletableFuture;

import com.kn8.KN8Constants;
import com.kn8.common.config.ClientConfig;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/**
 * FIX-1 (Miguel, 2026-10-29: "diminuir os sons", com o volume geral em 29%): os sons do mod tocam mais baixo pelo
 * {@code [fx] modSoundVolume} do cliente. Nao basta baixar o volume no sounds.json: o motor corta o volume em 1 e
 * os rugidos tocam com 2-8 so para serem ouvidos de longe. Aqui a altura cai e o alcance (volume acima de 1 vezes
 * a distancia de atenuacao) e mantido pela distancia de atenuacao do som trocado.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ModSoundVolume {

    private ModSoundVolume() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        // Sons que se atualizam por tick (lacos) leem o volume a cada tick: ficam como estao.
        if (sound == null || sound instanceof TickableSoundInstance || sound instanceof Quieter
                || !KN8Constants.MOD_ID.equals(sound.getLocation().getNamespace())) {
            return;
        }
        double factor = ClientConfig.MOD_SOUND_VOLUME.get();
        if (factor >= 1.0) return;
        event.setSound(new Quieter(sound, (float) factor));
    }

    /** Mesmo som com o volume vezes o fator e a distancia de atenuacao corrigida para ouvir do mesmo lugar. */
    private static final class Quieter implements SoundInstance {

        private final SoundInstance delegate;
        private final float factor;
        private Sound sound;
        private float volume;

        Quieter(SoundInstance delegate, float factor) {
            this.delegate = delegate;
            this.factor = factor;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            WeighedSoundEvents events = delegate.resolve(manager);
            Sound original = delegate.getSound();
            // O volume do delegado sorteia o volume do som a cada leitura: le uma vez so.
            float originalVolume = delegate.getVolume();
            // A altura ouvida e o volume cortado em 1; acima de 1 o volume so aumenta o alcance.
            volume = Math.min(originalVolume, 1.0F) * factor;
            if (original == null || original == SoundManager.EMPTY_SOUND) {
                sound = original;
                return events;
            }
            float range = Math.max(originalVolume, 1.0F) * original.getAttenuationDistance();
            int attenuation = Math.max(1, Math.round(range));
            sound = new Sound(original.getLocation(), ConstantFloat.of(1.0F), original.getPitch(),
                    original.getWeight(), original.getType(), original.shouldStream(), original.shouldPreload(),
                    attenuation);
            return events;
        }

        @Override
        public ResourceLocation getLocation() {
            return delegate.getLocation();
        }

        @Override
        public Sound getSound() {
            return sound != null ? sound : delegate.getSound();
        }

        @Override
        public SoundSource getSource() {
            return delegate.getSource();
        }

        @Override
        public boolean isLooping() {
            return delegate.isLooping();
        }

        @Override
        public boolean isRelative() {
            return delegate.isRelative();
        }

        @Override
        public int getDelay() {
            return delegate.getDelay();
        }

        @Override
        public float getVolume() {
            return volume;
        }

        @Override
        public float getPitch() {
            return delegate.getPitch();
        }

        @Override
        public double getX() {
            return delegate.getX();
        }

        @Override
        public double getY() {
            return delegate.getY();
        }

        @Override
        public double getZ() {
            return delegate.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return delegate.getAttenuation();
        }

        @Override
        public boolean canStartSilent() {
            return delegate.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return delegate.canPlaySound();
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
            return delegate.getStream(library, sound, looping);
        }
    }
}
