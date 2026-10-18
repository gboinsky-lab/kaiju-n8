// src/main/java/com/kn8/common/soldier/special/IsaoBaseEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Isao Shinomiya sem a arma numerada 2 (MOV-5G, Miguel 2026-10-17: "sem traje = sem o traje kaiju"). Balanceamento
 * v1.2 secao 12 (Isao normal: 520 de vida, armadura 20, velocidade 0,29, T6, pesado). Luta com os punhos (a forca vem
 * das pernas para os punhos, como na obra) mas sem as ondas de choque das manoplas: so golpes de perto, perfil
 * {@code special_soldier/isao_base.json} e item invisivel {@code kn8:isao_fist}. Usa o modelo e as animacoes do Isao
 * (prefixo "isao") [DECIDIR: o modelo tem as manoplas]. A obra nao mostra o Isao lutando sem o Numbers 2.
 */
public class IsaoBaseEntity extends IsaoEntity {

    public static final ResourceLocation PROFILE_BASE = KN8Constants.id("isao_base");
    public static final String VARIANT_BASE = "isao_base";
    public static final List<String> ANIMATED_BASE = List.of(
            "jab_combo", "elbow_strike", "heavy_straight", "counter", "dash", "parry");

    public IsaoBaseEntity(EntityType<? extends IsaoBaseEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE_BASE;
    }

    @Override
    protected String variantName() {
        return VARIANT_BASE;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED_BASE;
    }
}
