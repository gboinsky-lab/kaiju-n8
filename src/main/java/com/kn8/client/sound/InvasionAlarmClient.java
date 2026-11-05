package com.kn8.client.sound;

import com.kn8.KN8Constants;
import com.kn8.client.invasion.ClientInvasion;
import com.kn8.common.config.ClientConfig;
import com.kn8.common.invasion.Invasion;
import com.kn8.common.invasion.InvasionStateS2C;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * SONS (plano do AUDIO-0, secao 3): sirene da invasao em laco no cliente, guiada pela fase que o servidor ja
 * sincroniza ({@link InvasionStateS2C}, sem pacote novo). Toca no aviso e entre as ondas e se cala na luta e no fim
 * (abaixa aos poucos). Uma instancia so: so cria outra quando a anterior acabou. A propria instancia confere o
 * estado a cada tick, entao para sozinha mesmo que um pacote se perca ou o jogador saia do mundo.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class InvasionAlarmClient {

    private static final SoundEvent SIREN_LOOP = SoundEvent.createVariableRangeEvent(
            KN8Constants.id("alert.siren_loop"));
    /** Ate esta distancia do centro a sirene toca cheia; depois abaixa ate sumir em {@link #SILENT_DISTANCE}. */
    private static final double FULL_DISTANCE = 64.0;
    private static final double SILENT_DISTANCE = 176.0;
    private static final float BASE_VOLUME = 0.8F;
    /** Quanto o volume anda por tick (subir e sumir em ~1 s). */
    private static final float FADE_PER_TICK = 0.05F;

    private static SirenLoop loop;

    private InvasionAlarmClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || target(mc.player) <= 0.0F) {
            return;
        }
        if (loop == null || loop.isStopped()) {
            loop = new SirenLoop();
            mc.getSoundManager().play(loop);
        }
    }

    /** Volume que a sirene deve ter agora (0 = sem sirene). */
    static float target(Player player) {
        InvasionStateS2C state = ClientInvasion.state();
        boolean alarm = state.phase() == Invasion.Phase.WARNING.ordinal()
                || state.phase() == Invasion.Phase.BREAK.ordinal();
        if (!alarm || player == null) {
            return 0.0F;
        }
        double distance = Math.sqrt(player.distanceToSqr(state.center().getCenter()));
        double near = 1.0 - Math.max(0.0, distance - FULL_DISTANCE) / (SILENT_DISTANCE - FULL_DISTANCE);
        return (float) (Math.max(0.0, near) * BASE_VOLUME * ClientConfig.MOD_SOUND_VOLUME.get());
    }

    /** Laco sem posicao (no ouvido): o volume ja cai com a distancia ao centro da invasao. */
    private static final class SirenLoop extends AbstractTickableSoundInstance {

        SirenLoop() {
            super(SIREN_LOOP, SoundSource.HOSTILE, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            float wanted = mc.level == null ? 0.0F : target(mc.player);
            if (volume < wanted) {
                volume = Math.min(wanted, volume + FADE_PER_TICK);
            } else {
                volume = Math.max(wanted, volume - FADE_PER_TICK);
            }
            if (wanted <= 0.0F && volume <= 0.0F) {
                stop();
            }
        }
    }
}
