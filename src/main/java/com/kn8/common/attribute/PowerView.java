// src/main/java/com/kn8/common/attribute/PowerView.java
package com.kn8.common.attribute;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que o DONO enxerga do proprio poder (HUD, M6). Calculado no servidor e enviado so a ele; no cliente fica no
 * attachment {@code kn8:power_view} do jogador local. Valores arredondados para a HUD.
 */
public record PowerView(int trained, int effective, int cap, int surge, float stamina, float maxStamina, float heat,
        int heatMax, int heatStage, float energy, int control, int releaseXp, int xpToNext, boolean panic,
        boolean winded) {

    public static final PowerView EMPTY = new PowerView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, false, false);

    // StreamCodec.composite aceita no maximo 6 campos por vez: dois grupos aninhados.
    private record Release(int trained, int effective, int cap, int surge, int releaseXp, int xpToNext) {
        static final StreamCodec<ByteBuf, Release> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Release::trained, ByteBufCodecs.VAR_INT, Release::effective,
                ByteBufCodecs.VAR_INT, Release::cap, ByteBufCodecs.VAR_INT, Release::surge,
                ByteBufCodecs.VAR_INT, Release::releaseXp, ByteBufCodecs.VAR_INT, Release::xpToNext,
                Release::new);
    }

    private record Resources(float stamina, float maxStamina, float heat, int heatStage, float energy,
            int control) {
        static final StreamCodec<ByteBuf, Resources> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Resources::stamina, ByteBufCodecs.FLOAT, Resources::maxStamina,
                ByteBufCodecs.FLOAT, Resources::heat, ByteBufCodecs.VAR_INT, Resources::heatStage,
                ByteBufCodecs.FLOAT, Resources::energy, ByteBufCodecs.VAR_INT, Resources::control,
                Resources::new);
    }

    public static final StreamCodec<ByteBuf, PowerView> STREAM_CODEC = StreamCodec.composite(
            Release.CODEC, view -> new Release(view.trained(), view.effective(), view.cap(), view.surge(),
                    view.releaseXp(), view.xpToNext()),
            Resources.CODEC, view -> new Resources(view.stamina(), view.maxStamina(), view.heat(), view.heatStage(),
                    view.energy(), view.control()),
            ByteBufCodecs.VAR_INT, PowerView::heatMax,
            ByteBufCodecs.BOOL, PowerView::panic,
            ByteBufCodecs.BOOL, PowerView::winded,
            (release, resources, heatMax, panic, winded) -> new PowerView(release.trained(), release.effective(),
                    release.cap(), release.surge(), resources.stamina(), resources.maxStamina(), resources.heat(),
                    heatMax, resources.heatStage(), resources.energy(), resources.control(), release.releaseXp(),
                    release.xpToNext(), panic, winded));
}
