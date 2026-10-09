// src/main/java/com/kn8/common/soldier/special/KaijuNo8Entity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Kaiju No. 8 (0.7-F, modelo do Miguel, 2 m): o Kafka transformado, aliado da Forca de Defesa. Corpo a corpo de forca
 * enorme (Biblioteca v22 secao 2; Balanceamento v1.2 secao 11: 1.800 de vida, T8): soco pesado, golpe no chao (onda
 * que explode), rugido (empurra e deixa lento), investida, sequencia de socos; regenera 3%/s abaixo de 50%. Luta com
 * os punhos (item invisivel {@code kn8:kaiju_no8_fist}). Sem inimigo por um tempo volta a ser o Kafka
 * ({@code transform} no {@code special_soldier/kaiju_no8.json}).
 */
public class KaijuNo8Entity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_NO8 = KN8Constants.id("kaiju_no8");
    public static final String VARIANT_NO8 = "kaiju_no8";
    /** Tecnicas do JSON e reacoes com animacao propria (kaiju_no8.action.<id>). */
    public static final List<String> ANIMATED_NO8 = List.of(
            "heavy_punch", "ground_smash", "roar", "dash_strike", "rush_combo", "counter", "dash", "parry");

    public KaijuNo8Entity(EntityType<? extends KaijuNo8Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_NO8;
    }

    @Override
    protected String variantName() {
        return VARIANT_NO8;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_NO8;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_NO8;
    }

    @Override
    protected String armsFamily() {
        return "fists";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }
}
