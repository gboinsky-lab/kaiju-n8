// src/main/java/com/kn8/common/config/ServerConfig.java
package com.kn8.common.config;

import java.util.List;

import com.kn8.core.kaiju.KaijuStats;
import com.kn8.core.math.FortitudeCurve;
import com.kn8.core.power.PowerParams;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code kn8-server.toml} (tipo SERVER, sincronizado com os clientes pelo NeoForge). No NeoForge 21.1 o arquivo e
 * gerado em {@code <instancia>/config/}; uma copia em {@code <mundo>/serverconfig/} so serve para sobrescrever os
 * valores naquele mundo. Contem todo numero de balanceamento e todo limite de servidor (Fase 4, secao 4.5), mais o
 * que os prototipos revelaram (carcaca do PT7, partes dormentes do PT8), a protecao de rede do M3, os atributos
 * do jogador do M5 e a IA de kaiju do M7a.
 *
 * <p>Valores padrao = GDD. Comentarios em ingles porque aparecem no arquivo para administradores de qualquer pais.</p>
 */
public final class ServerConfig {

    private static final String PREFIX = "kn8.configuration.";
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- balance ---------------------------------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue KAIJU_HEALTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue KAIJU_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CURVE_HEALTH_BASE;
    public static final ModConfigSpec.DoubleValue CURVE_HEALTH_EXPONENT_BASE;
    public static final ModConfigSpec.DoubleValue CURVE_DAMAGE_BASE;
    public static final ModConfigSpec.DoubleValue CURVE_DAMAGE_EXPONENT_BASE;
    public static final ModConfigSpec.DoubleValue CURVE_ARMOR_PER_FORTITUDE;
    public static final ModConfigSpec.DoubleValue CURVE_ARMOR_CAP;
    public static final ModConfigSpec.EnumValue<KN8Difficulty> DIFFICULTY;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> DIFFICULTY_HEALTH_MULTIPLIERS;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> DIFFICULTY_DAMAGE_MULTIPLIERS;
    public static final ModConfigSpec.DoubleValue RELEASE_DAMAGE_DIVISOR;
    /** 0.5 (ideia do Miguel): abaixo desta fracao de vida a % sobe ate DESPERATION_MAX_POINTS (vida 0). */
    public static final ModConfigSpec.DoubleValue DESPERATION_HEALTH;
    public static final ModConfigSpec.IntValue DESPERATION_MAX_POINTS;
    // --- carreira e treino (0.2, Etapa 2) --------------------------------------------------------------------------
    public static final ModConfigSpec.IntValue RELEASE_MAX;
    public static final ModConfigSpec.DoubleValue XP_PER_KAIJU_DAMAGE;
    public static final ModConfigSpec.IntValue XP_KILL_YOJU;
    public static final ModConfigSpec.IntValue XP_KILL_HONJU;
    public static final ModConfigSpec.IntValue XP_DISMANTLE_STEP;
    public static final ModConfigSpec.IntValue DUMMY_XP_PER_HIT;
    public static final ModConfigSpec.IntValue DUMMY_XP_PER_MINUTE;
    public static final ModConfigSpec.IntValue MERIT_KILL_YOJU;
    public static final ModConfigSpec.IntValue MERIT_KILL_HONJU;
    public static final ModConfigSpec.IntValue MERIT_DISMANTLE;
    public static final ModConfigSpec.DoubleValue MISSION_AREA_MIN;
    public static final ModConfigSpec.DoubleValue MISSION_AREA_MAX;
    public static final ModConfigSpec.DoubleValue MISSION_PATROL_MIN;
    public static final ModConfigSpec.DoubleValue MISSION_PATROL_MAX;
    public static final ModConfigSpec.DoubleValue MISSION_REACH_RADIUS;
    public static final ModConfigSpec.DoubleValue SUPPLY_COOLANT_HEAT;
    public static final ModConfigSpec.IntValue SUPPLY_CATALYST_XP;
    public static final ModConfigSpec.BooleanValue INVASION_NATURAL;
    public static final ModConfigSpec.DoubleValue INVASION_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue INVASION_REWARD_MIN_FACTOR;
    public static final ModConfigSpec.DoubleValue INVASION_REWARD_MAX_FACTOR;

    // --- spawn -----------------------------------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue NATURAL_SPAWN;
    public static final ModConfigSpec.IntValue MAX_KAIJU_PER_CHUNK;
    public static final ModConfigSpec.IntValue MAX_KAIJU_PER_LEVEL;
    public static final ModConfigSpec.IntValue MAX_YOJU_PER_HONJU;
    public static final ModConfigSpec.IntValue MAX_TOTAL_PER_HONJU;
    public static final ModConfigSpec.BooleanValue SPAWN_SURFACE_ONLY;
    public static final ModConfigSpec.BooleanValue SPAWN_REQUIRE_DARKNESS;

    // --- events / destruction (sistemas da 0.6; chaves ja existem para o arquivo nao mudar de formato) ----------
    public static final ModConfigSpec.BooleanValue EVENTS_ENABLED;
    public static final ModConfigSpec.IntValue EVENTS_MAX_PER_REGION;
    public static final ModConfigSpec.IntValue EVENTS_MAX_PER_LEVEL;
    public static final ModConfigSpec.BooleanValue DESTRUCTION_ENABLED;
    public static final ModConfigSpec.IntValue DESTRUCTION_BLOCKS_PER_TICK;
    public static final ModConfigSpec.IntValue REBUILD_BLOCKS_PER_TICK;
    public static final ModConfigSpec.IntValue REBUILD_DELAY_TICKS;
    public static final ModConfigSpec.BooleanValue DESTRUCTION_DROP_ITEMS;
    public static final ModConfigSpec.IntValue DESTRUCTION_LOG_LIMIT;
    public static final ModConfigSpec.IntValue PATH_CLEAR_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue PATH_POWER_YOJU;
    public static final ModConfigSpec.IntValue PATH_POWER_HONJU;
    public static final ModConfigSpec.IntValue PATH_POWER_DAIKAIJU;

    // --- boss --------------------------------------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue BOSS_PLAYER_SCALING;
    public static final ModConfigSpec.BooleanValue BOSS_NPC_HELP;

    // --- transform ---------------------------------------------------------------------------------------------
    public static final ModConfigSpec.IntValue MAX_HOSTS;
    public static final ModConfigSpec.DoubleValue ENERGY_DRAIN_FULL;
    public static final ModConfigSpec.IntValue TRANSFORM_COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue BERSERK_ENABLED;

    // --- suspicion ---------------------------------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue SUSPICION_PER_WITNESS_FULL;
    public static final ModConfigSpec.DoubleValue SUSPICION_PER_WITNESS_PARTIAL;
    public static final ModConfigSpec.DoubleValue SUSPICION_DECAY_PER_DAY;

    // --- carcass (PT7) -------------------------------------------------------------------------------------------
    public static final ModConfigSpec.IntValue CARCASS_DESPAWN_TICKS;

    // --- pvp -----------------------------------------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue PVP_ENABLED;
    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE;

    // --- performance (PT8) ---------------------------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue FAR_PARTS_SLEEP;
    public static final ModConfigSpec.IntValue PARTS_SLEEP_RADIUS;

    // --- power / stamina / heat / energy / training (M5, GDD secoes 5 a 8) ---------------------------------------
    public static final ModConfigSpec.DoubleValue SPEED_PER_RELEASE;
    public static final ModConfigSpec.DoubleValue DAMAGE_REDUCTION_PER_RELEASE;
    public static final ModConfigSpec.DoubleValue MAX_DAMAGE_REDUCTION;
    public static final ModConfigSpec.DoubleValue KNOCKBACK_PER_RELEASE;
    public static final ModConfigSpec.IntValue SURGE_MAX;
    public static final ModConfigSpec.DoubleValue STAMINA_BASE;
    public static final ModConfigSpec.DoubleValue STAMINA_PER_RELEASE;
    public static final ModConfigSpec.DoubleValue STAMINA_REGEN_PER_SECOND;
    public static final ModConfigSpec.IntValue STAMINA_REGEN_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue SPRINT_STAMINA_PER_SECOND;
    public static final ModConfigSpec.DoubleValue SPRINT_MIN_STAMINA;
    public static final ModConfigSpec.DoubleValue WARM_REGEN_FACTOR;
    public static final ModConfigSpec.IntValue HEAT_WARM_AT;
    public static final ModConfigSpec.IntValue HEAT_OVERLOAD_AT;
    public static final ModConfigSpec.IntValue HEAT_CRITICAL_AT;
    public static final ModConfigSpec.IntValue HEAT_MAX;
    public static final ModConfigSpec.DoubleValue SURGE_HEAT_PER_10_PER_SECOND;
    public static final ModConfigSpec.DoubleValue COOL_OUT_OF_COMBAT_PER_SECOND;
    public static final ModConfigSpec.DoubleValue COOL_IN_COMBAT_PER_SECOND;
    public static final ModConfigSpec.IntValue COMBAT_GRACE_TICKS;
    public static final ModConfigSpec.DoubleValue OVERLOAD_DAMAGE_BONUS;
    public static final ModConfigSpec.DoubleValue OVERLOAD_DRAIN_PER_SECOND;
    public static final ModConfigSpec.DoubleValue CRITICAL_DRAIN_PER_SECOND;
    public static final ModConfigSpec.IntValue PANIC_TICKS;
    public static final ModConfigSpec.IntValue PANIC_RELEASE;
    public static final ModConfigSpec.DoubleValue PANIC_DAMAGE;
    public static final ModConfigSpec.DoubleValue ENERGY_MAX;
    public static final ModConfigSpec.DoubleValue ENERGY_REGEN_PER_SECOND;
    public static final ModConfigSpec.IntValue TRAINING_XP_BASE;
    public static final ModConfigSpec.IntValue TRAINING_XP_PER_POINT;

    // --- kaiju AI (M7a) -------------------------------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue FOLLOW_RANGE_BASE;
    public static final ModConfigSpec.DoubleValue FOLLOW_RANGE_PER_INTELLIGENCE;
    public static final ModConfigSpec.DoubleValue MELEE_REACH_BONUS;
    public static final ModConfigSpec.IntValue BASIC_ATTACK_INTERVAL_TICKS;
    public static final ModConfigSpec.DoubleValue CHARGE_SPEED;
    public static final ModConfigSpec.DoubleValue ABILITY_KNOCKBACK;
    /** 0.5 (Miguel): empurrao que o golpe de um soldado causa no kaiju (1 = vanilla; 0 = nenhum). */
    public static final ModConfigSpec.DoubleValue SOLDIER_KNOCKBACK_ON_KAIJU;
    public static final ModConfigSpec.DoubleValue LARGE_KAIJU_WIDTH;
    public static final ModConfigSpec.DoubleValue STEP_HEIGHT_FRACTION;

    // --- combat (M10, GDD secoes 7 e 12) ---------------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue LIGHT_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue HEAVY_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue DODGE_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue NO_STAMINA_SLOWDOWN;
    public static final ModConfigSpec.IntValue COMBO_WINDOW_TICKS;
    public static final ModConfigSpec.DoubleValue BLOCK_DAMAGE_REDUCTION;
    public static final ModConfigSpec.DoubleValue BLOCK_STAMINA_PER_DAMAGE;
    public static final ModConfigSpec.DoubleValue BLOCK_STAMINA_REGEN_PER_SECOND;
    public static final ModConfigSpec.IntValue DODGE_INVULNERABLE_TICKS;
    public static final ModConfigSpec.IntValue DODGE_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue DODGE_SPEED;
    public static final ModConfigSpec.DoubleValue CORE_EXPOSED_MULTIPLIER;
    public static final ModConfigSpec.IntValue CORE_EXPOSED_TICKS;
    public static final ModConfigSpec.IntValue PARRY_WINDOW_TICKS;
    public static final ModConfigSpec.IntValue PARRY_WINDOW_TICKS_HIGH;
    public static final ModConfigSpec.IntValue PARRY_HIGH_RELEASE;
    public static final ModConfigSpec.DoubleValue PARRY_STAMINA_REFUND;
    public static final ModConfigSpec.IntValue PARRY_STAGGER_TICKS;
    public static final ModConfigSpec.IntValue CRITICAL_WINDOW_TICKS;
    public static final ModConfigSpec.DoubleValue CRITICAL_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue HEAVY_IGNORES_BLOCK;
    public static final ModConfigSpec.DoubleValue DASH_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue DASH_SPEED;
    public static final ModConfigSpec.IntValue DASH_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue CHARGED_STAMINA_COST;
    public static final ModConfigSpec.IntValue CHARGE_MIN_TICKS;
    public static final ModConfigSpec.IntValue CHARGE_MAX_TICKS;
    public static final ModConfigSpec.DoubleValue CHARGE_FULL_MULTIPLIER;

    // --- vfx (0.1-B, Etapa D) -------------------------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue VFX_ENABLED;
    public static final ModConfigSpec.IntValue SUIT_VFX_INTERVAL_TICKS;

    // --- network (M3) --------------------------------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue RATE_LIMIT_MULTIPLIER;

    // --- debug ---------------------------------------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue DEBUG_COMMANDS;

    public static final ModConfigSpec SPEC;

    static {
        section("balance", "Kaiju strength and player power (GDD sections 5, 10, 36).");
        KAIJU_HEALTH_MULTIPLIER = doubleValue("kaijuHealthMultiplier", "Multiplies every kaiju's health.",
                1.0, 0.1, 10.0);
        KAIJU_DAMAGE_MULTIPLIER = doubleValue("kaijuDamageMultiplier", "Multiplies every kaiju's damage.",
                1.0, 0.1, 10.0);

        section("fortitudeCurve", "health = healthBase * healthExponentBase^(fortitude - 2); same shape for damage;"
                + " armor = min(armorPerFortitude * fortitude, armorCap).");
        CURVE_HEALTH_BASE = doubleValue("healthBase", "Health of a fortitude 2.0 kaiju.", 20.0, 1.0, 1000.0);
        CURVE_HEALTH_EXPONENT_BASE = doubleValue("healthExponentBase", "Health growth per fortitude point.",
                2.0, 1.1, 4.0);
        CURVE_DAMAGE_BASE = doubleValue("damageBase", "Damage of a fortitude 2.0 kaiju.", 2.0, 0.1, 100.0);
        CURVE_DAMAGE_EXPONENT_BASE = doubleValue("damageExponentBase", "Damage growth per fortitude point.",
                1.6, 1.1, 4.0);
        CURVE_ARMOR_PER_FORTITUDE = doubleValue("armorPerFortitude", "Armor points per fortitude point.",
                2.0, 0.0, 10.0);
        CURVE_ARMOR_CAP = doubleValue("armorCap", "Maximum kaiju armor.", 20.0, 0.0, 30.0);
        BUILDER.pop();

        DIFFICULTY = BUILDER.comment("Difficulty preset (GDD section 36).")
                .translation(PREFIX + "difficulty")
                .defineEnum("difficulty", KN8Difficulty.NORMAL);
        DIFFICULTY_HEALTH_MULTIPLIERS = multipliers("difficultyHealthMultipliers",
                "Kaiju health multiplier per difficulty, in order EASY, NORMAL, HARD, EXTREME, KN8.",
                List.of(0.7, 1.0, 1.4, 2.0, 2.5));
        DIFFICULTY_DAMAGE_MULTIPLIERS = multipliers("difficultyDamageMultipliers",
                "Kaiju damage multiplier per difficulty, in order EASY, NORMAL, HARD, EXTREME, KN8.",
                List.of(0.6, 1.0, 1.3, 1.6, 2.0));
        RELEASE_DAMAGE_DIVISOR = doubleValue("releaseDamageDivisor",
                "Player damage = weapon base * (1 + release% / divisor).", 25.0, 5.0, 100.0);
        DESPERATION_HEALTH = doubleValue("desperationHealth", "Health fraction below which the release rises on"
                + " its own (fighting with your back against the wall).", 0.5, 0.0, 1.0);
        DESPERATION_MAX_POINTS = intValue("desperationMaxPoints", "Release points added at zero health, growing"
                + " linearly from desperationHealth (0 = off).", 15, 0, 100);
        BUILDER.pop();

        // 0.2 (decisao do Miguel): a patente nao limita mais o Release; o teto e o mesmo para todos e se alcanca
        // treinando. A patente da vida, esquadrao e desbloqueios. Saiu o antigo "rankCaps".
        section("career", "Ranks, merit and release training sources (0.2).");
        RELEASE_MAX = intValue("releaseMax", "Maximum trainable release % for everyone.", 100, 1, 100);
        XP_PER_KAIJU_DAMAGE = doubleValue("xpPerKaijuDamage", "Training XP per point of damage dealt to kaiju.",
                0.5, 0.0, 100.0);
        XP_KILL_YOJU = intValue("xpKillYoju", "Training XP for killing a Yoju.", 40, 0, 100000);
        XP_KILL_HONJU = intValue("xpKillHonju", "Training XP for killing a Honju or bigger.", 200, 0, 100000);
        XP_DISMANTLE_STEP = intValue("xpDismantleStep", "Training XP per dismantle step.", 5, 0, 100000);
        DUMMY_XP_PER_HIT = intValue("dummyXpPerHit", "Training XP per hit on a training dummy.", 5, 0, 1000);
        DUMMY_XP_PER_MINUTE = intValue("dummyXpPerMinute", "Maximum training XP per minute from dummies.", 60, 0,
                100000);
        MERIT_KILL_YOJU = intValue("meritKillYoju", "Merit for killing a Yoju.", 25, 0, 100000);
        MERIT_KILL_HONJU = intValue("meritKillHonju", "Merit for killing a Honju or bigger.", 150, 0, 100000);
        MERIT_DISMANTLE = intValue("meritDismantle", "Merit for finishing a carcass dismantle.", 15, 0, 100000);
        BUILDER.pop();

        section("mission", "Missions (0.2): where targets and points appear around the player.");
        MISSION_AREA_MIN = doubleValue("areaMinDistance", "Minimum distance of a mission area (kaiju, boss).", 40.0,
                8.0, 512.0);
        MISSION_AREA_MAX = doubleValue("areaMaxDistance", "Maximum distance of a mission area.", 80.0, 8.0, 1024.0);
        MISSION_PATROL_MIN = doubleValue("patrolMinDistance", "Minimum distance of the next patrol point.", 40.0,
                8.0, 512.0);
        MISSION_PATROL_MAX = doubleValue("patrolMaxDistance", "Maximum distance of the next patrol point.", 90.0,
                8.0, 1024.0);
        MISSION_REACH_RADIUS = doubleValue("reachRadius", "Horizontal distance that counts as reaching a point.",
                6.0, 1.0, 64.0);
        BUILDER.pop();

        section("supply", "Supplies made at the Defense Force workbench (0.2).");
        SUPPLY_COOLANT_HEAT = doubleValue("coolantHeat", "Suit heat removed by one coolant.", 50.0, 0.0, 1000.0);
        SUPPLY_CATALYST_XP = intValue("catalystXp", "Release training XP given by one catalyst.", 500, 0, 100000);
        BUILDER.pop();

        section("invasion", "Invasion alerts (0.2). Kaiju only appear through alerts, invasions and missions.");
        INVASION_NATURAL = booleanValue("natural", "Invasions may start on their own at nightfall.", true);
        INVASION_NATURAL_CHANCE = doubleValue("naturalChance", "Chance per in-game day of a natural invasion.",
                0.2, 0.0, 1.0);
        INVASION_REWARD_MIN_FACTOR = doubleValue("rewardMinFactor", "Smallest share of the invasion reward for a"
                + " player who dealt any damage (0.3: rewards follow each player's contribution).", 0.25, 0.0, 10.0);
        INVASION_REWARD_MAX_FACTOR = doubleValue("rewardMaxFactor", "Largest share of the invasion reward for the"
                + " player who carried the fight (1.0 = an even share).", 1.5, 0.0, 10.0);
        BUILDER.pop();

        section("spawn", "Natural spawning and kaiju count limits.");
        NATURAL_SPAWN = booleanValue("natural", "Allow kaiju to spawn naturally in biomes (0.2: off; kaiju come"
                + " from alerts, invasions and missions).", false);
        MAX_KAIJU_PER_CHUNK = intValue("maxKaijuPerChunk", "Kaiju limit per chunk.", 4, 0, 64);
        MAX_KAIJU_PER_LEVEL = intValue("maxKaijuPerLevel", "Kaiju limit per dimension.", 60, 0, 500);
        MAX_YOJU_PER_HONJU = intValue("maxYojuPerHonju", "Yoju a Honju may keep alive at once.", 8, 0, 64);
        MAX_TOTAL_PER_HONJU = intValue("maxTotalPerHonju", "Yoju a Honju may spawn in its whole life.",
                20, 0, 256);
        SPAWN_SURFACE_ONLY = booleanValue("surfaceOnly",
                "Natural kaiju spawns only where the sky is visible (Phase 4, section 8.3).", true);
        SPAWN_REQUIRE_DARKNESS = booleanValue("requireDarkness",
                "Natural kaiju spawns follow vanilla monster light rules (night or dark places).", true);
        BUILDER.pop();

        section("events", "World events (version 0.6).");
        EVENTS_ENABLED = booleanValue("enabled", "Enable random world events.", true);
        EVENTS_MAX_PER_REGION = intValue("maxPerRegion", "Active events per region.", 1, 0, 10);
        EVENTS_MAX_PER_LEVEL = intValue("maxPerLevel", "Active events per dimension.", 2, 0, 10);
        BUILDER.pop();

        section("destruction", "Controlled block destruction inside marked zones (version 0.6).");
        DESTRUCTION_ENABLED = booleanValue("enabled",
                "Allow kaiju attacks to destroy blocks (0.1-B; protected areas are never touched).", true);
        DESTRUCTION_BLOCKS_PER_TICK = intValue("blocksPerTick", "Block changes per tick, per dimension.",
                32, 0, 512);
        REBUILD_BLOCKS_PER_TICK = intValue("rebuildBlocksPerTick", "Blocks restored per tick.", 16, 0, 512);
        REBUILD_DELAY_TICKS = intValue("rebuildDelayTicks", "Ticks after an event before rebuilding starts.",
                6000, 0, 72000);
        DESTRUCTION_DROP_ITEMS = booleanValue("dropItems", "Destroyed blocks drop items (off avoids item lag).", false);
        DESTRUCTION_LOG_LIMIT = intValue("logLimit", "Destroyed blocks remembered per dimension for restoration.",
                20000, 0, 1000000);
        // 0.6 (Miguel): kaiju quebram o caminho so de andar (presos em construcoes). Chaves novas ("walk*") para os
        // valores novos valerem tambem em servidores com o arquivo antigo (pathPower* era 1/2/3).
        PATH_CLEAR_COOLDOWN_TICKS = intValue("walkBreakCooldownTicks", "Minimum ticks between two path breaks of a"
                + " kaiju that bumps into blocks while walking (or is stuck inside them).", 5, 1, 200);
        PATH_POWER_YOJU = intValue("walkPowerYoju", "Destruction power (0-4) of a walking Yoju: breaks the block"
                + " categories below it (3 = fragile, normal and resistant: stone, bricks, concrete; 0 = off).", 3, 0,
                4);
        PATH_POWER_HONJU = intValue("walkPowerHonju", "Destruction power (0-4) of a walking Honju or numbered"
                + " kaiju.", 3, 0, 4);
        PATH_POWER_DAIKAIJU = intValue("walkPowerDaikaiju", "Destruction power (0-4) of a walking Daikaiju.", 4,
                0, 4);
        BUILDER.pop();

        section("boss", "Boss fights (GDD section 23).");
        BOSS_PLAYER_SCALING = doubleValue("playerScaling", "Extra boss health per additional player in the arena.",
                0.5, 0.0, 2.0);
        BOSS_NPC_HELP = booleanValue("npcHelp", "Story NPC captains join story boss fights.", true);
        BUILDER.pop();

        section("transform", "Kaiju No. 8 transformation (GDD sections 13 and 14).");
        MAX_HOSTS = intValue("maxHosts", "How many players may be kaiju hosts at once.", 1, 0, 100);
        ENERGY_DRAIN_FULL = doubleValue("energyDrainFull", "Energy drained per second in full kaiju form.",
                1.5, 0.0, 100.0);
        TRANSFORM_COOLDOWN_TICKS = intValue("cooldownTicks", "Cooldown after returning to human form.",
                400, 0, 72000);
        BERSERK_ENABLED = booleanValue("berserkEnabled", "Allow Berserk mode.", true);
        BUILDER.pop();

        section("suspicion", "Suspicion system (GDD section 19).");
        SUSPICION_PER_WITNESS_FULL = doubleValue("perWitnessFull",
                "Suspicion per Defense Force witness of a full transformation.", 15.0, 0.0, 100.0);
        SUSPICION_PER_WITNESS_PARTIAL = doubleValue("perWitnessPartial",
                "Suspicion per Defense Force witness of a partial transformation.", 5.0, 0.0, 100.0);
        SUSPICION_DECAY_PER_DAY = doubleValue("decayPerDay", "Suspicion lost per in-game day without incidents.",
                2.0, 0.0, 100.0);
        BUILDER.pop();

        section("carcass", "Kaiju carcasses (PT7).");
        CARCASS_DESPAWN_TICKS = intValue("despawnTicks", "Ticks until an untouched carcass disappears.",
                6000, 200, 72000);
        BUILDER.pop();

        section("pvp", "Player versus player rules (GDD section 33).");
        PVP_ENABLED = booleanValue("enabled", "Allow players to damage each other.", false);
        FRIENDLY_FIRE = booleanValue("friendlyFire", "Allow damage between members of the same squad.", false);
        BUILDER.pop();

        section("performance", "Server performance options (PT8).");
        FAR_PARTS_SLEEP = booleanValue("farPartsSleep",
                "Stop repositioning multipart hitboxes of kaiju with no player nearby.", false);
        PARTS_SLEEP_RADIUS = intValue("partsSleepRadius", "Distance without players before parts sleep.",
                64, 16, 256);
        BUILDER.pop();

        section("power", "Release percentage effects (GDD sections 5 and 6).");
        SPEED_PER_RELEASE = doubleValue("speedPerRelease", "Movement speed bonus per release point.",
                0.004, 0.0, 0.05);
        DAMAGE_REDUCTION_PER_RELEASE = doubleValue("damageReductionPerRelease",
                "Incoming damage reduction per release point.", 0.004, 0.0, 0.01);
        MAX_DAMAGE_REDUCTION = doubleValue("maxDamageReduction", "Maximum damage reduction from release.",
                0.4, 0.0, 0.9);
        KNOCKBACK_PER_RELEASE = doubleValue("knockbackPerRelease", "Knockback resistance per release point.",
                0.005, 0.0, 0.02);
        SURGE_MAX = intValue("surgeMax", "Maximum points a player can surge above the trained release.", 20, 0, 50);
        BUILDER.pop();

        section("stamina", "Stamina (GDD section 7).");
        STAMINA_BASE = doubleValue("base", "Stamina at 0% release.", 100.0, 1.0, 1000.0);
        STAMINA_PER_RELEASE = doubleValue("perRelease", "Extra maximum stamina per release point.", 0.5, 0.0, 5.0);
        STAMINA_REGEN_PER_SECOND = doubleValue("regenPerSecond", "Stamina regenerated per second.", 15.0, 0.0,
                200.0);
        STAMINA_REGEN_DELAY_TICKS = intValue("regenDelayTicks", "Ticks without spending before regeneration.",
                20, 0, 200);
        SPRINT_STAMINA_PER_SECOND = doubleValue("sprintCostPerSecond",
                "Stamina spent per second while sprinting (creative/spectator are free).", 5.0, 0.0, 100.0);
        SPRINT_MIN_STAMINA = doubleValue("sprintMinStamina",
                "Stamina needed to sprint again after running out.", 20.0, 0.0, 1000.0);
        WARM_REGEN_FACTOR = doubleValue("warmRegenFactor", "Regeneration multiplier from the WARM heat stage on.",
                0.75, 0.0, 1.0);
        BUILDER.pop();

        section("heat", "Suit heat (GDD section 8).");
        HEAT_WARM_AT = intValue("warmAt", "Heat where the WARM stage starts.", 40, 0, 1000);
        HEAT_OVERLOAD_AT = intValue("overloadAt", "Heat where the OVERLOAD stage starts.", 70, 0, 1000);
        HEAT_CRITICAL_AT = intValue("criticalAt", "Heat where the CRITICAL stage starts.", 90, 0, 1000);
        HEAT_MAX = intValue("max", "Maximum heat; reaching it triggers a suit panic.", 100, 1, 1000);
        SURGE_HEAT_PER_10_PER_SECOND = doubleValue("surgeHeatPer10PerSecond",
                "Heat per second for every 10 surge points.", 2.0, 0.0, 100.0);
        COOL_OUT_OF_COMBAT_PER_SECOND = doubleValue("coolOutOfCombatPerSecond", "Cooling per second out of combat.",
                10.0, 0.0, 100.0);
        COOL_IN_COMBAT_PER_SECOND = doubleValue("coolInCombatPerSecond", "Cooling per second in combat.", 3.0, 0.0,
                100.0);
        COMBAT_GRACE_TICKS = intValue("combatGraceTicks", "Ticks after the last hit dealt or taken that still count"
                + " as combat.", 100, 0, 1200);
        OVERLOAD_DAMAGE_BONUS = doubleValue("overloadDamageBonus", "Extra damage dealt in the OVERLOAD stage.",
                0.10, 0.0, 2.0);
        OVERLOAD_DRAIN_PER_SECOND = doubleValue("overloadDrainPerSecond", "Health lost per second in OVERLOAD.",
                0.5, 0.0, 20.0);
        CRITICAL_DRAIN_PER_SECOND = doubleValue("criticalDrainPerSecond", "Health lost per second in CRITICAL.",
                1.0, 0.0, 20.0);
        PANIC_TICKS = intValue("panicTicks", "Duration of a suit panic.", 200, 0, 1200);
        PANIC_RELEASE = intValue("panicRelease", "Release percentage forced during a suit panic.", 1, 0, 100);
        PANIC_DAMAGE = doubleValue("panicDamage", "Damage taken when the suit panics.", 4.0, 0.0, 100.0);
        BUILDER.pop();

        section("energy", "Kaiju energy (GDD sections 5 and 13).");
        ENERGY_MAX = doubleValue("max", "Maximum kaiju energy.", 100.0, 1.0, 1000.0);
        ENERGY_REGEN_PER_SECOND = doubleValue("regenPerSecond", "Energy regenerated per second in human form.",
                1.0, 0.0, 100.0);
        BUILDER.pop();

        section("training", "Release training (GDD section 6).");
        TRAINING_XP_BASE = intValue("xpBase", "Training XP from 0 to 1%.", 50, 1, 100000);
        TRAINING_XP_PER_POINT = intValue("xpPerPoint", "Extra training XP per point already trained.", 10, 0,
                100000);
        BUILDER.pop();

        section("kaiju", "Kaiju behaviour (M7a). Strength comes from the fortitude curve and the multipliers above.");
        FOLLOW_RANGE_BASE = doubleValue("followRangeBase", "Detection range of an intelligence 0 kaiju, in blocks.",
                16.0, 1.0, 128.0);
        FOLLOW_RANGE_PER_INTELLIGENCE = doubleValue("followRangePerIntelligence",
                "Extra detection range per intelligence point (JSON field 'intelligence', 1 to 5).", 8.0, 0.0, 64.0);
        MELEE_REACH_BONUS = doubleValue("meleeReachBonus",
                "Melee ability reach beyond the touching distance of both hitboxes, in blocks.", 1.0, 0.0, 8.0);
        BASIC_ATTACK_INTERVAL_TICKS = intValue("basicAttackIntervalTicks",
                "Attack interval of kaiju species without abilities in their JSON.", 20, 1, 200);
        CHARGE_SPEED = doubleValue("chargeSpeed", "Speed of a kaiju charge (kn8:charge), in blocks per tick.", 0.6,
                0.1, 3.0);
        ABILITY_KNOCKBACK = doubleValue("abilityKnockback", "Knockback strength of area and charge abilities.", 0.8,
                0.0, 5.0);
        SOLDIER_KNOCKBACK_ON_KAIJU = doubleValue("soldierKnockback", "Knockback a Defense Force soldier's hit causes"
                + " on a kaiju, as a multiplier of vanilla (0 = none: groups of soldiers kept kaiju from reaching"
                + " them).", 0.0, 0.0, 1.0);
        LARGE_KAIJU_WIDTH = doubleValue("largeKaijuWidth", "Kaiju at least this wide walk straight to their target"
                + " instead of using vanilla pathfinding (which fails for wide mobs).", 2.5, 1.0, 30.0);
        STEP_HEIGHT_FRACTION = doubleValue("stepHeightFraction", "Step height of a kaiju as a fraction of its"
                + " height (clamped between 1 and 4 blocks).", 0.25, 0.0, 1.0);
        BUILDER.pop();

        section("combat", "Player combat (GDD sections 7 and 12).");
        LIGHT_STAMINA_COST = doubleValue("lightStaminaCost", "Stamina spent by a light attack.", 5.0, 0.0, 100.0);
        HEAVY_STAMINA_COST = doubleValue("heavyStaminaCost", "Stamina spent by a heavy attack.", 12.0, 0.0, 100.0);
        DODGE_STAMINA_COST = doubleValue("dodgeStaminaCost", "Stamina spent by a dodge (refused without it).", 20.0,
                0.0, 100.0);
        NO_STAMINA_SLOWDOWN = doubleValue("noStaminaSlowdown", "Attacks without enough stamina are this many times"
                + " slower.", 1.3, 1.0, 3.0);
        COMBO_WINDOW_TICKS = intValue("comboWindowTicks", "Ticks after a light attack ends in which the next one"
                + " continues the combo.", 10, 0, 100);
        BLOCK_DAMAGE_REDUCTION = doubleValue("blockDamageReduction", "Fraction of frontal damage stopped by"
                + " blocking.", 0.7, 0.0, 1.0);
        BLOCK_STAMINA_PER_DAMAGE = doubleValue("blockStaminaPerDamage", "Stamina spent per point of blocked damage.",
                0.5, 0.0, 10.0);
        BLOCK_STAMINA_REGEN_PER_SECOND = doubleValue("blockStaminaRegenPerSecond", "Stamina regenerated per second"
                + " while blocking.", 5.0, 0.0, 100.0);
        DODGE_INVULNERABLE_TICKS = intValue("dodgeInvulnerableTicks", "Invulnerability ticks at the start of a"
                + " dodge.", 6, 0, 40);
        DODGE_DURATION_TICKS = intValue("dodgeDurationTicks", "Duration of a dodge (no other action meanwhile).",
                10, 1, 40);
        DODGE_SPEED = doubleValue("dodgeSpeed", "Horizontal speed of the dodge burst.", 0.9, 0.0, 5.0);
        CORE_EXPOSED_MULTIPLIER = doubleValue("coreExposedMultiplier", "Extra core damage multiplier after a heavy"
                + " hit exposes the core.", 1.5, 1.0, 5.0);
        CORE_EXPOSED_TICKS = intValue("coreExposedTicks", "How long the core stays exposed.", 100, 0, 1200);
        PARRY_WINDOW_TICKS = intValue("parryWindowTicks", "A block started at most this many ticks before the hit"
                + " is a parry.", 3, 0, 20);
        PARRY_WINDOW_TICKS_HIGH = intValue("parryWindowTicksHigh", "Parry window from parryHighRelease on.", 6, 0,
                20);
        PARRY_HIGH_RELEASE = intValue("parryHighRelease", "Release % that unlocks the larger parry window and"
                + " lets parries stagger Honju (GDD section 6).", 60, 0, 100);
        PARRY_STAMINA_REFUND = doubleValue("parryStaminaRefund", "Stamina given back by a successful parry.", 10.0,
                0.0, 100.0);
        PARRY_STAGGER_TICKS = intValue("parryStaggerTicks", "How long a parried kaiju stays staggered.", 30, 0,
                200);
        CRITICAL_WINDOW_TICKS = intValue("criticalWindowTicks", "After a parry, the first hit within this many"
                + " ticks is a critical.", 40, 0, 200);
        CRITICAL_MULTIPLIER = doubleValue("criticalMultiplier", "Damage multiplier of a critical hit.", 1.5, 1.0,
                5.0);
        DASH_STAMINA_COST = doubleValue("dashStaminaCost", "Stamina spent by a dash (GDD section 7).", 25.0, 0.0,
                100.0);
        DASH_SPEED = doubleValue("dashSpeed", "Horizontal speed of the dash burst.", 1.6, 0.0, 5.0);
        DASH_DURATION_TICKS = intValue("dashDurationTicks", "Duration of a dash (no other action meanwhile).", 8, 1,
                40);
        CHARGED_STAMINA_COST = doubleValue("chargedStaminaCost", "Stamina spent by a charged attack (GDD section"
                + " 7).", 20.0, 0.0, 100.0);
        CHARGE_MIN_TICKS = intValue("chargeMinTicks", "Holding less than this is a normal heavy attack.", 6, 0, 100);
        CHARGE_MAX_TICKS = intValue("chargeMaxTicks", "Ticks for a full charge (full charge = critical).", 30, 1,
                200);
        CHARGE_FULL_MULTIPLIER = doubleValue("chargeFullMultiplier", "Extra damage multiplier at full charge (on top"
                + " of the heavy action).", 2.0, 1.0, 10.0);
        HEAVY_IGNORES_BLOCK = booleanValue("heavyIgnoresBlock", "Kaiju abilities marked \"heavy\" go through a"
                + " normal block; only a parry or a dodge avoids them.", true);
        BUILDER.pop();

        section("vfx", "Visual effects sent to clients (0.1-B). Clients choose how many particles they draw.");
        VFX_ENABLED = booleanValue("enabled", "Send visual effects (impacts, shockwaves, dust, auras).", true);
        SUIT_VFX_INTERVAL_TICKS = intValue("suitVfxIntervalTicks", "Ticks between suit overheat effect pulses (the"
                + " power aura is drawn by each client from data/kn8/kn8/aura/*.json).",
                10, 2, 100);
        BUILDER.pop();

        section("network", "Network protection (M3).");
        RATE_LIMIT_MULTIPLIER = doubleValue("rateLimitMultiplier",
                "Scales every client packet rate limit (1.0 = protocol defaults; raise for laggy servers).",
                1.0, 0.1, 10.0);
        BUILDER.pop();

        section("debug", "Debug tools.");
        DEBUG_COMMANDS = booleanValue("allowCommands", "Enable /kn8 debug commands for operators.", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ServerConfig() {
    }

    /** Curva de fortitude com os valores atuais do config. Chamar no momento do uso (o config pode recarregar). */
    public static FortitudeCurve fortitudeCurve() {
        return new FortitudeCurve(CURVE_HEALTH_BASE.get(), CURVE_HEALTH_EXPONENT_BASE.get(), CURVE_DAMAGE_BASE.get(),
                CURVE_DAMAGE_EXPONENT_BASE.get(), CURVE_ARMOR_PER_FORTITUDE.get(), CURVE_ARMOR_CAP.get());
    }

    /** Parametros das regras de poder do jogador com os valores atuais do config (M5). */
    public static PowerParams powerParams() {
        return new PowerParams(RELEASE_DAMAGE_DIVISOR.get(), SPEED_PER_RELEASE.get(),
                DAMAGE_REDUCTION_PER_RELEASE.get(), MAX_DAMAGE_REDUCTION.get(), KNOCKBACK_PER_RELEASE.get(),
                SURGE_MAX.get(), STAMINA_BASE.get(), STAMINA_PER_RELEASE.get(), STAMINA_REGEN_PER_SECOND.get(),
                STAMINA_REGEN_DELAY_TICKS.get(), WARM_REGEN_FACTOR.get(), HEAT_WARM_AT.get(), HEAT_OVERLOAD_AT.get(),
                HEAT_CRITICAL_AT.get(), HEAT_MAX.get(), SURGE_HEAT_PER_10_PER_SECOND.get(),
                COOL_OUT_OF_COMBAT_PER_SECOND.get(), COOL_IN_COMBAT_PER_SECOND.get(), ENERGY_MAX.get(),
                ENERGY_REGEN_PER_SECOND.get(), TRAINING_XP_BASE.get(), TRAINING_XP_PER_POINT.get());
    }

    /** Multiplicadores do config para os atributos de kaiju (global e dificuldade atual). */
    public static KaijuStats.Multipliers kaijuMultipliers() {
        return new KaijuStats.Multipliers(KAIJU_HEALTH_MULTIPLIER.get(), KAIJU_DAMAGE_MULTIPLIER.get(),
                difficultyHealthMultiplier(), difficultyDamageMultiplier());
    }

    /** Multiplicador de vida da dificuldade atual (lista na ordem de {@link KN8Difficulty}). */
    public static double difficultyHealthMultiplier() {
        return pick(DIFFICULTY_HEALTH_MULTIPLIERS.get(), DIFFICULTY.get());
    }

    /** Multiplicador de dano da dificuldade atual. */
    public static double difficultyDamageMultiplier() {
        return pick(DIFFICULTY_DAMAGE_MULTIPLIERS.get(), DIFFICULTY.get());
    }

    private static double pick(List<? extends Double> values, KN8Difficulty difficulty) {
        int index = difficulty.ordinal();
        // Lista editada a mao com menos itens: usa 1.0 em vez de quebrar o servidor (regra 10).
        return index < values.size() ? values.get(index) : 1.0;
    }

    private static void section(String name, String comment) {
        BUILDER.comment(comment).translation(PREFIX + name).push(name);
    }

    private static ModConfigSpec.DoubleValue doubleValue(String name, String comment, double def, double min,
            double max) {
        return BUILDER.comment(comment).translation(PREFIX + name).defineInRange(name, def, min, max);
    }

    private static ModConfigSpec.IntValue intValue(String name, String comment, int def, int min, int max) {
        return BUILDER.comment(comment).translation(PREFIX + name).defineInRange(name, def, min, max);
    }

    private static ModConfigSpec.BooleanValue booleanValue(String name, String comment, boolean def) {
        return BUILDER.comment(comment).translation(PREFIX + name).define(name, def);
    }

    private static ModConfigSpec.ConfigValue<List<? extends Double>> multipliers(String name, String comment,
            List<Double> defaults) {
        return BUILDER.comment(comment).translation(PREFIX + name).defineList(name, defaults, () -> 1.0,
                value -> value instanceof Double multiplier && multiplier >= 0.1 && multiplier <= 10.0);
    }
}
