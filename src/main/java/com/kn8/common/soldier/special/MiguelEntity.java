// src/main/java/com/kn8/common/soldier/special/MiguelEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Miguel (MIGUEL-CAMILLY, personagem original e modelo do Miguel): foice-armamento que corta em arco e dispara
 * pelo eixo; Eclipse do Ceifador como golpe final [PROPOSTA ORIGINAL].
 * Numeros em {@code special_soldier/miguel.json} [SUPOSICAO].
 */
public class MiguelEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("miguel");
    public static final String VARIANT = "miguel";
    /** Tecnicas do JSON e reacoes com animacao propria (miguel.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "crimson_orbital_cut", "scythe_axis_shot", "reapers_hook", "rising_arc", "core_piercing_shot",
            "double_reap_dance", "crimson_shard_rain", "reaper_eclipse", "counter", "dash", "parry");

    public MiguelEntity(EntityType<? extends MiguelEntity> type, Level level) {
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
        return "spear";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }
}
