// src/main/java/com/kn8/common/soldier/special/CamillyEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Camilly (MIGUEL-CAMILLY, personagem original, modelo do Miguel): atiradora com o rifle de lamina lateral em
 * tons de rosa; tecnicas renomeadas no tema rosa e a Tempestade Rosa como golpe final [PROPOSTA ORIGINAL].
 * Numeros em {@code special_soldier/camilly.json} [SUPOSICAO].
 */
public class CamillyEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("camilly");
    public static final String VARIANT = "camilly";
    /** Tecnicas do JSON e reacoes com animacao propria (camilly.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "rose_precision_shot", "petal_burst", "rose_side_cut", "rose_bayonet_charge", "pink_piercing_shot",
            "pink_cross_sequence", "rose_cover_fire", "rose_storm", "counter", "dash", "parry",
            "shoot_rifle", "reload_rifle");

    public CamillyEntity(EntityType<? extends CamillyEntity> type, Level level) {
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
