package com.kn8.common.numbered;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.NumberedDef;
import com.kn8.common.invasion.InvasionService;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * O que o Kaiju No. 9 faz alem de lutar (0.2, Etapa 8; numeros no {@code numbered/kaiju_no9.json}):
 * <ul>
 *   <li><b>reviver</b>: para perto de uma carcaca da tabela {@code revive}, faz o gesto por {@code revive_cast_ticks}
 *   e a carcaca levanta como a especie revivida (primigenius -> ressurgido, Honju -> revivido);</li>
 *   <li><b>comandar</b>: os kaiju por perto sem alvo atacam o alvo dele;</li>
 *   <li><b>fugir</b>: com a vida em {@code flee_health} ele some (vilao recorrente) e quem lutava ganha merito.</li>
 * </ul>
 */
public final class No9Service {

    /** Marca nos kaiju levantados pelo No. 9. */
    public static final String KN8_REVIVED = KN8Constants.MOD_ID + "_revived";
    private static final int THINK_INTERVAL = 10;
    private static final int PARTICLE_INTERVAL = 4;
    private static final double ANNOUNCE_RADIUS = 64.0;
    private static final double FLEE_REWARD_RADIUS = 48.0;
    /** Verde-acido das costuras dos revividos (a "assinatura" do No. 9). */
    private static final DustParticleOptions ACID = new DustParticleOptions(new Vector3f(0.45F, 1.0F, 0.2F), 1.4F);

    private No9Service() {
    }

    public static Optional<NumberedDef> def(KaijuEntity kaiju) {
        return KN8Data.NUMBERED.get(kaiju.kaijuId(), false);
    }

    static void tick(KaijuNo9Entity no9) {
        Optional<NumberedDef> found = def(no9);
        if (found.isEmpty() || !(no9.level() instanceof ServerLevel level)) {
            return;
        }
        NumberedDef def = found.get();
        if (!no9.fled && no9.getHealth() <= no9.getMaxHealth() * def.fleeHealth()) {
            flee(level, no9, def);
            return;
        }
        long now = level.getGameTime();
        if (no9.reviving != null) {
            tickRevive(level, no9, def, now);
            return;
        }
        if (no9.tickCount % THINK_INTERVAL != 0) {
            return;
        }
        command(level, no9, def);
        no9.revived.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });
        if (now >= no9.nextReviveAt && no9.revived.size() < def.maxRevivedAlive() && !no9.isUsingAbility()) {
            nearestCarcass(level, no9, def).ifPresent(carcass -> startRevive(level, no9, def, carcass, now));
        }
    }

    // --- reviver -------------------------------------------------------------------------------------------------

    private static Optional<CarcassEntity> nearestCarcass(ServerLevel level, KaijuNo9Entity no9, NumberedDef def) {
        AABB area = no9.getBoundingBox().inflate(def.reviveRadius());
        return level.getEntitiesOfClass(CarcassEntity.class, area, carcass -> carcass.isAlive()
                        && def.revive().containsKey(carcass.species())).stream()
                .min(Comparator.comparingDouble(no9::distanceToSqr));
    }

    private static void startRevive(ServerLevel level, KaijuNo9Entity no9, NumberedDef def, CarcassEntity carcass,
            long now) {
        no9.reviving = carcass.getUUID();
        no9.reviveEndsAt = now + def.reviveCastTicks();
        no9.getNavigation().stop();
        no9.triggerAnim("special", "revive");
        level.playSound(null, no9.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F,
                0.6F);
        announce(level, no9, Component.translatable("kn8.no9.reviving",
                Component.translatable("entity." + carcass.species().getNamespace() + "." + carcass.species()
                        .getPath())).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void tickRevive(ServerLevel level, KaijuNo9Entity no9, NumberedDef def, long now) {
        Entity entity = level.getEntity(no9.reviving);
        if (!(entity instanceof CarcassEntity carcass) || !carcass.isAlive()) {
            // Desmontaram a carcaca a tempo: o gesto falha.
            no9.reviving = null;
            no9.nextReviveAt = now + def.reviveCooldownTicks() / 2;
            return;
        }
        no9.getNavigation().stop();
        no9.getLookControl().setLookAt(carcass);
        if (no9.tickCount % PARTICLE_INTERVAL == 0) {
            // Fio verde das maos ate a carcaca.
            Vec3 from = no9.position().add(0, no9.getBbHeight() * 0.6, 0);
            Vec3 to = carcass.position().add(0, carcass.getBbHeight() * 0.5, 0);
            for (int i = 0; i <= 8; i++) {
                Vec3 point = from.lerp(to, i / 8.0);
                level.sendParticles(ACID, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            level.sendParticles(ParticleTypes.SOUL, to.x, to.y, to.z, 4, carcass.getBbWidth() * 0.3, 0.4,
                    carcass.getBbWidth() * 0.3, 0.02);
        }
        if (now < no9.reviveEndsAt) {
            return;
        }
        no9.reviving = null;
        no9.nextReviveAt = now + def.reviveCooldownTicks();
        ResourceLocation species = def.revive().get(carcass.species());
        BlockPos pos = carcass.blockPosition();
        float yaw = carcass.getYRot();
        carcass.discard();
        KaijuSpawner.spawn(level, species, pos).ifPresent(revived -> {
            revived.setYRot(yaw);
            revived.getPersistentData().putBoolean(KN8_REVIVED, true);
            no9.revived.add(revived.getUUID());
            if (no9.getPersistentData().getBoolean(InvasionService.TAG)) {
                InvasionService.join(level, revived);
            }
            if (no9.getTarget() != null) {
                revived.setTarget(no9.getTarget());
            }
            level.sendParticles(ACID, revived.getX(), revived.getY() + revived.getBbHeight() * 0.5, revived.getZ(),
                    60, revived.getBbWidth() * 0.4, revived.getBbHeight() * 0.4, revived.getBbWidth() * 0.4, 0.0);
            level.playSound(null, pos, KN8Sounds.KAIJU_ROAR.get(), SoundSource.HOSTILE, 4.0F, 0.8F);
            announce(level, no9, Component.translatable("kn8.no9.revived", revived.getDisplayName())
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        });
    }

    // --- comandar e fugir ----------------------------------------------------------------------------------------

    private static void command(ServerLevel level, KaijuNo9Entity no9, NumberedDef def) {
        LivingEntity target = no9.getTarget();
        if (target == null || def.commandRadius() <= 0) {
            return;
        }
        for (KaijuEntity kaiju : level.getEntitiesOfClass(KaijuEntity.class,
                no9.getBoundingBox().inflate(def.commandRadius()), kaiju -> kaiju != no9 && kaiju.isAlive()
                        && kaiju.getTarget() == null)) {
            kaiju.setTarget(target);
        }
    }

    private static void flee(ServerLevel level, KaijuNo9Entity no9, NumberedDef def) {
        no9.fled = true;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, no9.getX(), no9.getY() + 1.0, no9.getZ(), 40, 0.5, 1.0, 0.5,
                0.05);
        level.sendParticles(ACID, no9.getX(), no9.getY() + 1.0, no9.getZ(), 40, 0.4, 1.0, 0.4, 0.0);
        level.playSound(null, no9.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
        announce(level, no9, Component.translatable("kn8.no9.fled").withStyle(ChatFormatting.DARK_GREEN,
                ChatFormatting.ITALIC));
        List<ServerPlayer> fighters = level.getEntitiesOfClass(ServerPlayer.class,
                no9.getBoundingBox().inflate(FLEE_REWARD_RADIUS), player -> !player.isSpectator());
        for (ServerPlayer player : fighters) {
            if (def.fleeMerit() > 0) {
                player.sendSystemMessage(Component.translatable("kn8.no9.flee_merit", def.fleeMerit())
                        .withStyle(ChatFormatting.GOLD));
                CareerService.addMerit(player, def.fleeMerit());
            }
        }
        InvasionService.release(level, no9.getUUID());
        KN8Constants.LOGGER.info("[kn8] Kaiju No. 9 fugiu em {}", no9.blockPosition().toShortString());
        no9.discard();
    }

    private static void announce(ServerLevel level, Entity source, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(source) <= ANNOUNCE_RADIUS * ANNOUNCE_RADIUS) {
                player.displayClientMessage(message, true);
            }
        }
    }
}
