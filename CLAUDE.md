# CLAUDE.md — Kaiju No. 8: Defense Force (`kn8`)

Mod fan gratuito de Minecraft, desenvolvido por Miguel Augusto Gnoinsky. Este arquivo é a memória do projeto para o
Claude Code: leia antes de qualquer tarefa. Responda sempre em **português do Brasil**.

Memória técnica das novas frentes: `docs/SOLDADO_1_IMPLEMENTATION.md`, `docs/COMBAT_VFX_AND_DESTRUCTION.md`,
`docs/MEGA_ATUALIZACAO_0_2.md` (plano da 0.2, em etapas), `docs/PLANO_SOLDADOS_ESPECIAIS.md` (soldados especiais,
armas especiais, trajes numerados e novos kaiju: ideia do Miguel, entra com os modelos dele) e `docs/BALANCEAMENTO.md` (todos os números do jogo e onde
ficam; atualizar ao mudar qualquer JSON/config de balanceamento)
(ler antes de "Continue o Soldado 1", "Continue os efeitos", "Adicione este ataque").

Documentos de referência (coloque em `docs/` se ainda não estiverem): **Fase 3 — GDD**, **Fase 4 — Arquitetura
Técnica**, `Prompt_Fase4_Kaiju_No8_Mod.md`, `Prompt_Fase5_Kaiju_No8_Mod.md`. Em caso de dúvida, o GDD e a Fase 4
mandam; se algo precisar mudar, marque **[DECIDIR]** e pergunte.

---

## Stack e comandos

- Minecraft **1.21.1**, **NeoForge 21.1.252**, Java 21, ModDevGradle 2.0.148.
- GeckoLib 4.7.5 (entidades), Player Animation Library 1.1.5 (`transitive=false`, animação do jogador).
- Pacote raiz `com.kn8`, mod ID `kn8`. Licença do código: MIT.

```
./gradlew build                 # compila + JUnit
./gradlew runGameTestServer     # GameTests
./gradlew runServer             # servidor dedicado
./gradlew runClient / runClient2  # dois clientes (Dev1, Dev2) — todo teste manual é com 2 clientes
./gradlew runClientJoin         # Dev1 entrando direto no runServer local
```

- O `java` do PATH desta máquina é o 8: rodar o Gradle com `JAVA_HOME` no JDK 21
  (`~/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`).
- O projeto é um repositório git desde 2026-10-06 (primeiro commit = estado antes da 0.1-B). Python não está
  instalado nesta máquina (os scripts de `tools/` precisam dele).

---

## Forma de trabalho (Fase 5)

- **Uma etapa (módulo) por vez.** Ao terminar, pare e espere o teste e a aprovação do Miguel antes da próxima.
- Cada etapa entrega: objetivo e escopo; verificação de API (`// TODO VERIFICAR:` se não confirmar); arquivos;
  dados (JSON, config, lang `en_us` + `pt_br`); testes (JUnit para lógica pura, GameTests); roteiro manual com
  servidor dedicado + 2 clientes; critério de pronto (Fase 4 §12); commit `feat(mX): ...`; delta do Project Memory.
- Termine cada etapa com: **ETAPA <id> CONCLUÍDA — AGUARDANDO TESTE E APROVAÇÃO PARA A PRÓXIMA ETAPA.**
- Erro reportado → causa, arquivos corrigidos, e registro em "Bugs e soluções" abaixo.
- Marque suposições como **[SUPOSIÇÃO]** e mudanças de arquitetura/GDD como **[DECIDIR]** (nunca em silêncio).
- Ao fechar um módulo, **atualize este arquivo** (status, decisões, bugs, pendências).

## Regras de código (não negociáveis)

1. O servidor decide o estado; o cliente só envia intenção e renderiza.
2. Números em JSON ou config, nunca fixos no Java (nomes iguais aos do GDD).
3. Tudo testado em servidor dedicado com 2+ clientes.
4. Nenhuma classe de `client` referenciada em `common`; nenhum estado global mutável (serviços em `KN8Server`).
5. Todo [VERIFICAR] é confirmado no início do módulo que o usa (código-fonte do NeoForge/GeckoLib/PAL).
6. Dano sempre no servidor, no tick de impacto do JSON, nunca pela animação.
7. Mixins só com justificativa registrada aqui.
8. Payloads validados com rate limit (`C2SGuard`); sync só na mudança, com reenvio em login/respawn/dimensão.
9. JSON inválido gera log claro e nunca derruba o servidor.
10. Convenções da Fase 4 §2.5: sufixos `C2S`/`S2C`, chaves `kn8.<área>.<chave>`, 4 espaços, **120 colunas**,
    comentários explicam o porquê. Javadoc/comentários do código em português sem acento.
11. Compilar de primeira: imports completos, sem APIs inventadas.

---

## Decisões fixadas

| Área | Decisão |
|---|---|
| Sync público / privado | Público: `.sync()` nativo. Privado: NUNCA `.sync()` — payload manual ao dono via `NetworkSync` (PT1) |
| Dados de jogo | Reload listener + Codec (`DataRegistry`, `KN8Data`); registry de datapack só para worldgen/damage_type. Missões e desmonte também vão ao cliente desde a 0.2 (menu) |
| Menu (0.2) | `client/menu`: `DefenseForceScreen` + uma classe por aba (`MenuTab`), desenho por `fill` em `MenuStyle`; só lê o que o cliente já tem (PowerView, dados sincronizados, estatística vanilla `ENTITY_KILLED`, entidades por perto). Bestiário: registrado = já abatido (no criativo, tudo) |
| Animação de entidades | GeckoLib, 4 controllers (movement/action/reaction/overlay); nomes `<espécie>.<camada>.<nome>` |
| Animação do jogador | PAL camada `kn8:combat` prioridade 2000 via `AnimTriggerS2C` (com compensação de atraso) |
| Multipartes | `PartEntity`, IDs consecutivos; quantidade/nomes de partes fixos por entidade |
| Transformação | Método D (fantoche GeoReplacedEntity) — M12 |
| Fluxo de dano | `ActionTimeline` + raycast no servidor; impacto único por ação |
| Explosão em kaiju | Corpo inteiro ×1,0, uma vez por tick, sem atingir o núcleo |
| Hitbox de mob | `getDefaultDimensions` (em `LivingEntity` o `getDimensions` é final) |
| Alcance de kaiju | Sempre entre bordas (`edgeDistance`), nunca centro a centro |
| Config de servidor | Tipo SERVER, gerado em `<instância>/config/kn8-server.toml` |
| Protocolo de rede | `"14"` (subir ao mudar qualquer payload; 14 = registro sincronizado `locomotion`, 0.5.0-C; 13 = tecla de Release `ReleaseInputC2S` e `PowerView` novo, 0.5.0-A; 7 = `PowerView.winded`, 8 = `sounds` no JSON da arma, 9 = carreira, missões, bancada e invasão da 0.2, 10 = nível da invasão, 0.3; 11 = ataque especial `CombatAction.SPECIAL`, 0.5; 12 = `special.slash` da espada do Hoshina, 0.6-D) |
| Locomocao (0.5.0-C, v21 Prioridade 2) | `locomotion/<id>.json` (`LocomotionDef`, registro sincronizado; id = kaiju ou tipo de entidade; separado do `kaiju/*.json` porque o codec dele ja tem 16 campos). `Locomotion.drive` no controller `movement` (kaiju, soldado, Hoshina): velocidade da animacao = distancia andada no tick x duracao da volta (`Animation#length`, ticks) / `stride`, com min/max; `nextStep` a cada meia passada; `step_dust` nos grandes. `CreatureMovementProfile` (IA: escalar, fugir, prioridade de alvo) fica para a 0.5.0-F |
| Soldados especiais (0.6-D) | Perfil em `special_soldier/<id>.json` (`SpecialSoldierDef`: atributos, Release, `kaiju_damage`, aura, duas armas, técnicas `slash`/`combo`, `dash`, `counter`, `parry`). `HoshinaEntity extends SoldierEntity` (aliado, menu, alvos de kaiju iguais; `def()` vazio, sem soldier_1.json). Técnica: maior prioridade pronta e ao alcance (+`honju_priority` contra Honju); golpes no tick do JSON. Reações lidas do kaiju (`abilityTarget`, `ticksToImpact`, `isPreparingHeavy`): `heavy` → Kaeshi-uchi, outro → esquiva; parry no `hurt`. `SlashProjectile` (corte que atravessa e poupa aliados) serve o Hoshina e o especial `slash_wave` da espada dele para o jogador. Render: `SoldierRenderer<T>(context, espécie)`, segunda arma no osso `item_left` (com o display da mão direita: a GeckoLib não espelha como o vanilla). Animações: `tools/art/gen_hoshina_animations.py` (depois de `rig_soldier_mesh.py hoshina`) |
| Hoshina: força e onde aparece (Miguel, 2026-10-07) | No poder total vence o No. 10 pequeno (8,3) e perde para o gigante (9); vence o No. 9 atual sem facilidade; com o traje numerado 10 (0.6-F) fica mais forte. **Escalada de combate** (`escalation`): Release 40% + 2/s com alvo até +52 (teto 92%), velocidade e dano seguem o Release, recargas −40% no máximo. Só defensor nas invasões de nível 4–5 (variante `"hoshina"`). Espada **fabricada** na bancada (patente Vice-Capitão). No. 9 subiu para fortitude 8,0, regenera abaixo de 50% (2,5%/s; 5%/s abaixo de 20%) e foge com 15%. Duelos em `docs/BALANCEAMENTO.md` |
| No. 10 e Preondactyl (0.6-E) | No. 10: `KaijuNo10Entity` (as duas formas, `EntityType` proprios) + `No10Service`: regeneracao (`numbered/<id>.json`, mesma do No. 9), comando dos kaiju por perto, **forma gigante** pelo `transform` (Miguel: depois de um tempo de batalha, 60 s; ou abaixo de 50%): a gigante surge no lugar com a vida cheia e toma o lugar na invasao. Forma gigante com `overrides.health` 4.500. Preondactyl: `PreondactylEntity` (FlyingMoveControl; circula acima do alvo, mergulha, pousa sem alvo; frente blindada/costas fracas e autodestruicao no `flyer/<id>.json`, `FlyerDef`). Invasao **nivel 6** `no10_assault` (`chance` por kaiju da onda: No. 9 de surpresa; quem pode nao vir nao conta na barra), missao `no10_assault_defense`. Rig: `rig_primigenius_mesh.py kaiju_no10_small/giant`, `rig_preondactyl_mesh.py` |
| Hoshina: teto de Release (Miguel, 2026-10-07) | Forma normal no maximo **92%** (`max_release`, ja com o desespero); 100% so com o traje numerado 10 (0.6-F). Vida 460. Vence o No. 10 pequeno (5/5, com 4–45% da vida) e o No. 9 (63–75%); perde para a forma gigante (3/3) |
| Hoshina com a arma numerada 10 (0.6-F) | `HoshinaNo10Entity extends HoshinaEntity` (perfil `special_soldier/hoshina_no10.json`, variante `"hoshina_no10"`). `numbers10` no perfil: **cauda** independente no servidor (`tickExtra`: alvo = quem mira o Hoshina > quem esta atras/do lado > mais perto; a propria cauda corta, sem terceira espada: Miguel, 2026-10-08) e **guarda da cauda** (golpe `heavy` ou projetil de fora do arco da frente × `guard_factor`); **sincronizacao** = Release, ate 100% so neste traje; **Full Release** a 100% (dano, velocidade, recargas e cauda melhores) libera tecnicas com `requires_full_release` (Juni-hitoe: 12 golpes, ultimo forte, ignora armadura por `indirectMagic` [SUPOSICAO], expoe o nucleo, nao e interrompido). Rig: `rig_soldier_mesh.py hoshina_no10` (cauda por polilinha, 4 ossos), animacoes `gen_hoshina_animations.py hoshina_no10`. [DECIDIR] onde aparece: por ora so ovo/`/summon` |
| Release do jogador (0.5.0-A, Miguel 2026-10-08) | **So com traje** (`SuitEvents.worn`): sem traje a % e 0. Tecla **G** segurada sobe (`[power] raisePercentPerSecond` 2, Miguel), **Shift+G** desce (40); no maximo **20 acima do limite** (`maxOverLimit`; limite 40 -> ate 60%), dano por segundo cresce com o excesso (`overLimitDamagePerPoint`) e, ao voltar para dentro do limite, **fadiga** proporcional (Lentidao IV, Fraqueza, sem Release; `fatigue*`). Golpe comum de jogador em kaiju tira no maximo 15% da vida maxima (`[combat] maxHitFractionOfKaijuHealth`); so o especial da arma (`kn8:weapon_special`) passa disso. O traje impulsiona o corpo: forca e resistencia multiplicam com o Release, velocidade do corpo `ADD_MULTIPLIED_TOTAL`, agilidade x(1 + Release/100); `ReleaseInputC2S` so na mudanca, a % ativa (`PowerData.active`, volatil) muda no tick do servidor. O "treinado" virou o **limite pessoal**: talento sorteado uma vez (`[talent]`: comum 5-10%, raro 15-30%, chance 10% [SUPOSICAO]; quem ja tinha treinado mais fica com o treinado) e sobe por XP. **Acima do limite a % nao cai** (v21): calor +2/s a cada 10 de excesso e dano no corpo (sobrecarga/critico/maximo) **so enquanto acima do limite**; abaixo do limite nao aquece; na **forca total** (% igual ao limite pessoal, seja 40, 60 ou 100) aquece ate morno (~1 min, `useHeatPerSecond`; Miguel 2026-10-08). O PANIC antigo (Release 1%) saiu. Desespero com vida baixa so com traje. Atributos do corpo (`BodyStat`: forca, velocidade, resistencia, agilidade; `[body]`), treinados pelo uso, valem sem traje. Aura `defense_force` com `min_release` 1 (aparece ao subir) |
| Vida acima de 1024 (0.6-D) | O vanilla limita `max_health` a 1024 (No. 10 pequeno e gigante ficariam iguais): `AttributeLimits` troca o `maxValue` do `RangedAttribute` por reflexão no setup comum (100.000), sem mixin (regra 7; como o mod AttributeFix). GameTest `maxHealthGoesAbove1024` |
| Ataque especial de arma (0.5) | `special` no `weapon/<id>.json` (tipo, dano, área, custo, recarga, VFX, som), tecla **R**; resolvedor `SpecialAttacks` serve jogador e (depois) soldado especial. Machado = Golpe Sísmico (`ground_slam`) |
| Aura de poder (0.5, Miguel) | `aura/<id>.json` (cor, secundária, estilo `sparks`/`lightning`/`flame`, `min_release`, tamanho); id público `kn8:aura` (sync nativo) + `kn8:release_visual`; cada cliente desenha (`AuraRenderer`, sem pacote por tick). Jogador: aura do comando > do traje (`suit.aura`) > `defense_force`. Com vida baixa a % sobe sozinha (`[power] desperationHealth`/`desperationMaxPoints`). [DECIDIR] jogador escolher a cor |
| Habilidades de kaiju (0.6-A) | `behavior` no `ability/*.json` (min/max_range, prioridade, health_below, setor, golpes, projétil, lentidão); tipos novos `kn8:sweep`, `kn8:projectile` (`KaijuProjectile`), `kn8:leap`, `kn8:multi_hit` em `KaijuAbilities`; escolha = maior prioridade entre as prontas e ao alcance (empate sorteado); `particles` = aviso no início do preparo; `rage` no `kaiju/*.json`. Animações por `tools/art/gen_ability_animations.py` (rodar depois dos scripts de rig) |
| Empurrão de soldado em kaiju (0.6) | `[kaiju] soldierKnockback` 0 (o empurrão vanilla de cada golpe impedia o kaiju de chegar no grupo) |
| Trajes 3D (0.6-C) | Malha do Meshy presa às ModelParts do jogador (tronco, braços, pernas) por um `RenderLayer` próprio (`client/render/suit/SuitLayer`), não `GeoArmorRenderer` [DECIDIR aprovado em princípio → trocado por camada vanilla: segue a PAL e o modelo do jogador sem GeckoLib no jogador]. `tools/art/rig_suit_mesh.py <traje>`: divide, endireita os braços (PCA) e grava cada parte no espaço local da ModelPart (blocos, Y para baixo, X espelhado; gola em 0, pés em 1,5). Malha em `meshes/suit/<item>.json`, textura `textures/models/suit/<item>.png`; braço em primeira pessoa por `RenderArmEvent`. Armadura vanilla do Mk1/Mk1 Reforçado transparente (traje de treino sem modelo continua vanilla). **0.5.0-B (corpo inteiro, v21):** cada parte ganha uma "segunda pele" escura (`liner`: caixa do HumanoidModel + 0,3 px, pintada com o preto do traje) para a skin nunca aparecer pelos vaos do Meshy; Mk1 novo com `max_stretch` 2,4 e `arm_reach` (manga ate cobrir a mao). Sem cabeca (o asset nao tem) |
| Kaiju quebram o caminho ao andar (0.6, Miguel) | Todo kaiju (qualquer tamanho, passeando ou perseguindo) que bate em blocos tentando andar quebra a faixa da frente; preso dentro de blocos, quebra o que ocupa o corpo (`KaijuEntity.breakWhileWalking`). Força `[destruction] walkPowerYoju` 3, `walkPowerHonju` 3 (numerados também), `walkPowerDaikaiju` 4, intervalo `walkBreakCooldownTicks` 5; block entities, áreas protegidas e `mobGriefing` continuam valendo. Chaves novas (as antigas `pathPower*` eram 1/2/3 e só valiam para kaiju grande em combate) |
| Balanceamento (0.3, Miguel) | Kaiju mais fortes que soldados comuns (Trichonephila fortitude 3,5). Dano de soldado contra kaiju × `kaiju_damage` do `soldier_1.json` (baixo 0,25, normal 0,35, alto 0,8, elite 1,0), no corpo (sem núcleo, [SUPOSIÇÃO]): comuns precisam de grupo, alto/elite resolvem (medido: aranha 12 s com 1 normal, 3,4 s com 4, 2,4 s com 1 elite). Patente de armas/trajes só nos `unlocks` das patentes. Recompensa de invasão proporcional à contribuição (dano/abates; `[invasion] rewardMinFactor`/`rewardMaxFactor`); sem dano, sem recompensa. Tudo em `docs/BALANCEAMENTO.md` |
| Soldados comuns (0.4) | Variantes no `soldier_1.json`: `{weapon, sidearm, weight}` (rifle/pistola com faca de apoio, espada, faca; recruta peso 0). Arma de apoio na mão secundária: troca quando o kaiju está a menos de `sidearm_distance` (3,5). Sem variante pedida (ovo, `/summon` sem NBT, defensor `"random"`), sorteio por peso. **Soldado comum nunca usa arma especial nem traje numerado; o machado é de um soldado especial** |
| Invulnerabilidade de kaiju (0.3) | Golpe de quem ataca (com entidade, sem ser explosão) zera a invulnerabilidade vanilla antes de aplicar: o combate do mod já acerta uma vez por ação. Fogo/lava/queda mantêm. Ataques (`MeleeRaycast`) ignoram carcaças |
| Níveis de invasão (0.3) | `level` 1–5 no `invasion/*.json` (barra e aba Alertas). Nível 4 `kaiju_horde` (20 kaiju, Honju chefe); nível 5 `mass_resurrection` (horda + onda `mass_revive`: o No. 9 revive todas as carcaças da área da invasão, uma a cada `mass_revive_interval_ticks`; `revive_boss` faz o Honju voltar como chefe `revived_honju`). Só por missão/comando (`natural_weight` 0) |
| Carreira (0.2) | `CareerData` (attachment salvo, copiado na morte) → `CareerView` privado (`CareerSyncS2C`). Patente por mérito + missão de avaliação; a patente dá vida, esquadrão e `unlocks` (receitas/armas), não teto de Release |
| Bancada (0.2) | Receitas em `data/kn8/kn8/workbench/*.json` (sincronizadas); `CraftC2S` → servidor confere bancada a 6 blocos, patente e materiais. Trajes: armadura/resistência a calor do `suit/<id do item>.json` via `ItemAttributeModifierEvent` |
| Invasões (0.2) | Uma por dimensão, no `KN8Server` (não salva); ondas do `invasion/*.json`; `InvasionStateS2C` público só na mudança; kaiju só por alertas/invasões/missões (`spawn.natural` desligado) |
| Numerados (0.2) | `KaijuNo9Entity extends KaijuEntity` (mesmo `EntityType<KaijuEntity>`, fábrica própria); comportamento extra em `No9Service`, números em `numbered/<id>.json` |
| Construções (0.2) | Templates `.nbt` gerados por `tools/world/gen_structures.py` (escritor NBT próprio, sem nbtlib) + worldgen jigsaw de 1 peça (`worldgen/structure`, `template_pool`, `structure_set`) e baús com `loot_table/chests/*` |
| Sons de arma (0.2) | No JSON da arma: `"sounds": {"swing", "heavy", "hit", "shot"}` → id de som (qualquer id, até de resource pack); sem a chave, som genérico. `CombatService.weaponSound` serve jogador e soldado |
| Ataque "heavy" de kaiju | [SUPOSIÇÃO] atravessa o bloqueio comum; só parry/esquiva evitam (`combat.heavyIgnoresBlock`) |
| Pacotes de entrega | Nunca incluir `build.gradle`/`gradle.properties` (os do `kn8-main` são os corretos: PAL `transitive=false`, run `clientJoin`, repo Modrinth, bloco do Better Combat). Limite de envio de 30 MB: desde a 0.6-E o pacote vai em 3 partes (malhas, código/dados, texturas), todas extraídas na raiz do projeto |
| Modelos Meshy | Armas: OBJ (`neoforge:obj`) na mão; personagens: malha presa aos ossos GeckoLib (`client/render/mesh`: `meshes/<especie>.json` + um OBJ por osso; .geo.json só com ossos; mesma textura/render type do modelo). Conversor `tools/art/meshy_convert.py`; rigging: `rig_trichonephila_mesh.py`, `rig_soldier_mesh.py`, `rig_primigenius_mesh.py` (divisão pela forma; mesmos nomes de ossos, então as animações continuam) |
| Arma na mão (0.1-B, teste) | Lâmina: display de espada vanilla. Arma de fogo: cano ao longo do braço (`thirdperson` rotação `[0, 90, 0]`, `hand_grip` em `meshy_assets.json`), jogador em `CROSSBOW_HOLD` (`HeldWeaponPoses`). Soldado: mesma cadeia do vanilla em Y para cima (só `Rx(-90)`), item alinhado ombro→mão. Conferir com `tools/art/preview_held_items.py` |
| Tamanho da arma na mão (0.2) | Jogador: display ×1,3 (3ª pessoa) e ×1,2 (1ª) por padrão, faca [1,5; 1,3], machado [1,1; 1,0] (`held_scale` em `meshy_assets.json`), punho parado na mão (`blade_transform`/`gun_transform`). Refazer só o display: `meshy_convert.py --display-only`. Soldado: `SoldierRenderer` divide pela escala do display e volta a 0,85 (tamanho de antes) |
| Texturas de malha Meshy | Sem mipmap no Minecraft: textura proporcional ao modelo (soldado 512) com borda nas ilhas (`pad_texture.py`); UV quebrada pela redução → `rebake_mesh_texture.py` |
| Pivô na GeckoLib | O X do pivô do .geo.json é invertido ao carregar: gravar `-x` (confirmado no código da GeckoLib) |
| Escala de kaiju (aprovada) | Yoju 4–8, Honju 6–15, Daikaiju 20–30 blocos (1 bloco ≈ 1 m) — aplicar na Etapa C |
| Guia de arte | 16 px/bloco; até 20 ossos (pequeno/médio), 35 (grande), 50 (chefe); texturas 64 (pequenos), 128/256 (grandes) |

## Status dos módulos

| Módulo | Status |
|---|---|
| M1 projeto base · M2 config · M3 rede · M4 dados JSON | ✅ aprovados |
| M5 atributos (R, stamina, calor, pane, energia, treino) | ✅ |
| M6 HUD | ✅ |
| M7a framework de kaiju · M7b multipartes e núcleo | ✅ |
| M8 Trichonephila (habilidades do JSON, spawn natural, bando) | ✅ |
| M9 animação (AnimationBridge, PAL, AnimTriggerS2C) | ✅ |
| M10a combate corpo a corpo (faca, combo, bloqueio, esquiva) | ✅ |
| M10b rifle, parry, crítico, CombatStateS2C/HUD, clamp de stamina, arte embutida | ✅ |
| M11a habilidades `area_melee`/`charge`, núcleo na cabeça da Trichonephila, GameTests do M10b | ⏳ aguardando teste |
| Etapa A pipeline Meshy (armas OBJ na mão, pistola, espada, animação por arma) · Etapa B HUD | ⏳ aguardando teste |
| **0.2 Modelos v2** (soldado 7k, Primigenius Yoju branco, ressurgido verde, Honju marrom de chifres, revivido roxo; rig por esqueleto; buracos tampados; aranha sem atravessar) | ✅ compila, JUnit 70/70, GameTests 31/31; vistos em jogo com 2 clientes (`docs/img/modelos_v2_*`, `soldado_v2_jogo.png`); aguardando roteiro §14 |
| **0.2 Etapa 1: sons + corrida** (17 eventos `KN8Sounds`, 41 `.ogg` sintetizados por `tools/audio/gen_sounds.py`; voz do kaiju mais grave quanto maior; corrida gasta stamina com "sem fôlego"; carcaça tomba ao morrer, hitbox deitada) | ✅ compila, JUnit 70/70, GameTests 31/31; **roteiro §13 inteiro passou na nuvem com 2 clientes** (som gravado em WAV + legendas); passos aprovados pelo Miguel; **rugido, rosnado, dano e morte refeitos** (voz com formantes, 36–50 Hz) e **som próprio para faca, espada e machado** (leve, pesado, acerto), vistos em jogo pelas legendas; aguardando o Miguel ouvir (`previa_sons_kaiju_e_armas.mp3`) |
| **0.2 Menu da Força de Defesa** (tecla M; abas Perfil, Missões, Alertas, Esquadrão, Bestiário, Arsenal; Q/E troca) | ✅ compila, testes passam, todas as abas vistas em jogo (pt_br e en_us); partes de etapas futuras com "Em breve (Etapa N)"; aguardando roteiro §12 |
| **0.2 Machado** (`kn8:axe`, estilo `heavy`, 2,0 de comprimento) e **espada nova** do Meshy | ✅ jogador (1ª/3ª pessoa) e soldado (`/kn8 soldier spawn axe`/`sword`) vistos em jogo; arma pesada também carrega golpe; números [SUPOSIÇÃO] em `weapon/axe.json` |
| **0.2 HUD nova** (fiel à referência "HUD de combate avançado - estilo anime"; 60% do tamanho da arte, `BASE_SIZE`) | ✅ vista em jogo; arte em `textures/gui/hud/` gerada por `tools/art/gen_hud.py` (texturas 4× desenhadas em pixel de textura, `blur` ligado); aguardando aprovação |
| **Modelos Meshy estilo Minecraft** (soldado, aranha 8 patas, Primigenius) | ⏳ compila, JUnit 69/69, GameTests 31/31 e **vistos em jogo** (servidor dedicado + 1 cliente na nuvem, capturas em `docs/img/jogo_*`); aguardando roteiro §11 com 2 clientes |
| **0.1-B correções visuais** (aranha, soldado, armas na mão, espada nova) | ⏳ compila e testes passam; armas na mão do jogador (1ª/3ª pessoa) e do soldado e mira do soldado vistas em jogo; **aguardando roteiro §10** |
| **0.1-B** (atualização grande, escrita no chat): escala, VFX, destruição, carcaça/desmonte, Primigenius verde e Honju marrom, barra de vida, dash/ataque carregado, Soldado 1 | ⏳ compila, JUnit (69) e GameTests (31/31) passam em 2026-10-06; **aguardando teste manual** — `docs/ROTEIRO_TESTE_0_1_B.md` (resultado e correções em `docs/ATUALIZACAO_0_1_B.md`) |
| **0.2 Etapas 2, 5 e 6** (carreira: patente por mérito + exame, Release por treino até 100%, boneco de treino; missões com rastreador; chefes com fases e invocação da própria espécie) | ✅ compila, GameTests; boneco, aba Missões e rastreador vistos em jogo; aguardando roteiro §15 |
| **0.2 Etapa 3** (bancada da Força de Defesa, trajes vestíveis training_suit/mk1/mk1_reinforced, resfriador/estimulante/catalisador) | ✅ fabricação e traje Mk1 vistos em jogo; aguardando roteiro §16 |
| **0.2 Etapa 7** (alertas de invasão: sirene, ondas, defensores, barra, recompensa; `/kn8 invasion`; missão Defesa da Cidade) | ✅ vista em jogo com 2 clientes (vitória e recompensa); aguardando roteiro §17 |
| **0.2 Etapa 8** (Kaiju No. 9: revive carcaças, comanda kaiju, foge; invasão `no9_resurrection`, missão Ameaça Revivida) | ✅ reviver e fuga vistos em jogo; aguardando roteiro §18 |
| **0.2 Construções** (posto avançado, prédio destruído, restos de kaiju, torre de vigia; worldgen + baús) | ✅ vistas com `/place structure`; GameTests 41/41; aguardando roteiro §19 (mundo novo) |
| **0.3 Níveis de invasão** (ideia do Miguel: horda de 20 com Honju; No. 9 revivendo o exército inteiro) | ✅ GameTests 42/42; nível 5 completo visto em jogo com 2 clientes (26 revividos, 7,6 ms/tick); aguardando roteiro §20 |
| **0.4 Soldados comuns** (variantes, faca de apoio, sorteio, papel no menu, sem machado) | ✅ GameTests 45/45; troca para a faca e aba Esquadrão vistas em jogo com 2 clientes; aguardando roteiro §21 |
| **0.5 Ataque especial + aura** (Golpe Sísmico do machado na tecla R; aura por personagem, violeta com raios pela referência do Miguel; potência com vida baixa) | ✅ GameTests 49/49; aguardando teste em jogo do Miguel |
| **0.6-A Ataques novos dos kaiju** (casco, rabada, soco pesado, raio de energia do Honju, estocada/varredura/várias patas/teia/salto da aranha, Finger Gun do No. 9; fúria dos revividos; soldado sem empurrar kaiju; kaiju quebram o caminho ao andar) | ✅ **aprovada pelo Miguel (2026-10-07)**; GameTests 56/56. Plano 0.6 em `docs/PLANO_0_6.md`, especificação em `docs/ESPECIFICACAO_HABILIDADES_MOBS.md` |
| **0.6-B Trichonephila Honju** (Tecedeira Abissal: entidade, partes e núcleo, ataques da aranha + explosão de teia, fúria, chefe que invoca Trichonephila, desmonte de 6 etapas, invasão nível 3 `web_queen`, missão `web_queen_hunt`) | ✅ GameTests 57/57; vista em jogo (`docs/img/trichonephila_honju_*`: modelo, teia e salto); aguardando o Miguel |
| **0.6-E No. 10 e Preondactyl** (No. 10 pequeno e gigante do Miguel com regeneracao, forma gigante por tempo de batalha ou vida, furia, 12 golpes novos; Preondactyl voador com raio, mergulho, couraca e autodestruicao; invasao nivel 6 com o No. 9 de surpresa; missao) | ✅ GameTests 72/72; voo do Preondactyl e duelos com o Hoshina medidos no servidor dedicado; aguardando o Miguel (roteiro §23) |
| **0.6-D Hoshina** (soldado especial: rig do modelo do Miguel com bainhas no tronco, duas espadas, aura roxa, Kūuchi, Kōsa-uchi, Ran-uchi, Kasumi-uchi, Yae-uchi, esquiva, Kaeshi-uchi, parry; espada do Hoshina para o jogador com Kūuchi na tecla R) | ✅ GameTests 64/64; visto em jogo (`docs/img/hoshina_*`: modelo, luta contra Honju, Kūuchi do jogador); aguardando o Miguel (roteiro §22) |
| **0.6-F Hoshina + arma numerada 10** (modelo do Miguel com cauda, cauda que corta e defende sozinha, sincronizacao ate 100%, Full Release, Juni-hitoe) | ✅ GameTests 76/76; modelo e cauda cortando vistos em jogo (`docs/img/hoshina_no10_*`); vence o No. 10 gigante sem facilidade; aguardando o Miguel (roteiro §24) |
| **0.5.0-A Release e corpo do jogador** (Biblioteca v21 Prioridade 1: tecla G, traje obrigatorio, limite pessoal sorteado, excesso sem baixar a %, atributos do corpo) | ✅ JUnit e GameTests 80/80; visto em jogo (sem traje avisa; com traje sobe com aura; 96% com limite 19 tirou 11 de vida em 12 s sem baixar a %; Shift+G desce) — `docs/img/release_0_5_0_tecla.png`; aguardando o Miguel (roteiro §25) |
| **0.5.0-B Corpo inteiro + modelos refeitos** (Mk1 novo do Miguel no jogador, segunda pele escura nos dois trajes, soldado e Hoshina refeitos) | ✅ GameTests 81/81; vistos em jogo de frente, de costas, andando, em 1a pessoa e em luta (`docs/img/mk1_novo_corpo_inteiro.png`, `mk1_reforcado_corpo_inteiro.png`, `soldado_hoshina_refeitos.png`). Depois trocados pelo **Mk1 e soldado estilo Minecraft** do Miguel (`mk1_e_soldado_estilo_minecraft.png`; cortes remedidos, Mk1 sem esticar braco); aguardando o Miguel (roteiro §26) |
| **0.5.0-C No. 10 refeito + locomocao** (No. 10 pequeno e gigante do Miguel com rig novo: cauda enrolada em 4 pedacos por base/fundo/curva/ponta, cabeca centrada; perfis de passada por criatura) | ✅ JUnit e GameTests 83/83; vistos em jogo (`docs/img/no10_refeito_pequeno_gigante.png`), forma pequena lutando e gigante andando; aguardando o Miguel (roteiro §27) |
| **0.6-C Trajes 3D** (Mk1 e Mk1 Reforçado do Miguel no jogador: tronco, braços e pernas presos ao modelo, braço em 1ª pessoa) | ✅ build e GameTests; vistos em jogo de frente, de costas, andando e em 1ª pessoa (`docs/img/trajes_3d_*`); aguardando o Miguel. Soldados continuam com o próprio modelo (já vestem o uniforme) |
| M11b carcaças e desmonte · M12 transformação · M13 NPCs | pendentes (patentes/crafting do M14 entraram na 0.2) |
| M15 missões · M16 chefe Honju · M17 Tachikawa · M18 endurecimento/performance | pendentes |

## Bugs e soluções (resumo)

- **0.2 (super atualização):** `ResourceLocation` como argumento de `Component.translatable` derruba o comando
  ("arguments must be Component, Number, Boolean or String") → passar `id.toString()`. Barra do kaiju cobria a barra
  de chefe/invasão → desce para baixo das barras (`CustomizeGuiOverlayEvent.BossEventProgress`). GameTest de
  invasão: sem jogador os chunks a 36–56 blocos descarregam (kaiju some de `level.getEntity`) → chunks forçados e
  segunda passada de abate. Receitas/invasões/numerados precisam de `publish` na `DataValidation` (senão o
  servidor fica sem eles). `pgrep -f`/`pkill -f` com o nome do processo casa com o próprio shell (mata a sessão):
  matar pelo PID. Ruína preta: regra de pilar no eixo errado deixava a fachada inteira de concreto cinza-escuro.
  Teste dos roteiros (2026-10-07): promoção pelo `promote_to` da missão não mostrava "PROMOVIDO" →
  `CareerService.promote`; rastreador de missão ficava sob a barra do kaiju → desce para baixo dela; restos de
  kaiju enterrados em morro (sem adaptação de terreno) → `beard_thin` e só biomas planos. `/reload` não recarrega
  estruturas de worldgen (reiniciar o servidor).
- **0.3 (balanceamento, duelo soldados × kaiju):** 4 soldados matavam quase no mesmo tempo que 1 → invulnerabilidade
  vanilla de 10 ticks engolia os golpes dos outros (valia para 2 jogadores também) → zerada para golpes de quem
  ataca. Kaiju vivo em cima de carcaça não levava tiro (o raycast parava na carcaça) → `MeleeRaycast` ignora
  carcaças. GameTest `twoAttackersInTheSameTickBothHit`.
- **0.3 (níveis de invasão):** soldados de defesa **elite** matam uma Trichonephila (28 de vida) em menos de 1 s e
  esvaziavam as ondas sozinhos → defensores dos níveis 4–5 em nível normal/alto (balanceamento da aranha e do dano
  dos soldados fica para a tabela). Carcaças ficam espalhadas pelo anel de chegada e o No. 9 chega por um lado só →
  a ressurreição em massa procura na área inteira da invasão, não só em volta dele.

- **0.5.0-C (No. 10 refeito):** cauda enrolada (desce, abre e sobe) cortada so por base -> meio -> ponta deixava
  a volta de fora sem dono → junta opcional `tail_bend` no `rig_primigenius_mesh.py`; metade de baixo do rosto ia
  para o tronco (espinhos mais perto pela superficie) → `head_cylinder` (acima do queixo e perto do eixo = cabeca).
- **0.6-E (Miguel, cabeca do No. 10):** nas duas formas a cabeca ficava deslocada para o lado (rosto ~0,36 m fora
  do eixo na pequena, ~0,9 m na gigante; o giro de 65 graus do `fix_head_yaw.py` nao a trouxe de volta ao meio) →
  `head_shift` no `rig_primigenius_mesh.py` (desloca cabeca/mandibula e, aos poucos, o pescoco; ombros ficam).
  Preondactyl tinha pescoco e cabeca desviados ~20 graus → giro rigido `HEAD_YAW_DEG` no `rig_preondactyl_mesh.py`.
- **0.5.0-A (GameTests):** o `no9MassRevivesEverything` (lote seguinte) falhava quando um teste do lote padrao deixava
  coisas no mundo: os lotes reaproveitam as mesmas posicoes e a ressurreicao em massa pega carcaca de outro teste no
  raio do No. 9 → teste que mata kaiju apaga carcacas e kaiju no fim. Um laco de 1.200 ticks de `PowerService.tick`
  tambem o derrubava (causa nao achada); o "1 minuto ate morno" ficou no JUnit.
- **0.5.0-B (modelos refeitos):** soldado novo: lateral do capacete virava braco (capacete mais largo que o pescoco) →
  `arm_max_y`. Hoshina novo veio com a malha toda soldada (bainhas encostadas no corpo, um componente so) e o
  `back_items` por componente nao achava nada → regiao `{"x_max", "z_min"}` atras do braco esquerdo, e as bainhas
  ficam no tronco inteiras (mesmo abaixo do quadril). Mk1 novo: cortes ainda eram do Mk1 antigo (metade do braco ia
  para o tronco) e o braco fino nao chegava na caixa nem com 1,8x → medidas novas, `max_stretch` 2,4, manga
  esticada ate a mao e segunda pele (a calca roxa da skin aparecia pelos vaos da coxa). Scripts de rig gravam LF:
  `fixcrlf.sh` (scratchpad) devolve o CRLF dos arquivos que ja eram CRLF.
- **0.6-F (rig do Hoshina com cauda):** a cauda enrolada passa por cima da cabeca e era rotulada como cabeca/tronco
  pelos cortes de altura → cauda por polilinha (`tail` + Dijkstra contra sementes do corpo) e caixa explicita da
  cabeca (`head_box`); cabeca virava tronco (`neck_y` alto: 1,31) e maos viravam perna (`hand_min_y` 0,60).
- **0.6-E:** o `build_primigenius.py` regrava as animacoes dos Primigenius sem os golpes da 0.6-A: rodar o
  `gen_ability_animations.py` depois (e conferir CRLF). GameTest do No. 10: a forma gigante quebra blocos ao surgir
  e a fila de destruicao continuava depois do teste, quebrando o lote da ressurreicao em massa → teste com
  `mobGriefing` desligado. Preondactyl "parado": o raio matava o soldado-alvo (24 de vida) logo no primeiro tiro;
  area de GameTest tem teto de barreira de 7 blocos (o teste so confere que decolou).
- **0.6-D (Hoshina):** bainhas das costas iam para o braço/cabeça no corte por forma → peça solta (componente
  conexo) atrás de `z_min` fica no tronco (`back_items` em `rig_soldier_mesh.py`). Combos de 1–2 ticks perdiam
  golpes pela invulnerabilidade vanilla → `invulnerableTime = 0` antes de cada golpe da técnica e de cada corte.
  Espada da mão esquerda apontando para baixo → display da mão direita também no `item_left`. Duelo de calibragem:
  kaiju de fortitude 8,3 e 9 nasciam com a mesma vida (teto vanilla 1024) → `AttributeLimits`. Kaeshi-uchi às vezes
  errava (o dash lateral deixava o Hoshina longe) → o contra-golpe vem com avanço e +3 de alcance.

- **0.6-C (trajes):** o traje do Meshy é mais fino que o boneco do Minecraft (braço 0,2 contra 0,25 + manga da
  skin) e a skin cobria o traje; centrar a peça inteira deixava a canela atrás da calça (a bota puxa o centro) →
  `fit_box` fatia por fatia: centra cada fatia na caixa do jogador e alarga até a caixa da armadura vanilla
  (tronco 10×6 px, braços 6×6, pernas 5×5), limite 1,8×, suavizado. Captura de tela com `import -crop` guarda o
  deslocamento: usar `+repage` antes de recortar. `/summon` não aceita rotação (usar `data merge` com `Rotation`).
- **0.6 (Miguel):** kaiju presos em construções → força de andar do Yoju era 1 (só frágeis: vidro, folhas) e do
  Honju 2 (madeira/terra), e só kaiju grande em combate abria caminho → todo kaiju, força 3 (pedra, tijolo,
  concreto), também preso dentro de blocos. GameTest `yojuBreaksAStoneBrickWallWhileWalking`.
- **0.6-A:** combo de vários golpes engolido pela invulnerabilidade vanilla (10 ticks) → `invulnerableTime = 0` antes
  de cada golpe do `multi_hit`. Salto caía a meio caminho (a parábola ignorava o freio do ar: ×0,91 horizontal,
  ×0,98 vertical) → velocidades calculadas com o freio (`AbilitySelection.leap*Speed`). Mob com IA desligada não
  anda pela velocidade (GameTest de salto precisa da IA). Rabada escolhida com o alvo na frente → a escolha confere o
  setor. Teste em jogo: kaiju a 28 blocos não via o jogador (alcance de detecção 16 + 8 × inteligência).
- **PT1:** `syncInitialAttachments` ignora `sendToPlayer` → dados privados sem `.sync()`.
- **PT6:** tecla L conflitava com Conquistas → esquiva em **Z**.
- **PT7:** GameTest de raycast parava na barreira do template 3×3 → template `empty_9x7x9`.
- **M7a:** `LivingEntity#getDimensions` é final → usar `getDefaultDimensions`.
- **M7b:** núcleo zerado não matava por causa da armadura → depois do golpe aceito, `setHealth(0)` + `die(source)`.
  GameTests de dano usam fonte que passa pela armadura (`mobAttack`), nunca `generic`.
- **M8:** kaiju parava a ~2,4 blocos e não mordia → aproximação direta (`MoveControl`) + alcance entre bordas.
- **M8:** spawn natural denso e de dia → biomas da Fase 4 §8.3, peso 2, `spawn.surfaceOnly`/`requireDarkness`.
- **M10a:** stamina 130/100 depois de reduzir a % → `PowerService.changed()` limita ao máximo (M10b).
- **Testes com jogador:** usar `FakePlayerFactory.get(level, profile)` (é invulnerável: só como atacante/estado).
  O mesmo perfil devolve a **mesma instância** e os GameTests do lote rodam em paralelo: um perfil por teste.
- **0.1-B (GameTests):** `runAfterDelay` dentro de `runAfterDelay` pode rodar o bloco de fora duas vezes (o mapa de
  tarefas é alterado durante a iteração) → agendar todos os passos no nível de cima do teste, com atraso absoluto.
- **0.1-B (aranha "bugada"):** a redução 10k→6k triângulos do `meshy_convert.py` dava a cada vértice a UV do
  vértice original mais próximo; nas costuras, 2.900 triângulos ficaram com cantos em ilhas diferentes (linhas
  douradas). Textura refeita por triângulo (`rebake_mesh_texture.py`) e `decimate` corrigido (UV por triângulo).
- **0.1-B (soldado/armas):** armas de fogo usavam o display de espada vanilla (`[0,-90,55]`) com o modelo deitado →
  apontavam para o chão; o `SoldierRenderer` girava o item 180° a mais (cadeia do vanilla copiada sem converter para
  Y para cima) e a `BlockAndItemGeoLayer` gira de novo pela rotação do osso. Soldado parecia parado: idle de ±2°
  e sem pose de arma → controllers `movement` (pernas) + `arms` (pose por arma, mira com alvo) + cabeça seguindo.
- **0.1-B (espada):** o modelo do Meshy saiu como agulha → modelo próprio por código (`build_sword.py`); em
  2026-10-06 trocado pelo novo GLB do Miguel (`blade_thickness` alarga a lâmina também; não usar nele).
- **Modelos Meshy (em jogo, 2026-10-06):** `/summon kn8:soldier ~ ~ ~ {kn8_variant:...}` deixava a mão vazia (o
  vanilla não chama `finalizeSpawn` quando há NBT) → `readAdditionalSaveData` aplica arma/atributos se o NBT não
  traz `HandItems`/`attributes`. Clarão do tiro (`ParticleTypes.FLASH`) era uma bola branca de vários blocos →
  chamas pequenas no cano.
- **Teste com 2 clientes (2026-10-06):** segurar o clique direito no desmonte virava ataque carregado quando a
  carcaça sumia (última etapa) → `CombatInput.useStartedOnCarcass` até soltar o botão.
- **Compilar na nuvem:** os arquivos de build não estão no repositório; a sessão monta um `build.gradle` provisório
  (só local, em `.git/info/exclude`) com as versões desta página e roda servidor + cliente em Xvfb com RCON.
- **Meshy (redução):** o GLB separa vértices nas costuras de UV e a redução abria buracos → `decimate` solda pela
  posição antes de reduzir. **Rig dos Honju:** dedos abaixo do corte de altura viravam perna e as costas atrás do
  ombro viravam braço (pedaços soltos no slam) → `arm_front_z` e `arm_back_z` por espécie.
- **0.2 (teste do §13):** carcaça do `primigenius` com hitbox 2×2 só no cliente (id padrão do synched data igual à
  espécie → `onSyncedDataUpdated` não roda) → `refreshDimensions()` no primeiro tick. Corpo tombado fora da hitbox em
  pé → hitbox da carcaça é a do corpo deitado e o renderer centra o corpo nela. Dash baixo demais → som refeito.
- **Modelos v2 (2026-10-06):** os dois Honju vieram girados ~35° em Y (achado pela simetria; `yaw_deg`); modelos
  de cauda longa ficavam à frente da hitbox (`recenter_feet`); corte por plano soltava garras/mãos em poses
  agachadas → **rig por esqueleto** (`split_skeleton`: juntas medidas, Dijkstra pela superfície da malha soldada).
  Soldado novo: Meshy deixa aberta a lateral do quadril sob o braço (buraco ao mirar) → `cap_holes` tampa todo
  contorno aberto de cada osso (soldado: preto do macacão; kaiju/aranha: cor da borda); pedaços isolados vão para
  o osso vizinho (`absorb_fragments`). Torso novo engolia o núcleo nos GameTests de mira → núcleo na pele do peito.
  Aranha "atravessando": patas vizinhas giravam ±14° em oposição e subiam por posição (base entrando no corpo) →
  balanço 7°, subida por rotação em Z, `CORE_HALF_WIDTH` 0,5.
- **Testar som na nuvem:** `ALSOFT_DRIVERS=wave` + `ALSOFT_CONF` com `[wave] file=...wav` no cliente grava a
  mixagem (float 32, 48 kHz, estéreo); desligar música/ambiente em `options.txt` e ligar `showSubtitles`.
- **0.1-B (compilação):** único erro foi um import (`LinkedHashMap` em `DataValidation`); as APIs "a conferir" existem.

## Arte

- **Pipeline:** `tools/art/kaiju_art.py` (base comum: cubos, ossos, pintores de textura por paleta, atlas, validação)
  e um script por espécie. Variantes de cor ("ressurgido") compartilham o esqueleto e trocam só a textura.
  Prévia: `python3 tools/art/preview.py <espécie> [saída.png] [escala]`.
- `tools/gen_kaiju_placeholders.py` **pula** as espécies em `FINAL_ART` (não sobrescreve arte final).
- Animações do jogador: `tools/art/gen_player_animations.py` (tempos lidos do JSON das armas).
- Armas: `tools/art/meshy_convert.py` (o antigo `gen_item_textures.py` foi removido). Espada: modelo novo do Meshy
  (`sword.glb`, 2026-10-06); `build_sword.py` fica só como alternativa (para usar, pôr `replaced_by` na tabela). Prévia das armas na mão (jogador 3ª/1ª pessoa e soldado):
  `python3 tools/art/preview_held_items.py` → `build/preview_held_items_*.png`.
- Malhas: `rebake_mesh_texture.py` (refaz textura de malha com UV quebrada), `pad_texture.py` (borda nas ilhas +
  reduzir); `rig_soldier_mesh.py` já gera a textura 512 com borda.

| Kaiju (id) | Categoria | Arte | No jogo |
|---|---|---|---|
| `trichonephila` | Yoju | malha do Meshy estilo Minecraft (6.850 tri, 8 patas, 5,5 m de envergadura), hitbox 3,4 × 1,8 | ⏳ 2026-10-06 |
| `primigenius` | Yoju | malha do Meshy estilo Minecraft (8.276 tri, 6 m, com cauda), ossos do modelo de cubos | ⏳ 2026-10-06 |
| `primigenius_resurrected` | Yoju ressurgido | malha do Meshy (verde, 10 mil tri), 6 de altura | ⏳ 2026-10-06, visto em jogo |
| `primigenius_honju` | Honju | malha do Meshy (marrom, chifres, 12 mil tri), 9 de altura | ⏳ 2026-10-06, visto em jogo |
| `primigenius_revived` | Honju ressurgido | malha do Meshy (roxo, chifres, 12 mil tri), hitbox 5,73 × 9,0 | ⏳ 2026-10-06, visto em jogo |
| `trichonephila_honju` | Honju | malha do Meshy do Miguel (Tecedeira Abissal: roxa e amarela, rosto humanoide, 11.436 tri, 8 m), rig por caminhos na superfície (`rig_trichonephila_mesh.py trichonephila_honju`: pontas das patas e joelho medidos), hitbox 6 × 4 | 0.6-B |

Regra de design do Miguel: Honju e Yoju são **criaturas diferentes** (modelo e textura próprios). Desde
2026-10-06 as versões ressurgida/revivida também ganham **modelo próprio no Meshy** (antes: só outra paleta).

Modelos do Meshy (GLB com o id como nome, pasta fora do Git; tabela `tools/art/meshy_assets.json`):
`trichonephila` (~4,5 m de comprimento, 8 patas), `primigenius` e `primigenius_resurrected` (6 m de altura,
largura ~3,1), `primigenius_honju` e `primigenius_revived` (9 m, largura ~5,7), `soldier` + `soldier_parts`
(1,9 m), armas `rifle`, `pistol`, `combat_knife`, `sword`, `axe` (no jogo), `twin_swords_sheathed`, `single_sheath`,
`twin_swords`.
Os 4 Primigenius estão com malha do Meshy (`rig_primigenius_mesh.py <espécie>`, tabela `SPECIES` com os cortes de
cada um: cauda, cabeça/mandíbula, chifres, braço/antebraço com `arm_front_z`/`arm_back_z`, pernas). Áreas de acerto
remedidas na malha [SUPOSIÇÃO: núcleo no peito]. `build_primigenius.py` não sobrescreve espécie que já tem malha (só regera as animações).

## Pendências e [DECIDIR]

- **Versao 0.5.0 (Miguel, 2026-10-08):** a Biblioteca v21 (`docs/BIBLIOTECA_KAIJU_N8_v21.md`) e a atualizacao
  **0.5.0** do mod; seguir as prioridades **na ordem**, uma etapa por vez (0.5.0-A, -B...). A 1.0.0 vem depois de
  corrigir tudo da 0.5.0. Auditoria (Prioridade 0), conflitos [DECIDIR] e plano: `docs/AUDITORIA_BIBLIOTECA_V21.md`.

- **Feito (Miguel, 2026-10-08):** calor so na forca total do traje (% = limite) ou acima dele. Novos modelos (forma preta do No. 9 [nova forma, nao substitui o atual], formiga, formiga revivida, No. 9 +
  formiga, Diclonius, fusao No. 9 + No. 10) so guardados: `docs/MODELOS_RECEBIDOS_2026_10_08.md`.
- **Modelos recebidos (Miguel, 2026-10-08), so guardados:** soldado, Mk1, No. 10 pequeno e gigante refeitos
  (substituem os atuais; **soldado, Hoshina e Mk1 ja trocados na 0.5.0-B**); novos Kaiju No. 8 (2 m), larva, Kafka, Kikoru (normal e Numbers 4 com asas), Reno
  (normal e Numbers 6), Mina (com o canhao), Gen Narumi (com a baioneta) e o Hoshina normal refeito (substitui). **Nao implementar ate o Miguel mandar o documento e pedir.**
  Lista e analise em `docs/MODELOS_RECEBIDOS_2026_10_08.md`.

- **Decidido (Miguel, 2026-10-07):** o No. 9 é o vilão principal e é forte de propósito (fortitude 8,0, ~34 por
  golpe, derruba jogador fraco). Tem **regeneração** (`regeneration` no `numbered/<id>.json`, serve também ao No. 10)
  e foge com 15%. Receita da espada e patente Vice-Capitão são [SUPOSIÇÃO].

- **Feito (2026-10-06, segunda leva do Meshy):** soldado novo (7 mil tri, textura 1024), 4 Primigenius novos,
  aranha sem atravessar ao andar. Aguardando o Miguel testar (roteiro §14). Leve sobreposição na raiz das patas da
  aranha vista só de cima, de perto (aceitável).
- Prompts do Meshy para os modelos novos (ressurgido, Honju, revivido e, opcional, No. 9): `docs/PROMPTS_MESHY_0_2.md`.
- **Decidido (2026-10-06):** Release vai até 100% para todos, por **treino**; `rankCaps` sai do config (Etapa 2 da
  0.2). Plano completo da mega atualização: `docs/MEGA_ATUALIZACAO_0_2.md`.
- [SUPOSIÇÃO a confirmar] `primigenius_resurrected` fortitude 5,9 e `primigenius_honju` 6,0, sem spawn natural.
- **Feito (Miguel, 2026-10-06):** kaiju **não nascem naturalmente** (`spawn.natural` padrão `false`); vêm de
  alertas, invasões (chance natural por dia em `invasion.naturalChance`, [SUPOSIÇÃO] 20%) e missões.
- **Decidido (Miguel, 2026-10-06):** primeiro numerado = **Kaiju No. 9** (humanoide ~2 m, inteligente, comanda
  kaiju). É ele quem **revive** os kaiju (versões ressurgida/revivida). Entra depois do chefe Honju e das invasões
  (Etapa 8 da 0.2): feito (rig `tools/art/rig_kaiju_no9_mesh.py`, entidade, reviver, comandar, fugir com 30%).
  [SUPOSIÇÃO] fortitude 6,5 (452 de vida), fuga dá 250 de mérito, revive no máximo 3 vivos por vez.
- **Decidido (Miguel, 2026-10-06):** o Honju invoca só Yoju da **própria espécie** (`primigenius_honju` →
  `primigenius`; `primigenius_revived` → `primigenius_resurrected` [SUPOSIÇÃO]; Trichonephila Honju →
  `trichonephila`). Limites `maxYojuPerHonju`/`maxTotalPerHonju` ainda a definir no M16.
- Rifle sem munição na 0.1 (GDD não define); `required_rank` das armas só vale com as patentes (M14).
- [SUPOSIÇÃO] Corrida: `stamina.sprintCostPerSecond` 5 (100 de stamina = 20 s) e `sprintMinStamina` 20 para voltar a correr
  (depois de recuperar, aperte correr de novo). Criativo/espectador não gastam.
- 0.1-B: sem GameTest para destruição, dash e ataque carregado (só JUnit da matemática). (0.3: os GameTests de
  Carcass, KaijuAbility e KaijuAreaAbility não aninham mais `runAfterDelay`; ícones refeitos com `pixel_shading.py`.)
- **0.3 — aprovado e feito (Miguel):** aranha 57 de vida; dano de soldado por nível; patente só nos `unlocks`;
  desmonte próprio do Honju (8 etapas); Vice-Capitão 8.000 e Capitão 20.000 de mérito; recompensa de invasão por
  contribuição.
- **Modelos a caminho (Miguel):** Trichonephila **Honju** (aranha Honju, modelo próprio) e, depois, trajes da Força
  de Defesa no Meshy (prompts em `docs/PROMPTS_MESHY_0_3.md`; render por `GeoArmorRenderer` da GeckoLib, [DECIDIR]
  aprovado em princípio).
- [SUPOSIÇÃO] Hitbox nova da Trichonephila 3,4 × 1,8 (o modelo novo é baixo e largo) e partes de
  Trichonephila/Primigenius medidas na malha nova.
- Sinais de Y/Z das rotações nas animações GeckoLib (braço direito: Y negativo = para dentro): **confirmados em
  jogo** (o soldado mirando fecha os braços à frente).
- Sinal do eixo X para partes assimétricas nos `.geo.json`: conferir em jogo na primeira parte fora do centro.
- Pendências antigas não bloqueantes: Dev2 com Better Combat (PT6), bloqueio de montaria e mods de skin (PT5),
  teste com 4 clientes.
