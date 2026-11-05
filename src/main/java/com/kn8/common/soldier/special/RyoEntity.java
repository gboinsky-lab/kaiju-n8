// src/main/java/com/kn8/common/soldier/special/RyoEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ryo Ikaruga (ESPECIAIS-2, modelo do Miguel): comandante; a Ordem de disparo e o Zelo contagiante
 * fortalecem os aliados por perto.
 * Numeros em {@code special_soldier/ryo.json} [SUPOSICAO].
 */
public class RyoEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("ryo");
    public static final String VARIANT = "ryo";
    /** Tecnicas do JSON e reacoes com animacao propria (ryo.action.<id>). */
    public static final List<String> ANIMATED = List.of(
            "hot_blooded_shot", "firing_command", "contagious_zeal", "passionate_shot", "counter", "dash", "parry",
            "shoot_rifle", "reload_rifle");

    public RyoEntity(EntityType<? extends RyoEntity> type, Level level) {
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
