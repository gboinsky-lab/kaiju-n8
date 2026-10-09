// src/main/java/com/kn8/common/registry/KN8Entities.java
package com.kn8.common.registry;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuProjectile;
import com.kn8.common.kaiju.PreondactylEntity;
import com.kn8.common.numbered.KaijuNo10Entity;
import com.kn8.common.numbered.KaijuNo9Entity;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.soldier.special.HoshinaNo10Entity;
import com.kn8.common.soldier.special.KikoruEntity;
import com.kn8.common.soldier.special.KafkaEntity;
import com.kn8.common.soldier.special.KaijuNo8Entity;
import com.kn8.common.soldier.special.KikoruNo4Entity;
import com.kn8.common.soldier.special.MinaEntity;
import com.kn8.common.soldier.special.NarumiEntity;
import com.kn8.common.soldier.special.NarumiNo1Entity;
import com.kn8.common.soldier.special.RenoEntity;
import com.kn8.common.soldier.special.RenoNo6Entity;
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
    /** 0.6-B: Trichonephila Honju (Tecedeira Abissal, modelo do Miguel, 8 m): chefe que invoca Trichonephila. */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> TRICHONEPHILA_HONJU =
            kaiju("trichonephila_honju", 6.0F, 4.0F);
    /** 0.7-A: Philinosoma (Honju lagarto do Miguel, 9 m, cauda longa) e Diclonius (Honju em pe, 9 m). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> PHILINOSOMA =
            kaiju("philinosoma", 6.0F, 9.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> DICLONIUS =
            kaiju("diclonius", 5.0F, 9.0F);
    /** 0.7-A: Camponotus (formiga Yoju do Miguel, 6 m de comprimento) e a forma revivida (azul, mesmo corpo). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> CAMPONOTUS =
            kaiju("camponotus", 4.0F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> CAMPONOTUS_REBORN =
            kaiju("camponotus_reborn", 4.0F, 2.9F);
    /** 0.7-B: kaiju cogumelo do Miguel: Phaneroplasmodium (Yoju, 5 m, 8 patas) e Myxogasterocarp (Honju, 9 m). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> PHANEROPLASMODIUM =
            kaiju("phaneroplasmodium", 4.4F, 5.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> MYXOGASTEROCARP =
            kaiju("myxogasterocarp", 7.0F, 9.0F);

    /** 0.2 (Etapa 8): Kaiju No. 9, o primeiro numerado (humanoide de 2 m; revive carcacas). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> KAIJU_NO9 = ENTITY_TYPES.register(
            "kaiju_no9", () -> EntityType.Builder.<KaijuEntity>of(KaijuNo9Entity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.0F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("kaiju_no9"));
    /** 0.6-E: Kaiju No. 10, forma pequena (4 m) e gigante (24 m); comportamento extra no No10Service. */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> KAIJU_NO10_SMALL = numbered(
            "kaiju_no10_small", 2.0F, 4.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> KAIJU_NO10_GIANT = numbered(
            "kaiju_no10_giant", 10.0F, 24.0F);
    /** 0.6-E: Preondactyl, kaiju voador (flyer/preondactyl.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> PREONDACTYL = flyer(
            "preondactyl", 4.0F, 4.0F);
    /** 0.7-B: larva que infecta o Kafka (0,5 m, asas): voa como o Preondactyl (flyer/kaiju_larva.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> KAIJU_LARVA = flyer(
            "kaiju_larva", 0.5F, 0.5F);

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

    /** 0.6-D: Hoshina, primeiro soldado especial (perfil em special_soldier/hoshina.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<HoshinaEntity>> HOSHINA = ENTITY_TYPES.register(
            "hoshina", () -> EntityType.Builder.<HoshinaEntity>of(HoshinaEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.85F)
                    .eyeHeight(1.6F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("hoshina"));
    /** 0.6-F: Hoshina com o traje numerado 10 (cauda que luta sozinha; special_soldier/hoshina_no10.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<HoshinaNo10Entity>> HOSHINA_NO10 =
            ENTITY_TYPES.register("hoshina_no10", () -> EntityType.Builder.<HoshinaNo10Entity>of(
                            HoshinaNo10Entity::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.85F)
                    .eyeHeight(1.45F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("hoshina_no10"));
    /** 0.5.0-D8: Kikoru Shinomiya, traje normal (machado; special_soldier/kikoru.json). 1,57 m (Miguel). */
    public static final DeferredHolder<EntityType<?>, EntityType<KikoruEntity>> KIKORU = ENTITY_TYPES.register(
            "kikoru", () -> EntityType.Builder.<KikoruEntity>of(KikoruEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.57F)
                    .eyeHeight(1.38F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("kikoru"));
    /** 0.7-C: Reno Ichikawa, traje normal (rifle; special_soldier/reno.json). 1,70 m [SUPOSICAO]. */
    public static final DeferredHolder<EntityType<?>, EntityType<RenoEntity>> RENO = ENTITY_TYPES.register(
            "reno", () -> EntityType.Builder.<RenoEntity>of(RenoEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.7F)
                    .eyeHeight(1.5F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("reno"));
    /** 0.7-C: Mina Ashiro com o canhao pesado (special_soldier/mina.json). 1,65 m [SUPOSICAO]. */
    public static final DeferredHolder<EntityType<?>, EntityType<MinaEntity>> MINA = ENTITY_TYPES.register(
            "mina", () -> EntityType.Builder.<MinaEntity>of(MinaEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.65F)
                    .eyeHeight(1.45F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("mina"));
    /** 0.7-C: Gen Narumi com a baioneta (special_soldier/narumi.json). 1,78 m [SUPOSICAO]. */
    public static final DeferredHolder<EntityType<?>, EntityType<NarumiEntity>> NARUMI = ENTITY_TYPES.register(
            "narumi", () -> EntityType.Builder.<NarumiEntity>of(NarumiEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.78F)
                    .eyeHeight(1.57F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("narumi"));
    /** 0.7-D: Kikoru com a arma numerada 4 (asas, voo; special_soldier/kikoru_no4.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<KikoruNo4Entity>> KIKORU_NO4 = ENTITY_TYPES.register(
            "kikoru_no4", () -> EntityType.Builder.<KikoruNo4Entity>of(KikoruNo4Entity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.57F)
                    .eyeHeight(1.38F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("kikoru_no4"));
    /** 0.7-D: Reno com a arma numerada 6 (criocinese; special_soldier/reno_no6.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<RenoNo6Entity>> RENO_NO6 = ENTITY_TYPES.register(
            "reno_no6", () -> EntityType.Builder.<RenoNo6Entity>of(RenoNo6Entity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.7F)
                    .eyeHeight(1.5F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("reno_no6"));
    /** 0.7-D: Gen Narumi com a arma numerada 1 (modelo do Narumi; special_soldier/narumi_no1.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<NarumiNo1Entity>> NARUMI_NO1 = ENTITY_TYPES.register(
            "narumi_no1", () -> EntityType.Builder.<NarumiNo1Entity>of(NarumiNo1Entity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.78F)
                    .eyeHeight(1.57F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("narumi_no1"));
    /** 0.7-F: Kafka Hibino, forma humana (vira o Kaiju No. 8 perdendo; special_soldier/kafka.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<KafkaEntity>> KAFKA = ENTITY_TYPES.register(
            "kafka", () -> EntityType.Builder.<KafkaEntity>of(KafkaEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.81F)
                    .eyeHeight(1.6F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("kafka"));
    /** 0.7-F: Kaiju No. 8 (Kafka transformado, aliado; special_soldier/kaiju_no8.json). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuNo8Entity>> KAIJU_NO8 = ENTITY_TYPES.register(
            "kaiju_no8", () -> EntityType.Builder.<KaijuNo8Entity>of(KaijuNo8Entity::new, MobCategory.CREATURE)
                    .sized(0.8F, 2.0F)
                    .eyeHeight(1.75F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .build("kaiju_no8"));

    /** 0.6: projetil de habilidade de kaiju (raio de energia, teia, Finger Gun); so particulas no cliente. */
    public static final DeferredHolder<EntityType<?>, EntityType<KaijuProjectile>> KAIJU_PROJECTILE =
            ENTITY_TYPES.register("kaiju_projectile", () -> EntityType.Builder.<KaijuProjectile>of(
                            KaijuProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .updateInterval(1)
                    .build("kaiju_projectile"));
    /** 0.6-D: corte a distancia do Hoshina e da espada dele (SlashProjectile). */
    public static final DeferredHolder<EntityType<?>, EntityType<SlashProjectile>> SLASH_PROJECTILE =
            ENTITY_TYPES.register("slash_projectile", () -> EntityType.Builder.<SlashProjectile>of(
                            SlashProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                    .updateInterval(1)
                    .build("slash_projectile"));

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
                    TRICHONEPHILA_HONJU, KAIJU_NO9, KAIJU_NO10_SMALL, KAIJU_NO10_GIANT,
                    PREONDACTYL, PHILINOSOMA, DICLONIUS, CAMPONOTUS, CAMPONOTUS_REBORN, PHANEROPLASMODIUM,
                    MYXOGASTEROCARP, KAIJU_LARVA);

    private KN8Entities() {
    }

    /** 0.6-D: soldado especial pelo nome (defensores de invasao: variante "hoshina"); vazio para os comuns. */
    public static Optional<EntityType<? extends SoldierEntity>> specialSoldier(String name) {
        if (HoshinaNo10Entity.VARIANT_NO10.equals(name)) {
            return Optional.of(HOSHINA_NO10.get());
        }
        if (KikoruEntity.VARIANT_KIKORU.equals(name)) {
            return Optional.of(KIKORU.get());
        }
        if (RenoEntity.VARIANT_RENO.equals(name)) {
            return Optional.of(RENO.get());
        }
        if (MinaEntity.VARIANT_MINA.equals(name)) {
            return Optional.of(MINA.get());
        }
        if (NarumiEntity.VARIANT_NARUMI.equals(name)) {
            return Optional.of(NARUMI.get());
        }
        if (KikoruNo4Entity.VARIANT_NO4.equals(name)) {
            return Optional.of(KIKORU_NO4.get());
        }
        if (RenoNo6Entity.VARIANT_NO6.equals(name)) {
            return Optional.of(RENO_NO6.get());
        }
        if (NarumiNo1Entity.VARIANT_NO1.equals(name)) {
            return Optional.of(NARUMI_NO1.get());
        }
        if (KafkaEntity.VARIANT_KAFKA.equals(name)) {
            return Optional.of(KAFKA.get());
        }
        if (KaijuNo8Entity.VARIANT_NO8.equals(name)) {
            return Optional.of(KAIJU_NO8.get());
        }
        return HoshinaEntity.VARIANT.equals(name) ? Optional.of(HOSHINA.get()) : Optional.empty();
    }

    private static DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> flyer(String name, float width,
            float height) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<KaijuEntity>of(PreondactylEntity::new,
                        MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                .build(name));
    }

    private static DeferredHolder<EntityType<?>, EntityType<KaijuEntity>> numbered(String name, float width,
            float height) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<KaijuEntity>of(KaijuNo10Entity::new,
                        MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(KAIJU_TRACKING_RANGE_CHUNKS)
                .build(name));
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
        event.put(HOSHINA.get(), SoldierEntity.createAttributes().build());
        event.put(HOSHINA_NO10.get(), SoldierEntity.createAttributes().build());
        event.put(KIKORU.get(), SoldierEntity.createAttributes().build());
        event.put(RENO.get(), SoldierEntity.createAttributes().build());
        event.put(MINA.get(), SoldierEntity.createAttributes().build());
        event.put(NARUMI.get(), SoldierEntity.createAttributes().build());
        event.put(KIKORU_NO4.get(), SoldierEntity.createAttributes().build());
        event.put(RENO_NO6.get(), SoldierEntity.createAttributes().build());
        event.put(NARUMI_NO1.get(), SoldierEntity.createAttributes().build());
        event.put(KAFKA.get(), SoldierEntity.createAttributes().build());
        event.put(KAIJU_NO8.get(), SoldierEntity.createAttributes().build());
        event.put(TRAINING_DUMMY.get(), LivingEntity.createLivingAttributes().build());
        KAIJU.forEach(type -> event.put(type.get(), KaijuEntity.baseAttributes().build()));
    }
}
