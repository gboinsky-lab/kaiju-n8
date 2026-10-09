// src/main/java/com/kn8/common/soldier/special/NarumiNo1Entity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Gen Narumi com a arma numerada 1 (0.7-D). Nao veio modelo proprio: usa o do Narumi (mesmas animacoes, prefixo
 * "narumi") com outro perfil, {@code special_soldier/narumi_no1.json} (v1.2: 650 de vida, T7). Pseudo-previsao
 * (Biblioteca v22 secao 11) pelos numeros: reage antes aos golpes do kaiju (esquiva, aparar e contra-ataque com mais
 * chance e janela maior) e os golpes de "leitura de ponto fraco" expoem o nucleo para o esquadrao. [DECIDIR] modelo
 * proprio da Numbers 1.
 */
public class NarumiNo1Entity extends NarumiEntity {

    public static final ResourceLocation PROFILE_NO1 = KN8Constants.id("narumi_no1");
    public static final String VARIANT_NO1 = "narumi_no1";
    public static final List<String> ANIMATED_NO1 = List.of("thrust_combo", "lunge", "bayonet_shot",
            "sweeping_slash", "weak_point_thrust", "predicted_counter", "counter", "dash", "parry");

    public NarumiNo1Entity(EntityType<? extends NarumiNo1Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_NO1;
    }

    @Override
    protected String variantName() {
        return VARIANT_NO1;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_NO1;
    }
}
