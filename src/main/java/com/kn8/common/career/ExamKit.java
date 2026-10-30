package com.kn8.common.career;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.combat.MagazineItem;
import com.kn8.common.combat.WeaponHandling;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.site.SiteService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Kit do exame (EXAME): itens emprestados pela Forca de Defesa no inicio da prova ({@link MissionDef.Arena#kit}).
 * Cada item leva a marca {@value #TAG} com o id da missao nos dados do item; no fim da prova (e na morte) os itens
 * marcados saem do inventario. Tambem acha o centro do local da prova ({@link MissionDef.Arena#site}).
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class ExamKit {

    public static final String TAG = "kn8_exam_kit";

    private ExamKit() {
    }

    /** Missao a que o item pertence (vazio = nao e do kit). */
    public static Optional<String> missionOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag tag = data.copyTag();
        return tag.contains(TAG) ? Optional.of(tag.getString(TAG)) : Optional.empty();
    }

    /** Entrega o kit: armadura vestida no slot livre, armas de fogo e pentes cheios, o resto no inventario. */
    public static int give(ServerPlayer player, ResourceLocation mission, List<MissionDef.KitItem> kit) {
        int given = 0;
        for (MissionDef.KitItem entry : kit) {
            Item item = BuiltInRegistries.ITEM.get(entry.item());
            if (item == Items.AIR) {
                continue;
            }
            for (int i = 0; i < entry.count(); i += item.getDefaultMaxStackSize()) {
                ItemStack stack = new ItemStack(item, Math.min(entry.count() - i, item.getDefaultMaxStackSize()));
                if (item instanceof MagazineItem) {
                    MagazineItem.setRounds(stack, WeaponHandling.magazineCapacity(item, false));
                }
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG, mission.toString()));
                given += stack.getCount();
                if (item instanceof ArmorItem armor && player.getItemBySlot(armor.getEquipmentSlot()).isEmpty()) {
                    player.setItemSlot(armor.getEquipmentSlot(), stack);
                } else if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        return given;
    }

    /** Recolhe os itens do kit desta missao (inventario, armadura e segunda mao). */
    public static int take(ServerPlayer player, ResourceLocation mission) {
        int taken = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (missionOf(stack).filter(mission.toString()::equals).isPresent()) {
                taken += stack.getCount();
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        return taken;
    }

    /** Morte durante a prova: os itens do kit nao caem no chao (voltam para a Forca de Defesa). */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer) {
            event.getDrops().removeIf(drop -> missionOf(drop.getItem()).isPresent());
        }
    }

    /** Centro do local da prova (caixa da estrutura inteira), na superficie; vazio se o jogador esta fora dele. */
    public static Optional<BlockPos> siteCenter(ServerPlayer player, ResourceLocation site) {
        ServerLevel level = player.serverLevel();
        return SiteService.siteAt(level, player.blockPosition()).filter(hit -> hit.id().equals(site)).map(hit -> {
            BoundingBox box = hit.start().getBoundingBox();
            int x = (box.minX() + box.maxX()) / 2;
            int z = (box.minZ() + box.maxZ()) / 2;
            return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        });
    }
}
