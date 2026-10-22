// src/main/java/com/kn8/client/vfx/draw/ProjectileRenderers.java
package com.kn8.client.vfx.draw;

import java.util.Optional;

import org.joml.Matrix4f;

import com.kn8.client.vfx.KaijuVfx;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.core.vfx.VfxColors;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * VFX-R2 (Miguel: sem particulas): os projeteis do mod desenhados como formas, a cada quadro, na posicao do proprio
 * projetil (antes eram pontos de po soltos a cada tick). Coordenadas locais: a pilha ja esta na posicao do projetil.
 */
public final class ProjectileRenderers {

    private ProjectileRenderers() {
    }

    /**
     * Corte que voa (Hoshina, espada do jogador): meia-lua perpendicular ao voo, inclinada pelo {@code roll}, com
     * miolo escuro e fio brilhante na cor do corte; bala (Reno, Mina, Narumi): risco reto com miolo claro.
     */
    public static final class Slash extends EntityRenderer<SlashProjectile> {
        private static final int STEPS = 16;
        private static final float ARC_DEGREES = 70.0F;
        /** VFX-5: bala a partir desta largura ganha a cabeca acesa. */
        private static final float HEAVY_WIDTH = 0.6F;

        public Slash(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(SlashProjectile entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers,
                int light) {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.lengthSqr() < 1.0E-6) {
                return;
            }
            Matrix4f m = pose.last().pose();
            Vec3 camera = entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partial));
            int color = entity.color();
            int light2 = VfxColors.shade(color, 0.6F);
            trail(entity);
            if (entity.bullet()) {
                double width = Math.max(0.08, Math.min(0.5, entity.width() * 0.5));
                Vec3 tail = motion.scale(-1.5);
                VertexConsumer glow = buffers.getBuffer(KN8RenderTypes.GLOW);
                Drawn.ribbon(glow, m, tail, Vec3.ZERO, width * 0.2, width, camera, color, 0.0F, 1.0F);
                Drawn.ribbon(glow, m, tail.scale(0.5), Vec3.ZERO, width * 0.1, width * 0.4, camera, light2, 0.0F,
                        1.0F);
                if (entity.width() >= HEAVY_WIDTH) {
                    // VFX-5: projetil espesso (canhao da Mina, cristal do Reno No. 6): cabeca acesa na frente.
                    Drawn.disc(glow, m, Vec3.ZERO, entity.width() * 0.55, camera, color, 0.9F, 14);
                    Drawn.disc(glow, m, Vec3.ZERO, entity.width() * 0.25, camera, light2, 1.0F, 10);
                }
                return;
            }
            Vec3 forward = motion.normalize();
            Vec3 right = forward.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 up = right.cross(forward).normalize();
            float roll = entity.roll() * Mth.DEG_TO_RAD;
            Vec3 side = right.scale(Mth.cos(roll)).add(up.scale(Mth.sin(roll)));
            double width = entity.width();
            Vec3[] outer = new Vec3[STEPS + 1];
            Vec3[] inner = new Vec3[STEPS + 1];
            for (int i = 0; i <= STEPS; i++) {
                double u = i / (double) STEPS;
                float angle = (float) ((u * 2.0 - 1.0) * ARC_DEGREES * Mth.DEG_TO_RAD);
                // Meia-lua: as pontas ficam um pouco para tras do centro; mais grossa no meio.
                Vec3 at = side.scale(Mth.sin(angle) * width).add(forward.scale((Mth.cos(angle) - 1.0F) * width * 0.6));
                double thick = width * 0.28 * Math.sin(Math.PI * u) + width * 0.02;
                outer[i] = at;
                inner[i] = at.subtract(forward.scale(thick));
            }
            int core = VfxColors.shade(color, -0.65F);
            VertexConsumer shade = buffers.getBuffer(KN8RenderTypes.SHADE);
            for (int i = 0; i < STEPS; i++) {
                Drawn.quad(shade, m, inner[i], outer[i], outer[i + 1], inner[i + 1], core, 0.85F);
            }
            VertexConsumer glow = buffers.getBuffer(KN8RenderTypes.GLOW);
            for (int i = 0; i < STEPS; i++) {
                Drawn.ribbon(glow, m, outer[i], outer[i + 1], width * 0.08, width * 0.08, camera, color, 1.0F, 1.0F);
                Drawn.ribbon(glow, m, outer[i], outer[i + 1], width * 0.03, width * 0.03, camera, light2, 1.0F, 1.0F);
            }
        }

        /**
         * VFX-5: rastro da tecnica (fumaca cinza do canhao, cristais e nevoa do gelo), uma vez por tick do projetil,
         * pela receita do perfil de quem atirou, no tamanho do corpo dele.
         */
        private static void trail(SlashProjectile entity) {
            if (!entity.claimTrailTick() || !(entity.level() instanceof ClientLevel level)) {
                return;
            }
            entity.trail().ifPresent(trail -> {
                Optional<ResourceLocation> id = Optional.ofNullable(ResourceLocation.tryParse(trail[0]));
                Vec3 at = entity.position();
                if (!KaijuVfx.inRange(at)) {
                    return;
                }
                Entity owner = entity.getOwner();
                KaijuVfx.Body body = KaijuVfx.Body.of(owner == null ? 1.8F : owner.getBbHeight());
                KaijuVfx.profile(id).ifPresent(profile -> profile.recipe(trail[1]).ifPresent(layers ->
                        KaijuVfx.playRecipe(level, profile, layers, at, entity.getDeltaMovement(), 1.0F, body,
                                KaijuVfx.budget(), KaijuVfx.GenericEffect.NONE)));
            });
        }

        @Override
        public ResourceLocation getTextureLocation(SlashProjectile entity) {
            return TextureAtlas.LOCATION_BLOCKS;
        }
    }

    /** Projetil de kaiju (raio, teia, acido, Finger Gun): risco brilhante na cor do JSON com a ponta acesa. */
    public static final class Kaiju extends EntityRenderer<KaijuProjectile> {
        private static final double WIDTH = 0.35;

        public Kaiju(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(KaijuProjectile entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers,
                int light) {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.lengthSqr() < 1.0E-6) {
                return;
            }
            Matrix4f m = pose.last().pose();
            Vec3 camera = entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partial));
            int color = entity.trailColor();
            Vec3 tail = motion.scale(-2.0);
            VertexConsumer glow = buffers.getBuffer(KN8RenderTypes.GLOW);
            Drawn.ribbon(glow, m, tail, Vec3.ZERO, 0.0, WIDTH, camera, color, 0.0F, 1.0F);
            Drawn.ribbon(glow, m, tail.scale(0.5), Vec3.ZERO, 0.0, WIDTH * 0.4, camera, VfxColors.shade(color, 0.6F),
                    0.0F, 1.0F);
            Drawn.disc(glow, m, Vec3.ZERO, WIDTH * 0.9, camera, color, 0.9F, 12);
        }

        @Override
        public ResourceLocation getTextureLocation(KaijuProjectile entity) {
            return TextureAtlas.LOCATION_BLOCKS;
        }
    }
}
