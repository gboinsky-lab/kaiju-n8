// src/main/java/com/kn8/common/soldier/special/KikoruNo4Entity.java
package com.kn8.common.soldier.special;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.SpecialSoldierDef;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Kikoru Shinomiya com a arma numerada 4 (0.7-D, modelo do Miguel com as 4 asas em X nas costas). Mesma IA da Kikoru
 * (machado, tecnicas, esquiva, contra-ataque), mais o voo da Biblioteca v22 secao 9: com alvo ela sai do chao e paira
 * acima dele ({@code flight} no {@code special_soldier/kikoru_no4.json}); cada tecnica e um mergulho ate a altura do
 * alvo (Dive Strike, Vertical Assault, Air Combo) e depois ela sobe de novo. Sem alvo, pousa. O voo e decidido no
 * servidor; o cliente so ve a posicao e troca a animacao (asas batendo, pernas recolhidas) pelo "no chao".
 */
public class KikoruNo4Entity extends KikoruEntity {

    public static final ResourceLocation PROFILE_NO4 = KN8Constants.id("kikoru_no4");
    public static final String VARIANT_NO4 = "kikoru_no4";
    public static final List<String> ANIMATED_NO4 = List.of("high_speed_axe", "dive_strike", "vertical_assault",
            "air_combo", "aerial_dash", "counter", "dash", "parry");
    /** Distancia horizontal (alem das bordas) em que para de avancar e so ajusta a altura. */
    private static final double HOLD_DISTANCE = 1.2;
    /** Fracao da diferenca de altura corrigida por tick (subida/descida suave). */
    private static final double CLIMB_RATE = 0.25;

    public KikoruNo4Entity(EntityType<? extends KikoruNo4Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_NO4;
    }

    @Override
    protected String variantName() {
        return VARIANT_NO4;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_NO4;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_NO4;
    }

    /** No ar (servidor e cliente: o cliente ve a entidade fora do chao). */
    public boolean isFlying() {
        return !onGround() && isAggressive();
    }

    @Override
    protected String movementName(boolean moving) {
        return !onGround() ? "fly" : super.movementName(moving);
    }

    @Override
    protected void registerExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation flap = RawAnimation.begin().thenLoop(VARIANT_NO4 + ".wings.flap");
        RawAnimation fold = RawAnimation.begin().thenLoop(VARIANT_NO4 + ".wings.fold");
        controllers.add(new AnimationController<>(this, "wings", 4, state ->
                state.setAndContinue(onGround() ? fold : flap)));
    }

    @Override
    protected void tickExtra(long now) {
        super.tickExtra(now);
        Optional<SpecialSoldierDef.Flight> flight = profile().flatMap(SpecialSoldierDef::flight);
        LivingEntity target = getTarget();
        boolean flying = flight.isPresent() && target != null && target.isAlive() && !isPassenger();
        setNoGravity(flying);
        if (!flying) {
            return;
        }
        fallDistance = 0.0F;
        double speed = flight.get().flySpeed();
        // Mergulho: durante a tecnica desce ate a altura do alvo; fora dela paira acima do topo dele.
        double goalY = isUsingTechnique() ? target.getY() + target.getBbHeight() * 0.3
                : target.getY() + target.getBbHeight() + flight.get().hoverHeight();
        Vec3 flat = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
        double gap = flat.length() - (target.getBbWidth() + getBbWidth()) / 2.0 - HOLD_DISTANCE;
        Vec3 horizontal = gap > 0.0 && flat.lengthSqr() > 1.0E-6
                ? flat.normalize().scale(Math.min(speed, gap)) : Vec3.ZERO;
        double vertical = Mth.clamp((goalY - getY()) * CLIMB_RATE, -speed, speed);
        Vec3 current = getDeltaMovement();
        // Mistura com o movimento atual: avancos e esquivas das tecnicas continuam valendo.
        setDeltaMovement(current.x * 0.5 + horizontal.x * 0.5, vertical, current.z * 0.5 + horizontal.z * 0.5);
        getNavigation().stop();
        getLookControl().setLookAt(target, 30.0F, 30.0F);
    }
}
