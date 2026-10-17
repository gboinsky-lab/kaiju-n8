// src/main/java/com/kn8/common/data/def/SpecialSoldierDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Soldado especial ({@code data/<ns>/kn8/special_soldier/<id>.json}, 0.6-D; primeiro: Hoshina). Um perfil por
 * personagem: atributos, Release, aura, armas e tecnicas. Numeros do texto do Miguel
 * (docs/ESPECIFICACAO_HABILIDADES_MOBS.md secao 2); os que o texto nao fixa sao [SUPOSICAO] e estao em
 * docs/BALANCEAMENTO.md.
 *
 * <ul>
 *   <li>{@code max_release} (Miguel, 0.6-D): teto da % com escalada e desespero somados (Hoshina: 92 no traje
 *   comum; 100% so com o traje numerado 10, na 0.6-F).</li>
 *   <li>{@code release}: % de Release base (formulas do jogador). Com a vida baixa sobe sozinha como no jogador
 *   ({@code [power] desperationHealth/desperationMaxPoints}), e a aura acompanha.</li>
 *   <li>{@code kaiju_damage}: multiplicador do dano contra kaiju (como o {@code kaiju_damage} dos soldados comuns).</li>
 *   <li>{@code techniques}: id -> {@link Technique}. A IA escolhe a de maior {@code priority} entre as prontas e com o
 *   alvo entre {@code min_range} e {@code max_range} (borda da hitbox); empate sorteado.</li>
 *   <li>{@code dash}, {@code counter} (Kaeshi-uchi) e {@code parry}: reacoes aos golpes do kaiju.</li>
 *   <li>{@code escalation} (Miguel): o poder de combate escala durante a luta (Release sobe, ele fica mais rapido e
 *   mais forte, as recargas encurtam).</li>
 * </ul>
 */
public record SpecialSoldierDef(float health, float armor, float speed, float followRange, float knockbackResistance,
        int release, int maxRelease, float kaijuDamage, ResourceLocation aura, ResourceLocation weapon,
        Optional<ResourceLocation> offhand, Map<String, Technique> techniques, Dash dash, Counter counter,
        Parry parry, Growth growth) {

    /** Escalada de combate (atalho: o campo fica no mesmo nivel do JSON, dentro de {@link Growth}). */
    public Escalation escalation() {
        return growth.escalation();
    }

    public Optional<Heavy> heavy() {
        return growth.heavy();
    }

    public Optional<HeavyGuard> heavyGuard() {
        return growth.heavyGuard();
    }

    public Tactics tactics() {
        return growth.tactics();
    }

    /** MOV-5C: ritmo das tecnicas (alternar rapida e pesada, pressionar quem regenera). */
    public Rhythm rhythm() {
        return growth.rhythm();
    }

    /** IA-2: tecnicas com usos limitados por vida ({@code technique_uses}; Anti-Giant da Mina = 4 tiros). */
    public Map<String, Integer> techniqueUses() {
        return growth.techniqueUses();
    }

    public Optional<Numbers10> numbers10() {
        return growth.numbers10();
    }

    public Optional<Flight> flight() {
        return growth.flight();
    }

    public Optional<Transform> transform() {
        return growth.transform();
    }

    public Optional<Regeneration> regeneration() {
        return growth.regeneration();
    }

    /** Comportamento de atirador que mantem distancia (Mina, Reno); vazio = vai ate o alcance da arma. */
    public Optional<Ranged> ranged() {
        return growth.ranged();
    }

    /** Altura do personagem em blocos (tabela mestra de alturas; 0 = a do modelo). */
    public float height() {
        return growth.height();
    }

    /**
     * Campos que crescem com a luta, lidos no mesmo nivel do JSON (o RecordCodecBuilder aceita no maximo 16 campos
     * por grupo): {@code escalation} e, so com o traje numerado 10 (0.6-F), {@code numbers10}.
     */
    public record Growth(Escalation escalation, Optional<Numbers10> numbers10, Optional<Flight> flight,
            Optional<Transform> transform, Optional<Regeneration> regeneration, float height,
            Optional<Ranged> ranged, Optional<Heavy> heavy, Map<String, Integer> techniqueUses,
            Optional<HeavyGuard> heavyGuard, Tactics tactics, Rhythm rhythm) {
        public static final MapCodec<Growth> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Escalation.CODEC.optionalFieldOf("escalation", Escalation.NONE).forGetter(Growth::escalation),
                Numbers10.CODEC.optionalFieldOf("numbers10").forGetter(Growth::numbers10),
                Flight.CODEC.optionalFieldOf("flight").forGetter(Growth::flight),
                Transform.CODEC.optionalFieldOf("transform").forGetter(Growth::transform),
                Regeneration.CODEC.optionalFieldOf("regeneration").forGetter(Growth::regeneration),
                Codec.floatRange(0.0F, 4.0F).optionalFieldOf("height", 0.0F).forGetter(Growth::height),
                Ranged.CODEC.optionalFieldOf("ranged").forGetter(Growth::ranged),
                Heavy.CODEC.optionalFieldOf("heavy").forGetter(Growth::heavy),
                Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 1000)).optionalFieldOf("technique_uses", Map.of())
                        .forGetter(Growth::techniqueUses),
                HeavyGuard.CODEC.optionalFieldOf("heavy_guard").forGetter(Growth::heavyGuard),
                Tactics.CODEC.optionalFieldOf("tactics", Tactics.DEFAULT).forGetter(Growth::tactics),
                Rhythm.CODEC.optionalFieldOf("rhythm", Rhythm.NONE).forGetter(Growth::rhythm)
        ).apply(i, Growth::new));
    }

    /**
     * MOV-5C (plano de mobilidade v3 §7, Kikoru: "alternar golpes rapidos e comprometidos"): {@code alternate_bonus}
     * soma prioridade as tecnicas do tipo oposto ao da anterior (rapida depois de pesada e pesada depois de rapida,
     * pela lista do bloco {@code heavy}); {@code press_regeneration} soma prioridade a tecnica que acerta mais cedo
     * quando o alvo esta regenerando (No. 9, No. 10: a cura para logo depois de cada golpe) e o goal nao reposiciona.
     */
    public record Rhythm(int alternateBonus, int pressRegeneration) {
        public static final Rhythm NONE = new Rhythm(0, 0);
        public static final Codec<Rhythm> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 100).optionalFieldOf("alternate_bonus", 0).forGetter(Rhythm::alternateBonus),
                Codec.intRange(0, 100).optionalFieldOf("press_regeneration", 0).forGetter(Rhythm::pressRegeneration)
        ).apply(i, Rhythm::new));
    }

    /**
     * IA-3 (Miguel, 2026-10-16: "alguns personagens conseguem defender mas ainda tomam dano"): golpe pesado de kaiju
     * vindo de frente ({@code front_arc_degrees}) cai para {@code factor} do dano, uma vez a cada
     * {@code cooldown_ticks}. So os mais fortes (Narumi, Kaiju No. 8, Hoshina, Kikoru) tem.
     */
    public record HeavyGuard(float factor, float frontArcDegrees, int cooldownTicks) {
        public static final Codec<HeavyGuard> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("factor").forGetter(HeavyGuard::factor),
                Codec.floatRange(1.0F, 360.0F).optionalFieldOf("front_arc_degrees", 120.0F)
                        .forGetter(HeavyGuard::frontArcDegrees),
                DefCodecs.TICKS.optionalFieldOf("cooldown_ticks", 40).forGetter(HeavyGuard::cooldownTicks)
        ).apply(i, HeavyGuard::new));
    }

    /**
     * IA-3: perfil tatico do personagem (plano de IA, Parte 4.3 e 9; ficha em docs/FICHAS_IA_PERSONAGENS.md). Nao
     * muda atributos de combate, so decisoes:
     * <ul>
     *   <li>{@code aggression} (0-1): depois de uma tecnica, chance de continuar pressionando; senao reposiciona
     *   ({@code lateral_step}: passo para o lado; senao recua ate {@code preferred_distance}) por
     *   {@code reposition_ticks};</li>
     *   <li>{@code damage_tolerance} (fracao da vida; 0 = nunca): abaixo disso recua ate {@code safety_distance} por
     *   {@code retreat_ticks} (uma vez a cada {@code retreat_cooldown_ticks});</li>
     *   <li>{@code heavy_vs_big} / {@code fast_vs_small}: prioridade extra das tecnicas pesadas ({@code heavy}) contra
     *   Honju/numerados/Daikaiju e das outras contra Yoju;</li>
     *   <li>{@code dodge_chance} (0-1): chance de esquivar quando um golpe vem nele;</li>
     *   <li>{@code melee_counter}: usa o contra-ataque corpo a corpo (atiradores nao: bug #16 da auditoria);</li>
     *   <li>{@code protect_ally} / {@code big_target}: blocos de bonus na escolha de alvo (kaiju que mira um aliado;
     *   kaiju grande).</li>
     * </ul>
     */
    public record Tactics(float aggression, float preferredDistance, boolean lateralStep, int repositionTicks,
            float damageTolerance, float safetyDistance, int retreatTicks, int retreatCooldownTicks, int heavyVsBig,
            int fastVsSmall, float dodgeChance, boolean meleeCounter, float protectAlly, float bigTarget,
            int exploitOpenings, int missCooldownTicks) {
        public static final Tactics DEFAULT = new Tactics(1.0F, 0.0F, false, 20, 0.0F, 0.0F, 40, 200, 0, 0, 1.0F,
                true, 0.0F, 0.0F, 0, 0);
        public static final Codec<Tactics> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("aggression", 1.0F).forGetter(Tactics::aggression),
                Codec.floatRange(0.0F, 64.0F).optionalFieldOf("preferred_distance", 0.0F)
                        .forGetter(Tactics::preferredDistance),
                Codec.BOOL.optionalFieldOf("lateral_step", false).forGetter(Tactics::lateralStep),
                DefCodecs.TICKS.optionalFieldOf("reposition_ticks", 20).forGetter(Tactics::repositionTicks),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("damage_tolerance", 0.0F)
                        .forGetter(Tactics::damageTolerance),
                Codec.floatRange(0.0F, 64.0F).optionalFieldOf("safety_distance", 0.0F)
                        .forGetter(Tactics::safetyDistance),
                DefCodecs.TICKS.optionalFieldOf("retreat_ticks", 40).forGetter(Tactics::retreatTicks),
                DefCodecs.TICKS.optionalFieldOf("retreat_cooldown_ticks", 200).forGetter(Tactics::retreatCooldownTicks),
                Codec.intRange(0, 100).optionalFieldOf("heavy_vs_big", 0).forGetter(Tactics::heavyVsBig),
                Codec.intRange(0, 100).optionalFieldOf("fast_vs_small", 0).forGetter(Tactics::fastVsSmall),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("dodge_chance", 1.0F).forGetter(Tactics::dodgeChance),
                Codec.BOOL.optionalFieldOf("melee_counter", true).forGetter(Tactics::meleeCounter),
                Codec.floatRange(0.0F, 128.0F).optionalFieldOf("protect_ally", 0.0F).forGetter(Tactics::protectAlly),
                Codec.floatRange(0.0F, 128.0F).optionalFieldOf("big_target", 0.0F).forGetter(Tactics::bigTarget),
                // MOV-5B: bonus de prioridade da tecnica que mais tira quando o kaiju abre a guarda (janela depois do
                // golpe pesado, atordoado) e espera extra de um combo que nao acertou nenhum golpe.
                Codec.intRange(0, 100).optionalFieldOf("exploit_openings", 0).forGetter(Tactics::exploitOpenings),
                DefCodecs.TICKS.optionalFieldOf("miss_cooldown_ticks", 0).forGetter(Tactics::missCooldownTicks)
        ).apply(i, Tactics::new));
    }

    /**
     * IA-2 (Miguel): so os especiais pesados destroem construcoes, com os golpes pesados e os tiros poderosos (Kikoru,
     * Mina, Narumi, Kaiju No. 8; Isao quando existir). {@code techniques}: quais tecnicas do perfil sao pesadas;
     * {@code power}: forca de destruicao (1 a 4, tags {@code destruction/*}); {@code radius}: raio da quebra no ponto
     * do golpe (tiro explosivo usa o maior entre este e o da explosao). Golpe pesado tambem nao e "bloqueado" pela
     * pele endurecida do No. 9.
     */
    public record Heavy(List<String> techniques, int power, float radius) {
        public static final Codec<Heavy> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().fieldOf("techniques").forGetter(Heavy::techniques),
                Codec.intRange(1, 4).fieldOf("power").forGetter(Heavy::power),
                Codec.floatRange(0.5F, 16.0F).fieldOf("radius").forGetter(Heavy::radius)
        ).apply(i, Heavy::new));
    }

    /**
     * 0.7-F (Kafka e Kaiju No. 8): troca de forma no lugar. Vira a entidade {@code into} quando a vida cai abaixo de
     * {@code health_below} (fracao; 0 = nunca) ou depois de {@code revert_after_idle_ticks} sem alvo (0 = nunca). A
     * forma nova nasce com a vida cheia dela e herda o alvo.
     */
    public record Transform(ResourceLocation into, float healthBelow, int revertAfterIdleTicks) {
        public static final Codec<Transform> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("into").forGetter(Transform::into),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("health_below", 0.0F).forGetter(Transform::healthBelow),
                Codec.intRange(0, 72000).optionalFieldOf("revert_after_idle_ticks", 0)
                        .forGetter(Transform::revertAfterIdleTicks)
        ).apply(i, Transform::new));
    }

    /**
     * Atirador que mantem distancia (Miguel, 2026-10-15: Mina e Reno). Com o alvo a menos de
     * {@code retreat_distance} blocos (borda a borda) recua para {@code keep_distance}; entre as duas distancias corre
     * em volta dele ({@code orbit}, trocando de lado a cada {@code orbit_switch_ticks} ou quando o caminho fecha) para
     * distrai-lo; se o alvo se afasta, sai da distancia ou da linha de visao, persegue. Atira em movimento.
     */
    public record Ranged(float keepDistance, float retreatDistance, boolean orbit, int orbitSwitchTicks,
            float retreatSpeed, float orbitSpeed, float chaseSpeed) {
        public static final Codec<Ranged> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(2.0F, 64.0F).optionalFieldOf("keep_distance", 16.0F).forGetter(Ranged::keepDistance),
                Codec.floatRange(0.0F, 64.0F).optionalFieldOf("retreat_distance", 9.0F)
                        .forGetter(Ranged::retreatDistance),
                Codec.BOOL.optionalFieldOf("orbit", true).forGetter(Ranged::orbit),
                DefCodecs.TICKS.optionalFieldOf("orbit_switch_ticks", 80).forGetter(Ranged::orbitSwitchTicks),
                Codec.floatRange(0.1F, 3.0F).optionalFieldOf("retreat_speed", 1.3F).forGetter(Ranged::retreatSpeed),
                Codec.floatRange(0.1F, 3.0F).optionalFieldOf("orbit_speed", 1.15F).forGetter(Ranged::orbitSpeed),
                Codec.floatRange(0.1F, 3.0F).optionalFieldOf("chase_speed", 1.3F).forGetter(Ranged::chaseSpeed)
        ).apply(i, Ranged::new));
    }

    /** 0.7-F (Kaiju No. 8): regenera {@code per_second} da vida maxima por segundo abaixo de {@code health_below}. */
    public record Regeneration(float perSecond, float healthBelow) {
        public static final Codec<Regeneration> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("per_second").forGetter(Regeneration::perSecond),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("health_below", 1.0F)
                        .forGetter(Regeneration::healthBelow)
        ).apply(i, Regeneration::new));
    }

    /**
     * 0.7-D (Kikoru com a arma numerada 4, Biblioteca v22 secao 9): voo. Com alvo, sai do chao e paira
     * {@code hover_height} blocos acima do topo do alvo a {@code fly_speed} blocos/tick; durante uma tecnica desce ate
     * a altura do alvo (mergulho) e volta a subir depois. Sem alvo, pousa.
     */
    public record Flight(float flySpeed, float hoverHeight) {
        public static final Codec<Flight> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.05F, 3.0F).fieldOf("fly_speed").forGetter(Flight::flySpeed),
                Codec.floatRange(0.0F, 16.0F).optionalFieldOf("hover_height", 2.5F).forGetter(Flight::hoverHeight)
        ).apply(i, Flight::new));
    }

    /**
     * Traje numerado 10 (0.6-F, especificacao do Miguel secao 3): cauda que luta sozinha
     * ({@code tail}), e o Full Release ao chegar a 100% de sincronizacao ({@code full_release}). A sincronizacao
     * ({@code numbers10_sync}) e a % de Release, que sobe com a escalada de combate.
     */
    public record Numbers10(Tail tail, FullRelease fullRelease) {
        public static final Codec<Numbers10> CODEC = RecordCodecBuilder.create(i -> i.group(
                Tail.CODEC.fieldOf("tail").forGetter(Numbers10::tail),
                FullRelease.CODEC.fieldOf("full_release").forGetter(Numbers10::fullRelease)
        ).apply(i, Numbers10::new));
    }

    /**
     * Cauda independente: a cada {@code cooldown_ticks} corta ({@code multiplier} x dano da espada) um inimigo a ate
     * {@code reach} blocos (de preferencia quem esta atacando o Hoshina, depois quem esta atras ou do lado), mesmo
     * enquanto ele luta com outro. Guarda: golpe pesado ou projetil vindo de fora da frente ({@code front_arc_degrees})
     * cai para {@code guard_factor}, com recarga {@code guard_cooldown_ticks}.
     */
    public record Tail(int cooldownTicks, float reach, float multiplier, float guardFactor, int guardCooldownTicks,
            float frontArcDegrees) {
        public static final Codec<Tail> CODEC = RecordCodecBuilder.create(i -> i.group(
                DefCodecs.TICKS.fieldOf("cooldown_ticks").forGetter(Tail::cooldownTicks),
                Codec.floatRange(0.5F, 16.0F).fieldOf("reach").forGetter(Tail::reach),
                DefCodecs.MULTIPLIER.fieldOf("multiplier").forGetter(Tail::multiplier),
                Codec.floatRange(0.0F, 1.0F).fieldOf("guard_factor").forGetter(Tail::guardFactor),
                DefCodecs.TICKS.optionalFieldOf("guard_cooldown_ticks", 30).forGetter(Tail::guardCooldownTicks),
                Codec.floatRange(0.0F, 360.0F).optionalFieldOf("front_arc_degrees", 120.0F)
                        .forGetter(Tail::frontArcDegrees)
        ).apply(i, Tail::new));
    }

    /** Full Release (100%): multiplicadores de dano, velocidade, recargas e recarga da cauda; libera as tecnicas com
     * {@code requires_full_release} (Juni-hitoe). */
    public record FullRelease(float damageMultiplier, float speedMultiplier, float cooldownMultiplier,
            float tailCooldownMultiplier) {
        public static final Codec<FullRelease> CODEC = RecordCodecBuilder.create(i -> i.group(
                DefCodecs.MULTIPLIER.fieldOf("damage_multiplier").forGetter(FullRelease::damageMultiplier),
                DefCodecs.MULTIPLIER.fieldOf("speed_multiplier").forGetter(FullRelease::speedMultiplier),
                DefCodecs.MULTIPLIER.fieldOf("cooldown_multiplier").forGetter(FullRelease::cooldownMultiplier),
                DefCodecs.MULTIPLIER.fieldOf("tail_cooldown_multiplier")
                        .forGetter(FullRelease::tailCooldownMultiplier)
        ).apply(i, FullRelease::new));
    }

    /** Forma da tecnica: cortes a distancia ou sequencia de golpes corpo a corpo. */
    public enum TechniqueType implements StringRepresentable {
        SLASH("slash"),
        COMBO("combo");

        public static final Codec<TechniqueType> CODEC = StringRepresentable.fromEnum(TechniqueType::values);

        private final String serializedName;

        TechniqueType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /**
     * Uma tecnica. {@code hits}: multiplicador de cada golpe sobre o dano da arma (combo: um por golpe; slash: o de
     * cada corte). O primeiro golpe/corte sai em {@code windup_ticks}, os outros a cada {@code hit_interval}.
     * {@code dash_in}: avanco ate o alvo no inicio (velocidade, 0 = sem). {@code sidestep_before_last}: pequeno
     * reposicionamento lateral antes do ultimo golpe (Kasumi-uchi). {@code expose_core_ticks}: o ultimo golpe expoe o
     * nucleo do kaiju por esse tempo (Yae-uchi; os aliados aproveitam). {@code final_knockback}: empurrao do ultimo
     * golpe. {@code honju_priority}: soma a prioridade contra Honju/numerados/Daikaiju.
     */
    public record Technique(TechniqueType type, List<Float> hits, int windupTicks, int hitInterval, int durationTicks,
            int cooldownTicks, float minRange, float maxRange, int priority, SlashSpec slash, float dashIn,
            boolean sidestepBeforeLast, int exposeCoreTicks, float finalKnockback, int honjuPriority,
            boolean requiresFullRelease) {
        public static final Codec<Technique> CODEC = RecordCodecBuilder.<Technique>create(i -> i.group(
                TechniqueType.CODEC.fieldOf("type").forGetter(Technique::type),
                DefCodecs.MULTIPLIER.listOf().fieldOf("hits").forGetter(Technique::hits),
                Codec.intRange(0, 200).fieldOf("windup_ticks").forGetter(Technique::windupTicks),
                Codec.intRange(1, 40).optionalFieldOf("hit_interval", 2).forGetter(Technique::hitInterval),
                Codec.intRange(1, 400).fieldOf("duration_ticks").forGetter(Technique::durationTicks),
                DefCodecs.TICKS.fieldOf("cooldown_ticks").forGetter(Technique::cooldownTicks),
                Codec.floatRange(0.0F, 64.0F).optionalFieldOf("min_range", 0.0F).forGetter(Technique::minRange),
                Codec.floatRange(0.0F, 64.0F).fieldOf("max_range").forGetter(Technique::maxRange),
                Codec.intRange(0, 100).optionalFieldOf("priority", 1).forGetter(Technique::priority),
                SlashSpec.CODEC.optionalFieldOf("slash", SlashSpec.DEFAULT).forGetter(Technique::slash),
                Codec.floatRange(0.0F, 4.0F).optionalFieldOf("dash_in", 0.0F).forGetter(Technique::dashIn),
                Codec.BOOL.optionalFieldOf("sidestep_before_last", false).forGetter(Technique::sidestepBeforeLast),
                DefCodecs.TICKS.optionalFieldOf("expose_core_ticks", 0).forGetter(Technique::exposeCoreTicks),
                Codec.floatRange(0.0F, 10.0F).optionalFieldOf("final_knockback", 0.0F)
                        .forGetter(Technique::finalKnockback),
                Codec.intRange(0, 100).optionalFieldOf("honju_priority", 0).forGetter(Technique::honjuPriority),
                Codec.BOOL.optionalFieldOf("requires_full_release", false).forGetter(Technique::requiresFullRelease)
        ).apply(i, Technique::new)).validate(Technique::check);

        private static DataResult<Technique> check(Technique technique) {
            if (technique.hits().isEmpty()) {
                return DataResult.error(() -> "hits precisa de pelo menos um golpe");
            }
            if (technique.lastHitTick() >= technique.durationTicks()) {
                return DataResult.error(() -> "o ultimo golpe (tick " + technique.lastHitTick()
                        + ") precisa sair antes de duration_ticks " + technique.durationTicks());
            }
            return DataResult.success(technique);
        }

        /** Tick (desde o inicio) do golpe {@code index}. */
        public int hitTick(int index) {
            return windupTicks + index * hitInterval;
        }

        public int lastHitTick() {
            return hitTick(type == TechniqueType.SLASH ? 0 : hits.size() - 1);
        }
    }

    /**
     * Esquiva: quando um golpe de kaiju vai acertar em ate {@code react_ticks}, salta para o lado/tras a
     * {@code speed}, invulneravel por {@code invulnerable_ticks}. Tambem fecha distancia (dash para a frente) com o
     * alvo a mais de {@code gap_close_distance}. Recarga {@code cooldown_ticks} (sem spam).
     */
    public record Dash(int cooldownTicks, float speed, int invulnerableTicks, int reactTicks, float gapCloseDistance) {
        public static final Codec<Dash> CODEC = RecordCodecBuilder.create(i -> i.group(
                DefCodecs.TICKS.fieldOf("cooldown_ticks").forGetter(Dash::cooldownTicks),
                Codec.floatRange(0.1F, 4.0F).fieldOf("speed").forGetter(Dash::speed),
                Codec.intRange(0, 40).optionalFieldOf("invulnerable_ticks", 6).forGetter(Dash::invulnerableTicks),
                Codec.intRange(1, 40).optionalFieldOf("react_ticks", 5).forGetter(Dash::reactTicks),
                Codec.floatRange(0.0F, 64.0F).optionalFieldOf("gap_close_distance", 7.0F)
                        .forGetter(Dash::gapCloseDistance)
        ).apply(i, Dash::new));
    }

    /**
     * Kaeshi-uchi: so contra golpe "heavy" de kaiju prestes a acertar (em ate {@code react_ticks}). Dash lateral
     * passando pelo inimigo, invulneravel por {@code invulnerable_ticks}, e o contra-ataque sai
     * {@code strike_delay_ticks} depois, com {@code multiplier} sobre o dano da arma.
     */
    public record Counter(float multiplier, int cooldownTicks, float dashSpeed, int invulnerableTicks,
            int strikeDelayTicks, int reactTicks) {
        public static final Codec<Counter> CODEC = RecordCodecBuilder.create(i -> i.group(
                DefCodecs.MULTIPLIER.fieldOf("multiplier").forGetter(Counter::multiplier),
                DefCodecs.TICKS.fieldOf("cooldown_ticks").forGetter(Counter::cooldownTicks),
                Codec.floatRange(0.1F, 4.0F).fieldOf("dash_speed").forGetter(Counter::dashSpeed),
                Codec.intRange(0, 40).optionalFieldOf("invulnerable_ticks", 10).forGetter(Counter::invulnerableTicks),
                Codec.intRange(1, 40).optionalFieldOf("strike_delay_ticks", 8).forGetter(Counter::strikeDelayTicks),
                Codec.intRange(1, 40).optionalFieldOf("react_ticks", 6).forGetter(Counter::reactTicks)
        ).apply(i, Counter::new));
    }

    /**
     * Parry: golpe corpo a corpo de kaiju (nao "heavy") tem {@code chance} de ser aparado: o dano cai para
     * {@code damage_factor}, faiscas e som metalico, e abre uma janela de {@code counter_window_ticks} para o
     * Kaeshi-uchi. Recarga {@code cooldown_ticks}.
     */
    public record Parry(float chance, float damageFactor, int cooldownTicks, int counterWindowTicks) {
        public static final Codec<Parry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Parry::chance),
                Codec.floatRange(0.0F, 1.0F).fieldOf("damage_factor").forGetter(Parry::damageFactor),
                DefCodecs.TICKS.optionalFieldOf("cooldown_ticks", 20).forGetter(Parry::cooldownTicks),
                DefCodecs.TICKS.optionalFieldOf("counter_window_ticks", 10).forGetter(Parry::counterWindowTicks)
        ).apply(i, Parry::new));
    }

    /**
     * Escalada de combate (Miguel, 0.6-D): com alvo, o Release sobe {@code points_per_second} ate +{@code max_points};
     * sem alvo, cai {@code decay_per_second}. Dano, velocidade e reducao de dano seguem o Release (formulas do
     * jogador); na escalada maxima as recargas das tecnicas e do dash caem {@code cooldown_reduction_at_max}.
     */
    public record Escalation(float pointsPerSecond, int maxPoints, float decayPerSecond,
            float cooldownReductionAtMax) {
        public static final Escalation NONE = new Escalation(0.0F, 0, 0.0F, 0.0F);
        public static final Codec<Escalation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 20.0F).fieldOf("points_per_second").forGetter(Escalation::pointsPerSecond),
                Codec.intRange(0, 100).fieldOf("max_points").forGetter(Escalation::maxPoints),
                Codec.floatRange(0.0F, 20.0F).optionalFieldOf("decay_per_second", 2.0F)
                        .forGetter(Escalation::decayPerSecond),
                Codec.floatRange(0.0F, 0.9F).optionalFieldOf("cooldown_reduction_at_max", 0.0F)
                        .forGetter(Escalation::cooldownReductionAtMax)
        ).apply(i, Escalation::new));
    }

    public static final Codec<SpecialSoldierDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(1.0F, 2000.0F).fieldOf("health").forGetter(SpecialSoldierDef::health),
            Codec.floatRange(0.0F, 30.0F).optionalFieldOf("armor", 0.0F).forGetter(SpecialSoldierDef::armor),
            Codec.floatRange(0.01F, 2.0F).fieldOf("speed").forGetter(SpecialSoldierDef::speed),
            Codec.floatRange(1.0F, 128.0F).optionalFieldOf("follow_range", 40.0F)
                    .forGetter(SpecialSoldierDef::followRange),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("knockback_resistance", 0.0F)
                    .forGetter(SpecialSoldierDef::knockbackResistance),
            Codec.intRange(0, 100).fieldOf("release").forGetter(SpecialSoldierDef::release),
            Codec.intRange(0, 100).optionalFieldOf("max_release", 100).forGetter(SpecialSoldierDef::maxRelease),
            Codec.floatRange(0.0F, 10.0F).optionalFieldOf("kaiju_damage", 1.0F)
                    .forGetter(SpecialSoldierDef::kaijuDamage),
            ResourceLocation.CODEC.fieldOf("aura").forGetter(SpecialSoldierDef::aura),
            ResourceLocation.CODEC.fieldOf("weapon").forGetter(SpecialSoldierDef::weapon),
            ResourceLocation.CODEC.optionalFieldOf("offhand").forGetter(SpecialSoldierDef::offhand),
            Codec.unboundedMap(Codec.STRING, Technique.CODEC).fieldOf("techniques")
                    .forGetter(SpecialSoldierDef::techniques),
            Dash.CODEC.fieldOf("dash").forGetter(SpecialSoldierDef::dash),
            Counter.CODEC.fieldOf("counter").forGetter(SpecialSoldierDef::counter),
            Parry.CODEC.fieldOf("parry").forGetter(SpecialSoldierDef::parry),
            Growth.MAP_CODEC.forGetter(SpecialSoldierDef::growth)
    ).apply(i, SpecialSoldierDef::new));
}
