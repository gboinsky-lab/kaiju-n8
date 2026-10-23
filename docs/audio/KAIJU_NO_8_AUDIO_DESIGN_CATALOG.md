# Kaiju No. 8 — Catálogo de design de áudio

Planejamento da etapa **AUDIO-0** (2026-10-23, adendo do Miguel: alarmes de invasão e substituição dos sons
especiais). **Nenhum código mudou nesta etapa.** Os prompts estão em `ELEVENLABS_PROMPTS.md`, a lista de produção
em `AUDIO_PRODUCTION_CHECKLIST.md` e a ordem de implementação em `AUDIO_PRIORITY_PLAN.md`.

> Os quatro documentos não existiam no repositório (nenhum arquivo `*AUDIO*`/`*ELEVENLABS*` em nenhuma branch nem
> no histórico do Git): foram criados nesta etapa em `docs/audio/`. Se houver versões locais com outro conteúdo,
> elas precisam ser juntadas a estas.

## 1. Como a auditoria foi feita

- **Código:** todas as chamadas `playSound`/`playNotifySound`/`SimpleSoundInstance` em `src/main/java`, o registro
  `KN8Sounds`, os campos de som dos JSON (`weapon/*.json` → `sounds` e `special.sound`; `weapon_profile/*.json` →
  saque e etapas de recarga; `ability/*.json` → `sound`) e `assets/kn8/sounds.json`.
- **Arquivos:** os 66 `.ogg` em `assets/kn8/sounds/` foram decodificados e medidos (duração, RMS, pico, centroide
  espectral, frequência dominante por quarto do arquivo).
- **"Escutar":** esta máquina não tem saída de áudio. Para saber o que cada arquivo é de verdade, a medição foi
  cruzada com a função que o gerou em `tools/audio/gen_sounds.py` (todos os sons do mod são **síntese
  procedural** em numpy: ruído filtrado, senoides, envelopes; nenhum é gravação). **Limitação:** a avaliação de
  timbre ("parece machado", "parece soco") é pela receita da síntese e pelas medidas, não por audição; o Miguel deve
  ouvir os atuais antes de aprovar cada substituição (passo 0 do checklist).
- **Momento de reprodução:** para cada som, foi conferido em que ponto do código ele toca (início da ação, tick do
  impacto, acerto do projétil). Isso decide o que precisa ser dividido em etapas.

## 2. Estado atual (resumo)

| Item | Quantidade | Observação |
|---|---|---|
| Eventos de som do mod (`KN8Sounds`) | 27 | 7 de kaiju, 16 de arma, 2 do traje, 1 de carcaça, 1 de alerta |
| Arquivos `.ogg` | 66 | todos sintetizados por `gen_sounds.py` (placeholders) |
| Sons vanilla usados pelo mod | 24 chamadas | incluindo Warden, Ravager, Wither, Enderman, Creeper, bigorna, TNT, besta |
| Habilidades de kaiju (`ability/*.json`) | 58 | **todas** dividem só 4 sons: `kaiju.bite` (22), `kaiju.slam` (22), `kaiju.roar` (11), `weapon.pistol_shot` (3) |
| Técnicas de soldados especiais | 62 (13 perfis) | todas começam com `weapon.sword_swing` e todo tiro/corte/onda sai com `weapon.sword_heavy` |
| Projéteis de kaiju com som de saída ou de voo | 0 | o som da habilidade toca **só no impacto** (`AbilityEffects.play`) |

### Medições que mudam decisões

- **Sirene** (`alert/siren_1.ogg`): senoide com 2º e 3º harmônicos varrendo 650 ↔ 1000 Hz em ciclos de 1,5 s, 3 s,
  RMS −8,8 dB. É tocada com `playNotifySound` (só no ouvido de cada jogador, sem fonte no mundo) **a cada 100 ticks**
  durante o aviso e o intervalo entre ondas: cópias pontuais de 3 s com 2 s de silêncio, não um laço controlado.
  Não há som nenhum durante a luta (fase FIGHT).
- **Alarme de superaquecimento** (`suit/overheat_alarm_1.ogg`): **onda quadrada** alternando 1450/1100 Hz, RMS −6,8 dB
  — o timbre mais estridente do mod, tocado em repetição enquanto o traje está acima do limite.
- **Saturação:** tiros (`rifle_shot`, `pistol_shot`, pico até +1,8 dB), `guard_break` (+1,4), `axe_special` (+0,2),
  `kaiju_hurt` (+1,0) passam de 0 dBFS (cortam no Vorbis).
- **Variações iguais:** `axe_heavy_1/2`, `blade_heavy_1/2`, `dash_1/2`, `pistol_shot_1/2` e `kaiju_bite_2/3` têm o
  mesmo espectro dominante; as "variações" quase não variam.

## 3. Auditoria e substituição de sons existentes

Arquivo de destino = `src/main/resources/assets/kn8/sounds/<grupo>/<nome>_<n>.ogg` (evento `kn8:<grupo>.<nome>`).
"Local" = onde o som toca hoje (ou onde o novo deve tocar). IDs novos entre crases; prompts em
`ELEVENLABS_PROMPTS.md` com o mesmo código.

### 3.1 Invasões e alertas

| Evento do jogo | Som atual | Problema identificado | Novo som recomendado | Arquivo de destino | Local de reprodução |
|---|---|---|---|---|---|
| Invasão começa (fase WARNING) | `kn8:alert.siren` (síntese, varredura 650–1000 Hz, 3 s) por `playNotifySound` | Sirene genérica de síntese; sem fonte no mundo; o início não se distingue da repetição | A01 `alarm.invasion_start` (estalo de abertura) + A02 `alarm.siren_loop` | `alarm/invasion_start_*.ogg`, `alarm/siren_loop.ogg` | `InvasionService.start` (servidor) → cliente pelo `InvasionStateS2C` (fase WARNING) |
| Aviso e intervalo entre ondas (WARNING/BREAK) | o mesmo `kn8:alert.siren`, uma cópia a cada 100 ticks | Repetição por cópias soltas; nada garante que pare no meio quando a fase muda; não há fade | A02 `alarm.siren_loop` em laço controlado no cliente | `alarm/siren_loop.ogg` | `InvasionService.tick` (`SIREN_INTERVAL`) → substituir por gerenciador no cliente |
| Mensagem "ALERTA DE INVASÃO" no chat | nenhum | Alerta escrito sem sinal sonoro de interface | A03 `alarm.kaiju_detected` | `alarm/kaiju_detected.ogg` | `InvasionService.start` (junto do `kn8.invasion.alert`) |
| Onda começa (fase FIGHT) | `kn8:kaiju.roar` no centro, volume 6, tom 0,6 | Rugido genérico; igual para Yoju, Honju e numerado; não é um alerta da Força de Defesa | A04/A05/A06/A07 por classe do kaiju mais forte da onda (o rugido do kaiju pode continuar, mais baixo) | `alarm/class_yoju.ogg` etc. | `InvasionService.startWave` |
| Ondas 2+ (a situação piora) | só o mesmo rugido | Sem sinal de agravamento | A10 `alarm.threat_rise` | `alarm/threat_rise.ogg` | `InvasionService.startWave` com `wave > 0` |
| Chefe entra na onda (`wave.boss`) | `kn8:kaiju.roar` (BossService.spawn) | Sem alerta de ameaça crítica | A08 `alarm.critical_threat` | `alarm/critical_threat.ogg` | `InvasionService.startWave` (boss presente) |
| Kaiju de surpresa na onda (`chance` < 1, ex.: No. 9 no nível 6) | nenhum | A surpresa entra calada | A10 `alarm.threat_rise` (+ A07 se numerado) | — | `InvasionService.startWave` (quando o sorteio da surpresa passa) |
| Onda eliminada (BREAK) | nenhum (só texto verde) | Sem confirmação sonora | A12 `alarm.wave_cleared` | `alarm/wave_cleared.ogg` | `InvasionService.tick` (`wave_cleared`) |
| Invasão vencida | `minecraft:ui.toast.challenge_complete` | Som de conquista vanilla | A13 `alarm.all_clear` | `alarm/all_clear.ogg` | `InvasionService.finish` (VICTORY) |
| Invasão perdida (tempo esgotado ou `/kn8 invasion stop`) | nenhum | Fim sem som | A14 `alarm.invasion_lost` | `alarm/invasion_lost.ogg` | `InvasionService.finish` (DEFEAT, só com `rewards` = fim por tempo) |
| Luta em andamento (fase FIGHT) | nenhum | Sem lembrete ambiente de emergência | A09 `alarm.ongoing_bed` (laço baixo) | `alarm/ongoing_bed.ogg` | gerenciador no cliente (fase FIGHT) |
| Troca de fase do chefe (PM-5) | `kn8:kaiju.roar` | Mesmo rugido do resto | A15 `alarm.boss_phase` + rugido | `alarm/boss_phase.ogg` | `BossService.tick` (transição) |
| Superaquecimento do traje | `kn8:suit.overheat_alarm` (onda quadrada 1450/1100 Hz) | Estridente em repetição; cansativo | A16 `suit.overheat_warning` | `suit/overheat_warning.ogg` | `PowerService` (linha do `OVERHEAT_ALARM`) |
| Aviso de golpe pesado de kaiju (IA-6) | `minecraft:entity.warden.sonic_charge` | Som do Warden para todo kaiju | Preparo próprio por habilidade (seção 3.4) + K00 `kaiju.heavy_windup` como reserva | `kaiju/heavy_windup.ogg` | `KaijuTelegraph.start` |
| Kaiju se aproxima de área protegida/jogador | **mecânica não existe** | — | A11 `alarm.proximity` (reservado) | — | precisa de mecânica nova (ver plano) |
| Falha de comunicação | **mecânica não existe** | — | A17 `alarm.comm_failure` (reservado) | — | — |
| Evacuação | **mecânica não existe** (a barra diz "evacuar" só em texto; civis na PM-10) | — | A18 `alarm.evacuation` (reservado para a PM-10) | — | — |
| Daikaiju detectado | **nenhuma espécie é Daikaiju** (a classe existe em `KaijuClass`, nenhum `kaiju/*.json` a usa) | — | A06 `alarm.class_daikaiju` (produzir já, ligar quando houver Daikaiju) | `alarm/class_daikaiju.ogg` | `startWave` |

### 3.2 Armas do jogador e especiais de arma

| Evento do jogo | Som atual | Problema identificado | Novo som recomendado | Arquivo de destino | Local de reprodução |
|---|---|---|---|---|---|
| Golpe Sísmico do machado (R) | `kn8:weapon.axe_special` (síntese, 1,8 s, satura) | Um arquivo só para preparo + impacto; satura | B01/B02 `weapon.axe_seismic_windup` + `axe_seismic_impact` | `weapon/axe_seismic_*.ogg` | `SpecialAttacks` (`ground_slam`): preparo no começo, impacto no tick do JSON |
| Corte em avanço da espada (R, PM-1) | `kn8:weapon.sword_heavy` | Reaproveita o golpe pesado comum | B03 `weapon.sword_dash_cut` | `weapon/sword_dash_cut_*.ogg` | `SpecialAttacks` (`lunge`) |
| Sequência de cortes da faca (R, PM-1) | `kn8:weapon.knife_heavy` | Reaproveita a estocada pesada | B04 `weapon.knife_flurry` (4 variações, um por corte) | `weapon/knife_flurry_*.ogg` | fila do `flurry` no `KN8Server` (cada corte) |
| Kūuchi da espada do Hoshina do jogador (R) | `kn8:weapon.sword_heavy` | Mesmo som do golpe pesado | C-HOS-01 `hoshina.kuuchi_release` (o mesmo do NPC) | `hoshina/kuuchi_*.ogg` | `SpecialAttacks` (`slash_wave`) |
| Tiro preciso (clique direito com arma de fogo, PM-1) | o mesmo `weapon.rifle_shot`/`pistol_shot` do tiro comum | O especial não se distingue do comum | B07/B08 `weapon.rifle_precise_shot`, `pistol_precise_shot` | `weapon/*_precise_shot_*.ogg` | `CombatService.weaponSound(..., "shot")` com ação `heavy` |
| Tiro comum do rifle e da pistola | síntese, pico até +1,8 dB | Saturam; base contra a qual os especiais precisam soar diferentes | B05/B06 (refazer, prioridade menor) | `weapon/rifle_shot_*.ogg`, `pistol_shot_*.ogg` | `CombatService` |
| Canhão da Mina (tiro comum do jogador e do NPC) | `kn8:weapon.rifle_shot` (`mina_cannon.json`) | Canhão de quadril soando como rifle | C-MIN-01 `mina.cannon_shot` | `mina/cannon_shot_*.ogg` | `weapon/mina_cannon.json` → `sounds.shot` |
| Socos do No. 8, do Isao e da manopla Numbers 2 | `weapon.axe_swing/axe_heavy/axe_hit` (`kaiju_no8_fist`, `isao_fist`, `numbers2_gauntlet`) | Soco com som de machado | C-K8-01..03 e C-ISA-01..03 (sopro, golpe, acerto de punho) | `no8/fist_*.ogg`, `isao/fist_*.ogg`, `isao/gauntlet_*.ogg` | `weapon/<punho>.json` → `sounds` |
| Baioneta do Narumi | `weapon.sword_swing/heavy/hit` | Estocada de baioneta com som de espada | C-NAR-01..03 | `narumi/bayonet_*.ogg` | `weapon/narumi_bayonet.json` |
| Recarga do rifle e da pistola | `minecraft:item.crossbow.loading_start/middle/end` | Besta vanilla para arma de fogo | B09/B10 `weapon.rifle_reload_*`, `pistol_reload_*` | `weapon/rifle_reload_*.ogg` | `weapon_profile/rifle.json`/`pistol.json` (etapas) |
| Carregar pente com botão direito | `minecraft:item.crossbow.loading_middle` | Besta vanilla | B11 `weapon.magazine_load` | `weapon/magazine_load.ogg` | `MagazineItem` |
| Saque das armas | `minecraft:item.armor.equip_chain/iron` | Aceitável (não é especial) | manter; refazer só no fim (P4) | — | `weapon_profile/*.json` |
| Esquiva perfeita (PM-1) | nenhum (só HUD e efeito) | O momento mais recompensador do combate sem som | B12 `combat.perfect_dodge` | `combat/perfect_dodge.ogg` | `CombatEvents` (`PERFECT_DODGE`) |
| Esquiva/dash, aparar, guarda quebrada | sons próprios de síntese | Próprios e coerentes; `guard_break` satura | manter (refazer `guard_break` com P4) | — | `CombatService` |

### 3.3 Soldados especiais (técnicas)

| Evento do jogo | Som atual | Problema identificado | Novo som recomendado | Arquivo de destino | Local de reprodução |
|---|---|---|---|---|---|
| Início de **toda** técnica de especial (preparo) | `kn8:weapon.sword_swing` fixo (`HoshinaEntity.startTechnique`) | O mesmo assobio de espada para machado, canhão, rifle, soco e lança | Preparo/carga por técnica (seção 5) | `<personagem>/<tecnica>_windup.ogg` | `HoshinaEntity.startTechnique` (junto do VFX `_charge`) |
| Disparo de técnica a distância (corte, tiro, onda) | `kn8:weapon.sword_heavy` fixo (`HoshinaEntity.fireHit`) | **Tiros da Mina, do Reno, do Narumi, ondas do Isao e do No. 8 soam como espada** | Disparo próprio por técnica | `<personagem>/<tecnica>_release.ogg` | `HoshinaEntity.fireHit` (tick do golpe do JSON) |
| Acerto de golpe de técnica | `sounds.hit` da arma (`axe_hit` nos socos) | Socos com acerto de machado | Acerto por arma/personagem | `<personagem>/*_hit.ogg` | `HoshinaEntity.strike` |
| Explosão de tiro de técnica (Mina, Reno No. 6, No. 8) | `minecraft:entity.generic.explode` | TNT vanilla | Explosão própria por técnica | `<personagem>/<tecnica>_explosion.ogg` | `SlashProjectile` (explosão) |
| Guarda da cauda do Hoshina No. 10 | `minecraft:block.anvil.land` | Bigorna | C-HOS-09 `hoshina.tail_guard` | `hoshina/tail_guard.ogg` | `HoshinaNo10Entity` (guarda) |
| Troca de forma Kafka ↔ No. 8 | `minecraft:entity.ravager.roar` | Ravager vanilla | C-K8-08/09 `no8.transform` e `no8.revert` | `no8/transform.ogg` | `HoshinaEntity.transformInto` |
| Contra-ataque e aparar dos especiais | `kn8:weapon.parry` | Próprio, igual para todos | manter (P4: variante por arma) | — | `HoshinaEntity` |
| Esquiva/avanço dos especiais | `kn8:suit.dash` | Próprio | manter | — | `HoshinaEntity` |

### 3.4 Kaiju

| Evento do jogo | Som atual | Problema identificado | Novo som recomendado | Arquivo de destino | Local de reprodução |
|---|---|---|---|---|---|
| 22 habilidades: mordida, garra, patas, estocada, rabada de ponta, apêndices, agarrão, lâmina, tentáculo, **jato de ácido (3)**, **espinhos**, **teia (2)**, **esporos**, ataque com cauda do Preondactyl | `kn8:kaiju.bite` (síntese, 0,4 s) | Projéteis e jatos com som de mordida; tudo igual entre espécies | Som próprio por habilidade (seção 6) | `kaiju/<habilidade>_*.ogg` | `AbilityEffects.play` (impacto) + saída no `KaijuAbilities.fire` |
| 22 habilidades: pisão, soco pesado, salto, rabada, esmagamento, **Finger Cannon (2)**, **Thunder Emperor da forma com o Isao**, **voo curto**, **explosão de espinhos**, mergulho | `kn8:kaiju.slam` (síntese, 1,6 s) | Canhão de energia e soco único com som de pisão | Som próprio por habilidade | idem | idem |
| 11 habilidades: investida, **raio de energia do Honju (2)**, **sopro atômico do Diclonius**, **raio do Preondactyl**, **raio híbrido da fusão**, **bomba de esporos**, **ondas do No. 9 com o Isao (2)**, corridas | `kn8:kaiju.roar` (rugido) | Raios e ondas com som de rugido, tocado no **impacto** | Carga + disparo + voo + impacto | idem | idem |
| Finger Gun, Multi-Finger Gun, Multi-Finger Cannon (No. 9 e formas) | `kn8:weapon.pistol_shot` | Tiro de pistola humana, e só onde a bala bate (sem estalo na mão do No. 9) | D-N9-01..04 (estalo + bala supersônica + impacto) | `no9/finger_gun_*.ogg` | `KaijuAbilities.fire` (saída) + `KaijuProjectile` (impacto) |
| Saída de projétil de kaiju | **nenhum** | O tiro sai calado; o som chega só quando acerta | `<habilidade>_release` | — | `KaijuAbilities.fire` |
| Voo de projétil de kaiju | **nenhum** | — | `<habilidade>_travel` só onde faz sentido (raios longos, bomba) | — | som preso à entidade `KaijuProjectile` no cliente |
| Autodestruição do Preondactyl | `creeper.primed` + `generic.explode` | Creeper e TNT vanilla | D-PRE-04/05 | `preondactyl/self_destruct_*.ogg` | `PreondactylEntity` |
| No. 9: absorção, reviver, ressurreição em massa, fuga, retirada | `kaiju.ambient`, `block.anvil.land`, `evoker.prepare_summon`, `wither.spawn`, `enderman.teleport`, `enderman.scream` | Mistura de mobs vanilla | D-N9-10..15 | `no9/*.ogg` | `No9Service` (cada evento) |
| Rugido, rosnado, dano, morte, passos | `kaiju.roar/ambient/hurt/death/step` (síntese, sem diferença por espécie além do tom) | Fora do escopo deste adendo (não são ataques especiais) | manter; ver P4 | — | `KaijuEntity` |

## 4. Identidade sonora

| Família | Paleta de áudio | Regra |
|---|---|---|
| Força de Defesa (interface, alarmes) | eletrônico militar limpo, tons sintéticos com ataque definido, salas grandes de concreto, rádio | nada de música nem voz; dois alarmes nunca com o mesmo ritmo |
| Armas convencionais (rifle, pistola, faca, espada) | mecânica real: estampido, mecanismo, metal | o especial sempre tem uma camada que o comum não tem (carga, estalo supersônico, cauda longa) |
| Equipamento numerado (feito de kaiju: Numbers 1, 2, 4, 6, 10) | metal + matéria orgânica: zumbido grave vivo, rangido de carapaça, pulso como batimento | toda arma numerada tem um "batimento" sob o som mecânico |
| Kaiju Yoju | orgânico, úmido, quitina, grunhidos curtos | menor = mais agudo e mais curto |
| Kaiju Honju | o mesmo com corpo maior: sub-grave, peso, eco | cauda de 1–2 s no impacto |
| Numerados (No. 8, 9, 10, 13) | o orgânico com algo errado: estalos "digitais" de carne, distorção, pressão de ar | o No. 9 é frio e preciso; o No. 10 bruto; o No. 8 pressão de ar e trovão grave |

Cores do VFX que guiam o timbre (decisões do Miguel): Hoshina violeta e lâmina preta (cortes secos, finos);
Hoshina No. 10 vermelho-arroxeado (cauda viva); Kikoru dourada (peso, rocha); Mina verde (canhão grave,
devastador); Reno gelo; Narumi rosa (precisão, agulhas); Isao laranja (ar comprimido; Thunder Emperor **sem raio**,
soco único com toda a força); Finger Gun cinza (balas em velocidade extrema); raio do Preondactyl laranja; ácido
verde.

## 5. Ataques especiais dos soldados — composição por etapas

Etapas: **P** preparo, **C** carga, **E** execução (disparo/golpe), **I** impacto, **S** consequência. "—" = a etapa
está combinada com a anterior. Os códigos remetem a `ELEVENLABS_PROMPTS.md`.

| Personagem | Técnica | P | C | E | I | S |
|---|---|---|---|---|---|---|
| Hoshina | Kūuchi (corte que voa) | C-HOS-01a | — | C-HOS-01b | C-HOS-02 (corte em carne) | — |
| Hoshina | Kōsa-uchi (X) | C-HOS-03 | — | C-HOS-03 | C-HOS-02 | — |
| Hoshina | Ran-uchi (10 golpes) | — | — | C-HOS-04 (4 variações por golpe) | C-HOS-02 | — |
| Hoshina | Kasumi-uchi (X duplo + baixo) | — | — | C-HOS-05 | C-HOS-02 | — |
| Hoshina | Yae-uchi (8 em estrela) | C-HOS-06a | — | C-HOS-06b (8 variações) | C-HOS-02 | — |
| Hoshina No. 10 | Jūni-hitoe (12 golpes, o último forte) | C-HOS-07a | C-HOS-07a | C-HOS-04 ×11 + C-HOS-07b | C-HOS-02 | C-HOS-07c |
| Hoshina No. 10 | Cauda (corte) / guarda da cauda / Full Release | C-HOS-08 / C-HOS-09 / C-HOS-10 | | | | |
| Kikoru | axe_slash, heavy_swing | C-KIK-01 | — | C-KIK-02 | C-KIK-03 | — |
| Kikoru | shockwave (onda no chão) | C-KIK-04a | — | C-KIK-04b | — | C-KIK-04c (pedras) |
| Kikoru | dash_strike | C-KIK-05 | — | C-KIK-05 | C-KIK-03 | — |
| Kikoru | ground_smash (cratera) | C-KIK-06a | — | C-KIK-06b | C-KIK-06b | C-KIK-06c |
| Kikoru | guard_break | — | — | C-KIK-07 | C-KIK-07 | — |
| Kikoru No. 4 | dive_strike, vertical_assault, air_combo, aerial_dash, high_speed_axe, asas | C-KIK-08..12 | | | | |
| Mina | precision_shot | — | — | C-MIN-02 | C-MIN-06 | — |
| Mina | cannon_shot | — | — | C-MIN-01 | — | C-MIN-07 (explosão) |
| Mina | charged_cannon | C-MIN-03a | C-MIN-03a | C-MIN-03b | — | C-MIN-07 |
| Mina | anti_giant_shot | C-MIN-04a | C-MIN-04a | C-MIN-04b | C-MIN-04c (voo) | C-MIN-08 (explosão grande) |
| Reno | precision_shot, burst_fire, suppression, butt_strike | — | — | C-REN-01..03, C-REN-05 | C-REN-06 | — |
| Reno | freeze_round | C-REN-04a | — | C-REN-04b | C-REN-04c (congelamento) | — |
| Reno No. 6 | ice_spear | C-REN-07a | C-REN-07a | C-REN-07b | C-REN-07c | — |
| Reno No. 6 | freeze_burst | — | — | C-REN-08a | — | C-REN-08b (estilhaços) |
| Reno No. 6 | ice_field, multi_freeze, auxiliary_cannons | C-REN-09..11 | | | | |
| Narumi | thrust_combo, lunge, sweeping_slash | — | — | C-NAR-01..04 | C-NAR-03 | — |
| Narumi | bayonet_shot | — | — | C-NAR-05 | C-REN-06 | — |
| Narumi No. 1 | weak_point_thrust, predicted_counter, previsão | C-NAR-06..08 | | | | |
| Isao | barrage_blast | C-ISA-04a | — | C-ISA-04b (5) | C-ISA-03 | — |
| Isao | spiral_blast | C-ISA-05a | — | C-ISA-05b | C-ISA-05c (voo) | C-ISA-05d |
| Isao | force_blast | C-ISA-06a | — | C-ISA-06b | — | C-ISA-06c |
| Isao | Thunder Emperor | C-ISA-07a | C-ISA-07a | C-ISA-07b | C-ISA-07b | C-ISA-07c |
| Isao sem o Numbers 2 | jab_combo, elbow_strike, heavy_straight | — | — | C-ISA-01/02 | C-ISA-03 | — |
| Kafka | punch_combo; rifle | — | — | C-K8-10 | C-K8-03 | — |
| Kaiju No. 8 | rush_combo, heavy_punch, dash_strike | — | — | C-K8-01/02/04 | C-K8-03 | — |
| Kaiju No. 8 | ground_smash | C-K8-05a | — | C-K8-05b | C-K8-05b | C-K8-05c |
| Kaiju No. 8 | roar (técnica) | — | — | C-K8-06 | — | — |
| Kaiju No. 8 | regeneração / transformação / volta ao Kafka | C-K8-07 / C-K8-08 / C-K8-09 | | | | |

## 6. Kaiju — habilidades com som próprio

Mesma regra de etapas. O **preparo** substitui o Warden do aviso de golpe pesado (`KaijuTelegraph`); habilidades
que não são pesadas não têm preparo sonoro (o rosnado comum já serve). Lista completa com prompts na Parte D de
`ELEVENLABS_PROMPTS.md`.

| Família | Habilidades | Som próprio |
|---|---|---|
| Primigenius (4 espécies) | slam, heavy_punch, tail_swipe, charge, energy_blast(_revived), hooved_strike, claw, bite, leap | D-PRI-01..09 (ressurgido/revivido: a mesma base com camada de vapor, D-PRI-10) |
| Aranhas e formigas | bite, leg_stab, leg_swipe, multi_leg, web_shot, web_burst, leap, camponotus_acid_spray(_reborn) | D-ARA-01..06, D-FOR-01..02 |
| Philinosoma, Diclonius | philinosoma_spine_shot, diclonius_atomic_breath | D-PHI-01, D-DIC-01..03 |
| Cogumelos | phaneroplasmodium_spore_shot, myxogasterocarp_spore_bomb | D-FUN-01..03 |
| Preondactyl, larva | preondactyl_energy_beam, dive_strike, tail_strike, autodestruição | D-PRE-01..05 |
| No. 9 e formas | finger_gun, multi_finger_gun, multi_finger_cannon, blade_arm, tendril, spike_burst, short_flight, hybrid_beam, tail_barrage, ant_*, isao_* | D-N9-01..15 |
| No. 10 (pequeno e gigante) | finger_cannon, heavy_punch, heavy_smash, multi_appendage, tail_stab, tail_sweep, tail_smash | D-N10-01..07 (gigante = mesmos arquivos, tom mais grave pelo código) |
| No. 13 | flurry, heavy_punch, pounce, sprint, throat_grab | D-N13-01..05 |

## 7. Regras de sincronização (valem para a implementação)

1. **O servidor decide o momento; o som segue o evento, não a animação.** Disparo = tick do golpe do JSON
   (`fireHit`/`strike` nos especiais, impacto da habilidade nos kaiju, `KaijuAbilities.fire` na saída do projétil).
   Nunca tocar o disparo no `triggerAnim`.
2. **Preparo/carga** toca no início do preparo (mesmo momento do VFX `_charge` e do aviso `KaijuTelegraph`), com
   duração igual ou menor que o preparo; se a técnica for cancelada (`cancelTechnique`, alvo morto,
   `shotInvalid`), a carga é interrompida (som com fim curto ou parado pelo cliente).
3. **Impacto** toca onde o golpe ou o projétil acerta (o lugar já vem do código: `AbilityEffects.play(where)`,
   `SlashProjectile` no acerto).
4. **Voo** só para projéteis lentos ou longos o bastante para serem ouvidos passando (bomba de esporos, lança de
   gelo, tiro Anti-Giant, raios contínuos): som preso à entidade no cliente (`EntityBoundSoundInstance`), que para
   sozinho quando a entidade some.
5. **Golpes de vários acertos** (Ran-uchi, Yae-uchi, Jūni-hitoe, rajadas): um arquivo por golpe, sorteado entre as
   variações, com tom levemente diferente a cada golpe (o código já faz isso com o `pitch`).
6. **Animação só para som cosmético** (asas da Kikoru No. 4, passos): marca de som da GeckoLib
   (`setSoundKeyframeHandler`) no cliente — nunca para dano.
7. **Alarmes contínuos**: uma instância por invasão no cliente, iniciada e parada pela fase recebida no
   `InvasionStateS2C` (seção 4 do `AUDIO_PRIORITY_PLAN.md`).
