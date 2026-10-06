# CLAUDE.md — Kaiju No. 8: Defense Force (`kn8`)

Mod fan gratuito de Minecraft, desenvolvido por Miguel Augusto Gnoinsky. Este arquivo é a memória do projeto para o
Claude Code: leia antes de qualquer tarefa. Responda sempre em **português do Brasil**.

Memória técnica das novas frentes: `docs/SOLDADO_1_IMPLEMENTATION.md` e `docs/COMBAT_VFX_AND_DESTRUCTION.md`
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
5. Nada reutilizado sem licença verificada; nada dos mods de Kaiju No. 8 existentes; **nenhum asset da obra**.
6. Todo [VERIFICAR] é confirmado no início do módulo que o usa (código-fonte do NeoForge/GeckoLib/PAL).
7. Dano sempre no servidor, no tick de impacto do JSON, nunca pela animação.
8. Mixins só com justificativa registrada aqui.
9. Payloads validados com rate limit (`C2SGuard`); sync só na mudança, com reenvio em login/respawn/dimensão.
10. JSON inválido gera log claro e nunca derruba o servidor.
11. Convenções da Fase 4 §2.5: sufixos `C2S`/`S2C`, chaves `kn8.<área>.<chave>`, 4 espaços, **120 colunas**,
    comentários explicam o porquê. Javadoc/comentários do código em português sem acento.
12. Compilar de primeira: imports completos, sem APIs inventadas.

---

## Decisões fixadas

| Área | Decisão |
|---|---|
| Sync público / privado | Público: `.sync()` nativo. Privado: NUNCA `.sync()` — payload manual ao dono via `NetworkSync` (PT1) |
| Dados de jogo | Reload listener + Codec (`DataRegistry`, `KN8Data`); registry de datapack só para worldgen/damage_type |
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
| Modelos Meshy | Armas: OBJ (`neoforge:obj`) na mão; personagens: malha presa aos ossos GeckoLib (`client/render/mesh`: `meshes/<especie>.json` + um OBJ por osso; .geo.json só com ossos; mesma textura/render type do modelo). Conversor `tools/art/meshy_convert.py`; rigging da aranha `tools/art/rig_trichonephila_mesh.py` |
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
| Etapa A pipeline Meshy (armas OBJ na mão, pistola, espada, animação por arma) · Etapa B HUD (referência "HUD de combate avançado - estilo anime"; ícones em `textures/gui/hud_icons.png`, gerados por `tools/art/gen_hud_icons.py`) | ⏳ aguardando teste |
| **0.1-B correções visuais** (aranha, soldado, armas na mão, espada nova) | ⏳ fora do jogo (sem rede para o Maven nesta sessão: não compilado); **aguardando compilar + roteiro §10** de `docs/ROTEIRO_TESTE_0_1_B.md` |
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
- **0.1-B (espada):** o modelo do Meshy saiu como agulha → modelo próprio por código (`build_sword.py`).
- **0.1-B (compilação):** único erro foi um import (`LinkedHashMap` em `DataValidation`); as APIs "a conferir" existem.

## Arte

- **Direitos autorais:** todos os kaiju usam **designs originais**. Não recriar nem aproximar designs oficiais da
  obra; não usar imagens oficiais como referência em geradores. Nomes e mecânicas do GDD são mantidos.
- **Pipeline:** `tools/art/kaiju_art.py` (base comum: cubos, ossos, pintores de textura por paleta, atlas, validação)
  e um script por espécie. Variantes de cor ("ressurgido") compartilham o esqueleto e trocam só a textura.
  Prévia: `python3 tools/art/preview.py <espécie> [saída.png] [escala]`.
- `tools/gen_kaiju_placeholders.py` **pula** as espécies em `FINAL_ART` (não sobrescreve arte final).
- Animações do jogador: `tools/art/gen_player_animations.py` (tempos lidos do JSON das armas).
- Armas: `tools/art/meshy_convert.py` (o antigo `gen_item_textures.py` foi removido); espada: `build_sword.py`
  (o conversor pula a espada: `replaced_by`). Prévia das armas na mão (jogador 3ª/1ª pessoa e soldado):
  `python3 tools/art/preview_held_items.py` → `build/preview_held_items_*.png`.
- Malhas: `rebake_mesh_texture.py` (refaz textura de malha com UV quebrada), `pad_texture.py` (borda nas ilhas +
  reduzir); `rig_soldier_mesh.py` já gera a textura 512 com borda.

| Kaiju (id) | Categoria | Arte | No jogo |
|---|---|---|---|
| `trichonephila` | Yoju | malha do Meshy (6 mil tri, 3 patas por lado), hitbox 3,0 × 3,4 | ✅ |
| `primigenius` | Yoju | final (brute de cabeça de crocodilo, claro) | ✅ |
| `primigenius_resurrected` | Yoju ressurgido | final (verde), 6 de altura | ✅ registrado (0.1-B) |
| `primigenius_honju` | Honju | final (Titã Bruto, marrom), 9 de altura | ✅ registrado (0.1-B) |
| `primigenius_revived` | Honju ressurgido | final (Titã Bruto, roxo, hitbox 3,5×5,5) | ✅ |
| Trichonephila Honju | Honju | conceito escolhido: Tecedeira Abissal (falta confirmar e modelar) | pós-0.1 |

Regra de design do Miguel: Honju e Yoju são **criaturas diferentes** (modelo e textura próprios); a versão
"ressurgida" é a mesma criatura com outra paleta e danos.

## Pendências e [DECIDIR]

- [DECIDIR] Teto de liberação duplicado (`rankCaps` no config × `release_cap` no JSON da patente). Recomendação:
  JSON como fonte única; remover `rankCaps` no M14.
- [SUPOSIÇÃO a confirmar] `primigenius_resurrected` fortitude 5,9 e `primigenius_honju` 6,0, sem spawn natural.
- [DECIDIR] Qual Yoju o Honju invoca no M16 (limites `maxYojuPerHonju`/`maxTotalPerHonju`).
- Rifle sem munição na 0.1 (GDD não define); `required_rank` das armas só vale com as patentes (M14).
- Corrida com custo de stamina (GDD §7) ainda não implementada (dash e ataque carregado: 0.1-B).
- 0.1-B: sem GameTest para destruição, dash e ataque carregado (só JUnit da matemática); GameTests de Carcass,
  KaijuAbility e KaijuAreaAbility ainda aninham `runAfterDelay`; sons próprios (.ogg) e ícones finais dos materiais.
- [SUPOSIÇÃO] Sinais de Y/Z das rotações nas animações GeckoLib (braço direito: Y negativo = para dentro) vêm da
  convenção do Blockbench; conferidos só no simulador `preview_held_items.py`. Se os braços abrirem em vez de
  fechar na mira, inverter o Y das poses em `rig_soldier_mesh.py` (`POSES`/`AIM`).
- Sinal do eixo X para partes assimétricas nos `.geo.json`: conferir em jogo na primeira parte fora do centro.
- Pendências antigas não bloqueantes: Dev2 com Better Combat (PT6), bloqueio de montaria e mods de skin (PT5),
  teste com 4 clientes.
