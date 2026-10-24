package com.kn8.common.host;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.site.SiteService;
import com.kn8.common.vfx.VfxService;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * T-1: a larva rara que infecta o futuro hospedeiro do Kaiju No. 8 (Miguel 2026-10-08: "o jogador tambem pode virar o
 * Kaiju No. 8, mas so depois de achar o inseto; ele e raro de encontrar, infecta o corpo e o jogador recebe um aviso";
 * 2026-10-24: "bem rara, no meio de restos de kaiju ou obras", "apenas um jogador"). O servidor decide tudo:
 * <ul>
 *   <li>de tempos em tempos ({@code [larva] checkTicks}) o jogador que esta em restos de kaiju (a construcao de restos
 *   ou uma carcaca por perto) ou dentro de um local da obra tira a sorte ({@code chance}); so se o mundo ainda nao tem
 *   hospedeiro nem larva voando;</li>
 *   <li>a larva aparece longe, circula o jogador e mergulha nele; ao encostar, entra no corpo (infecta), some e o
 *   jogador recebe o aviso; foge se o jogador se afasta demais ou se demora;</li>
 *   <li>a larva nao fere ninguem, nao pode ser ferida (os soldados nao a veem como inimiga) e nao alarma a regiao
 *   [SUPOSICAO: na obra ela entra no Kafka sem ninguem notar].</li>
 * </ul>
 * A infeccao e salva no jogador ({@code kn8:kaiju_host}, mantida na morte) e no mundo ({@link KaijuHostData}).
 */
public final class LarvaService {

    public static final String TAG_HOST = "kn8_larva_host";
    private static final String TAG_BORN = "kn8_larva_born";
    private static final ResourceLocation LARVA = KN8Constants.id("kaiju_larva");
    private static final ResourceKey<Structure> REMAINS = ResourceKey.create(Registries.STRUCTURE,
            KN8Constants.id("kaiju_remains"));
    private static final double HOVER_HEIGHT = 1.6;
    private static final double CIRCLE_RADIUS = 3.5;
    private static final int TITLE_FADE_IN = 10;
    private static final int TITLE_STAY = 70;
    private static final int TITLE_FADE_OUT = 20;
    private static final int NAUSEA_TICKS = 200;
    private static final int DARKNESS_TICKS = 60;

    private LarvaService() {
    }

    // --- estado do jogador ---------------------------------------------------------------------------------------

    public static boolean isHost(ServerPlayer player) {
        return player.getData(KN8Attachments.KAIJU_HOST.get()) >= 0;
    }

    /** Larva voando atras de um jogador (a das infeccoes; a de {@code /summon} e um kaiju comum). */
    public static boolean isHostLarva(Entity entity) {
        return entity instanceof KaijuEntity && entity.getPersistentData().hasUUID(TAG_HOST);
    }

    // --- sorteio ---------------------------------------------------------------------------------------------------

    /** Tick do jogador no servidor: sorteio da larva nos lugares certos. */
    public static void playerTick(ServerPlayer player) {
        if (!ServerConfig.LARVA_ENABLED.get() || player.tickCount % ServerConfig.LARVA_CHECK_TICKS.get() != 0
                || player.isSpectator() || !canAppearFor(player)) {
            return;
        }
        if (!inLarvaPlace(player.serverLevel(), player.blockPosition())) {
            return;
        }
        if (player.getRandom().nextDouble() < ServerConfig.LARVA_CHANCE.get()) {
            spawnFor(player);
        }
    }

    /** O mundo ainda nao tem hospedeiro, nao ha larva voando e este jogador nao foi infectado. */
    public static boolean canAppearFor(ServerPlayer player) {
        KaijuHostData data = KaijuHostData.get(player.getServer());
        return data.host() == null && !isHost(player) && larvaAlive(player.getServer(), data) == null;
    }

    /** Restos de kaiju (a construcao ou uma carcaca por perto) ou dentro de um local da obra. */
    public static boolean inLarvaPlace(ServerLevel level, BlockPos pos) {
        Structure remains = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(REMAINS);
        if (remains != null && level.structureManager().getStructureAt(pos, remains).isValid()) {
            return true;
        }
        if (SiteService.siteAt(level, pos).isPresent()) {
            return true;
        }
        double radius = ServerConfig.LARVA_CARCASS_RADIUS.get();
        return radius > 0 && !level.getEntitiesOfClass(CarcassEntity.class, new AABB(pos)
                .inflate(radius)).isEmpty();
    }

    /** Faz a larva aparecer longe do jogador, no ar (tambem pelo comando de teste). */
    public static Optional<KaijuEntity> spawnFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double distance = ServerConfig.LARVA_SPAWN_DISTANCE.get();
        Vec3 at = player.position().add(Math.cos(angle) * distance, 3.0, Math.sin(angle) * distance);
        Optional<KaijuEntity> spawned = KaijuSpawner.spawn(level, LARVA, BlockPos.containing(at));
        spawned.ifPresent(larva -> {
            larva.moveTo(at.x, at.y, at.z, larva.getYRot(), 0.0F);
            larva.setNoAi(true);
            larva.noPhysics = true;
            larva.setNoGravity(true);
            larva.setInvulnerable(true);
            larva.getPersistentData().putUUID(TAG_HOST, player.getUUID());
            larva.getPersistentData().putLong(TAG_BORN, level.getGameTime());
            KaijuHostData.get(player.getServer()).setLarva(larva.getUUID());
            // Um zumbido baixo: a unica pista de que algo apareceu.
            level.playSound(null, larva.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 0.6F,
                    0.6F);
        });
        return spawned;
    }

    // --- voo da larva ----------------------------------------------------------------------------------------------

    /** Tick da larva das infeccoes (evento de tick de entidade). */
    public static void larvaTick(KaijuEntity larva) {
        if (!(larva.level() instanceof ServerLevel level) || larva.isRemoved()) {
            return;
        }
        UUID hostId = larva.getPersistentData().getUUID(TAG_HOST);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(hostId);
        long age = level.getGameTime() - larva.getPersistentData().getLong(TAG_BORN);
        if (player == null || player.level() != level || player.isSpectator()
                || player.distanceTo(larva) > ServerConfig.LARVA_LOSE_DISTANCE.get()
                || age > ServerConfig.LARVA_LIFE_TICKS.get()) {
            flyAway(level, larva);
            return;
        }
        steer(larva, player, age);
    }

    /**
     * Um tick de voo: primeiro circula o jogador ({@code circleTicks}), depois mergulha no peito dele; ao encostar,
     * infecta. Publico para os GameTests (o jogador falso nao esta na lista de jogadores).
     */
    public static void steer(KaijuEntity larva, ServerPlayer player, long age) {
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.6, 0);
        Vec3 goal;
        if (age < ServerConfig.LARVA_CIRCLE_TICKS.get()) {
            double angle = age * 0.12;
            goal = player.position().add(Math.cos(angle) * CIRCLE_RADIUS, HOVER_HEIGHT + Math.sin(age * 0.3) * 0.3,
                    Math.sin(angle) * CIRCLE_RADIUS);
        } else {
            goal = chest;
        }
        Vec3 here = larva.position();
        Vec3 to = goal.subtract(here);
        double speed = ServerConfig.LARVA_SPEED.get();
        Vec3 next = to.length() <= speed ? goal : here.add(to.normalize().scale(speed));
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        larva.moveTo(next.x, next.y, next.z, yaw, 0.0F);
        larva.setYHeadRot(yaw);
        larva.setYBodyRot(yaw);
        if (age >= ServerConfig.LARVA_CIRCLE_TICKS.get()
                && next.distanceTo(chest) <= ServerConfig.LARVA_INFECT_RADIUS.get()) {
            if (infect(player)) {
                larva.discard();
            } else {
                flyAway((ServerLevel) larva.level(), larva);
            }
        }
    }

    private static void flyAway(ServerLevel level, KaijuEntity larva) {
        VfxService.keyed(level, VfxService.SMOKE, larva.position(), Vec3.ZERO, 0.4F, "#3a4a55", 0.0F);
        larva.discard();
        KaijuHostData data = KaijuHostData.get(level.getServer());
        if (larva.getUUID().equals(data.larva())) {
            data.setLarva(null);
        }
    }

    /** A larva registrada no mundo, se ainda existir em alguma dimensao (some do registro se nao existe mais). */
    @Nullable
    public static Entity larvaAlive(MinecraftServer server, KaijuHostData data) {
        if (data.larva() == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(data.larva());
            if (entity != null && entity.isAlive()) {
                return entity;
            }
        }
        data.setLarva(null);
        return null;
    }

    // --- infeccao --------------------------------------------------------------------------------------------------

    /** Infecta o jogador (falso se o mundo ja tem outro hospedeiro). Aviso, efeitos e estado salvo. */
    public static boolean infect(ServerPlayer player) {
        KaijuHostData data = KaijuHostData.get(player.getServer());
        if (data.host() != null && !data.host().equals(player.getUUID())) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        player.setData(KN8Attachments.KAIJU_HOST.get(), level.getGameTime());
        data.setHost(player.getUUID());
        data.setLarva(null);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0, false, false, true));
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 0.7F);
        VfxService.keyed(level, VfxService.SMOKE, player.position().add(0, 1.2, 0), Vec3.ZERO, 0.5F, "#1d2a33",
                0.0F);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("kn8.host.infected.title")
                .withStyle(ChatFormatting.DARK_AQUA)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(
                "kn8.host.infected.subtitle").withStyle(ChatFormatting.GRAY)));
        player.sendSystemMessage(Component.translatable("kn8.host.infected.chat").withStyle(ChatFormatting.DARK_AQUA));
        return true;
    }

    /** Desfaz a infeccao (comando de operador e GameTests). */
    public static void clear(ServerPlayer player) {
        player.setData(KN8Attachments.KAIJU_HOST.get(), -1L);
        KaijuHostData data = KaijuHostData.get(player.getServer());
        if (player.getUUID().equals(data.host())) {
            data.setHost(null);
        }
    }
}
