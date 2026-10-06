# SOLDADO_1_IMPLEMENTATION.md — Soldado 1, armas e armadura

Memória técnica do Soldado 1. Leia antes de "Continue o Soldado 1". Atualize ao fim de cada etapa.

## Estado atual

| Item | Estado |
|---|---|
| Pipeline GLB → OBJ (`tools/art/meshy_convert.py` + `meshy_assets.json`) | ✅ Etapa A |
| Faca de combate, rifle — modelo do Meshy na mão | ✅ Etapa A (substituíram os ícones 16×16) |
| Pistola (`kn8:pistol`), espada (`kn8:sword`) — itens + JSON de arma | ✅ Etapa A, números [SUPOSIÇÃO] |
| Animações do jogador por arma (`player.<item>.<acao>`) | ✅ Etapa A |
| Soldado convertido e dividido em 7 partes (`tools/art/converted/soldier_1/`) | ✅ preparado |
| Espadas duplas embainhadas (`tools/art/converted/twin_swords_sheathed/`) | ✅ preparado (sem item ainda) |
| Malha presa aos ossos (`MeshRenderLayer` + `MeshModels`) | ✅ já em uso pela Trichonephila |
| Entidade Soldado 1 (GeckoLib + malha presa aos ossos) | ✅ escrita (0.1-B, falta compilar/testar) |
| Variantes (sem arma, rifle, pistola, espada, faca) | ✅ escrita |
| Animação em camadas (pernas × braços por arma, mira, cabeça) e arma alinhada ao braço | ✅ escrita (correção do teste, 2026-10-06; falta compilar) |
| Espada própria (`build_sword.py`) no lugar da do Meshy | ✅ |
| Níveis de potência do traje (low/normal/high/elite = Release 5/10/20/30) | ✅ escrita |
| Armadura em peças (equipável pelo jogador) | PENDENTE — depende dos trajes (M14) |
| Bainha da espada única | PENDENTE — ASSET MESHY (prompt abaixo) |
| Espadas duplas soltas (para sacar) | PENDENTE — ASSET MESHY (prompt abaixo) |

## Modelos (fonte: GLB do Meshy, fora do repositório)

| Nome no mod | Arquivo GLB | Triângulos | Textura no mod |
|---|---|---|---|
| `combat_knife` | Meshy_AI_Kafka_Knife_Minecraft_1005185426_texture.glb | 1.246 | 512 |
| `rifle` | Meshy_AI_Reno_Rifle_Minecraft__1005185506_texture.glb | 1.512 | 512 |
| `pistol` | Meshy_AI_Kafka_Handgun_Minecra_1005185501_texture.glb | 1.566 | 512 |
| `sword` | Meshy_AI_Hoshina_Blade_Minecra_1005185625_texture.glb | 1.243 | 512 |
| `soldier_1` (7 partes) | Meshy_AI_Defense_Force_Soldier_1005185755_.glb + Meshy_AI_export_1791226720_part-segmentation.glb | 8.618 | 1024 |
| `trichonephila` (malha) | Meshy_AI_Kaiju_Spider_5k_Minec_1005185750_.glb | 6.000 (reduzida de 10.060) | 1024 |
| `twin_swords_sheathed` | Meshy_AI_Hoshina_Sheath_Minecr_1005185743_texture.glb | 1.559 | 512 |

Nomes no jogo são genéricos (os nomes dos arquivos citam personagens da obra; no jogo, não).

**Regerar:** guarde os GLB numa pasta fora do Git (ex.: `meshy_src/`, no `.gitignore`) e rode
`python3 tools/art/meshy_convert.py --src meshy_src`.

**Correções do teste (Etapa A):** armas de fogo montadas na horizontal (`orientation: horizontal`), tamanhos no
estilo Minecraft (`item_length`: faca 0,85, pistola 0,75, rifle 1,6, espada 1,5) e lâmina engrossada sem mexer no
cabo (`blade_fraction` + `blade_thickness`).

**Armas de fogo na mão (2026-10-06):** display `[0, 90, 0]` (cano ao longo do braço) com o ponto `hand_grip` do
OBJ no centro do punho; gerado pelo `meshy_convert.py` (`held_display`). Conferir com
`python3 tools/art/preview_held_items.py` antes de testar em jogo.

**Ajuste fino da arma na mão:** abrir `assets/kn8/models/item/<arma>.json` no Blockbench (com o OBJ), aba Display,
ajustar e copiar os valores de `display` de volta para `meshy_assets.json`/JSON. A orientação base é a de uma
espada vanilla (empunhadura no canto de baixo, ponta no de cima).

## Balanceamento (soldados)

Hierarquia do prompt: soldado baixa < normal < alta < elite <<< kaiju forte. Proposta para a Etapa F: os níveis
são faixas da **% liberada** que já existe (M5), definidas no JSON do soldado — baixa ~5%, normal ~10%, alta ~20%,
elite ~30% [SUPOSIÇÃO] —, então dano, velocidade e redução vêm das fórmulas existentes; nenhum sistema novo.

## Plano da Etapa F

1. Entidade `kn8:soldier` (uma só) com variantes por dados (`soldier/<variante>.json`: arma, nível, armadura).
2. Renderizador: esqueleto GeckoLib (head, torso, waist, arm_left, arm_right, legs, boots) com a malha de cada
   parte desenhada no osso (`GeoRenderLayer`), arma na mão pelo modelo de item.
3. IA de combate reaproveitando `CombatService`/`ActionTimeline` (mesmo fluxo de dano no tick do JSON).
4. Armadura em peças a partir da segmentação.

## Prompts Meshy pendentes

Bainha da espada única e espadas duplas soltas: ver `docs/COMBAT_VFX_AND_DESTRUCTION.md`, seção "Prompts Meshy".
