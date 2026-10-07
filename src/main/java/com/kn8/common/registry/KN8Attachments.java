// src/main/java/com/kn8/common/registry/KN8Attachments.java
package com.kn8.common.registry;

import java.util.Optional;
import java.util.function.Supplier;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerData;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerView;
import com.kn8.common.combat.CombatState;
import com.kn8.common.attribute.PowerView;
import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Tipos de Data Attachment (estado por jogador/entidade/nivel).
 *
 * <p>Regra de sincronizacao (PT1):</p>
 * <ul>
 *   <li><b>Dados publicos</b>: sync nativo do NeoForge ({@code .sync(StreamCodec)}).</li>
 *   <li><b>Dados privados</b>: NUNCA {@code .sync(...)} (o sync inicial do NeoForge 21.1.252 ignora o filtro de
 *   destinatario); vao pelo {@code NetworkSync}, so para o dono.</li>
 * </ul>
 */
public final class KN8Attachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KN8Constants.MOD_ID);

    /**
     * M3: sonda privada de diagnostico do {@code NetworkSync} ({@code /kn8 net probe}). Salva e mantida na morte.
     */
    public static final Supplier<AttachmentType<Integer>> NET_PROBE = ATTACHMENT_TYPES.register("net_probe",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build());

    /**
     * M5: poder do jogador (PRIVADO, so no servidor). Persistido e copiado na morte; os campos volateis voltam ao
     * padrao porque nao fazem parte do Codec.
     */
    public static final Supplier<AttachmentType<PowerData>> POWER = ATTACHMENT_TYPES.register("power",
            () -> AttachmentType.builder(() -> new PowerData())
                    .serialize(PowerData.CODEC)
                    .copyOnDeath()
                    .build());

    /** M5: visao do proprio poder no cliente (recebida por {@code PowerSyncS2C}; nunca salva). */
    public static final Supplier<AttachmentType<PowerView>> POWER_VIEW = ATTACHMENT_TYPES.register("power_view",
            () -> AttachmentType.builder(() -> PowerView.EMPTY).build());

    /** M5: % efetiva PUBLICA (sync nativo): outros jogadores usam para efeitos visuais do traje. Nunca salva. */
    public static final Supplier<AttachmentType<Integer>> RELEASE_VISUAL = ATTACHMENT_TYPES.register("release_visual",
            () -> AttachmentType.builder(() -> 0)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /** 0.2 (Etapas 2 e 5): carreira do jogador (PRIVADO: patente, merito, missoes). Salva e copiada na morte. */
    public static final Supplier<AttachmentType<CareerData>> CAREER = ATTACHMENT_TYPES.register("career",
            () -> AttachmentType.builder(() -> new CareerData())
                    .serialize(CareerData.CODEC)
                    .copyOnDeath()
                    .build());

    /** 0.2: visao da propria carreira no cliente (recebida por {@code CareerSyncS2C}; nunca salva). */
    public static final Supplier<AttachmentType<CareerView>> CAREER_VIEW = ATTACHMENT_TYPES.register("career_view",
            () -> AttachmentType.builder(() -> CareerView.EMPTY).build());

    /** M10: estado de combate do jogador (so no servidor, nunca salvo e nunca sincronizado). */
    public static final Supplier<AttachmentType<CombatState>> COMBAT = ATTACHMENT_TYPES.register("combat",
            () -> AttachmentType.builder(() -> new CombatState()).build());

    /** Aura padrao (jogador sem traje com aura propria): {@code data/kn8/kn8/aura/defense_force.json}. */
    public static final ResourceLocation DEFAULT_AURA = KN8Constants.id("defense_force");

    /**
     * 0.5: id PUBLICO da aura de poder (sync nativo), em qualquer entidade viva que libera potencia (jogador, soldado
     * especial). O cliente desenha com o {@code aura/<id>.json} e a % de {@link #RELEASE_VISUAL}. Nunca salva: o
     * servidor recalcula (traje, perfil do soldado).
     */
    public static final Supplier<AttachmentType<ResourceLocation>> AURA = ATTACHMENT_TYPES.register("aura",
            () -> AttachmentType.builder(() -> DEFAULT_AURA)
                    .sync(ResourceLocation.STREAM_CODEC)
                    .build());

    /** 0.5: aura forcada por comando ({@code /kn8 aura}), por cima da do traje. So no servidor, nunca salva. */
    public static final Supplier<AttachmentType<Optional<ResourceLocation>>> AURA_OVERRIDE =
            ATTACHMENT_TYPES.register("aura_override", () -> AttachmentType.<Optional<ResourceLocation>>builder(
                    () -> Optional.empty()).build());

    private KN8Attachments() {
    }
}
