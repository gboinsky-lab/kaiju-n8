// src/main/java/com/kn8/common/soldier/special/KafkaEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Kafka Hibino, forma humana (0.7-F, modelo do Miguel). Soldado da Forca de Defesa fraco (Balanceamento v1.2 secao 11:
 * 24 de vida, T2): rifle e soco. Quando esta perdendo (vida abaixo do {@code transform.health_below} do
 * {@code special_soldier/kafka.json}) vira o Kaiju No. 8 no mesmo lugar (Miguel, 2026-10-08: "vira o Kaiju No. 8
 * quando esta perdendo"); a troca de forma e do {@link HoshinaEntity} (tickForm).
 */
public class KafkaEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_KAFKA = KN8Constants.id("kafka");
    public static final String VARIANT_KAFKA = "kafka";
    /** Tecnicas do JSON e reacoes com animacao propria (kafka.action.<id>). */
    public static final List<String> ANIMATED_KAFKA = List.of(
            "shoot_rifle", "reload_rifle", "punch_combo", "counter", "dash", "parry");

    public KafkaEntity(EntityType<? extends KafkaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_KAFKA;
    }

    @Override
    protected String variantName() {
        return VARIANT_KAFKA;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_KAFKA;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_KAFKA;
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
