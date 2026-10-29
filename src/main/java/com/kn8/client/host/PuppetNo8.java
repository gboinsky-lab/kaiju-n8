package com.kn8.client.host;

import com.kn8.common.host.KaijuFormService;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.special.KaijuNo8Entity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/**
 * T-2 (metodo D, fantoche): Kaiju No. 8 que so existe no cliente e nunca entra no mundo. Segue o jogador
 * transformado a cada tick (posicao, giro, passada) e e desenhado pelo renderer do No. 8 aliado no lugar do jogador:
 * mesma malha, mesmas animacoes de andar/correr e postura dos punhos. Id negativo para nao cruzar com entidades reais.
 */
public final class PuppetNo8 extends KaijuNo8Entity {

    /** Dono sem controle do corpo (T-3): o modelo troca as linhas e os olhos ciano por vermelho. */
    private boolean rampaging;

    PuppetNo8(ClientLevel level, Player player) {
        super(KN8Entities.KAIJU_NO8.get(), level);
        setId(-1_000_000 - player.getId());
        moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        copyRotations(player);
        yBodyRotO = yBodyRot;
        yHeadRotO = yHeadRot;
    }

    /** Tick do cliente: anda junto com o jogador (o controller de movimento le xo/zo e a passada). */
    void follow(Player player) {
        setOldPosAndRot();
        yBodyRotO = yBodyRot;
        yHeadRotO = yHeadRot;
        setPos(player.getX(), player.getY(), player.getZ());
        setDeltaMovement(getX() - xo, getY() - yo, getZ() - zo);
        copyRotations(player);
        setOnGround(player.onGround());
        setShiftKeyDown(player.isShiftKeyDown());
        hurtTime = player.hurtTime;
        rampaging = KaijuFormService.isRampaging(player);
        calculateEntityAnimation(false);
        tickCount++;
    }

    public boolean rampaging() {
        return rampaging;
    }

    /** Giro do jogador neste quadro (a interpolacao usa o anterior guardado no tick). */
    void copyRotations(Player player) {
        setYRot(player.getYRot());
        setXRot(player.getXRot());
        yBodyRot = player.yBodyRot;
        yHeadRot = player.yHeadRot;
    }
}
