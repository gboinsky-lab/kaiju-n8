// src/main/java/com/kn8/common/numbered/KaijuNo10Entity.java
package com.kn8.common.numbered;

import java.util.HashMap;
import java.util.Map;

import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.world.damagesource.DamageSource;
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
    /** NUMERADOS-C: marcas da adaptacao por tipo de golpe (No. 12), como as do No. 9. */
    final Map<String, long[]> adaptation = new HashMap<>();

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

    /** NUMERADOS-C: adaptacao do numbered/<id>.json depois da armadura (como no No. 9; sem o bloco, nada muda). */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        float afterArmor = super.getDamageAfterArmorAbsorb(source, amount);
        return level().isClientSide() ? afterArmor : No9Service.adapt(this, adaptation, source, afterArmor);
    }
}
