// src/main/java/com/kn8/gametest/VfxGameTests.java
package com.kn8.gametest;

import java.util.ArrayList;
import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.combat.SlashProjectile;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SlashSpec;
import com.kn8.common.data.def.VfxProfileDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.vfx.VfxService;
import com.kn8.core.vfx.VfxColors;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * VFX-1 (diretriz KaijuVFX): todo kaiju e personagem tem perfil visual, com as cores decididas pelo Miguel; o efeito
 * sai com o perfil da forma ativa; efeitos desligados nao mudam o dano (diretriz §7).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VfxGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation BITE = KN8Constants.id("bite");
    private static final float TOLERANCE = 0.05F;

    private VfxGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void everyKaijuAndCharacterHasAVisualProfile(GameTestHelper helper) {
        List<ResourceLocation> ids = new ArrayList<>(KN8Data.KAIJU.server().keySet());
        ids.addAll(KN8Data.SPECIAL_SOLDIER.server().keySet());
        ids.add(KN8Constants.id("soldier"));
        ids.add(KN8Constants.id("player"));
        ids.forEach(id -> helper.assertTrue(KN8Data.VFX_PROFILE.server().containsKey(id),
                "Sem perfil visual: " + id));
        // Cores do Miguel (2026-10-22): Mina verde, Narumi rosa, Isao amarelo/laranja, Hoshina violeta.
        int mina = accent("mina");
        helper.assertTrue(green(mina) > red(mina) && green(mina) > blue(mina), "Mina deveria ser verde");
        int narumi = accent("narumi");
        helper.assertTrue(red(narumi) > green(narumi) && blue(narumi) > green(narumi), "Narumi deveria ser rosa");
        int isao = accent("isao");
        helper.assertTrue(red(isao) > green(isao) && green(isao) > blue(isao), "Isao deveria ser laranja");
        int hoshina = accent("hoshina");
        helper.assertTrue(blue(hoshina) > green(hoshina) && red(hoshina) > green(hoshina),
                "Hoshina deveria ser violeta");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void effectsCarryTheProfileOfTheActiveForm(GameTestHelper helper) {
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(2, 1, 2));
        HoshinaEntity no10 = helper.spawn(KN8Entities.HOSHINA_NO10.get(), new BlockPos(6, 1, 2));
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 6));
        helper.assertTrue(VfxService.profileOf(spider).orElseThrow().equals(KN8Constants.id("trichonephila")),
                "Perfil da aranha");
        helper.assertTrue(VfxService.profileOf(no10).orElseThrow().equals(KN8Constants.id("hoshina_no10")),
                "Perfil do Hoshina com o Numbers 10");
        helper.assertTrue(VfxService.profileOf(soldier).orElseThrow().equals(KN8Constants.id("soldier")),
                "Perfil do soldado");
        spider.discard();
        no10.discard();
        soldier.discard();
        helper.succeed();
    }

    /** O mesmo golpe com os efeitos desligados ([vfx] enabled false) tira a mesma vida no mesmo tick. */
    @GameTest(template = TEMPLATE)
    public static void effectsOffKeepTheDamage(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(3, 1, 4));
        kaiju.setNoAi(true);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(5, 1, 4));
        cow.setNoAi(true);
        float[] state = new float[2];
        helper.runAfterDelay(2, () -> {
            ServerConfig.VFX_ENABLED.set(false);
            state[0] = cow.getHealth();
            state[1] = (float) kaiju.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(kaiju.startAbility(BITE, cow), "A mordida deveria comecar");
        });
        helper.runAfterDelay(11, () -> {
            ServerConfig.VFX_ENABLED.set(true);
            helper.assertTrue(Math.abs(cow.getHealth() - (state[0] - state[1])) < TOLERANCE,
                    "Esperava " + (state[0] - state[1]) + " de vida sem efeitos, veio " + cow.getHealth());
            kaiju.discard();
            cow.discard();
            helper.succeed();
        });
    }

    /**
     * VFX-2: Kafka e Kaiju No. 8 com efeitos proprios (ficha): transformacao, regeneracao, cada soco e as tecnicas
     * a distancia do No. 8; o tiro e o soco do Kafka; nenhuma camada recusada na validacao.
     */
    @GameTest(template = TEMPLATE)
    public static void kafkaAndNo8HaveTheirOwnEffects(GameTestHelper helper) {
        VfxProfileDef no8 = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kaiju_no8"));
        VfxProfileDef kafka = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kafka"));
        helper.assertTrue(no8 != null && kafka != null, "Perfis do No. 8 e do Kafka");
        for (String key : List.of("transform", "regenerate", "rush_combo", "heavy_punch", "dash_strike", "counter",
                "ground_smash", "roar", "fist_trail", "slash")) {
            helper.assertTrue(no8.recipe(key).map(layers -> !layers.isEmpty()).orElse(false),
                    "No. 8 sem a receita " + key);
        }
        for (String key : List.of("transform", "weapon_fire", "punch_combo")) {
            helper.assertTrue(kafka.recipe(key).map(layers -> !layers.isEmpty()).orElse(false),
                    "Kafka sem a receita " + key);
        }
        // Socos do No. 8 sem arco de lamina (diretriz: silhueta curta e reta, nao a do corte).
        helper.assertTrue(no8.recipe("rush_combo").orElseThrow().stream().noneMatch(layer -> layer.shape()
                .equals("arc")), "Soco do No. 8 nao pode ter arco de corte");
        helper.succeed();
    }

    /**
     * VFX-3: cada forma do Hoshina com a propria silhueta (Yae-uchi = 8 arcos em estrela, Kosa-uchi em X), o
     * Hoshina com o Numbers 10 com Juni-hitoe, cauda, guarda da cauda e Full Release; Kafka com a aura cinza.
     */
    @GameTest(template = TEMPLATE)
    public static void hoshinaFormsHaveTheirOwnCuts(GameTestHelper helper) {
        VfxProfileDef hoshina = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("hoshina"));
        VfxProfileDef no10 = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("hoshina_no10"));
        helper.assertTrue(hoshina != null && no10 != null, "Perfis do Hoshina");
        java.util.Set<Float> star = new java.util.HashSet<>();
        for (int i = 1; i <= 8; i++) {
            star.add(hoshina.recipe("yae_uchi/" + i).orElseThrow().get(0).tilt());
        }
        helper.assertTrue(star.size() == 8, "Yae-uchi deveria ter 8 inclinacoes, tem " + star.size());
        helper.assertTrue(hoshina.recipe("kosa_uchi").orElseThrow().stream().anyMatch(layer -> layer.tilt() > 0)
                && hoshina.recipe("kosa_uchi").orElseThrow().stream().anyMatch(layer -> layer.tilt() < 0),
                "Kosa-uchi deveria ser um X");
        for (String key : List.of("juni_hitoe/1", "juni_hitoe/12", "tail", "tail_guard", "full_release")) {
            helper.assertTrue(no10.recipe(key).isPresent(), "Hoshina No. 10 sem a receita " + key);
        }
        helper.assertTrue(KN8Data.SPECIAL_SOLDIER.server().get(KN8Constants.id("kafka")).aura()
                .equals(KN8Constants.id("kafka_gray")), "Kafka com a aura cinza (Miguel, 2026-10-22)");
        helper.succeed();
    }

    /**
     * VFX-4A (Miguel: "sair raios igual no anime"): toda aura de traje e personagem tem raios, a do No. 8 azul/ciano
     * e a da Mina verde.
     */
    @GameTest(template = TEMPLATE)
    public static void everyAuraHasLightning(GameTestHelper helper) {
        KN8Data.AURA.server().forEach((id, aura) -> helper.assertTrue(aura.bolts().isPresent(),
                "Aura sem raios: " + id));
        int no8 = KN8Data.AURA.server().get(KN8Constants.id("kaiju_no8")).bolts().orElseThrow().color();
        helper.assertTrue(blue(no8) > red(no8), "Raios do No. 8 deveriam ser azuis");
        int mina = KN8Data.AURA.server().get(KN8Constants.id("mina_command")).color();
        helper.assertTrue(green(mina) > red(mina) && green(mina) > blue(mina), "Aura da Mina deveria ser verde");
        helper.succeed();
    }

    /**
     * VFX-R1 (Miguel: sem particulas): as formas desenhadas (raio, faixa, onda, pulso, feixe, clarao, fumaca)
     * passam pela validacao sem perder camada (perfil de previa {@code kn8:demo}).
     */
    @GameTest(template = TEMPLATE)
    public static void drawnShapesAreAccepted(GameTestHelper helper) {
        VfxProfileDef demo = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("demo"));
        helper.assertTrue(demo != null, "Perfil de previa kn8:demo");
        for (String shape : VfxProfileDef.DRAWN_SHAPES) {
            boolean used = demo.effects().values().stream().flatMap(List::stream)
                    .anyMatch(layer -> layer.shape().equals(shape) && layer.drawn());
            helper.assertTrue(used, "Forma desenhada recusada ou sem previa: " + shape);
        }
        helper.assertTrue(demo.recipe("band_x").orElseThrow().size() == 2, "As duas faixas do X");
        helper.succeed();
    }

    /**
     * VFX-R2 (Miguel: nenhum efeito com particulas): toda camada de toda receita vira forma desenhada (formas novas,
     * camada {@code generic} ou silhueta antiga traduzida por {@code DrawnFactory.fromParticle}).
     */
    @GameTest(template = TEMPLATE)
    public static void everyRecipeLayerIsDrawn(GameTestHelper helper) {
        List<String> legacy = List.of("arc", "ring", "shard", "streak", "glow", "spore", "pulse");
        int layers = 0;
        for (var entry : KN8Data.VFX_PROFILE.server().entrySet()) {
            for (var recipe : entry.getValue().effects().entrySet()) {
                for (VfxProfileDef.Layer layer : recipe.getValue()) {
                    layers++;
                    helper.assertTrue(layer.drawn() || layer.generic() || legacy.contains(layer.shape()),
                            "Camada sem forma desenhada: " + entry.getKey() + " " + recipe.getKey() + " "
                                    + layer.shape());
                }
            }
        }
        helper.assertTrue(layers > 100, "Receitas carregadas: " + layers);
        helper.succeed();
    }

    /**
     * VFX-4B: a Kikoru tem golpes de machado proprios (arco largo dourado, nao a lamina fina do Hoshina), marca no
     * chao so com receita e o vento das asas do Numbers 4 em ciano.
     */
    @GameTest(template = TEMPLATE)
    public static void kikoruAxeIsWideGoldAndGroundHasRecipe(GameTestHelper helper) {
        VfxProfileDef kikoru = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kikoru"));
        VfxProfileDef hoshina = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("hoshina"));
        VfxProfileDef no4 = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kikoru_no4"));
        helper.assertTrue(kikoru != null && hoshina != null && no4 != null, "Perfis da Kikoru e do Hoshina");
        VfxProfileDef.Layer band = kikoru.recipe("axe_slash/1").orElseThrow().stream()
                .filter(layer -> "band".equals(layer.shape())).findFirst().orElseThrow();
        int gold = kikoru.palette().resolve(band.color());
        helper.assertTrue(band.size() >= 0.6F && red(gold) > blue(gold) + 0.3F,
                "Arco do machado largo e dourado: " + band.size() + " " + VfxColors.format(gold));
        float hoshinaWidth = hoshina.recipe("kosa_uchi").orElseThrow().get(0).size();
        helper.assertTrue(band.size() > hoshinaWidth * 3, "Machado bem mais grosso que a lamina do Hoshina");
        for (String key : List.of("heavy_swing_1_ground", "shockwave", "ground_smash", "guard_break_1_ground")) {
            helper.assertTrue(kikoru.recipe(key).isPresent(), "Receita da Kikoru: " + key);
        }
        int wind = no4.palette().resolve(no4.recipe("wing_beat").orElseThrow().get(0).color());
        helper.assertTrue(blue(wind) > red(wind) + 0.3F, "Vento das asas ciano: " + VfxColors.format(wind));
        HoshinaEntity kikoruEntity = helper.spawnWithNoFreeWill(KN8Entities.KIKORU.get(), new BlockPos(1, 2, 1));
        HoshinaEntity hoshinaEntity = helper.spawnWithNoFreeWill(KN8Entities.HOSHINA.get(), new BlockPos(3, 2, 1));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(VfxService.hasRecipe(kikoruEntity, "heavy_swing_1_ground"),
                    "Marca no chao do golpe pesado da Kikoru");
            helper.assertFalse(VfxService.hasRecipe(hoshinaEntity, "kosa_uchi_1_ground"),
                    "Sem receita, sem pacote do chao (Hoshina)");
            helper.succeed();
        });
    }

    /**
     * VFX-5: a Mina carrega e explode em verde, o gelo do Reno/No. 6 tem rastro e impacto proprios; o tiro da tecnica
     * leva a chave dela (rastro so com receita: o corte do Hoshina continua sem).
     */
    @GameTest(template = TEMPLATE)
    public static void minaGreenCannonAndRenoIceTrail(GameTestHelper helper) {
        VfxProfileDef mina = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("mina"));
        VfxProfileDef reno = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("reno"));
        VfxProfileDef no6 = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("reno_no6"));
        helper.assertTrue(mina != null && reno != null && no6 != null, "Perfis da Mina e do Reno");
        for (String key : List.of("charged_cannon_charge", "anti_giant_shot_charge", "cannon_shot_trail",
                "cannon_shot_explosion", "anti_giant_shot_explosion")) {
            helper.assertTrue(mina.recipe(key).isPresent(), "Receita da Mina: " + key);
        }
        int blast = mina.palette().resolve(mina.recipe("cannon_shot_explosion").orElseThrow().stream()
                .filter(layer -> "wave".equals(layer.shape())).findFirst().orElseThrow().color());
        helper.assertTrue(green(blast) > red(blast) + 0.3F && green(blast) > blue(blast) + 0.2F,
                "Anel da explosao verde: " + VfxColors.format(blast));
        helper.assertTrue(reno.recipe("freeze_round_trail").isPresent() && reno.recipe("freeze_round_impact")
                .isPresent(), "Rastro e impacto da municao congelante");
        for (String key : List.of("ice_spear_trail", "freeze_burst_explosion", "ice_field")) {
            helper.assertTrue(no6.recipe(key).isPresent(), "Receita do Reno No. 6: " + key);
        }
        int frost = no6.palette().resolve(no6.recipe("ice_spear_impact").orElseThrow().get(1).color());
        helper.assertTrue(blue(frost) > red(frost) + 0.2F, "Gelo azul: " + VfxColors.format(frost));
        HoshinaEntity minaEntity = helper.spawnWithNoFreeWill(KN8Entities.MINA.get(), new BlockPos(1, 2, 1));
        HoshinaEntity hoshinaEntity = helper.spawnWithNoFreeWill(KN8Entities.HOSHINA.get(), new BlockPos(5, 2, 1));
        helper.runAfterDelay(2, () -> {
            SlashSpec cannon = new SlashSpec(1, 0.0F, 0.0F, 2.5F, 44.0F, 0.6F, 0x3FCF5A, true, 2.5F, 0, 1, 0, 1.0F,
                    0.0F);
            SlashProjectile.fire(minaEntity, new Vec3(0, 0, 1), cannon, 1.0F, 1.0F, 0, 0.0F, "cannon_shot");
            SlashProjectile.fire(hoshinaEntity, new Vec3(0, 0, 1), cannon, 1.0F, 1.0F, 0, 0.0F, "kuuchi");
            List<SlashProjectile> shots = helper.getLevel().getEntitiesOfClass(SlashProjectile.class,
                    minaEntity.getBoundingBox().inflate(12.0));
            SlashProjectile fromMina = shots.stream().filter(shot -> shot.getOwner() == minaEntity).findFirst()
                    .orElseThrow();
            SlashProjectile fromHoshina = shots.stream().filter(shot -> shot.getOwner() == hoshinaEntity)
                    .findFirst().orElseThrow();
            helper.assertTrue("cannon_shot".equals(fromMina.vfxKey()) && fromMina.trail()
                    .map(trail -> trail[1].equals("cannon_shot_trail")).orElse(false), "Tiro da Mina com rastro");
            helper.assertTrue(fromHoshina.trail().isEmpty(), "Corte do Hoshina sem receita de rastro");
            shots.forEach(SlashProjectile::discard);
            helper.succeed();
        });
    }

    /**
     * VFX-6: estocada do Narumi e linha reta (sem arco; so a varrida e arco), o Numbers 1 tem a previsao, o Thunder
     * Emperor e um soco sem raio (Miguel) e o Isao sem o Numbers 2 nao tem onda de energia.
     */
    @GameTest(template = TEMPLATE)
    public static void narumiThrustsAreLinesAndThunderEmperorHasNoBolt(GameTestHelper helper) {
        VfxProfileDef narumi = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("narumi"));
        VfxProfileDef no1 = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("narumi_no1"));
        VfxProfileDef isao = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("isao"));
        VfxProfileDef base = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("isao_base"));
        helper.assertTrue(narumi != null && no1 != null && isao != null && base != null, "Perfis do Narumi e do Isao");
        for (String key : List.of("thrust_combo/1", "thrust_combo/3", "lunge/1")) {
            List<VfxProfileDef.Layer> layers = narumi.recipe(key).orElseThrow();
            helper.assertTrue(layers.stream().anyMatch(layer -> "beam".equals(layer.shape()))
                    && layers.stream().noneMatch(layer -> "band".equals(layer.shape())), "Estocada em linha: " + key);
        }
        helper.assertTrue(narumi.recipe("sweeping_slash/1").orElseThrow().stream()
                .anyMatch(layer -> "band".equals(layer.shape())), "Varrida em arco");
        helper.assertTrue(no1.recipe("foresight").isPresent() && narumi.recipe("foresight").isEmpty(),
                "Previsao so no Numbers 1");
        helper.assertTrue(isao.recipe("thunder_emperor/2").orElseThrow().stream()
                .noneMatch(layer -> "bolt".equals(layer.shape())), "Thunder Emperor sem raio");
        helper.assertTrue(isao.recipe("thunder_emperor_charge").isPresent(), "Carga do Thunder Emperor");
        for (String key : List.of("jab_combo/1", "heavy_straight/1")) {
            int color = base.palette().resolve(base.recipe(key).orElseThrow().get(0).color());
            float spread = Math.max(red(color), Math.max(green(color), blue(color)))
                    - Math.min(red(color), Math.min(green(color), blue(color)));
            helper.assertTrue(spread < 0.2F, "Soco sem energia (sem cor saturada): " + key);
        }
        helper.succeed();
    }

    /**
     * VFX-7: Finger Gun cinza (Miguel) em todas as formas que o usam, eventos do No. 9 (pele, adaptacao, regeneracao,
     * retirada, fuga, absorcao) com receita propria em cada forma, raio hibrido com a faixa bege e o Thunder Emperor
     * da forma com o Isao sem raio.
     */
    @GameTest(template = TEMPLATE)
    public static void no9FormsHaveOwnEffectsAndGrayFingerGun(GameTestHelper helper) {
        for (String form : List.of("kaiju_no9", "kaiju_no9_black", "kaiju_no9_fusion", "kaiju_no9_fusion_giant",
                "kaiju_no9_camponotus", "kaiju_no9_isao")) {
            VfxProfileDef def = KN8Data.VFX_PROFILE.server().get(KN8Constants.id(form));
            helper.assertTrue(def != null, "Perfil " + form);
            for (String key : List.of("harden", "harden_hit", "adapt", "regenerate", "retreat", "escape",
                    "absorb_end")) {
                helper.assertTrue(def.recipe(key).isPresent(), "Evento do No. 9 em " + form + ": " + key);
            }
            def.recipe("finger_gun").ifPresent(layers -> {
                int color = def.palette().resolve(layers.get(1).color());
                helper.assertTrue(Math.abs(red(color) - blue(color)) < 0.05F && red(color) > 0.5F,
                        "Finger Gun cinza em " + form + ": " + VfxColors.format(color));
            });
        }
        VfxProfileDef fusion = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kaiju_no9_fusion"));
        helper.assertTrue(fusion.recipe("no9_fusion_hybrid_beam").orElseThrow().stream()
                .anyMatch(layer -> "core".equals(layer.color())), "Raio hibrido com a faixa bege");
        VfxProfileDef isao = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kaiju_no9_isao"));
        helper.assertTrue(isao.recipe("no9_isao_thunder_emperor").orElseThrow().stream()
                .noneMatch(layer -> "bolt".equals(layer.shape())), "Thunder Emperor do No. 9 sem raio");
        KaijuEntity no9 = helper.spawnWithNoFreeWill(KN8Entities.KAIJU_NO9.get(), new BlockPos(3, 2, 3));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(VfxService.hasRecipe(no9, "finger_gun_muzzle"), "Clarao na saida do Finger Gun");
            no9.discard();
            helper.succeed();
        });
    }

    /**
     * VFX-8: raio do Preondactyl laranja/vermelho (Miguel), autodestruicao e carga proprias, No. 10 gigante com o
     * crescimento, rastro da investida do No. 13 e a larva com efeito proprio.
     */
    @GameTest(template = TEMPLATE)
    public static void no10No13PreondactylAndLarvaHaveOwnEffects(GameTestHelper helper) {
        VfxProfileDef preon = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("preondactyl"));
        VfxProfileDef giant = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kaiju_no10_giant"));
        VfxProfileDef small = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kaiju_no10_small"));
        VfxProfileDef no13 = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kaiju_no13"));
        VfxProfileDef larva = KN8Data.VFX_PROFILE.server().get(KN8Constants.id("kaiju_larva"));
        for (String key : List.of("energy_charge", "preondactyl_energy_beam", "preondactyl_energy_beam_muzzle",
                "self_destruct_charge", "self_destruct", "preondactyl_dive_strike")) {
            helper.assertTrue(preon.recipe(key).isPresent(), "Receita do Preondactyl: " + key);
        }
        int beam = KN8Data.ABILITY.server().get(KN8Constants.id("preondactyl_energy_beam")).behavior().color();
        helper.assertTrue(red(beam) > 0.8F && blue(beam) < 0.3F, "Raio laranja/vermelho: " + VfxColors.format(beam));
        helper.assertTrue(giant.recipe("grow").isPresent() && small.recipe("grow").isEmpty(),
                "So a forma gigante cresce com efeito proprio");
        helper.assertTrue(giant.recipe("no10g_finger_cannon").isPresent()
                && small.recipe("no10_finger_cannon").isPresent(), "Finger Cannon das duas formas");
        helper.assertTrue(no13.recipe("no13_sprint_trail").isPresent(), "Rastro da investida do No. 13");
        helper.assertTrue(larva.recipe("bite").isPresent(), "Larva com efeito proprio");
        helper.succeed();
    }

    /**
     * VFX-9: os outros kaiju com receitas proprias para os golpes de cada um; acido da formiga verde (Miguel), sopro
     * do Diclonius vermelho, versoes revividas com vapor e cada tiro com a saida propria.
     */
    @GameTest(template = TEMPLATE)
    public static void otherKaijuHaveOwnEffects(GameTestHelper helper) {
        List<String> species = List.of("trichonephila", "trichonephila_honju", "primigenius",
                "primigenius_resurrected", "primigenius_honju", "primigenius_revived", "philinosoma", "diclonius",
                "camponotus", "camponotus_reborn", "phaneroplasmodium", "myxogasterocarp");
        for (String id : species) {
            VfxProfileDef def = KN8Data.VFX_PROFILE.server().get(KN8Constants.id(id));
            helper.assertTrue(def != null && def.effects().size() >= 6, "Receitas de " + id);
        }
        int acid = KN8Data.ABILITY.server().get(KN8Constants.id("camponotus_acid_spray")).behavior().color();
        helper.assertTrue(green(acid) > red(acid) + 0.2F && green(acid) > blue(acid) + 0.4F,
                "Acido da formiga verde: " + VfxColors.format(acid));
        int breath = KN8Data.ABILITY.server().get(KN8Constants.id("diclonius_atomic_breath")).behavior().color();
        helper.assertTrue(red(breath) > blue(breath) + 0.4F,
                "Sopro do Diclonius vermelho: " + VfxColors.format(breath));
        for (String id : List.of("primigenius_resurrected", "primigenius_revived")) {
            helper.assertTrue(KN8Data.VFX_PROFILE.server().get(KN8Constants.id(id)).recipe("slam").orElseThrow()
                    .stream().anyMatch(layer -> "wisp".equals(layer.shape())), "Vapor de revivido em " + id);
        }
        helper.assertTrue(KN8Data.VFX_PROFILE.server().get(KN8Constants.id("primigenius_honju"))
                .recipe("energy_blast_muzzle").isPresent(), "Saida do raio do Honju");
        helper.succeed();
    }

    /** VFX-10: toda habilidade de kaiju e toda tecnica de especial tem a propria receita (nenhuma cai no generico). */
    @GameTest(template = TEMPLATE)
    public static void everyAbilityAndTechniqueHasARecipe(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        KN8Data.KAIJU.server().forEach((id, kaiju) -> {
            VfxProfileDef def = KN8Data.VFX_PROFILE.server().get(id);
            for (ResourceLocation ability : kaiju.abilities()) {
                if (def == null || !def.effects().containsKey(ability.getPath())) {
                    missing.add(id.getPath() + ":" + ability.getPath());
                }
            }
        });
        KN8Data.SPECIAL_SOLDIER.server().forEach((id, soldier) -> {
            VfxProfileDef def = KN8Data.VFX_PROFILE.server().get(id);
            for (String technique : soldier.techniques().keySet()) {
                boolean found = def != null && def.effects().keySet().stream()
                        .anyMatch(key -> key.equals(technique) || key.startsWith(technique + "/"));
                if (!found) {
                    missing.add(id.getPath() + ":" + technique);
                }
            }
        });
        helper.assertTrue(missing.isEmpty(), "Sem receita: " + missing);
        helper.succeed();
    }

    private static int accent(String id) {
        VfxProfileDef def = KN8Data.VFX_PROFILE.server().get(KN8Constants.id(id));
        return def == null ? 0 : def.palette().resolve("accent");
    }

    private static float red(int rgb) {
        return VfxColors.red(rgb);
    }

    private static float green(int rgb) {
        return VfxColors.green(rgb);
    }

    private static float blue(int rgb) {
        return VfxColors.blue(rgb);
    }
}
