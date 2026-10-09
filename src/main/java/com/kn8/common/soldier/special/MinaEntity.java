// src/main/java/com/kn8/common/soldier/special/MinaEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Mina Ashiro (0.7-C, modelo do Miguel), com o canhao pesado dela ({@code kn8:mina_cannon}) na altura do quadril.
 * Atiradora anti-Daikaiju (Biblioteca v22 secao 3): fica longe, tiro de precisao, tiro do canhao e canhao carregado
 * (explodem) e o Anti-Giant Shot (preparo longo, prioridade contra Honju/numerados). Numeros em
 * {@code special_soldier/mina.json} (Balanceamento v1.2: 420 de vida, T6); aura laranja.
 */
public class MinaEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE_MINA = KN8Constants.id("mina");
    public static final String VARIANT_MINA = "mina";
    /** Tecnicas do JSON e reacoes com animacao propria (mina.action.<id>). */
    public static final List<String> ANIMATED_MINA = List.of(
            "shoot_rifle", "reload_rifle", "precision_shot", "cannon_shot", "charged_cannon", "anti_giant_shot",
            "counter", "dash", "parry");

    public MinaEntity(EntityType<? extends MinaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_MINA;
    }

    @Override
    protected String variantName() {
        return VARIANT_MINA;
    }

    @Override
    protected String animPrefix() {
        return VARIANT_MINA;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_MINA;
    }

    @Override
    protected String armsFamily() {
        return "cannon";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }
}
