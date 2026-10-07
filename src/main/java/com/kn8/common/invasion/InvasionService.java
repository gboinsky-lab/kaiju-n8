package com.kn8.common.invasion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.boss.BossService;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.InvasionDef;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.numbered.No9Service;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.server.KN8Server;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Alertas de invasao (0.2, Etapa 7, GDD secao 21): sirene, barra no topo, ondas de kaiju vindo de fora para o centro,
 * soldados de defesa e recompensa para quem esteve na area. Os numeros vem do {@code invasion/*.json} e do config
 * ({@code invasion.*}). Uma invasao por dimensao.
 */
public final class InvasionService {

    /** Marca nos kaiju e soldados da invasao (dados persistentes da entidade). */
    public static final String TAG = KN8Constants.MOD_ID + "_invasion";
    private static final int LOGIC_INTERVAL = 20;
    private static final String RANDOM_VARIANT = "random";
    private static final int SIREN_INTERVAL = 100;
    private static final double MARCH_SPEED = 1.0;
    /** Kaiju sem alvo a menos disso do centro param de marchar (passeiam/atacam na area). */
    private static final double MARCH_STOP = 10.0;

    private InvasionService() {
    }

    public enum Result {
        OK, UNKNOWN, ALREADY_ACTIVE
    }

    public static Optional<Invasion> active(ServerLevel level) {
        KN8Server server = KN8Server.getOrNull(level.getServer());
        return server == null ? Optional.empty() : Optional.ofNullable(server.invasions().get(level.dimension()));
    }

    /** Comeca uma invasao centrada no ponto (comando, missao ou chance natural). */
    public static Result start(ServerLevel level, ResourceLocation id, BlockPos center) {
        Optional<InvasionDef> def = KN8Data.INVASION.get(id, false);
        if (def.isEmpty()) {
            return Result.UNKNOWN;
        }
        if (active(level).isPresent()) {
            return Result.ALREADY_ACTIVE;
        }
        ServerBossEvent bar = new ServerBossEvent(title(id, def.get().level(), Invasion.Phase.WARNING, 0,
                def.get().waves().size()), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setDarkenScreen(true);
        Invasion invasion = new Invasion(id, def.get(), center, level.getGameTime(), bar);
        KN8Server.get(level.getServer()).invasions().put(level.dimension(), invasion);
        spawnDefenders(level, invasion);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.translatable("kn8.invasion.alert", name(id), center.getX(),
                    center.getZ()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
        siren(level, invasion);
        KN8Constants.LOGGER.info("[kn8] Invasao {} em {} ({})", id, center.toShortString(), level.dimension()
                .location());
        sync(level, invasion);
        return Result.OK;
    }

    /** Para a invasao da dimensao (comando). Os kaiju que ja chegaram ficam. */
    public static boolean stop(ServerLevel level) {
        Optional<Invasion> invasion = active(level);
        invasion.ifPresent(value -> finish(level, value, Invasion.Phase.DEFEAT, false));
        return invasion.isPresent();
    }

    /** Pula o aviso/intervalo: a proxima onda vem no proximo segundo (comando e GameTests). */
    public static boolean skip(ServerLevel level) {
        Optional<Invasion> invasion = active(level).filter(value -> value.phase == Invasion.Phase.WARNING
                || value.phase == Invasion.Phase.BREAK);
        invasion.ifPresent(value -> value.phaseEndsTick = level.getGameTime());
        return invasion.isPresent();
    }

    /** Um tick do nivel. */
    public static void tick(ServerLevel level) {
        Optional<Invasion> found = active(level);
        if (found.isEmpty()) {
            return;
        }
        Invasion invasion = found.get();
        long now = level.getGameTime();
        if ((now - invasion.startTick) % SIREN_INTERVAL == 0 && (invasion.phase == Invasion.Phase.WARNING
                || invasion.phase == Invasion.Phase.BREAK)) {
            siren(level, invasion);
        }
        if ((now - invasion.startTick) % LOGIC_INTERVAL != 0) {
            return;
        }
        updatePlayers(level, invasion);
        switch (invasion.phase) {
            case WARNING, BREAK -> {
                if (now >= invasion.phaseEndsTick) {
                    startWave(level, invasion);
                }
            }
            case FIGHT -> {
                march(level, invasion);
                if (invasion.alive.isEmpty()) {
                    if (invasion.wave + 1 >= invasion.def.waves().size()) {
                        finish(level, invasion, Invasion.Phase.VICTORY, true);
                        return;
                    }
                    invasion.wave++;
                    invasion.phase = Invasion.Phase.BREAK;
                    invasion.phaseEndsTick = now + invasion.def.waves().get(invasion.wave).delayTicks();
                    announce(level, invasion, Component.translatable("kn8.invasion.wave_cleared", invasion.wave,
                            invasion.def.waves().size()).withStyle(ChatFormatting.GREEN));
                    sync(level, invasion);
                }
            }
            default -> {
            }
        }
        if (!invasion.ended() && now >= invasion.deadline) {
            finish(level, invasion, Invasion.Phase.DEFEAT, true);
            return;
        }
        updateBar(invasion);
    }

    /** 0.3: dano de jogador em kaiju da invasao (conta para a recompensa por contribuicao). */
    public static void onKaijuDamaged(ServerLevel level, KaijuEntity kaiju, ServerPlayer player, float amount) {
        active(level).filter(invasion -> invasion.alive.contains(kaiju.getUUID())).ifPresent(invasion ->
                invasion.damage.merge(player.getUUID(), amount, Float::sum));
    }

    /** Kaiju da invasao morreu (LivingDeathEvent); {@code killer} pode ser nulo (soldado, queda...). */
    public static void onKaijuDeath(ServerLevel level, KaijuEntity kaiju, ServerPlayer killer) {
        active(level).filter(invasion -> invasion.alive.remove(kaiju.getUUID())).ifPresent(invasion -> {
            invasion.killed++;
            if (killer != null) {
                invasion.kills.merge(killer.getUUID(), 1, Integer::sum);
            }
            updateBar(invasion);
            sync(level, invasion);
        });
    }

    /** Kaiju que entrou na invasao no meio (revivido pelo No. 9): precisa morrer para a onda acabar. */
    public static void join(ServerLevel level, KaijuEntity kaiju) {
        active(level).ifPresent(invasion -> {
            enlist(invasion, kaiju);
            sync(level, invasion);
        });
    }

    /** Kaiju que saiu sem morrer (o No. 9 fugindo): deixa de segurar a onda, sem contar como abate. */
    public static void release(ServerLevel level, UUID uuid) {
        active(level).filter(invasion -> invasion.alive.remove(uuid)).ifPresent(invasion -> sync(level, invasion));
    }

    // --- ondas ---------------------------------------------------------------------------------------------------

    private static void startWave(ServerLevel level, Invasion invasion) {
        InvasionDef.Wave wave = invasion.def.waves().get(invasion.wave);
        for (InvasionDef.Spawn spawn : wave.kaiju()) {
            for (int i = 0; i < spawn.count(); i++) {
                BlockPos pos = KaijuSpawner.surfaceAround(level, invasion.center, invasion.def.spawnMin(),
                        invasion.def.spawnMax(), level.random);
                KaijuSpawner.spawn(level, spawn.species(), pos).ifPresent(kaiju -> {
                    enlist(invasion, kaiju);
                    if (wave.massRevive() && kaiju instanceof KaijuNo9Entity) {
                        kaiju.getPersistentData().putBoolean(No9Service.MASS_TAG, true);
                    }
                });
            }
        }
        wave.boss().ifPresent(boss -> BossService.spawn(level, boss, KaijuSpawner.surfaceAround(level,
                invasion.center, invasion.def.spawnMin(), invasion.def.spawnMax(), level.random))
                .ifPresent(kaiju -> enlist(invasion, kaiju)));
        invasion.phase = Invasion.Phase.FIGHT;
        announce(level, invasion, Component.translatable("kn8.invasion.wave", invasion.wave + 1,
                invasion.def.waves().size()).withStyle(ChatFormatting.RED));
        level.playSound(null, invasion.center, KN8Sounds.KAIJU_ROAR.get(), SoundSource.HOSTILE, 6.0F, 0.6F);
        sync(level, invasion);
    }

    private static void enlist(Invasion invasion, KaijuEntity kaiju) {
        kaiju.getPersistentData().putBoolean(TAG, true);
        invasion.alive.add(kaiju.getUUID());
    }

    /** Kaiju sem alvo andam para o centro (senao ficariam passeando longe da area). */
    private static void march(ServerLevel level, Invasion invasion) {
        for (UUID uuid : List.copyOf(invasion.alive)) {
            Entity entity = level.getEntity(uuid);
            if (entity == null) {
                continue; // chunk descarregado: continua contando como vivo
            }
            if (!entity.isAlive()) {
                invasion.alive.remove(uuid);
                continue;
            }
            if (entity instanceof KaijuEntity kaiju && kaiju.getTarget() == null
                    && kaiju.distanceToSqr(invasion.center.getCenter()) > MARCH_STOP * MARCH_STOP) {
                kaiju.getNavigation().moveTo(invasion.center.getX() + 0.5, invasion.center.getY(),
                        invasion.center.getZ() + 0.5, MARCH_SPEED);
            }
        }
    }

    private static void spawnDefenders(ServerLevel level, Invasion invasion) {
        for (InvasionDef.Defender defender : invasion.def.defenders()) {
            // 0.6-D: variante com o nome de um soldado especial ("hoshina") chama o soldado especial.
            Optional<EntityType<? extends SoldierEntity>> special = KN8Entities.specialSoldier(defender.variant());
            for (int i = 0; i < defender.count(); i++) {
                SoldierEntity soldier = special.<SoldierEntity>map(type -> type.create(level))
                        .orElseGet(() -> KN8Entities.SOLDIER.get().create(level));
                if (soldier == null) {
                    continue;
                }
                BlockPos pos = KaijuSpawner.surfaceAround(level, invasion.center, 2, 8, level.random);
                soldier.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F,
                        0.0F);
                if (!RANDOM_VARIANT.equals(defender.variant()) && special.isEmpty()) {
                    // "random": o finalizeSpawn sorteia pelo peso do JSON (esquadrao misturado).
                    soldier.setVariant(defender.variant());
                }
                soldier.setPowerLevel(defender.level());
                soldier.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                soldier.getPersistentData().putBoolean(TAG, true);
                level.addFreshEntity(soldier);
            }
        }
    }

    // --- jogadores, barra e fim ----------------------------------------------------------------------------------

    private static boolean inArea(Player player, Invasion invasion) {
        double dx = player.getX() - (invasion.center.getX() + 0.5);
        double dz = player.getZ() - (invasion.center.getZ() + 0.5);
        double radius = invasion.def.radius();
        return dx * dx + dz * dz <= radius * radius;
    }

    private static void updatePlayers(ServerLevel level, Invasion invasion) {
        for (ServerPlayer player : List.copyOf(invasion.bar.getPlayers())) {
            if (player.level() != level || !inArea(player, invasion)) {
                invasion.bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (inArea(player, invasion) && !player.isSpectator()) {
                invasion.bar.addPlayer(player);
                if (invasion.phase == Invasion.Phase.FIGHT || invasion.phase == Invasion.Phase.BREAK) {
                    invasion.participants.add(player.getUUID());
                }
            }
        }
    }

    /** Total da invasao: o do JSON ou mais, quando o No. 9 revive kaiju no meio dela. */
    private static int total(Invasion invasion) {
        return Math.max(invasion.def.totalKaiju(), invasion.killed + invasion.alive.size());
    }

    private static void updateBar(Invasion invasion) {
        int total = Math.max(1, total(invasion));
        invasion.bar.setName(title(invasion, invasion.phase, invasion.wave, invasion.def.waves().size()));
        invasion.bar.setProgress(invasion.phase == Invasion.Phase.WARNING ? 1.0F
                : Math.max(0.0F, 1.0F - invasion.killed / (float) total));
    }

    private static Component title(Invasion invasion, Invasion.Phase phase, int wave, int waves) {
        return title(invasion.id, invasion.def.level(), phase, wave, waves);
    }

    private static Component title(ResourceLocation id, int level, Invasion.Phase phase, int wave, int waves) {
        return switch (phase) {
            case WARNING -> Component.translatable("kn8.invasion.bar.warning", level, name(id));
            case BREAK -> Component.translatable("kn8.invasion.bar.break", level, name(id), wave + 1, waves);
            default -> Component.translatable("kn8.invasion.bar.fight", level, name(id), wave + 1, waves);
        };
    }

    private static void finish(ServerLevel level, Invasion invasion, Invasion.Phase result, boolean rewards) {
        invasion.phase = result;
        invasion.bar.removeAllPlayers();
        KN8Server.get(level.getServer()).invasions().remove(level.dimension());
        boolean victory = result == Invasion.Phase.VICTORY;
        List<ServerPlayer> rewarded = new ArrayList<>();
        float teamDamage = (float) invasion.damage.values().stream().mapToDouble(Float::doubleValue).sum();
        int fighters = (int) invasion.damage.values().stream().filter(value -> value > 0).count();
        for (ServerPlayer player : level.players()) {
            boolean participant = invasion.participants.contains(player.getUUID());
            if (participant || inArea(player, invasion)) {
                player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(victory
                        ? "kn8.invasion.victory" : "kn8.invasion.defeat").withStyle(victory ? ChatFormatting.GOLD
                        : ChatFormatting.RED)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(name(invasion.id)));
            }
            if (victory && rewards && participant) {
                if (reward(player, invasion, teamDamage, fighters)) {
                    rewarded.add(player);
                }
            }
        }
        if (victory) {
            level.playSound(null, invasion.center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS,
                    2.0F, 1.0F);
        }
        KN8Constants.LOGGER.info("[kn8] Invasao {} terminou: {} ({} recompensado(s))", invasion.id, result,
                rewarded.size());
        PacketDistributor.sendToPlayersInDimension(level, InvasionStateS2C.NONE);
    }

    /**
     * Recompensa por contribuicao (0.3, pedido do Miguel: subir lutando, nao so por estar no local). Quem nao causou
     * dano em kaiju da invasao nao ganha a recompensa final. Quem lutou recebe a recompensa do JSON vezes
     * (sua parte do dano dos jogadores x numero de jogadores que lutaram), entre {@code invasion.rewardMinFactor}
     * e {@code invasion.rewardMaxFactor}: quem lutou igual aos outros ganha 100%; quem carregou ganha mais.
     * Abates e dano ja dao merito e XP de treino na hora (carreira), alem desta recompensa.
     */
    private static boolean reward(ServerPlayer player, Invasion invasion, float teamDamage, int fighters) {
        float dealt = invasion.damage.getOrDefault(player.getUUID(), 0.0F);
        int kills = invasion.kills.getOrDefault(player.getUUID(), 0);
        if (dealt <= 0.0F || teamDamage <= 0.0F) {
            player.sendSystemMessage(Component.translatable("kn8.invasion.no_contribution", name(invasion.id))
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }
        double factor = Math.max(ServerConfig.INVASION_REWARD_MIN_FACTOR.get(), Math.min(
                ServerConfig.INVASION_REWARD_MAX_FACTOR.get(), dealt / teamDamage * fighters));
        MissionDef.Rewards base = invasion.def.rewards();
        int merit = (int) Math.round(base.merit() * factor);
        int xp = (int) Math.round(base.trainingXp() * factor);
        MissionDef.Rewards rewards = new MissionDef.Rewards(merit, base.promoteTo(), base.items(), xp);
        for (ResourceLocation itemId : rewards.items()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        if (rewards.trainingXp() > 0) {
            PowerService.addTrainingXp(player, rewards.trainingXp());
        }
        player.sendSystemMessage(Component.translatable("kn8.invasion.reward", name(invasion.id), rewards.merit(),
                rewards.trainingXp()).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("kn8.invasion.contribution", kills, Math.round(dealt),
                Math.round(dealt / teamDamage * 100), Math.round(factor * 100)).withStyle(ChatFormatting.YELLOW));
        CareerService.data(player).addInvasionDefended();
        MissionService.progress(player, MissionDef.ObjectiveType.DEFEND_INVASION, invasion.id);
        CareerService.addMerit(player, rewards.merit());
        CareerService.changed(player);
        return true;
    }

    private static void siren(ServerLevel level, Invasion invasion) {
        double reach = invasion.def.radius() * 2.0;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(invasion.center.getCenter()) <= reach * reach) {
                // Toca perto do jogador (a sirene "da cidade" se ouve em toda a area).
                player.playNotifySound(KN8Sounds.SIREN.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            }
        }
    }

    private static void announce(ServerLevel level, Invasion invasion, Component message) {
        for (ServerPlayer player : level.players()) {
            if (inArea(player, invasion) || invasion.participants.contains(player.getUUID())) {
                player.displayClientMessage(message, true);
            }
        }
    }

    // --- sync e chance natural -----------------------------------------------------------------------------------

    public static InvasionStateS2C state(Invasion invasion) {
        return new InvasionStateS2C(invasion.phase.ordinal(), invasion.id, invasion.center, invasion.wave,
                invasion.def.waves().size(), invasion.alive.size(), total(invasion), invasion.timerEnd(),
                invasion.def.level());
    }

    private static void sync(ServerLevel level, Invasion invasion) {
        PacketDistributor.sendToPlayersInDimension(level, state(invasion));
    }

    /** Login, respawn ou troca de dimensao: manda o estado da dimensao atual. */
    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, active(player.serverLevel()).map(InvasionService::state)
                .orElse(InvasionStateS2C.NONE));
    }

    /**
     * Chance natural (config {@code invasion.naturalChance}), sorteada uma vez por dia no inicio da noite do
     * Overworld: escolhe um jogador na superficie e uma invasao pelo {@code natural_weight}.
     */
    public static void rollNatural(ServerLevel level) {
        if (!ServerConfig.INVASION_NATURAL.get() || level.dimension() != Level.OVERWORLD
                || active(level).isPresent() || level.random.nextDouble() >= ServerConfig.INVASION_NATURAL_CHANCE
                .get()) {
            return;
        }
        List<ServerPlayer> candidates = level.players().stream().filter(player -> !player.isSpectator()
                && !player.isCreative() && level.canSeeSky(player.blockPosition())).toList();
        if (candidates.isEmpty()) {
            return;
        }
        int totalWeight = KN8Data.INVASION.server().values().stream().mapToInt(InvasionDef::naturalWeight).sum();
        if (totalWeight <= 0) {
            return;
        }
        int roll = level.random.nextInt(totalWeight);
        for (Map.Entry<ResourceLocation, InvasionDef> entry : KN8Data.INVASION.server().entrySet()) {
            roll -= entry.getValue().naturalWeight();
            if (roll < 0) {
                ServerPlayer target = candidates.get(level.random.nextInt(candidates.size()));
                start(level, entry.getKey(), target.blockPosition());
                return;
            }
        }
    }

    public static Component name(ResourceLocation id) {
        return Component.translatable("kn8.invasion." + id.getPath());
    }
}
