// src/main/java/com/kn8/common/vfx/VfxKeyframes.java
package com.kn8.common.vfx;

import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;

/**
 * VFX-1 (diretriz KaijuVFX §4: "use GeckoLib para sincronizar os momentos visuais com as animacoes"): marcas
 * {@code particle_effects} das animacoes ({@code "effect"} = chave da receita do perfil visual, {@code "locator"} =
 * de onde sai) viram efeito no quadro certo, so no cliente. O controller e criado no codigo comum; quem desenha e o
 * cliente, que instala o {@link Handler} (o comum nunca referencia o pacote do cliente, regra 4).
 */
public final class VfxKeyframes {

    /** Chamado no cliente quando uma animacao passa por uma marca de efeito. */
    @FunctionalInterface
    public interface Handler {
        void play(Entity entity, String effect, String locator);
    }

    private static final Handler NONE = (entity, effect, locator) -> { };
    private static volatile Handler handler = NONE;

    private VfxKeyframes() {
    }

    public static void install(Handler clientHandler) {
        handler = clientHandler;
    }

    public static <T extends Entity & GeoAnimatable> AnimationController<T> attach(AnimationController<T> controller) {
        return controller.setParticleKeyframeHandler(event -> handler.play(event.getAnimatable(),
                event.getKeyframeData().getEffect(), event.getKeyframeData().getLocator()));
    }
}
