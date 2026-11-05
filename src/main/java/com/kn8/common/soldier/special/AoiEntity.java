// src/main/java/com/kn8/common/soldier/special/AoiEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Aoi Kaguragi (ESPECIAIS-2, modelo do Miguel): linha de frente com o Pile Driver PB-3115 (canon), golpes
 * pesados de perto e a Postura inabalavel que protege o esquadrao (Resistencia).
 * Numeros em {@code special_soldier/aoi.json} [SUPOSICAO].
 */
public class AoiEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("aoi");
    public static final String VARIANT = "aoi";
    /** Tecnicas do JSON e reacoes com animacao propria (aoi.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "steadfast_swing", "skillful_bash", "stalwart_stance", "fortified_crushing_blow", "pile_driver", "counter",
            "dash", "parry");

    public AoiEntity(EntityType<? extends AoiEntity> type, Level level) {
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
