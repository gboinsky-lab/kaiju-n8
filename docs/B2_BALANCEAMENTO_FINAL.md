# B-2 — Balanceamento final (2026-10-31)

Plano 1.0.0, etapa B-2: matriz de duelos com todos, jogador por patente contra cada kaiju, armas numeradas e apoio
militar. Decisão do Miguel (2026-10-30, F): conferir aqui o `kaiju_damage` do Reno No. 6 e do Isao sem o Numbers 2.

## 1. Matriz de duelos (servidor dedicado, 5 por confronto)

Script de duelo no scratchpad (`duel.py`): chão refeito a cada duelo, mundo plano, `/tick rate 60`, formas do
No. 9 e o No. 10 pequeno isolados por pacote de dados do mundo (sem `transform`/`absorb`). **V** = kaiju morto
(carcaça); **E** = o kaiju escapou na retirada (No. 9 e formas) ou passaram 5 min; **D** = personagem morto. Desde
a retirada do No. 9 (2026-10-22) ele não some mais a 15%: tenta fugir e morre se for pego.

| Confronto | Meta v1.2 | MOV-7 | B-2 V / E / D | Leitura |
|---|---|---|---|---|
| Hoshina × No. 9 | 70% | 0/5/0 | 0 / 5 / 0 | todas escapando na retirada |
| Hoshina × No. 10 pequeno | 58% | 4/1/0 | 5 / 0 / 0 | |
| Hoshina × No. 10 gigante | 10% | 0/0/5 | 0 / 0 / 5 | papel mantido |
| Hoshina No. 10 × No. 9 | 90% | 0/5/0 | 1 / 4 / 0 | |
| Hoshina No. 10 × No. 10 gigante | 60% | 5/0/0 | 4 / 0 / 1 | na meta |
| Hoshina No. 10 × No. 9 preto | 45% | 0/5/0 | 0 / 5 / 0 | |
| Hoshina No. 10 × No. 9 formiga | 35% | 0/5/0 | 1 / 3 / 1 | |
| Hoshina No. 10 × fusão | 18% | 0/0/5 | 0 / 0 / 5 | fora desde o v1.2 |
| Kaiju No. 8 × No. 10 pequeno | 75% | 5/0/0 | 5 / 0 / 0 | |
| Kaiju No. 8 × No. 10 gigante | 45% | 1/4/0 | 2 / 1 / 2 | na meta |
| Kaiju No. 8 × No. 9 | 40% | 0/5/0 | 1 / 4 / 0 | |
| Kaiju No. 8 × No. 9 preto | 20% | 0/5/0 | 1 / 1 / 3 | na meta |
| Kaiju No. 8 × No. 9 formiga | 15% | 0/5/0 | 2 / 1 / 2 | acima (No. 8 com 3 m desde a COMBATE-7) |
| Kaiju No. 8 × fusão | 8% | 0/5/0 | 1 / 2 / 2 | acima (idem) |
| Kikoru × No. 9 | 25% | 0/4/1 | 1 / 4 / 0 | perto |
| Kikoru × No. 10 pequeno | 35% | 0/0/5 | 0 / 0 / 5 | fora desde o v1.2 |
| Kikoru No. 4 × No. 9 | 75% | 0/5/0 | 3 / 2 / 0 | perto |
| Mina × No. 10 gigante (40 blocos) | 30% | 4/1/0 | 2 / 0 / 3 | na meta |
| Mina × No. 9 (40 blocos) | — | 0/3/2 | 3 / 1 / 1 | aceito que ela vença (2026-10-18) |
| Reno × No. 9 | 12% | 0/0/5 | 0 / 0 / 5 | fora desde o v1.2 |
| **Reno No. 6 × No. 9** | 70% | 0/5/0 | 1 / 4 / 0 com 0,80; **6 / 2 / 0 (8) com 0,90** | **ajustado** |
| Narumi × No. 9 | 35% | 0/5/0 | 3 / 2 / 0 | acima |
| Narumi No. 1 × No. 9 | 85% | 0/5/0 | 4 / 1 / 0 | na meta |
| Narumi × No. 13 | — | 4/1/0 | 5 / 0 / 0 | |
| Narumi No. 1 × No. 13 | — | 5/0/0 | 5 / 0 / 0 | |
| Isao × No. 9 | 30% | 0/5/0 | 0 / 5 / 0 | todas escapando |
| Isao × No. 10 pequeno | — | 5/0/0 | 5 / 0 / 0 | |
| Isao × No. 10 gigante | 60% | 3/0/2 | 4 / 0 / 1 | na meta |
| **Isao sem o Numbers 2 × No. 10 pequeno** | 45% | 1/0/4 | **2 / 0 / 3 com 1,2** | **confirmado** |
| Isao sem o Numbers 2 × No. 9 | 30% | — | 1 / 4 / 0 | |

**Decisões F conferidas:**

- **Reno No. 6 `kaiju_damage` 0,80 → 0,90.** Com 0,80 ele leva o No. 9 até a retirada em ~30 s, e o No. 9 escapa
  em 4 de 5. Com 0,90 ele o mata antes em 6 de 8 (75%, meta 70%), com 51-63% da vida sobrando.
- **Isao sem o Numbers 2 `kaiju_damage` 1,2 fica.** 2/5 contra o No. 10 pequeno (meta 45%) e 1/5 contra o No. 9.

**Fora da meta e sem mudança** (5 duelos por confronto é pouco; ficam para o Miguel):

- Kaiju No. 8 × formiga e × fusão acima da meta. É provável que seja efeito do No. 8 com 3 m, que o Miguel aceitou
  sem remedir.
- Narumi × No. 9 acima da meta.
- Kikoru × No. 10 pequeno, Reno × No. 9 e Hoshina No. 10 × fusão abaixo da meta, como desde o v1.2.
- Contra o No. 9, o Hoshina, o Hoshina No. 10 e o Isao só empatam: chegam à retirada e ele escapa. É a regra da
  retirada (2026-10-22): vence quem o mata antes ou o alcança na fuga.

## 2. Jogador por patente

`docs/B2_JOGADOR_POR_PATENTE.md`, gerado por `tools/balance/jogador_por_patente.py`. É conta pelos JSON e pelo
config, sem rodar o jogo, e o Release e o corpo de cada patente são um jogador típico [SUPOSIÇÃO]. O que a conta
mostra:

- **Candidato** (faca, traje de treino, 8%) leva ~21 s para derrubar um Primigenius e cai com ~2 golpes dele. O exame
  de admissão é feito com 4 candidatos de apoio (AJUSTES).
- Do **Oficial Sênior** em diante, Yoju caem em menos de 10 s.
- **Numerados** (No. 9, No. 10, No. 13) seguem sendo inimigos de esquadrão até Vice-Capitão. O jogador cai com 1-2
  golpes deles, como o Miguel decidiu ("o No. 9 é forte de propósito").

## 3. Técnicas e armas numeradas do jogador

Conta por arma, no dano base (sem Release, força, crítico, núcleo e teto): **combo** = dano por segundo do golpe leve
em sequência; **técnicas** = soma de dano por uso / recarga de todas (girando pelo X). A stamina segura o uso: cada
técnica gasta 20-45, e a stamina volta 15 por segundo.

| Arma (patente / requisito) | Combo /s | Técnicas /s (antes → B-2) | Dano por stamina |
|---|---|---|---|
| Faca (Candidato) | 10,7 | 4,3 | 1,1 |
| Espada (Oficial Sênior) | 13,0 | 2,4 | 0,6 |
| Machado (Líder de Pelotão) | 13,1 | 36,0 → **31,6** | 1,0 |
| Espada do Hoshina (Vice-Capitão) | 16,4 | 31,8 | 1,1 |
| Numbers 1 | 16,2 | 41,0 | 1,3 |
| Numbers 2 | 15,1 | 24,5 | 0,95 → **1,1** |
| Numbers 4 | 14,3 | 46,1 → **40,3** | 1,2 |
| Numbers 6 (rifle) | 15,0 | 1,2 | 0,5 (o forte é o gelo: paralisa e expõe o núcleo) |

**Ajustes:**

- **Machado e Numbers 4: recargas × 1,15.** O machado, de patente mais baixa, fazia mais dano em rajada que a
  espada do Hoshina.
- **Numbers 2: 4 a menos de stamina por técnica.** Era a arma numerada que menos rendia por stamina.

Espada e faca continuam com um especial só (armas básicas).

## 4. Apoio militar

Sem mudança:

- O **bombardeio** tira 8% da vida máxima, com teto de 150. Isso dá ~18 de um Primigenius, 150 do No. 10 gigante
  (3%) e 116 do No. 9 (8%). Ajuda, não decide.
- O **reconhecimento** e os **suprimentos** não ferem e continuam com uso por operação.

## 5. Testes

- GameTests 328/328 com os ajustes das técnicas.
- Duelos no servidor dedicado (acima).
