// src/main/java/com/kn8/common/soldier/special/SoichiroEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Soichiro Hoshina, capitao da Sexta Divisao (ESPECIAIS-1, modelo do Miguel): mestre do estilo Hoshina com
 * a katana longa ({@code kn8:katana}) e arma de fogo; tecnicas da unidade de THE GAME (dossie do Miguel). Numeros em
 * {@code special_soldier/soichiro.json} [SUPOSICAO].
 */
public class SoichiroEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("soichiro");
    public static final String VARIANT = "soichiro";
    /** Tecnicas do JSON e reacoes com animacao propria (soichiro.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "poised_marksmanship", "adaptive_stance", "prodigious_swordsmanship", "astute_strike",
            "finishing_masterstroke", "counter", "dash", "parry");

    public SoichiroEntity(EntityType<? extends SoichiroEntity> type, Level level) {
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
        return "katana";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }
}
