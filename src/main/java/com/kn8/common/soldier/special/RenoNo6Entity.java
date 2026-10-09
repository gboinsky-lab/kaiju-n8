// src/main/java/com/kn8/common/soldier/special/RenoNo6Entity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Reno Ichikawa com a arma numerada 6 (0.7-D, modelo do Miguel com o traje azul). Criocinese (Biblioteca v22 secao 7):
 * lanca de gelo que atravessa, explosao congelante, campo de gelo em volta, congelamento de varios alvos e os canhoes
 * auxiliares (tiros que congelam). Tudo com o projetil de tecnica (rastro, explosao e Lentidao do JSON); fica a
 * distancia como o Reno normal. Numeros em {@code special_soldier/reno_no6.json} (v1.2: 650 de vida, T7).
 */
public class RenoNo6Entity extends RenoEntity {

    public static final ResourceLocation PROFILE_NO6 = KN8Constants.id("reno_no6");
    public static final String VARIANT_NO6 = "reno_no6";
    public static final List<String> ANIMATED_NO6 = List.of("shoot_rifle", "reload_rifle", "ice_spear",
            "freeze_burst", "ice_field", "multi_freeze", "auxiliary_cannons", "counter", "dash", "parry");

    public RenoNo6Entity(EntityType<? extends RenoNo6Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_NO6;
    }

    @Override
    protected String variantName() {
        return VARIANT_NO6;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_NO6;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_NO6;
    }
}
