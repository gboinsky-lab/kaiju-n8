// src/main/java/com/kn8/common/registry/AttributeLimits.java
package com.kn8.common.registry;

import java.lang.reflect.Field;

import com.kn8.KN8Constants;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/**
 * Teto da vida maxima (0.6-D). O vanilla limita {@code generic.max_health} a 1024: kaiju acima de fortitude ~7,6
 * (vida = 20 x 2^(fortitude - 2)) e chefes com multiplicador nasciam com 1024. O No. 10 pequeno (8,3 -> 1576) e o
 * grande (9 -> 2560) ficariam com a mesma vida. Medido no duelo de calibragem do Hoshina.
 *
 * <p>Sem mixin (regra 7): o limite e o campo {@code maxValue} do RangedAttribute, trocado por reflexao uma vez no
 * setup comum, nos dois lados (o cliente tambem limita ao receber os atributos). Mesma solucao do mod AttributeFix.
 * Valor tecnico, nao de balanceamento: por isso constante, nao config (o config ainda nao carregou aqui).</p>
 */
public final class AttributeLimits {

    public static final double MAX_HEALTH_CAP = 100_000.0;

    private AttributeLimits() {
    }

    public static void raiseMaxHealthCap() {
        Attribute attribute = Attributes.MAX_HEALTH.value();
        if (!(attribute instanceof RangedAttribute ranged) || ranged.getMaxValue() >= MAX_HEALTH_CAP) {
            return;
        }
        try {
            Field field = ObfuscationReflectionHelper.findField(RangedAttribute.class, "maxValue");
            field.setDouble(ranged, MAX_HEALTH_CAP);
            KN8Constants.LOGGER.info("[kn8] Teto de vida maxima: {}", ranged.getMaxValue());
        } catch (IllegalAccessException | RuntimeException exception) {
            KN8Constants.LOGGER.error("[kn8] Nao foi possivel subir o teto de vida (fica 1024): kaiju muito fortes "
                    + "nascem com 1024 de vida", exception);
        }
    }
}
