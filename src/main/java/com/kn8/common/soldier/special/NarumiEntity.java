// src/main/java/com/kn8/common/soldier/special/NarumiEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Gen Narumi, traje normal (0.7-C, modelo do Miguel), com a baioneta longa ({@code kn8:narumi_bayonet}, maior que ele).
 * Comandante de combate hibrido (Biblioteca v22 secao 10): estocadas em sequencia, investida, varrida que empurra e
 * o tiro da baioneta a distancia. Numeros em {@code special_soldier/narumi.json} (Balanceamento v1.2: 480 de vida,
 * T6); aura rosa com raios. A forma Numbers 1 fica para a 0.7-D.
 */
public class NarumiEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_NARUMI = KN8Constants.id("narumi");
    public static final String VARIANT_NARUMI = "narumi";
    /** Tecnicas do JSON e reacoes com animacao propria (narumi.action.<id>). */
    public static final List<String> ANIMATED_NARUMI = List.of(
            "thrust_combo", "lunge", "bayonet_shot", "sweeping_slash", "counter", "dash", "parry");

    public NarumiEntity(EntityType<? extends NarumiEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_NARUMI;
    }

    @Override
    protected String variantName() {
        return VARIANT_NARUMI;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_NARUMI;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_NARUMI;
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
