// src/main/java/com/kn8/common/soldier/special/HaruichiEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Haruichi Izumo (ESPECIAIS-1, modelo do Miguel): atirador de precisao que fica longe, com o arco AW-0027 de
 * flechas envenenadas na salva e a Postura de supressao que fortalece os aliados. Numeros em
 * {@code special_soldier/haruichi.json} [SUPOSICAO].
 */
public class HaruichiEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("haruichi");
    public static final String VARIANT = "haruichi";
    /** Tecnicas do JSON e reacoes com animacao propria (haruichi.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "personal_style_shot", "precision_snipe", "clearance", "suppressive_stance",
            "manifold_arrow_salvo", "counter", "dash", "parry", "shoot_rifle", "reload_rifle");

    public HaruichiEntity(EntityType<? extends HaruichiEntity> type, Level level) {
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
        return "rifle";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }
}
