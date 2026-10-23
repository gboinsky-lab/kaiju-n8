# Kaiju No. 8 — Prompts para o ElevenLabs (efeitos sonoros)

Etapa **AUDIO-0** (2026-10-23). Cada efeito tem: arquivo, evento, som que substitui, duração, tipo, variações,
dependência de animação, laço, prioridade e volume/distância, e o prompt pronto. Códigos iguais aos do
`KAIJU_NO_8_AUDIO_DESIGN_CATALOG.md`.

## Como gerar

- **Ferramenta:** ElevenLabs → *Sound Effects*. Prompts em inglês (o gerador entende melhor).
- **Duração:** usar a coluna "Duração" no campo de duração. **Prompt influence:** 0,6 por padrão; 0,8 nos alarmes (o
  ritmo precisa ser seguido). **Laço:** os marcados "laço" usam a opção de loop do gerador, se a conta tiver
  [VERIFICAR na interface: o recurso e o limite de duração mudam com o modelo]; sem ela, gerar mais longo e cortar o
  laço no editor (passo do checklist).
- **Gerar 4 candidatos por prompt** e guardar os 2 melhores quando a coluna pedir variações.
- **Todos os prompts terminam com** "no music, no voice, no speech" — já incluído no texto. Os alarmes nunca têm voz.
- **Arquivo final:** `src/main/resources/assets/kn8/sounds/<grupo>/<nome>_<n>.ogg`, OGG Vorbis **mono** (som no mundo,
  com posição) ou estéreo só para os sons de interface tocados no ouvido (`playNotifySound`/UI). Números `_1`, `_2`…
  por variação.
- **Prioridade:** P1 alarmes de invasão; P2 tiros especiais; P3 ataques especiais (soldados) e habilidades de kaiju
  mais vistas; P4 o resto. Detalhe em `AUDIO_PRIORITY_PLAN.md`.
- **Volume/distância:** no Minecraft o alcance padrão é 16 blocos x volume da chamada; os números abaixo são o alvo
  de alcance para a implementação (via volume da chamada ou `attenuation_distance` no `sounds.json`).

Legenda de tipo: **pontual** (toca uma vez), **repetitivo** (curto, tocado várias vezes pelo jogo), **contínuo** (laço
controlado por início/fim).

### Valores padrão das entradas em tabela (Partes C e D)

Quando a linha da tabela não diz outra coisa, vale:

| Campo | Padrão | Exceções |
|---|---|---|
| Tipo | pontual | golpes de combo/rajada (C-HOS-04, C-HOS-06b, C-ISA-04b, C-KIK-11, B04, D-N13-01): repetitivo |
| Variações | a coluna "Var." (Parte C); 2 na Parte D | "4 var." quando escrito |
| Depende de animação | **não**: toca no evento do servidor (preparo, tick do golpe, acerto) | C-KIK-08 (batida da asa) e passos: marca de som da animação no cliente |
| Laço | não | voos (C-MIN-04c, C-ISA-05c), feixes longos (D-DIC-02), aviso de autodestruição (D-PRE-04): laço preso à entidade, para quando ela some ou a habilidade acaba |
| Prioridade | Parte C: P2 para tiros e disparos especiais (Mina, Reno, Reno No. 6, Narumi `bayonet_shot`, Isao `spiral`/`force`, No. 8 `ground_smash`, Kūuchi), P3 para golpes corpo a corpo; Parte D: P3 para raios, tiros, ácido, teia e Finger Gun; P4 para golpes corpo a corpo de kaiju | — |
| Volume/distância | golpe de personagem: alcance 24; disparo de arma de fogo: 48–64; canhão/explosão: 80–128 (por linha); kaiju Yoju 32, Honju 48, numerado gigante 64 (o código soma pelo tamanho) | interface (B12, alarmes): no ouvido, sem posição |

---

## Parte A — Alarmes e alertas de invasão (P1)

Família sonora: eletrônico militar limpo da Força de Defesa, salas de concreto, alto-falantes de cidade. Cada alarme
tem um ritmo e um timbre próprios (tabela de diferenciação no fim da parte). Nenhum com voz ou música.

### A01 · `alarm/invasion_start` — Invasão iniciada (abertura, curta)
- **Evento:** `InvasionService.start` (fase WARNING), uma vez por jogador na área. **Substitui:** a primeira cópia de
  `kn8:alert.siren`. **Duração:** 2,5 s. **Tipo:** pontual. **Variações:** 1. **Animação:** não. **Laço:** não.
  **Volume:** interface (no ouvido), −14 LUFS; seguido do A02.
```
Military emergency activation sting for a futuristic anti-monster defense force: a heavy mechanical relay clunk, then a
fast rising electronic klaxon swell that peaks into two sharp, wide, bright alarm blasts, ending with a short
low concrete-room reverb tail. Urgent and powerful but not shrill, clean sci-fi military tone, no music, no voice,
no speech.
```

### A02 · `alarm/siren_loop` — Sirene de invasão (ambiente, contínua)
- **Evento:** fases WARNING e BREAK da invasão (cliente, pela fase do `InvasionStateS2C`). **Substitui:** as cópias
  de `kn8:alert.siren` a cada 100 ticks. **Duração:** 8 s (laço). **Tipo:** contínuo. **Variações:** 1 (+ versão
  distante A02d, abaixo). **Animação:** não. **Laço:** sim, ponto de corte sem clique. **Volume:** posicional no
  centro da invasão, alcance 96 blocos (área ×2), −20 LUFS; fade de 1,5 s ao parar.
```
Seamless looping city-wide civil defense siren for a giant monster attack: a large rotating electro-mechanical siren,
slow wail rising over about two seconds and falling over two seconds, twice per loop, with a faint synthetic
undertone, heard from a few hundred meters away across a city with soft echoes off buildings. Steady, ominous,
not piercing. Loopable, no music, no voice, no speech.
```
- **A02d `alarm/siren_far`** (mesmo laço, só a camada distante para quem está longe do centro; opcional P4):
```
The same slow rotating city defense siren heard from very far away, muffled by distance and buildings, low-pass
filtered, long reflections, quiet and unsettling, seamless loop, no music, no voice, no speech.
```

### A03 · `alarm/kaiju_detected` — Kaiju detectado (interface)
- **Evento:** `InvasionService.start`, junto da mensagem "ALERTA DE INVASÃO" no chat (também nas invasões naturais
  e dos locais C3). **Substitui:** nada (hoje é só texto). **Duração:** 1,4 s. **Tipo:** pontual. **Variações:** 1.
  **Laço:** não. **Volume:** interface, −16 LUFS.
```
Short electronic detection signal from a military monster-tracking radar: a quick sonar-like scan sweep followed by
three ascending crisp digital pings, the last ping held slightly longer with a subtle lock-on chirp. Precise,
high-tech, clearly a notification, no music, no voice, no speech.
```

### A04 · `alarm/class_yoju` — Alerta de Yoju
- **Evento:** `startWave` quando o kaiju mais forte da onda é Yoju. **Substitui:** o `kaiju.roar` de início de onda
  (o rugido pode ficar baixo por baixo). **Duração:** 1,6 s. **Tipo:** pontual. **Variações:** 1. **Volume:**
  interface, −16 LUFS.
```
Medium-priority military threat alert tone: two rapid pairs of alternating high-low electronic chirps, bright and
clean, like a tactical system flagging small hostile creatures, with a light metallic click at the start. Alert but
not panicked, no music, no voice, no speech.
```

### A05 · `alarm/class_honju` — Alerta de Honju
- **Evento:** `startWave` quando o kaiju mais forte da onda é Honju (ou chefe Honju). **Substitui:** o
  `kaiju.roar` de início de onda. **Duração:** 2,8 s. **Tipo:** pontual. **Variações:** 1. **Volume:** interface,
  −15 LUFS.
```
Serious high-priority military alarm for a large monster: two long, low, brass-like electronic horn pulses with a deep
sub-bass thump under each one, a slight detuned edge, and a heavy concrete-bunker reverb. Grave, weighty, conveys
danger and scale, no music, no voice, no speech.
```

### A06 · `alarm/class_daikaiju` — Alerta de Daikaiju (reservado)
- **Evento:** `startWave` com Daikaiju. **Hoje nenhuma espécie é Daikaiju**: produzir e deixar pronto. **Duração:**
  4 s. **Tipo:** pontual. **Volume:** interface, −14 LUFS.
```
Maximum-priority catastrophic threat alarm: an enormous air-raid horn blast that slowly descends in pitch, layered
with a dense pulsing low drone and a distant rumbling pressure wave, ending in a short silence-like tail. Sounds like
the end of a city is coming. Unique and unmistakable, no music, no voice, no speech.
```

### A07 · `alarm/class_numbered` — Alerta de kaiju numerado (No. 9, 10, 13 e formas)
- **Evento:** `startWave` com kaiju da classe `numbered` (inclui a surpresa do No. 9 no nível 6). **Substitui:**
  o rugido de início de onda. **Duração:** 3 s. **Tipo:** pontual. **Volume:** interface, −15 LUFS.
- No jogo o numerado é a ameaça máxima que existe; este alerta não deve soar como os de classe: é uma anomalia.
```
Anomalous threat alert for an intelligent numbered monster: a clean military alarm tone that suddenly glitches and
distorts, stutters digitally, with a slow heavy heartbeat-like sub pulse underneath and a cold high whine. Unnatural,
eerie, something is very wrong, no music, no voice, no speech.
```

### A08 · `alarm/critical_threat` — Ameaça crítica
- **Evento:** `startWave` quando a onda traz chefe (`wave.boss`) ou a invasão é de nível 5–6. **Substitui:** nada
  (hoje o chefe só ruge). **Duração:** 3 s. **Tipo:** pontual. **Volume:** interface, −14 LUFS.
```
Critical threat warning: a fast strobing tri-tone electronic alarm that climbs in pitch over three seconds, each cycle
slightly faster and higher, with a hard metallic clank at the start and a sharp cutoff at the end. Intense urgency,
command-center emergency, not ear-piercing, no music, no voice, no speech.
```

### A09 · `alarm/ongoing_bed` — Invasão em andamento (ambiente)
- **Evento:** fase FIGHT (cliente, pelo `InvasionStateS2C`). **Substitui:** nada (hoje a luta não tem lembrete).
  **Duração:** 20 s (laço). **Tipo:** contínuo, muito baixo. **Laço:** sim. **Volume:** posicional no centro,
  alcance 96, −28 LUFS (cama de fundo); não pode cobrir os sons de combate.
```
Seamless looping background ambience of an ongoing monster emergency in a city: very distant overlapping sirens,
occasional far-off muffled explosions, faint radio squelch bursts without words, wind between buildings. Low,
tense, distant, sits under combat sounds, no music, no voice, no speech.
```

### A10 · `alarm/threat_rise` — Nível de ameaça subiu
- **Evento:** `startWave` com `wave > 0`, e quando um kaiju de surpresa (`chance` < 1) entra. **Substitui:** nada.
  **Duração:** 1,8 s. **Tipo:** pontual. **Volume:** interface, −16 LUFS.
```
Escalation stinger for a military tactical system: three stepped rising synth tones, each one heavier, ending in a
deep sub-bass drop and a short metallic shimmer. Communicates that the situation just got worse, no music, no voice,
no speech.
```

### A11 · `alarm/proximity` — Alarme de aproximação (reservado)
- **Mecânica não existe** (nenhum aviso de kaiju se aproximando de área protegida ou do jogador). Produzir só se a
  mecânica for aprovada (plano, item 8). **Duração:** 2 s (laço de cadência variável). **Tipo:** repetitivo.
```
Proximity warning beeper for an approaching giant creature: short dry electronic ticks that get closer together like a
parking sensor mixed with a Geiger counter, cold and technical, loopable, no music, no voice, no speech.
```

### A12 · `alarm/wave_cleared` — Onda eliminada
- **Evento:** fim da onda (fase BREAK, mensagem `kn8.invasion.wave_cleared`). **Substitui:** nada. **Duração:**
  1 s. **Tipo:** pontual. **Volume:** interface, −18 LUFS.
```
Short positive confirmation signal from a military command system: two clean ascending electronic tones with a soft
mechanical latch click, calm and reassuring, brief, no music, no voice, no speech.
```

### A13 · `alarm/all_clear` — Invasão encerrada (vitória)
- **Evento:** `InvasionService.finish` com VICTORY. **Substitui:** `minecraft:ui.toast.challenge_complete`.
  **Duração:** 3,5 s. **Tipo:** pontual. **Volume:** interface, −16 LUFS.
```
All-clear signal after a monster attack is over: one long steady civil defense tone that slowly settles and fades,
followed by a gentle three-note electronic chime of relief, with distant wind. Calm, final, dignified, no music, no
voice, no speech.
```

### A14 · `alarm/invasion_lost` — Invasão não contida
- **Evento:** `finish` com DEFEAT por tempo esgotado (não no `/kn8 invasion stop`). **Substitui:** nada (hoje
  silêncio). **Duração:** 3 s. **Tipo:** pontual. **Volume:** interface, −16 LUFS.
```
Failure signal of a military defense system: a detuned electronic tone that slowly sinks in pitch and loses power,
with crackling radio static and a low distant rumble at the end. Somber, defeat, no music, no voice, no speech.
```

### A15 · `alarm/boss_phase` — Chefe mudou de fase (PM-5)
- **Evento:** `BossService.tick` na troca de fase (junto do rugido e do aviso escrito). **Substitui:** fica junto do
  rugido. **Duração:** 1,5 s. **Tipo:** pontual. **Volume:** interface, −16 LUFS.
```
Combat analysis alert: a sharp metallic warning ping followed by a quick descending digital tone and a low impact
thud, like a tactical computer reporting that a giant monster changed its behavior. Brief and tense, no music, no
voice, no speech.
```

### A16 · `suit/overheat_warning` — Traje superaquecendo
- **Evento:** `PowerService` (acima do limite do traje). **Substitui:** `kn8:suit.overheat_alarm` (onda quadrada).
  **Duração:** 1,2 s. **Tipo:** repetitivo (o código já repete). **Variações:** 1. **Volume:** só do jogador,
  −20 LUFS; precisa ser suportável em repetição.
```
Combat suit overheat warning inside a helmet: a soft pulsing two-tone electronic warning, rounded and muffled rather
than sharp, with a hiss of venting heat and a faint metallic creak of armor plates. Urgent but not irritating when
repeated, no music, no voice, no speech.
```

### A17 · `alarm/comm_failure` — Falha de comunicação (reservado)
- **Mecânica não existe.** **Duração:** 1,5 s. **Tipo:** pontual.
```
Military radio communication failure: a burst of harsh static, a broken digital handshake stutter, and a low error buzz
that cuts off abruptly, no music, no voice, no speech.
```

### A18 · `alarm/evacuation` — Evacuação (reservado para a PM-10)
- **Mecânica não existe** (civis e evacuação são da PM-10; hoje a barra só escreve "evacuar"). **Duração:** 6 s
  (laço). **Tipo:** contínuo.
```
Urgent evacuation alarm for civilians: a fast alternating high-low two-tone emergency signal from street speakers,
steady rhythm, clearly different from an air-raid siren, slightly echoing between buildings, loopable, no music, no
voice, no speech.
```

### Diferenciação dos alarmes

| Código | Forma | Ritmo | Faixa | Duração |
|---|---|---|---|---|
| A01 | relê + subida + 2 rajadas | 1 vez | médio-agudo | 2,5 s |
| A02 | sirene rotativa | 2 ciclos lentos por laço | médio | laço 8 s |
| A03 | varredura + 3 pings subindo | rápido | agudo | 1,4 s |
| A04 | pares alto-baixo | rápido | agudo | 1,6 s |
| A05 | 2 buzinas graves longas + sub | lento | grave | 2,8 s |
| A06 | buzina de ataque aéreo descendo | 1 vez, longa | muito grave | 4 s |
| A07 | tom limpo que falha + batimento | irregular | médio + sub | 3 s |
| A08 | tri-tom acelerando e subindo | acelerando | sobe | 3 s |
| A09 | ambiente distante | sem pulso definido | fundo | laço 20 s |
| A10 | 3 degraus + queda | médio | sobe e cai | 1,8 s |
| A12 | 2 tons subindo | curto | médio | 1 s |
| A13 | tom longo + 3 notas | lento | médio | 3,5 s |
| A14 | tom desafinando para baixo | lento | cai | 3 s |
| A15 | ping metálico + descida | curto | agudo-grave | 1,5 s |
| A16 | 2 tons arredondados + vapor | pulso | médio | 1,2 s |

---

## Parte B — Armas do jogador: especiais e tiros (P2)

### B01 · `weapon/axe_seismic_windup` — Golpe Sísmico, preparo
- **Evento:** início do especial `ground_slam` (`SpecialAttacks`). **Substitui:** a primeira parte de
  `kn8:weapon.axe_special`. **Duração:** 0,7 s. **Tipo:** pontual. **Animação:** acompanha o machado subindo, mas
  toca pelo início do especial no servidor. **Volume:** alcance 24.
```
A huge combat axe being raised overhead with force: heavy metal blade whoosh rising, armor plates creaking, a short
building hum of power-suit servos, ends just before the strike, no music, no voice, no speech.
```

### B02 · `weapon/axe_seismic_impact` — Golpe Sísmico, impacto
- **Evento:** tick de impacto do `ground_slam`. **Substitui:** `kn8:weapon.axe_special`. **Duração:** 2 s.
  **Variações:** 2. **Volume:** alcance 40.
```
A massive axe slamming into the ground and splitting it: a deep cracking boom, ground fracturing outward, rocks and
debris tumbling, a low rumbling aftershock, gritty and heavy, no music, no voice, no speech.
```

### B03 · `weapon/sword_dash_cut` — Corte em avanço (espada)
- **Evento:** especial `lunge` (`dash_cut`). **Substitui:** `kn8:weapon.sword_heavy`. **Duração:** 0,8 s.
  **Variações:** 2. **Volume:** alcance 24.
```
A swordsman dashing forward at high speed and cutting through: sharp burst of displaced air, fast footstep scrape, a
long clean blade slice that whips past, metallic ring fading, no music, no voice, no speech.
```

### B04 · `weapon/knife_flurry` — Sequência de cortes (faca)
- **Evento:** cada um dos 4 cortes do `flurry`. **Substitui:** `kn8:weapon.knife_heavy`. **Duração:** 0,25 s.
  **Variações:** 4 (uma por corte). **Volume:** alcance 16.
```
One very fast combat knife slash, a tight short whip of air with a thin metallic edge, crisp and light, part of a rapid
sequence, no music, no voice, no speech.
```

### B05 · `weapon/rifle_shot` — Tiro comum do rifle (refazer, P3)
- **Evento:** tiro do rifle (jogador e soldados). **Substitui:** `kn8:weapon.rifle_shot` (satura). **Duração:**
  0,7 s. **Variações:** 3. **Volume:** alcance 48.
```
Single shot from a futuristic military assault rifle outdoors: tight punchy report, short mechanical action clack, brief
echo off buildings, controlled and realistic, no music, no voice, no speech.
```

### B06 · `weapon/pistol_shot` — Tiro comum da pistola (refazer, P3)
- **Evento:** tiro da pistola. **Substitui:** `kn8:weapon.pistol_shot`. **Duração:** 0,5 s. **Variações:** 3.
```
Single shot from a compact military sidearm outdoors: snappy higher-pitched crack, slide action click, short tail, no
music, no voice, no speech.
```

### B07 · `weapon/rifle_precise_shot` — Tiro preciso do rifle
- **Evento:** clique direito com rifle (ação `heavy`, PM-1). **Substitui:** o tiro comum (mesmo arquivo hoje).
  **Duração:** 1,4 s. **Variações:** 2. **Volume:** alcance 64.
```
A precision high-power rifle shot: a short electronic targeting lock tick, then a much louder, deeper report than a
normal rifle shot with a sharp supersonic crack splitting the air and a long rolling echo across a city, no music,
no voice, no speech.
```

### B08 · `weapon/pistol_precise_shot` — Tiro preciso da pistola
- **Evento:** clique direito com pistola. **Duração:** 1 s. **Variações:** 2. **Volume:** alcance 48.
```
A focused heavy pistol shot: brief steady breath-like mechanical pause click, then a hard deep crack with extra punch
and a whip-like ricochet tail, clearly stronger than a normal pistol shot, no music, no voice, no speech.
```

### B09 · `weapon/rifle_reload` — Recarga do rifle (3 etapas)
- **Evento:** etapas do `reload` em `weapon_profile/rifle.json` (jogador e soldado). **Substitui:**
  `minecraft:item.crossbow.loading_start/middle/end`. **Duração:** 3 arquivos de 0,3–0,5 s
  (`rifle_reload_out`, `_in`, `_bolt`). **Volume:** alcance 12.
```
Military rifle reload, three separate clean takes: magazine released and pulled out of the rifle; a fresh magazine
slapped in and seated with a click; charging handle pulled back and released with a solid metallic clack. Close,
dry, realistic, no music, no voice, no speech.
```

### B10 · `weapon/pistol_reload` — Recarga da pistola (2 etapas)
- **Evento:** etapas do `reload` da pistola. **Substitui:** sons de besta vanilla. **Duração:** 2 × 0,3 s.
```
Compact pistol reload, two separate takes: magazine dropped out with a light metal slide, new magazine inserted and slide
released forward with a crisp snap, no music, no voice, no speech.
```

### B11 · `weapon/magazine_load` — Carregar o pente
- **Evento:** `MagazineItem` (botão direito no pente vazio). **Substitui:** `crossbow.loading_middle`. **Duração:**
  0,8 s.
```
Rounds being pressed one by one into a rifle magazine: quick series of small metallic clicks and spring compressions,
close and dry, no music, no voice, no speech.
```

### B12 · `combat/perfect_dodge` — Esquiva perfeita (PM-1)
- **Evento:** resultado `PERFECT_DODGE`. **Substitui:** nada. **Duração:** 0,8 s. **Tipo:** pontual. **Volume:** só
  do jogador (interface), −16 LUFS.
```
A perfectly timed dodge in an anime action game: an attack whooshing past very close, then a crisp high time-slow chime
and a reversed air swell that opens a counterattack window, sleek and satisfying, no music, no voice, no speech.
```

---

## Parte C — Soldados especiais (técnicas, armas e tiros especiais)

Todos os arquivos ficam em `sounds/<personagem>/`. "Evento" usa os nomes do código: **preparo** =
`HoshinaEntity.startTechnique` (mesmo momento do VFX `_charge`), **disparo/golpe** = `fireHit`/`strike` no tick do
JSON, **explosão** = `SlashProjectile` ao explodir.

### Hoshina e Hoshina No. 10 (lâminas pretas, cortes secos e finos; No. 10 com cauda viva)

#### C-HOS-01a · `hoshina/kuuchi_windup` — Kūuchi, preparo (P3)
- **Evento:** preparo do `kuuchi`. **Substitui:** `weapon.sword_swing`. **Duração:** 0,5 s. **Volume:** alcance 20.
```
A swordsman in a very low stance drawing breath and tightening grip on two short blades: a soft fabric rustle, a quiet
metallic slide of steel, a held tense silence, no music, no voice, no speech.
```

#### C-HOS-01b · `hoshina/kuuchi_release` — Kūuchi, corte que voa (P2)
- **Evento:** disparo do corte (NPC) e especial `slash_wave` da espada do Hoshina do jogador. **Substitui:**
  `weapon.sword_heavy`. **Duração:** 0,9 s. **Variações:** 2. **Volume:** alcance 32.
```
A single instantaneous sword cut so fast it sends a blade of air flying forward: a razor-thin high slice, a sharp clap of
air, then a narrow whistling cut that travels away and fades, precise and clean, no music, no voice, no speech.
```

#### C-HOS-02 · `hoshina/blade_hit` — Corte acertando kaiju
- **Evento:** acerto de golpe/corte do Hoshina (técnicas e Kūuchi). **Substitui:** `weapon.sword_hit`. **Duração:**
  0,4 s. **Variações:** 3.
```
A sharp blade slicing through thick monster hide: quick wet cut through tough leathery flesh with a crisp edge, short and
precise, no music, no voice, no speech.
```

#### C-HOS-03 · `hoshina/kosa_uchi` — Kōsa-uchi (X)
- **Evento:** golpe da técnica. **Duração:** 0,7 s. **Variações:** 2.
```
Two short swords crossing in an X slash at once: two blade whooshes overlapping perfectly with a single bright metallic
scrape where they cross, tight and fast, no music, no voice, no speech.
```

#### C-HOS-04 · `hoshina/rapid_cut` — Golpe rápido (Ran-uchi, Jūni-hitoe)
- **Evento:** cada golpe do `ran_uchi` (10) e os 11 primeiros do `juni_hitoe`. **Duração:** 0,2 s. **Variações:** 4.
```
A single very fast short-sword strike, a thin snapping cut of air, extremely quick and light, made to repeat in a rapid
flurry, no music, no voice, no speech.
```

#### C-HOS-05 · `hoshina/kasumi_uchi` — Kasumi-uchi
- **Evento:** golpe da técnica (3). **Duração:** 0,5 s. **Variações:** 3.
```
A low sweeping double-blade slash that blurs past like mist: a soft long whoosh with a faint metallic shimmer and a quick
footstep slide, no music, no voice, no speech.
```

#### C-HOS-06a · `hoshina/yae_uchi_windup` / C-HOS-06b · `hoshina/yae_uchi_cut`
- **Evento:** preparo e cada um dos 8 golpes do `yae_uchi`. **Duração:** 0,6 s / 0,25 s. **Variações:** 1 / 4.
```
(06a) A swordsman spinning both blades into position, a rising circular whoosh with steel singing faintly, tension
building, no music, no voice, no speech.
(06b) One cut of an eight-way star slash, a short bright blade snap with a tiny metallic ring, no music, no voice, no
speech.
```

#### C-HOS-07a/b/c · `hoshina/juni_hitoe_*` — Jūni-hitoe (No. 10, Full Release)
- **Evento:** preparo (a); golpe final (b); consequência depois do último golpe (c). **Duração:** 1,2 s / 1 s / 1,5 s.
  **Volume:** alcance 40.
```
(07a) A living combat suit synchronizing to full power: a deep organic heartbeat pulse merging with a rising electric
hum and crackling red energy, building fast, no music, no voice, no speech.
(07b) The final, strongest strike of a twelve-hit sword combo: a huge cross-shaped double slash with a deep air
boom and a heavy metallic ring, no music, no voice, no speech.
(07c) Aftermath of a devastating slash on a giant monster: a long tearing rip through flesh, a crackle of fading energy
and a low settling rumble, no music, no voice, no speech.
```

#### C-HOS-08 · `hoshina/tail_strike` — Corte da cauda (No. 10)
- **Evento:** golpe da cauda (`tickExtra`). **Duração:** 0,6 s. **Variações:** 2.
```
A long armored living tail whipping and slicing: a heavy organic whip crack with segmented plates clattering and a sharp
blade-like edge at the end, no music, no voice, no speech.
```

#### C-HOS-09 · `hoshina/tail_guard` — Guarda da cauda (No. 10)
- **Evento:** guarda bloqueando golpe (`HoshinaNo10Entity`). **Substitui:** `minecraft:block.anvil.land`.
  **Duração:** 0,6 s. **Variações:** 2.
```
A blow deflected by an armored organic tail: a dense thud on hard chitin plates with a short scraping metallic ring and a
spark-like crackle, no music, no voice, no speech.
```

#### C-HOS-10 · `hoshina/full_release` — Full Release (No. 10)
- **Evento:** sincronização chega a 100%. **Substitui:** nada. **Duração:** 2 s. **Volume:** alcance 32.
```
A monster-derived combat suit reaching full release: a deep pulse like a giant heart, a surge of red crackling energy,
armor plates locking with a hiss, ending in a low sustained hum, no music, no voice, no speech.
```

### Kikoru e Kikoru No. 4 (machado pesado, ouro, rocha; No. 4 com asas e vento)

#### C-KIK-01 · `kikoru/axe_windup` — Preparo dos golpes do machado
- **Evento:** preparo de `axe_slash`, `heavy_swing`, `dash_strike`, `guard_break`. **Substitui:**
  `weapon.sword_swing`. **Duração:** 0,4 s. **Variações:** 2.
```
A heavy two-handed battle axe being hauled back for a strike: a deep metal whoosh, armored gloves tightening on the haft,
servo whir of a combat suit, no music, no voice, no speech.
```

#### C-KIK-02 · `kikoru/axe_swing` — Golpe do machado
- **Evento:** golpe (`axe_slash`, `heavy_swing`). **Substitui:** `weapon.axe_swing/heavy` pelo caminho da técnica.
  **Duração:** 0,6 s. **Variações:** 3.
```
A wide, powerful swing of a heavy battle axe, thick low whoosh of air with a ringing edge, weighty and fast for its size,
no music, no voice, no speech.
```

#### C-KIK-03 · `kikoru/axe_hit` — Machado acertando kaiju
- **Evento:** acerto das técnicas da Kikoru. **Substitui:** `weapon.axe_hit`. **Duração:** 0,6 s. **Variações:** 3.
```
A heavy axe blade biting deep into monster carapace: a cracking chop through hard chitin and dense flesh, with a short
low thud, brutal and satisfying, no music, no voice, no speech.
```

#### C-KIK-04a/b/c · `kikoru/shockwave_*` — Onda de choque
- **Evento:** preparo, disparo da onda (`SlashProjectile`), pedras saltando (consequência).
  **Duração:** 0,6 / 1,2 / 1 s. **Volume:** alcance 40.
```
(04a) A battle axe raised high with a gathering rumble in the ground beneath, no music, no voice, no speech.
(04b) An axe smashed into the ground sending a shockwave racing forward: a sharp crack, then a rolling rumble that
travels away tearing up the ground, no music, no voice, no speech.
(04c) Rocks and chunks of pavement bursting up and falling back in a line, gritty debris, no music, no voice, no speech.
```

#### C-KIK-05 · `kikoru/dash_strike` — Investida com o machado
- **Duração:** 0,7 s. **Variações:** 2.
```
A fast forward dash ending in an axe strike: burst of air, boots skidding on concrete, a heavy chopping whoosh, no
music, no voice, no speech.
```

#### C-KIK-06a/b/c · `kikoru/ground_smash_*` — Esmagamento (cratera)
- **Evento:** preparo, golpe/impacto, consequência. **Duração:** 0,8 / 1,5 / 2 s. **Volume:** alcance 48.
```
(06a) A powerful leap upward with an axe, armored suit thrusters of muscle and air, a rising wind, no music, no voice,
no speech.
(06b) A battle axe crashing down into the earth from above: an enormous boom, ground caving into a crater, concrete
shattering, no music, no voice, no speech.
(06c) Dust and debris raining down after a crater impact, rumbling settling ground and small rocks clattering, no music,
no voice, no speech.
```

#### C-KIK-07 · `kikoru/guard_break` — Quebra-guarda
- **Duração:** 0,7 s. **Variações:** 2.
```
A heavy axe pommel smashing through a defensive guard: a hard metallic clang followed by cracking armor plates and a
low thud, no music, no voice, no speech.
```

#### C-KIK-08..12 · Kikoru No. 4 (asas)
- C-KIK-08 `kikoru_no4/wing_beat` — marca de animação da asa (cosmético, cliente). 0,5 s, 3 variações, alcance 16.
- C-KIK-09 `kikoru_no4/dive_strike` — mergulho: vento crescente + impacto. 1,4 s.
- C-KIK-10 `kikoru_no4/aerial_dash` — investida aérea (também `vertical_assault`). 0,8 s.
- C-KIK-11 `kikoru_no4/air_combo_hit` — golpes no ar (`air_combo`, `high_speed_axe`). 0,4 s, 3 variações.
- C-KIK-12 `kikoru_no4/takeoff` — decolagem (início do voo). 1 s.
```
(08) A powerful mechanical-organic wing flap, a deep whump of air with feather-like plates rustling, no music, no voice,
no speech.
(09) A fighter diving from the sky with an axe: rising wind roar as she falls, ending in a heavy crashing axe impact with
a gust of air, no music, no voice, no speech.
(10) A winged fighter dashing through the air: a sharp jet of wind and a quick wing snap, no music, no voice, no speech.
(11) A quick airborne axe slash with a gust of wind behind it, short and sharp, no music, no voice, no speech.
(12) Large wings spreading and beating twice to lift off, gust of air and dust kicked up, no music, no voice, no speech.
```

### Mina (canhão de quadril: lento, grave, devastador)

#### C-MIN-01 · `mina/cannon_shot` — Tiro do canhão (P2)
- **Evento:** tiro comum do `kn8:mina_cannon` (jogador e NPC) e `cannon_shot`. **Substitui:** `weapon.rifle_shot` e
  `weapon.sword_heavy` (técnica). **Duração:** 1,6 s. **Variações:** 2. **Volume:** alcance 80.
```
A hip-mounted heavy anti-monster cannon firing: a deep chest-thumping boom with a sharp muzzle blast, a heavy
mechanical recoil slam and shell cycling clunk, and a long rolling echo through city streets, no music, no voice, no
speech.
```

#### C-MIN-02 · `mina/precision_shot` — Tiro de precisão do canhão
- **Duração:** 1,2 s. **Variações:** 2. **Volume:** alcance 80.
```
A precise high-velocity round from a heavy cannon: tight focused blast, a piercing supersonic crack, less boom and more
penetration than a normal cannon shot, long echo, no music, no voice, no speech.
```

#### C-MIN-03a/b · `mina/charged_cannon_*` — Canhão carregado
- **Evento:** preparo (carga, ocupa o preparo inteiro) e disparo. **Duração:** 1,8 s / 2 s. **Volume:** alcance 96.
```
(03a) A heavy cannon charging up: a deep capacitor whine rising in pitch, mechanical locks clanking into place, power
hum building to a peak, no music, no voice, no speech.
(03b) A fully charged anti-monster cannon releasing: a massive deep blast with a pressure wave, a sharp energized
crack at the front, and heavy recoil, rolling thunder-like echo, no music, no voice, no speech.
```

#### C-MIN-04a/b/c · `mina/anti_giant_*` — Anti-Giant (4 tiros por vida)
- **Evento:** carga, disparo, voo do projétil (som preso à entidade). **Duração:** 2,2 s / 2,5 s / 2 s (laço curto
  enquanto voa). **Volume:** alcance 128 (o tiro mais alto do jogo).
```
(04a) The biggest anti-giant cannon preparing to fire: deep mechanical bracing, heavy shell chambered with a slam, a
low powerful charge drone rising, no music, no voice, no speech.
(04b) An anti-giant cannon firing: an overwhelming low boom that shakes everything, a blinding sharp crack, debris
and glass rattling nearby, enormous echo across a city, no music, no voice, no speech.
(04c) A massive shell flying through the air, a deep heavy rushing whistle passing by, loopable, no music, no voice, no
speech.
```

#### C-MIN-05 · `mina/cannon_reload` — Recarga do canhão
- **Evento:** recarga do `mina_cannon` (perfil de rifle hoje). **Duração:** 1,2 s.
```
Reloading a heavy cannon: a large shell casing ejected with a clang, a new massive round slid in and locked with a
heavy breech slam, no music, no voice, no speech.
```

#### C-MIN-06 · `mina/shell_hit` — Projétil acertando kaiju (sem explosão)
- **Duração:** 0,6 s. **Variações:** 2.
```
A heavy armor-piercing round slamming into a giant monster's hide: a deep punching thud with a cracking of carapace,
no music, no voice, no speech.
```

#### C-MIN-07 · `mina/explosion` — Explosão do canhão
- **Evento:** `SlashProjectile` ao explodir (`cannon_shot`, `charged_cannon`). **Substitui:**
  `minecraft:entity.generic.explode`. **Duração:** 2 s. **Variações:** 2. **Volume:** alcance 80.
```
A heavy explosive shell detonating on a monster: a deep explosive boom with a meaty impact, shrapnel spray, debris and
smoke rumble, no music, no voice, no speech.
```

#### C-MIN-08 · `mina/anti_giant_explosion` — Explosão do Anti-Giant
- **Duração:** 3 s. **Volume:** alcance 128.
```
A colossal anti-giant shell exploding inside a huge monster: an immense low detonation, a shockwave rolling outward,
flesh and carapace tearing, heavy debris falling for seconds, no music, no voice, no speech.
```

### Reno e Reno No. 6 (rifle; gelo; No. 6 com canhões auxiliares)

| Código | Arquivo | Evento | Duração | Var. | Prompt |
|---|---|---|---|---|---|
| C-REN-01 | `reno/precision_shot` | `precision_shot` | 1 s | 2 | `A marksman's aimed rifle shot: steady, crisp and loud with a precise supersonic snap and clean echo, no music, no voice, no speech.` |
| C-REN-02 | `reno/burst_fire` | `burst_fire` | 0,8 s | 2 | `A controlled three-round burst from a military rifle, tight and rhythmic with mechanical cycling between shots, no music, no voice, no speech.` |
| C-REN-03 | `reno/suppression` | `suppression` (rajada congelante) | 1,5 s | 1 | `Sustained suppressive rifle fire, rapid continuous shots with frosty crackling hisses on each round, no music, no voice, no speech.` |
| C-REN-04a | `reno/freeze_round_load` | preparo do `freeze_round` | 0,6 s | 1 | `A special cryogenic round being chambered: a cold hiss of vapor and a metallic click with a faint crystalline ring, no music, no voice, no speech.` |
| C-REN-04b | `reno/freeze_round_shot` | disparo | 0,8 s | 2 | `A rifle firing a freezing round: a sharp shot followed by an icy shimmering whistle, no music, no voice, no speech.` |
| C-REN-04c | `reno/freeze_impact` | acerto (congelamento) | 1 s | 2 | `Ice rapidly spreading over a creature after being hit: crackling, crystallizing ice forming with sharp creaks and a frosty hiss, no music, no voice, no speech.` |
| C-REN-05 | `reno/butt_strike` | `butt_strike` | 0,4 s | 2 | `A hard rifle-butt strike, solid wooden-polymer thud against armor, short and blunt, no music, no voice, no speech.` |
| C-REN-06 | `reno/bullet_hit` | acerto de bala de técnica (Reno e Narumi) | 0,3 s | 3 | `A rifle bullet striking thick monster hide, a short dense smack with a tiny crack, no music, no voice, no speech.` |
| C-REN-07a | `reno_no6/ice_spear_charge` | preparo do `ice_spear` | 1,2 s | 1 | `A monster-derived weapon charging with cold: deep frosty hum, ice crystals crackling as they form along a barrel, a pulsing organic undertone, no music, no voice, no speech.` |
| C-REN-07b | `reno_no6/ice_spear_launch` | disparo | 0,9 s | 2 | `A spear of ice launched at high speed: a sharp cold crack and a whistling rush of frozen air, no music, no voice, no speech.` |
| C-REN-07c | `reno_no6/ice_spear_impact` | acerto | 1,2 s | 2 | `A huge ice spear impaling a monster and shattering: a heavy piercing thud, ice exploding into shards, a freezing hiss, no music, no voice, no speech.` |
| C-REN-08a | `reno_no6/freeze_burst` | disparo do `freeze_burst` | 0,8 s | 1 | `A freezing grenade-like round fired from a rifle, a hollow pop and a cold whoosh, no music, no voice, no speech.` |
| C-REN-08b | `reno_no6/freeze_explosion` | explosão (substitui `generic.explode`) | 2 s | 2 | `An explosion of ice: a sharp cold blast, glass-like ice shards scattering everywhere, crackling frost spreading and settling, no music, no voice, no speech.` |
| C-REN-09 | `reno_no6/ice_field` | `ice_field` | 2,5 s | 1 | `Ground freezing over in a wide area: a rolling wave of crackling ice spreading outward, deep creaks and a cold wind, no music, no voice, no speech.` |
| C-REN-10 | `reno_no6/multi_freeze` | `multi_freeze` | 1,2 s | 1 | `Several freezing rounds fired in quick succession, each with an icy whistle, overlapping frosty cracks, no music, no voice, no speech.` |
| C-REN-11 | `reno_no6/auxiliary_cannons` | `auxiliary_cannons` | 1,4 s | 2 | `Two shoulder-mounted auxiliary cannons firing one after the other: mechanical deployment clank, two deep punchy blasts, no music, no voice, no speech.` |

### Narumi e Narumi No. 1 (baioneta longa; precisão; previsão)

| Código | Arquivo | Evento | Duração | Var. | Prompt |
|---|---|---|---|---|---|
| C-NAR-01 | `narumi/bayonet_swing` | `sounds.swing` da `narumi_bayonet` | 0,4 s | 3 | `A long bayonet rifle swung in an arc, a long thin whoosh with a metallic edge, no music, no voice, no speech.` |
| C-NAR-02 | `narumi/thrust` | golpes do `thrust_combo` e `lunge` | 0,3 s | 4 | `A lightning-fast bayonet thrust, a piercing needle-like snap of air, extremely precise, no music, no voice, no speech.` |
| C-NAR-03 | `narumi/pierce_hit` | acerto de estocada | 0,4 s | 3 | `A bayonet blade piercing deep into monster flesh, a sharp tight puncture with a short wet crunch, no music, no voice, no speech.` |
| C-NAR-04 | `narumi/sweeping_slash` | `sweeping_slash` | 0,6 s | 2 | `A wide sweeping bayonet slash, a long sharp arc of air with a ringing blade, no music, no voice, no speech.` |
| C-NAR-05 | `narumi/bayonet_shot` | `bayonet_shot` (substitui `sword_heavy`) | 0,9 s | 2 | `A shot fired from a bayonet-mounted heavy rifle at close range: a loud sharp report with a metallic ring from the blade, no music, no voice, no speech.` |
| C-NAR-06 | `narumi_no1/foresight` | previsão do Numbers 1 (log "previsao") | 0,8 s | 1 | `A subtle eerie perception cue: a soft reversed swell and a high crystalline tick, as if time briefly sharpens, no music, no voice, no speech.` |
| C-NAR-07 | `narumi_no1/weak_point_thrust` | `weak_point_thrust` | 0,7 s | 2 | `A single perfectly aimed thrust into a weak point: a held breath of silence, a sharp piercing strike and a deep cracking give, no music, no voice, no speech.` |
| C-NAR-08 | `narumi_no1/predicted_counter` | `predicted_counter` | 0,8 s | 2 | `A counterattack timed before the enemy's blow lands: a quick whoosh of evasion followed instantly by a sharp double thrust, no music, no voice, no speech.` |

### Isao (Numbers 2: manoplas de ar comprimido; Thunder Emperor = soco único com toda a força, sem raio) e Isao sem o Numbers 2

| Código | Arquivo | Evento | Duração | Var. | Prompt |
|---|---|---|---|---|---|
| C-ISA-01 | `isao/fist_swing` | `sounds.swing` de `isao_fist` e `numbers2_gauntlet` (substitui `axe_swing`) | 0,3 s | 3 | `A powerful punch thrown by a big armored fighter, a short heavy whoosh of air, no music, no voice, no speech.` |
| C-ISA-02 | `isao/fist_heavy` | `sounds.heavy` (substitui `axe_heavy`); `heavy_straight`, `elbow_strike` | 0,5 s | 2 | `A heavy straight punch with full body weight, a deep air thump and a creak of armored gloves, no music, no voice, no speech.` |
| C-ISA-03 | `isao/fist_hit` | `sounds.hit` (substitui `axe_hit`) | 0,4 s | 3 | `A gauntleted fist smashing into thick monster hide, a dense meaty impact with a crack of carapace, no music, no voice, no speech.` |
| C-ISA-04a | `isao/barrage_windup` | preparo do `barrage_blast` | 0,6 s | 1 | `Pneumatic combat gauntlets pressurizing: rising hiss of compressed air and mechanical pistons cocking, no music, no voice, no speech.` |
| C-ISA-04b | `isao/air_blast` | cada golpe do `barrage_blast` | 0,4 s | 4 | `A punch that fires a blast of compressed air, a sharp pneumatic bang with a rushing gust, no music, no voice, no speech.` |
| C-ISA-05a | `isao/spiral_windup` | preparo do `spiral_blast` | 0,6 s | 1 | `Air being spun into a vortex around a gauntlet, a swirling rising whoosh, no music, no voice, no speech.` |
| C-ISA-05b | `isao/spiral_release` | disparo | 0,6 s | 2 | `A spiraling blast of air launched from a punch, a twisting high-pressure bang, no music, no voice, no speech.` |
| C-ISA-05c | `isao/spiral_travel` | voo (preso ao projétil) | 1,5 s laço | 1 | `A spinning vortex of air flying through the air, a whirling rushing hum, loopable, no music, no voice, no speech.` |
| C-ISA-05d | `isao/spiral_impact` | acerto | 0,8 s | 2 | `A vortex of compressed air hitting a giant creature, a twisting thudding burst with scattered gusts, no music, no voice, no speech.` |
| C-ISA-06a | `isao/force_windup` | preparo do `force_blast` | 0,8 s | 1 | `Both gauntlets drawing in air with a deep inhaling roar of pressure building, no music, no voice, no speech.` |
| C-ISA-06b | `isao/force_release` | disparo | 1 s | 2 | `A massive forward shockwave of air from both fists, a deep blast and a rolling wall of wind, no music, no voice, no speech.` |
| C-ISA-06c | `isao/force_wake` | consequência (leque no chão) | 1,2 s | 1 | `A shockwave tearing along the ground in a fan, dust and debris swept away, no music, no voice, no speech.` |
| C-ISA-07a | `isao/thunder_emperor_charge` | preparo do Thunder Emperor (inteiro) | 2 s | 1 | `A giant fighter channeling every ounce of strength into one fist: gauntlet mechanisms locking, deep groaning pressure, a slow building rumble, absolutely no electricity, no music, no voice, no speech.` |
| C-ISA-07b | `isao/thunder_emperor_hit` | golpe final | 1,5 s | 1 | `A single overwhelming punch landing with full force: an enormous deep thunderclap-like boom of air, bones and carapace cracking, ground shaking, no electricity, no music, no voice, no speech.` |
| C-ISA-07c | `isao/thunder_emperor_wake` | consequência (núcleo exposto) | 2 s | 1 | `The aftermath of a colossal punch: a long rumbling echo, debris falling, a giant creature's body groaning, no music, no voice, no speech.` |

### Kafka e Kaiju No. 8 (soco com pressão de ar; transformação)

| Código | Arquivo | Evento | Duração | Var. | Prompt |
|---|---|---|---|---|---|
| C-K8-01 | `no8/fist_swing` | `sounds.swing` do `kaiju_no8_fist` (substitui `axe_swing`) | 0,3 s | 3 | `A monstrous punch thrown at incredible speed, a deep compressed air burst, no music, no voice, no speech.` |
| C-K8-02 | `no8/fist_heavy` | `sounds.heavy`, `heavy_punch` | 0,6 s | 2 | `A devastating monster-strength punch, a deep boom of displaced air with an organic creak of a powerful body, no music, no voice, no speech.` |
| C-K8-03 | `no8/fist_hit` | `sounds.hit` (substitui `axe_hit`); também acertos do Kafka | 0,5 s | 3 | `A monster fist smashing into another monster, a massive meaty impact with cracking carapace, no music, no voice, no speech.` |
| C-K8-04 | `no8/dash_strike` | `dash_strike`, `rush_combo` | 0,7 s | 2 | `A giant fighter leaping forward with a powerful punch, a burst of air and a heavy strike, no music, no voice, no speech.` |
| C-K8-05a | `no8/ground_smash_windup` | preparo do `ground_smash` | 0,7 s | 1 | `A monster raising its fist high, a deep organic groan of muscles tensing and air drawing in, no music, no voice, no speech.` |
| C-K8-05b | `no8/ground_smash_hit` | golpe no chão (substitui `sword_heavy`) | 1,5 s | 2 | `A colossal fist smashing the ground, an enormous boom, earth splitting and a pressure wave rolling outward, no music, no voice, no speech.` |
| C-K8-05c | `no8/ground_smash_explosion` | explosão do golpe (substitui `generic.explode`) | 2 s | 1 | `Debris and dust erupting after a ground-shattering impact, rocks raining down, low rumble settling, no music, no voice, no speech.` |
| C-K8-06 | `no8/roar_blast` | técnica `roar` | 2 s | 1 | `A humanoid monster's roar so loud it becomes a physical shockwave, deep and distorted with pressure, no music, no speech.` |
| C-K8-07 | `no8/regenerate` | regeneração do No. 8 | 1,2 s | 1 | `Monster flesh rapidly regenerating, wet knitting tissue and soft crackles of hardening carapace, no music, no voice, no speech.` |
| C-K8-08 | `no8/transform` | Kafka → No. 8 (substitui `ravager.roar`) | 2,5 s | 1 | `A man transforming into a monster: bones cracking and shifting, flesh hardening into armor with a deep crackle, ending in a low growl of power, no music, no speech.` |
| C-K8-09 | `no8/revert` | No. 8 → Kafka | 1,5 s | 1 | `A monster body dissolving back into a human, carapace cracking and steaming away, a soft exhale of vapor, no music, no voice, no speech.` |
| C-K8-10 | `kafka/punch` | `punch_combo` do Kafka | 0,3 s | 3 | `A clumsy but strong human punch in a combat suit, a short thud of air, no music, no voice, no speech.` |

---

## Parte D — Kaiju: habilidades com som próprio

Arquivos em `sounds/kaiju/<família>/`. **R** = saída/disparo (`KaijuAbilities.fire` ou tick do golpe), **T** = voo
(som preso ao `KaijuProjectile`), **I** = impacto (`AbilityEffects.play`). Preparo só nos golpes pesados (substitui o
Warden do `KaijuTelegraph`). Espécies maiores usam o mesmo arquivo com tom mais grave (o código já ajusta o tom pela
altura do corpo).

### K00 · `kaiju/heavy_windup` — Preparo genérico de golpe pesado (reserva)
- **Evento:** `KaijuTelegraph.start` quando a habilidade não tem preparo próprio. **Substitui:**
  `minecraft:entity.warden.sonic_charge`. **Duração:** 1 s. **Variações:** 2. **Volume:** alcance 32.
```
A giant creature tensing before a heavy attack: a deep organic inhale, muscles and carapace creaking, a low growing
growl and ground rumble, no music, no speech.
```

### Primigenius (Yoju branco, ressurgido verde, Honju marrom de chifres, revivido roxo)

| Código | Arquivo | Habilidade | Etapas | Duração | Prompt |
|---|---|---|---|---|---|
| D-PRI-01 | `primigenius/slam` | `slam` | I | 1,6 s | `A huge reptilian monster stomping the ground with both feet, a deep earth-shaking thud and cracking pavement, no music, no voice, no speech.` |
| D-PRI-02 | `primigenius/heavy_punch` | `heavy_punch` | P (K00) + I | 1,2 s | `A giant clawed fist slamming down, a heavy meaty boom with stone breaking, no music, no voice, no speech.` |
| D-PRI-03 | `primigenius/tail_swipe` | `tail_swipe` | I | 1 s | `A massive thick tail sweeping through the air and smashing into things, a heavy whoosh and blunt impact, no music, no voice, no speech.` |
| D-PRI-04 | `primigenius/charge` | `charge` | R (rugido curto) + I | 1,8 s | `A monster charging forward, pounding footsteps accelerating into a violent crashing collision, no music, no speech.` |
| D-PRI-05a | `primigenius/energy_charge` | `energy_blast`, `energy_blast_revived` | P | 1,5 s | `Energy building inside a giant monster's throat, a deep humming glow rising with crackles, no music, no speech.` |
| D-PRI-05b | `primigenius/energy_release` | idem | R | 1 s | `A monster firing a beam of energy from its mouth, a roaring blast of power, no music, no speech.` |
| D-PRI-05c | `primigenius/energy_impact` | idem (substitui `kaiju.roar` no impacto) | I | 1,2 s | `A blast of monster energy hitting the ground, a sizzling explosive burst and scattering debris, no music, no voice, no speech.` |
| D-PRI-06 | `primigenius/hooved_strike` | `hooved_strike` | I | 0,8 s | `A heavy hoof-like foot stamping down hard, a dense crack on stone, no music, no voice, no speech.` |
| D-PRI-07 | `primigenius/claw` | `claw` | I | 0,6 s | `Big claws raking through armor and flesh, a ripping scrape with a wet edge, no music, no voice, no speech.` |
| D-PRI-08 | `primigenius/bite` | `bite` | I | 0,6 s | `Massive reptilian jaws snapping shut, a heavy teeth clack and crunch, no music, no voice, no speech.` |
| D-PRI-09 | `primigenius/leap_land` | `leap` | I | 1,5 s | `A gigantic creature landing after a jump, an immense thud with cracking ground and a wave of debris, no music, no voice, no speech.` |
| D-PRI-10 | `primigenius/revived_layer` | camada extra dos ressurgidos/revividos | I | 0,8 s | `Rotting revived monster flesh steaming and hissing, a wet gurgle, no music, no voice, no speech.` |

### Aranhas (Trichonephila, Tecedeira Abissal) e formigas (Camponotus)

| Código | Arquivo | Habilidade | Etapas | Duração | Prompt |
|---|---|---|---|---|---|
| D-ARA-01 | `spider/bite` | `bite` (aranha/formiga) | I | 0,4 s | `A giant spider's fangs biting, sharp chitin clicks and a quick wet puncture, no music, no voice, no speech.` |
| D-ARA-02 | `spider/leg_stab` | `leg_stab`, `multi_leg` | I | 0,4 s | `A sharp spider leg stabbing down like a spear, a hard chitin tick and puncture, no music, no voice, no speech.` |
| D-ARA-03 | `spider/leg_swipe` | `leg_swipe` | I | 0,6 s | `Several long spider legs sweeping across, rapid chitin rattling and a whoosh, no music, no voice, no speech.` |
| D-ARA-04a | `spider/web_shot` | `web_shot` | R | 0,5 s | `A giant spider shooting a thick web strand, a wet sticky thwip, no music, no voice, no speech.` |
| D-ARA-04b | `spider/web_hit` | `web_shot`, `web_burst` | I | 0,6 s | `Thick sticky web splattering and wrapping around something, a gooey splat and stretching strands, no music, no voice, no speech.` |
| D-ARA-05 | `spider/web_burst` | `web_burst` | R | 1 s | `A burst of webs sprayed in every direction at once, many sticky thwips overlapping, no music, no voice, no speech.` |
| D-ARA-06 | `spider/leap_land` | `leap` (aranhas) | I | 0,8 s | `A large spider landing after a leap, many legs hitting the ground with a scuttling thud, no music, no voice, no speech.` |
| D-FOR-01 | `ant/acid_spray` | `camponotus_acid_spray`, `camponotus_reborn_acid_spray`, `no9_ant_acid_spray` | R | 1 s | `A giant ant spraying acid from its abdomen, a pressurized wet hissing spray, no music, no voice, no speech.` |
| D-FOR-02 | `ant/acid_hit` | idem (substitui `kaiju.bite`) | I | 1,2 s | `Corrosive acid splashing onto a surface and sizzling, bubbling and smoking, no music, no voice, no speech.` |

### Philinosoma, Diclonius, cogumelos

| Código | Arquivo | Habilidade | Etapas | Duração | Prompt |
|---|---|---|---|---|---|
| D-PHI-01 | `philinosoma/spine_shot` | `philinosoma_spine_shot` | R + I | 1 s | `A lizard monster firing a volley of sharp spines, a rattling launch followed by several hard thunks as they stick in, no music, no voice, no speech.` |
| D-DIC-01 | `diclonius/breath_charge` | `diclonius_atomic_breath` | P | 1,8 s | `A huge monster charging an atomic breath: dorsal plates crackling with energy one after another, a deep rising hum, no music, no speech.` |
| D-DIC-02 | `diclonius/breath_release` | idem (substitui `kaiju.roar`) | R | 2 s (laço curto se o feixe durar) | `A monster unleashing a searing breath beam, a roaring sustained blast of energy, no music, no speech.` |
| D-DIC-03 | `diclonius/breath_impact` | idem | I | 1,5 s | `A burning energy beam hitting the ground, a sizzling explosion with melting debris, no music, no voice, no speech.` |
| D-FUN-01 | `fungus/spore_shot` | `phaneroplasmodium_spore_shot` | R + I | 0,8 s | `A giant mushroom creature puffing out a cloud of spores, a soft wet pop and a hissing powdery cloud, no music, no voice, no speech.` |
| D-FUN-02 | `fungus/spore_bomb_launch` | `myxogasterocarp_spore_bomb` | R + T | 1,5 s | `A large fungal pod launched into the air, a deep wet thump and a whooshing flight, no music, no voice, no speech.` |
| D-FUN-03 | `fungus/spore_bomb_burst` | idem (substitui `kaiju.roar`) | I | 1,5 s | `A spore pod bursting on impact, a muffled organic explosion and a thick hissing cloud spreading, no music, no voice, no speech.` |

### Preondactyl e larva

| Código | Arquivo | Habilidade | Etapas | Duração | Prompt |
|---|---|---|---|---|---|
| D-PRE-01a | `preondactyl/beam_charge` | `preondactyl_energy_beam` | P | 1,2 s | `A flying monster charging an orange energy beam in its throat, a rising crackling hum, no music, no speech.` |
| D-PRE-01b | `preondactyl/beam_fire` | idem (substitui `kaiju.roar`) | R | 1,2 s | `A flying monster firing a fiery energy beam from above, a screaming blast of power, no music, no speech.` |
| D-PRE-01c | `preondactyl/beam_impact` | idem | I | 1,2 s | `A hot energy beam striking the ground, an explosive burst with flames crackling, no music, no voice, no speech.` |
| D-PRE-02 | `preondactyl/dive_strike` | `preondactyl_dive_strike` | R + I | 1,5 s | `A huge flying creature diving from the sky, a rising wind scream ending in a violent crash, no music, no speech.` |
| D-PRE-03 | `preondactyl/tail_strike` | `preondactyl_tail_strike` | I | 0,6 s | `A long whip-like tail striking hard, a snapping crack and thud, no music, no voice, no speech.` |
| D-PRE-04 | `preondactyl/self_destruct_charge` | aviso da autodestruição (substitui `creeper.primed`) | P (laço 3 s) | 3 s | `A dying flying monster's body swelling with unstable energy, a rising pulsing hum and organic crackling, accelerating, no music, no speech.` |
| D-PRE-05 | `preondactyl/self_destruct_blast` | explosão (substitui `generic.explode`) | I | 2,5 s | `A monster exploding in midair, a deep organic detonation with fiery burst and falling debris, no music, no voice, no speech.` |

### No. 9 e formas (frio, preciso, inteligente)

| Código | Arquivo | Habilidade/evento | Etapas | Duração | Prompt |
|---|---|---|---|---|---|
| D-N9-01 | `no9/finger_gun_fire` | `finger_gun` (saída) | R | 0,4 s | `A monster flicking its finger to fire a bullet at extreme speed, a dry finger snap merged with a supersonic crack, no music, no voice, no speech.` |
| D-N9-02 | `no9/finger_gun_hit` | `finger_gun` (impacto, substitui `pistol_shot`) | I | 0,3 s | `A tiny super-fast projectile hitting hard, a sharp thwack and stone chip, no music, no voice, no speech.` |
| D-N9-03 | `no9/multi_finger_gun` | `no9_black_multi_finger_gun` | R | 1 s | `A rapid barrage of finger-fired bullets at extreme speed, a crackling series of supersonic snaps, no music, no voice, no speech.` |
| D-N9-04 | `no9/multi_finger_cannon` | `no9_fusion_multi_finger_cannon` | R | 1,4 s | `Several heavy energy shots fired from monster fingers in a volley, deep punchy blasts overlapping, no music, no voice, no speech.` |
| D-N9-05 | `no9/blade_arm` | `no9_black_blade_arm` | I | 0,6 s | `A monster arm hardened into a blade slicing through, a sharp organic slash with a dense cut, no music, no voice, no speech.` |
| D-N9-06 | `no9/tendril` | `no9_black_tendril` | R + I | 0,8 s | `Fleshy tendrils lashing out and wrapping around a target, wet whips and tightening squeezes, no music, no voice, no speech.` |
| D-N9-07 | `no9/spike_burst` | `no9_black_spike_burst` | I | 1 s | `Sharp spikes erupting from the ground in a burst, cracking stone and piercing thunks, no music, no voice, no speech.` |
| D-N9-08 | `no9/short_flight` | `no9_black_short_flight` | R + I | 1,2 s | `A humanoid monster launching into a short flight and landing hard, a rush of air and a heavy thud, no music, no speech.` |
| D-N9-09a | `no9/hybrid_beam_charge` | `no9_fusion_hybrid_beam` | P | 1,6 s | `A fused monster charging a hybrid energy beam, red energy crackling in bursts with a deep rising drone, no music, no speech.` |
| D-N9-09b | `no9/hybrid_beam_fire` | idem (substitui `kaiju.roar`) | R + I | 2 s | `A searing hybrid energy beam blasting forward and exploding on impact, a roaring sustained beam and a heavy blast, no music, no speech.` |
| D-N9-10 | `no9/absorb` | início da absorção (substitui `kaiju.ambient`) e fim (substitui `anvil.land`) | P + S | 2,5 s | `A monster absorbing another creature: tendrils dragging, wet flesh merging and bones cracking, a deep satisfied growl, no music, no speech.` |
| D-N9-11 | `no9/revive` | reviver carcaça (substitui `evoker.prepare_summon`) | S | 2 s | `A dead monster's corpse twitching back to life, wet crackling sinew, a slow rising groan, no music, no speech.` |
| D-N9-12 | `no9/mass_revive` | ressurreição em massa (substitui `wither.spawn`) | S | 3 s | `Many dead monsters being raised at once, a deep spreading rumble, wet crackling from all directions, an eerie low drone, no music, no speech.` |
| D-N9-13 | `no9/escape` | fuga (substitui `enderman.teleport`) | S | 1,2 s | `A cunning monster vanishing in a burst of smoke, a quick hiss and a rush of displaced air, no music, no voice, no speech.` |
| D-N9-14 | `no9/retreat` | início da retirada (substitui `enderman.scream`) | S | 1,5 s | `A wounded intelligent monster hissing and retreating, a strained growl and quick footsteps, no music, no speech.` |
| D-N9-15 | `no9/isao_blasts` | `no9_isao_barrage_blast`, `force_blast`, `sonic_blast`, `thunder_emperor` (forma com o Isao) | R + I | 1–1,5 s | `Monster-sized pneumatic gauntlet blasts, deep compressed-air booms twisted with an organic growl, no music, no speech.` |

### No. 10 (pequeno e gigante: brutal) e No. 13 (rápido)

| Código | Arquivo | Habilidade | Etapas | Duração | Prompt |
|---|---|---|---|---|---|
| D-N10-01a | `no10/finger_cannon_charge` | `no10_finger_cannon`, `no10g_finger_cannon` | P | 1,2 s | `A monster charging a cannon blast in its fingertip, a deep red energy hum and crackle, no music, no speech.` |
| D-N10-01b | `no10/finger_cannon_fire` | idem (substitui `kaiju.slam`) | R + I | 1,5 s | `A finger cannon firing a heavy energy blast and exploding on impact, a deep boom and fiery burst, no music, no speech.` |
| D-N10-02 | `no10/heavy_punch` | `no10_heavy_punch`, `no10g_heavy_punch` | I | 1,2 s | `A brutal monster punch with massive weight, a deep crushing boom, no music, no voice, no speech.` |
| D-N10-03 | `no10/heavy_smash` | `no10_heavy_smash`, `no10g_heavy_smash` | I | 1,8 s | `A monster smashing down with both fists, an enormous ground-shaking crash and breaking concrete, no music, no voice, no speech.` |
| D-N10-04 | `no10/multi_appendage` | `no10_multi_appendage`, `no10g_multi_appendage` | I | 1 s | `Several bony appendages striking in rapid succession, a flurry of hard organic stabs, no music, no voice, no speech.` |
| D-N10-05 | `no10/tail_stab` | `no10_tail_stab` | I | 0,6 s | `A sharp tail spike thrust forward and piercing, a fast whip and stab, no music, no voice, no speech.` |
| D-N10-06 | `no10/tail_sweep` | `no10_tail_sweep`, `no10g_tail_sweep` | I | 1 s | `A thick monster tail sweeping wide, a heavy whoosh and scattering debris, no music, no voice, no speech.` |
| D-N10-07 | `no10/tail_smash` | `no10g_tail_smash` | I | 1,6 s | `A giant tail slamming down from above, a colossal crash and cracking ground, no music, no voice, no speech.` |
| D-N13-01 | `no13/flurry` | `no13_flurry` | I | 0,3 s (4 var.) | `A lightning-fast monster punch, a short sharp thud of air, part of a rapid flurry, no music, no voice, no speech.` |
| D-N13-02 | `no13/heavy_punch` | `no13_heavy_punch` | I | 0,9 s | `A fast agile monster landing a heavy punch, a sharp crack and deep impact, no music, no voice, no speech.` |
| D-N13-03 | `no13/pounce` | `no13_pounce` | R + I | 1 s | `A fast monster pouncing onto its prey, a quick rush of air and a heavy landing, no music, no voice, no speech.` |
| D-N13-04 | `no13/sprint` | `no13_sprint` | R | 1,2 s | `A monster sprinting at incredible speed, rapid heavy footfalls and wind rush, no music, no voice, no speech.` |
| D-N13-05 | `no13/throat_grab` | `no13_throat_grab` | R + I | 0,9 s | `A monster's hand shooting out and seizing a throat, a fast lunge and a crushing grip snap, no music, no voice, no speech.` |
