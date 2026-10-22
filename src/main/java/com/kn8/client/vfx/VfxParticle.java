// src/main/java/com/kn8/client/vfx/VfxParticle.java
package com.kn8.client.vfx;

import org.joml.Quaternionf;

import com.kn8.common.vfx.VfxParticleOptions;
import com.kn8.core.vfx.VfxColors;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/**
 * Particula do KaijuVFX (VFX-1): textura branca da silhueta tingida pela cor das opcoes. Cada silhueta tem o seu
 * movimento: o arco e o risco somem rapido e brilham; o anel fica deitado no chao e cresce; a lasca cai girando; o
 * brilho encolhe; o esporo sobe devagar e cresce. Vida e tamanho vem das opcoes (receita do perfil visual).
 */
public final class VfxParticle extends TextureSheetParticle {

    /** Comportamento por silhueta. */
    public enum Kind {
        ARC, RING, SHARD, STREAK, GLOW, SPORE, PULSE
    }

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Quaternionf FLAT_UP = new Quaternionf().rotationX(-Mth.HALF_PI);
    private static final Quaternionf FLAT_DOWN = new Quaternionf().rotationX(Mth.HALF_PI);

    private final Kind kind;
    private final float baseSize;
    private final float baseAlpha;
    private final float spin;

    VfxParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
            VfxParticleOptions options, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.kind = kind;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = Math.max(1, options.lifetime() + random.nextInt(Math.max(1, options.lifetime() / 4 + 1)));
        this.baseSize = options.size();
        this.quadSize = options.size();
        this.rCol = VfxColors.red(options.color());
        this.gCol = VfxColors.green(options.color());
        this.bCol = VfxColors.blue(options.color());
        this.baseAlpha = kind == Kind.SPORE ? 0.7F : kind == Kind.RING ? 0.85F : 1.0F;
        this.alpha = baseAlpha;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        this.spin = kind == Kind.SHARD ? (random.nextFloat() - 0.5F) * 0.6F : 0.0F;
        this.hasPhysics = kind == Kind.SHARD;
        this.gravity = switch (kind) {
            case SHARD -> 0.9F;
            case SPORE -> -0.02F;
            default -> 0.0F;
        };
        this.friction = switch (kind) {
            case STREAK, ARC, PULSE -> 0.8F;
            case SPORE -> 0.94F;
            case RING -> 0.6F;
            default -> 0.9F;
        };
        pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        roll += spin;
        float life = age / (float) lifetime;
        // Some no fim da vida (o esporo tambem aparece aos poucos no comeco).
        float fadeIn = kind == Kind.SPORE ? Math.min(1.0F, life * 5.0F) : 1.0F;
        alpha = baseAlpha * fadeIn * (1.0F - life * life);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float life = Mth.clamp((age + partialTicks) / lifetime, 0.0F, 1.0F);
        return switch (kind) {
            case RING -> baseSize * (0.3F + 0.7F * life);
            // Ar comprimido: abre rapido e quase nao cresce depois.
            case PULSE -> baseSize * (0.2F + 0.8F * (float) Math.sqrt(life));
            case GLOW -> baseSize * (1.0F - 0.6F * life);
            case SPORE -> baseSize * (0.6F + 0.6F * life);
            case ARC -> baseSize * (0.8F + 0.4F * life);
            default -> baseSize;
        };
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return kind == Kind.SHARD || kind == Kind.SPORE ? super.getLightColor(partialTicks) : FULL_BRIGHT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (kind == Kind.RING) {
            // Deitado no chao, visto de cima e de baixo.
            renderRotatedQuad(buffer, camera, FLAT_UP, partialTicks);
            renderRotatedQuad(buffer, camera, FLAT_DOWN, partialTicks);
            return;
        }
        super.render(buffer, camera, partialTicks);
    }

    /** Fabrica registrada por silhueta no {@code RegisterParticleProvidersEvent}. */
    public record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<VfxParticleOptions> {
        @Override
        public Particle createParticle(VfxParticleOptions options, ClientLevel level, double x, double y, double z,
                double vx, double vy, double vz) {
            return new VfxParticle(level, x, y, z, vx, vy, vz, options, sprites, kind);
        }
    }
}
