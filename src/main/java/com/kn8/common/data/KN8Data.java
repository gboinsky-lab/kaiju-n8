// src/main/java/com/kn8/common/data/KN8Data.java
package com.kn8.common.data;

import java.util.List;
import java.util.Optional;

import com.kn8.common.data.def.AbilityDef;
import com.kn8.common.data.def.BossDef;
import com.kn8.common.data.def.DismantleDef;
import com.kn8.common.data.def.KaijuDef;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.data.def.RankDef;
import com.kn8.common.data.def.SoldierDef;
import com.kn8.common.data.def.SuitDef;
import com.kn8.common.data.def.WeaponDef;

import net.minecraft.resources.ResourceLocation;

/**
 * Todos os tipos de dado do mod (Fase 4, secao 4.4; formato decidido no PT2). As definicoes sao constantes como
 * DeferredRegisters; o conteudo de cada uma vive nas fotos do {@link DataRegistry}.
 *
 * <p>{@code true} = sincronizado com o cliente (precisa para render, HUD, tooltips ou partes calculadas dos dois
 * lados); missoes, chefes e desmonte ficam so no servidor.</p>
 */
public final class KN8Data {

    public static final DataRegistry<RankDef> RANK = new DataRegistry<>("rank", RankDef.CODEC, true);
    public static final DataRegistry<AbilityDef> ABILITY = new DataRegistry<>("ability", AbilityDef.CODEC, true);
    public static final DataRegistry<DismantleDef> DISMANTLE =
            new DataRegistry<>("dismantle", DismantleDef.CODEC, false);
    public static final DataRegistry<KaijuDef> KAIJU = new DataRegistry<>("kaiju", KaijuDef.CODEC, true);
    public static final DataRegistry<BossDef> BOSS = new DataRegistry<>("boss", BossDef.CODEC, false);
    public static final DataRegistry<WeaponDef> WEAPON = new DataRegistry<>("weapon", WeaponDef.CODEC, true);
    public static final DataRegistry<SuitDef> SUIT = new DataRegistry<>("suit", SuitDef.CODEC, true);
    public static final DataRegistry<MissionDef> MISSION = new DataRegistry<>("mission", MissionDef.CODEC, false);
    /** 0.1-B (Etapa F): soldados da Forca de Defesa (so o servidor precisa). */
    public static final DataRegistry<SoldierDef> SOLDIER = new DataRegistry<>("soldier", SoldierDef.CODEC, false);

    /** Ordem = ordem de validacao (cada um so referencia os anteriores). */
    public static final List<DataRegistry<?>> ALL =
            List.of(RANK, ABILITY, DISMANTLE, KAIJU, BOSS, WEAPON, SUIT, MISSION, SOLDIER);

    private KN8Data() {
    }

    public static Optional<DataRegistry<?>> byId(ResourceLocation id) {
        return ALL.stream().filter(registry -> registry.id().equals(id)).findFirst();
    }

    /** Busca pelo nome curto usado em comandos ({@code kaiju}, {@code rank}...). */
    public static Optional<DataRegistry<?>> byName(String name) {
        return ALL.stream().filter(registry -> registry.id().getPath().equals(name)).findFirst();
    }

    static void clearServer() {
        ALL.forEach(DataRegistry::clearServer);
    }

    /** Chamado pelo cliente ao desconectar. */
    public static void clearClient() {
        ALL.forEach(DataRegistry::clearClient);
    }
}
