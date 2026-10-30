# BALANCEAMENTO.md — Todos os números do jogo (balanceamento v1.0, 2026-10-08)

Um lugar só para ver e ajustar o balanceamento. Nada aqui está no Java (regra 2): cada número mora num JSON do
datapack (`src/main/resources/data/kn8/kn8/...`) ou no config do servidor (`<instância>/config/kn8-server.toml`,
seção entre colchetes). Para mudar: edite o arquivo indicado; JSON vale com `/reload`, config vale ao salvar.

Legenda da última coluna: **✅** testado e ok · **[SUPOSIÇÃO]** número chutado, falta você jogar · **⚠ proposta**
algo que eu mudaria (não mudei sem a sua aprovação).

---

## 1. Kaiju

Vida, dano e armadura saem da **fortitude** (`kaiju/<id>.json`) pela curva do config `[fortitudeCurve]`:
vida = 20 × 2^(fortitude − 2), dano = 2 × 1,6^(fortitude − 2), armadura = 2 × fortitude (máx. 20).
**Balanceamento v1.0 do Miguel (2026-10-08, `docs/BALANCEAMENTO_COMPLETO_v1_0.md`):** os 10 kaiju têm vida, dano e
armadura fixados no `overrides` do JSON (a fortitude continua valendo para o resto); a tabela abaixo já mostra os
valores novos, com o antigo entre parênteses.
Multiplicadores globais: `[balance] kaijuHealthMultiplier` / `kaijuDamageMultiplier` (1,0).

| Kaiju | Fortitude | Vida | Dano base | Armadura | Velocidade | Habilidades | Observação |
|---|---|---|---|---|---|---|---|
| Trichonephila (Yoju) | 3,5 | **65** (57) | 4,2 (4,0) | 7 | 0,31 (0,30) | mordida, estocada, varredura de patas, várias patas, teia, salto | ✅ aprovado (era 2,5 / 28 de vida): kaiju mais fortes que soldados comuns |
| Primigenius (Yoju) | 5,4 | 220 (211) | 10,0 (9,9) | 11 (10,8) | 0,23 (0,22) | casco, rabada, slam, investida | [SUPOSIÇÃO] |
| Primigenius ressurgido | 5,9 | 310 (299) | 12,8 (12,5) | 12 (11,8) | 0,24 (0,22) | casco, rabada, slam, investida; fúria | [SUPOSIÇÃO] |
| Primigenius Honju | 6,0 | 360 (320) | 13,8 (13,1) | 12 | 0,25 (0,24) | soco pesado, mordida, rabada, slam, investida, raio de energia | [SUPOSIÇÃO] |
| Primigenius revivido (Honju) | 6,4 | 460 (422) | 16,5 (15,8) | 13 (12,8) | 0,25 (0,24) | os do Honju (raio roxo); fúria | [SUPOSIÇÃO] |
| Trichonephila Honju (0.6-B) | 6,2 | 400 (368); 600 como chefe | 15,0 (14,4) | 13 (12,4) | 0,29 (0,28) | os da aranha + explosão de teia; fúria (dano ×1,2, velocidade ×1,25, recargas ×0,7) | [SUPOSIÇÃO]; hitbox 6 × 4; chefe invoca 3 Trichonephila (máx. 6 vivas) |
| Kaiju No. 10, forma pequena (0.6-E) | 8,3 | 1.650 (1.576) | 39,5 (38,6) | 17 (16,6) | 0,35 (0,34) | soco pesado, esmagamento, varredura e perfuração de cauda, Finger Cannon, vários membros, investida | 4 m (hitbox 2 × 4); regenera; vira a forma gigante depois de 60 s de batalha ou abaixo de 50% da vida; fúria abaixo de 25% (dano ×1,2, velocidade ×1,15, recargas ×0,6); comanda kaiju a até 48 blocos |
| Kaiju No. 10, forma gigante (0.6-E) | 9,0 | **4.600** (4.500) | 54,5 (53,7) | 18 | 0,27 (0,26) | versões gigantes (esmagamento de raio 11, varredura de cauda 12, pancada de cauda, Finger Cannon de 64 blocos) | 24 m (hitbox 10 × 24); nasce com a vida cheia e quebra blocos num raio de 8; regenera; fúria abaixo de 25% (dano ×1,25, velocidade ×1,1, recargas ×0,55) |
| Preondactyl (0.6-E, voador) | 6,3 | 430 (394) | 15,5 (15,1) | 13 (12,6) | 0,31 (0,30) (voo 0,55) | raio de energia, mergulho, mordida, garra, golpe de cauda | voa a 9 blocos acima do alvo, em círculo de raio 12; mergulha a cada 8 s; frente ×0,35, costas ×1,3; autodestruição abaixo de 15% (3 s de aviso, raio 6, ×3 de dano); obedece ao No. 10 [SUPOSIÇÃO] |
| Philinosoma (0.7-A, Honju lagarto) | 6,1 | 380 | 14,2 | 12 | 0,27 | soco pesado, mordida, rabada, investida, disparo de espinhos | [SUPOSIÇÃO] tier T4 (entre o Honju 360 e o Preondactyl 430); hitbox 6 × 9; cauda como parte ×0,7 |
| Diclonius (0.7-A, Honju em pé) | 6,4 | 430 | 15,5 | 14 | 0,23 | soco pesado, mordida, rabada, slam, sopro de energia | [SUPOSIÇÃO] tier T4+: lento e o mais blindado dos Honju; hitbox 5 × 9 |
| Camponotus (0.7-A, formiga Yoju) | 4,8 | 200 | 9,5 | 10 | 0,33 | mordida, estocada, varredura de patas, jato de ácido, salto | [SUPOSIÇÃO] tier T3 (pouco abaixo do Primigenius 220, mais rápida); hitbox 4 × 2,9; núcleo na cabeça |
| Camponotus revivida (0.7-A) | 5,5 | 290 | 11,5 | 11 | 0,34 | os da formiga com ácido mais forte; fúria (dano ×1,2, velocidade ×1,15, recargas ×0,7) | [SUPOSIÇÃO] tier T3+ (abaixo do ressurgido 310) |
| Phaneroplasmodium (0.7-B, cogumelo Yoju) | 5,2 | 240 | 10,5 | 9 | 0,27 | mordida, estocada, varredura de patas, várias patas, nuvem de esporos | [SUPOSIÇÃO] tier T3; hitbox 4,4 × 5; núcleo no chapéu |
| Myxogasterocarp (0.7-B, cogumelo Honju) | 6,3 | 420 | 15,0 | 13 | 0,20 | mordida, estocada, varredura de raízes, várias raízes, bomba de esporos | [SUPOSIÇÃO] tier T4; o mais lento; hitbox 7 × 9; núcleo no caule |
| Larva misteriosa (0.7-B, voadora) | 1,5 | 20 | 2,0 | 0 | 0,30 (voo 0,45) | mordida, mergulho | [SUPOSIÇÃO]; 0,5 m (hitbox 0,5 × 0,5), sem partes; voa a 3 blocos do alvo em círculo de raio 4 (`flyer/kaiju_larva.json`), sem autodestruição; categoria `numbered` (origem do No. 8) |
| Kaiju No. 9 | 8,0 | 1.450 (1.280) | 34,5 (33,6) | 16 | 0,34 (0,32) | garra, investida, Finger Gun | 0.6-D (Miguel: vilão principal, forte de propósito; o Hoshina vence, mas não com facilidade); a garra (×1,3) tira ~44 por golpe; regenera |

### Habilidades (`ability/<id>.json`)

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
| Disparo de espinhos do Philinosoma (0.7-A) | 1,2 | 16 | 2 | 160 | não | de 5 a 24 blocos, projétil azul 1,6 bloco/tick [SUPOSIÇÃO] |
| Sopro de energia do Diclonius (0.7-A) | 2,4 | 32 (aviso na boca) | 4 | 260 | sim | de 6 a 34 blocos, explosão de raio 3,5 [SUPOSIÇÃO] |
| Jato de ácido da Camponotus (0.7-A) | 0,6 | 10 | 2 | 120 | não | de 4 a 14 blocos, Lentidão II por 3 s [SUPOSIÇÃO] |
| Jato de ácido da revivida (0.7-A) | 0,75 | 10 | 2 | 110 | não | de 4 a 14 blocos, Lentidão III por 3,5 s [SUPOSIÇÃO] |
| Nuvem de esporos do Phaneroplasmodium (0.7-B) | 0,7 | 14 | 2 | 130 | não | de 4 a 16 blocos, Lentidão II por 3 s [SUPOSIÇÃO] |
| Bomba de esporos do Myxogasterocarp (0.7-B) | 2,0 | 28 (aviso) | 4 | 220 | sim | de 6 a 28 blocos, explosão de raio 3,5, Lentidão II por 3 s [SUPOSIÇÃO] |
| Explosão de teia (0.6-B, Trichonephila Honju) | 0,8 | 20 | 4 | 200 | não | de 6 a 24 blocos; área de raio 3, lentidão III por 5 s |

\* Pesada atravessa o bloqueio comum; só parry ou esquiva evitam (`[combat] heavyIgnoresBlock`) [SUPOSIÇÃO].

Escolha (0.6): entre as habilidades prontas e ao alcance, vence a de maior `behavior.priority` (empate: sorteio);
as de distância precisam de linha de visão. Todos os números das habilidades da 0.6 são [SUPOSIÇÃO].

**Fúria** (`rage` no `kaiju/<id>.json`, 0.6): abaixo de 30% de vida, de uma vez. Ressurgido: dano ×1,2, velocidade
×1,15, recargas ×0,7. Revivido: dano ×1,25, velocidade ×1,2, recargas ×0,6 [SUPOSIÇÃO].

**Empurrão dos soldados no kaiju** (`[kaiju] soldierKnockback`, 0.6): 0 (antes, o empurrão vanilla de cada golpe
impedia o kaiju de chegar perto de um grupo de soldados).

**IA dos kaiju** (`[kaijuAi]`, IA-1A, Miguel 2026-10-15) [SUPOSIÇÃO nos números]:

| Chave | Valor | Efeito |
|---|---|---|
| `smartIntelligence` | 4 | inteligência (JSON) a partir da qual o kaiju é "inteligente": No. 9 e formas (5), No. 10 (4) |
| `hearingFraction` | 0,35 | o comum só percebe quem não vê dentro de 35% do alcance (Primigenius: 32 × 0,35 = 11,2 blocos) |
| `retargetTicks` | 20 | reavalia o alvo a cada 1 s |
| `switchMargin` | 2 | só troca de alvo se o novo for 2 blocos "melhor" |
| `shooterBonus` | 12 | inteligente: atirador conta como 12 blocos mais perto |
| `threatPerDamage` / `maxThreatBonus` | 0,5 / 16 | inteligente: cada ponto de dano recebido aproxima o agressor 0,5 bloco, no máximo 16 |
| `threatMemoryTicks` | 200 | lembra quem o feriu por 10 s (o comum percebe esse agressor mesmo sem ver) |
| `packAlertRadius` | 16 | bando (aranha): vizinhas da mesma espécie ficam sabendo do agressor |
| `searchTicks` | 200 | sem alvo, vai à última posição conhecida e procura por até 10 s |
| `progressCheckTicks` / `minProgress` | 60 / 1 | a cada 3 s precisa chegar 1 bloco mais perto ou ferir o alvo |
| `giveUpTicks` / `unreachableTicks` | 200 / 400 | 10 s sem progresso: alvo inalcançável e ignorado por 20 s |
| `commandTicks` | 200 | alvo dado pelo No. 9/No. 10 vale 10 s |
| `telegraphHeavy` | true | IA-6: golpe pesado marca no chão a área ameaçada durante o preparo (anel, arco ou linha; amarelo → vermelho perto do impacto) e toca um som grave no início |
| `telegraphIntervalTicks` / `telegraphPoints` | 4 / 28 | IA-6: um pulso do aviso a cada 4 ticks, 28 partículas por pulso |
| `bossPhaseBonus` | 3 | IA-6: os ataques da fase atual do chefe (`boss/*.json`) ganham +3 de prioridade; o peso do JSON decide o empate; ataque listado que a espécie não tem é liberado |
| `adaptationMemory` | 6 | IA-6: a análise do No. 9 lembra no máximo 6 tipos de golpe (esquece o mais antigo); tipo sem marcas sai da memória |
| `heavyWindupEasy` / `heavyWindupNormal` / `heavyWindupHard` | 1,25 / 1,0 / 0,85 | IA-7 (Miguel 2026-10-16): preparo dos golpes pesados de kaiju pela dificuldade do mundo (Fácil e Pacífico / Normal / Difícil); pisão de 20 ticks vira 25 / 20 / 17; o aviso continua aparecendo |
| `scanIntervalTicks` | 5 | IA-7: buscas de vizinhos da IA (reações dos especiais, kaiju se espalhando) refeitas a cada 5 ticks |
| `farFromPlayers` / `farRetargetFactor` | 96 / 4 | IA-7: sem jogador a 96 blocos, a escolha de alvo de kaiju e soldados fica 4x mais espaçada (alvo morto reavalia na hora) |
| `debugLog` | false | IA-7: com true, cada troca de alvo vai para o log do servidor com o motivo |
| `[destruction] sinkHeight` | 3 | Miguel 2026-10-16: kaiju em pé sobre construção 3+ blocos acima do alvo ou do chão natural (tag `kn8:destruction/natural_ground`) quebra o piso embaixo e desce (inclusive baú/púlpito, soltando o conteúdo) |

Fases dos chefes (IA-6) [SUPOSIÇÃO nos pesos]: **Honju** acima de 50% soco pesado 2, pisão 3, mordida 1; abaixo, investida 3, raio 3, pisão 2. **Revivido** soco 2 / pisão 3 → investida 2, raio 3, pisão 2 → investida 3, raio 3, rabada 1. Tecedeira e Honju do exame sem mudança.

**IA dos soldados** (`[soldierAi]`, IA-1B, Miguel 2026-10-15) [SUPOSIÇÃO nos números]:

| Chave | Valor | Efeito |
|---|---|---|
| `detectionRange` | 64 | soldados (comuns e especiais) detectam kaiju a até 64 blocos, vendo ou não |
| `retargetTicks` | 20 | reavalia o alvo a cada 1 s |
| `switchMargin` | 3 | só troca de alvo se o novo for 3 blocos "melhor" |
| `coverBonus` | 12 | kaiju mirando um aliado que está recarregando conta como 12 blocos mais perto (cobertura) |
| `revengeBonus` / `revengeTicks` | 6 / 100 | kaiju que feriu o soldado nos últimos 5 s conta como 6 blocos mais perto |
| `reloadRetreat` | 6 | recarregando, o atirador recua até a distância de manter + 6 |
| `outOfAmmoWeapon` | `kn8:combat_knife` | arma branca de quem fica sem munição e não tem arma de apoio (Mina, Reno, Kafka) |
| `landingMaxDrop` | 3 | MOV-1: dash, avanço de técnica, esquiva e saída de luta travada só com chão no destino até 3 blocos abaixo |
| `landingMinFraction` | 0,5 | MOV-1: com parede no caminho, o deslocamento vale se chegar a pelo menos metade da distância |
| `dashBlocksPerSpeed` | 5,0 | MOV-1: distância conferida do dash = velocidade do impulso × 5 blocos |
| `stuckTicks` / `stuckMinMove` | 60 / 1,0 | MOV-1: soldado (comum e especial) que quer chegar ao alvo e anda menos de 1 bloco em 3 s está travado |
| `unstuckStep` | 5,0 | MOV-1: travado, pula obstáculo de até 1,2 ou anda 5 blocos para um lado com chão (o outro lado, ou para trás se travar de novo no mesmo lugar); sem teleporte |
| `leapMaxHeight` | 3,0 | MOV-2: obstáculo ou borda mais alta onde o soldado salta (blocos acima dos pés; até 1,2 é o pulo vanilla) |
| `leapMaxDistance` | 4,0 | MOV-2: maior vão/plataforma do salto depois da beira (com até 1,5 bloco de corrida antes dela) |
| `leapCheckTicks` | 10 | MOV-2: querendo andar e andando menos de 0,5 bloco em 0,5 s, procura um salto para o alvo |
| `leapCooldownTicks` | 40 | MOV-2: espera entre dois saltos |
| queda do caminho | = `landingMaxDrop` (3) | MOV-2: o caminho do soldado só desce quedas de até 3 (o vanilla aceitava ~18 com vida cheia e alvo) |
| `cover` | true | MOV-3: atirador comum usa cobertura contra kaiju com ataque a distância (projétil) e para recarregar |
| `coverRadius` | 10 | MOV-3: raio da busca (4 anéis × 12 direções) |
| `coverCheckTicks` | 20 | MOV-3: a cada 1 s confere se a cobertura ainda corta a visão e procura outra |
| `coverMinThreatDistance` | 9 | MOV-3: kaiju a menos de 9 da cobertura → larga e volta a recuar (com 6 os soldados ficavam presos à cobertura e morriam antes) |
| `coverHideTicks` | 20 | MOV-3: escondido por 1 s depois de o kaiju mirar um ataque nele |
| `coverDangerRadius` | 4 | MOV-3: cobertura a menos de 4 de um kaiju preparando golpe pesado é largada |
| `avoidDanger` | true | MOV-4: soldados saem andando da área do golpe pesado em preparo (a do aviso no chão) e os especiais esquivam para fora dela |
| `dangerMargin` | 1,5 | MOV-4: folga em volta da área |
| `dangerEscapeSpeed` | 1,5 | MOV-4: velocidade (multiplicador) saindo da área |
| `allyLineSidestep` | 3 | MOV-4: atirador com aliado na frente do tiro segura o tiro e dá 3 blocos para o lado |
| `projectileDodgeRange` | 12 | MOV-4: especiais olham os projéteis de kaiju a até 12 blocos e esquivam dos que vão acertar (pela `dodge_chance` do perfil) |
| `[combat] soldierNoFallDamage` | true | Armadura cinética (Miguel, 2026-10-16): soldados comuns e especiais não tomam dano de queda |
| `[combat] kineticSuitNoFallDamage` | true | Armadura cinética: jogador com traje da Força de Defesa no peito não toma dano de queda (sem traje, a queda fere como no vanilla) |

**Destruição pelos ataques** (IA-2, Miguel 2026-10-15) [SUPOSIÇÃO nos números]:

| Chave / campo | Valor | Efeito |
|---|---|---|
| `[destruction] abilityAutoDestruction` | true | ataque de kaiju sem `destruction` no JSON quebra no ponto do golpe |
| `abilityRadius` / `abilityHeavyRadius` | 1,5 / 3,0 | raio da quebra (normal / pesado), × multiplicador de dano do ataque (pelo menos 1) |
| `abilityHeavyPowerBonus` | 1 | ataque pesado quebra uma categoria a mais (força = a de andar da categoria + 1, até 4) |
| `abilityMaxRadius` | 6 | maior raio automático |
| `heavy` no `special_soldier` | força 3, raio 2–3 | Kikoru (golpe pesado, onda de choque, golpe no chão, quebra-guarda), Kikoru No. 4 (mergulho, ataque vertical), Mina (3 tiros de canhão), Narumi e No. 1 (investida, varrida; No. 1 também estocada no ponto fraco), Kaiju No. 8 (soco pesado, investida, golpe no chão) |
| `[combat] tailGuardBlocksHeavy` | true | a guarda da cauda do Hoshina No. 10 continua segurando golpe pesado (Miguel, 2026-10-16) |

**Tática por espécie de kaiju** (`kaiju_ai/<id>.json`, IA-4, Miguel 2026-10-16) [SUPOSIÇÃO nos números; não muda vida,
dano nem armadura]:

| Espécie | circular (chance / distância / ticks) | cercar | recuo (vida → blocos) | janela após pesado | giro (°/tick) | outros |
|---|---|---|---|---|---|---|
| Trichonephila | 0,4 / 3 / 30 | 6 | 25% → 10 | — | 30 | teia +2 com o alvo a mais de 6 |
| Primigenius | 0,3 / 3 / 30 | 6 | 20% → 10 | — | 30 | |
| Primigenius ressurgido | 0,25 / 3 / 30 | 6 | 15% → 10 | — | 30 | |
| Camponotus / revivida | 0,35 / 3 / 30 | 6 | 20% / 15% → 10 | — | 30 | ácido +1 além de 6 |
| Phaneroplasmodium | 0,2 / 3 / 30 | 6 | 20% → 10 | — | 30 | |
| Primigenius Honju / Philinosoma / Diclonius | — | — | — | 20 ticks | 12 | tiro +1 além de 8 |
| Primigenius revivido | — | — | — | 14 | 14 | tiro +1 além de 8 |
| Myxogasterocarp | — | — | — | 24 | 10 | |
| Tecedeira Abissal | — | — | — | 16 | 14 | teia +2 além de 6 |
| No. 10 pequeno / gigante | — | — | — | 8 / 10 | 24 / 10 | perseguição ×1,15 / ×1,1 |
| No. 9 / forma preta | 0,3 / 4 / 30 | — | — | — | 30 | Finger Gun +1 além de 8 |
| Fusão No. 9 + No. 10 / gigante | — | — | — | 10 / 20 | 20 / 8 | perseguição ×1,1 (pequena) |
| No. 9 formiga | — | — | — | 10 | 16 | |
| Preondactyl | — | — | — | — | — | raio +1 além de 10; só mergulha com golpe de perto pronto |

**Esquadrões** (`[squad]`, IA-5, Miguel 2026-10-16) [SUPOSIÇÃO nos números]:

| Chave | Padrão | Efeito |
|---|---|---|
| `followDistance` | 4 | seguindo, o membro volta para perto do líder quando passa disso (para a ~1,6 bloco dele, cada um num lado) |
| `leashRange` | 20 | seguindo ou no posto, só luta com kaiju a até 20 blocos do líder/posto ("atacar" caça no alcance inteiro; o kaiju designado da invasão conta sempre) |
| `guardRadius` | 6 | no posto ("ficar" e invasão), sem alvo, volta quando passa disso |
| `recruitRange` | 8 | distância máxima para recrutar |
| `specialRankOrder` | 4 | soldado especial só entra no esquadrão de Vice-Capitão (ordem 4) para cima |
| `invasionSquadSize` | 3 | defensores de invasão em esquadrões de 3 (o último pode ser menor) |
| `invasionPostDistance` | 12 | postos dos esquadrões em volta do centro da invasão |
| `assignedBonus` | 16 | o kaiju designado ao esquadrão conta 16 blocos mais perto na escolha de alvo |
| `separationRadius` | 4 | com aliado a até 4 blocos, cada soldado chega ao alvo por um ângulo próprio (gira 30° por reavaliação para longe do aliado) |

Tamanho do esquadrão do jogador = `npc_squad_size` da patente (`rank/*.json`): Candidato e Oficial 0, Oficial Sênior 2, Líder de Pelotão 4, Vice-Capitão 6, Capitão 8; no criativo, o maior.

**Perfis táticos e guarda contra golpe pesado** (IA-3, Miguel 2026-10-16) [SUPOSIÇÃO nos números; fichas em
`docs/FICHAS_IA_PERSONAGENS.md`]:

| Personagem | aggression | recuo (vida / distância) | preferência de golpe | esquiva | outros | guarda pesado |
|---|---|---|---|---|---|---|
| Hoshina / No. 10 | 0,6 / 0,65, passo lateral | 25% / 20% → 8 | rápido vs Yoju +2; brecha +3 (MOV-5B) | 1,0 | combo que erra tudo: +40 ticks na recarga (MOV-5B) | ½ dano, 120°, 2 s |
| Kikoru / No. 4 | 0,85 / 0,9, passo lateral | 20% → 8 / 10 | pesado vs grande +1, rápido +1; brecha +2; alternar +1; regenerando +2 (MOV-5C) | 0,9 | combo que erra: +40 ticks | ½ dano |
| Mina | 1,0 | 30% → 24 | pesado vs grande +2 | 0,6 | alvo grande 12, sem contra-ataque | — |
| Reno / No. 6 | 1,0 | 30% / 25% → 18 | | 0,8 / 0,85 | protege aliado 10, sem contra-ataque | — |
| Narumi / No. 1 | 0,75 / 0,8, passo lateral | 15% → 10 | pesado vs grande +1 | 1,0 | alvo grande 6 / 8 | ½ dano |
| Kafka | 0,4 | 60% → 14 | | 0,7 | protege aliado 6, sem contra-ataque | — |
| Kaiju No. 8 | 0,95 | — | pesado vs grande +2, rápido +1 | 0,5 | | ½ dano |

MOV-5B (`tactics.exploit_openings`/`miss_cooldown_ticks`, só Hoshina e Hoshina No. 10 por ora) [SUPOSIÇÃO]: com a
guarda do kaiju aberta (janela parada depois do golpe pesado do `kaiju_ai` ou atordoado), a técnica de maior dano
total ganha +3 de prioridade e o Hoshina não reposiciona. Combo que termina sem acertar nenhum golpe soma 40 ticks à
recarga dele. Para todos os especiais: no máximo um golpe de técnica por tick; combo não começa se o alvo, pelo
quanto se afastou no último tick, vai estar fora do alcance (+1 de folga + 2,2 x `dash_in`) quando o primeiro golpe
sair. Duelos depois (servidor dedicado, 5 cada): Hoshina × No. 10 pequeno 5/5 (27–51% da vida; antes 7–50%),
× No. 10 gigante 0/5 (gigante com 42–71%; antes 46–67%), × Primigenius Honju 5/5 em 10–14 s com 99–100% da vida.

MOV-5C (`rhythm` no `special_soldier`, Kikoru e Kikoru No. 4) [SUPOSIÇÃO]: depois de técnica pesada (lista `heavy`) a
rápida ganha `alternate_bonus` 1 e vice-versa; com o alvo regenerando (No. 9/No. 10 na faixa de cura) a técnica que
acerta mais cedo ganha `press_regeneration` 2 e ela não reposiciona. **Kikoru `kaiju_damage` 0,62 → 0,64** (Kikoru ×
No. 9: 0/10 → 3/10, meta 25%; 0,68 dava 7/10). × No. 10 pequeno 0/5, × Primigenius Honju 3/3.

MOV-5D (Mina): `ranged.reserve_limited` true (Anti-Giant só contra alvo grande). Para todo especial: tiro de técnica
conferido no disparo (linha de tiro e `max_range` + 1) e, nos atiradores, cancelado no preparo com o alvo mais perto
que o `min_range` (uso limitado devolvido, recarga = preparo). Duelos: × No. 10 pequeno 5/5, gigante 4/5, No. 9 4/5
(meta "perde" retirada: **Miguel 2026-10-18 aceitou que ela vença**). Cobertura adaptativa (2026-10-18):
`ranged.cover_health` 0,5 (Mina, Reno, Reno No. 6) [SUPOSIÇÃO], `[soldierAi] specialCover` true; duelos com o chão
refeito a cada duelo: Mina × No. 9 4/10 e × No. 10 pequeno 4/10 (cobertura desligada: 6/10 e 6/10; diferença dentro do ruído de 10 duelos, com o chão refeito a cada duelo); Reno No. 6 × No. 9 0/3 com e sem cobertura (era 6/6 no v1.2: queda anterior a esta mudança, fica para a etapa do Reno).

MOV-5E (Reno): `[kaijuAi] paralyzedSlowLevel` 6 (Lentidão 6+ = brecha para os aliados), Reno e Reno No. 6
`exploit_openings` 2 e `rhythm.press_regeneration` 2 [SUPOSIÇÃO]. **Reno No. 6 `kaiju_damage` 0,75 → 0,80**
[SUPOSIÇÃO]: × No. 9 1/6 → 5/8 (meta 70%; 0,85 dava 5/6). Reno × No. 9 0/3 (meta 12%), Hoshina + Reno × No. 10
gigante 1/3.

MOV-5F (Narumi No. 1): `rhythm.read_windup` 3 e `[soldierAi] readWindupMargin` 2 ticks [SUPOSIÇÃO]. Narumi No. 1 ×
No. 9 5/5, × No. 10 pequeno 5/5, × No. 13 5/5, × No. 10 gigante 0/3 (gigante com 34–42%). [DECIDIR] Narumi
`kaiju_damage` 1,3 maior que o do No. 1 (0,85).

MOV-5G (Isao): **Isao sem o Numbers 2** `special_soldier/isao_base.json` (520/20/0,29 do v1.2; Release 40–85%,
`kaiju_damage` 1,2, punhos `weapon/isao_fist.json` base 7, jab 0,6/0,6/0,7/0,9, cotovelada 1,4, direto pesado 2,2
[SUPOSIÇÃO]); Isao e Isao sem o Numbers 2 com `exploit_openings` 3 e `press_regeneration` 2. Duelos: sem o Numbers 2 ×
No. 10 pequeno 2/5 (meta 45%), × No. 9 5/5 por fuga (meta 30%), × Honju 3/3; com o Numbers 2 × No. 9 5/5, × No. 10
pequeno 5/5, × No. 10 gigante 0/5 (meta 60%; [DECIDIR]: 4/5 na 0.8-A, dano até 1,5 não resolve).

**Quebra ao andar** (`[destruction] walk*`, 0.6): força 3 para Yoju, Honju e numerados (quebra frágil, normal e
resistente: vidro, madeira, terra, pedra, tijolo, concreto), 4 para Daikaiju (também deepslate e ferro); um pedido a
cada 5 ticks no máximo. Força 0 desliga [SUPOSIÇÃO].

### Chefes (`boss/<id>.json`)

| Chefe | Kaiju | Vida (× JSON) | Vida final | Fases (troca em) | Invoca | Mérito |
|---|---|---|---|---|---|---|
| Honju do Exame | Primigenius Honju | ×0,444 | 160 | — | 1 Primigenius (máx. 2) | 100 |
| Primigenius Honju | Primigenius Honju | ×1,333 | 480 | 50% | 2 Primigenius (máx. 4) | 300 |
| Honju revivido | Primigenius revivido | ×1,835 | 844 | 60%, 30% | 2 ressurgidos (máx. 4) | 500 |

Cada jogador a mais na arena soma +50% de vida (`[boss] playerScaling`). Balanceamento v1.0: a vida dos
chefes ficou igual (o Honju subiu para 360 e o revivido para 460, então os multiplicadores baixaram).

### Kaiju No. 9 (`numbered/kaiju_no9.json`)

| Número | Valor |
|---|---|
| Reviver: raio / gesto / espera | 24 blocos / 60 ticks (3 s) / 300 ticks (15 s) |
| Máx. revividos vivos (modo normal) | 3 |
| Comandar kaiju (raio) | 32 blocos |
| Foge com | **15%** da vida (era 30%; 0.6-D), dando 250 de mérito a quem está a até 48 blocos |
| Regeneração (0.6-D, Miguel) | abaixo de 50% da vida: 2,5% da vida máxima por segundo; abaixo de 20%: 5%/s; para por 10 ticks depois de cada golpe recebido (`regeneration`) |
| Ressurreição em massa: gesto / intervalo | 100 ticks (5 s) / 4 ticks entre um kaiju e o próximo |
| Revive | Primigenius → ressurgido · Honju → chefe **Honju revivido** · aranha → aranha [SUPOSIÇÃO: falta modelo de aranha ressurgida] |
| Forma preta (0.7-E) | abaixo de **40%** da vida vira a forma preta com a vida cheia (`transform`) [SUPOSIÇÃO] |
| Absorver o No. 10 (0.7-E) | com a própria vida abaixo de **75%**, puxa um No. 10 pequeno (vivo com qualquer vida, ou a carcaça) a até 24 blocos por 60 ticks e vira a **fusão** (`absorb`) [SUPOSIÇÃO] |
| Absorver a formiga (0.7-E) | abaixo de **60%**, puxa uma Camponotus (ou a revivida, viva ou carcaça) a até 20 blocos por 50 ticks e vira a **forma formiga** [SUPOSIÇÃO] |

### Formas do No. 9 (0.7-E, `kaiju/` + `numbered/kaiju_no9_<forma>.json`; números do Balanceamento v1.2 §5–7)

Matriz de vitória medida (10 duelos por confronto) e o que mudou em cada personagem: `docs/BALANCEAMENTO_v1_2_APLICADO.md`;
fichas de habilidades de todos: `docs/FICHAS_HABILIDADES.md`. Análise do No. 9 (todas as formas): golpe repetido do
mesmo tipo tira 2–3% menos a cada vez, até −15/−25% (`adaptation`).

| Forma | Vida | Dano | Armadura | Velocidade | Regeneração (abaixo de 50% / 20%) | Pele endurecida (chance, redução, duração, espera) | Foge com | Ataques novos (multiplicador, recarga em ticks) |
|---|---|---|---|---|---|---|---|---|
| Preta (`kaiju_no9_black`, 1,9 m) | 2.200 | 42 | 17 | 0,36 | 3% / 5,5% por s | 15%, −50%, 40, 200 | 15% (250 de mérito) | Multi-Finger Gun 3 × 0,75 em leque (70), voo curto 1,0 de 7 a 18 blocos (160), braço-lâmina 1,4 (50), tentáculo 0,8 + lentidão II 2,5 s (90, 4–20 blocos), espinhos do corpo 1,2 em 3,5 blocos (120, abaixo de 70%); garra, investida e Finger Gun; ainda absorve o No. 10 e a formiga |
| Fundida ao No. 10 (`kaiju_no9_fusion`, 5 m) | 5.400 | 58 | 19 | 0,38 | 3,5% / 6% por s | 10%, −40%, 40, 240 | 10% (500) | rajada de cauda 6 × 0,7 (140), raio híbrido 1,8 que explode em 3 blocos (200, 6–32 blocos), Multi-Finger Cannon 5 × 0,9 em leque com explosão 2 (150); golpes do No. 10 e o Finger Cannon dele; comanda kaiju a 64 blocos; revive até 4 |
| Fundida à formiga (`kaiju_no9_camponotus`, 6 m) | 3.200 | 46 | 18 | **0,45** | 3,2% por s | 5%, −40%, 30, 240 | 15% (400) | salto 1,5 de 5 a 16 blocos (100), mordidas 5 × 0,6 (110), ácido 0,8 + lentidão II (120), Ant Rush 1,4 de 4 a 20 blocos (90); mordida, patas e Finger Gun; ainda absorve o No. 10 |

**Fusão gigante** (`kaiju_no9_fusion_giant`, 20 m, tabela mestra de alturas 2026-10-15): surge da fusão de 5 m abaixo
de 50% da vida, com a vida cheia. Vida 9.000, dano 72, armadura 20, velocidade 0,32, categoria `supergiant`; mesmos
ataques, regeneração, comando, revividos e fuga (10%) da fusão (números confirmados pelo Miguel, 2026-10-15).

**Alturas (tabela mestra, 2026-10-15):** forma preta 5 m (escala 2,632), formiga 11 m (2,813); escalas de todas as
entidades em `docs/AUDITORIA_ALTURAS.md`. A escala não muda vida, dano nem velocidade.

Fúria abaixo de 30% nas três (dano ×1,15–1,25, velocidade ×1,1–1,2, recargas ×0,6–0,7). Núcleo: peito na preta e na
fusão; no peito do torso do No. 9 em cima da formiga.

---

### Locomocao (`locomotion/<id>.json`, 0.5.0-C)

A animacao de andar toca na velocidade em que os pes acompanham o chao: `velocidade = blocos andados por tick x
duracao da volta da animacao / passada`, entre `min_animation_speed` (padrao 0,35) e `max_animation_speed` (2,5);
parado, 1,0. Um som de passo a cada meia passada (`step_distance`); `step_dust` = particulas do chao por passo.
Passadas [SUPOSICAO] ~1,2 x altura do quadril (mais curta que a real: passo pesado sem parecer congelado).

| Id | Passada (blocos) | Poeira | Observacao |
|---|---|---|---|
| `primigenius`, `primigenius_resurrected` | 3,2 | 0 | 6 m |
| `primigenius_honju`, `primigenius_revived` | 4,5 | 4 | 9 m |
| `trichonephila` | 2,5 | 0 | 8 patas |
| `trichonephila_honju` | 3,5 | 3 | 8 m |
| `philinosoma`, `diclonius` | 4,5 | 4 | 9 m (0.7-A) |
| `camponotus`, `camponotus_reborn` | 2,8 | 0 | 6 patas, 6 m de comprimento (0.7-A) |
| `phaneroplasmodium` | 2,2 | 0 | 8 patas, 5 m (0.7-B) |
| `myxogasterocarp` | 3,6 | 4 | raízes, 9 m (0.7-B) |
| `kaiju_larva` | 0,4 | 0 | 0,5 m; no ar usa a animação de voo (0.7-B) |
| `kaiju_no9` | 1,4 | 0 | 2 m |
| `kaiju_no10_small` | 2,6 | 0 | 4 m |
| `kaiju_no10_giant` | 12,0 | 16 | 24 m; minimo 0,3 (passos lentos e pesados) |
| `preondactyl` | 2,0 | 0 | no chao; no ar usa a animacao de voo |
| `soldier` | 1,6 | 0 | id do tipo de entidade |
| `hoshina`, `hoshina_no10` | 1,7 | 0 | |

## 2. Jogador

### Armas (`weapon/<id>.json`)

Dano = base × (1 + Release% / 25) (`[fortitudeCurve] releaseDamageDivisor`).

| Arma | Base | 0% | 10% | 30% | 60% | 100% | Golpe leve | Pesado | Alcance |
|---|---|---|---|---|---|---|---|---|---|
| Faca | 6 | 6 | 8,4 | 13,2 | 20,4 | 30 | 12 ticks | ×2,0 / 24 ticks | 3,0 |
| Espada | 8 | 8 | 11,2 | 17,6 | 27,2 | 40 | 14 | ×2,0 / 28 | 3,5 |
| Machado | 11 | 11 | 15,4 | 24,2 | 37,4 | 55 | 18 | ×2,4 / 32 | 3,2 |
| Espada do Hoshina (0.6-D) | 7 | 7 | 9,8 | 15,4 | 23,8 | 35 | 10 | ×1,9 / 22 | 3,0 |

**Ataque especial** (`special` no JSON da arma, tecla R, 0.5): Golpe Sísmico do machado = ×2,6 em área (raio 4,
2 blocos à frente), 35 de stamina, 6 de calor, recarga de 200 ticks, empurrão 1,2, atordoa Yoju por 30 ticks
[SUPOSIÇÃO]. **Potência com vida baixa** (`[power] desperationHealth` 0,5 / `desperationMaxPoints` 15): até
+15 pontos de Release com a vida no fim [SUPOSIÇÃO].
| Pistola | 3,5 | 3,5 | 4,9 | 7,7 | 11,9 | 17,5 | 6 | — | 32 |
| Rifle | 5 | 5 | 7 | 11 | 17 | 25 | 8 | — | 48 |

✅ A patente de armas e trajes fica só nos `unlocks` das patentes (o antigo `required_rank` dos JSONs de arma e
traje foi removido na 0.3 porque divergia deles).

### Perfis de arma (`weapon_profile/<id>.json`, 0.5.0-D)

| Perfil (armas) | Mãos | Saque | Pente | Recarga (etapas) | Coice da câmera | 1ª pessoa | NPC |
|---|---|---|---|---|---|---|---|
| `knife` (faca) | uma | 4 ticks | — | — | — | braços do modelo | lâmina |
| `sword` (espada) | uma | 8 ticks | — | — | — | braços do modelo | lâmina |
| `dual_reverse` (espada do Hoshina) | duas armas, pegada invertida | 10 ticks | — | — | — | braços do modelo | lâmina |
| `two_handed_axe` (machado da Kikoru) | duas mãos | 14 ticks | — | — | — | braços do modelo | lâmina |
| `rifle` | duas mãos | 12 ticks | **30** | 10 + 14 + 8 = 32 ticks (soltar, colocar, engatilhar) | 1,6° para cima, 0,6° de lado, volta 60% em 4 ticks | vanilla | rifle |
| `pistol` | uma | 6 ticks | **12** | 6 + 10 + 6 = 22 ticks | 3,0° / 1,0°, 5 ticks; corpo ×1,6 | vanilla | pistola |

**Munição como item (0.5.0-D2, Miguel: nada infinito).** A recarga troca o pente da arma por um pente carregado da
mochila (`kn8:rifle_magazine` 30, `kn8:pistol_magazine` 12; o vazio volta para a mochila). Pente vazio se carrega
com o botão direito, gastando `kn8:rifle_ammo`/`kn8:pistol_ammo`. Sem pente carregado: "Sem pente carregado". O
soldado leva 3 pentes de reserva (`npc.spare_magazines`) e, sem munição, troca para a arma de apoio. Criativo não
gasta. A tecla R recarrega na arma de fogo (que não tem especial). Números do perfil e receitas [SUPOSIÇÃO].

| Receita (bancada, patente Oficial) | Materiais | Sai |
|---|---|---|
| Pente de rifle | 3 ferro + 1 redstone | 1 (vazio) |
| Pente de pistola | 2 ferro + 1 redstone | 1 (vazio) |
| Munição de rifle | 2 ferro + 1 pólvora | 24 |
| Munição de pistola | 2 ferro + 1 pólvora | 16 |

**Espada do Hoshina é de par (0.5.0-D3, Miguel):** o especial (Kūuchi, tecla R) só sai com uma espada do Hoshina
em cada mão; com uma só, "Técnica de par: segure uma espada em cada mão". O Hoshina NPC já luta com as duas.

### Combate (`[combat]`)

| Número | Valor | | Número | Valor |
|---|---|---|---|---|
| Stamina do golpe leve / pesado | 5 / 12 | | Parry: janela (≥ 60% Release) | 3 ticks (6) |
| Esquiva: stamina / invulnerável | 20 / 6 ticks | | Parry: atordoa / devolve stamina | 30 ticks / 10 |
| Dash: stamina / velocidade | 25 / 1,6 | | Crítico depois do parry | ×1,5 por 40 ticks |
| Ataque carregado: stamina / máx. | 20 / ×2,0 em 30 ticks | | Bloqueio segura | 70% do dano |
| Núcleo exposto (golpe pesado) | ×1,5 por 100 ticks | | Combo: janela | 10 ticks |
| Golpes mais rápidos com o Release (0.5.0-D7, Miguel) [SUPOSIÇÃO] | ×(1 + 0,5 × R/100): 40% → ×1,2, 100% → ×1,5 | | Vale para | jogador (leve, pesado, especial) e técnicas do Hoshina |

`[combat] attackSpeedAtFullRelease` (0,5): duração e tick de impacto do golpe são divididos pelo fator; a animação toca
na mesma velocidade (`AnimTriggerS2C.speed` no jogador, `ACTION_SPEED` sincronizado no Hoshina). Duelos do Hoshina
remedidos depois disso (2 de cada): perde para o No. 10 gigante (93–104 s, gigante com 23–24% da vida), vence o No. 9
(30–32 s, com 58–73% da vida) e o No. 10 pequeno (26–28 s, com 41–48%).

### Release, stamina e calor

0.5.0 (Biblioteca v21, decisao do Miguel de 2026-10-08):
- O Release so funciona com o traje vestido.
- Sobe segurando **G** e desce com **Shift + G**.
- O limite pessoal e sorteado uma vez e sobe treinando.
- Acima do limite a % nao cai: o traje esquenta e o corpo desgasta.

| Numero | Valor | Onde |
|---|---|---|
| Subir / descer na tecla | **2% por segundo** (Miguel) / 40 por segundo | `[power] raisePercentPerSecond`, `lowerPerSecond` |
| Acima do limite | no maximo **+20** (limite 40 -> ate 60%) | `[power] maxOverLimit` |
| Desgaste acima do limite | **so com o calor no maximo** (FIX-1, Miguel 2026-10-29): 0,06 de vida/s por ponto acima (+20 = 1,2/s) mais 2/s do calor maximo; antes disso o traje so esquenta (limite 40 e 60%: ~29 s sem dano, depois ~3,2/s) | `[power] overLimitDamagePerPoint`, `[heat] maxHeatDrainPerSecond` [SUPOSICAO] (`overloadDrainPerSecond`/`criticalDrainPerSecond` ficam sem uso) |
| Empurrao ao tomar dano (jogador) | **0** (FIX-1, Miguel: tirar o empurrao); 1 = vanilla | `[combat] playerKnockback` |
| Volume dos sons do mod (cliente) | **35%** da altura (FIX-1, Miguel com o volume geral em 29%); o alcance continua igual | `[fx] modSoundVolume` (kn8-client.toml) [SUPOSICAO] |
| Fadiga ao voltar para o limite | 1 tick por tick no maximo acima (metade na metade); entre 3 s e 30 s; Lentidao IV, Fraqueza II, Cansaco II, sem Release | `[power] fatigueTicksPerStrain`, `fatigueMinTicks`, `fatigueMaxTicks` [SUPOSICAO] |
| Golpe em kaiju (balanceamento v1.0) | comum no maximo **12%** da vida maxima por golpe, pesado/carregado **15%** (ja com parte/nucleo); so o golpe especial da arma passa disso | `[combat] maxLightHitFraction`, `maxHeavyHitFraction` |
| Limite pessoal inicial (talento) | comum 5-10%; **raro** (10% de chance, Miguel) 15-30% | `[talent]` |
| Limite maximo | 100% para todos, por treino | `[career] releaseMax` |
| XP de treino por ponto do limite | 50 + 10 x pontos ja treinados (1% custa 50; 99->100% custa 1.040) | `[training]` |
| Por ponto de Release ativo | +0,4% velocidade, -0,4% de dano recebido (max. 40%), +0,5 de stamina | `[power]`, `[stamina]` |
| Stamina | 100 + 0,5 por ponto; recupera 15/s depois de 1 s | `[stamina]` |
| Corrida | gasta 5/s; volta a correr com 20 | `[stamina]` [SUPOSICAO] |
| Forca total do traje (Release = limite) | aquece 0,67/s so com a % no proprio limite (40, 60 ou 100, o que o jogador tiver), so ate morno (40): ~1 minuto e cansa (stamina regenera 25% mais devagar), sem dano. Abaixo do limite nao aquece (esfria) | `[heat] useHeatPerSecond` |
| Acima do limite | +2 de calor/s a cada 10 pontos de excesso | `[heat] excessHeatPer10PerSecond` |
| Calor (so com a % acima do limite) | sobrecarga 70 (+10% dano, -0,5 vida/s), critico 90 (-1/s, Lentidao I), maximo 100 (-2/s, Lentidao II, alarme). A % **nao cai** | `[heat]` |
| Esfriar | 10/s fora de combate, 3/s em combate | `[heat]` |

### Atributos do corpo (0.5.0, `[body]`)

Forca, velocidade, resistencia e agilidade vao de 0 a 100 e sao treinados pelo uso. Regra do Miguel: o corpo tem a
forca dele e o **traje impulsiona** (o Release multiplica o bonus do corpo: forca e resistencia multiplicam com o
dano/reducao do Release, velocidade multiplica a do Release, agilidade x(1 + Release/100), desconto maximo 60%).
Sem traje vale so o corpo. Numeros [SUPOSICAO].

| Atributo | Treina com | Bonus no nivel 100 |
|---|---|---|
| Forca | 0,5 XP por ponto de dano corpo a corpo em kaiju ou no boneco (o boneco respeita o limite por minuto) | +30% de dano corpo a corpo (nao vale na arma de fogo) |
| Velocidade | 0,2 XP por bloco corrido no chao | +15% de velocidade |
| Resistencia | 1 XP por ponto de dano recebido de alguem (nao do traje) | -20% de dano recebido |
| Agilidade | 3 XP por esquiva, dash ou parry | -30% de stamina na esquiva e no dash |

Cada nivel custa 20 + 4 x nivel de XP (do 0 ao 100: ~21.800).

### Parkour do jogador (0.5.0-E, `[parkour]`)

O cliente pede, o servidor confere a geometria (`probeReach` 0,9) e a stamina. Todo custo e x fator de agilidade do
corpo (treinado gasta ate 60% menos) e cada movimento da 3 XP de agilidade (`[body] xpAgilityPerAction`). Numeros
[SUPOSICAO] ate o teste do Miguel.

| Movimento | Como | Custo de stamina | Numeros |
|---|---|---|---|
| Vault | correndo, pular contra obstaculo de 0,6 a 1,5 | 6 | sobe a altura + 0,25, avanco 0,45/tick |
| Escalada curta | pular contra obstaculo de 1,5 a 2,5 (ou mais baixo sem correr) | 12 | sobe a altura + 0,25, avanco 0,2 |
| Salto longo | correndo, tecla C no chao | 15 | 0,95 na horizontal + 0,5 para cima (medido: ~7,9 blocos no ar) |
| Wall-run | no ar, correndo, C com parede ao lado | 14/s (1/4 para comecar) | 0,36/tick ao longo, sobe 0,16 e cai ate -0,16; ate 30 ticks |
| Wall-slide | no ar, C de frente para a parede | 4/s | queda maxima 0,12/tick, sem dano; ate 60 ticks |
| Wall-jump | Espaco na parede (ate 5 ticks depois de sair) | 10 | 0,5 para longe, 0,25 ao longo, 0,55 para cima; ate 4 (wall-runs + wall-jumps) sem pousar; a mesma parede so depois de 4 ticks no chao (`landingTicks`) |
| Segurar a borda | no ar, C com borda de 0,9 a 2,2 acima dos pes | 3/s | pes 1,7 abaixo da borda; ate 60 ticks; subir 8 |
| Slide | correndo, Shift | 10 | 0,6/tick, atrito 0,93 por tick, ate 20 ticks, deitado (vao de 1 bloco) |
| Rolamento | Shift ao cair de 4+ blocos | 8 | ate 7 blocos sem dano; acima, -60% |

Forca pelo nivel (Miguel, 2026-10-15): velocidades, saltos e alturas maximas x (1 + 0,3 x agilidade/100 + 0,3 x Release/100), ate 1,6x no topo (`scalePerAgility`, `scalePerRelease`); o custo ja cai com a agilidade e a stamina maxima sobe com o Release. Intervalo entre impulsos 6 ticks; estados continuos aguentam 6 ticks sem parede (atraso da rede); corrida conta ate 8
ticks depois de parar.

### Arena de treino (construções C2, `[trainingArena]`)

| Chave | Valor | O que faz |
|---|---|---|
| `arenaKaiju` | aranha, Primigenius, formiga, cogumelo, ressurgido, Honju, Tecedeira, Philinosoma, Diclonius, Myxogasterocarp, revivido, No. 9, No. 10 pequeno | Kaiju que o painel solta, na ordem da troca (até 16); os gigantes (No. 10 gigante, fusões, formiga do No. 9) ficam fora porque não cabem na arena |
| `arenaDistance` | 22 | Do painel ao centro da arena (bate com a arena gerada) |
| `arenaHalfWidth` / `arenaHalfLength` / `arenaHeight` | 24 / 20 / 16 | Área de luta: 49 x 41 x 16 |
| `arenaProtectMargin` | 3 | Margem protegida da destruição em volta (paredes e painel) |
| `arenaLeashCheckTicks` | 20 | Kaiju fora da arena volta ao centro a cada 1 s |
| `arenaIdleRange` / `arenaIdleDespawnTicks` | 24 / 1.200 | Sem ninguém a 24 blocos da arena por 1 minuto, o treino acaba |

O kaiju de treino tem a vida, o dano e as habilidades normais; só não deixa carcaça, mérito, missão nem chefe.

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
| Vice-Capitão | 8.000 | — | 28 | 6 | — |
| Capitão | 20.000 | — | 30 | 8 | — |

✅ 0.3: Vice-Capitão 4.000 → **8.000** e Capitão 10.000 → **20.000** (uma invasão nível 5 levava de Oficial Sênior a
Vice-Capitão de uma vez). A recompensa das invasões agora depende da contribuição (ver seção 5).

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
| Primigenius e ressurgido | 5 | 2 s | 4–6 tecidos, 1–2 fibras + fragmento ou núcleo inteiro |
| Honju e Honju revivido (0.3) | 8 | 2,5 s | 10–14 tecidos, 4–6 fibras + 2–3 fragmentos, ou núcleo inteiro + 1 fragmento |

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
| Enxame de Trichonephila | 1 | 3 + 5 aranhas | 2 rifle + 1 pistola (normal) | 10 min | 80 | 150 | peso 3 |
| Ataque dos Primigenius | 2 | 2 + 4 + 3 | 3 sorteados (rifle/pistola) + 1 pistola alto | 15 min | 150 | 300 | peso 2 |
| Ofensiva do Honju | 3 | 3 + (2 + Honju) | 4 rifle + 2 pistola (alto) | 20 min | 400 | 600 | peso 1 |
| Ressurreição do No. 9 | 3 | 2 + (No. 9 + 1) | 3 rifle + 1 pistola (alto) | 20 min | 350 | 500 | só missão |
| Horda Kaiju | 4 | 10 + (9 + Honju) | 4 sorteados (normal) + Hoshina + Kikoru | 30 min | 700 | 900 | só missão |
| Ressurreição em Massa | 5 | horda + No. 9 revivendo tudo | 3 rifle + 1 espada | 40 min | 1.500 | 2.000 | só missão |
| Kaiju sobre Tachikawa (C3) | 3 | (3 Primigenius + 4 aranhas) + (2 Preondactyl + 2 ressurgidos) | 4 sorteados (normal) + 2 rifle (alto) | 20 min | 400 | 600 | cidade de Tachikawa |
| Horda de Formigas em Shinagawa (C3) | 5 | 6 + (4 + 3 revividas) + (3 revividas + No. 9 50% + No. 9 formiga 20%) | 4 sorteados (alto) + 2 rifle elite + Narumi + Hoshina | 30 min | 1.500 | 2.000 | Shinagawa |
| Raide do No. 10 a Tachikawa (C3) | 6 | (4 Preondactyl + 3 Primigenius) + (3 + 2 ressurgidos + 3 aranhas) + (No. 10 + 2 Preondactyl) | 4 sorteados (alto) + 2 rifle elite + Hoshina | 40 min | 2.500 | 3.000 | base de Tachikawa |
| Ataque a Ariake (C3) [SUPOSIÇÃO] | 6 | (4 formigas + 3 ressurgidos) + (revivido + 3 formigas revividas) + (No. 9 + forma preta 30% + 2) | 4 sorteados (alto) + 2 rifle elite + Narumi + Hoshina | 40 min | 2.500 | 3.000 | Ariake |

Chance de invasão natural por noite: 20% (`[invasion] naturalChance`) [SUPOSIÇÃO].

**Locais da obra (C3, `site/<id>.json`):** quando a invasão natural cai num jogador dentro de um local, é uma das
invasões do local (pesos: Ariake `ariake_assault` 3 / `no9_resurrection` 1; base de Tachikawa `tachikawa_raid` 3 /
`primigenius_raid` 1; Shinagawa `shinagawa_ants` 3 / `spider_swarm` 1; cidade de Tachikawa `tachikawa_city_raid` 3 /
`primigenius_raid` 1), na entrada oeste da peça inicial (`[sites] invasionChance` 1,0). As quatro novas têm
`natural_weight` 0 (só no local, por missão ou comando). Guarnição (uma vez por mundo, na primeira visita;
`[sites] garrison`): Ariake = Narumi + 2 rifle elite; base de Tachikawa = Mina, Hoshina, Kikoru e Reno.
`[sites] checkIntervalTicks` 20 (de quanto em quanto o servidor confere em que local o jogador está).

**Onde os locais nascem (C3):** conjunto `kn8:defense_sites` (`spacing` 48, `separation` 16 chunks: ~1 candidato a
cada 770 blocos, antes das recusas; bases com peso 2) [SUPOSIÇÃO]; terreno (`check_step` 12): raio 104/112 (bases) e 96
(cidades), desnível máximo 14 (bases) e 16 (cidades) só no miolo de raio 64 (`slope_radius`), até 5% das amostras com
água (`max_water`: riacho ou lagoa pequena). Medido na seed 8008: locais a 4–16 mil blocos do spawn. Evita (raio em
chunks, só onde o bioma deixa a construção nascer) vilas 14, postos de saqueadores 8, mansões 10, templos do
deserto/selva, iglus e cabanas 6. As construções pequenas do mod não nascem a 8 chunks dos locais. A base grande genérica saiu do mundo (só `/place`) [DECIDIR].

**Recompensa por contribuição (0.3, pedido do Miguel):** o mérito e o XP da tabela acima são a recompensa de quem
lutou "na média". Cada jogador recebe isso × (sua parte do dano dos jogadores × nº de jogadores que lutaram), entre
**25%** e **150%** (`[invasion] rewardMinFactor` / `rewardMaxFactor`). Quem não causou dano em nenhum kaiju da
invasão não ganha a recompensa final. Os abates e o dano já dão mérito e XP na hora (seção 2), então quem mais luta
sobe mais rápido. A fuga do No. 9 só dá mérito a quem causou dano nele.

**Kūuchi** (especial da espada do Hoshina, 0.6-D, tipo `slash_wave`): corte que voa 12 blocos a 1,6 bloco/tick,
×1,6, atravessa os alvos, 20 de stamina, 3 de calor, recarga de 40 ticks [SUPOSIÇÃO].

**Estilo do machado da Kikoru (COMBATE-3)** [SUPOSIÇÃO todos]: jogador (`axe.json`, iguais no `numbers4_axe.json`,
dano x base 11): Falling Thunder `dive_slam` x2,8, raio 3, so no ar a 3+ blocos do chao, +5% por bloco caido (ate +75%, `[combat] diveSlam*`), ate 40 ticks caindo, 30 de stamina,
recarga 140; Water Skimmer corte horizontal x1,8, 8 blocos, recarga 70; Half-Moon area em volta x1,7, raio 3,4,
recarga 80; Rising Thunder avanco de 2,5 x2,0 com empurrao 1,6, recarga 90; Twin Swallows avanco de 4 com 2 cortes
x1,15, recarga 80; Daruma Dropper 4 cortes x0,9, recarga 110; Ten Hit (so Numbers 4) 10 cortes x0,6, recarga 220.
NPC (`kikoru.json`/`kikoru_no4.json`): Falling Thunder [2,4] (Kikoru: salto real com preparo de 30 ticks), Water Skimmer [1,4], Half-Moon [1,7], Rising Thunder
[1,9] (expoe o nucleo 40 ticks), Twin Swallows [1,1; 1,1], Daruma Dropper [0,8 x3; 1,0], Ten Hit [0,55 x9; 1,2];
prioridades 3-5 (genericas 1). Duelos da matriz nao remedidos (COMBATE-8).

**Squadron Style do Isao (COMBATE-4)** [SUPOSIÇÃO todos]: NPC (`isao.json`): Barrage Blast cotovelada [1,6] com avanço,
Twin Blast 2 ondas [1,1] até 10 blocos, Rising Blast uppercut [2,0] (empurrão 1,8), Spiral Blast [1,5] amarelo,
Thunder Emperor [0,9 x8; 2,6] a cada 2 ticks (recarga 180, expõe o núcleo 120 ticks). No. 9 com o Isao: Thunder
Emperor `multi_hit` 9 x0,85, Barrage Blast `melee` x1,9. Jogador (manoplas, base 8): Barrage Blast `lunge` x2,0,
Twin Blast x1,4, Rising Blast x2,2 (atordoa Yoju 24), Spiral Blast x1,8, Thunder Emperor `flurry` 9 x1,0 (recarga
300, 45 de stamina, 8 de calor). **Thunder Emperor só com a vida até 40%** (Miguel 2026-10-29, jogador e NPC: `low_health`
no `numbers2_gauntlets.json` e no `isao.json`, `behavior.health_below` 0,4 na `no9_isao_thunder_emperor`)
[SUPOSIÇÃO 40%]. Duelos não remedidos (COMBATE-8).

**Técnicas do Narumi (COMBATE-6)** [SUPOSIÇÃO todos; prévia, refazer na COMBATE-8]: NPC Narumi Eruption Slash
[0,7 x3; 1,1] a cada 3 ticks (recarga 80), Slash Salvo [0,9 x2; 1,3] com avanço 2,0 (alcance 2-8, recarga 100),
Seven-Branched Sword [0,6 x6; 1,6] a cada 2 ticks (recarga 220, pesado); Narumi No. 1: x1,25 e um golpe a mais
(Seven-Branched com 7 golpes, [0,75 x6; 2,0]) e acerto até o alcance da técnica. Jogador, baioneta do Narumi (base 9):
Eruption Slash `flurry` 4 x0,8 (28,8 por uso, recarga 5,5 s: 5,2/s), Slash Salvo `lunge` 6 blocos 3 x0,9 (24,3; 5 s:
4,9/s), Seven-Branched `flurry` 7 x0,7 (44,1; 11 s: 4,0/s, atordoa Yoju 20); baioneta do Numbers 1 (base 8, x domínio):
5 x0,9 (36), `lunge` 7 blocos 4 x1,0 (32), 7 x0,85 (47,6).
Inferidas pelos nomes (Miguel, 2026-10-30: "pode decidir atraves das inspirações") [SUPOSIÇÃO e INFERÊNCIA]: NPC
**Thunder Fire** 3 tiros explosivos (raio 1,5) x1,4 (alcance 3-24, recarga 90), **Flame Shower** 6 tiros em leque de
30° x0,7 com explosão 2,0 (alcance 2-16, recarga 160, pesado), **Sky Turner** combo [0,9; 0,9; 1,6] com avanço 2,2 e
lançamento (alcance 2-7, recarga 140); No. 1 x1,25, um tiro/golpe a mais (Flame Shower dois). Jogador (baioneta do
Narumi): Thunder Fire `slash_wave` 3 x1,3 (recarga 5 s), Flame Shower `slash_wave` 6 x0,7 (9 s), Sky Turner `lunge`
5 blocos 3 x0,85 (recarga 6 s); Numbers 1 x1,25 e um a mais (Flame Shower dois).

**Kōsa-uchi do jogador** (segunda técnica da espada do Hoshina, COMBATE-1, `techniques` do `hoshina_sword.json`): dois
cortes em X (`roll_degrees` 45, abertos 6 graus) que voam 12 blocos a 1,5 bloco/tick, ×1,5 cada, 18 ticks (corte no 7),
25 de stamina, 4 de calor, recarga própria de 90 ticks (a do NPC) [SUPOSIÇÃO]. Cada técnica tem a sua recarga.

**Formas do Hoshina do jogador (COMBATE-5)** [SUPOSIÇÃO], espada do Hoshina (base 7, cortes `flurry` a cada 3 ticks).
**Prévia de balanceamento** (Miguel 2026-10-29: "faça uma prévia agora e depois refazemos"): conta de dano por uso e
por segundo de recarga (base x multiplicador x cortes, sem Release, sem o teto de 12%/15% por golpe, que não chega a
limitar contra Honju), comparada ao machado da Kikoru (4,4–7,2/s) e às manoplas do Numbers 2 (3,5–5,3/s). As formas
comuns ficaram 3,1–4,8/s e foram subidas para a faixa; Yae-uchi e Jūni-hitoe são explosão com recarga longa:

| Forma | Antes | Agora | Dano por uso | Recarga | Dano/s |
|---|---|---|---|---|---|
| Kūuchi | 1 × 1,6 | igual | 11,2 | 2 s | 5,6 |
| Kōsa-uchi | 2 × 1,5 | igual | 21,0 | 4,5 s | 4,7 |
| Ran-uchi | 10 × 0,45 | 10 × 0,5 | 35,0 | 6,5 s | 5,4 |
| Kasumi-uchi | 3 × 0,75 | 3 × 1,1 | 23,1 | 5 s | 4,6 |
| Kaeshi-uchi | 2 × 1,0 | 2 × 1,6 | 22,4 | 4,5 s | 5,0 |
| Yae-uchi (atordoa Yoju 20) | 8 × 0,55 | 8 × 0,8 | 44,8 | 12 s | 3,7 |
| Jūni-hitoe (só Numbers 10 no limite) | 12 × 0,6 | 12 × 1,0 | 84,0 | 30 s | 2,8 |

Fora da faixa, para a COMBATE-8: Daruma Dropper do machado (7,2/s; 7,9/s no do Numbers 4) e o Force Blast das
manoplas (1,7/s, é golpe de área). Duelos não medidos.

### Soldados especiais (`special_soldier/<id>.json`, 0.6-D)

**Reno, Mina e Narumi** (0.7-C): vida, armadura e velocidade da seção 12 do Balanceamento v1.2; o resto
[SUPOSIÇÃO], sem duelos medidos ainda (o Miguel deixou o balanceamento para depois de todos os personagens).

| Personagem | Vida | Armadura | Velocidade | Release (teto) | `kaiju_damage` | Arma | Técnicas (multiplicador sobre a arma, recarga em ticks) |
|---|---|---|---|---|---|---|---|
| Reno (T5, suporte/rifle) | 360 | 17 | 0,31 | 30% (70%) | 0,5 | rifle (5) | tiro de precisão 2,2 (60), rajada 3 × 0,9 (50), munição congelante 1,2 + Lentidão III 4 s (140), supressão 5 × 0,6 + Lentidão I (160), coronhada 0,8 (40) |
| Mina (T6, anti-Daikaiju) | 420 | 20 | 0,30 | 40% (90%) | 0,65 | canhão (12) | tiro de precisão 1,4 (40), tiro do canhão 1,0 com explosão de raio 2,5 (50), canhão carregado 3,0 raio 3,5 (140), Anti-Giant 10,0 raio 4,5 (300, preparo de 2,5 s, prioridade contra Honju/numerados; IA-2: contra qualquer kaiju, só 4 tiros por vida) |
| Narumi (T6, comandante) | 480 | 20 | 0,32 | 40% (85%) | 1,3 (era 0,52; Miguel 2026-10-16: mais forte no geral) | baioneta (9) | estocadas 0,9/1,0/1,3 (40), investida 1,8 (70), tiro da baioneta 1,3 (60), varrida 1,5 com empurrão (90) |

**Formas com as armas numeradas (0.7-D)**, mesma regra (v1.2 seção 12 para vida/armadura/velocidade, o resto
[SUPOSIÇÃO]):

| Personagem | Vida | Armadura | Velocidade | Release (teto) | `kaiju_damage` | Arma | Técnicas / extra |
|---|---|---|---|---|---|---|---|
| Kikoru + No. 4 (T7, aérea) | 580 | 20 | 0,44 | 40% (90%) | 0,5 | machado | voo (`flight`: 0,45 bloco/tick, paira 2,5 acima do alvo, desce nas técnicas); machado veloz 0,9/1,0 (22), mergulho 1,8 com avanço (50), ataque vertical 2,4 expõe o núcleo 60 ticks (90), combo aéreo 0,8/0,9 (40), dash aéreo 1,2 (70) |
| Reno + No. 6 (T7, criocinese) | 650 | 20 | 0,36 | 50% (100%) | 0,80 (MOV-5E; era 0,75) | rifle | lança de gelo 2,6 + Lentidão II (50), explosão congelante 2,0 raio 3,5 + Lentidão IV 5 s (120), campo de gelo 8 × 0,8 em volta + Lentidão III (160), congelamento múltiplo 5 × 1,0 (140), canhões auxiliares 2 × 1,2 com explosão 1,5 (60) |
| Narumi + No. 1 (T7, previsão) | 650 | 20 | 0,36 | 50% (100%) | 0,85 | baioneta | as do Narumi + golpe no ponto fraco 2,0 expõe o núcleo 100 ticks (100) e contra-golpe previsto 1,6 com avanço (60); aparar 35% (era 15%) com janela de 16 ticks, contra-ataque com recarga 120 e reação 10 ticks, esquiva com reação 8 |

**Kafka e Kaiju No. 8 (0.7-F)** (v1.2 seção 11; o resto [SUPOSIÇÃO]):

| Forma | Vida | Armadura | Velocidade | Arma | Técnicas / extra |
|---|---|---|---|---|---|
| Kafka humano (T2) | 24 | 6 | 0,30 | rifle (5), `kaiju_damage` 0,3 | sequência de socos 0,5/0,6 (40); **vira o No. 8 abaixo de 50% da vida** (`transform.health_below` 0,5) |
| Kaiju No. 8 (T8) | 1.800 | 19 | 0,40 | punhos (`kn8:kaiju_no8_fist`, 11 de dano × 5 do Release 100% = 55, alcance 3), `kaiju_damage` 0,6 | sequência de 4 socos 0,9/1,0/1,1/1,4 (30), soco pesado 1,8 com empurrão (60), investida 1,5 (3–12 blocos, 70), golpe no chão 1,6 com explosão de raio 4 (120), rugido 8 × 0,6 + Lentidão II (240); **regenera 1,5%/s abaixo de 50%** (era 3%; Miguel, 2026-10-15); **volta a ser o Kafka depois de 15 s sem alvo** (`revert_after_idle_ticks` 300). O v1.2 fala em "dano base equivalente 55": é o golpe base (11 × 5) |

Armas novas: `weapon/mina_cannon.json` (arma de fogo, 12 de dano, perfil do rifle) e `weapon/narumi_bayonet.json`
(pesada, 9 de dano, alcance 3,6, perfil do machado) [SUPOSIÇÃO; as animações próprias para o jogador ficam para
depois]. Reno e Mina fecham distância só acima de 40 blocos (`gap_close_distance`): lutam de longe.

**Kikoru** (0.5.0-D8, `special_soldier/kikoru.json`; Balanceamento v1.0 seção 12: Kikoru normal T5, 390 de vida,
armadura 18, velocidade 0,34; 1,57 m, Miguel 2026-10-09; o resto [SUPOSIÇÃO], regra usada: mais forte que soldado
comum e que um Honju, mais fraca que o Hoshina, T5+): Release 30% + escalada até +45 (teto 75%), `kaiju_damage` 0,6,
machado `kn8:axe`. Defensora nas invasões `honju_assault`, `web_queen`, `no9_resurrection` (nível 3) e `kaiju_horde`
(nível 4, junto com o Hoshina). Técnicas: Axe Slash (2 golpes 0,8/0,95, recarga 30), Heavy Swing
(1,8, 80), Shockwave (onda 1,2 a 4–12 blocos, 100), Dash Strike (1,4 com avanço, 3–9 blocos, 60), Ground Smash (5
ondas curtas em leque, 160), Guard Break (1,5, expõe o núcleo 80 ticks, 160). Duelos com os números v1.0 (2 de
cada, servidor dedicado): vence o Honju marrom em 11–12 s (com 354–361 de vida), o No. 9 em 38–42 s (com 45–49%) e o
No. 10 pequeno em 28–34 s (com 10–51%); o Hoshina vence o No. 9 em 30 s com 58–73%.

**Calibragem pelo v1.2 (seção 36.8, 10 duelos por confronto, servidor dedicado):**

| Rodada | Kikoru × No. 9 (meta 25%) | Kikoru × No. 10 pequeno (meta 35%) |
|---|---|---|
| Defesas copiadas do Hoshina, `kaiju_damage` 0,6 | 10/10 (vida final 17–67%) | 10/10 (4–44%) |
| Esquiva 100 ticks, contra-ataque 200, aparar 15% | 8/10 (3–16%) | 5/10 |
| + `kaiju_damage` 0,52 | **5/10** (vence com 1–9%; perde com o No. 9 a 17–29%) | **3/10** |
| No. 9 com a análise (v1.2), ainda 0,52 | 0/10 | 0/10 |
| `kaiju_damage` **0,62** (atual) | **5/10** (vence com 1–20%, todas por fuga) | **0/10** (No. 10 com 13–63%) |

Causa achada antes de mexer nos números (regra 36.7.4): em 2 duelos o No. 9 usou 63 habilidades (43 garras, 14
Finger Gun, 6 investidas) e só 20 acertaram; a Kikoru esquivou 36 vezes (a esquiva herdada do Hoshina saía a cada
2 s). Vida 390 e velocidade 0,34 mantidas. Contra o No. 9 ainda fica 25 pontos acima da meta porque **a fuga do No. 9
a 15% conta como vitória** (ela nunca precisa matá-lo); baixar mais o dano jogaria o No. 10 pequeno abaixo da meta.
[DECIDIR] se fuga conta como vitória na matriz. Mesma bateria: Hoshina × No. 9 10/10 (30–37 s, 48–81%; meta 70%) e
Hoshina + No. 10 × No. 9 10/10 (23–27 s, 71–83%; meta 90%), sem mudança (valores "não alterar sem teste"). Com `kaiju_damage` 1,0 e teto 85% ela batia o No. 9 em
22 s (mais forte que o Hoshina): reduzido.

**Hoshina** (regra do Miguel: no poder total vence o No. 10 pequeno, fortitude 8,3, mas perde para a forma gigante,
fortitude 9; vence o No. 9 atual, mas não com facilidade; com o traje numerado 10, na 0.6-F, fica ainda mais forte):
vida **460**, armadura 20, velocidade 0,30 × (1 + 0,004 × Release), resistência a empurrão 0,6, `kaiju_damage` 1,0,
aura `violet_lightning`, duas espadas do Hoshina (base 7). Só aparece como defensor nas invasões de **nível 4 e 5**
(`kaiju_horde`, `mass_resurrection`; variante `"hoshina"` nos `defenders`).

**Escalada de combate** (`escalation`, Miguel: fica mais rápido e mais forte conforme o poder sobe): Release base
**40%**; com alvo, +**2 por segundo** até +**52**; sem alvo, −2 por segundo. Teto **`max_release` 92%** (Miguel: no
traje comum o máximo é 92%; 100% só com o traje numerado 10, na 0.6-F), já contando o bônus da vida baixa. Dano
×2,6 → ×4,7, velocidade +16% → +37%, redução de dano 16% → 37%; na escalada máxima as recargas das técnicas e do
dash caem **40%**.

| Técnica | Tipo | Golpes (× dano da arma) | Tempo | Recarga | Alcance (borda) | Prioridade |
|---|---|---|---|---|---|---|
| Kūuchi | corte a distância | 1 × 1,3 (12 blocos) | 14 ticks, corte no 5 | 30 | 4–12 | 2 |
| Kōsa-uchi | 2 cortes em X | 2 × 0,9 | 18, cortes no 7 | 90 | 4–12 | 3 |
| Ran-uchi | combo com avanço | 10 × 0,35 (a cada 2 ticks) | 28 | 130 | 0–5 | 3 |
| Kasumi-uchi | 3 golpes (20/20/60%) | 0,5 / 0,5 / 1,5 + passo lateral | 26 | 100 | 0–3,5 | 2 |
| Yae-uchi | 8 golpes | 8 × 0,45 (1 por tick), expõe o núcleo 100 ticks | 22 | 240 | 0–3,5 | 1 (+4 contra Honju) |

Reações: **esquiva** (dash 1,1, recarga 30, invulnerável 6 ticks, reage 5 ticks antes do impacto; também fecha
distância com o alvo a mais de 7 blocos), **Kaeshi-uchi** (só contra golpe `heavy`: dash lateral, invulnerável 10
ticks, contra-ataque ×2,5 8 ticks depois, recarga 120), **parry** (35% dos golpes corpo a corpo comuns de kaiju,
dano ×0,3, recarga 20, abre 10 ticks para o Kaeshi-uchi).

**Duelos com o balanceamento v1.0 (2026-10-08, servidor dedicado; No. 9 1.450, No. 10 1.650 / 4.600, transformação
desligada na luta contra o pequeno):** Hoshina × No. 10 pequeno vence 2/3 em 50–55 s (termina com 17–30%; perdeu uma
deixando o kaiju com 10%) = "luta difícil"; Hoshina × No. 10 gigante perde 2/2; Hoshina × No. 9 vence 3/3 em
38–42 s com 64–76%; Hoshina + No. 10 × gigante vence 2/2 em 64–67 s com 20–47%; Hoshina + No. 10 × No. 9 vence 2/2 em
~27 s com 74–82%. Todos batem com a seção 28 do Balanceamento v1.0.

Duelos medidos (2026-10-07, servidor dedicado, Hoshina final: 460 de vida, teto 92%; 0.6-E com o No. 10 real; na
luta contra a forma pequena a transformação ficou desligada só para o teste):

| Adversário | Resultado |
|---|---|
| Primigenius Honju normal (6,0) | vence em ~10 s quase sem dano |
| Kaiju No. 9 (8,0; regenera; foge com 15%) | vence 3/3 em 31–34 s, termina com 63–75% da vida |
| Kaiju No. 10 pequeno (8,3; regenera) | vence 5/5 em 40–64 s, termina com 4–45% da vida |
| Kaiju No. 10 gigante (9,0; 4.500 de vida) | perde 3/3 em ~2 min (deixa o gigante com 11–19%) |

Calibragem: com 3.600 de vida o gigante perdia 1 em 3; com a regeneração do No. 10 em 1,2%/s o pequeno vencia o
Hoshina metade das vezes (agora 0,8%/s; 2%/s abaixo de 20%).

**Espada do Hoshina na bancada:** 8 ferro, 6 fibra muscular, 4 fragmentos de núcleo, 1 núcleo intacto; desbloqueia
na patente **Vice-Capitão** [SUPOSIÇÃO].

**Hoshina com a arma numerada 10** (`special_soldier/hoshina_no10.json`, 0.6-F; Miguel: com o traje do No. 10 fica
ainda mais forte e só ele chega a 100%): tudo do Hoshina, com vida **520**, Release base **50%**, escalada até +50
(teto **`max_release` 100%** = sincronização com o traje). Números [SUPOSIÇÃO] calibrados por duelo:

| Parte | Valor |
|---|---|
| Cauda (sem espada: Miguel) | corta sozinha a cada **30 ticks** (×0,6 em Full Release), alcance **4,5** (borda), golpe ×1,0 da arma; alvo: quem mira o Hoshina > quem está atrás/do lado > o mais perto |
| Guarda da cauda | golpe `heavy` ou projétil vindo de fora dos **120°** da frente: dano ×**0,3**, recarga 30 ticks |
| Full Release (100%) | dano ×**1,15**, velocidade ×1,15, recargas das técnicas ×0,7 |
| Jūni-hitoe (só em Full Release) | 11 × 0,45 + último ×3,5 (preparo 10, 1 golpe a cada 2 ticks), ignora armadura [SUPOSIÇÃO], expõe o núcleo 160 ticks, recarga **600**, alcance 0–4, prioridade 20; não é interrompido por esquiva/contra-ataque |

Duelos medidos (2026-10-08, servidor dedicado): contra o **No. 10 gigante** vence 4/4 em ~1 min terminando com
22–51% da vida (com 600 de vida e Full Release ×1,25 vencia com 61–69%: fácil demais); contra o **No. 9** vence em
~25 s com 71–88% da vida (medido com 600 de vida). Em jogo, a cauda tira ~19 por golpe do Primigenius Honju com o Release em 50%.

### Soldados (`soldier/soldier_1.json`)

Níveis de força (Release): baixo 5, normal 10, alto 20, elite 30. Alcance de visão 40.
Balanceamento v1.0 (`level_stats`): vida / armadura / velocidade por nível — baixo (recruta) 20 / 5 / 0,29, normal
24 / 6 / 0,30, alto 28 / 8 / 0,31, elite 34 / 10 / 0,32 (antes todos 24 / 6 / 0,30).

Variantes comuns (0.4, peso no sorteio): Atirador rifle + faca de apoio (4), Patrulheiro pistola + faca (2),
Espadachim espada (0), Batedor faca (0), Recruta sem arma (0). **IA-1B (Miguel, 2026-10-15):** soldado comum nasce só
com arma de fogo (espada e faca continuam por comando, peso 0; defensores das invasões que eram "espada" viraram
"pistola"); **4 pentes reserva** (IA-2, Miguel 2026-10-16; `npc.spare_magazines` do perfil, contados em balas: a recarga completa o pente e as balas que sobravam voltam para a reserva; rifle 30 + 120 = 150 tiros, pistola 12 + 48 = 60) e depois a arma branca. Troca para a faca com o kaiju
a menos de 3,5 blocos (`sidearm_distance`). Soldado comum não usa machado (arma especial).

✅ 0.3 (decisão do Miguel: soldados simples precisam de grupo; fortes resolvem sozinhos): dano contra kaiju ×
`kaiju_damage` por nível — baixo **0,25**, normal **0,35**, alto **0,8**, elite **1,0**. [SUPOSIÇÃO] O tiro do
soldado acerta o corpo (sem núcleo nem partes): mirar no núcleo é habilidade do jogador.

Medido em jogo (2026-10-07, soldados com rifle contra kaiju parado):

| Atacantes | Trichonephila (57) | Primigenius (211) |
|---|---|---|
| 1 normal | 12 s | 54 s (na luta real ele morre antes) |
| 4 normais | 3,4 s | 13 s |
| 1 alto | 6,4 s | 16 s |
| 1 elite | 2,4 s | 9 s |

### 0.8-A: Isao (Numbers 2), No. 9 com o Isao e Kaiju No. 13

Fichas e fontes: `docs/FICHAS_0_8_A.md`. Duelos no servidor dedicado (`duel12.py`, 5 por confronto, formas do No. 9
isoladas pelo datapack sem `transform`/`absorb`; fuga do No. 9 conta como vitória).

**Isao** (`special_soldier/isao.json`, v1.2 §12 "Isao + No. 2"): vida 700, armadura 20, velocidade 0,32, Release 50
(teto 96 [SUPOSIÇÃO]), `kaiju_damage` **1,2**, altura 1,90. Manoplas `weapon/numbers2_gauntlet.json` (base 9, como o
punho do No. 8). Técnicas: Barrage Blast 0,7/0,7/0,8/0,8/1,2 (1 a cada 3 ticks, recarga 40), Spiral Blast 1,5 (onda
reta de 28 blocos, recarga 70), Force Blast 7 ondas de 0,8 em 90° (recarga 160), Thunder Emperor 1,6 + **3,0**
(avanço, expõe o núcleo 60 ticks, recarga **160**). Pesados (quebram blocos, força 4, raio 3): Thunder Emperor e Force
Blast. Guarda contra golpe pesado 45%, arco 140°. `tactics`: agressão 0,85, distância 3, recua com 12%, pesado contra
grande **3**, rápido contra pequeno 1, protege aliado 10.

| Isao × | 1ª rodada (`kaiju_damage` 0,8) | 2ª (1,0; Thunder 1,6/3,0) | Final (1,2; pesado contra grande 3) | Meta v1.2 |
|---|---|---|---|---|
| No. 10 pequeno | 5/5 (43–75% de vida) | 5/5 | 5/5 (68–81%) | 85% |
| No. 9 | 5/5 (todas por fuga) | 5/5 (fuga) | — | 75% |
| No. 10 gigante | 0/5 (gigante com 18–29%) | 0/5 (2–17%) | **4/5** (vence com 10–32%) | 60% |
| No. 9 preto | 0/5 (preto com 38–48%) | 1/5 | **3/5** (3 por fuga) | 40% |
| No. 13 | 5/5 (84–92%) | — | — | — |

**No. 9 com o Isao** (`kaiju/kaiju_no9_isao.json`, v1.2 §9): **8 m** (escala 4, Miguel 2026-10-16), vida 7.500, dano 72, armadura 20, velocidade 0,38,
fortitude 9,5, regeneração 4,5%/s abaixo de 50% e 7,5%/s abaixo de 20%; pele endurecida 20%/−55%, análise 3,5% por
golpe até 30% [SUPOSIÇÃO]. Habilidades: garra, Finger Gun, rajada de dedos, tentáculo, voo curto (do No. 9 preto) e
`no9_isao_barrage_blast` (5 × 0,75), `no9_isao_thunder_emperor` (×3,0 pesado, cratera), `no9_isao_force_blast` (área
6 blocos ×1,3 pesado, abaixo de 80% da vida), `no9_isao_sonic_blast` (projétil ×1,5 com explosão 2,5). IA: circula
pouco (0,1; chefe que pressiona). Surge pela absorção do Isao abaixo de 30% da vida (`absorb` do No. 9 e da forma preta).

| Confronto | Resultado | Meta v1.2 |
|---|---|---|
| Hoshina No. 10 × No. 9 com o Isao | 0/5 (o kaiju termina com 89–94%) | 5% |
| Kaiju No. 8 × No. 9 com o Isao | antes: 5/5 empates de 300 s com os dois a ~50% (regeneração dos dois); depois (circula 0,1, Barrage 0,75 recarga 70, Thunder ×3,0 recarga 140): **0/3**, o No. 8 cai em 157–202 s com o kaiju a 49% | 3% |

**Kaiju No. 13** (`kaiju/kaiju_no13.json`, **proposta do Miguel de 2026-10-16**, ainda sem teste dele): **6 m** (escala
2,727), vida **2.600**, dano **44**, armadura **17**, velocidade 0,48, fortitude **9,1** (o Miguel deu "9,0 → 9,2":
ficou o meio; a vida/dano/armadura vêm do `overrides`), inteligência 3 [SUPOSIÇÃO]. Antes (minha [SUPOSIÇÃO]): vida 900,
dano 26 → 34, armadura 12, fortitude 7. Habilidades: garra, `no13_flurry` (4 × 0,5),
`no13_heavy_punch` (×1,7 pesado), `no13_throat_grab` (projétil curto ×1,1, lento nível 4 por 2 s), `no13_sprint`
(investida ×1,3 até 22 blocos), `no13_pounce` (salto ×1,4). IA: circula (0,45) e persegue rápido (×1,25).

| No. 13 × | Dano 26 | Dano 34 |
|---|---|---|
| Hoshina | 5/5 (Hoshina com 76–84%) | 5/5 (52–76%) |
| Kikoru | 5/5 (52–72%) | 5/5 (25–49%) |
| Kaiju No. 8 | 5/5 em 12–22 s (96–98%) | — |
| Isao | 5/5 (84–92%) | — |

**Depois da altura (No. 9 com o Isao 8 m, No. 13 6 m) e dos números do No. 13 do Miguel (2026-10-16)**, 5 duelos:

| Confronto | Resultado | Observação |
|---|---|---|
| Kaiju No. 8 × No. 13 | 5/5 (No. 8 com 73–84%, 40–51 s) | antes 92–98% em 12–22 s |
| Hoshina × No. 13 | 3/5 (vence com 3–25%; perde com o No. 13 a 3–5%) | luta parelha |
| Kikoru × No. 13 | 0/5 (No. 13 com 50–73%) | |
| Isao × No. 13 | 5/5 (Isao com 48–65%) | |
| Hoshina No. 10 × No. 13 | 5/5 (53–69%) | |
| Narumi × No. 13 | 0/5 com 0,52 (No. 13 com 45–56%); 3/5 com 1,1; **5/5 com 1,3** (Narumi com 3–49%) | Miguel 2026-10-16: o Narumi vence o No. 13 e fica mais forte no geral → `kaiju_damage` 1,3. Com 1,3: No. 9 5/5 (todas por fuga, Narumi com 62–96%), No. 10 pequeno 5/5 (4 viram a forma gigante perto de 50%: o script conta como fuga), No. 10 gigante 0/5 (gigante com 18–55%) |
| Kaiju No. 8 × No. 9 com o Isao (8 m) | 0/5 (3 derrotas em 184–260 s, 2 empates de 300 s com o No. 8 a 18–31% e o kaiju a 45–50%) | meta 3%; os empates voltaram com o corpo maior [DECIDIR] |
| Hoshina No. 10 × No. 9 com o Isao (8 m) | 0/5 (kaiju com 88–95%, 19–26 s) | meta 5% |

## Decisões de 2026-10-22 (Miguel)

- **Matriz:** fuga do No. 9 (15%) e fim do tempo = **empate** (antes a fuga contava como vitória).
- **Regeneração dos numerados:** `[kaijuAi] exposedCoreStopsRegeneration` (padrão `true`): No. 9, No. 10 e formas não
  regeneram com o núcleo exposto (v1.2, regeneração dinâmica).
- **Isao com o Numbers 2:** Thunder Emperor `expose_core_ticks` 120 (era 60) [SUPOSIÇÃO]; `kaiju_damage` 1,2 mantido.
- **Narumi No. 1:** `kaiju_damage` 1,4 (era 0,85) e `tactics.exploit_openings` 3 [SUPOSIÇÃO].

| Confronto (fuga = empate) | V / E / D | Observação |
|---|---|---|
| Isao × No. 10 gigante | 7 / 0 / 3 de 10 | meta 60%; antes 0/5; vence com 2–44% |
| Isao × No. 10 gigante, só o núcleo cortando a regeneração (Thunder 60) | 2 / 0 / 3 | com 1,35 de dano: 1/0/4 |
| Narumi No. 1 × No. 13 | 5 / 0 / 0 | ~25 s, 53–70% de vida |
| Narumi × No. 13 | 4 / 0 / 1 | ~40 s, 26–45% |
| Narumi No. 1 × No. 10 pequeno | 5 / 0 / 0 | ~17 s, 57–78% |
| Narumi No. 1 × No. 9 / Narumi × No. 9 | 0 / 5 / 0 e 0 / 5 / 0 | todos por fuga; No. 1 em ~16 s, Narumi em ~26 s |
| Narumi No. 1 × No. 10 gigante / Narumi × gigante | 0 / 0 / 5 e 0 / 0 / 5 | gigante com 1–16% contra 23–52% |
| Hoshina × No. 10 gigante / Hoshina No. 10 × gigante | 0 / 0 / 3 e 3 / 0 / 0 | papéis mantidos |

## MOV-6 (kaiju, 2026-10-22) [SUPOSIÇÃO nos números]

| Config `[kaijuAi]` | Padrão | O que faz |
|---|---|---|
| `strikeNeedsClearPath` | true | Golpe de perto/área do kaiju só fere sem bloco no caminho |
| `abilityFailLimit` | 3 | Falhas seguidas até a habilidade descansar (0 desliga) |
| `abilityFailRestTicks` | 160 | Recarga extra do descanso |
| `leapCheckLanding` | true | Salto só com arco livre e chão no pouso |
| `leapMaxDrop` | 8 | Chão do pouso até tantos blocos abaixo do pé do alvo |
| `flightSteer` | true | Voadores desviam de prédios e morros |
| `flightLookahead` | 8 | Alcance da conferência do caminho (+ meia largura do corpo) |
| `flightClimb` | 6 | Quanto sobe para passar por cima (+ meia altura) |
| `flightSteerIntervalTicks` | 5 | Intervalo entre as conferências |

Duelos depois da MOV-6 (fuga = empate): Hoshina × No. 10 pequeno 3/0/0, Isao × No. 10 gigante 2/0/1, Narumi No. 1 ×
No. 13 3/0/0, Kikoru × No. 9 0/3/0 (fugas), Kaiju No. 8 × No. 9 preto 0/3/0 (fugas): sem regressão.

## MOV-7 (2026-10-22): matriz com a fuga como empate

29 confrontos × 5 duelos em `docs/MOV7_REGRESSAO.md` §3. Nenhum confronto mudou por causa da mobilidade. Com a fuga do
No. 9 contando como empate, todos os duelos contra o No. 9 e as formas dele terminam em empate (fuga a 15%) ou derrota
[DECIDIR metas].

## Retirada do No. 9 (Miguel, 2026-10-22: "tenta se retirar; se não conseguir, morre")

Bloco `retreat` no `numbered/<id>.json` das 6 formas do No. 9 [SUPOSIÇÃO nos números]: `max_ticks` 300 (15 s),
`escape_distance` 24, `escape_ticks` 60 (3 s a salvo), `speed` 1,4, `smoke_interval_ticks` 20, `regenerate` false.
Começa na vida do `flee_health` (15%; fusões 10%).

Duelos (4 por confronto, V = matou, E = escapou, D = morreu):

| Confronto | Antes (fuga instantânea) | Com a retirada |
|---|---|---|
| Hoshina × No. 9 | 0 / 5 / 0 | 2 / 2 / 0 |
| Hoshina No. 10 × No. 9 | 0 / 5 / 0 | 1 / 3 / 0 |
| Kaiju No. 8 × No. 9 | 0 / 5 / 0 | **4 / 0 / 0** |
| Kikoru × No. 9 | 0 / 4 / 1 | 1 / 2 / 1 |
| Narumi No. 1 × No. 9 | 0 / 5 / 0 | **4 / 0 / 0** |
| Isao × No. 9 | 0 / 5 / 0 | 2 / 2 / 0 |
| Reno No. 6 × No. 9 | 0 / 5 / 0 | 1 / 3 / 0 |
| Mina × No. 9 (40 blocos) | 0 / 3 / 2 | 0 / 2 / 2 |
| Hoshina No. 10 × No. 9 preto | 0 / 5 / 0 | 0 / 4 / 0 |
| Kaiju No. 8 × No. 9 preto | 0 / 5 / 0 | 1 / 1 / 0 (2 por tempo, 5 min) |

Ele foi encurralado (15 s sem escapar, volta a lutar) 9 vezes nesses 40 duelos.

## PM-1: esquiva perfeita e funções das armas (2026-10-23) [SUPOSIÇÃO todos os números]

| Item | Valor | Onde |
|---|---|---|
| Janela da esquiva perfeita | 3 ticks do começo da esquiva (a esquiva é invulnerável 6) | `[combat] perfectDodgeTicks` |
| Contra-ataque | o primeiro acerto em 30 ticks é crítico (×1,5) | `perfectDodgeCounterTicks`, `criticalMultiplier` |
| Stamina devolvida | 12 (a esquiva custa 20: nunca sai de graça) | `perfectDodgeStaminaRefund` |
| Atordoamento | Yoju 20 ticks, só golpe corpo a corpo | `perfectDodgeStaggerTicks` |
| Espada: Corte em Avanço (`lunge`) | ×1,8, 5 blocos à frente, meia largura 1,3, impacto no tick 5 de 16, stamina 25, calor 3, recarga 6 s, empurrão 0,4, atordoa Yoju 12 ticks | `weapon/sword.json`, `[combat] lungeSpeedPerBlock` 0,45 |
| Faca: Sequência de Cortes (`flurry`) | 4 cortes de ×0,9 a cada 3 ticks até 2,6 blocos, stamina 20, calor 2, recarga 5 s | `weapon/combat_knife.json`, `[combat] flurryHits`/`flurryIntervalTicks` |
| Rifle: tiro preciso (clique direito) | ×2,2, 24 ticks (impacto no 6), 1 bala, conta como golpe pesado (teto de 15%) | `weapon/rifle.json` `actions.heavy` |
| Pistola: tiro preciso | ×1,7, 14 ticks (impacto no 4), 1 bala | `weapon/pistol.json` `actions.heavy` |

## PM-2: ferimentos por parte (2026-10-23) [SUPOSIÇÃO todos os números]

Dano que a parte levou de fato (depois da armadura) soma no ferimento dela; passando de `threshold` x vida máxima, a
parte fica ferida por `duration_ticks`. Ferida, não acumula (o efeito não cresce) e, ao sarar, a conta recomeça.
No máximo 2 partes feridas ao mesmo tempo (`max_active`); com o teto cheio o dano fica parado no limiar. O núcleo
não fere (tem o sistema dele). Números no `kaiju_ai/<id>.json` (`wounds`); `[kaijuAi] wounds` liga/desliga e
`woundBleedIntervalTicks` 40 é o intervalo das lascas desenhadas.

| Espécies | Parte | Limiar | Dura | Efeito |
|---|---|---|---|---|
| Primigenius (4), Philinosoma, Diclonius | pernas | 12% | 20 s | velocidade ×0,7, sem investida/salto, parte leva ×1,2 |
| Primigenius (4), Philinosoma, Diclonius | cabeça | 10% | 15 s | atordoa 1,5 s ao abrir, atordoamentos ×1,5; Honju/revivido/Philinosoma/Diclonius sem mordida e sem o tiro/sopro da boca |
| Philinosoma, Diclonius | cauda | 10% | 20 s | sem rabada |
| Trichonephila, Trichonephila Honju | cefalotórax (onde saem as patas) | 12% | 20 s | velocidade ×0,7, sem salto |
| Trichonephila, Trichonephila Honju | abdômen (fiandeiras) | 12% | 20 s | sem teia |
| Trichonephila Honju | cabeça | 12% | 15 s | atordoa, sem mordida (na aranha comum a cabeça é o núcleo) |
| Camponotus e revivida | tórax / abdômen | 12% | 20 s | tórax: ×0,7 e sem salto; abdômen (glândula de ácido): sem o jato de ácido |
| Phaneroplasmodium | pernas | 12% | 20 s | ×0,7 (o chapéu é o núcleo) |
| Myxogasterocarp | chapéu / raízes | 12% | 20 s | chapéu: atordoa 1 s e sem a bomba de esporos; raízes: ×0,7 |
| No. 9 e formas, No. 10 (as duas), No. 13 | pernas | 12% | 10 s (regeneram) | ×0,75, sem investida/salto, parte leva ×1,2 |
| No. 9 e formas, No. 10 (as duas), No. 13 | cabeça | 10% | 10 s | atordoa 1 s, atordoamentos ×1,3 |
| No. 9 com a formiga | cabeça / tórax / abdômen | 10-12% | 10 s | cabeça: sem mordida e Mandible Rush; tórax: ×0,75, sem bote e Ant Rush; abdômen: sem o ácido |
| Preondactyl, larva | — | — | — | sem partes: sem ferimento |

Sem parte de braço nos modelos atuais: "braço ferido tira um ataque" fica para quando houver a parte (a cauda e o
abdômen fazem esse papel onde existem).

## PM-3: leitura de hábitos dos kaiju inteligentes (2026-10-23) [SUPOSIÇÃO todos os números]

Só kaiju com `intelligence` ≥ `[kaijuAi] smartIntelligence` 4 e bloco `habits` no `kaiju_ai/<id>.json`: No. 9 e
as 5 formas, No. 10 pequeno e gigante. O No. 13 tem inteligência 3 no JSON e fica de fora (o audit do Plano Mestre o
listava). `[kaijuAi] habits` liga/desliga.

| Item | Valor |
|---|---|
| Janela da memória | 200 ticks (10 s); sem alvo 200 ticks = esquece; alvo novo = esquece |
| Distância | amostra a cada 10 ticks; "fica longe" ≥ 10 blocos entre bordas, "fica colado" ≤ 2,5, em 70% de no mínimo 8 amostras |
| Esquiva | 3 golpes do kaiju no alvo que não pegam na janela (barrado por parede não conta) |
| Repetição | 4 acertos seguidos do alvo do mesmo tipo (leve, pesado, a distância) |
| Resposta | 100 ticks; depois espera 160 antes de ler de novo; o sinal usado é apagado |
| Fica longe | +3 de prioridade a investida, salto e tiro; perseguição ×1,3 |
| Fica colado | recua 5 blocos por até 30 ticks; +3 a golpe de área, varrida e tiro |
| Esquiva | +3 a golpe de área, varrida, sequência e tiro (mais difíceis de esquivar) |
| Repete | recua 4 blocos por até 20 ticks; +2 a golpe de área e varrida (quebra o ritmo) |

## PM-4: terreno por espécie e papéis em grupo (2026-10-23) [SUPOSIÇÃO todos os números]

`kaiju_ai/<id>.json`, blocos `terrain` e `group`.

| Espécie | Terreno | Grupo |
|---|---|---|
| Primigenius | quebra (força da categoria, 3) | agressor, máx. 3 no alvo; líder morto: recua 4 s |
| Primigenius ressurgido | quebra | flanco (chega por trás), máx. 3; líder morto: fúria 10 s |
| Primigenius Honju, revivido | força própria 4, atravessa água | agressor, **líder** (dá o alvo a quem está sem a até 24 blocos) |
| Trichonephila | **contorna** (quebra só travada 3 s) | flanco, máx. 3; líder morto: fúria 8 s |
| Trichonephila Honju | contorna (travada 4 s) | agressor, líder |
| Camponotus | quebra | apoio a 7 blocos (ácido), máx. 3; líder morto: recua 4 s |
| Camponotus revivida | quebra | apoio a 7, máx. 3; líder morto: fúria 8 s |
| Phaneroplasmodium | quebra | apoio a 8 (esporos), máx. 3 |
| Myxogasterocarp | atravessa água | apoio a 10 (bomba de esporos), líder |
| Philinosoma | atravessa água | agressor |
| Diclonius | atravessa água | apoio a 10 (sopro) |
| No. 9 e formas, No. 10 | gigantes e formiga-No. 9 atravessam água | agressor, líder |
| No. 13 | quebra | flanco |

Fúria do líder morto = Força I + Velocidade I ocultas (sem partículas). "Colado no alvo" = até o alcance + 1 bloco,
recontado a cada `[kaijuAi] scanIntervalTicks`; dois kaiju podem entrar no mesmo intervalo (visto: 4 colados uma vez).
O apoio só vale para quem tem golpe a distância; perto demais (2 blocos dentro da distância) se afasta.

## PM-5: mudança concreta por fase dos chefes (2026-10-23) [SUPOSIÇÃO todos os números]

`boss/<id>.json`, campos novos em cada fase: `speed_multiplier` (velocidade de andar, modificador `kn8:boss_phase_speed`),
`cooldown_multiplier` (recargas das habilidades), `preferred_distance` (com golpe a distância pronto, segura essa
distância; 0 = não), `expose_core_ticks` (núcleo exposto depois da invulnerabilidade da troca) e `message` (aviso).

| Chefe | Fase | Velocidade | Recargas | Distância | Núcleo exposto |
|---|---|---|---|---|---|
| Honju (`honju`) | 1 (até 50%) | x1,0 | x1,0 | — | — |
| | 2 | x1,2 | x0,75 | 10 | 40 ticks |
| Honju revivido | 1 (até 60%) | x1,0 | x1,0 | — | — |
| | 2 (até 30%) | x1,15 | x0,85 | 10 | 30 ticks |
| | 3 | x1,3 | x0,65 | — | 40 ticks |
| Tecedeira (`trichonephila_honju`) | 1 (até 50%) | x1,0 | x1,0 | — | — |
| | 2 | x1,25 | x0,7 | — | 40 ticks |
| Honju do exame | fase única | x1,0 | x1,0 | — | — |

Primeira versão com 80 ticks de núcleo exposto: com a Mina na luta o Honju chefe morria em ~3 s depois da troca (o
Anti-Giant no núcleo); 40 ticks deixam a janela sem acabar a luta. A recompensa continua só na derrota.

## PM-6: ordens do esquadrão (2026-10-23) [SUPOSIÇÃO todos os números]

`[squad]` no `kn8-server.toml`.

| Chave | Valor | O quê |
|---|---|---|
| `focusRange` | 64 | Até onde o "focar" acha o kaiju mirado (blocos) |
| `focusConeDegrees` | 12 | Ângulo da mira até o ponto da caixa do kaiju mais perto dela (0 = só com a mira passando pela caixa) |
| `protectRadius` | 16 | Raio da área da "guarda"; anel do atirador 0,25 x raio (4), do assalto 0,5 x raio (8) |

## PM-7: provas de promoção e avaliação pós-missão (2026-10-23) [SUPOSIÇÃO todos os números]

Nota = média das partes medidas (0-100 cada): **tempo** (100 até o tempo ideal da prova, depois ideal/tempo),
**precisão** (acertos/tiros ÷ `gradeAccuracyForFullScore`; só com `gradeMinShots`+ tiros, ou o `min_shots` da prova),
**dano recebido** (100 x (1 - dano/vida máxima ÷ `gradeDamageTakenForZeroScore`)) e **aliados** (100 - perdas x
`gradeAllyLostPenalty`). `[career]` no `kn8-server.toml`:

| Chave | Valor | O quê |
|---|---|---|
| `gradeS` / `gradeA` / `gradeB` / `gradeC` | 90 / 75 / 60 / 40 | Nota mínima de cada letra (abaixo de C = D) |
| `gradeAccuracyForFullScore` | 0,6 | Precisão que vale 100 pontos |
| `gradeDamageTakenForZeroScore` | 2,0 | Dano recebido (em vidas máximas) que zera a parte |
| `gradeAllyLostPenalty` | 25 | Pontos por soldado do esquadrão perdido |
| `gradeMinShots` | 5 | Tiros para a precisão entrar na nota |
| `gradeMeritBonusS` / `gradeMeritBonusA` | 0,2 / 0,1 | Mérito extra (fração da recompensa) em S e A |

Provas (`grading` no `mission/<id>.json`; patente = `promotion_mission` do `rank/<id>.json`, junto do mérito):

| Patente | Prova | Objetivo | Requisitos | Espera ao reprovar | Recompensa |
|---|---|---|---|---|---|
| Oficial | `exam` (Exame de Admissão) | o de antes | só avaliada (nota mínima D), tempo ideal 5 min | — | igual |
| Oficial Sênior | `exam_marksman` | 3 Primigenius | precisão 35% com 15+ tiros, dano ≤ 100% da vida, nota C, ideal 4 min | 10 min | 150 mérito, 200 XP |
| Líder de Pelotão | `exam_squad` | 4 Primigenius | esquadrão 2+, 0 perdas, dano ≤ 80%, nota C, ideal 5 min | 10 min | 300, 400 |
| Vice-Capitão | `exam_command` | Honju (chefe) | esquadrão 3+, até 1 perda, dano ≤ 100%, nota B, ideal 8 min | 20 min | 800, 800 |
| Capitão | `exam_captain` | horda (`kaiju_horde`) | esquadrão 4+, até 2 perdas, dano ≤ 150%, nota B | 20 min | 2000, 1500 |

## PM-8: instalações da base (2026-10-23) [SUPOSIÇÃO todos os números]

`[squad]` no `kn8-server.toml`:

| Chave | Valor | O quê |
|---|---|---|
| `stationRadius` | 16 | Membros do esquadrão até essa distância da estação são atendidos |
| `stationCooldownTicks` | 1200 | Espera entre usos da estação por jogador (1 min; criativo sem espera) |

Estação: cura até a vida máxima, tira só os efeitos ruins, completa a reserva até `npc.spare_magazines` pentes do
perfil da arma de fogo (rifle 4 x 30 = 120, pistola 4 x pente) cobrando **1 item de munição por bala** do inventário
do jogador (criativo não paga). Receita na bancada: 6 ferro, 2 estimulantes, 1 pente de rifle (Oficial Sênior).

## PM-8B: provas (cursos) dentro da base (2026-10-23) [SUPOSIÇÃO todos os números]

`[career]` no `kn8-server.toml`:

| Chave | Valor | O quê |
|---|---|---|
| `examOfficerRange` | 8 | A prova só é aceita com o Oficial de Provas a até essa distância |
| `examAreaMinDistance` / `examAreaMaxDistance` | 6 / 14 | Kaiju e pontos da prova surgem nessa faixa de onde ela foi aceita (dentro da base) |
| `examLeaveDistance` | 64 | Passar disso (na horizontal) do lugar da prova reprova ("você saiu da base") |
| `examProtectRadius` | 40 | Área protegida (sem quebrar blocos) em volta do lugar da prova enquanto ela dura |

XP do corpo por prova (`rewards.body_xp` do `mission/*.json`):

| Prova | Força | Velocidade | Resistência | Agilidade | Outros |
|---|---|---|---|---|---|
| Curso de Agilidade (`course_agility`) | | 80 | | 120 | patrulha de 5 pontos, 2 min, repete a cada 10 min |
| Curso de Força (`course_strength`) | 160 | | 40 | | 2 Primigenius, 5 min, repete a cada 10 min |
| Curso de Resistência (`course_endurance`) | 40 | | 160 | | 1 Primigenius ressurgido, 5 min, repete a cada 10 min |
| Exame de Admissão (`exam`) | 150 | | 100 | | |
| Prova de Tiro (`exam_marksman`) | | 100 | | 150 | |
| Prova de Esquadrão (`exam_squad`) | | 150 | 200 | | |
| Prova de Comando (`exam_command`) | 250 | | 250 | | |
| Prova de Capitão (`exam_captain`) | 300 | 200 | 300 | 200 | |

Kaiju de prova não dão mérito nem contam no total de abates da carreira (para a prova não virar farm); o chefe da
prova ainda dá o mérito do `boss/*.json` [SUPOSIÇÃO].

## PM-8C: pesquisa no laboratório (2026-10-23) [SUPOSIÇÃO todos os números]

`[research]` no `kn8-server.toml`: `terminalRange` 6 (blocos do terminal para começar), `durationMultiplier` 1,0
(multiplica o tempo de todas). Uma pesquisa por vez; termina sozinha no tempo de jogo, mesmo longe da base. O bônus
de dano vale no golpe do jogador (comum, pesado, tiro e especial) contra o kaiju, antes do teto de 12%/15% por golpe;
bônus de pesquisas diferentes se somam.

| Pesquisa (`research/*.json`) | Patente | Pré-requisito | Material | Tempo | Efeito |
|---|---|---|---|---|---|
| Análise de Tecido Yoju (`yoju_tissue`) | | | 8 tecido | 1 min | +10% contra Yoju |
| Estudo dos Artrópodes (`arachnid_study`) | | Tecido Yoju | 10 tecido, 1 fragmento de núcleo | 2 min | +15% contra Trichonephila, Tecedeira, Camponotus e revivida |
| Mecânica Muscular (`muscle_mechanics`) | Oficial | Tecido Yoju | 6 fibra muscular, 4 tecido | 3 min | Libera o Mk1 Reforçado (sem esperar Oficial Sênior) |
| Ressonância do Núcleo (`core_resonance`) | Oficial | | 3 fragmento de núcleo | 4 min | Libera o Catalisador de Release (sem esperar Líder de Pelotão) |
| Anatomia Honju (`honju_anatomy`) | Oficial Sênior | Mecânica Muscular | 1 núcleo intacto, 6 fibra, 10 tecido | 6 min | +12% contra Honju |
| Teoria das Armas Numeradas (`numbered_weapon_theory`) | Líder de Pelotão | Ressonância e Anatomia Honju | 2 núcleo intacto, 6 fragmento | 10 min | Libera a Espada do Hoshina (sem esperar Vice-Capitão), +5% contra numerados |

Terminal de pesquisa na bancada (Oficial): 6 ferro, 4 painéis de vidro, 4 redstone, 2 tecido de kaiju.

## PM-8D: base modular do jogador (2026-10-23) [SUPOSIÇÃO todos os números]

`[base]` no `kn8-server.toml`: `manageDistance` 24 (blocos do núcleo para construir/demolir; criativo em qualquer
lugar), `protect` true (kaiju não quebram blocos na grade da base). Uma base por jogador e dimensão; 8 lotes de
11 x 11 em volta do núcleo (grade 3 x 3, 1 bloco de passagem). Demolir não devolve o material.

| Módulo (`base_module/*.json`) | Patente | Pede | Material | Tem |
|---|---|---|---|---|
| Oficina (`workshop`) | | | 32 tijolos de pedra, 8 ferro | 2 bancadas, ferraria, bigorna, 2 baús |
| Laboratório (`laboratory`) | Oficial | | 32 tijolos, 8 vidro, 4 ferro, 4 tecido de kaiju | 2 terminais de pesquisa, suporte de poções |
| Alojamento (`barracks`) | Oficial Sênior | | 32 tijolos, 4 lã branca, 4 ferro | Estação de preparação, 4 camas |
| Comando (`command`) | Oficial Sênior | Oficina | 32 tijolos, 8 ferro, 4 ouro | Oficial de Missões e Oficial de Provas |
| Pátio de Treino (`training`) | | | 16 tábuas de carvalho, 4 fardos de feno | 3 bonecos de treino, alvos |

Núcleo da Base na bancada (Oficial): 8 ferro, 2 fragmentos de núcleo, 4 redstone.

## PM-9: alerta por região e rastro de destruição (2026-10-23) [SUPOSIÇÃO todos os números]

`[region]` no `kn8-server.toml`:

| Chave | Valor | O quê |
|---|---|---|
| `enabled` | true | Locais da obra reagem a kaiju e invasões |
| `kaijuCheckTicks` | 40 | Cada kaiju informa o local onde está nesse intervalo (sem varredura global) |
| `alertTicks` | 1200 | Alerta dura 1 min depois do último kaiju visto |
| `combatTicks` | 600 | Combate dura 30 s depois da última luta (ou enquanto a invasão centrada no local durar) |
| `recoveryTicks` | 2400 | Recuperação de 2 min antes de voltar ao normal |
| `notifyMargin` | 32 | Jogadores até 32 blocos em volta do local recebem o aviso de mudança |
| `closeServicesInCombat` | true | Em combate: estação de preparação, terminal de pesquisa e aceite de provas fechados |

`[trail]`:

| Chave | Valor | O quê |
|---|---|---|
| `enabled` | true | Kaiju deixam pistas onde quebram o caminho/golpeiam com destruição e onde caem |
| `maxPoints` | 256 | Pistas por dimensão (as mais velhas saem) |
| `lifeTicks` | 12000 | Pistas somem depois de 10 min |
| `pointSpacing` | 8 | Uma pista nova por kaiju a cada 8 blocos |
| `scanRadius` | 48 | Alcance do Rastreador de Kaiju |
| `scanCooldownTicks` | 60 | Espera entre usos do Rastreador |
| `markTicks` | 200 | Tempo do caminho desenhado no chão depois de investigar |
| `meritPerClue` | 2 | Mérito por pista investigada pela primeira vez (por qualquer jogador) |

Rastreador de Kaiju na bancada (livre): 3 ferro, 1 bússola, 2 redstone.

## PM-10A: evacuação com civis (2026-10-23) [SUPOSIÇÃO todos os números]

`[evacuation]` no `kn8-server.toml`:

| Chave | Valor | O quê |
|---|---|---|
| `regionTicks` | 200 | O local fica em EVACUAÇÃO 10 s depois do último segundo de uma evacuação nele (renovado a cada segundo) |
| `spawnMin` / `spawnMax` | 10 / 18 | Distância dos civis ao jogador quando a missão começa |
| `extractionMin` / `extractionMax` | 28 / 44 | Distância do ponto de extração aos civis |
| `extractionRadius` | 4 | Civil a até 4 blocos do ponto é evacuado (também o raio da zona verde) |
| `followRange` | 12 | Civil segue o jogador ou soldado mais perto até essa distância |
| `selfWalkRadius` | 16 | Perto do ponto o civil vai sozinho |
| `threatDistance` | 18 | O kaiju da missão surge a essa distância dos civis |
| `panicDistance` | 8 | Civil foge de kaiju mais perto que isso |
| `markIntervalTicks` | 40 | Zona de extração redesenhada no chão |
| `kaijuTargetCivilians` | true | Kaiju atacam civis como a soldados |

Civil: 20 de vida, velocidade 0,3 (jogadores e soldados não o ferem).

| Missão | Patente | Civis (+ folga) | Perdas permitidas | Ameaça | Prazo | Recompensa |
|---|---|---|---|---|---|---|
| `evacuation_district` | Candidato | 4 (+1) | 1 | Primigenius | 8 min | 90 mérito, 150 XP de treino |
| `evacuation_honju` | Oficial Sênior | 6 (+2) | 2 | Primigenius Honju | 10 min | 220 mérito, 300 XP, resfriador |

Civis perdidos contam na nota da missão como aliados perdidos.

## PM-10B: apoio militar pelo Rádio de Campo (2026-10-23) [SUPOSIÇÃO todos os números]

`[support]` no `kn8-server.toml`. Só durante uma missão de campo (provas não contam) ou dentro da área de uma invasão;
usos contados por operação (missão aceita ou invasão começada de novo = usos novos); a espera vale entre operações.

| Tamanho | Apoio | Patente | Usos por operação | Espera | Efeito |
|---|---|---|---|---|---|
| Pequeno | Reconhecimento | Candidato | 2 | 30 s | Kaiju a até 48 blocos do ponto brilham por 20 s (vistos através das paredes) e vão ao chat com distância e vida; sem kaiju nada é gasto |
| Médio | Suprimentos | Oficial | 1 | 2 min | Depois de 3 s, caixa no ponto com 60 balas de rifle, 30 de pistola e 1 resfriador; Regeneração II por 10 s para jogadores, soldados e civis a até 8 blocos |
| Grande | Bombardeio | Oficial Sênior | 1 | 5 min | Área vermelha de 6 blocos marcada no chão por 5 s; depois 8% da vida máxima de cada kaiju dentro (no máximo 150), sem ferir aliados nem quebrar blocos |

Outros: alcance da mira `callRange` 64 blocos, intervalo mínimo entre chamadas `callGapTicks` 20. Rádio de Campo na
bancada (livre): 4 ferro, 4 redstone, 2 cobre.

## PM-11A: armas numeradas do jogador, Numbers 1, 2 e 6 (2026-10-24) [SUPOSIÇÃO todos os números]

Registro `numbered_gear/<id>.json` (sincronizado). Para usar (golpe e especial) é preciso a patente, a pesquisa
"Teoria das Armas Numeradas" e o limite pessoal de Release mínimo; o criativo passa. Fabricadas na bancada com a mesma
pesquisa (material de núcleo).

| Arma | Patente | Limite mínimo | Dano base | Alcance | Especial (R) | Calor extra do especial (sem domínio) | Outro |
|---|---|---|---|---|---|---|---|
| Numbers 1 (Baioneta) | Vice-Capitão | 30% | 8 | 3,6 | Estocada Prevista: avanço de 6 blocos, x2,0, recarga 7 s | +6 | Esquiva perfeita +3 ticks |
| Numbers 2 (Manoplas) | Líder de Pelotão | 25% | 8 | 3,0 | Force Blast: onda de 4,5 blocos, x2,4, recarga 11 s | +8 | — |
| Numbers 6 (Rifle de Gelo) | Líder de Pelotão | 20% | 6 | 48 | Tiro Congelante: atravessa, x2,0, Lentidão III por 4 s, recarga 10 s | +6 | — |

**Domínio:** XP = dano real causado em kaiju com a arma na mão (`xp_per_damage` 1,0); níveis em 0/300/900/2.000/4.000
XP. O dano vai de 70% (`effect_start` 0,7) no nível 0 a 100% no nível 4 (linear pelo progresso); o calor extra do
especial cai até 0 no domínio total; o bônus de esquiva do Numbers 1 cresce do mesmo jeito (0 a 3 ticks).
O fator do domínio entra depois do da pesquisa e antes do teto de golpe.
Numbers 4 (asas/machado) e Numbers 10 (traje com a cauda) ficam para a PM-11B.

## PM-11B: Numbers 4 (machado com asas) e Numbers 10 (traje com cauda) do jogador (2026-10-24) [SUPOSIÇÃO todos os números]

Mesmo sistema da PM-11A (requisitos e domínio no `numbered_gear/<id>.json`). O domínio vale também para o traje: todo
dano em kaiju com ele vestido dá XP.

| Equipamento | Patente | Limite mínimo | Níveis de domínio (XP) | Base |
|---|---|---|---|---|
| Numbers 4 (Machado com Asas) | Vice-Capitão | 30% | 0/300/900/2.000/4.000 | Dano 12 (o machado comum tem 11), alcance 3,2; especial Mergulho do Céu: onda de 5 blocos, x2,8, recarga 10 s; calor extra +8 sem domínio |
| Numbers 10 (Traje com Cauda) | Vice-Capitão | 40% | 0/500/1.500/3.500/7.000 | Armadura 18, resistência 3, calor -35%, aura do Hoshina No. 10 |

**Asas (Numbers 4 na mão):** Shift no ar caindo abre as asas: impulso para onde o jogador olha de 0,7 bloco/tick ×
domínio (0,49 no nível 0) e queda lenta (a Queda Lenta do jogo, renovada a cada tick, sem ícone) enquanto o Shift
estiver apertado; 6 de stamina por segundo (sem stamina as asas fecham até pousar). Sem dano de queda planando.

**Cauda (Numbers 10 vestido):** a cada 40 ticks corta sozinha um kaiju a até 4,5 blocos da caixa do jogador (primeiro
quem mira o jogador, depois quem está atrás/do lado, depois o mais perto): 6 × domínio × (1 + Release/100), pelo
caminho normal do golpe do jogador (teto de golpe, pesquisa, domínio da arma na mão). **Guarda:** golpe pesado de kaiju
(`[combat] tailGuardBlocksHeavy`) ou projétil vindo de fora dos 120° da frente leva de 49% (nível 0) a 70% (domínio
total) a menos, uma vez a cada 40 ticks. Medido em jogo (nível 0, Release 0): ~2 de vida por corte num Primigenius.

## T-1: larva rara e infecção (2026-10-24) [SUPOSIÇÃO todos os números]

Config `[larva]` (`kn8-server.toml`). Miguel 2026-10-24: "bem rara, no meio de restos de kaiju ou obras", um hospedeiro
por mundo.

| Chave | Valor | Efeito |
|---|---|---|
| `enabled` | true | Liga o sorteio da larva |
| `checkTicks` | 600 | Intervalo do sorteio por jogador (30 s) |
| `chance` | 0,004 | Chance a cada sorteio no lugar certo (~1 vez a cada 2 h dentro dos restos/locais) |
| `carcassRadius` | 24 | Carcaça de kaiju a até este raio conta como "restos" |
| `spawnDistance` | 10 | Distância em que a larva aparece |
| `circleTicks` | 100 | Tempo circulando antes do mergulho |
| `speed` | 0,35 | Blocos por tick da larva |
| `infectRadius` | 1,2 | Distância do peito em que infecta |
| `lifeTicks` | 2400 | Larva vai embora depois disso sem infectar |
| `loseDistance` | 48 | Jogador mais longe que isso: a larva vai embora |

## T-2: forma do Kaiju No. 8 do jogador (2026-10-24) [SUPOSIÇÃO todos os números]

Config `[kaijuForm]`. Miguel 2026-10-24: o Release não muda a forma; só os atributos do corpo treinados (nível 0-100).

| Chave | Valor | Efeito |
|---|---|---|
| `cooldownTicks` | 200 | Espera depois de voltar a humano |
| `height` | 2,0 | Altura da forma (escala 1,11 sobre o jogador) |
| `healthBonus` / `healthPerResistance` | 180 / 2,0 | Vida: 20 + 180 = 200; com resistência 100, 400 |
| `armor` / `armorPerResistance` / `toughness` | 16 / 0,04 / 8 | Armadura extra (até +20) e dureza |
| `speedBonus` / `speedPerLevel` | 0,3 / 0,003 | Velocidade +30% (até +60% com velocidade 100) |
| `jumpBonus` / `jumpPerAgility` | 0,2 / 0,002 | Salto (até +0,4) |
| `knockbackResistance` | 0,8 | Quase não é empurrado |
| `fallDamageMultiplier` | 0,2 | Leva 20% do dano de queda |
| `stepHeightBonus` | 0,5 | Sobe degraus de 1 bloco andando |
| `auraRelease` | 60 | Força com que a aura do No. 8 é desenhada |

## T-3: combate, tempo limite e descontrole da forma (2026-10-29) [SUPOSIÇÃO todos os números]

Golpes = `weapon/kaiju_no8_fist.json` (o mesmo punho do No. 8 aliado: 11 de dano, alcance 3, leve x1,0 / pesado x1,8,
combo 1,0/1,1/1,3) x `damageMultiplier` x força treinada (`[body] strengthDamagePerLevel`); o Release não entra. O teto
por golpe em kaiju (12% leve / 15% pesado) continua. Especial na tecla R: Golpe no Chão (`ground_slam`, x2,2, raio 5,
recarga 200 ticks, 30 de stamina).

| Chave `[kaijuForm]` | Valor | Efeito |
|---|---|---|
| `damageMultiplier` | 5,0 | Soco leve 55 (x força treinada) |
| `regenPerSecond` / `regenBelow` | 0,015 / 0,5 | Regenera 1,5%/s abaixo de 50% da vida |
| `limitTicks` | 2400 | 2 minutos na forma (Miguel) |
| `warnTicks` | 400 | Aviso vermelho nos últimos 20 s |
| `rampageTicks` | 300 | 15 s sem controle |
| `rampageRadius` / `rampageSpeed` | 16 / 0,45 | Ataca quem estiver a 16 blocos, correndo a 0,45 bloco/tick |
| `rampageCooldownTicks` | 2400 | Espera de 2 min depois do surto (em vez de 10 s) |
| `exhaustTicks` | 400 | Lentidão II e Fraqueza II por 20 s depois do surto |
| `detectRadius` | 64 | Detecção de kaiju na barra de ação |

Medido em jogo: Primigenius 220 → 124 com o combo e → 23 com o Golpe no Chão (Dev sem força treinada).
