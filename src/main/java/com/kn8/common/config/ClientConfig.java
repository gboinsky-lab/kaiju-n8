// src/main/java/com/kn8/common/config/ClientConfig.java
package com.kn8.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code kn8-client.toml} (tipo CLIENT: so na maquina do jogador, nunca sincronizado). Preferencias visuais e de
 * desempenho do cliente (Fase 4, secao 4.5). Fica em {@code common} porque e so uma especificacao de valores; quem
 * le os valores e codigo de cliente.
 */
public final class ClientConfig {

    /** Canto da tela onde a HUD do kn8 e ancorada. */
    public enum HudAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    /** Quantidade de particulas dos efeitos do kn8. */
    public enum ParticleLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    /** Como desenhar o jogador transformado: D = fantoche GeckoLib (padrao, PT5); B = fallback simples. */
    public enum TransformRenderMethod {
        D,
        B
    }

    private static final String PREFIX = "kn8.configuration.";
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ModConfigSpec.EnumValue<ParticleLevel> PARTICLE_LEVEL;
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE;
    public static final ModConfigSpec.IntValue ANIMATION_MAX_CATCH_UP_TICKS;
    public static final ModConfigSpec.EnumValue<TransformRenderMethod> TRANSFORM_METHOD;
    public static final ModConfigSpec.BooleanValue DEBUG_SHOW_PARTS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Heads-up display.").translation(PREFIX + "hud").push("hud");
        HUD_SCALE = BUILDER.comment("HUD scale.").translation(PREFIX + "scale")
                .defineInRange("scale", 1.0, 0.5, 2.0);
        HUD_ANCHOR = BUILDER.comment("Screen corner where the HUD is anchored.").translation(PREFIX + "anchor")
                .defineEnum("anchor", HudAnchor.BOTTOM_LEFT);
        BUILDER.pop();

        BUILDER.comment("Visual effects.").translation(PREFIX + "fx").push("fx");
        PARTICLE_LEVEL = BUILDER.comment("Amount of kn8 particles.").translation(PREFIX + "particleLevel")
                .defineEnum("particleLevel", ParticleLevel.MEDIUM);
        CAMERA_SHAKE = BUILDER.comment("Camera shake intensity (0 disables).").translation(PREFIX + "cameraShake")
                .defineInRange("cameraShake", 1.0, 0.0, 1.0);
        ANIMATION_MAX_CATCH_UP_TICKS = BUILDER.comment("Player animations received late skip ahead by the delay, up"
                        + " to this many ticks, to stay in sync with the server (M9).")
                .translation(PREFIX + "animationMaxCatchUpTicks")
                .defineInRange("animationMaxCatchUpTicks", 6, 0, 20);
        BUILDER.pop();

        BUILDER.comment("Rendering.").translation(PREFIX + "render").push("render");
        TRANSFORM_METHOD = BUILDER.comment("Transformed player rendering: D = animated GeckoLib puppet (default),"
                        + " B = simple fallback for modpacks with skin/render conflicts.")
                .translation(PREFIX + "transformMethod")
                .defineEnum("transformMethod", TransformRenderMethod.D);
        BUILDER.pop();

        BUILDER.comment("Debug tools.").translation(PREFIX + "debug").push("debug");
        DEBUG_SHOW_PARTS = BUILDER.comment("Draw kaiju part hitboxes (operators only).")
                .translation(PREFIX + "showParts")
                .define("showParts", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ClientConfig() {
    }
}
