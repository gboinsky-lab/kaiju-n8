# Decisões da matriz de duelos (MATRIZ, 2026-11-06)

Miguel pediu para resolver os três [DECIDIR] da `docs/ETAPA7D_MATRIZ.md`. Os números estão em
`tools/data/gen_matriz.py`, que roda por último. As medidas vêm do servidor dedicado com o mesmo `duel.py` e as
mesmas regras da 7-D:
- 3 ou 4 duelos por confronto, a 14 blocos (30 no No. 2 e no No. 6), `/tick rate 60`.
- **V** = o kaiju morreu, **D** = o personagem morreu.
- A vida que sobrou é a do vencedor.

## Referência: quem é quem no dossiê

- **Hoshina** é T5+ e o duelista (nenhum outro o substitui).
- **Soichiro** e **Jura** são T5+.
- **Miguel**, **Camilly**, **Mina** e **Narumi** são T6.
- **No. 13** é T7+ e o **No. 2** e o **No. 6** são T8+.

## 1. Soichiro e Jura contra o No. 13

O Hoshina continua vencendo o No. 13 nos 7 duelos, de 63 a 74 s, com 16 a 37% da vida. Antes, o Soichiro e a Jura
venciam em metade do tempo e com mais vida, ou seja, duelavam melhor que o Hoshina.

| | `kaiju_damage` | Contra o No. 13 |
|---|---|---|
| Soichiro antes | 1,1 | 3 V, 32-35 s, 21-31% |
| **Soichiro agora** | **0,75** | **2 V / 1 D**, 48-56 s, 18-29% (a derrota deixa o No. 13 com 9%) |
| Jura antes | 1,15 | 3 V, 38-40 s, 39-56% |
| **Jura agora** | **0,7** | **3 V**, 64-73 s, 7-12% |

Os dois continuam vencendo um numerado sozinhos, como o Hoshina, que tem o mesmo nível T5+. Agora vencem mais
devagar e com menos sobra que ele. A vida deles (560 e 640) não mudou, pela decisão de 2026-11-06: os mais fortes
ficam.

## 2. Miguel e Camilly (comandantes T6)

| | `kaiju_damage` | Contra o No. 13 |
|---|---|---|
| Miguel | 1,05 (sem mudança) | 3 V, 31-35 s, 24-50% |
| Camilly antes | 0,75 | 3 D, o No. 13 fica com 40-50% |
| Camilly com 0,95 | | 3 D, o No. 13 fica com 22-26% |
| **Camilly agora** | **1,0** | 3 D, o No. 13 fica com 13-26% |
| Camilly com 1,15 (não usado) | | 3 D, o No. 13 fica com 10-30% |
| Mina (T6, atiradora) | 0,6 | 1 V / 2 D; nas derrotas o No. 13 fica com 81% |
| Narumi (T6, corpo a corpo) | 1,3 | 3 V |

Subir o dano acima de 1,0 quase não muda nada: a Camilly morre em cerca de 45 s porque é atiradora contra um kaiju
rápido de perto. A diferença para o Miguel é de papel, não de número. Ela agora fica entre a Mina e o Miguel. Fazer
a Camilly vencer pediria mudar a sobrevivência dela (aparar, esquiva, distância), que é o kit e não o número:
[DECIDIR].

## 3. No. 2 e No. 6 (T8+) contra o Hoshina No. 10 (T7)

No log da IA, o Hoshina No. 10 **sai a pé da área de todo golpe pesado avisado**: foram 244 saídas em 4 duelos
contra o No. 2. Os golpes do No. 2 falham três vezes seguidas, inclusive o soco e a rabada.

Mudanças, todas de dados:
- No. 2:
  - onda de choque com raio 9 → 14, preparo 16 → 12 e alcance 8 → 12;
  - esmagamento com raio 4,5 → 7 e preparo 20 → 16.
- No. 6:
  - avalanche com raio 5 → 7,5 e preparo 22 → 16;
  - campo congelante com raio 10 → 14, dano 0,35 → 0,6 e alcance 9 → 12.
- Os dois: janela parada depois do golpe pesado de 24 e 20 ticks → 12.

| Hoshina No. 10 contra | Antes | Agora |
|---|---|---|
| No. 2 | 3 V, Hoshina com 66-86% | 3 V, Hoshina com 55-77% |
| No. 6 | 3 V, Hoshina com 68-89% | 3 V, Hoshina com 38-76% |

Melhorou pouco, e o motivo é mecânico: o soldado sai da área (`DangerAvoid.exitSpot`) sem conferir se dá tempo de
sair antes do impacto. Contra um corpo de 22-25 m, que tem um preparo longo, ele sempre sai. Para o gigante render
como T8+ é preciso mudar o código [DECIDIR]:

- **(a)** A saída da área só vale se der para chegar fora dela antes do impacto (velocidade × ticks que faltam);
  senão o soldado usa a guarda ou a esquiva.
- **(b)** Deixar como está: o gigante é ameaça de grupo e de destruição nas invasões, e o Hoshina No. 10 derruba
  gigantes, como o No. 10 gigante.

## Segunda rodada (Miguel 2026-11-06: "Só sai se der tempo" e "Mais defesa")

### Saída da área do golpe pesado só com tempo

`DangerAvoid.exitSpot` agora confere se dá tempo de chegar à borda da área antes do impacto. A conta é: distância
até a borda ≤ ticks que faltam × velocidade × `[soldierAi] dangerStepPerSpeed` 2,0. A regra pura é
`DangerZone.reachable` (JUnit). Sem tempo, o soldado fica, e o especial ainda tem a guarda, a esquiva e o
contra-ataque. Vale para todo kaiju com golpe pesado. Com 0 no config, o soldado nunca sai andando; com 10, volta a
sair sempre, como antes.

| Duelo | Regra desligada | Regra ligada |
|---|---|---|
| Hoshina No. 10 × No. 2 | 3 V, Hoshina com 55-77% | 3 V, Hoshina com 60-88% |
| Hoshina No. 10 × No. 6 | 3 V, Hoshina com 38-76% | 3 V |
| Hoshina × No. 13 | 7 V | 3 V |
| Hoshina × No. 10 pequeno | 4 V, Hoshina com 33-64% | 6 V / 1 D, Hoshina com 19-72% |
| Hoshina No. 10 × No. 10 gigante | 3 V | 3 V |
| Mina × No. 10 pequeno | 4/10 (MOV-5D) | 2 V / 1 D |

No log, em 3 duelos contra o No. 2 o Hoshina No. 10 ficou 41 vezes sem tempo de sair e saiu andando 107 vezes. Mesmo
assim vence os gigantes: a guarda da cauda segura golpe pesado de fora da frente, a guarda contra golpe pesado reduz
pela metade o que vem de frente, e o contra-ataque (Kaeshi-uchi) tem invulnerabilidade. É o kit dele, não a
saída a pé. Fazer o No. 2 e o No. 6 renderem como T8+ contra ele pediria mexer nessas defesas [DECIDIR].

### Camilly com mais defesa

Aparar 30% → 45% (o golpe aparado passa com 30% em vez de 40%), esquiva 0,75 → 0,9 com recarga de 100 → 70 ticks,
e distância do alvo 14 → 16 (recua a partir de 10, antes 8). Dano contra kaiju continua 1,0.

| | Contra o No. 13 |
|---|---|
| Camilly antes (0,75) | 3 D, o No. 13 fica com 40-50% |
| Camilly só com o dano 1,0 | 3 D, o No. 13 fica com 13-26% |
| **Camilly com mais defesa** | **1 V / 2 D** |
| Mina (T6, atiradora) | 1 V / 2 D |
| Miguel (T6) | 3 V |

## GIGANTES (2026-11-07): guarda sem segurar gigante

Decisão do Miguel: a guarda contra golpe pesado e a guarda da cauda não seguram golpes do No. 2 e do No. 6 (tag
`kn8:breaks_guards`). O Kaeshi-uchi não mudou. Três duelos cada, servidor dedicado, datapack de isolamento ligado:

| Confronto | Antes (MATRIZ-2) | Agora |
|---|---|---|
| Hoshina No. 10 x No. 2 | 3 V, 55-77% da vida | 3 V em 36-42 s, 30-67% |
| Hoshina No. 10 x No. 6 | 3 V, 38-76% | 3 V em 52-59 s, 32-65% |
| Hoshina No. 10 x No. 10 gigante | 3 V | 3 V em 52-62 s, 60-75% (sem mudança: fora da tag) |

As guardas pesavam pouco: o Hoshina No. 10 sai mais ferido, mas ainda vence os dois. [DECIDIR] o próximo passo.

## GIGANTES-2 (2026-11-07): dano contra especiais

Decisão do Miguel (opção 2): o No. 2 e o No. 6 batem mais forte em soldados especiais (`special_damage` no
`kaiju_ai`; jogador e soldado comum iguais). Três duelos por valor:

| Multiplicador | Hoshina No. 10 x No. 2 | Hoshina No. 10 x No. 6 |
|---|---|---|
| x1,5 | 3 V (23-53% da vida) | 2 V / 1 D |
| x2,5 | **3 D** (duas com o No. 2 a 7%) | 2 V / 1 D (10-20% da vida) |
| x3,0 | — | **3 D** |

Ficou No. 2 x2,5 e No. 6 x3,0. O No. 10 gigante não mudou (o Hoshina No. 10 continua vencendo).
