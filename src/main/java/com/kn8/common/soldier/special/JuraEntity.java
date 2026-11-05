// src/main/java/com/kn8/common/soldier/special/JuraEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Jura Igarashi, capita da Segunda Divisao (ESPECIAIS-1, modelo do Miguel, 1,93 m): bola de ferro em chamas
 * segurada pela corda ({@code kn8:jura_flail}); golpes com fogo ({@code burn}) e o Hot-Blooded que fortalece os aliados
 * ({@code rally}). Numeros em {@code special_soldier/jura.json} [SUPOSICAO].
 */
public class JuraEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("jura");
    public static final String VARIANT = "jura";
    /** Tecnicas do JSON e reacoes com animacao propria (jura.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "herculean_toss", "hot_blooded_impact", "zenith_plunge", "blazing_slugger", "counter", "dash",
            "parry");

    public JuraEntity(EntityType<? extends JuraEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE;
    }

    @Override
    protected String variantName() {
        return VARIANT;
    }

    @Override
    protected String animPrefix() {
        return VARIANT;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED;
    }

    @Override
    protected String armsFamily() {
        return "flail";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }
}
