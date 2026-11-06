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
