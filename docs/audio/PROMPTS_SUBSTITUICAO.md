# Kaiju No. 8 — Prompts para substituir os sons que já existem

Decisão do Miguel (2026-10-23): **não criar sons novos agora**, só regravar no ElevenLabs os sons que o mod já tem.
Esta lista cobre os **27 eventos de som do mod** (`assets/kn8/sounds.json`, 66 arquivos), com o **mesmo nome de
arquivo** e a **mesma quantidade de variações**. Ou seja, o arquivo novo só toma o lugar do antigo, sem código e
sem mudar o `sounds.json`.

Os sons novos de `ELEVENLABS_PROMPTS.md` (alarmes por classe, etapas de carga/voo, sons por personagem) ficam
**guardados para depois**.

## Como usar

1. ElevenLabs → *Sound Effects*. Copie o prompt em inglês sem mudar nada.
2. Ponha a duração da tabela. O gerador não aceita menos de ~0,5 s: nos sons mais curtos gere com 0,5 s, que eu
   corto.
3. "Prompt influence": 0,6 (0,8 na sirene e no alarme do traje).
4. Gere uma vez **para cada variação** (`_1`, `_2`, `_3`): mesmo prompt, resultados diferentes. Escolha os que
   soarem diferentes entre si.
5. Baixe em **WAV** (ou MP3, se o plano só der esse) com o nome da coluna "Arquivos" e me mande.
6. Eu corto o silêncio, ajusto o volume, converto para OGG e ponho no lugar do antigo.

Todos os prompts terminam com "no music, no voice, no speech".

### Nome dos arquivos enviados (Miguel, 2026-10-23)

A letra na frente diz o grupo; depois vem o nome da coluna "Arquivos" sem a pasta:

| Letra | Grupo | Exemplo | Vai para |
|---|---|---|---|
| `a_` | kaiju | `a_kaiju_roar_1.wav` | `sounds/kaiju/kaiju_roar_1.ogg` |
| `b_` | armas | `b_rifle_shot_2.wav` | `sounds/weapon/rifle_shot_2.ogg` |
| `c_` | traje, carcaça e alerta | `c_dash_1.wav`, `c_dismantle_3.wav`, `c_siren_1.wav` | `sounds/suit/`, `sounds/carcass/`, `sounds/alert/` |

> **Atenção:** vários eventos servem a mais de uma coisa no jogo (veja "Onde toca"). Por exemplo, o
> `kaiju.bite` também toca no jato de ácido e na teia, e o `weapon.axe_*` também nos socos do No. 8 e do Isao.
> O prompt foi escrito para soar bem no uso principal sem ficar errado nos outros. Separar cada um no som certo
> é a etapa dos sons novos, mais tarde.

---

## Kaiju (7 eventos, 19 arquivos)

| Evento | Arquivos | Duração | Onde toca | Prompt |
|---|---|---|---|---|
| `kaiju.roar` | `kaiju/kaiju_roar_1..3` | 2,7 s | rugido de investida, início de onda da invasão, chefe surgindo e mudando de fase, 11 habilidades (raios, investidas) | `A giant monster's furious roar: deep guttural bellow that rises in pitch and then falls, with rasping texture, wet throat and a heavy sub-bass rumble, echoing off city buildings, no music, no voice, no speech.` |
| `kaiju.ambient` | `kaiju/kaiju_ambient_1..3` | 2 s | rosnado de espera dos kaiju, absorção do No. 9 | `A huge creature's low idle growl with its mouth nearly closed: slow deep rumbling breath, occasional throaty clicks, menacing and patient, no music, no voice, no speech.` |
| `kaiju.hurt` | `kaiju/kaiju_hurt_1..3` | 0,7 s | kaiju levando dano, ferimento por parte | `A giant monster's short pained snarl when wounded: a sharp guttural yelp that jumps up and drops quickly, heavy and raspy, no music, no voice, no speech.` |
| `kaiju.death` | `kaiju/kaiju_death_1..2` | 3 s | morte de kaiju | `A giant monster's dying roar: a long bellow that loses strength, pitch sinking into a final rattling groan and a heavy body collapsing, no music, no voice, no speech.` |
| `kaiju.step` | `kaiju/kaiju_step_1..3` | 0,5 s | passos dos kaiju (o código engrossa o tom nos grandes) | `A single heavy footstep of a giant creature on concrete: a deep thud with a small crack of stone underfoot, short, no music, no voice, no speech.` |
| `kaiju.slam` | `kaiju/kaiju_slam_1..2` | 1,6 s | pisão, soco pesado, salto, rabada e mais 18 golpes pesados de kaiju | `A giant monster slamming the ground with tremendous force: a deep booming impact, pavement cracking, debris scattering and a low rumbling aftershock, no music, no voice, no speech.` |
| `kaiju.bite` | `kaiju/kaiju_bite_1..3` | 0,5 s | mordida, garra, patas e mais 19 golpes rápidos de kaiju (ácido e teia também) | `Massive monster jaws snapping shut on prey: a fast heavy clack of teeth with a wet crunch, short and vicious, no music, no voice, no speech.` |

## Armas (16 eventos, 41 arquivos)

| Evento | Arquivos | Duração | Onde toca | Prompt |
|---|---|---|---|---|
| `weapon.rifle_shot` | `weapon/rifle_shot_1..3` | 0,7 s | tiro do rifle (jogador, soldados, Reno, Kafka) e do canhão da Mina | `Single shot from a futuristic military assault rifle outdoors: tight punchy report, short mechanical action clack, brief echo off buildings, no music, no voice, no speech.` |
| `weapon.pistol_shot` | `weapon/pistol_shot_1..3` | 0,5 s | tiro da pistola; Finger Gun do No. 9 | `Single shot from a compact military sidearm outdoors: snappy crack, slide action click, short tail, no music, no voice, no speech.` |
| `weapon.blade_swing` | `weapon/blade_swing_1..3` | 0,5 s | golpe leve de arma sem som próprio, ataque de soldado | `A quick light blade swing cutting the air, a short clean whoosh, no music, no voice, no speech.` |
| `weapon.blade_heavy` | `weapon/blade_heavy_1..2` | 0,6 s | golpe pesado de arma sem som próprio | `A strong two-handed blade swing, a deep wide whoosh of air with a faint metallic ring, no music, no voice, no speech.` |
| `weapon.knife_swing` | `weapon/knife_swing_1..3` | 0,5 s | golpe leve da faca | `A very fast combat knife slash, a tight thin whip of air, crisp and light, no music, no voice, no speech.` |
| `weapon.knife_heavy` | `weapon/knife_heavy_1..2` | 0,5 s | estocada pesada da faca; especial da faca | `A sharp forward combat knife thrust, a short rising swish ending in a crisp metallic tick of the edge, no music, no voice, no speech.` |
| `weapon.knife_hit` | `weapon/knife_hit_1..3` | 0,5 s | faca acertando | `A combat knife cutting into tough monster hide, a short tight slice with a small wet snap, no music, no voice, no speech.` |
| `weapon.sword_swing` | `weapon/sword_swing_1..3` | 0,5 s | golpe leve da espada; início de toda técnica dos soldados especiais | `A sword swung quickly, a clean ringing whoosh of steel through air, no music, no voice, no speech.` |
| `weapon.sword_heavy` | `weapon/sword_heavy_1..2` | 0,7 s | golpe pesado da espada; especiais da espada; disparo das técnicas a distância (cortes, tiros, ondas) | `A powerful wide sword slash: the blade sings with a bright metallic ring as it spins, then a deep sweeping rush of air, no music, no voice, no speech.` |
| `weapon.sword_hit` | `weapon/sword_hit_1..3` | 0,5 s | espada e baioneta acertando | `A sword blade slicing into thick monster flesh, a crisp cut with a short wet edge and a brief metallic ring, no music, no voice, no speech.` |
| `weapon.axe_swing` | `weapon/axe_swing_1..3` | 0,55 s | golpe leve do machado; socos do No. 8 e do Isao | `A heavy weapon swung hard, a thick low whoosh of air, weighty and fast, no music, no voice, no speech.` |
| `weapon.axe_heavy` | `weapon/axe_heavy_1..2` | 0,85 s | golpe pesado do machado; soco pesado do No. 8 e do Isao | `A massive overhead swing with full body weight, a deep powerful whoosh and the creak of armored grips, no music, no voice, no speech.` |
| `weapon.axe_hit` | `weapon/axe_hit_1..3` | 0,6 s | machado acertando; socos do No. 8 e do Isao acertando | `A heavy blow smashing into monster carapace: a deep blunt thud with cracking chitin and bone, brutal and dense, no music, no voice, no speech.` |
| `weapon.axe_special` | `weapon/axe_special_1..2` | 1,8 s | Golpe Sísmico do machado (tecla R) | `A huge axe slammed into the ground and splitting it open: a sharp blade bite, a deep booming crack, ground fracturing outward and rocks tumbling down, no music, no voice, no speech.` |
| `weapon.parry` | `weapon/parry_1..2` | 0,9 s | aparar (jogador e especiais), guarda do Hoshina | `A perfect weapon parry: a bright sharp metallic clang with a clean ringing tail, satisfying and crisp, no music, no voice, no speech.` |
| `weapon.guard_break` | `weapon/guard_break_1..2` | 0,8 s | guarda quebrada; aviso de missão falhada | `A defensive guard being smashed open: a harsh metallic clang followed by a cracking crunch and a dull thud, no music, no voice, no speech.` |

## Traje, carcaça e alerta (4 eventos, 6 arquivos)

| Evento | Arquivos | Duração | Onde toca | Prompt |
|---|---|---|---|---|
| `suit.dash` | `suit/dash_1..2` | 0,5 s | esquiva/dash do jogador, dos especiais e o impulso do parkour | `A combat suit boosting into a fast dodge, a short sharp burst of air with a light servo whir, no music, no voice, no speech.` |
| `suit.overheat_alarm` | `suit/overheat_alarm_1` | 0,7 s | traje acima do limite (toca repetido) | `Combat suit overheat warning inside a helmet: a soft rounded two-tone electronic pulse with a faint hiss of venting heat, urgent but not shrill, comfortable when repeated, no music, no voice, no speech.` |
| `carcass.dismantle` | `carcass/dismantle_1..3` | 0,5 s | desmonte da carcaça | `Cutting and prying apart a dead monster's carcass: a wet tearing crunch of tough hide and a crack of chitin plates, no music, no voice, no speech.` |
| `alert.siren` | `alert/siren_1` | 3 s | sirene da invasão (toca de novo a cada 5 s no aviso e entre as ondas) | `City civil defense siren for a giant monster attack: a large rotating electro-mechanical siren with a slow wail rising and falling twice, heard across a city with soft echoes, ominous but not piercing, no music, no voice, no speech.` |

---

## Observações

- **Durações:** mantive perto das atuais para não mudar o ritmo do jogo. A sirene tem que ficar em **3 s**
  (no máximo 4,5 s): o jogo repete a cada 5 s, e mais longa ela se sobrepõe.
- **Volume:** não precisa se preocupar; eu nivelo todos (hoje 6 arquivos distorcem por volume alto demais).
- **Total:** 27 prompts e 66 gerações (uma por variação).
- **Ordem sugerida:** sirene e alarme do traje primeiro, depois os de kaiju, depois as armas.
