// src/main/java/com/kn8/common/soldier/special/IsaoEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Isao Shinomiya com a arma numerada 2 (0.8-A, modelo do Miguel, 1,90 m): Diretor-Geral da Forca de Defesa, luta
 * com as manoplas FS-1002 feitas do Kaiju No. 2 (ondas de choque sonicas). Balanceamento v1.2 secao 12 (Isao + No. 2:
 * 700 de vida, T7+). Tecnicas pela obra: N.1 Barrage Blast (sequencia de socos), N.5 Spiral Blast (onda que vai
 * longe), N.6 Thunder Emperor (soco duplo pesado que expoe o nucleo) e Force Blast (onda em leque, so do jogo
 * KAIJU NO. 8 THE GAME [SUPOSICAO]). Luta com os punhos (item invisivel {@code kn8:numbers2_gauntlet}: as manoplas
 * ja estao no modelo). E a presa do No. 9 na absorcao que cria o {@code kaiju_no9_isao}.
 */
public class IsaoEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_ISAO = KN8Constants.id("isao");
    public static final String VARIANT_ISAO = "isao";
    /** Tecnicas do JSON e reacoes com animacao propria (isao.action.<id>). */
    public static final List<String> ANIMATED_ISAO = List.of(
            "barrage_blast", "spiral_blast", "force_blast", "thunder_emperor", "counter", "dash", "parry");

    public IsaoEntity(EntityType<? extends IsaoEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_ISAO;
    }

    @Override
    protected String variantName() {
        return VARIANT_ISAO;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_ISAO;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_ISAO;
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
