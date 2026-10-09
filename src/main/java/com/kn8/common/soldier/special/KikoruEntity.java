// src/main/java/com/kn8/common/soldier/special/KikoruEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Kikoru Shinomiya, traje normal (0.5.0-D8, modelo do Miguel). Soldado especial como o Hoshina (mesma IA de tecnicas,
 * esquiva, contra-ataque, aparar e escalada de combate), com o machado de duas maos ({@code kn8:axe}, "esse machado e
 * dela", Miguel 2026-10-08) e as habilidades da Biblioteca v22 secao 8: Axe Slash, Heavy Swing, Shockwave, Dash
 * Strike, Ground Smash e Guard Break. Numeros em {@code special_soldier/kikoru.json}; aura amarela com raios.
 */
public class KikoruEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_KIKORU = KN8Constants.id("kikoru");
    public static final String VARIANT_KIKORU = "kikoru";
    /** Tecnicas do JSON e reacoes com animacao propria (kikoru.action.<id>). */
    public static final List<String> ANIMATED_KIKORU = List.of("axe_slash", "heavy_swing", "shockwave", "dash_strike",
            "ground_smash", "guard_break", "counter", "dash", "parry");

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
}
