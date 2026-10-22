// src/main/java/com/kn8/client/vfx/draw/KN8RenderTypes.java
package com.kn8.client.vfx.draw;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * VFX-R1: tipos de render dos efeitos desenhados (sem particulas). Vertices so com posicao e cor, sem textura.
 * <ul>
 *   <li>{@link #GLOW}: brilho somado ao fundo, o mesmo shader e mistura do raio vanilla ({@code RenderType.lightning});
 *   raios, fios de corte, aneis, clarao;</li>
 *   <li>{@link #SHADE}: cor com transparencia comum, para o que precisa escurecer (miolo preto da lamina, fumaca).</li>
 * </ul>
 * Os dois desenham as duas faces (sem culling) e respeitam a profundidade (somem atras de paredes).
 */
public final class KN8RenderTypes {

    public static final RenderType GLOW = RenderType.create("kn8_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    public static final RenderType SHADE = RenderType.create("kn8_shade", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private KN8RenderTypes() {
    }
}
