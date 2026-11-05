// src/main/java/com/kn8/common/soldier/special/IharuEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Iharu Furuhashi (ESPECIAIS-1, modelo do Miguel): atirador de curta e media distancia com municao eletrificada da
 * Izumo Tech (Lentidao = choque) e o Flash Adapter da obra (pico de potencia: avanco com Forca e Velocidade). Numeros
 * em {@code special_soldier/iharu.json} [SUPOSICAO].
 */
public class IharuEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("iharu");
    public static final String VARIANT = "iharu";
    /** Tecnicas do JSON e reacoes com animacao propria (iharu.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "single_minded_shot", "thunderbolt_blast", "conduct_round_sweep", "rapid_thunderbolt",
            "flash_adapter", "counter", "dash", "parry", "shoot_rifle", "reload_rifle");

    public IharuEntity(EntityType<? extends IharuEntity> type, Level level) {
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
