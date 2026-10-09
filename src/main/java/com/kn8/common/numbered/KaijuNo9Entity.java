package com.kn8.common.numbered;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Kaiju No. 9 (0.2, Etapa 8): humanoide de 2 m, primeiro numerado. Luta com as habilidades do JSON da especie como
 * qualquer kaiju; o que ele tem a mais (reviver carcacas, comandar kaiju, fugir) fica no {@link No9Service}. O
 * estado abaixo e so do servidor e nao e salvo (ao recarregar, ele recomeca sem reviver nada em andamento).
 */
public class KaijuNo9Entity extends KaijuEntity {

    UUID reviving;
    long reviveEndsAt;
    long nextReviveAt;
    boolean fled;
    final List<UUID> revived = new ArrayList<>();
    /** 0.3: carcacas na fila da ressurreicao em massa e o tick do proximo levante. */
    final List<UUID> massQueue = new ArrayList<>();
    long nextMassAt;
    /** 0.3: jogadores que causaram dano nele (so eles ganham o merito da fuga). */
    final Set<UUID> attackers = new HashSet<>();
    /** 0.7-E (formas do No. 9): ja mudou de forma (nao muda de novo) e a absorcao em andamento. */
    boolean transformed;
    UUID absorbing;
    int absorbRule;
    long absorbEndsAt;
    long nextAbsorbAt;
    /** 0.7-E: pele endurecida ate este tick; proximo endurecimento so depois de {@code hardenReadyAt}. */
    long hardenedUntil;
    long hardenReadyAt;

    public KaijuNo9Entity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive()) {
            No9Service.tick(this);
        }
    }

    /** Nao ataca no meio do gesto de reviver nem da absorcao. */
    @Override
    public boolean startAbility(ResourceLocation id, LivingEntity target) {
        return reviving == null && absorbing == null && super.startAbility(id, target);
    }

    /**
     * 0.7-E: endurecimento da pele (numbered/<id>.json {@code hardening}) reduz o dano depois da armadura. Fica aqui e
     * nao no {@code actuallyHurt}: no NeoForge ele ignora o valor recebido e usa o conteiner de dano, que guarda o
     * resultado deste metodo como reducao da armadura.
     */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        float afterArmor = super.getDamageAfterArmorAbsorb(source, amount);
        return level().isClientSide() ? afterArmor : No9Service.harden(this, afterArmor);
    }

    public boolean isAbsorbing() {
        return absorbing != null;
    }

    public boolean isHardened() {
        return level().getGameTime() < hardenedUntil;
    }

    /** Endurece a pele agora por {@code ticks} (comandos e GameTests; em luta e pela chance do JSON). */
    public void hardenFor(int ticks) {
        hardenedUntil = level().getGameTime() + ticks;
        hardenReadyAt = hardenedUntil;
    }

    public void recordAttacker(Player player) {
        attackers.add(player.getUUID());
    }

    public boolean isReviving() {
        return reviving != null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        // Controle proprio para o gesto de reviver (o "action" do kaiju so conhece as habilidades do JSON).
        controllers.add(new AnimationController<>(this, "special", 0, state -> PlayState.STOP)
                .triggerableAnim("revive", RawAnimation.begin().thenPlay(kaijuId().getPath() + ".action.revive"))
                .triggerableAnim("absorb", RawAnimation.begin().thenPlay(kaijuId().getPath() + ".action.absorb")));
    }
}
