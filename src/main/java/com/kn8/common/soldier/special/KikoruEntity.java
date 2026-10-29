// src/main/java/com/kn8/common/soldier/special/KikoruEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.core.combat.SpecialGeometry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Kikoru Shinomiya, traje normal (0.5.0-D8, modelo do Miguel). Soldado especial como o Hoshina (mesma IA de tecnicas,
 * esquiva, contra-ataque, aparar e escalada de combate), com o machado de duas maos ({@code kn8:axe}, "esse machado e
 * dela", Miguel 2026-10-08) e as habilidades da Biblioteca v22 secao 8: Axe Slash, Heavy Swing, Shockwave, Dash
 * Strike, Ground Smash e Guard Break. Numeros em {@code special_soldier/kikoru.json}; aura amarela com raios.
 */
public class KikoruEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_KIKORU = KN8Constants.id("kikoru");
    public static final String VARIANT_KIKORU = "kikoru";
    /** COMBATE-3: tecnica que vem de cima (salto real da Kikoru) e folga ate a borda do kaiju ao cair (blocos). */
    private static final String FALLING_THUNDER = "falling_thunder";
    private static final double LANDING_GAP = 0.3;
    private static final double STAND_PROBE = 0.2;
    /** Tecnicas do JSON e reacoes com animacao propria (kikoru.action.<id>). */
    public static final List<String> ANIMATED_KIKORU = List.of("axe_slash", "heavy_swing", "shockwave", "dash_strike",
            "ground_smash", "guard_break", "counter", "dash", "parry",
            // COMBATE-3: estilo do machado do Esquadrao (nomes da obra; a no. 4 e nova, Miguel 2026-10-29).
            "falling_thunder", "water_skimmer", "half_moon", "rising_thunder", "twin_swallows", "daruma_dropper");

    public KikoruEntity(EntityType<? extends KikoruEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_KIKORU;
    }

    @Override
    protected String variantName() {
        return VARIANT_KIKORU;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_KIKORU;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_KIKORU;
    }

    @Override
    protected String armsFamily() {
        return "axe";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }

    /**
     * COMBATE-3 (Miguel: "o falling thunder deve vir de um ataque de cima"): no chao, a Kikoru salta alto e cai em
     * cima do kaiju no tick do golpe (mesma fisica de mob do vanilla: o arco tem o tempo do preparo do JSON). No ar
     * (a No. 4 voando) o golpe ja vem de cima.
     */
    @Override
    protected void onTechniqueStart(String id, LivingEntity target, int firstHitTicks) {
        // No chao: o onGround do mob demora um tick a aparecer (logo ao nascer, depois de um passo); um bloco solido
        // logo abaixo dos pes tambem vale.
        boolean standing = onGround() || !level().noCollision(this, getBoundingBox().move(0, -STAND_PROBE, 0));
        if (!FALLING_THUNDER.equals(id) || !standing) {
            return;
        }
        Vec3 toTarget = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        double distance = Math.max(0.0, toTarget.length() - (target.getBbWidth() + getBbWidth()) * 0.5
                - LANDING_GAP);
        Vec3 direction = toTarget.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toTarget.normalize();
        int ticks = Math.max(1, firstHitTicks);
        double horizontal = SpecialGeometry.leapHorizontalSpeed(distance, ticks);
        setDeltaMovement(direction.x * horizontal, SpecialGeometry.leapVerticalSpeed(ticks),
                direction.z * horizontal);
        hasImpulse = true;
    }
}
