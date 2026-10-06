# ATUALIZACAO_0_1_B.md — Atualização grande (feita no chat, testada no Claude Code)

O Miguel pediu tudo de uma vez: as partes foram escritas no chat SEM compilar. O Claude Code deve compilar,
rodar os testes e corrigir, parte por parte, na ordem abaixo. Cada parte tem interruptor no config quando faz sentido.

| Parte | Conteúdo | Estado |
|---|---|---|
| 1 | Escala (Etapa C): Primigenius 6, Honju 9; faixas por categoria (`KaijuScale`); kaiju grande anda direto e sobe degraus | ✅ escrita |
| 5 | `primigenius_resurrected` (Yoju, 5,9) e `primigenius_honju` (Honju, 6,0) registrados | ✅ escrita |
| 2 | Efeitos visuais e sons (Etapa D) | ✅ escrita |
| 3 | Destruição do ambiente (Etapa E) | ✅ escrita |
| 4 | Carcaça e desmonte (M11b) | ✅ escrita |
| 6 | Barra de vida do kaiju | ✅ escrita |
| 7 | Dash e ataque carregado | ✅ escrita |
| 8 | Soldado 1 | ✅ escrita |

## Resultado no Claude Code (2026-10-06)

Roteiro de teste manual único: `docs/ROTEIRO_TESTE_0_1_B.md` (inclui a lista de pendências).

| Verificação | Resultado |
|---|---|
| `./gradlew build` | ✅ compila; 15 suítes JUnit, 69 testes, 0 falhas |
| `./gradlew runGameTestServer` | ✅ 31/31 |
| Datapack no servidor | ✅ 0 erros, 0 avisos de dados; as 5 tags `destruction/*` carregam (todas as tags de bloco existem) |
| Cliente até a tela inicial (`runClient`) | ✅ sem erro de recurso, modelo ou textura; lang `en_us`/`pt_br` com as mesmas 259 chaves |
| Em jogo (renderers, HUD, VFX, tremor, destruição ao vivo) | ⏳ **não visto**: depende do roteiro manual |

Correções feitas (nenhuma funcionalidade removida):

| Commit | Parte | Causa e correção |
|---|---|---|
| `fix(0.1-b/parte8)` | 8 | `DataValidation.validateSoldiers` usava `LinkedHashMap` sem import (único erro de compilação da atualização) |
| `fix(0.1-b/gametests)` | testes do M10b | `criticalIsConsumedOnTheFirstHit` falhava com "O golpe leve deveria comecar": o `runAfterDelay` de dentro alterava o mapa de tarefas do GameTest durante a iteração e o bloco de fora rodava duas vezes (2º golpe = `DENIED_BUSY`). Passos agendados no nível de cima; um `FakePlayer` de perfil próprio por teste |

As partes 1 a 7 não precisaram de correção para compilar nem para passar nos testes automáticos.

APIs da lista "a conferir primeiro": **todas existem com o nome e a assinatura usados** (confirmado pela compilação
contra GeckoLib 4.7.5, NeoForge 21.1.252 e vanilla 1.21.1 com Parchment). O comportamento em jogo das que são só de
cliente (`GeoRenderLayer#renderForBone`, `BlockAndItemGeoLayer`, `ViewportEvent.ComputeCameraAngles#setRoll`,
`RegisterGuiLayersEvent#registerAbove`) ainda depende do roteiro manual.

Ambiente: o projeto virou repositório git (`chore: estado base` = `kn8-main` antes da 0.1-B). O `java` do PATH é o 8;
rodar o Gradle com `JAVA_HOME` no JDK 21 (`~/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`).

## Parte 1 — detalhes
- `tools/art/kaiju_art.py`: `Model(scale)` e `scale_animations`; `build_primigenius.py` (×6/4,6) e
  `build_primigenius_honju.py` (×9/5,5) regerados. JSON: dimensions e parts na mesma proporção.
- `KaijuScale` (core, JUnit `KaijuScaleTest`): Yoju 4–8, Honju 6–15, Daikaiju 20–30; maior medida da hitbox; folga
  15%; `DataValidation` avisa (não remove) fora da faixa.
- Config `kaiju.largeKaijuWidth` (2,5) e `kaiju.stepHeightFraction` (0,25): kaiju largo anda direto
  (`KaijuCombatGoal`) e chama `clearPathIfBlocked()`; `STEP_HEIGHT` = altura × fração (1 a 4).
- GameTests: hitbox do Primigenius 3,13 × 6,0; Honju em template novo `empty_15x12x15`;
  `newPrimigeniusSpeciesUseTheNewScale`.

## Parte 5 — detalhes [SUPOSIÇÃO]
- `primigenius_resurrected`: cópia do Primigenius, fortitude 5,9, spawn natural desligado.
- `primigenius_honju`: cópia do revivido, fortitude 6,0, spawn natural desligado.

## Parte 3 — detalhes
- `core/destruction/DestructionMath` (JUnit `DestructionMathTest`): categorias 0–4 pela dureza, `canBreak(forca, categoria)`,
  cratera irregular (`craterDepth`, borda 75–125% do raio por ruído determinista).
- `common/destruction/`: `DestructionService` (fila por dimensão em `KN8Server`, orçamento `destruction.blocksPerTick`,
  `mobGriefing`, áreas protegidas, block entity = indestrutível, sem drop por padrão), `ProtectedAreas` e
  `DestructionLog` (SavedData), `DestructionEvents` (LevelTickEvent.Post), restauração gradual
  (`rebuildBlocksPerTick`). Tags `data/kn8/tags/block/destruction/{fragile,normal,resistant,very_resistant,indestructible}`.
- Comandos `/kn8 destruction protect|unprotect|list|test <raio> <forca> [cratera]|restore`.
- Config novo: `destruction.dropItems`, `logLimit`, `pathClearCooldownTicks`, `pathPower{Yoju,Honju,Daikaiju}`.
- Habilidade ganhou `destruction {radius, power, crater, depth}`: slam (3,5 / 2 / cratera 1,5), charge (abre
  passagem à frente a cada tick com força 2). Kaiju grande travado usa `requestPathClear` (força pela categoria).

## Parte 2 — detalhes
- `VfxS2C` (protocolo **"6"**), `VfxService` (ids impact, shockwave, dust, slash, weapon_fire, roar, suit_release,
  overheat; alcance 64), `AbilityEffects` (vfx + camera_shake + sound do JSON no tick de impacto).
- Cliente: `client/vfx/VfxEffects` (partículas vanilla, quantidade por `fx.particleLevel`), `CameraShake`
  (trauma com decaimento, limite `fx.cameraShake`, cai com a distância).
- Habilidade ganhou `vfx` (lista), `camera_shake`, `sound`; atualizadas bite, slam, charge (sons vanilla como
  placeholder — PENDENTE: sons próprios .ogg).
- Aura do Release (a partir de `vfx.releaseAuraMin` = 20%, intensidade R/100) e sobrecarga (OVERLOAD/CRITICAL/PANIC)
  em pulsos (`vfx.suitVfxIntervalTicks`), no `PowerService`. Armas: clarão/fumaça no cano, impacto e corte no alvo.

## Parte 4 — detalhes
- `CarcassEntity` (Entity + GeoEntity, sem IA, cai e assenta): espécie sincronizada (hitbox da espécie), núcleo
  destruído ou não, totais da tabela `dismantle` sorteados na criação (já filtrados por `core_intact`/`core_destroyed`),
  entregues por etapa (`core/dismantle/DismantleMath.share`, JUnit). Segurar clique direito com item da tag
  `kn8:dismantle_tools` (faca, espada) = +4 ticks por interação; `ticks_per_step` fecha a etapa; última etapa some.
  Some sozinha após `carcass.despawnTicks` sem uso. Salva tudo no NBT.
- `KaijuEntity.tickDeath`: vira carcaça no 1º tick morto (sem animação de morte vanilla).
- Cliente: `CarcassRenderer` (modelo da espécie por instância; `MeshRenderLayer` aceita função espécie-por-instância).
  `CombatInput` não intercepta o clique direito quando a mira está numa carcaça.
- Itens: `kaiju_tissue`, `muscle_fiber`, `core_fragment`, `intact_core` (ícones placeholder por
  `tools/art/gen_material_icons.py`). Restam os avisos de itens de traje (M14).
- GameTest `CarcassGameTests.deadKaijuBecomesADismantlableCarcass`.

## Parte 6 — detalhes
- `KaijuEntity`: `CORE_FRACTION` e `CORE_EXPOSED` no SynchedEntityData (só envia quando muda; fração em passos de 0,5%).
- `client/hud/KaijuHealthBar` (camada acima da barra de chefe vanilla): nome, "categoria · porte · estado", vida,
  núcleo (pisca "EXPOSTO"). Alvo: mira (inclusive partes) até 48 blocos; fica 5 s após desviar.

## Parte 7 — detalhes
- `CombatAction` ganhou DASH, CHARGE_START, CHARGE_RELEASE (no fim do enum; índices antigos iguais).
- Dash: tecla Alt esquerdo (`key.kn8.dash`), 25 de stamina (recusa sem), `dashSpeed` 1,6, sem invulnerabilidade,
  animação `player.action.dash`, poeira.
- Ataque carregado: clique direito com lâmina = CHARGE_START (postura `player.action.charge`); soltar = RELEASE.
  < `chargeMinTicks` (6) = pesado comum; senão custo 20, multiplicador `CombatMath.chargedMultiplier` até
  `chargeFullMultiplier` (2,0) em `chargeMaxTicks` (30); carga completa = crítico garantido (e o pesado expõe o
  núcleo). HUD mostra "CARGA" / "CARGA MÁXIMA" (estimativa no cliente com o config do servidor sincronizado).
- O clique direito com lâmina NÃO dispara mais o pesado direto: passa pela carga.

## Parte 8 — detalhes
- Dados: `SoldierDef` + `data/kn8/kn8/soldier/soldier_1.json` (vida 24, armadura 6, velocidade 0,3, níveis low 5 /
  normal 10 / high 20 / elite 30 [SUPOSIÇÃO], variantes unarmed/rifle/pistol/sword/knife). `KN8Data.SOLDIER` +
  `DataValidation.validateSoldiers`.
- Malha: `tools/art/rig_soldier_mesh.py` (partes da segmentação; corpo acima de `HIP_Y` = 1,1 m, pernas abaixo pelo
  lado; pivôs no pescoço/ombros/quadris; `item_right` na mão) → `meshes/soldier`, `geo/entity/soldier.geo.json` (só
  ossos), `animations/entity/soldier.animation.json` (idle, walk, attack, shoot, hurt), `textures/entity/soldier.png`.
- `common/soldier/SoldierEntity` (PathfinderMob + GeoEntity): variante e nível sincronizados; Release do nível pelas
  fórmulas do jogador (`PowerMath.damageMultiplier`, `speedBonus`, `damageReduction`); combate com `ActionTimeline` e
  dano no tick de impacto do JSON da arma, raycast que acerta partes e ignora jogadores/soldados; fogo amigo não fere.
  `SoldierCombatGoal`: atirador mantém `keep_distance` (12 ± 4), lâmina/soco se aproxima até o alcance (borda).
- Kaiju caçam soldados (alvo prioridade 3). `SoldierRenderer` (malha + arma no osso `item_right` via
  `BlockAndItemGeoLayer`). Ovo de spawn (rifle, normal). `/kn8 soldier spawn <variante> [nivel]`.
- GameTests `SoldierGameTests` (atirador a > 20 blocos, níveis escalam, jogador não fere soldado).

## Para o Claude Code: APIs a conferir primeiro (escritas sem compilar) — ✅ todas compilam (2026-10-06)
- GeckoLib 4.7.5: `GeoRenderLayer#renderForBone` (assinatura), `BlockAndItemGeoLayer` (métodos `getStackForBone`,
  `getTransformTypeForStack`, `renderStackForBone`), `GeoBone#isHidden`, `AnimationState#isMoving`.
- NeoForge 21.1: `DeferredSpawnEggItem`, `DeferredRegister.Items#registerSimpleItem(String, Properties)`,
  `PacketDistributor#sendToPlayersNear`, `ViewportEvent.ComputeCameraAngles#setRoll`,
  `RegisterClientReloadListenersEvent`, `RegisterGuiLayersEvent#registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ...)`.
- Vanilla 1.21.1: `SavedData.Factory` (3 args) e `save(CompoundTag, HolderLookup.Provider)`, `AABB#setMinY`,
  `Entity#maxUpStep`, `EntityType.Builder#eyeHeight`, `ResourceLocation#withPath(UnaryOperator)`,
  `VertexConsumer#addVertex(Pose, ...)`, `Entity#getDimensions` sobrescrito em entidade não viva,
  `Attributes.STEP_HEIGHT`, `NbtUtils.readBlockState(HolderGetter, CompoundTag)`.
- Tags de bloco usadas nas categorias de destruição: conferir que todas existem em 1.21.1 (se uma faltar, o arquivo
  inteiro de tag falha ao carregar).
- Ordem: compile → corrija → GameTests (`runGameTestServer`) → corrija, parte por parte, na ordem da tabela acima.

## Correções visuais do primeiro teste em jogo (2026-10-06)

Pedido do Miguel (capturas): textura da aranha bugada; soldado parado e com textura meio bugada; rifle e pistola
errados na mão do jogador e do soldado; espada toda errada. **Feito sem compilar**: a rede desta sessão bloqueia o
Maven do NeoForge/Minecraft. Tudo visual foi conferido num simulador fora do jogo que reproduz a cadeia de
transformações do vanilla e da GeckoLib (`tools/art/preview_held_items.py`; ele reproduziu exatamente as capturas
antes da correção: rifle diagonal para o chão, espada para a frente). Prévias em `docs/img/`.

| Problema | Causa | Correção |
|---|---|---|
| Aranha: linhas douradas, manchas brancas | `meshy_convert.decimate` dava a cada vértice a UV do vértice original mais próximo; nas costuras, 2.899 de 5.998 triângulos ficaram com cantos em ilhas diferentes da textura (cobrindo até a textura inteira) | `tools/art/rebake_mesh_texture.py`: atlas novo, uma célula por triângulo (mesma forma, 90 texels/m, borda de 1 texel); triângulo bom copia a textura antiga, quebrado pega a cor do ponto bom mais próximo (posição + normal). Rig regerado. `decimate` corrigido para as próximas conversões (UV decidida por triângulo, testado: 1.170 → 11 triângulos ruins num teste com o soldado) |
| Soldado: textura cintilando/riscos | Textura 1024 (~400 texels/m) sem mipmap e fundo preto entre as ilhas | 512 com borda de 16 texels nas ilhas (`pad_texture.py`, chamado pelo `rig_soldier_mesh.py`). As UVs do soldado estavam boas |
| Soldado "estático" | Idle de ±2° (invisível), braços sempre pendurados, nenhuma pose de arma | Controllers `movement` (pernas e tronco: idle com respiração, andar ±30°) + `arms` (pose por classe de arma: `rifle`, `pistol`, `blade`, `unarmed` × `ready`/`walk`/`aim`; `aim` quando `Mob#isAggressive`, ligado pelo `SoldierCombatGoal`) + `action` (`attack`, `shoot_rifle`, `shoot_pistol` com coice) + `reaction`. Cabeça segue o olhar e, mirando com arma de fogo, os braços somam a inclinação/giro da cabeça (`SoldierRenderer.SoldierModel`) |
| Rifle/pistola na mão do jogador | Display copiado da espada vanilla (`[0,-90,55]`) com o modelo deitado | Cano ao longo do braço: `thirdperson` `[0, 90, 0]` com o cabo (`hand_grip`) no centro do punho; 1ª pessoa `[0, 93, 0]`; jogador segurando rifle/pistola usa a pose `CROSSBOW_HOLD` (dois braços à frente, seguindo a mira) via `IClientItemExtensions` (`HeldWeaponPoses`) |
| Arma na mão do soldado | `SoldierRenderer` aplicava `Rx(-90) Ry(180)` do vanilla sem converter para Y para cima (sobrava o `Ry(180)`); a `BlockAndItemGeoLayer` gira o item de novo pela rotação do osso; braços da malha abertos em "A" | `renderForBone` próprio (só volta ao pivô), item alinhado à linha ombro → mão, ponto de origem igual ao do vanilla, depois `Rx(-90)` e o display de 3ª pessoa do item (o mesmo do jogador). `item_right` agora no centro do punho |
| Espada | Modelo do Meshy virou uma agulha preta com guarda redonda | Modelo próprio por código (`tools/art/build_sword.py`, design original): lâmina de um gume com fio prateado e faixa ciano, guarda, cabo trançado, pomo; orientação de espada vanilla. O `meshy_convert.py` pula a espada (`replaced_by`) |

Pendente: compilar (`./gradlew build`, `runGameTestServer`) e o roteiro §10. Se preferir outro modelo de espada,
mande o GLB: é só tirar o `replaced_by` da tabela e rodar o conversor.

## Modelos novos do Meshy, estilo Minecraft (2026-10-06)

O Miguel refez no Meshy o soldado, a Trichonephila (agora com 8 patas) e o Primigenius, no estilo "blocos". GLB
fora do Git (pasta `--src` do conversor), nomes = id. Conferido fora do jogo (prévias em `docs/img/`).

| Modelo | Conversão | Rig | Mudanças no jogo |
|---|---|---|---|
| `soldier` (8.353 tri) | sem redução; segmentação do Meshy não é mais usada (`soldier_parts.glb` veio em 14 pedaços) | `rig_soldier_mesh.py` divide pela forma: braços (vão entre braço e tronco), cabeça acima do pescoço, corpo acima do quadril, pernas pelo lado | Poses de arma reajustadas (braços retos agora, não em "A") |
| `trichonephila` (6.850 tri, 8 patas) | sem redução, 5,5 m de envergadura, 1,7 de altura | `rig_trichonephila_mesh.py` refeito: cefalotórax/cabeça/quelíceras/abdômen + cada pata achada pela parte distante do centro (scipy) | Hitbox 3,0 × 3,4 → **3,4 × 1,8** [SUPOSIÇÃO]; partes (cabeça = núcleo, cefalotórax, abdômen) remedidas; GameTest e JUnit de escala atualizados |
| `primigenius` (8.276 tri) | sem redução, 6 m de altura | `rig_primigenius_mesh.py` (novo): cauda (4 segmentos), cabeça/mandíbula, braço/antebraço, pernas, corpo; mesmos ossos e animações do modelo de cubos | Partes (cabeça, torso, núcleo no peito, pernas) remedidas; hitbox igual (3,13 × 6,0) |

`meshy_convert.py` agora pula GLB ausente; `build_primigenius.py` não sobrescreve espécie com malha.
Pendente: compilar e o roteiro §11. Os outros modelos (Honju, ressurgido, revivido, armas) ficam para depois.

## Compilado e visto em jogo (2026-10-06, sessão na nuvem com rede liberada)

`./gradlew build` ✅ (69 JUnit), `runGameTestServer` ✅ 31/31, dados 0 erro / 0 aviso. Servidor dedicado + cliente
(Xvfb, comandos por RCON), capturas em `docs/img/jogo_*.png`:

| Visto | Resultado |
|---|---|
| Soldado novo, 4 variantes armadas | ✅ malha e textura certas; arma na mão direita, poses de §10 |
| Soldado com alvo | ✅ mira com os dois braços fechando à frente (sinal de Y confirmado), coice e fumaça; abate a aranha |
| Trichonephila 8 patas e Primigenius com cauda | ✅ texturas, barra de vida, sombra; hitbox (F3+B) e partes sobre o modelo |
| Kaiju caçando soldado | ✅ andar e slam com a malha nova; morte vira carcaça com o modelo novo |
| Jogador com rifle/pistola/espada/faca | ✅ 1ª pessoa no canto direito; 3ª pessoa com os dois braços à frente nas armas de fogo |

Corrigido no teste: `/summon` com NBT deixava o soldado sem arma; clarão do tiro gigante (FLASH vanilla).
Não testado aqui: 2 clientes (sincronização), som (sem placa de áudio), combate do jogador em survival.

## Testes das seções 10, 11 e 12 na nuvem (servidor dedicado + 2 clientes, 2026-10-06)

| Item | Resultado |
|---|---|
| 10.3–10.6, 10.9 | ✅ soldados com cada arma; Dev2 vê o Dev com o rifle nos dois braços (pose sincronizada) |
| 11.4–11.6 | ✅ kaiju novos com IA caçando soldados; carcaças usam os modelos novos |
| 12.3, 12.4, 12.8, 12.10 | ✅ abate pelo Dev conta no Perfil (1) e registra a espécie no Bestiário (1/5, survival); Dev2 vê os próprios dados e o próprio idioma |
| 4.2/4.3 desmonte | ✅ etapas, carcaça some, materiais (tecido, fibra, núcleo intacto) |
| Bug achado e corrigido | segurar o clique direito ao terminar o desmonte começava ataque carregado |
| Observação | a carcaça fica em pé na pose do kaiju vivo (não tomba); sugestão abaixo |
| Não testado aqui | som (sem placa de áudio), dano por parte com um jogador real batendo (coberto por GameTest) |
