// src/main/java/com/kn8/common/kaiju/KaijuPart.java
package com.kn8.common.kaiju;

import com.kn8.common.data.def.KaijuDef;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Uma hitbox de parte de kaiju (M7b), definida pelo JSON da especie ({@code parts}). Nao e salva nem tem pacote
 * proprio: o kaiju cria as partes dos dois lados, posiciona a cada tick e recebe todo o dano delas (padrao do PT4).
 *
 * <p>Medidas, posicao e multiplicador vem da definicao atual e mudam com {@code /reload}
 * ({@link #setDefinition}); o NOME e a ordem sao fixos durante a vida da entidade.</p>
 */
public final class KaijuPart extends PartEntity<KaijuEntity> {

    private final String partName;
    private KaijuDef.Part definition;
    private EntityDimensions size;

    KaijuPart(KaijuEntity parent, KaijuDef.Part definition) {
        super(parent);
        this.partName = definition.name();
        setDefinition(definition);
    }

    public String partName() {
        return partName;
    }

    public KaijuDef.Part definition() {
        return definition;
    }

    public float multiplier() {
        return definition.multiplier();
    }

    public boolean isCore() {
        return definition.core();
    }

    /** Troca medidas/posicao/multiplicador (apos {@code /reload}) e atualiza a caixa. */
    void setDefinition(KaijuDef.Part newDefinition) {
        this.definition = newDefinition;
        this.size = EntityDimensions.scalable(newDefinition.width(), newDefinition.height());
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurtFromPart(this, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
