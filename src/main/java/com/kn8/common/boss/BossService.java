package com.kn8.common.boss;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.BossDef;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

/**
 * Chefes (0.2, Etapa 6, GDD secao 23): um kaiju dos dados com barra de chefe, fases por vida e invocacao de Yoju
 * da propria especie. A IA de ataque continua a do kaiju (as habilidades do JSON da especie).
 * [SUPOSICAO] os pesos de ataque por fase do {@code boss/*.json} ainda nao mudam a escolha da IA.
 */
public final class BossService {

    private static final ResourceLocation HEALTH_MODIFIER = KN8Constants.id("boss_health");
    private static final int BAR_UPDATE_INTERVAL = 10;
    private static final int SUMMON_SPREAD = 10;

    private BossService() {
    }

    /** Faz o chefe surgir no ponto (missao, alerta, comando). Vazio se o chefe ou a especie nao existir. */
    public static Optional<KaijuEntity> spawn(ServerLevel level, ResourceLocation bossId, BlockPos pos) {
        Optional<BossDef> def = KN8Data.BOSS.get(bossId, false);
        if (def.isEmpty()) {
            KN8Constants.LOGGER.warn("[kn8] Chefe desconhecido: {}", bossId);
            return Optional.empty();
        }
        Optional<KaijuEntity> spawned = KaijuSpawner.spawn(level, def.get().kaiju(), pos);
        spawned.ifPresent(kaiju -> {
            kaiju.setBossState(new BossState(bossId, 0));
            int players = level.getEntitiesOfClass(ServerPlayer.class, arena(kaiju, def.get())).size();
            double scaling = def.get().playerScaling().orElse(0.5);
            float multiplier = (float) (def.get().healthMultiplier() * (1.0 + scaling * Math.max(0, players - 1)));
            applyHealth(kaiju, multiplier);
            def.get().summon().ifPresent(summon -> summonMinions(level, kaiju, summon));
            level.playSound(null, kaiju.blockPosition(), KN8Sounds.KAIJU_ROAR.get(), SoundSource.HOSTILE, 4.0F,
                    0.7F);
        });
        return spawned;
    }

    /** Vida do chefe: multiplicador do JSON e por jogador na arena (enche a vida). */
    private static void applyHealth(KaijuEntity kaiju, float multiplier) {
        AttributeInstance health = kaiju.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(HEALTH_MODIFIER);
        if (multiplier != 1.0F) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, multiplier - 1.0F,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        kaiju.setHealth(kaiju.getMaxHealth());
    }

    /** Todo tick do chefe (servidor): barra, quem esta na arena e troca de fase. */
    public static void tick(KaijuEntity kaiju, BossState state) {
        Optional<BossDef> def = KN8Data.BOSS.get(state.id(), false);
        if (def.isEmpty() || !(kaiju.level() instanceof ServerLevel level)) {
            return;
        }
        float fraction = kaiju.getMaxHealth() > 0 ? kaiju.getHealth() / kaiju.getMaxHealth() : 0.0F;
        ServerBossEvent bar = state.bar(Component.translatable("kn8.boss." + state.id().getPath())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, fraction)));
        if (kaiju.tickCount % BAR_UPDATE_INTERVAL == 0) {
            List<ServerPlayer> inArena = level.getEntitiesOfClass(ServerPlayer.class, arena(kaiju, def.get()));
            for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
                if (!inArena.contains(player)) {
                    bar.removePlayer(player);
                }
            }
            inArena.forEach(bar::addPlayer);
        }
        int phase = phaseFor(def.get(), fraction);
        if (phase > state.phase()) {
            state.setPhase(phase);
            BossDef.Phase next = def.get().phases().get(phase);
            state.setInvulnerableUntil(level.getGameTime() + next.invulnTicks());
            level.playSound(null, kaiju.blockPosition(), KN8Sounds.KAIJU_ROAR.get(), SoundSource.HOSTILE, 5.0F, 0.6F);
            level.sendParticles(ParticleTypes.SONIC_BOOM, kaiju.getX(), kaiju.getY() + kaiju.getBbHeight() * 0.6,
                    kaiju.getZ(), 1, 0, 0, 0, 0);
            for (ServerPlayer player : bar.getPlayers()) {
                player.displayClientMessage(Component.translatable("kn8.boss.phase", phase + 1)
                        .withStyle(ChatFormatting.RED), true);
            }
            def.get().summon().ifPresent(summon -> summonMinions(level, kaiju, summon));
        }
    }

    /** Fase i esta ativa enquanto a vida estiver acima do {@code until_health} dela (a primeira que servir). */
    static int phaseFor(BossDef def, float fraction) {
        for (int i = 0; i < def.phases().size(); i++) {
            if (fraction > def.phases().get(i).untilHealth()) {
                return i;
            }
        }
        return Math.max(0, def.phases().size() - 1);
    }

    private static void summonMinions(ServerLevel level, KaijuEntity boss, BossDef.Summon summon) {
        long alive = level.getEntitiesOfClass(KaijuEntity.class, boss.getBoundingBox().inflate(64),
                kaiju -> kaiju != boss && kaiju.kaijuId().equals(summon.species())).size();
        int amount = (int) Math.min(summon.count(), Math.max(0, summon.maxAlive() - alive));
        for (int i = 0; i < amount; i++) {
            BlockPos spot = KaijuSpawner.surfaceAround(level, boss.blockPosition(), SUMMON_SPREAD * 0.5,
                    SUMMON_SPREAD, level.random);
            KaijuSpawner.spawn(level, summon.species(), spot).ifPresent(minion -> {
                minion.setTarget(boss.getTarget());
                level.sendParticles(ParticleTypes.LARGE_SMOKE, minion.getX(), minion.getY() + 1, minion.getZ(), 30,
                        1.0, 1.0, 1.0, 0.05);
            });
        }
    }

    /** Morte do chefe: merito e objetivo de missao para quem lutou, loot do chefe, barra some. */
    public static void onDefeated(KaijuEntity kaiju, BossState state, DamageSource source) {
        state.clearBar();
        if (!(kaiju.level() instanceof ServerLevel level)) {
            return;
        }
        Optional<BossDef> def = KN8Data.BOSS.get(state.id(), false);
        if (source.getEntity() instanceof ServerPlayer killer) {
            state.participants().add(killer.getUUID());
        }
        for (UUID uuid : state.participants()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                continue;
            }
            def.ifPresent(boss -> CareerService.addMerit(player, boss.rewards().merit()));
            MissionService.progress(player, MissionDef.ObjectiveType.DEFEAT_BOSS, state.id());
            player.sendSystemMessage(Component.translatable("kn8.boss.defeated",
                    Component.translatable("kn8.boss." + state.id().getPath())).withStyle(ChatFormatting.GOLD));
        }
        def.flatMap(boss -> boss.rewards().lootTable()).ifPresent(tableId -> {
            LootTable table = level.getServer().reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, tableId));
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, kaiju)
                    .withParameter(LootContextParams.ORIGIN, kaiju.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                    .create(LootContextParamSets.ENTITY);
            for (ItemStack stack : table.getRandomItems(params)) {
                kaiju.spawnAtLocation(stack);
            }
        });
    }

    private static AABB arena(KaijuEntity kaiju, BossDef def) {
        return kaiju.getBoundingBox().inflate(def.arena().radius());
    }
}
