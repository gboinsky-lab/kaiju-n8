# Balanceamento atual — kaiju e personagens (2026-10-08, versão 0.5.0-C)

Resumo dos números **que estão no jogo agora**, conferidos nos JSON do datapack. Serve de base para o seu arquivo
de balanceamento: a última coluna (**Novo**) das tabelas principais está vazia para você preencher. A tabela
completa (bancada, missões, invasões, patentes, desmonte) continua em `docs/BALANCEAMENTO.md`.

Legenda: **[SUPOSIÇÃO]** = número que eu chutei e você ainda não aprovou jogando.

---

## 1. Como os números funcionam

- **Kaiju:** vida, dano e armadura saem da **fortitude** (`kaiju/<id>.json`):
  - vida = 20 × 2^(fortitude − 2);
  - dano base = 2 × 1,6^(fortitude − 2);
  - armadura = 2 × fortitude (máx. 20).
  - O `overrides` do JSON fixa um valor fora da fórmula (só o No. 10 gigante usa: vida 4.500).
  - Multiplicadores globais no config: `[balance] kaijuHealthMultiplier` e `kaijuDamageMultiplier` (hoje 1,0).
- **Dano de cada habilidade** = dano base do kaiju × multiplicador da habilidade (tabela da seção 3).
- **Jogador e armas:** dano = base da arma × (1 + Release% / 25) × força do corpo.
  - Golpe comum em kaiju tira no máximo **15% da vida máxima** dele.
  - Só o ataque especial da arma (tecla R) passa disso.
- **Soldados contra kaiju:** dano × `kaiju_damage` do nível do soldado (baixo 0,25, normal 0,35, alto 0,8, elite 1,0).
- **Partes do kaiju:** cada kaiju tem multiplicadores próprios por parte (ex.: No. 10 pequeno: cabeça ×1,2, núcleo ×3,0).
  O golpe pesado do jogador expõe o núcleo: ×1,5 a mais por 100 ticks.

## 2. Kaiju

| Kaiju | Categoria | Fortitude | Vida | Dano base | Armadura | Velocidade | Hitbox (L × A) | Fúria | Habilidades (ids) | **Novo** |
|---|---|---|---|---|---|---|---|---|---|---|
| Trichonephila | Yoju | 3,5 | 57 | 4 | 7 | 0,30 | 3,4 × 1,8 | — | bite, leg_stab, leg_swipe, multi_leg, web_shot, leap |  |
| Primigenius | Yoju | 5,4 | 211 | 9,9 | 10,8 | 0,22 | 3,1 × 6 | — | hooved_strike, tail_swipe, slam, charge |  |
| Primigenius ressurgido | Yoju revivido | 5,9 | 299 | 12,5 | 11,8 | 0,22 | 3,1 × 6 | abaixo de 30%: dano ×1,20, vel. ×1,15, recargas ×0,70 | hooved_strike, tail_swipe, slam, charge |  |
| Primigenius Honju | Honju | 6 | 320 | 13,1 | 12 | 0,24 | 5,7 × 9 | — | heavy_punch, bite, tail_swipe, slam, charge, energy_blast |  |
| Primigenius revivido | Honju revivido | 6,4 | 422 | 15,8 | 12,8 | 0,24 | 5,7 × 9 | abaixo de 30%: dano ×1,25, vel. ×1,20, recargas ×0,60 | heavy_punch, bite, tail_swipe, slam, charge, energy_blast_revived |  |
| Trichonephila Honju (Tecedeira Abissal) | Honju | 6,2 | 368 | 14,4 | 12,4 | 0,28 | 6 × 4 | abaixo de 30%: dano ×1,20, vel. ×1,25, recargas ×0,70 | bite, leg_stab, leg_swipe, multi_leg, web_shot, web_burst, leap |  |
| Preondactyl | voador | 6,3 | 394 | 15,1 | 12,6 | 0,30 | 4 × 4 | — | preondactyl_energy_beam, preondactyl_dive_strike, bite, claw, preondactyl_tail_strike |  |
| Kaiju No. 9 | numerado | 8 | 1.280 | 33,6 | 16 | 0,32 | 0,8 × 2 | — | claw, charge, finger_gun |  |
| Kaiju No. 10 (pequeno) | numerado | 8,3 | 1.576 | 38,6 | 16,6 | 0,34 | 2 × 4 | abaixo de 25%: dano ×1,20, vel. ×1,15, recargas ×0,60 | no10_heavy_punch, no10_heavy_smash, no10_tail_sweep, no10_tail_stab, no10_finger_cannon, no10_multi_appendage, charge |  |
| Kaiju No. 10 (gigante) | numerado | 9 | 4.500 | 53,7 | 18 | 0,26 | 10 × 24 | abaixo de 25%: dano ×1,25, vel. ×1,10, recargas ×0,55 | no10g_heavy_punch, no10g_heavy_smash, no10g_tail_sweep, no10g_tail_smash, no10g_finger_cannon, no10g_multi_appendage |  |

Velocidade = atributo de movimento do Minecraft (jogador andando = 0,1). Dano base já é o do golpe ×1,0.

### Regras especiais

| Kaiju | Regra | Valor atual | **Novo** |
|---|---|---|---|
| Kaiju No. 9 | Regeneração | abaixo de 50% da vida: 2,5%/s; abaixo de 20%: 5%/s; para 10 ticks depois de cada golpe | |
| Kaiju No. 9 | Foge com | 15% da vida (dá 250 de mérito a quem causou dano) | |
| Kaiju No. 9 | Reviver carcaças | raio 24 blocos, gesto 3 s, espera 15 s, no máx. 3 revividos vivos | |
| Kaiju No. 9 | Comandar kaiju | raio 32 blocos | |
| Kaiju No. 10 (as duas formas) | Regeneração | abaixo de 50%: 0,8%/s; abaixo de 20%: 2%/s | |
| Kaiju No. 10 pequeno | Vira a forma gigante | depois de 60 s de batalha ou abaixo de 50% da vida (a gigante nasce com a vida cheia) | |
| Kaiju No. 10 | Comandar kaiju | raio 48 blocos | |
| Preondactyl | Voo | 9 blocos acima do alvo, círculo de raio 12, velocidade de voo 0,55; mergulha a cada 8 s | |
| Preondactyl | Couraça | dano pela frente ×0,35, pelas costas ×1,3 | |
| Preondactyl | Autodestruição | abaixo de 15%: 3 s de aviso, raio 6, ×3 de dano | |
| Trichonephila Honju (chefe) | Invoca | 3 Trichonephila (máx. 6 vivas); vida de chefe 551 | |

### Chefes (`boss/<id>.json`)

| Chefe | Kaiju | Vida | Fases | Invoca | Mérito | **Novo** |
|---|---|---|---|---|---|---|
| Honju do Exame | Primigenius Honju | 160 (×0,5) | — | 1 Primigenius (máx. 2) | 100 | |
| Primigenius Honju | Primigenius Honju | 480 (×1,5) | troca em 50% | 2 Primigenius (máx. 4) | 300 | |
| Honju revivido | Primigenius revivido | 844 (×2,0) | 60% e 30% | 2 ressurgidos (máx. 4) | 500 | |

Cada jogador a mais na arena soma +50% de vida ao chefe.

## 3. Habilidades dos kaiju

| Habilidade | Dano (× base) | Preparo | Ativa | Recarga | Pesada* | Área |
|---|---|---|---|---|---|---|
| Mordida | 1,0 | 6 ticks | 2 | 20 | não | — |
| Garra (No. 9) | 1,3 | 8 | 2 | 16 | não | — |
| Slam | 1,5 | 20 | 4 | 100 | sim | raio 3 |
| Investida | 1,2 | 15 | 20 | 160 | sim | raio 1,5; usada de 4 a 14 blocos (0.6) |
| Golpe de casco (0.6) | 1,1 | 8 | 2 | 30 | não | — |
| Rabada (0.6, `kn8:sweep`) | 1,0 | 12 | 4 | 80 | não | alcance +3, setor de 220° atrás e dos lados |
| Soco pesado (0.6) | 1,6 | 12 | 2 | 50 | sim | — |
| Raio de energia (0.6, `kn8:projectile`) | 2,2 | 30 (aviso na boca) | 4 | 240 | sim | de 6 a 32 blocos, explosão de raio 3 |
| Estocada de pata (0.6) | 1,4 | 10 | 2 | 40 | não | — |
| Varredura de patas (0.6) | 0,9 | 8 | 3 | 50 | não | alcance +2, setor de 160° à frente |
| Várias patas (0.6, `kn8:multi_hit`) | 0,5 × 4 golpes | 10 | 14 | 100 | não | um golpe a cada 3 ticks |
| Teia (0.6) | 0,3 | 10 | 2 | 120 | não | de 4 a 16 blocos; lentidão III por 4 s |
| Salto de emboscada (0.6, `kn8:leap`) | 1,3 | 10 | 24 | 140 | sim | de 5 a 12 blocos; área de raio 2 na queda |
| Finger Gun (0.6, No. 9) | 1,0 | 8 | 2 | 30 | não | de 4 a 28 blocos |
| Soco pesado do No. 10 (0.6-E) | 1,8 | 10 | 2 | 50 | sim | quebra blocos (raio 1,5) |
| Esmagamento do No. 10 | 1,6 | 18 | 4 | 140 | sim | raio 4, cratera |
| Varredura / perfuração de cauda do No. 10 | 1,1 / 1,4 | 10 / 8 | 2 | 70 / 60 | não | setor de 220° atrás (alcance +3,5) / alcance +3 à frente |
| Finger Cannon (No. 10) | 1,5 | 14 | 2 | 100 | não | de 5 a 36 blocos, explosão de raio 2 |
| Vários membros (No. 10) | 0,6 × 5 golpes | 12 | 16 | 160 | não | um golpe a cada 3 ticks |
| Versões da forma gigante | 1,6 a 1,8 | 14 a 26 | — | 60 a 180 | as de impacto | esmagamento raio 11 (cratera de 10), varredura 12, pancada de cauda raio 8, Finger Cannon de 10 a 56 blocos (explosão 4), vários membros 0,7 × 6 |
| Raio de energia do Preondactyl (0.6-E) | 2,2 | 32 (aviso na boca) | 4 | 220 | sim | de 6 a 44 blocos, explosão de raio 2,5 |
| Mergulho do Preondactyl | 1,4 | 4 | 2 | 100 | sim | raio 2,5 |
| Golpe de cauda do Preondactyl | 1,1 | 10 | 2 | 60 | não | setor de 200° atrás |
| Explosão de teia (0.6-B, Trichonephila Honju) | 0,8 | 20 | 4 | 200 | não | de 6 a 24 blocos; área de raio 3, lentidão III por 5 s |


## 4. Jogador

| Item | Valor atual | **Novo** |
|---|---|---|
| Vida por patente | Candidato 20, Oficial 22, Oficial Sênior 24, Líder de Pelotão 26, Vice-Capitão 28, Capitão 30 | |
| Limite pessoal inicial (talento) | comum 5–10%; raro (10% de chance) 15–30%; sobe treinando até 100% | |
| Release na tecla G | sobe 2%/s; Shift+G desce 40%/s; só com traje | |
| Acima do limite | no máx. +20; perde 0,06 de vida/s por ponto acima, mais o calor | |
| Fadiga depois de passar do limite | 3 s a 30 s; Lentidão IV, Fraqueza II, sem Release | |
| Por ponto de Release ativo | +0,4% de velocidade, −0,4% de dano recebido (máx. 40%), +0,5 de stamina | |
| Stamina | 100 + 0,5 por ponto de Release; recupera 15/s depois de 1 s; correr gasta 5/s | |
| Calor | só na força total (% = limite) ou acima dele; sobrecarga 70, crítico 90, máximo 100 | |
| Corpo (nível 100) | força +30% de dano corpo a corpo; velocidade +15%; resistência −20% de dano; agilidade −30% de stamina na esquiva/dash | |

### Armas

| Arma | Dano base | Leve | Pesado | Alcance | Especial (R) | **Novo** |
|---|---|---|---|---|---|---|
| Faca de combate | 6 | 12 ticks | ×2,0 / 24 ticks | 3,0 | — | |
| Espada | 8 | 14 | ×2,0 / 28 | 3,5 | — | |
| Machado (da Kikoru) | 11 | 18 | ×2,4 / 32 | 3,2 | Golpe Sísmico: ×2,6 em raio 4, 35 de stamina, recarga 10 s | |
| Espada do Hoshina | 7 | 10 | ×1,9 / 22 | 3,0 | Kūuchi: corte a 12 blocos ×1,6, 20 de stamina, recarga 2 s | |
| Pistola | 3,5 | 6 por tiro | — | 32 | — | |
| Rifle | 5 | 8 por tiro | — | 48 | — | |

Dano com o Release: 10% → ×1,4; 30% → ×2,2; 60% → ×3,4; 100% → ×5.

### Trajes

| Traje | Armadura | Resistência | Corta calor | Patente | **Novo** |
|---|---|---|---|---|---|
| Traje de Treino | 8 | 0 | 0% | Candidato | |
| Mk1 | 12 | 1 | 15% | Oficial | |
| Mk1 Reforçado | 15 | 2 | 30% | Oficial Sênior | |

### Combate

| Item | Valor atual | **Novo** |
|---|---|---|
| Stamina do golpe leve / pesado | 5 / 12 | |
| Esquiva | 20 de stamina, 6 ticks invulnerável | |
| Dash | 25 de stamina | |
| Ataque carregado | 20 de stamina, até ×2,0 em 30 ticks | |
| Bloqueio | segura 70% do dano (golpe pesado de kaiju atravessa) | |
| Parry | janela de 3 ticks (6 com Release ≥ 60%); atordoa 30 ticks; crítico ×1,5 por 40 ticks | |

## 5. Soldados comuns (`soldier/soldier_1.json`)

| Item | Valor atual | **Novo** |
|---|---|---|
| Vida / armadura / velocidade | 24 / 6 / 0,30 | |
| Release por nível | baixo 5%, normal 10%, alto 20%, elite 30% | |
| Dano contra kaiju por nível | baixo ×0,25, normal ×0,35, alto ×0,8, elite ×1,0 | |
| Variantes (peso no sorteio) | rifle + faca (4), pistola + faca (2), espada (2), faca (2), recruta sem arma (0) | |
| Distância | atirador fica a 12 blocos; troca para a faca com o kaiju a menos de 3,5 | |

Medido em jogo (soldados com rifle, kaiju parado): 1 normal mata uma Trichonephila em 12 s; 4 normais em 3,4 s;
1 elite em 2,4 s. Primigenius: 4 normais em 13 s; 1 elite em 9 s.

## 6. Hoshina

| Item | Hoshina | Hoshina + arma numerada 10 | **Novo** |
|---|---|---|---|
| Vida / armadura | 460 / 20 | 520 / 20 | |
| Velocidade | 0,30 (+0,4% por ponto de Release) | igual | |
| Release inicial / máximo | 40% / 92% | 50% / 100% | |
| Escalada em combate | +2%/s com alvo, até +52 (−2%/s sem alvo); recargas −40% no máximo | até +50 | |
| Armas | duas espadas do Hoshina (base 7) | igual + cauda | |
| Cauda | — | corta a cada 30 ticks (alcance 4,5, ×1,0); guarda de costas: dano ×0,3 | |
| Full Release (100%) | — | dano ×1,15, velocidade ×1,15, recargas ×0,7 | |

| Técnica | Golpes (× dano da arma) | Recarga (ticks) | Alcance | **Novo** |
|---|---|---|---|---|
| Kūuchi | 1 × 1,3 a 12 blocos | 30 | 4–12 | |
| Kōsa-uchi | 2 × 0,9 (cortes em X) | 90 | 4–12 | |
| Ran-uchi | 10 × 0,35 | 130 | 0–5 | |
| Kasumi-uchi | 0,5 + 0,5 + 1,5 | 100 | 0–3,5 | |
| Yae-uchi | 8 × 0,45, expõe o núcleo | 240 | 0–3,5 | |
| Jūni-hitoe (só Full Release, traje 10) | 11 × 0,45 + último ×3,5, ignora armadura | 600 | 0–4 | |
| Esquiva / Kaeshi-uchi / parry | dash; contra-golpe ×2,5 contra golpe pesado; parry em 35% dos golpes comuns (dano ×0,3) | 30 / 120 / 20 | — | |

Duelos medidos no servidor dedicado:

| Duelo | Resultado |
|---|---|
| Hoshina × Primigenius Honju | vence em ~10 s quase sem dano |
| Hoshina × Kaiju No. 9 | vence 3/3 em 31–34 s, termina com 63–75% da vida |
| Hoshina × No. 10 pequeno | vence 5/5 em 40–64 s, termina com 4–45% |
| Hoshina × No. 10 gigante | perde 3/3 (deixa o gigante com 11–19%) |
| Hoshina + traje 10 × No. 10 gigante | vence 4/4 em ~1 min, termina com 22–51% |
| Hoshina + traje 10 × No. 9 | vence em ~25 s com 71–88% |

## 7. O que ainda é [SUPOSIÇÃO]

- Fortitude dos Primigenius (5,4 / 5,9 / 6,0 / 6,4), da Tecedeira Abissal (6,2) e do Preondactyl (6,3).
- Todos os números das habilidades da seção 3 (dano, preparo, recarga, área).
- Fúria de cada kaiju e regeneração do No. 10.
- Teto de 15% por golpe comum do jogador, desgaste acima do limite, fadiga e atributos do corpo.
- Números da cauda e do Full Release do Hoshina com a arma numerada 10.
