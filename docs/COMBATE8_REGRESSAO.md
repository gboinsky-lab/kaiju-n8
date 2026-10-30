# COMBATE-8 — Regressão do combate

Miguel, 2026-10-30: "pode fazer a etapa 8". Números e duelos ficam para depois (decisão de 2026-10-30): aqui só a
regressão das técnicas das COMBATE-1 a 7 (animação, nome, efeito, som, GameTests e conferência em jogo).

## O que foi conferido

- **Auditoria automática** `tools/combat/auditar_tecnicas.py` (rodar de novo a cada técnica nova): 133 técnicas
  (46 do jogador em 11 armas, 87 das NPC em 13 perfis). Jogador: animação `player.<arma>.special(_<id>)`, nome
  `kn8.weapon.special.<id>` nos dois idiomas, receita visual no perfil `player` (punhos da forma: perfil e fantoche
  do No. 8) e som do JSON. NPC: técnica na lista de animadas da entidade (`ANIMATED*`), animação
  `<prefixo>.action.<id>` e receita visual. **0 lacunas** depois da correção abaixo.
- **Defeito achado e corrigido:** a baioneta do Narumi não tem `special` no JSON, então a primeira técnica (No. 1
  Eruption Slash) é mandada pelo servidor como `player.narumi_bayonet.special`, que não existia (o gerador só fazia
  `special_eruption_slash`): o jogador ficava parado na Eruption Slash. O `gen_player_animations.py` agora copia a
  primeira técnica para `special` quando a arma não tem especial; visto em jogo (estocadas na direção do kaiju).
- **GameTest novo** `everyPlayerTechniqueFires` (`Combat8GameTests`): o jogador usa, uma a uma, as 44 técnicas das
  armas com mais de uma técnica (X até ela, R), com a vida baixa e o último estágio montados só para quem pede;
  todas saem, e o Falling Thunder no chão é recusado (vem de cima). Fora: punhos da forma do No. 8 (só
  transformado; GameTest próprio) e armas de fogo. GameTests 303/303 em 3 rodadas seguidas.
- **Em jogo** (servidor dedicado + 2 clientes, `docs/img/combate8_regressao.jpg`): Eruption Slash do jogador com
  animação; Thunder Fire, Flame Shower e Sky Turner (pendentes da COMBATE-6) contra um Honju parado. Os estouros
  do Thunder Fire e do Flame Shower leem mais brancos que rosa/laranja de longe (clarão do núcleo forte) [anotado
  para a etapa de tamanhos/tempos dos efeitos].

## Sons das técnicas (sem som novo: regra do AUDIO-0)

Nenhuma técnica nova tem som próprio; todas reaproveitam os sons das armas:

| Som | Técnicas do jogador |
|---|---|
| `kn8:weapon.sword_heavy` | 21 (espada do Hoshina, baionetas) |
| `kn8:weapon.axe_heavy` | 15 (machados, manoplas) |
| `kn8:weapon.axe_special` | 8 (especiais e Thunder Emperor, soco de ruptura) |
| `kn8:weapon.knife_heavy`, `kn8:weapon.rifle_shot` | 1 cada |

As 87 técnicas das NPC começam todas com `kn8:weapon.sword_swing` (também os socos do Isao/No. 8 e os tiros da
Mina/Reno). Pela decisão do Miguel de 2026-10-23 (só regravar os 27 sons existentes), sons próprios por técnica
ficam para quando ele pedir. O `docs/audio/ELEVENLABS_PROMPTS.md` (AUDIO-0, antes das COMBATE) tem prompts
de parte delas (formas do Hoshina, Thunder Emperor); as técnicas novas da Kikoru, do Narumi, do Isao e o soco de
ruptura ainda não têm prompt.

**Decisão do Miguel (2026-10-30):** os sons serão arrumados numa etapa final própria (**SONS**, depois da
transformação parcial); sem prompts novos por ora.

## Tabela completa

| Quem | Arma/perfil | Tecnica | Animacao | Nome | Efeito | Som |
|---|---|---|---|---|---|---|
| jogador | axe | seismic_cleave | propria | ok | ok | kn8:weapon.axe_special |
| jogador | axe | falling_thunder | propria | ok | ok | kn8:weapon.axe_special |
| jogador | axe | water_skimmer | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | axe | half_moon | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | axe | rising_thunder | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | axe | twin_swallows | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | axe | daruma_dropper | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | combat_knife | knife_flurry | propria | ok | ok | kn8:weapon.knife_heavy |
| jogador | hoshina_sword | kuuchi | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | hoshina_sword | kosa_uchi | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | hoshina_sword | ran_uchi | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | hoshina_sword | kasumi_uchi | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | hoshina_sword | kaeshi_uchi | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | hoshina_sword | yae_uchi | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | hoshina_sword | juni_hitoe | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | kaiju_no8_fist | ground_smash | propria | ok | ok | kn8:weapon.axe_special |
| jogador | kaiju_no8_fist | rupture_punch | propria | ok | ok | kn8:weapon.axe_special |
| jogador | narumi_bayonet | eruption_slash | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | narumi_bayonet | slash_salvo | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | narumi_bayonet | seven_branched_sword | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | narumi_bayonet | thunder_fire | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | narumi_bayonet | flame_shower | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | narumi_bayonet | sky_turner | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers1_bayonet | numbers1_thrust | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers1_bayonet | eruption_slash | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers1_bayonet | slash_salvo | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers1_bayonet | seven_branched_sword | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers1_bayonet | thunder_fire | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers1_bayonet | flame_shower | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers1_bayonet | sky_turner | propria | ok | ok | kn8:weapon.sword_heavy |
| jogador | numbers2_gauntlets | numbers2_force_blast | propria | ok | ok | kn8:weapon.axe_special |
| jogador | numbers2_gauntlets | barrage_blast | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers2_gauntlets | twin_blast | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers2_gauntlets | rising_blast | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers2_gauntlets | spiral_blast | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers2_gauntlets | thunder_emperor | propria | ok | ok | kn8:weapon.axe_special |
| jogador | numbers4_axe | numbers4_sky_dive | propria | ok | ok | kn8:weapon.axe_special |
| jogador | numbers4_axe | falling_thunder | propria | ok | ok | kn8:weapon.axe_special |
| jogador | numbers4_axe | water_skimmer | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers4_axe | half_moon | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers4_axe | rising_thunder | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers4_axe | twin_swallows | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers4_axe | daruma_dropper | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers4_axe | daruma_dropper_ten_hit | propria | ok | ok | kn8:weapon.axe_heavy |
| jogador | numbers6_rifle | numbers6_freeze_shot | propria | ok | ok | kn8:weapon.rifle_shot |
| jogador | sword | dash_cut | propria | ok | ok | kn8:weapon.sword_heavy |
| npc | hoshina | kuuchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina | kosa_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina | ran_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina | kasumi_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina | yae_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina_no10 | kuuchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina_no10 | kosa_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina_no10 | ran_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina_no10 | kasumi_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina_no10 | yae_uchi | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | hoshina_no10 | juni_hitoe | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | axe_slash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | heavy_swing | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | shockwave | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | dash_strike | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | ground_smash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | guard_break | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | falling_thunder | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | water_skimmer | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | half_moon | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | rising_thunder | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | twin_swallows | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru | daruma_dropper | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | high_speed_axe | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | dive_strike | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | vertical_assault | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | air_combo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | aerial_dash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | falling_thunder | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | water_skimmer | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | half_moon | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | rising_thunder | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | twin_swallows | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | daruma_dropper | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kikoru_no4 | daruma_dropper_ten_hit | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | mina | precision_shot | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | mina | cannon_shot | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | mina | charged_cannon | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | mina | anti_giant_shot | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno | precision_shot | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno | burst_fire | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno | freeze_round | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno | suppression | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno | butt_strike | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno_no6 | ice_spear | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno_no6 | freeze_burst | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno_no6 | ice_field | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno_no6 | multi_freeze | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | reno_no6 | auxiliary_cannons | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | thrust_combo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | lunge | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | bayonet_shot | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | sweeping_slash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | eruption_slash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | slash_salvo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | seven_branched_sword | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | thunder_fire | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | flame_shower | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi | sky_turner | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | thrust_combo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | lunge | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | bayonet_shot | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | sweeping_slash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | weak_point_thrust | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | predicted_counter | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | eruption_slash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | slash_salvo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | seven_branched_sword | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | thunder_fire | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | flame_shower | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | narumi_no1 | sky_turner | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kafka | punch_combo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kaiju_no8 | rush_combo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kaiju_no8 | heavy_punch | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kaiju_no8 | dash_strike | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kaiju_no8 | ground_smash | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kaiju_no8 | roar | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | kaiju_no8 | rupture_punch | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao | barrage_blast | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao | spiral_blast | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao | force_blast | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao | thunder_emperor | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao | twin_blast | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao | rising_blast | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao_base | jab_combo | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao_base | elbow_strike | ok | - | ok | kn8:weapon.sword_swing (inicio) |
| npc | isao_base | heavy_straight | ok | - | ok | kn8:weapon.sword_swing (inicio) |
