# BALANCEAMENTO.md — Todos os números do jogo (0.3, 2026-10-07)

Um lugar só para ver e ajustar o balanceamento. Nada aqui está no Java (regra 2): cada número mora num JSON do
datapack (`src/main/resources/data/kn8/kn8/...`) ou no config do servidor (`<instância>/config/kn8-server.toml`,
seção entre colchetes). Para mudar: edite o arquivo indicado; JSON vale com `/reload`, config vale ao salvar.

Legenda da última coluna: **✅** testado e ok · **[SUPOSIÇÃO]** número chutado, falta você jogar · **⚠ proposta**
algo que eu mudaria (não mudei sem a sua aprovação).

---

## 1. Kaiju

Vida, dano e armadura saem da **fortitude** (`kaiju/<id>.json`) pela curva do config `[fortitudeCurve]`:
vida = 20 × 2^(fortitude − 2), dano = 2 × 1,6^(fortitude − 2), armadura = 2 × fortitude (máx. 20).
Multiplicadores globais: `[balance] kaijuHealthMultiplier` / `kaijuDamageMultiplier` (1,0).

| Kaiju | Fortitude | Vida | Dano base | Armadura | Velocidade | Habilidades | Observação |
|---|---|---|---|---|---|---|---|
| Trichonephila (Yoju) | 2,5 | **28** | 2,5 | 5 | 0,30 | mordida | ⚠ proposta: **3,5 (57 de vida)** — hoje um soldado elite a mata em < 1 s e ela some das ondas antes do jogador chegar |
| Primigenius (Yoju) | 5,4 | 211 | 9,9 | 10,8 | 0,22 | slam, investida | [SUPOSIÇÃO] |
| Primigenius ressurgido | 5,9 | 299 | 12,5 | 11,8 | 0,22 | slam, investida | [SUPOSIÇÃO] |
| Primigenius Honju | 6,0 | 320 | 13,1 | 12,0 | 0,24 | slam, investida, mordida | [SUPOSIÇÃO] |
| Primigenius revivido (Honju) | 6,4 | 422 | 15,8 | 12,8 | 0,24 | slam, investida, mordida | [SUPOSIÇÃO] |
| Kaiju No. 9 | 6,5 | 453 | 16,6 | 13,0 | 0,32 | garra, investida | [SUPOSIÇÃO]; a garra (×1,3) tira ~21 por golpe |

### Habilidades (`ability/<id>.json`)

| Habilidade | Dano (× base) | Preparo | Ativa | Recarga | Pesada* | Área |
|---|---|---|---|---|---|---|
| Mordida | 1,0 | 6 ticks | 2 | 20 | não | — |
| Garra (No. 9) | 1,3 | 8 | 2 | 16 | não | — |
| Slam | 1,5 | 20 | 4 | 100 | sim | raio 3 |
| Investida | 1,2 | 15 | 20 | 160 | sim | raio 1,5 |

\* Pesada atravessa o bloqueio comum; só parry ou esquiva evitam (`[combat] heavyIgnoresBlock`) [SUPOSIÇÃO].

### Chefes (`boss/<id>.json`)

| Chefe | Kaiju | Vida (× JSON) | Vida final | Fases (troca em) | Invoca | Mérito |
|---|---|---|---|---|---|---|
| Honju do Exame | Primigenius Honju | ×0,5 | 160 | — | 1 Primigenius (máx. 2) | 100 |
| Primigenius Honju | Primigenius Honju | ×1,5 | 480 | 50% | 2 Primigenius (máx. 4) | 300 |
| Honju revivido | Primigenius revivido | ×2,0 | 844 | 60%, 30% | 2 ressurgidos (máx. 4) | 500 |

Cada jogador a mais na arena soma +50% de vida (`[boss] playerScaling`).

### Kaiju No. 9 (`numbered/kaiju_no9.json`)

| Número | Valor |
|---|---|
| Reviver: raio / gesto / espera | 24 blocos / 60 ticks (3 s) / 300 ticks (15 s) |
| Máx. revividos vivos (modo normal) | 3 |
| Comandar kaiju (raio) | 32 blocos |
| Foge com | 30% da vida, dando 250 de mérito a quem está a até 48 blocos |
| Ressurreição em massa: gesto / intervalo | 100 ticks (5 s) / 4 ticks entre um kaiju e o próximo |
| Revive | Primigenius → ressurgido · Honju → chefe **Honju revivido** · aranha → aranha [SUPOSIÇÃO: falta modelo de aranha ressurgida] |

---

## 2. Jogador

### Armas (`weapon/<id>.json`)

Dano = base × (1 + Release% / 25) (`[fortitudeCurve] releaseDamageDivisor`).

| Arma | Base | 0% | 10% | 30% | 60% | 100% | Golpe leve | Pesado | Alcance |
|---|---|---|---|---|---|---|---|---|---|
| Faca | 6 | 6 | 8,4 | 13,2 | 20,4 | 30 | 12 ticks | ×2,0 / 24 ticks | 3,0 |
| Espada | 8 | 8 | 11,2 | 17,6 | 27,2 | 40 | 14 | ×2,0 / 28 | 3,5 |
| Machado | 11 | 11 | 15,4 | 24,2 | 37,4 | 55 | 18 | ×2,4 / 32 | 3,2 |
| Pistola | 3,5 | 3,5 | 4,9 | 7,7 | 11,9 | 17,5 | 6 | — | 32 |
| Rifle | 5 | 5 | 7 | 11 | 17 | 25 | 8 | — | 48 |

⚠ proposta — **a patente da arma está em dois lugares e não bate**: o `required_rank` do JSON da arma (faca e
pistola Candidato; rifle, espada e machado Oficial) é diferente dos `unlocks` das patentes (pistola e rifle Oficial,
espada Oficial Sênior, machado Líder de Pelotão). Hoje quem vale na bancada são os `unlocks`. Proponho apagar o
`required_rank` das armas e deixar só os `unlocks`.

### Combate (`[combat]`)

| Número | Valor | | Número | Valor |
|---|---|---|---|---|
| Stamina do golpe leve / pesado | 5 / 12 | | Parry: janela (≥ 60% Release) | 3 ticks (6) |
| Esquiva: stamina / invulnerável | 20 / 6 ticks | | Parry: atordoa / devolve stamina | 30 ticks / 10 |
| Dash: stamina / velocidade | 25 / 1,6 | | Crítico depois do parry | ×1,5 por 40 ticks |
| Ataque carregado: stamina / máx. | 20 / ×2,0 em 30 ticks | | Bloqueio segura | 70% do dano |
| Núcleo exposto (golpe pesado) | ×1,5 por 100 ticks | | Combo: janela | 10 ticks |

### Release, stamina e calor

| Número | Valor | Onde |
|---|---|---|
| Release máximo | 100% para todos, por treino | `[career] releaseMax` |
| XP de treino por ponto | 50 + 10 × pontos já treinados (1% custa 50; 99→100% custa 1.040) | `[training]` |
| Por ponto de Release | +0,4% velocidade, −0,4% de dano recebido (máx. 40%), +0,5 de stamina | `[power]`, `[stamina]` |
| Stamina | 100 + 0,5 por ponto; recupera 15/s depois de 1 s | `[stamina]` |
| Corrida | gasta 5/s; volta a correr com 20 | `[stamina]` [SUPOSIÇÃO] |
| Calor | morno 40, sobrecarga 70 (+10% dano, −0,5 vida/s), crítico 90 (−1/s), pane em 100 (4 de dano, Release 1% por 10 s) | `[heat]` |
| Esfriar | 10/s fora de combate, 3/s em combate | `[heat]` |

### Fontes de XP de treino e mérito (`[career]`)

| Fonte | XP de treino | Mérito |
|---|---|---|
| Dano em kaiju | 0,5 por ponto | — |
| Abater Yoju / Honju ou maior | 40 / 200 | 25 / 150 |
| Etapa de desmonte / desmonte completo | 5 por etapa | 15 |
| Boneco de treino | 5 por golpe, máx. 60 por minuto | — |
| Catalisador de Release | 500 | — |
| Fuga do No. 9 | — | 250 |

### Patentes (`rank/<id>.json`)

| Patente | Mérito | Missão | Vida | Esquadrão | Libera |
|---|---|---|---|---|---|
| Candidato | 0 | — | 20 | 0 | faca, traje de treino, boneco |
| Oficial | 0 | Exame de Admissão | 22 | 0 | rifle, pistola, Mk1 |
| Oficial Sênior | 500 | — | 24 | 2 | espada, Mk1 Reforçado |
| Líder de Pelotão | 1.500 | — | 26 | 4 | machado, catalisador |
| Vice-Capitão | 4.000 | — | 28 | 6 | — |
| Capitão | 10.000 | — | 30 | 8 | — |

⚠ observação: no teste, uma única invasão nível 5 deu ~1.750 de mérito (invasão + chefe + fuga do No. 9) e levou de
Oficial Sênior a Vice-Capitão de uma vez. Se quiser carreira mais longa, dobre os méritos de Vice-Capitão e Capitão
ou reduza a recompensa dos níveis 4–5.

### Trajes (`suit/<id do item>.json`, vestidos no peito)

| Traje | Armadura | Resistência | Corta calor | Patente |
|---|---|---|---|---|
| Traje de Treino | 8 | 0 | 0% | Candidato |
| Mk1 | 12 | 1 | 15% | Oficial |
| Mk1 Reforçado | 15 | 2 | 30% | Oficial Sênior |

### Suprimentos (`[supply]`)

Resfriador tira 50 de calor · Estimulante enche a stamina · Catalisador dá 500 de XP de treino.

---

## 3. Bancada (`workbench/<id>.json`)

| Receita | Rende | Materiais |
|---|---|---|
| Boneco de treino | 1 | 1 fardo de feno, 4 gravetos, 1 couro |
| Faca de combate | 1 | 2 ferros, 1 graveto |
| Pistola | 1 | 4 ferros, 2 redstone, 1 tecido |
| Rifle | 1 | 6 ferros, 4 redstone, 2 fibras |
| Espada | 1 | 5 ferros, 2 fibras, 2 tecidos |
| Machado | 1 | 6 ferros, 3 fibras, 1 fragmento de núcleo |
| Traje de Treino | 1 | 4 couros, 2 tecidos |
| Mk1 | 1 | 6 ferros, 4 tecidos, 4 fibras |
| Mk1 Reforçado | 1 | 2 diamantes, 6 fibras, 2 fragmentos |
| Resfriador | 2 | 1 gelo, 1 tecido |
| Estimulante | 2 | 2 açúcares, 1 fibra, 1 frasco |
| Catalisador | 1 | 3 fragmentos, 2 pó de glowstone |

A bancada em si: 7 ferros + mesa de trabalho + bloco de redstone (receita vanilla `recipe/defense_workbench.json`).

### Desmonte (`dismantle/<id>.json`)

| Carcaça | Etapas | Tempo por etapa | Materiais |
|---|---|---|---|
| Trichonephila | 3 | 1,5 s | 1–2 tecidos + fragmento (núcleo quebrado) ou núcleo inteiro |
| Primigenius (todos, inclusive Honju) | 5 | 2 s | 4–6 tecidos, 1–2 fibras + fragmento ou núcleo inteiro |

⚠ proposta: o Honju usa a mesma tabela do Yoju; uma tabela própria (mais etapas, mais material) recompensaria a luta.

---

## 4. Missões (`mission/<id>.json`)

| Missão | Precisa | Objetivo | Prazo | Mérito | XP | Espera p/ repetir |
|---|---|---|---|---|---|---|
| Primeiro Desmonte | — | desmontar 1 aranha | — | 20 | 60 | — |
| Ninho de Aranhas | Candidato | 4 aranhas | 10 min | 40 | 100 | 3 min |
| Extermínio de Yoju | Candidato | 2 Primigenius | 10 min | 60 | 150 | 5 min |
| Exame de Admissão | Candidato + Primeiro Desmonte | ponto, 3 Primigenius, Honju do Exame | 30 min | 100 → Oficial | 300 | — |
| Patrulha | Oficial | 3 pontos | 5 min | 40 | 80 | 20 min |
| Defesa da Cidade | Oficial | invasão nível 2 | — | 120 | 200 | 20 min |
| Ameaça Revivida | Oficial + Defesa da Cidade | invasão do No. 9 (nível 3) | — | 300 | 400 | 40 min |
| Caça ao Honju | Oficial Sênior | ponto, Primigenius Honju | 20 min | 400 | 600 | 40 min |
| Contenção da Horda | Oficial Sênior | invasão nível 4 | — | 400 | 600 | 40 min |
| Noite da Ressurreição | Oficial Sênior + Horda + Ameaça Revivida | invasão nível 5 | — | 1.000 | 1.500 | 1 h |

(20 min de jogo = 1 dia de Minecraft.)

## 5. Invasões (`invasion/<id>.json`)

| Invasão | Nível | Ondas (kaiju) | Defensores | Prazo | Mérito | XP | Natural |
|---|---|---|---|---|---|---|---|
| Enxame de Trichonephila | 1 | 3 + 5 aranhas | 2 rifle + 1 espada (normal) | 10 min | 80 | 150 | peso 3 |
| Ataque dos Primigenius | 2 | 2 + 4 + 3 | 3 rifle + 1 machado alto | 15 min | 150 | 300 | peso 2 |
| Ofensiva do Honju | 3 | 3 + (2 + Honju) | 4 rifle + 2 espada (alto) | 20 min | 400 | 600 | peso 1 |
| Ressurreição do No. 9 | 3 | 2 + (No. 9 + 1) | 3 rifle + 1 espada (alto) | 20 min | 350 | 500 | só missão |
| Horda Kaiju | 4 | 10 + (9 + Honju) | 3 rifle + 1 espada (normal) | 30 min | 700 | 900 | só missão |
| Ressurreição em Massa | 5 | horda + No. 9 revivendo tudo | 3 rifle + 1 espada | 40 min | 1.500 | 2.000 | só missão |

Chance de invasão natural por noite: 20% (`[invasion] naturalChance`) [SUPOSIÇÃO].

### Soldados (`soldier/soldier_1.json`)

Vida 24, armadura 6, alcance de visão 40. Níveis de força (Release): baixo 5, normal 10, alto 20, elite 30.
⚠ proposta: um soldado **elite** com rifle tira ~11 por tiro, 2,5 tiros por segundo; somado à aranha frágil, ondas
inteiras acabam sem o jogador. Proponho um multiplicador de dano dos soldados contra kaiju no config (`[soldier]
damageVsKaiju`, por exemplo 0,5) para eles ajudarem sem resolver sozinhos.
