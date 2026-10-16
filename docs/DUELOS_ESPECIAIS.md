# Duelos entre os soldados especiais (2026-10-16)

Pedido do Miguel: "em um combate dos soldados especiais que já temos, quem ganharia?". Eles são aliados e nunca
lutam no jogo normal; para medir foi criado o comando de teste `/kn8 duel <a> <b>` (`DuelCommands`: marca um com o
UUID do outro em `kn8_duel`; só os dois se ferem e miram só um no outro; `/kn8 duel stop` encerra).

Servidor dedicado, mundo plano, `/tick rate 80`, 5 duelos por par, começando a 12 blocos, limite de 300 s, todos
com os números atuais dos `special_soldier/*.json` (`tools`: `duelsp.py` no scratchpad da sessão). 65 pares (o par No.
8 × Kafka não conta: o Kafka vira outro No. 8).

## Ranking

| # | Personagem | Duelos vencidos (de 55) | Confrontos vencidos (de 11) |
|---|---|---|---|
| 1 | No. 8 | 50 de 50 | 10 de 10 |
| 2 | Mina | 45 de 55 | 9 de 11 |
| 3 | Kikoru No. 4 | 40 de 55 | 8 de 11 |
| 4 | Reno No. 6 | 39 de 55 | 8 de 11 |
| 5 | Isao | 38 de 55 | 8 de 11 |
| 6 | Narumi No. 1 | 38 de 55 | 7 de 11 |
| 7 | Narumi | 25 de 55 | 5 de 11 |
| 8 | Hoshina No. 10 | 18 de 55 | 4 de 11 |
| 9 | Kikoru | 13 de 55 | 2 de 11 |
| 10 | Hoshina | 10 de 55 | 2 de 11 |
| 11 | Kafka | 5 de 50 | 1 de 10 |
| 12 | Reno | 4 de 55 | 1 de 11 |

## Matriz (linha × coluna: vitórias da linha – vitórias da coluna)

| | No. 8 | Mina | Kikoru No. 4 | Reno No. 6 | Isao | Narumi No. 1 | Narumi | Hoshina No. 10 | Kikoru | Hoshina | Reno | Kafka |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **No. 8** | — | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | x |
| **Mina** | 0–5 | — | 0–5 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 |
| **Kikoru No. 4** | 0–5 | 5–0 | — | 5–0 | 0–5 | 0–5 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 |
| **Reno No. 6** | 0–5 | 0–5 | 0–5 | — | 5–0 | 4–1 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 |
| **Isao** | 0–5 | 0–5 | 5–0 | 0–5 | — | 3–2 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 |
| **Narumi No. 1** | 0–5 | 0–5 | 5–0 | 1–4 | 2–3 | — | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 |
| **Narumi** | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | — | 5–0 | 5–0 | 5–0 | 5–0 | 5–0 |
| **Hoshina No. 10** | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | — | 3–2 | 5–0 | 5–0 | 5–0 |
| **Kikoru** | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 2–3 | — | 5–0 | 1–4 | 5–0 |
| **Hoshina** | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | — | 5–0 | 5–0 |
| **Reno** | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 4–1 | 0–5 | — | 0–5 |
| **Kafka** | x | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 0–5 | 5–0 | — |

## Leitura e ressalvas

- **O resultado mede o mod, não a obra.** Quatro coisas distorcem um duelo entre humanos:
  - As defesas reativas (esquiva, aparar, contra-ataque e guarda contra golpe pesado) só reagem a golpes de
    **kaiju**: entre especiais ninguém se defende. Quem vive delas (Hoshina, Hoshina No. 10, os dois Narumi) perde
    muito.
  - A cauda do Hoshina No. 10 foi feita para kaiju e quase não entra; o Full Release depende da escalada contra
    kaiju.
  - O `kaiju_damage` não vale entre eles: conta só o dano bruto da arma × técnica × Release. Por isso o canhão da
    Mina (18) e o gelo do Reno No. 6 (lentidão até paralisar) pesam muito, e a IA de atirador deles (recua e circula)
    deixa os de corpo a corpo correndo atrás sem alcançar.
  - O Kafka tem 24 de vida e quase sempre morre antes de virar o No. 8.
- **Kaiju No. 8** vence todos (5–0 em todos os confrontos).
- **Mina** só perde para o No. 8 e para a Kikoru No. 4 (o voo chega por cima e ela não consegue recuar).
- **Kikoru No. 4** perde para o No. 8, o Isao e o Narumi No. 1, e vence a Mina e o Reno No. 6.
- **Isao** vence todos os de corpo a corpo, mas perde 0–5 para a Mina e para o Reno No. 6.
- **Hoshina No. 10** fica em 8º, abaixo do Narumi normal: é o mais prejudicado pela falta das defesas contra
  soldados.

[DECIDIR] Se o Miguel quiser duelos "pela obra", as defesas reativas, a cauda e a escalada precisariam reagir também
a soldados quando há duelo (mudança só no modo de teste, sem mexer no jogo normal).
