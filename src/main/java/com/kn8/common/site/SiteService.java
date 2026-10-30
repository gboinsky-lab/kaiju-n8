// src/main/java/com/kn8/common/site/SiteService.java
package com.kn8.common.site;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.InvasionDef;
import com.kn8.common.data.def.SiteDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.server.KN8Server;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Locais da obra no mundo (construcoes C3): o servidor confere a cada {@code [sites] checkIntervalTicks} em que
 * local cada jogador esta; ao entrar mostra o nome e a divisao na tela e, na primeira visita do mundo, faz aparecer
 * a guarnicao (personagens da obra). A invasao natural usa as invasoes do local quando cai num jogador dentro dele.
 */
public final class SiteService {

    /** Local encontrado: id, definicao e a estrutura gerada. */
    public record Hit(ResourceLocation id, SiteDef def, StructureStart start) {

        /** Ponto de chegada: rua da saida oeste da peca inicial (as quatro saidas sao rua nas quatro pecas). */
        public BlockPos gate() {
            BoundingBox box = start.getPieces().get(0).getBoundingBox();
            return new BlockPos(box.minX() + 3, box.minY() + 1, (box.minZ() + box.maxZ()) / 2);
        }

        /**
         * Onde a guarnicao aparece: o ponto {@code garrison_at} dentro da peca pedida (BASES-1: o patio da base dentro
         * da cidade), girado como a peca; sem ele (ou sem a peca gerada), {@link #gate()}.
         */
        public BlockPos garrisonPos() {
            return def.garrisonAt().flatMap(at -> start.getPieces().stream()
                    .filter(piece -> piece instanceof PoolElementStructurePiece pool
                            && isTemplate(pool, at.piece()))
                    .findFirst()
                    .map(piece -> {
                        PoolElementStructurePiece pool = (PoolElementStructurePiece) piece;
                        return pool.getPosition().offset(StructureTemplate.transform(at.pos(), Mirror.NONE,
                                pool.getRotation(), BlockPos.ZERO));
                    })).orElseGet(this::gate);
        }

        /**
         * O template do elemento e protegido no {@code SinglePoolElement}; o {@code toString} dele traz o id
         * ("Single[Left[kn8:tachikawa_base/hq]]"). Comeca com o id: vale tambem a variante pos-ataque
         * ({@code hq_ruined}, BASES-3), desenhada no mesmo lugar.
         */
        private static boolean isTemplate(PoolElementStructurePiece piece, ResourceLocation template) {
            return piece.getElement() instanceof SinglePoolElement
                    && piece.getElement().toString().contains("[" + template);
        }
    }

    private SiteService() {
    }

    /** Local (estrutura inteira, pela caixa) que contem a posicao. */
    public static Optional<Hit> siteAt(ServerLevel level, BlockPos pos) {
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<ResourceLocation, SiteDef> entry : KN8Data.SITE.server().entrySet()) {
            Structure structure = structures.get(entry.getValue().structure());
            if (structure == null) {
                continue;
            }
            StructureStart start = level.structureManager().getStructureAt(pos, structure);
            if (start.isValid()) {
                return Optional.of(new Hit(entry.getKey(), entry.getValue(), start));
            }
        }
        return Optional.empty();
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % ServerConfig.SITE_CHECK_TICKS.get() != 0 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Optional<Hit> hit = siteAt(level, player.blockPosition());
        Map<UUID, ResourceLocation> sites = KN8Server.get(level.getServer()).playerSites();
        ResourceLocation now = hit.map(Hit::id).orElse(null);
        if (Objects.equals(now, sites.get(player.getUUID()))) {
            return;
        }
        if (now == null) {
            sites.remove(player.getUUID());
            RegionService.syncTo(player);
            return;
        }
        sites.put(player.getUUID(), now);
        RegionService.syncTo(player);
        announce(player, hit.get());
        if (ServerConfig.SITE_GARRISON.get()) {
            garrison(level, hit.get(), player);
        }
    }

    public static void forget(ServerPlayer player) {
        KN8Server server = KN8Server.getOrNull(player.getServer());
        if (server != null) {
            server.playerSites().remove(player.getUUID());
        }
    }

    public static MutableComponent name(ResourceLocation site) {
        return Component.translatable("kn8.site." + site.getPath());
    }

    public static MutableComponent division(SiteDef def) {
        return Component.translatable("kn8.division." + def.division());
    }

    private static void announce(ServerPlayer player, Hit hit) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(division(hit.def())
                .withStyle(ChatFormatting.AQUA)));
        player.connection.send(new ClientboundSetTitleTextPacket(name(hit.id()).withStyle(ChatFormatting.BOLD)));
    }

    /** Guarnicao uma vez por mundo, perto da entrada oeste da peca inicial; cada um fica de posto ali. */
    private static void garrison(ServerLevel level, Hit hit, @Nullable ServerPlayer witness) {
        SiteData data = SiteData.get(level);
        String key = hit.id().toString();
        if (data.garrisoned(key) || hit.def().garrison().isEmpty()) {
            return;
        }
        data.markGarrisoned(key);
        BlockPos gate = hit.garrisonPos();
        int spawned = 0;
        for (InvasionDef.Defender member : hit.def().garrison()) {
            Optional<EntityType<? extends SoldierEntity>> special = KN8Entities.specialSoldier(member.variant());
            for (int i = 0; i < member.count(); i++) {
                SoldierEntity soldier = special.<SoldierEntity>map(type -> type.create(level))
                        .orElseGet(() -> KN8Entities.SOLDIER.get().create(level));
                if (soldier == null) {
                    continue;
                }
                BlockPos pos = gate.offset(2 + (spawned % 3) * 2, 0, (spawned / 3) * 2 - 2);
                soldier.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 270.0F, 0.0F);
                if (special.isEmpty()) {
                    soldier.setVariant(member.variant());
                }
                soldier.setPowerLevel(member.level());
                soldier.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
                soldier.setPersistenceRequired();
                soldier.setSquadPost(pos);
                level.addFreshEntity(soldier);
                spawned++;
            }
        }
        KN8Constants.LOGGER.info("[kn8] Guarnicao de {} em {} ({} soldados)", hit.id(), gate.toShortString(),
                spawned);
        if (witness != null) {
            witness.sendSystemMessage(Component.translatable("kn8.site.garrison", name(hit.id()),
                    division(hit.def())).withStyle(ChatFormatting.AQUA));
        }
    }

    /** Invasao do local sorteada pelo peso (vazio: o local nao tem invasao valida). */
    public static Optional<ResourceLocation> rollInvasion(Hit hit, RandomSource random) {
        int total = 0;
        for (SiteDef.SiteInvasion entry : hit.def().invasions()) {
            if (KN8Data.INVASION.server().containsKey(entry.invasion())) {
                total += entry.weight();
            }
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = random.nextInt(total);
        for (SiteDef.SiteInvasion entry : hit.def().invasions()) {
            if (!KN8Data.INVASION.server().containsKey(entry.invasion())) {
                continue;
            }
            roll -= entry.weight();
            if (roll < 0) {
                return Optional.of(entry.invasion());
            }
        }
        return Optional.empty();
    }
}
