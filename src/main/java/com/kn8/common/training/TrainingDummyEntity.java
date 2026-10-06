package com.kn8.common.training;

import com.kn8.common.career.CareerService;
import com.kn8.common.registry.KN8Items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Boneco de treino (0.2, Etapa 2): golpes de jogador dao XP de treino de Release, com limite por minuto
 * ({@code career.dummyXpPerHit}/{@code dummyXpPerMinute}). Nunca quebra com golpe; agachado e de mao vazia, o jogador
 * guarda o boneco de volta. Reaproveita o suporte de armadura (modelo, balanco ao levar golpe).
 */
public class TrainingDummyEntity extends ArmorStand {

    private static final byte HIT_WOBBLE_EVENT = 32;

    public TrainingDummyEntity(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
        setShowArms(true);
        // Visual de boneco de palha; o NBT salvo (se houver) substitui depois.
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved()) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // /kill e o vazio removem o boneco.
            kill();
            return true;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            spawnAtLocation(new ItemStack(KN8Items.TRAINING_DUMMY.get()));
            discard();
            return true;
        }
        int xp = CareerService.onDummyHit(player);
        level().broadcastEntityEvent(this, HIT_WOBBLE_EVENT);
        level().playSound(null, blockPosition(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1.0F,
                0.8F + random.nextFloat() * 0.4F);
        player.displayClientMessage(xp > 0
                ? Component.translatable("kn8.training.xp", xp).withStyle(ChatFormatting.AQUA)
                : Component.translatable("kn8.training.limit").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 position, InteractionHand hand) {
        // Sem trocar itens como no suporte de armadura.
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(KN8Items.TRAINING_DUMMY.get());
    }
}
