// src/main/java/com/kn8/common/registry/KN8Entities.java
package com.kn8.common.registry;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.training.TrainingDummyEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tipos de entidade. Um EntityType por especie de kaiju (decisao da Fase 4); o nome do tipo e o id do JSON em
 * {@code data/<ns>/kn8/kaiju/}. O tamanho aqui so vale ate os dados chegarem; a hitbox real vem do JSON.
 */
public final class KN8Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, KN8Constants.MOD_ID);

    private static final int KAIJU_TRACKING_RANGE_CHUNKS = 10;

    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> TRICHONEPHILA =
            kaiju("trichonephila", 3.4F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> PRIMIGENIUS =
            kaiju("primigenius", 3.13F, 6.0F);
    /** 0.1-B: Yoju ressurgido (arte verde). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> PRIMIGENIUS_RESURRECTED =
            kaiju("primigenius_resurrected", 3.13F, 6.0F);
    /** 0.1-B: Honju base (Tita Bruto marrom). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> PRIMIGENIUS_HONJU =
            kaiju("primigenius_honju", 5.73F, 9.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> PRIMIGENIUS_REVIVED =
            kaiju("primigenius_revived", 5.73F, 9.0F);

    /** 0.2 (Etapa 8): Kaiju No. 9, o primeiro numerado (humanoide de 2 m; revive carcacas). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> KAIJU_NO9 = ENTITY_TYPES.register(
            "kaiju_no9", () -> EntityType.Builder.<KaijuEntity>of(KaijuNo9Entity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.0F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("kaiju_no9"));

    /** 0.1-B (M11b): carcaca de kaiju morto (tamanho real vem da especie, sincronizada). */
    public static final DeferredHolder<EntityType<?>, EntityType<CarcassEntity>> CARCASS = ENTITY_TYPES.register(
            "carcass", () -> EntityType.Builder.<CarcassEntity>of(CarcassEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("carcass"));

    /** 0.1-B (Etapa F): soldado da Forca de Defesa (variante e nivel por dados). */
    public static final DeferredHolder<EntityType<?>, EntityType<SoldierEntity>> SOLDIER = ENTITY_TYPES.register(
            "soldier", () -> EntityType.Builder.<SoldierEntity>of(SoldierEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.9F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("soldier"));

    /** 0.6: projetil de habilidade de kaiju (raio de energia, teia, Finger Gun); so particulas no cliente. */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuProjectile>> KAIJU_PROJECTILE =
            ENTITY_TYPES.register("kaiju_projectile", () -> EntityType.Builder.<KaijuProjectile>of(
                            KaijuProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .updateInterval(1)
                    .build("kaiju_projectile"));

    /** 0.2 (Etapa 2): boneco de treino (XP de Release por golpe). */
    public static final DeferredHolder<EntityType<?>, EntityType<TrainingDummyEntity>> TRAINING_DUMMY =
            ENTITY_TYPES.register("training_dummy", () -> EntityType.Builder.<TrainingDummyEntity>of(
                            TrainingDummyEntity::new, MobCategory.MISC)
                    .sized(0.5F, 1.975F)
                    .eyeHeight(1.7775F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("training_dummy"));

    /** Todas as especies, para atributos, renderers e comandos. */
    public static final List<DeferredHolder<EntityType<?>, EntityType<KaijuEntity>>> KAIJU =
            List.of(TRICHONEPHILA, PRIMIGENIUS, PRIMIGENIUS_RESURRECTED, PRIMIGENIUS_HONJU, PRIMIGENIUS_REVIVED,
                    KAIJU_NO9);

    private KN8Entities() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> kaiju(String name, float width,
            float height) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<KaijuEntity>of(KaijuEntity::new,
                        MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                .build(name));
    }

    /** Mod bus: atributos base; os valores reais saem do JSON no primeiro tick. */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SOLDIER.get(), SoldierEntity.createAttributes().build());
        event.put(TRAINING_DUMMY.get(), LivingEntity.createLivingAttributes().build());
        KAIJU.forEach(type -> event.put(type.get(), KaijuEntity.baseAttributes().build()));
    }
}
