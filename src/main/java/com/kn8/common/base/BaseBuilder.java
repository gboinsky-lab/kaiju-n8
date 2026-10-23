package com.kn8.common.base;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kn8.common.career.CareerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.craft.WorkbenchService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.BaseModuleDef;
import com.kn8.common.data.def.WorkbenchRecipeDef;
import com.kn8.common.destruction.ProtectedAreas;
import com.kn8.common.training.TrainingDummyEntity;
import com.kn8.common.vfx.VfxService;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Base modular do jogador (PM-8D, Plano Mestre secao 10). O Nucleo da Base funda a base (uma por jogador e
 * dimensao); em volta dele ficam {@link PlayerBases#SLOTS} lotes de {@link #CELL} x {@link #CELL} numa grade 3 x 3
 * (o do meio e o patio do nucleo). Construir um modulo gasta o material do {@code base_module/*.json}, limpa o lote e
 * assenta o template um bloco abaixo do nucleo; demolir derruba o que tem dados (baus soltam o conteudo) e limpa o
 * resto, sem devolver o material. O cliente so pede; tudo e conferido aqui.
 */
public final class BaseBuilder {

    /** Lado do lote e distancia entre lotes (um bloco de passagem entre eles). */
    public static final int CELL = 11;
    public static final int PITCH = 12;
    public static final int HEIGHT = 9;
    /** Lotes na ordem do menu: linha de cima, meio (sem o centro) e linha de baixo; x = leste, z = sul. */
    private static final int[][] OFFSETS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};

    private BaseBuilder() {
    }

    public enum Result {
        OK, NOT_OWNER, TOO_FAR, BAD_SLOT, OCCUPIED, EMPTY, UNKNOWN, RANK_TOO_LOW, MISSING_MODULE, MISSING,
        NO_TEMPLATE, ALREADY_HAS_BASE
    }

    /** Canto minimo (oeste, chao, norte) do lote: o template comeca aqui. */
    public static BlockPos cellOrigin(BlockPos core, int slot) {
        int[] offset = OFFSETS[slot];
        return core.offset(offset[0] * PITCH - CELL / 2, -1, offset[1] * PITCH - CELL / 2);
    }

    /** Caixa da base inteira (grade 3 x 3), para a area protegida. */
    public static AABB area(BlockPos core) {
        int half = PITCH + CELL / 2;
        return new AABB(core.getX() - half, core.getY() - 2, core.getZ() - half, core.getX() + half + 1,
                core.getY() + HEIGHT + 2, core.getZ() + half + 1);
    }

    public static String areaName(UUID owner) {
        return "base_" + owner;
    }

    // --- fundar e perder a base -------------------------------------------------------------------------------

    /** Nucleo colocado: funda a base se o jogador ainda nao tem uma nesta dimensao. */
    public static Result found(ServerPlayer player, BlockPos core) {
        PlayerBases bases = PlayerBases.get(player.serverLevel());
        if (bases.of(player.getUUID()).isPresent()) {
            player.displayClientMessage(Component.translatable("kn8.base.refused.already_has_base",
                    bases.of(player.getUUID()).get().core().toShortString()).withStyle(ChatFormatting.RED), false);
            return Result.ALREADY_HAS_BASE;
        }
        bases.found(player.getUUID(), core);
        if (ServerConfig.BASE_PROTECT.get()) {
            AABB box = area(core);
            ProtectedAreas.get(player.serverLevel()).add(areaName(player.getUUID()),
                    BlockPos.containing(box.minX, box.minY, box.minZ),
                    BlockPos.containing(box.maxX - 1, box.maxY - 1, box.maxZ - 1));
        }
        player.sendSystemMessage(Component.translatable("kn8.base.founded").withStyle(ChatFormatting.GREEN));
        player.level().playSound(null, core, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.0F);
        return Result.OK;
    }

    /** Nucleo quebrado: a base deixa de existir (os modulos ficam como construcoes comuns). */
    public static void lost(ServerLevel level, BlockPos core) {
        PlayerBases.get(level).removeAt(core).ifPresent(owner -> {
            ProtectedAreas.get(level).remove(areaName(owner));
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("kn8.base.lost").withStyle(ChatFormatting.RED));
            }
        });
    }

    /** Usar o nucleo: o dono abre o menu da base; os outros veem de quem e. */
    public static void use(ServerPlayer player, BlockPos core) {
        PlayerBases bases = PlayerBases.get(player.serverLevel());
        Optional<UUID> owner = bases.ownerAt(core);
        if (owner.isEmpty()) {
            found(player, core);
        } else if (!owner.get().equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("kn8.base.not_yours").withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        bases.of(player.getUUID()).filter(base -> base.core().equals(core)).ifPresent(base ->
                PacketDistributor.sendToPlayer(player, new OpenBaseS2C(core, base.slotIds(), true)));
    }

    // --- construir e demolir ------------------------------------------------------------------------------------

    public static Result check(ServerPlayer player, BlockPos core, int slot, ResourceLocation module) {
        Optional<PlayerBases.Base> base = ownBase(player, core);
        if (base.isEmpty()) {
            return Result.NOT_OWNER;
        }
        if (slot < 0 || slot >= PlayerBases.SLOTS) {
            return Result.BAD_SLOT;
        }
        if (!player.getAbilities().instabuild
                && player.distanceToSqr(Vec3.atCenterOf(core)) > sq(ServerConfig.BASE_MANAGE_DISTANCE.get())) {
            return Result.TOO_FAR;
        }
        if (base.get().slot(slot).isPresent()) {
            return Result.OCCUPIED;
        }
        Optional<BaseModuleDef> def = KN8Data.BASE_MODULE.get(module, false);
        if (def.isEmpty()) {
            return Result.UNKNOWN;
        }
        if (def.get().rank().filter(rank -> !CareerService.hasRank(player, rank)).isPresent()) {
            return Result.RANK_TOO_LOW;
        }
        if (!def.get().requires().stream().allMatch(base.get()::has)) {
            return Result.MISSING_MODULE;
        }
        if (!player.getAbilities().instabuild) {
            for (WorkbenchRecipeDef.Ingredient ingredient : def.get().cost()) {
                if (WorkbenchService.count(player.getInventory(), BuiltInRegistries.ITEM.get(ingredient.item()))
                        < ingredient.count()) {
                    return Result.MISSING;
                }
            }
        }
        if (player.serverLevel().getStructureManager().get(def.get().template()).isEmpty()) {
            return Result.NO_TEMPLATE;
        }
        return Result.OK;
    }

    public static Result build(ServerPlayer player, BlockPos core, int slot, ResourceLocation module) {
        Result result = check(player, core, slot, module);
        if (result != Result.OK) {
            refuse(player, result);
            return result;
        }
        ServerLevel level = player.serverLevel();
        BaseModuleDef def = KN8Data.BASE_MODULE.get(module, false).orElseThrow();
        if (!player.getAbilities().instabuild) {
            for (WorkbenchRecipeDef.Ingredient ingredient : def.cost()) {
                WorkbenchService.remove(player.getInventory(), BuiltInRegistries.ITEM.get(ingredient.item()),
                        ingredient.count());
            }
        }
        BlockPos origin = cellOrigin(core, slot);
        clear(level, origin, false);
        StructureTemplate template = level.getStructureManager().get(def.template()).orElseThrow();
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_ALL);
        PlayerBases.get(level).setSlot(player.getUUID(), slot, module);
        Vec3 center = Vec3.atCenterOf(origin.offset(CELL / 2, 1, CELL / 2));
        VfxService.keyed(level, VfxService.SMOKE, center, Vec3.ZERO, 3.0F, "#C8C8C8", 0.0F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
        player.sendSystemMessage(Component.translatable("kn8.base.built", moduleName(module))
                .withStyle(ChatFormatting.GREEN));
        sendState(player, core, false);
        return Result.OK;
    }

    public static Result demolish(ServerPlayer player, BlockPos core, int slot) {
        Optional<PlayerBases.Base> base = ownBase(player, core);
        Result result = base.isEmpty() ? Result.NOT_OWNER : slot < 0 || slot >= PlayerBases.SLOTS ? Result.BAD_SLOT
                : base.get().slot(slot).isEmpty() ? Result.EMPTY
                : !player.getAbilities().instabuild && player.distanceToSqr(Vec3.atCenterOf(core))
                        > sq(ServerConfig.BASE_MANAGE_DISTANCE.get()) ? Result.TOO_FAR : Result.OK;
        if (result != Result.OK) {
            refuse(player, result);
            return result;
        }
        ResourceLocation module = base.get().slot(slot).get();
        clear(player.serverLevel(), cellOrigin(core, slot), true);
        PlayerBases.get(player.serverLevel()).setSlot(player.getUUID(), slot, null);
        player.sendSystemMessage(Component.translatable("kn8.base.demolished", moduleName(module))
                .withStyle(ChatFormatting.YELLOW));
        sendState(player, core, false);
        return Result.OK;
    }

    /**
     * Limpa o lote acima do chao: o que tem dados (baus, bancada com conteudo) e quebrado soltando o que tinha; o
     * resto vira ar. Os NPCs e bonecos do modulo saem junto ({@code npcs}).
     */
    private static void clear(ServerLevel level, BlockPos origin, boolean npcs) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.above(), origin.offset(CELL - 1, HEIGHT - 1, CELL - 1))) {
            if (level.getBlockEntity(pos) != null) {
                level.destroyBlock(pos, true);
            } else if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        if (npcs) {
            AABB box = new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + CELL,
                    origin.getY() + HEIGHT, origin.getZ() + CELL);
            List<Entity> list = level.getEntities((Entity) null, box,
                    entity -> entity instanceof DefenseOfficerEntity || entity instanceof TrainingDummyEntity);
            list.forEach(Entity::discard);
        }
    }

    private static Optional<PlayerBases.Base> ownBase(ServerPlayer player, BlockPos core) {
        return PlayerBases.get(player.serverLevel()).of(player.getUUID()).filter(base -> base.core().equals(core));
    }

    private static void sendState(ServerPlayer player, BlockPos core, boolean open) {
        ownBase(player, core).ifPresent(base ->
                PacketDistributor.sendToPlayer(player, new OpenBaseS2C(core, base.slotIds(), open)));
    }

    private static void refuse(ServerPlayer player, Result result) {
        player.displayClientMessage(Component.translatable("kn8.base.refused." + result.name()
                .toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.RED), true);
    }

    public static Component moduleName(ResourceLocation module) {
        return Component.translatable("kn8.base_module." + module.getPath());
    }

    private static double sq(double value) {
        return value * value;
    }
}
