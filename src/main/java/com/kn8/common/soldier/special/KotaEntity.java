// src/main/java/com/kn8/common/soldier/special/KotaEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Kota Tachibana (ESPECIAIS-2, modelo do Miguel): espingarda na linha de frente, protege quem esta perto e
 * lanca a granada congelante [THE GAME].
 * Numeros em {@code special_soldier/kota.json} [SUPOSICAO].
 */
public class KotaEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("kota");
    public static final String VARIANT = "kota";
    /** Tecnicas do JSON e reacoes com animacao propria (kota.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "steady_shot", "menacing_blow", "freeze_grenade", "solid_shot", "counter", "dash", "parry", "shoot_rifle",
            "reload_rifle");

    public KotaEntity(EntityType<? extends KotaEntity> type, Level level) {
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
