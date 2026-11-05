package com.kn8.common.kaiju;

import java.util.List;

import com.kn8.common.anim.Locomotion;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.vfx.VfxKeyframes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * AJUSTES-2 (Miguel 2026-11-05: "pode criar uma forma do n8 vermelha sem controle npc, uma pequena e uma gigante"):
 * o Kaiju No. 8 que perdeu o controle, como kaiju inimigo (o No. 8 aliado continua um soldado especial que nao perde o
 * controle). Arte do No. 8 com a textura vermelha (KN8Entities.MODEL_ALIASES e textureSpecies); o resto (vida, golpes,
 * IA, efeitos) vem dos JSON {@code kaiju_no8_rampage[_giant]}.
 *
 * <p>O modelo do No. 8 foi animado como soldado: tem andar, correr e os bracos num controller proprio, mas nao tem a
 * respiracao ({@code overlay}) dos kaiju. Aqui so entram os controles que existem no arquivo de animacao.</p>
 */
public class KaijuNo8RampageEntity extends KaijuEntity {

    /** Acima desta velocidade horizontal (blocos/tick, ja com a escala) a passada vira corrida. */
    private static final double RUN_SPEED = 0.18;

    public KaijuNo8RampageEntity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = loop("movement", "idle");
        RawAnimation walk = loop("movement", "walk");
        RawAnimation run = loop("movement", "run");
        controllers.add(Locomotion.drive(new AnimationController<>(this, "movement", 4, state -> {
            if (!state.isMoving()) {
                return state.setAndContinue(idle);
            }
            double speed = getDeltaMovement().horizontalDistance() / Math.max(0.5, getScale());
            return state.setAndContinue(speed > RUN_SPEED ? run : walk);
        }), KaijuEntity::kaijuId));
        // Os bracos do modelo de soldado ficam num controller proprio (guarda dos punhos parado, andando e correndo).
        RawAnimation ready = loop("arms", "fists_ready");
        RawAnimation armsWalk = loop("arms", "fists_walk");
        RawAnimation armsRun = loop("arms", "fists_run");
        controllers.add(new AnimationController<>(this, "arms", 4, state -> {
            if (!state.isMoving()) {
                return state.setAndContinue(ready);
            }
            double speed = getDeltaMovement().horizontalDistance() / Math.max(0.5, getScale());
            return state.setAndContinue(speed > RUN_SPEED ? armsRun : armsWalk);
        }));
        AnimationController<KaijuEntity> action = new AnimationController<KaijuEntity>(this, "action", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack", once("action", "attack"));
        for (ResourceLocation id : def().map(KaijuDef::abilities).orElse(List.of())) {
            KN8Data.ABILITY.get(id, level().isClientSide()).ifPresent(ability -> action.triggerableAnim(
                    ability.animation(),
                    RawAnimation.begin().thenPlay(animationSpecies() + "." + ability.animation())));
        }
        controllers.add(VfxKeyframes.attach(action));
        controllers.add(new AnimationController<>(this, "reaction", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", once("reaction", "hurt")));
    }
}
