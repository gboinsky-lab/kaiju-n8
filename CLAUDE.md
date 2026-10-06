# CLAUDE.md — Kaiju No. 8: Defense Force (`kn8`)

Mod fan gratuito de Minecraft, desenvolvido por Miguel Augusto Gnoinsky. Este arquivo é a memória do projeto para o
Claude Code: leia antes de qualquer tarefa. Responda sempre em **português do Brasil**.

Memória técnica das novas frentes: `docs/SOLDADO_1_IMPLEMENTATION.md`, `docs/COMBAT_VFX_AND_DESTRUCTION.md` e
`docs/MEGA_ATUALIZACAO_0_2.md` (plano da 0.2, em etapas)
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
| Protocolo de rede | `"6"` (subir ao mudar qualquer payload) |
| Ataque "heavy" de kaiju | [SUPOSIÇÃO] atravessa o bloqueio comum; só parry/esquiva evitam (`combat.heavyIgnoresBlock`) |
| Pacotes de entrega | Nunca incluir `build.gradle`/`gradle.properties` (os do `kn8-main` são os corretos: PAL `transitive=false`, run `clientJoin`, repo Modrinth, bloco do Better Combat) |
| Modelos Meshy | Armas: OBJ (`neoforge:obj`) na mão; personagens: malha presa aos ossos GeckoLib (`client/render/mesh`: `meshes/<especie>.json` + um OBJ por osso; .geo.json só com ossos; mesma textura/render type do modelo). Conversor `tools/art/meshy_convert.py`; rigging: `rig_trichonephila_mesh.py`, `rig_soldier_mesh.py`, `rig_primigenius_mesh.py` (divisão pela forma; mesmos nomes de ossos, então as animações continuam) |
| Arma na mão (0.1-B, teste) | Lâmina: display de espada vanilla. Arma de fogo: cano ao longo do braço (`thirdperson` rotação `[0, 90, 0]`, `hand_grip` em `meshy_assets.json`), jogador em `CROSSBOW_HOLD` (`HeldWeaponPoses`). Soldado: mesma cadeia do vanilla em Y para cima (só `Rx(-90)`), item alinhado ombro→mão. Conferir com `tools/art/preview_held_items.py` |
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
| **0.2 Menu da Força de Defesa** (tecla M; abas Perfil, Missões, Alertas, Esquadrão, Bestiário, Arsenal; Q/E troca) | ✅ compila, testes passam, todas as abas vistas em jogo (pt_br e en_us); partes de etapas futuras com "Em breve (Etapa N)"; aguardando roteiro §12 |
| **0.2 Machado** (`kn8:axe`, estilo `heavy`, 2,0 de comprimento) e **espada nova** do Meshy | ✅ jogador (1ª/3ª pessoa) e soldado (`/kn8 soldier spawn axe`/`sword`) vistos em jogo; arma pesada também carrega golpe; números [SUPOSIÇÃO] em `weapon/axe.json` |
| **0.2 HUD nova** (fiel à referência "HUD de combate avançado - estilo anime"; 60% do tamanho da arte, `BASE_SIZE`) | ✅ vista em jogo; arte em `textures/gui/hud/` gerada por `tools/art/gen_hud.py` (texturas 4× desenhadas em pixel de textura, `blur` ligado); aguardando aprovação |
| **Modelos Meshy estilo Minecraft** (soldado, aranha 8 patas, Primigenius) | ⏳ compila, JUnit 69/69, GameTests 31/31 e **vistos em jogo** (servidor dedicado + 1 cliente na nuvem, capturas em `docs/img/jogo_*`); aguardando roteiro §11 com 2 clientes |
| **0.1-B correções visuais** (aranha, soldado, armas na mão, espada nova) | ⏳ compila e testes passam; armas na mão do jogador (1ª/3ª pessoa) e do soldado e mira do soldado vistas em jogo; **aguardando roteiro §10** |
| **0.1-B** (atualização grande, escrita no chat): escala, VFX, destruição, carcaça/desmonte, Primigenius verde e Honju marrom, barra de vida, dash/ataque carregado, Soldado 1 | ⏳ compila, JUnit (69) e GameTests (31/31) passam em 2026-10-06; **aguardando teste manual** — `docs/ROTEIRO_TESTE_0_1_B.md` (resultado e correções em `docs/ATUALIZACAO_0_1_B.md`) |
| M11b carcaças e desmonte · M12 transformação · M13 NPCs · M14 patentes/crafting | pendentes |
| M15 missões · M16 chefe Honju · M17 Tachikawa · M18 endurecimento/performance | pendentes |

## Bugs e soluções (resumo)

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
- **Compilar na nuvem:** os arquivos de build não estão no repositório; a sessão monta um `build.gradle` provisório
  (só local, em `.git/info/exclude`) com as versões desta página e roda servidor + cliente em Xvfb com RCON.
- **Meshy (redução):** o GLB separa vértices nas costuras de UV e a redução abria buracos → `decimate` solda pela
  posição antes de reduzir. **Rig dos Honju:** dedos abaixo do corte de altura viravam perna e as costas atrás do
  ombro viravam braço (pedaços soltos no slam) → `arm_front_z` e `arm_back_z` por espécie.
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
| Trichonephila Honju | Honju | conceito escolhido: Tecedeira Abissal (falta confirmar e modelar) | pós-0.1 |

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

- **Decidido (2026-10-06):** Release vai até 100% para todos, por **treino**; `rankCaps` sai do config (Etapa 2 da
  0.2). Plano completo da mega atualização: `docs/MEGA_ATUALIZACAO_0_2.md`.
- [SUPOSIÇÃO a confirmar] `primigenius_resurrected` fortitude 5,9 e `primigenius_honju` 6,0, sem spawn natural.
- **Decidido (Miguel, 2026-10-06):** o Honju invoca só Yoju da **própria espécie** (`primigenius_honju` →
  `primigenius`; `primigenius_revived` → `primigenius_resurrected` [SUPOSIÇÃO]; Trichonephila Honju →
  `trichonephila`). Limites `maxYojuPerHonju`/`maxTotalPerHonju` ainda a definir no M16.
- Rifle sem munição na 0.1 (GDD não define); `required_rank` das armas só vale com as patentes (M14).
- Corrida com custo de stamina (GDD §7) ainda não implementada (dash e ataque carregado: 0.1-B).
- 0.1-B: sem GameTest para destruição, dash e ataque carregado (só JUnit da matemática); GameTests de Carcass,
  KaijuAbility e KaijuAreaAbility ainda aninham `runAfterDelay`; sons próprios (.ogg) e ícones finais dos materiais.
- [SUPOSIÇÃO] Hitbox nova da Trichonephila 3,4 × 1,8 (o modelo novo é baixo e largo) e partes de
  Trichonephila/Primigenius medidas na malha nova.
- Sinais de Y/Z das rotações nas animações GeckoLib (braço direito: Y negativo = para dentro): **confirmados em
  jogo** (o soldado mirando fecha os braços à frente).
- Sinal do eixo X para partes assimétricas nos `.geo.json`: conferir em jogo na primeira parte fora do centro.
- Pendências antigas não bloqueantes: Dev2 com Better Combat (PT6), bloqueio de montaria e mods de skin (PT5),
  teste com 4 clientes.
