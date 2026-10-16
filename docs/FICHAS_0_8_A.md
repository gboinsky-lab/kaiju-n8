# Fichas da 0.8-A: Isao (Numbers 2), No. 9 com o Isao e Kaiju No. 13

Modelos do Miguel recebidos em 2026-10-15 (`docs/MODELOS_RECEBIDOS_2026_10_15.md`), implementados em 2026-10-16.
Cada ficha separa **CANON** (o que a obra mostra), **INFERÊNCIA** (leitura para gameplay) e **MOD ORIGINAL**
(escolha do mod, [SUPOSIÇÃO] ou [DECIDIR]).

**Fontes (2026-10-16, só trechos de busca; quase tudo é wiki de fã, nada é confirmação oficial):**
[kaijuno8wiki — Isao](https://kaijuno8wiki.com/characters/isao-shinomiya),
[Fandom — Isao Shinomiya](https://kaiju-no-8.fandom.com/wiki/Isao_Shinomiya),
[VS Battles — Isao](https://vsbattles.fandom.com/wiki/Isao_Shinomiya),
[GameWith — KAIJU NO. 8 THE GAME](https://gamewith.net/kj8-thegame/71513),
[Wikipedia — List of Kaiju No. 8 chapters](https://en.wikipedia.org/wiki/List_of_Kaiju_No._8_chapters),
[CBR — cap. 54](https://www.cbr.com/no-8-chapter-54-spoilers-general-shinomiya-fate-explained/),
[Fandom — Kaiju No. 9](https://kaiju-no-8.fandom.com/wiki/Kaiju_No._9),
[Fandom — Kaiju No. 13](https://kaiju-no-8.fandom.com/wiki/Kaiju_No._13),
[kaijuno8wiki — No. 13](https://kaijuno8wiki.com/characters/kaiju-no.-13),
[Fandom — Rin Shinonome](https://kaiju-no-8.fandom.com/wiki/Rin_Shinonome).

## Isao Shinomiya com a arma numerada 2 (`isao`)

- **CANON:** Diretor-Geral da Força de Defesa, pai da Kikoru, 1,90 m. Numbers 2 = par de manoplas (FS-1002, Izumo
  Tech) feitas do Kaiju No. 2; rajadas sônicas e ondas de choque; luta corpo a corpo com a força vindo das pernas para
  os punhos. Técnicas numeradas: **N.º 1 Barrage Blast** (golpe com cotovelada), **N.º 5 Spiral Blast** (citada, não
  mostrada), **N.º 6 Thunder Emperor** (soco duplo seguido de onda de choque que quebra a guarda e expõe o núcleo).
  Acerta o No. 9 duas vezes e expõe o núcleo dele, mas é morto e absorvido (caps. 52–59, Tóquio).
- **Só no jogo** KAIJU NO. 8 THE GAME: **N.º 2 Force Blast** (golpe em área).
- **INFERÊNCIA:** postura estável, golpes comprometidos, pressão de perto (`aggression` 0,85, distância 3), técnica
  pesada contra kaiju grande (`heavy_vs_big` 2), protege aliados (10), recua só com 12% da vida.
- **MOD ORIGINAL:** números do Balanceamento v1.2 §12 (Isao + No. 2: 700 de vida, armadura 20, velocidade 0,32, T7+).
  Release 50% de base e teto 96% [SUPOSIÇÃO: a obra não dá %]. Spiral Blast = onda que vai longe (projétil reto,
  28 blocos) e Force Blast = leque de 7 ondas a 90° [SUPOSIÇÃO: a obra não mostra]. Guarda contra golpe pesado (45% do
  dano, arco de 140°). Thunder Emperor e Force Blast são especiais pesados (quebram blocos, IA-2). Manoplas são item
  invisível (`kn8:numbers2_gauntlet`, só para NPC): as manoplas já estão no modelo. Onde aparece: guarnição da Base
  Marítima de Ariake e defensor do `ariake_assault` [SUPOSIÇÃO: o QG é o lugar dele; o lugar da morte na obra é
  "Tóquio"].

## Kaiju No. 9 depois de absorver o Isao (`kaiju_no9_isao`)

- **CANON:** a fortitude teria subido de 8,5 para 9,5; usa o rosto e o corpo do Isao (para atormentar a Kikoru); usa
  as técnicas do Isao (o Thunder Emperor quebra a guarda do No. 8 e expõe o núcleo); continua se adaptando na luta;
  cria kaiju mais fortes.
- **INFERÊNCIA:** forma de chefe que mistura o No. 9 preto (Finger Gun, rajada de dedos, tentáculo, voo curto) com as
  manoplas (Barrage Blast, Thunder Emperor, Force Blast e uma onda sônica a distância).
- **MOD ORIGINAL:** números do Balanceamento v1.2 §9 (7.500 de vida, dano 72, armadura 20, velocidade 0,38,
  regeneração 4,5%/s abaixo de 50% e 7,5%/s abaixo de 20%, T9). Pele endurecida e análise um pouco mais fortes que na
  forma preta [SUPOSIÇÃO]. **Como surge:** o No. 9 (base ou preto) absorve o Isao **derrotado** (abaixo de 30% da
  vida; `absorb` com presa `kn8:isao`, que não é kaiju: a absorção agora aceita qualquer mob pelo id do tipo). Altura
  **8 m** (Miguel, 2026-10-16): `dimensions.scale` 4 sobre o modelo de 2 m.

## Kaiju No. 13 (`kaiju_no13`)

- **CANON:** criado pelo No. 9 (o que mais se parece com ele); humanoide alto e pálido, cabeça angulosa, sem olhos,
  dentes serrilhados. Aparece na rodovia Tohoku apostando corrida com carros (~12 km) e depois em Oizumi contra
  líderes de pelotão da Primeira Divisão (caps. 77–83): derruba o Tachibana pela garganta e destrói a metralhadora da
  Rin com um soco; o Kafka vira o No. 8 e o derruba com um soco.
- **Não confirmado:** braços e pernas vermelhos e chifres (só o modelo mostra); altura.
- **INFERÊNCIA:** rápido (investida longa, salto), agarra pelo pescoço (prende o alvo), sequência de socos e soco
  pesado; forte para um esquadrão comum, fraco contra o No. 8.
- **MOD ORIGINAL:** números da proposta do Miguel (2026-10-16): 2.600 de vida, dano 44, armadura 17, velocidade
  0,48, fortitude 9,1 (proposta 9,0–9,2). Agarrão = projétil curto que deixa lento nível 4 por 2 s. Altura **6 m** (Miguel,
  2026-10-16): `dimensions.scale` 2,727 sobre o modelo de 2,2 m. Onde aparece: terceira onda do `ariake_assault` com 50% de chance (o No. 9 traz a criação dele).
