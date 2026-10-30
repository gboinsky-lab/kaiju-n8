package com.kn8.common.boss;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.ai.AiTuning;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.ExamTargets;
import com.kn8.common.career.MissionService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.BossDef;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Sounds;
import com.kn8.common.vfx.VfxService;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;

/**
 * Chefes (0.2, Etapa 6, GDD secao 23): um kaiju dos dados com barra de chefe, fases por vida e invocacao de Yoju
 * da propria especie. A IA de ataque e a do kaiju; desde a IA-6 os ataques da fase atual ({@code attacks} do
 * {@code boss/*.json}) ganham {@code [kaijuAi] bossPhaseBonus} de prioridade e o peso decide o empate
 * (KaijuAbilities#choose). A fase so avanca (sem voltar com cura), entao nao ha troca repetida em laco.
 */
public final class BossService {

    private static final ResourceLocation HEALTH_MODIFIER = KN8Constants.id("boss_health");
    private static final ResourceLocation PHASE_SPEED_MODIFIER = KN8Constants.id("boss_phase_speed");
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
            VfxService.play(level, VfxService.ROAR, kaiju.position().add(0, kaiju.getBbHeight() * 0.6, 0),
                    kaiju.getLookAngle(), 2.0F, 0.0F, kaiju);
            // PM-5: logo depois da troca o nucleo fica exposto (janela para quem luta), contado a partir do fim da
            // invulnerabilidade da troca.
            if (next.exposeCoreTicks() > 0) {
                kaiju.exposeCore(next.invulnTicks() + next.exposeCoreTicks());
            }
            AiTuning.logMove(kaiju, "fase do chefe: " + (phase + 1) + " (velocidade x" + next.speedMultiplier()
                    + ", recargas x" + next.cooldownMultiplier() + ", distancia " + next.preferredDistance()
                    + ", nucleo exposto " + next.exposeCoreTicks() + " ticks)");
            Component message = next.message().map(key -> Component.translatable(key, phase + 1))
                    .orElseGet(() -> Component.translatable("kn8.boss.phase", phase + 1));
            for (ServerPlayer player : bar.getPlayers()) {
                player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
            }
            def.get().summon().ifPresent(summon -> summonMinions(level, kaiju, summon));
        }
        if (state.appliedPhase() != state.phase()) {
            // Modificadores da fase (tambem depois de carregar o mundo, sem repetir aviso, invocacao nem janela).
            state.setAppliedPhase(state.phase());
            phase(kaiju).ifPresent(current -> applyPhaseSpeed(kaiju, current.speedMultiplier()));
        }
    }

    /** PM-5: fase atual do chefe (vazio para kaiju comum). */
    public static Optional<BossDef.Phase> phase(KaijuEntity kaiju) {
        BossState state = kaiju.bossState();
        if (state == null) {
            return Optional.empty();
        }
        return KN8Data.BOSS.get(state.id(), false)
                .filter(def -> state.phase() >= 0 && state.phase() < def.phases().size())
                .map(def -> def.phases().get(state.phase()));
    }

    /** Recarga dos ataques na fase atual (ritmo). */
    public static float cooldownMultiplier(KaijuEntity kaiju) {
        return phase(kaiju).map(BossDef.Phase::cooldownMultiplier).orElse(1.0F);
    }

    /** Distancia que a fase atual prefere (0 = vai no alvo). */
    public static float preferredDistance(KaijuEntity kaiju) {
        return phase(kaiju).map(BossDef.Phase::preferredDistance).orElse(0.0F);
    }

    private static void applyPhaseSpeed(KaijuEntity kaiju, float multiplier) {
        AttributeInstance speed = kaiju.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(PHASE_SPEED_MODIFIER);
        if (multiplier != 1.0F) {
            speed.addTransientModifier(new AttributeModifier(PHASE_SPEED_MODIFIER, multiplier - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
                // EXAME: os invocados do chefe da prova tambem sao da prova (sem carcaca, sem alerta da regiao).
                ExamTargets.copy(boss, minion);
                VfxService.keyed(level, VfxService.SMOKE, minion.position().add(0, 1, 0), Vec3.ZERO,
                        minion.getBbWidth() * 0.5F + 0.5F, "", 0.0F);
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
