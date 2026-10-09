// src/main/java/com/kn8/common/soldier/special/RenoEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Reno Ichikawa, traje normal (0.7-C, modelo do Miguel). Soldado especial de rifle ({@code kn8:rifle}): fica a
 * distancia e atira como o soldado comum (mesma logica de atirador) e usa as habilidades da Biblioteca v22 secao 6:
 * Precision Shot, Burst Fire, Freeze Round (Lentidao), Suppression e uma coronhada de perto. Numeros em
 * {@code special_soldier/reno.json} (Balanceamento v1.2: 360 de vida, T5); aura azul-gelo. A forma Numbers 6 fica
 * para a 0.7-D.
 */
public class RenoEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_RENO = KN8Constants.id("reno");
    public static final String VARIANT_RENO = "reno";
    /** Tecnicas do JSON e reacoes com animacao propria (reno.action.<id>). */
    public static final List<String> ANIMATED_RENO = List.of(
            "shoot_rifle", "reload_rifle", "precision_shot", "burst_fire", "freeze_round", "suppression", "butt_strike",
            "counter", "dash", "parry");

    public RenoEntity(EntityType<? extends RenoEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_RENO;
    }

    @Override
    protected String variantName() {
        return VARIANT_RENO;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_RENO;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_RENO;
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
