// src/main/java/com/kn8/common/attribute/PowerView.java
package com.kn8.common.attribute;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que o DONO enxerga do proprio poder (HUD, M6). Calculado no servidor e enviado so a ele; no cliente fica no
 * attachment {@code kn8:power_view} do jogador local. Valores arredondados para a HUD.
 *
 * <p>0.5.0: {@code trained} e o limite pessoal; {@code active} a % liberada na tecla; {@code excess} quanto passa do
 * limite; {@code suit} se o traje esta vestido (sem traje nao ha Release); atributos do corpo em niveis.</p>
 */
public record PowerView(int trained, int effective, int cap, int excess, float stamina, float maxStamina, float heat,
        int heatMax, int heatStage, float energy, int control, int releaseXp, int xpToNext, boolean panic,
        boolean winded, int active, boolean suit, boolean talentRare, int strength, int speed, int resistance,
        int agility, boolean fatigued) {

    public static final PowerView EMPTY = new PowerView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, false, false, 0,
            false, false, 0, 0, 0, 0, false);

    // StreamCodec.composite aceita no maximo 6 campos por vez: grupos aninhados.
    private record Release(int trained, int effective, int cap, int excess, int releaseXp, int xpToNext) {
        static final StreamCodec<ByteBuf, Release> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Release::trained, ByteBufCodecs.VAR_INT, Release::effective,
                ByteBufCodecs.VAR_INT, Release::cap, ByteBufCodecs.VAR_INT, Release::excess,
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

    private record Flags(int heatMax, boolean panic, boolean winded, int active, boolean suit, boolean talentRare) {
        static final StreamCodec<ByteBuf, Flags> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Flags::heatMax, ByteBufCodecs.BOOL, Flags::panic,
                ByteBufCodecs.BOOL, Flags::winded, ByteBufCodecs.VAR_INT, Flags::active,
                ByteBufCodecs.BOOL, Flags::suit, ByteBufCodecs.BOOL, Flags::talentRare,
                Flags::new);
    }

    private record Body(int strength, int speed, int resistance, int agility, boolean fatigued) {
        static final StreamCodec<ByteBuf, Body> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Body::strength, ByteBufCodecs.VAR_INT, Body::speed,
                ByteBufCodecs.VAR_INT, Body::resistance, ByteBufCodecs.VAR_INT, Body::agility,
                ByteBufCodecs.BOOL, Body::fatigued,
                Body::new);
    }

    public static final StreamCodec<ByteBuf, PowerView> STREAM_CODEC = StreamCodec.composite(
            Release.CODEC, view -> new Release(view.trained(), view.effective(), view.cap(), view.excess(),
                    view.releaseXp(), view.xpToNext()),
            Resources.CODEC, view -> new Resources(view.stamina(), view.maxStamina(), view.heat(), view.heatStage(),
                    view.energy(), view.control()),
            Flags.CODEC, view -> new Flags(view.heatMax(), view.panic(), view.winded(), view.active(), view.suit(),
                    view.talentRare()),
            Body.CODEC, view -> new Body(view.strength(), view.speed(), view.resistance(), view.agility(),
                    view.fatigued()),
            (release, resources, flags, body) -> new PowerView(release.trained(), release.effective(),
                    release.cap(), release.excess(), resources.stamina(), resources.maxStamina(), resources.heat(),
                    flags.heatMax(), resources.heatStage(), resources.energy(), resources.control(),
                    release.releaseXp(), release.xpToNext(), flags.panic(), flags.winded(), flags.active(),
                    flags.suit(), flags.talentRare(), body.strength(), body.speed(), body.resistance(),
                    body.agility(), body.fatigued()));
}
