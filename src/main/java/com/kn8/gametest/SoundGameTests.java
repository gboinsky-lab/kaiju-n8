package com.kn8.gametest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kn8.KN8Constants;
import com.kn8.common.data.DataValidation;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SoundProfileDef;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.sound.SoundCues;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * SONS (Miguel 2026-10-30: "todos os sons feitos pelo Claude"): todo som que os dados usam existe no sounds.json, todo
 * arquivo do sounds.json esta no jar, e cada personagem e os ataques a distancia dos kaiju tem som proprio por etapa.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoundGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private SoundGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void everySoundInTheDataExists(GameTestHelper helper) throws Exception {
        Set<String> bundled = DataValidation.bundledSounds();
        helper.assertTrue(bundled.size() > 60, "sounds.json do jar: " + bundled.size() + " eventos");
        List<String> missing = new ArrayList<>();
        KN8Data.ABILITY.server().forEach((id, ability) -> ability.sound().ifPresent(sound -> check(sound, bundled,
                "ability " + id, missing)));
        for (var entry : KN8Data.WEAPON.server().entrySet()) {
            WeaponDef weapon = entry.getValue();
            weapon.sounds().forEach((key, sound) -> check(sound, bundled, "weapon " + entry.getKey() + "." + key,
                    missing));
            weapon.allTechniques().forEach(technique -> technique.sound().ifPresent(sound -> check(sound, bundled,
                    "weapon " + entry.getKey() + " " + technique.id(), missing)));
        }
        for (var entry : KN8Data.SOUND_PROFILE.server().entrySet()) {
            List<SoundProfileDef.Cue> cues = new ArrayList<>(entry.getValue().techniques().values());
            cues.addAll(entry.getValue().abilities().values());
            for (SoundProfileDef.Cue cue : cues) {
                cue.windup().ifPresent(sound -> check(sound, bundled, "sound_profile " + entry.getKey(), missing));
                cue.release().ifPresent(sound -> check(sound, bundled, "sound_profile " + entry.getKey(), missing));
            }
        }
        helper.assertTrue(missing.isEmpty(), "Sons inexistentes: " + missing);
        // Cada arquivo listado no sounds.json do mod esta no jar (senao o cliente toca silencio).
        List<String> files = new ArrayList<>();
        try (InputStream in = SoundGameTests.class.getResourceAsStream("/assets/kn8/sounds.json")) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (String event : json.keySet()) {
                for (JsonElement sound : json.getAsJsonObject(event).getAsJsonArray("sounds")) {
                    String name = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString()
                            : sound.getAsString();
                    ResourceLocation id = ResourceLocation.parse(name);
                    String path = "/assets/" + id.getNamespace() + "/sounds/" + id.getPath() + ".ogg";
                    if (KN8Constants.MOD_ID.equals(id.getNamespace())
                            && SoundGameTests.class.getResource(path) == null) {
                        files.add(path);
                    }
                }
            }
        }
        helper.assertTrue(files.isEmpty(), "Arquivos que faltam: " + files);
        helper.succeed();
    }

    private static void check(ResourceLocation sound, Set<String> bundled, String where, List<String> missing) {
        if (!DataValidation.knownSound(sound, bundled)) {
            missing.add(where + " -> " + sound);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void everyCharacterAndRangedKaijuHasItsOwnSounds(GameTestHelper helper) {
        // Todo soldado especial: as tecnicas com som de golpe/disparo proprio (antes todas "sword_swing/heavy").
        KN8Data.SPECIAL_SOLDIER.server().forEach((id, def) -> {
            Optional<SoundProfileDef> profile = Optional.ofNullable(KN8Data.SOUND_PROFILE.server().get(id));
            helper.assertTrue(profile.isPresent(), "Sem perfil de som: " + id);
            def.techniques().keySet().forEach(technique -> helper.assertTrue(profile.get().technique(technique)
                    .flatMap(SoundProfileDef.Cue::release).isPresent(), "Tecnica sem som: " + id + " " + technique));
        });
        // Todo tiro de kaiju sai com som (antes so o impacto tinha); o raio de energia carrega antes.
        SoundProfileDef kaiju = KN8Data.SOUND_PROFILE.server().get(SoundCues.KAIJU);
        helper.assertTrue(kaiju != null, "Sem o perfil de som dos kaiju");
        KN8Data.ABILITY.server().forEach((id, ability) -> {
            if ("projectile".equals(ability.type().getPath())) {
                helper.assertTrue(kaiju.ability(id.getPath()).flatMap(SoundProfileDef.Cue::release).isPresent(),
                        "Tiro de kaiju sem som de saida: " + id);
            }
        });
        helper.assertTrue(kaiju.ability("energy_blast").flatMap(SoundProfileDef.Cue::windup).isPresent(),
                "Raio do Honju sem carga");
        // Personagens com assinatura propria: o canhao da Mina nao e o tiro do rifle, o soco do No. 8 nao e o machado.
        Optional<ResourceLocation> mina = KN8Data.SOUND_PROFILE.server().get(KN8Constants.id("mina"))
                .technique("cannon_shot").flatMap(SoundProfileDef.Cue::release);
        helper.assertTrue(mina.map(KN8Constants.id("weapon.cannon_shot")::equals).orElse(false),
                "Canhao da Mina: " + mina);
        WeaponDef fist = KN8Data.WEAPON.server().get(KN8Constants.id("kaiju_no8_fist"));
        helper.assertTrue(!fist.sounds().get("hit").getPath().startsWith("weapon.axe"),
                "Soco do No. 8 ainda com som de machado");
        // No servidor o kaiju toca a etapa pelo perfil (sem perfil: o aviso de antes).
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(4, 1, 4));
        honju.setNoAi(true);
        helper.assertTrue(SoundCues.ability(honju, "energy_blast", SoundCues.Stage.WINDUP, 1.0F),
                "Carga do raio pelo perfil");
        helper.assertFalse(SoundCues.ability(honju, "bite", SoundCues.Stage.WINDUP, 1.0F),
                "Mordida sem preparo: fica o som de antes");
        honju.discard();
        helper.succeed();
    }
}
