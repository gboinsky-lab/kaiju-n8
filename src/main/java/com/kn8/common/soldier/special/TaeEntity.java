// src/main/java/com/kn8/common/soldier/special/TaeEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Tae Nakanoshima (ESPECIAIS-2, modelo do Miguel): municao incendiaria e granada termica [THE GAME].
 * Numeros em {@code special_soldier/tae.json} [SUPOSICAO].
 */
public class TaeEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("tae");
    public static final String VARIANT = "tae";
    /** Tecnicas do JSON e reacoes com animacao propria (tae.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "aggressive_shot", "blazing_fire_shot", "heat_grenade", "passionate_shot", "counter", "dash", "parry",
            "shoot_rifle", "reload_rifle");

    public TaeEntity(EntityType<? extends TaeEntity> type, Level level) {
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
