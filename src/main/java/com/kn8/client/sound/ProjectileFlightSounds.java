package com.kn8.client.sound;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.kn8.KN8Constants;
import com.kn8.common.config.ClientConfig;
import com.kn8.common.sound.TravelSoundSource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * AJUSTES-2 (Miguel 2026-11-05: "acrescente os sons de voo"): laco preso a cada projetil com som de voo
 * ({@link TravelSoundSource}: raio de energia, corte que voa, espiral do Isao, granada do canhao, lanca de gelo). O id
 * vem num dado sincronizado do projetil, entao nao ha pacote novo; o laco segue a posicao do projetil e para quando
 * ele some. Um laco por projetil, ate {@link #MAX_LOOPS} ao mesmo tempo (rajadas em leque nao enchem o canal).
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ProjectileFlightSounds {

    /** Teto de lacos de voo tocando juntos (o motor tem 247 canais; o leque do Flame Shower solta 6 de uma vez). */
    private static final int MAX_LOOPS = 12;
    /** Volume base do laco (o tiro e o impacto ja tem o proprio som; o voo fica por baixo). */
    private static final float BASE_VOLUME = 0.7F;
    /** Projeteis ja com laco (pelo id da entidade no cliente); limpo quando a entidade some. */
    private static final Set<Integer> PLAYING = new HashSet<>();

    private ProjectileFlightSounds() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            PLAYING.clear();
            return;
        }
        // Se o motor nao tocou o laco (canais cheios), o tick dele nunca roda: tira os projeteis que ja sumiram.
        PLAYING.removeIf(id -> mc.level.getEntity(id) == null);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof TravelSoundSource source) || PLAYING.contains(entity.getId())
                    || PLAYING.size() >= MAX_LOOPS) {
                continue;
            }
            Optional<ResourceLocation> sound = source.travelSound();
            // O dado chega logo depois da entidade: sem som nos primeiros ticks, tenta de novo no proximo.
            if (sound.isEmpty()) {
                continue;
            }
            PLAYING.add(entity.getId());
            mc.getSoundManager().play(new FlightLoop(entity, SoundEvent.createVariableRangeEvent(sound.get())));
        }
    }

    /** Laco que acompanha o projetil; entra rapido e para quando ele e removido. */
    private static final class FlightLoop extends AbstractTickableSoundInstance {

        private final Entity projectile;

        FlightLoop(Entity projectile, SoundEvent sound) {
            super(sound, SoundSource.HOSTILE, RandomSource.create(projectile.getId()));
            this.projectile = projectile;
            this.looping = true;
            this.delay = 0;
            this.volume = BASE_VOLUME * ClientConfig.MOD_SOUND_VOLUME.get().floatValue();
            this.pitch = 0.95F + random.nextFloat() * 0.1F;
            follow();
        }

        private void follow() {
            this.x = projectile.getX();
            this.y = projectile.getY();
            this.z = projectile.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (projectile.isRemoved()) {
                PLAYING.remove(projectile.getId());
                stop();
                return;
            }
            follow();
        }
    }
}
