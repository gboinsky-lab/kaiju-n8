// src/main/java/com/kn8/common/soldier/special/HakuaEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Hakua Igarashi (ESPECIAIS-2, modelo do Miguel): atiradora de apoio; o Incentivo Direto cura o esquadrao aos
 * poucos e a Rajada de Determinacao incendeia [THE GAME].
 * Numeros em {@code special_soldier/hakua.json} [SUPOSICAO].
 */
public class HakuaEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("hakua");
    public static final String VARIANT = "hakua";
    /** Tecnicas do JSON e reacoes com animacao propria (hakua.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "sincere_shot", "direct_encouragement", "determination_barrage", "advanced_pursuit", "counter", "dash",
            "parry", "shoot_rifle", "reload_rifle");

    public HakuaEntity(EntityType<? extends HakuaEntity> type, Level level) {
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
