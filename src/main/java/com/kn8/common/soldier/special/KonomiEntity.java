// src/main/java/com/kn8/common/soldier/special/KonomiEntity.java
package com.kn8.common.soldier.special;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.research.OpenResearchS2C;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Konomi Okonogi (ESPECIAIS-2, modelo do Miguel): operadora e pesquisadora, nao luta (Miguel: "a unica que nao entra
 * como combatente e a Konomi, que seria um npc para melhorias, pesquisas e etc"). Usar abre a Pesquisa do Arsenal e
 * ela conta como terminal de pesquisa para quem esta perto ({@code ResearchService}). Invulneravel, parada no posto,
 * fora do esquadrao, como o Oficial de Missoes.
 */
public class KonomiEntity extends HoshinaEntity {

    public static final ResourceLocation PROFILE = KN8Constants.id("konomi");
    public static final String VARIANT = "konomi";
    public static final List<String> ANIMATED = List.of("counter", "dash", "parry");

    public KonomiEntity(EntityType<? extends KonomiEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // Sem combate, passeio nem esquadrao: fica no posto olhando quem chega.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("kn8.konomi.greeting"));
            PacketDistributor.sendToPlayer(serverPlayer, OpenResearchS2C.INSTANCE);
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
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setInvulnerable(true);
    }

    @Override
    protected ResourceLocation profileId() {
        return PROFILE;
    }

    @Override
    protected String variantName() {
        return VARIANT;
    }

    @Override
    protected String animPrefix() {
        return VARIANT;
    }

    @Override
    protected List<String> animatedTechniques() {
        return ANIMATED;
    }

    @Override
    protected String armsFamily() {
        return "calm";
    }

    @Override
    protected String counterAnimation() {
        return "counter";
    }
}
