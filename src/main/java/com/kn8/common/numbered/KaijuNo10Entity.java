// src/main/java/com/kn8/common/numbered/KaijuNo10Entity.java
package com.kn8.common.numbered;

import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Kaiju No. 10 (0.6-E), nas duas formas (pequena de 4 m e gigante de 24 m; uma especie por forma, mesma classe).
 * Luta com as habilidades do JSON da especie; o que ele tem a mais fica no {@link No10Service}: regeneracao,
 * comando dos kaiju por perto (Preondactyl) e, na forma pequena, a mudanca para a gigante depois de um tempo de
 * batalha ou com a vida baixa ({@code transform} do {@code numbered/<id>.json}). Estado so do servidor.
 */
public class KaijuNo10Entity extends KaijuEntity {

    /** Ticks com alvo vivo nesta luta (zera sem alvo por muito tempo). */
    int combatTicks;
    int idleTicks;
    boolean transformed;

    public KaijuNo10Entity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive()) {
            No10Service.tick(this);
        }
    }

    public int combatTicks() {
        return combatTicks;
    }
}
