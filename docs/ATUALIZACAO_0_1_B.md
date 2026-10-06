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
