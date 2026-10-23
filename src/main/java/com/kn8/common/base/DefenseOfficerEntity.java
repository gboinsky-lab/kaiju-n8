package com.kn8.common.base;

import javax.annotation.Nullable;

import com.kn8.KN8Constants;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Oficial de missoes (PM-8, Plano Mestre secao 10): NPC parado nas bases que entrega as missoes do {@code giver} dele
 * ({@code kn8:officer} ou {@code kn8:exam_officer}, campo que ja existia nos {@code mission/*.json}). Usar abre o
 * quadro de missoes do proprio menu filtrado pelas missoes dele, com o briefing da proxima patente no chat; o menu
 * continua aceitando tudo de qualquer lugar (o oficial nao tranca nada).
 *
 * <p>Modelo do soldado [SUPOSICAO provisorio]; invulneravel (kaiju nao o miram: o vanilla nao conta invulneravel
 * como inimigo), nao entra em esquadrao e nao some longe dos jogadores.</p>
 */
public class DefenseOfficerEntity extends SoldierEntity {

    public static final ResourceLocation OFFICER = KN8Constants.id("officer");
    public static final ResourceLocation EXAM_OFFICER = KN8Constants.id("exam_officer");
    private static final String TAG_GIVER = "kn8_giver";
    private static final String VARIANT = "pistol";

    private ResourceLocation giver = OFFICER;

    public DefenseOfficerEntity(EntityType<? extends DefenseOfficerEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public ResourceLocation giver() {
        return giver;
    }

    public void setGiver(ResourceLocation value) {
        giver = value;
        setCustomName(Component.translatable("entity.kn8.defense_officer." + value.getPath()));
    }

    @Override
    protected void registerGoals() {
        // Sem combate, passeio nem esquadrao: fica no posto olhando quem chega.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData data) {
        setVariant(VARIANT);
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setGiver(giver);
        return result;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && level() instanceof ServerLevel) {
            BaseServices.talk(serverPlayer, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean canJoinSquad() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_GIVER, giver.toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ResourceLocation saved = ResourceLocation.tryParse(tag.getString(TAG_GIVER));
        setGiver(saved != null && tag.contains(TAG_GIVER) ? saved : OFFICER);
        setInvulnerable(true);
    }
}
