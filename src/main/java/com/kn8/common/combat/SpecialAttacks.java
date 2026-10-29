// src/main/java/com/kn8/common/combat/SpecialAttacks.java
package com.kn8.common.combat;

import java.util.List;

import com.kn8.common.evacuation.CivilianEntity;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.server.KN8Server;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.combat.SpecialGeometry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Resolvedor dos ataques especiais das armas (0.5), no tick de impacto do JSON. Nao conhece jogador nem soldado:
 * quem chama passa o dano ja calculado (jogador: Release; soldado especial: o nivel dele) e a fonte de dano. Assim o
 * mesmo ataque serve ao jogador e, depois, aos soldados especiais (docs/PLANO_SOLDADOS_ESPECIAIS.md).
 */
public final class SpecialAttacks {

    private static final float SOUND_VOLUME = 2.0F;
    /** Folga da busca: a hitbox de um kaiju largo pode estar com o centro bem fora do raio. */
    private static final double SEARCH_MARGIN = 8.0;
    /** COMBATE-3: mergulho do Falling Thunder: deriva para a frente e folga do toque no kaiju (blocos). */
    private static final double DIVE_FORWARD = 0.25;
    private static final double DIVE_TOUCH_BELOW = 0.6;
    private static final double DIVE_TOUCH_SIDE = 0.4;
    /** Empurrao vertical da onda (sai do chao). */
    private static final double KNOCK_UP = 0.35;

    private SpecialAttacks() {
    }

    /** Centro do golpe: {@code forward} blocos a frente, no chao (pes de quem ataca). */
    public static Vec3 center(LivingEntity source, WeaponDef.Special special) {
        Vec3 look = source.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 forward = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(special.forward());
        return source.position().add(forward);
    }

    /**
     * Resolve o ataque especial; devolve quantos alvos levaram dano. Aliados de quem ataca (soldados, jogadores sem
     * PvP) ficam de fora; kaiju levam no corpo (sem nucleo, como os outros golpes de area).
     */
    public static int resolve(LivingEntity source, WeaponDef.Special special, float damage, DamageSource damageSource) {
        if (!(source.level() instanceof ServerLevel level)) {
            return 0;
        }
        return switch (special.type()) {
            case GROUND_SLAM -> groundSlam(level, source, special, damage, damageSource);
            case SLASH_WAVE -> slashWave(level, source, special, damage);
            case LUNGE -> lunge(level, source, special, damage, damageSource);
            case FLURRY -> flurry(level, source, special, damage, damageSource);
            case DIVE_SLAM -> diveSlam(level, source, special, damage, damageSource);
        };
    }

    /** COMBATE-3: altura (blocos) dos pes ate o chao logo abaixo, ate {@code max} (sem chao ate la = {@code max}). */
    public static double heightAboveGround(LivingEntity source, double max) {
        Vec3 feet = source.position();
        HitResult hit = source.level().clip(new ClipContext(feet, feet.subtract(0, max, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, source));
        return hit.getType() == HitResult.Type.MISS ? max : feet.y - hit.getLocation().y;
    }

    /**
     * COMBATE-3: comeco do mergulho do {@code dive_slam} (Falling Thunder do jogador): para baixo a
     * {@code [combat] diveSlamSpeed}, um pouco para a frente (de cima do kaiju, cai nele).
     */
    public static void startDive(LivingEntity source) {
        Vec3 look = source.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 forward = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(DIVE_FORWARD);
        source.setDeltaMovement(forward.x, -ServerConfig.DIVE_SLAM_SPEED.get(), forward.z);
        source.hurtMarked = true;
    }

    /** COMBATE-3: o mergulho encostou num inimigo (caixa de quem cai, um pouco para baixo, cruza a dele)? */
    public static boolean diveTouches(LivingEntity source) {
        AABB below = source.getBoundingBox().expandTowards(0, -DIVE_TOUCH_BELOW, 0).inflate(DIVE_TOUCH_SIDE, 0,
                DIVE_TOUCH_SIDE);
        return !source.level().getEntitiesOfClass(LivingEntity.class, below,
                target -> target != source && target.isAlive() && !isAlly(source, target)).isEmpty();
    }

    /** COMBATE-3: o toque do mergulho: esmaga a area em volta de onde quem caiu tocou (raio pela borda). */
    private static int diveSlam(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage,
            DamageSource damageSource) {
        source.fallDistance = 0.0F;
        return areaHit(level, source, special, damage, damageSource, source.position());
    }

    /**
     * 0.6-D: corte que voa reto pelo olhar de quem ataca ({@link SlashProjectile}); o dano sai quando o corte cruza
     * cada alvo, nao neste tick. Devolve quantos cortes sairam.
     */
    private static int slashWave(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage) {
        int fired = SlashProjectile.fire(source, source.getLookAngle(), special.slash(), damage, 1.0F);
        effects(level, special, source.getEyePosition().add(source.getLookAngle()), source);
        return fired;
    }

    private static int groundSlam(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage,
            DamageSource damageSource) {
        return areaHit(level, source, special, damage, damageSource, center(source, special));
    }

    private static int areaHit(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage,
            DamageSource damageSource, Vec3 center) {
        double reach = special.radius() + SEARCH_MARGIN;
        AABB search = new AABB(center.x - reach, center.y - SpecialGeometry.SLAM_BELOW, center.z - reach,
                center.x + reach, center.y + SpecialGeometry.SLAM_HEIGHT, center.z + reach);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target != source && target.isAlive() && !isAlly(source, target));
        int hits = 0;
        for (LivingEntity target : targets) {
            AABB box = target.getBoundingBox();
            if (!SpecialGeometry.inGroundSlam(center.x, center.y, center.z, special.radius(), box.minX, box.minY,
                    box.minZ, box.maxX, box.maxY, box.maxZ)) {
                continue;
            }
            if (hit(special, target, damage, damageSource, center)) {
                hits++;
            }
        }
        effects(level, special, center, source);
        return hits;
    }

    /**
     * PM-1: corte em avanco (espada). Quem ataca e lancado {@code forward} blocos a frente (o corpo vai junto do
     * corte; jogador pelo {@code hurtMarked}) e todo inimigo na faixa do caminho, com caminho livre desde quem ataca,
     * leva o golpe uma vez.
     */
    private static int lunge(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage,
            DamageSource damageSource) {
        Vec3 look = source.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return 0;
        }
        Vec3 direction = flat.normalize();
        Vec3 start = source.position();
        double length = special.forward();
        double speed = length * ServerConfig.LUNGE_SPEED_PER_BLOCK.get();
        source.setDeltaMovement(direction.x * speed, Math.max(source.getDeltaMovement().y, 0.1), direction.z * speed);
        source.hurtMarked = true;
        Vec3 end = start.add(direction.scale(length));
        AABB search = new AABB(start, end).inflate(special.radius() + SEARCH_MARGIN, SpecialGeometry.SLAM_HEIGHT,
                special.radius() + SEARCH_MARGIN);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target != source && target.isAlive() && !isAlly(source, target));
        int hits = 0;
        for (LivingEntity target : targets) {
            AABB box = target.getBoundingBox();
            if (!SpecialGeometry.inLunge(start.x, start.y, start.z, direction.x, direction.z, length, special.radius(),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
                    || !clearPath(level, source, box.getCenter())) {
                continue;
            }
            // COMBATE-3: Twin Swallows = dois cortes no mesmo avanco (slash.count); cada um zera a invulnerabilidade.
            boolean struck = false;
            for (int cut = 0; cut < Math.max(1, special.slash().count()); cut++) {
                target.invulnerableTime = 0;
                struck |= hit(special, target, damage, damageSource, start);
            }
            if (struck) {
                hits++;
            }
        }
        effects(level, special, start.add(direction.scale(length * 0.5)), source);
        return hits;
    }

    /**
     * PM-1: sequencia de cortes (faca): {@code [combat] flurryHits} cortes, um a cada {@code flurryIntervalTicks},
     * em quem esta a frente ate {@code radius} (sem atravessar parede). O primeiro sai agora; os outros pela fila de
     * tarefas do servidor e param se quem ataca morrer. Cada corte zera a invulnerabilidade vanilla (como os golpes
     * das tecnicas): sem isso o segundo corte seria engolido. Devolve os alvos do primeiro corte.
     */
    private static int flurry(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage,
            DamageSource damageSource) {
        int first = flurryCut(level, source, special, damage, damageSource, 0);
        KN8Server server = KN8Server.get(level.getServer());
        // COMBATE-3: slash.count > 1 da o numero de cortes da tecnica (Daruma Dropper 4, Ten Hit 10).
        int cuts = special.slash().count() > 1 ? special.slash().count() : ServerConfig.FLURRY_HITS.get();
        for (int cut = 1; cut < cuts; cut++) {
            int index = cut;
            server.schedule(cut * ServerConfig.FLURRY_INTERVAL_TICKS.get(),
                    () -> flurryCut(level, source, special, damage, damageSource, index));
        }
        return first;
    }

    private static int flurryCut(ServerLevel level, LivingEntity source, WeaponDef.Special special, float damage,
            DamageSource damageSource, int cut) {
        if (!source.isAlive() || source.level() != level) {
            return 0;
        }
        Vec3 look = source.getLookAngle();
        Vec3 eye = source.getEyePosition();
        double reach = special.radius();
        AABB search = source.getBoundingBox().inflate(reach + SEARCH_MARGIN);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target != source && target.isAlive() && !isAlly(source, target));
        int hits = 0;
        for (LivingEntity target : targets) {
            AABB box = target.getBoundingBox();
            Vec3 to = box.getCenter().subtract(eye);
            boolean inFront = look.x * to.x + look.z * to.z > 0;
            if (!inFront || SpecialGeometry.horizontalDistanceToBox(source.getX(), source.getZ(), box.minX, box.minZ,
                    box.maxX, box.maxZ) > reach || !clearPath(level, source, box.getCenter())) {
                continue;
            }
            target.invulnerableTime = 0;
            if (hit(special, target, damage, damageSource, source.position())) {
                hits++;
            }
        }
        Vec3 center = eye.add(look.scale(Math.min(reach, 1.5)));
        if (cut == 0) {
            effects(level, special, center, source);
        } else {
            // Os cortes seguintes so com o desenho (sem tremer a camera nem repetir o som de cada vez). COMBATE-5:
            // chave "<tecnica>/<corte>" (como os golpes das tecnicas dos especiais): o cliente procura a receita do
            // corte, depois a da tecnica.
            VfxService.play(level, VfxService.SLASH, center, look, 0.7F, 0.0F, source,
                    special.id() + "/" + (cut + 1));
        }
        return hits;
    }

    /** Golpe do especial num alvo: dano, empurrao para longe de {@code from} e atordoamento de Yoju. */
    private static boolean hit(WeaponDef.Special special, LivingEntity target, float damage,
            DamageSource damageSource, Vec3 from) {
        if (!target.hurt(damageSource, damage)) {
            return false;
        }
        Vec3 away = new Vec3(target.getX() - from.x, 0, target.getZ() - from.z);
        if (special.knockback() > 0 && away.lengthSqr() > 1.0E-4) {
            // knockback() aplica a resistencia do alvo (kaiju grande quase nao sai do lugar).
            target.knockback(special.knockback(), -away.x, -away.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0, KNOCK_UP * special.knockback()
                    * (1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)), 0));
            target.hurtMarked = true;
        }
        if (special.staggerTicks() > 0 && target instanceof KaijuEntity kaiju
                && kaiju.def().map(def -> def.kaijuClass() == KaijuClass.YOJU).orElse(false)) {
            // Como o parry: so Yoju fica atordoado; Honju e numerados aguentam o tranco.
            kaiju.stagger(special.staggerTicks());
        }
        return true;
    }

    /** Nenhum bloco solido entre os olhos de quem ataca e o ponto (o corte nao atravessa parede). */
    private static boolean clearPath(ServerLevel level, LivingEntity source, Vec3 point) {
        return level.clip(new ClipContext(source.getEyePosition(), point, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, source)).getType() == HitResult.Type.MISS;
    }

    /** Aliado de quem ataca (soldados, suportes de armadura, jogadores sem PvP): fica fora do golpe. */
    public static boolean isAlly(LivingEntity source, LivingEntity target) {
        if (SoldierEntity.isDuelFoe(source, target)) {
            return false;
        }
        if (target instanceof ArmorStand || target instanceof SoldierEntity || target instanceof CivilianEntity) {
            return true;
        }
        if (target instanceof Player) {
            return !(source instanceof Player) || !ServerConfig.PVP_ENABLED.get();
        }
        return false;
    }

    private static void effects(ServerLevel level, WeaponDef.Special special, Vec3 center, LivingEntity source) {
        Vec3 look = source.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0, look.z);
        boolean first = true;
        for (AbilityDef.Vfx vfx : special.vfx()) {
            // PM-1: o primeiro efeito leva o id do especial como receita do perfil visual de quem ataca (sem
            // receita, o efeito comum).
            VfxService.play(level, vfx.effect(), center, direction, vfx.intensity(),
                    first ? special.cameraShake() : 0.0F, source, first ? special.id() : "");
            first = false;
        }
        SoundEvent sound = special.sound()
                .map(id -> BuiltInRegistries.SOUND_EVENT.getOptional(id)
                        .orElseGet(() -> SoundEvent.createVariableRangeEvent(id)))
                .orElse(KN8Sounds.AXE_SPECIAL.get());
        level.playSound(null, center.x, center.y, center.z, sound, source instanceof Player ? SoundSource.PLAYERS
                : SoundSource.NEUTRAL, SOUND_VOLUME, 1.0F);
    }
}
