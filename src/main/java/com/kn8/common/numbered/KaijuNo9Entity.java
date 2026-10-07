package com.kn8.common.numbered;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.resources.ResourceLocation;
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

    /** Nao ataca no meio do gesto de reviver. */
    @Override
    public boolean startAbility(ResourceLocation id, LivingEntity target) {
        return reviving == null && super.startAbility(id, target);
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
                .triggerableAnim("revive", RawAnimation.begin().thenPlay(kaijuId().getPath() + ".action.revive")));
    }
}
