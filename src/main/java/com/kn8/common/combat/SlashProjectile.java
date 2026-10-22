// src/main/java/com/kn8/common/combat/SlashProjectile.java
package com.kn8.common.combat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.SlashSpec;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.vfx.VfxService;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Corte a distancia (0.6-D): Kuuchi e Kosa-uchi do Hoshina e o especial da espada dele para o jogador. Voa reto, sem
 * gravidade, ate {@code range} blocos; atravessa entidades (cada uma leva uma vez) e para no primeiro bloco solido.
 * Aliados de quem cortou ficam de fora ({@link SpecialAttacks#isAlly}); kaiju levam no corpo, com o fator de dano
 * contra kaiju de quem cortou ({@code kaijuFactor}: soldado especial pelo JSON, jogador 1,0). Nao e salvo no mundo.
 *
 * <p>Cor, inclinacao e largura vao ao cliente como dados sincronizados; o arco do corte e desenhado la
 * ({@code SlashProjectileRenderer}, VFX-R2: forma desenhada, sem particulas; sem pacote por tick).</p>
 */
public class SlashProjectile extends Projectile {

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.FLOAT);
    /** 0.7-C: rastro reto de bala no lugar do arco (tiros do Reno, da Mina e da baioneta do Narumi). */
    private static final EntityDataAccessor<Boolean> BULLET =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final double FORWARD_SPAWN = 0.8;

    private final Set<UUID> struck = new HashSet<>();
    private float damage;
    private float kaijuFactor = 1.0F;
    private double range;
    private double travelled;
    private float explosionRadius;
    private int slowTicks;
    private int slowLevel = 1;
    private int exposeCoreTicks;
    private float vulnerable = 1.0F;
    /** IA-2: tiro/corte pesado de um especial pesado (quebra blocos onde acerta; 0 = nao quebra). */
    private int destructionPower;
    private float destructionRadius;
    private boolean destroyed;

    public SlashProjectile(EntityType<? extends SlashProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /**
     * Dispara os cortes do {@code spec} a partir dos olhos de quem corta, na direcao {@code direction}; devolve
     * quantos cortes sairam. O segundo corte de um par inclina para o outro lado (o "X").
     */
    public static int fire(LivingEntity owner, Vec3 direction, SlashSpec spec, float damage, float kaijuFactor) {
        return fire(owner, direction, spec, damage, kaijuFactor, 0, 0.0F);
    }

    /**
     * IA-2: com {@code destructionPower} > 0 o tiro/corte e pesado: quebra blocos no ponto onde acerta (explosao,
     * bloco ou primeiro alvo), no raio {@code destructionRadius} (ou no da explosao, se for maior).
     */
    public static int fire(LivingEntity owner, Vec3 direction, SlashSpec spec, float damage, float kaijuFactor,
            int destructionPower, float destructionRadius) {
        Vec3 dir = direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        for (int index = 0; index < spec.count(); index++) {
            float offset = spec.count() == 1 ? 0.0F
                    : spec.spreadDegrees() * (index / (float) (spec.count() - 1) * 2.0F - 1.0F);
            Vec3 heading = Vec3.directionFromRotation(pitch, yaw + offset);
            SlashProjectile slash = new SlashProjectile(KN8Entities.SLASH_PROJECTILE.get(), owner.level());
            slash.setOwner(owner);
            slash.damage = damage;
            slash.kaijuFactor = kaijuFactor;
            slash.range = spec.range();
            slash.entityData.set(COLOR, spec.color());
            slash.entityData.set(ROLL, index % 2 == 0 ? spec.rollDegrees() : -spec.rollDegrees());
            slash.entityData.set(WIDTH, spec.width());
            slash.entityData.set(BULLET, spec.bullet());
            slash.explosionRadius = spec.explosionRadius();
            slash.slowTicks = spec.slowTicks();
            slash.slowLevel = spec.slowLevel();
            slash.exposeCoreTicks = spec.exposeCoreTicks();
            slash.vulnerable = spec.vulnerable();
            slash.destructionPower = destructionPower;
            slash.destructionRadius = Math.max(destructionRadius, spec.explosionRadius());
            slash.setPos(owner.getEyePosition().subtract(0, 0.3, 0).add(heading.scale(FORWARD_SPAWN)));
            slash.setDeltaMovement(heading.scale(spec.speed()));
            slash.setYRot(yaw + offset);
            slash.setXRot(pitch);
            owner.level().addFreshEntity(slash);
        }
        return spec.count();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xB070FF);
        builder.define(ROLL, 0.0F);
        builder.define(WIDTH, 1.2F);
        builder.define(BULLET, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (level().isClientSide()) {
            setPos(position().add(motion));
            return;
        }
        Vec3 from = position();
        Vec3 to = from.add(motion);
        HitResult block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this));
        if (block.getType() != HitResult.Type.MISS) {
            to = block.getLocation();
        }
        Vec3 hit = strikeAlong(from, to);
        if (hit != null && explosionRadius > 0.0F) {
            explode(hit);
            return;
        }
        if (hit != null) {
            destroyAt(hit);
        }
        if (block.getType() != HitResult.Type.MISS) {
            destroyAt(to);
            if (explosionRadius > 0.0F) {
                explode(to);
                return;
            }
            VfxService.play((ServerLevel) level(), VfxService.IMPACT, to, motion, 0.6F, 0.0F, getOwner());
            discard();
            return;
        }
        setPos(to);
        travelled += motion.length();
        if (travelled > range) {
            if (explosionRadius > 0.0F) {
                explode(to);
                return;
            }
            discard();
        }
    }

    /**
     * 0.7-C: explosao do tiro do canhao. Fere uma vez quem estiver no raio (menos aliados e quem o tiro ja acertou),
     * com o mesmo dano e fator contra kaiju. IA-2: quebra blocos so se o tiro for pesado (especial pesado).
     */
    private void explode(Vec3 at) {
        ServerLevel server = (ServerLevel) level();
        VfxService.play(server, VfxService.EXPLOSION, at, getDeltaMovement(), explosionRadius, 0.3F);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 1.1F);
        destroyAt(at);
        if (getOwner() instanceof LivingEntity source) {
            DamageSource damageSource = damageSources().explosion(this, source);
            AABB area = new AABB(at, at).inflate(explosionRadius);
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, area,
                    living -> living != source && living.isAlive() && !SpecialAttacks.isAlly(source, living)
                            && !struck.contains(living.getUUID()))) {
                if (target.getBoundingBox().distanceToSqr(at) > explosionRadius * explosionRadius) {
                    continue;
                }
                struck.add(target.getUUID());
                target.invulnerableTime = 0;
                if (target.hurt(damageSource, target instanceof KaijuEntity ? damage * kaijuFactor : damage)) {
                    slow(target);
                }
            }
        }
        discard();
    }

    /** IA-2: quebra os blocos em volta do ponto, uma vez por tiro/corte, se for pesado. */
    private void destroyAt(Vec3 at) {
        if (destructionPower <= 0 || destroyed || !(level() instanceof ServerLevel server)) {
            return;
        }
        destroyed = true;
        DestructionService.request(server, at, new AbilityDef.Destruction(destructionRadius, destructionPower, false,
                1.0F), getOwner() != null ? getOwner() : this);
    }

    /** IA-2: golpe pesado (atravessa a pele endurecida do No. 9). */
    public boolean isHeavy() {
        return destructionPower > 0;
    }

    /** Tiro pesado que explode (canhao da Mina): o No. 9 le e desvia (esquiva do numbered). */
    public boolean isExplosive() {
        return explosionRadius > 0.0F;
    }

    /**
     * 0.7-C: municao congelante (Lentidao do JSON) em quem o tiro acertou. 2026-10-15: no kaiju, a area congelada
     * expoe o nucleo e fica vulneravel (todo dano, de qualquer aliado, multiplicado enquanto dura o gelo).
     */
    private void slow(LivingEntity target) {
        if (slowTicks > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowLevel - 1));
        }
        if (target instanceof KaijuEntity kaiju) {
            if (exposeCoreTicks > 0) {
                kaiju.exposeCore(exposeCoreTicks);
            }
            if (vulnerable > 1.0F) {
                kaiju.markVulnerable(Math.max(slowTicks, exposeCoreTicks), vulnerable);
            }
        }
    }

    /**
     * Fere quem a faixa do corte (largura {@code width}) cruzou neste tick, uma vez por alvo. Devolve o centro do
     * primeiro alvo atingido (onde o tiro explosivo explode) ou {@code null}.
     */
    private Vec3 strikeAlong(Vec3 from, Vec3 to) {
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity source)) {
            return null;
        }
        Vec3 first = null;
        double width = entityData.get(WIDTH);
        AABB swept = new AABB(from, to).inflate(width);
        DamageSource damageSource = damageSources().mobProjectile(this, source);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, swept,
                living -> living != source && living.isAlive() && !SpecialAttacks.isAlly(source, living))) {
            if (!struck.add(target.getUUID())) {
                continue;
            }
            float amount = target instanceof KaijuEntity ? damage * kaijuFactor : damage;
            // Os dois cortes do Kosa-uchi chegam quase juntos: a invulnerabilidade vanilla engoliria o segundo.
            target.invulnerableTime = 0;
            if (target.hurt(damageSource, amount)) {
                slow(target);
                Vec3 center = target.getBoundingBox().getCenter();
                VfxService.play((ServerLevel) level(), VfxService.IMPACT, center, getDeltaMovement(), 0.6F, 0.0F,
                        getOwner());
                if (first == null) {
                    first = center;
                }
            }
        }
        return first;
    }

    /** VFX-R2: o que o desenho do corte precisa no cliente (cor, inclinacao, largura, bala). */
    public int color() {
        return entityData.get(COLOR);
    }

    public float roll() {
        return entityData.get(ROLL);
    }

    public float width() {
        return entityData.get(WIDTH);
    }

    public boolean bullet() {
        return entityData.get(BULLET);
    }

    @Override
    protected void onHit(HitResult result) {
        // O voo e resolvido em tick() (atravessa entidades, para em bloco).
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public float damage() {
        return damage;
    }
}
