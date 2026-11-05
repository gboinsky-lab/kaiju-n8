// src/main/java/com/kn8/common/soldier/special/RinEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Rin Shinonome, lider de pelotao da Primeira Divisao (ESPECIAIS-1, modelo do Miguel): atiradora de grande volume
 * (a metralhadora da obra e o rifle do mod [SUPOSICAO]) com rajadas, rajada incendiaria e bastao de perto. Numeros em
 * {@code special_soldier/rin.json} [SUPOSICAO].
 */
public class RinEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("rin");
    public static final String VARIANT = "rin";
    /** Tecnicas do JSON e reacoes com animacao propria (rin.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "focused_shot", "thorough_sweep_fire", "incinerating_barrage", "circular_barrage", "baton_strike",
            "counter", "dash", "parry", "shoot_rifle", "reload_rifle");

    public RinEntity(EntityType<? extends RinEntity> type, Level level) {
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
