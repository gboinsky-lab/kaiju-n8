# SONS — sons feitos pelo Claude (2026-11-05)

Decisão do Miguel (2026-10-30, E): todos os sons (habilidades, técnicas, kaiju, alarmes) feitos pelo Claude, **sem
ElevenLabs**. Substitui o caminho dos `PROMPTS_SUBSTITUICAO.md`/`ELEVENLABS_PROMPTS.md`, que ficam só como referência
de intenção. Os sons de antes (27 eventos) continuam; os novos entram por cima, por dados.

## 1. Como foram feitos

- `tools/audio/gen_sounds_sons.py`: síntese em numpy (ruído filtrado, senoides, envelopes, ressonâncias), igual ao
  `gen_sounds.py` da 0.2: nada gravado de terceiros, sem problema de licença. Escreve os `.ogg` (mono, Vorbis), as
  entradas do `assets/kn8/sounds.json` (alcance dos grandes, `stream` na sirene) e as legendas `subtitles.kn8.<evento>`
  nos dois idiomas. **50 eventos, 112 arquivos** (1 a 3 variações por som).
- `tools/audio/gen_sound_profiles.py`: liga os sons aos dados por regras de nome (sem Java): `sound_profile/*.json`,
  `sound` das habilidades e das técnicas do jogador, `sounds` dos punhos e do canhão. Rodar de novo depois de criar
  técnica ou habilidade.
- Prévias: `previa_sons_SONS.mp3` (todos os sons em sequência) e gravações do cliente em jogo (sirene da invasão; Mina
  e Hoshina contra o Honju), enviadas no chat.

## 2. Assinatura de cada um

| Quem | Som | Ideia |
|---|---|---|
| Hoshina / No. 10 | `technique.hoshina_slash`, `hoshina_release`, `tail_slash` | ar fino muito rápido e o "shiing" agudo das lâminas duplas; Kuuchi com estalo de ar cortado |
| Kikoru / No. 4 | `technique.kikoru_swing`, `kikoru_thunder` | giro grave do machado com chiado elétrico; Falling Thunder = trovão e o machado cravando |
| Narumi / No. 1 | `technique.narumi_thrust`, `narumi_shot`, `narumi_blast` | perfuração reta com o "tsk" da ponta; tiro seco da baioneta; rajada de explosões em sequência |
| Isao | `technique.isao_punch`, `isao_blast`, `isao_thunder` | estampido de ar comprimido com baque metálico; onda de choque; Thunder Emperor em barragem |
| Kaiju No. 8 | `technique.no8_punch`, `no8_rupture` | soco de carne e osso muito grave com eco; ruptura = ar sugado, estouro e destroços |
| Mina | `technique.mina_charge`, `weapon.cannon_shot`, `technique.mina_explosion` | capacitor subindo até um apito; canhão enorme com eco longo (alcance 80) |
| Reno / No. 6 | `technique.reno_shot`, `ice_freeze`, `ice_shatter` | tiro limpo com cauda de gelo trincando; gelo se espalhando; estilhaço |
| Punhos (No. 8, Isao, jogador) | `weapon.fist_swing`, `fist_hit` | antes tocavam o som do machado |
| Kaiju | `kaiju.windup`, `energy_charge`, `energy_beam`, `finger_gun`, `acid_spray`, `web_shot`, `spine_shot`, `spore`, `claw`, `tail_swipe`, `punch`, `grab`, `self_destruct` (+ `charge_rush`, `leap` prontos) | preparo de golpe pesado (no lugar da carga do Warden); carga e disparo de raio; tiros com som de saída (antes só o impacto) |
| Jogador | `weapon.knife_flurry`, `weapon.dash_cut`, `combat.perfect_dodge` | especiais da faca e da espada; esquiva perfeita própria (antes o aparar agudo) |
| Alarmes | `alert.invasion_start`, `siren_loop`, `wave_yoju/honju/numbered`, `critical`, `wave_cleared`, `victory`, `defeat` | abertura, sirene em laço, alerta pela maior ameaça da onda, fim de onda, vitória e derrota |

## 3. Onde cada um toca (servidor decide, no mesmo ponto do efeito visual)

- **Técnicas dos especiais** (`HoshinaEntity`): `windup` no início do preparo, `release` no tick de cada golpe e no
  disparo do tiro/onda (tick do JSON, nunca no início da animação). Técnica com perfil e sem som de preparo fica em
  silêncio (antes **toda** técnica começava com o corte de espada).
- **Habilidades dos kaiju**: `windup` do `sound_profile/kaiju.json` no início do preparo (carga do raio); sem ele, o
  golpe pesado usa `kaiju.windup`; `release` na saída do tiro (`KaijuAbilities.fire`); o `sound` do JSON no impacto
  (agora aceita sons que só existem no sounds.json). O tom segue a voz do tamanho do kaiju.
- **Alarmes**: os toques são do servidor (`InvasionService.alarm`, no ouvido de quem está até 2x o raio ou lutou),
  uma vez por evento. A **sirene** é um laço do cliente (`client/sound/InvasionAlarmClient`) guiado pela fase do
  `InvasionStateS2C` que já era sincronizado: toca no aviso e entre as ondas, cala na luta e no fim (abaixa em ~1 s),
  cheia até 64 blocos do centro e some aos 176. Uma instância só; ela mesma confere o estado a cada tick. A sirene
  antiga (cópias de 3 s a cada 5 s) saiu. Sem pacote novo (protocolo continua 41).
- **Vanilla trocado**: rugido do Ravager na troca de forma (NPC e jogador) → rugido do kaiju; explosão vanilla do tiro
  que explode, do bombardeio e da autodestruição → explosões próprias; toque de conquista na vitória → `alert.victory`.
  O braço parcial do hospedeiro usa `technique.charge`. Ficam vanilla: a batida do coração da larva e o toque de missão
  concluída.

## 4. Validação

- Registro `sound_profile` (só servidor): som inexistente vira aviso no log e a etapa fica com o som de antes
  (regra 9). Os ids `kn8:` são conferidos no `sounds.json` que vai no jar.
- GameTests `everySoundInTheDataExists` (todo som dos dados existe; todo arquivo do sounds.json está no jar) e
  `everyCharacterAndRangedKaijuHasItsOwnSounds` (toda técnica de todo especial tem som de golpe; todo tiro de kaiju tem
  som de saída; canhão da Mina e soco do No. 8 com som próprio).
- Em jogo (servidor dedicado + 2 clientes, áudio gravado em WAV e legendas): sirene em laço durante o aviso e o
  intervalo, calada na luta; "Alert: Yoju", "Invasion repelled"; Mina "Cannon charges/shot/blast"; Hoshina "Twin-blade
  slash/Flying slash"; Isao "Gauntlet punch/Shockwave"; Kikoru "Electrified axe/Axe thunder"; No. 8 "Kaiju No. 8
  punch/Rupture punch"; Narumi No. 1 "Thrust/Explosive burst"; Reno No. 6 "Freezing shot/Ice spreads/Ice shatters";
  Honju "Energy beam", "Kaiju winds up" (`docs/img/sons_legendas_em_jogo.jpg`).

## 5. AJUSTES-2 (2026-11-05: "acrescente os sons de voo e etc")

- **Voo dos projéteis** (`travel` no `sound_profile`, `common/sound/TravelSoundSource`, `client/sound/ProjectileFlightSounds`):
  o servidor escolhe o laço no disparo e o grava num dado sincronizado do projétil; cada cliente toca o laço preso a
  ele e para quando ele some (até 12 ao mesmo tempo). Kaiju: raio (`travel.energy_beam`) e ácido/teia/espinhos/esporos
  (`travel.organic`) pelo nome da habilidade. Tiros e cortes dos especiais e do jogador: o perfil da técnica manda
  (espiral do Isao `travel.spiral`, lança de gelo `travel.ice`); sem ele, pelo tipo: corte que voa `travel.slash`,
  tiro que explode `travel.shell`, tiro que congela `travel.ice`, bala comum sem laço. Laços de 1,2 a 2 s com o fim
  fundido no começo (`seamless` no `gen_sounds_sons.py`).
- **Cauda do Numbers 10** (Hoshina No. 10 e o traje do jogador): corte com `technique.tail_slash`, guarda com o novo
  `technique.tail_guard` (antes a bigorna vanilla e o golpe de varrer).
- **Investida e salto dos kaiju**: `kaiju.charge_rush` no arranque e `kaiju.leap` no impulso, no tom da voz do tamanho.
- **Aviso de ameaça grande** (`alert.major_threat`): invasão de nível 4+ com numerado, ~3 s depois da abertura.
- Sem alarme de aproximação (A11) e sem som de fundo nas lutas comuns (decisão do Miguel de 2026-11-05).

## 6. O que ficou de fora

- Os sons são síntese: julgados por medida (volume, duração, faixa de frequência) e pelas legendas. Ouvir e
  aprovar é do Miguel.
