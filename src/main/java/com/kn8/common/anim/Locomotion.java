// src/main/java/com/kn8/common/anim/Locomotion.java
package com.kn8.common.anim;

import java.util.Optional;
import java.util.function.Function;

import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.LocomotionDef;
import com.kn8.core.anim.LocomotionMath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;

/**
 * Perfil de locomocao em uso (0.5.0-C): velocidade da animacao de andar pela passada e passos pela distancia. Os
 * dois lados leem o mesmo {@code locomotion/<id>.json} (sincronizado); sem perfil, tudo como antes.
 */
public final class Locomotion {

    private Locomotion() {
    }

    public static Optional<LocomotionDef> profile(Entity entity, ResourceLocation id) {
        return KN8Data.LOCOMOTION.get(id, entity.level().isClientSide());
    }

    /**
     * Liga o controller "movement" ao perfil: enquanto anda, a animacao toca na velocidade em que os pes
     * acompanham o chao (passada do JSON contra a distancia real andada no ultimo tick, que no cliente vem da
     * posicao interpolada).
     */
    public static <T extends Entity & GeoAnimatable> AnimationController<T> drive(AnimationController<T> controller,
            Function<T, ResourceLocation> id) {
        return controller.setAnimationSpeedHandler(entity -> profile(entity, id.apply(entity)).map(def -> {
            AnimationProcessor.QueuedAnimation current = controller.getCurrentAnimation();
            // Animation#length ja vem em ticks (a GeckoLib converte o "animation_length" do JSON).
            double loopTicks = current == null ? 0 : current.animation().length();
            double moved = Math.hypot(entity.getX() - entity.xo, entity.getZ() - entity.zo);
            return LocomotionMath.animationSpeed(moved, loopTicks, def.stride(), def.minAnimationSpeed(),
                    def.maxAnimationSpeed());
        }).orElse(1.0));
    }

    /** Id do perfil de quem nao e kaiju (soldados): o do tipo de entidade. */
    public static ResourceLocation typeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    /** Proximo passo do vanilla ({@code Entity#nextStep}) pela passada; sem perfil, o do vanilla (1 por bloco). */
    public static float nextStep(Entity entity, ResourceLocation id, float vanilla) {
        return profile(entity, id).map(def -> LocomotionMath.nextStep(entity.moveDist, def.stepEvery()))
                .orElse(vanilla);
    }

    /** Poeira do chao a cada passo dos grandes (o passo do vanilla so roda no servidor). */
    public static void stepDust(Entity entity, ResourceLocation id, BlockPos pos, BlockState state) {
        if (!(entity.level() instanceof ServerLevel level) || state.isAir()) {
            return;
        }
        profile(entity, id).filter(def -> def.stepDust() > 0).ifPresent(def -> {
            double spread = entity.getBbWidth() * 0.4;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), entity.getX(),
                    pos.getY() + 1.05, entity.getZ(), def.stepDust(), spread, 0.1, spread, 0.15);
        });
    }
}
