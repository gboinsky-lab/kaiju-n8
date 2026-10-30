# Plano 1.0.0 — depois do Plano Mestre (2026-10-24)

O Plano Mestre terminou na PM-12. Este plano junta o que ainda falta para a versão **1.0.0** do mod, em etapas, uma
por vez, cada uma esperando o teste e a aprovação do Miguel (regras do CLAUDE.md). Ordem combinada com o Miguel
(2026-10-24: "ok pode seguir então"): **transformação do jogador** primeiro, depois **onde cada um nasce e o
balanceamento final**, por último o **endurecimento e a versão 1.0.0**.

## Etapas

| Etapa | Conteúdo | Depende de |
|---|---|---|
| **T-0** | Auditoria da transformação do jogador (esta etapa: o que existe, o que falta, decisões) | — |
| **T-1** | Larva rara e infecção: a larva aparece no mundo, encosta no jogador e o infecta (aviso, estado salvo, passa pela morte) | decisões 1 e 6 |
| **T-2** | A transformação: tecla, estado decidido no servidor, o jogador desenhado como o Kaiju No. 8 (modelo do Miguel) para todos os clientes, caixa, vida, armadura, velocidade, aura azul, volta à forma humana, braços em 1ª pessoa | T-1, decisões 2 e 5 |
| **T-3** | Combate na forma No. 8: socos de corpo inteiro, Heavy Punch, Ground Smash, Roar, dash, regeneração, detecção de kaiju na HUD; dano no servidor pelo JSON; limite de tempo/cansaço | T-2, decisões 2, 4 e 5 |
| **T-4** | Consequências: reação da Força de Defesa ao jogador transformado, missões e carreira | T-3, decisão 3 |
| **B-1** | Onde cada kaiju e personagem aparece: kaiju da 0.7/0.8, formas do No. 9, Hoshina No. 10 e os outros especiais nas invasões, missões e locais da obra | — |
| **B-2** | Balanceamento final: matriz de duelos com todos, jogador por patente contra cada kaiju, armas numeradas, apoio militar | B-1 |
| **R-1** | Endurecimento: GameTests instáveis (`invasionRunsWavesAndBreaks`, `yojuBreaksAStoneBrickWallWhileWalking`), desempenho com vários jogadores, saves antigos, JSON inválido, tabela de status do CLAUDE.md | — |
| **R-2** | Versão 1.0.0: número da versão, lista de mudanças, roteiro de teste geral e o `.jar` final | tudo |

Fora deste plano, esperando o Miguel: os sons (`docs/audio/PROMPTS_SUBSTITUICAO.md`), os tamanhos e tempos dos efeitos
e os modelos provisórios (asas e cauda do jogador, Narumi No. 1, Isao sem o Numbers 2, civil, Oficial de Missões).

---

## T-0 — Auditoria da transformação do jogador

### O que já foi decidido

- **Miguel, 2026-10-08** (`docs/AUDITORIA_BIBLIOTECA_V21.md`, decisão 5): o Kafka é um aliado com duas formas, cada
  uma com spawn próprio, e **o jogador também pode virar o Kaiju No. 8, mas só depois de achar o inseto (larva)**: ele
  é raro de encontrar, infecta o corpo do jogador e o jogador recebe um aviso de que foi infectado.
- **Fase 4 / CLAUDE.md:** transformação pelo **método D** (fantoche `GeoReplacedEntity`): o jogador continua sendo
  jogador no servidor (inventário, carreira, conexão), e o cliente o desenha com o modelo GeckoLib do No. 8.
- **Biblioteca v22, seção 2 (Kafka / No. 8):** forma Kaiju com Super Strength, Super Speed, Regeneration, Kaiju
  Detection, Roar, Ground Smash, Heavy Punch, Grab/Throw; o soco envolve quadril, tronco, ombro, braço e punho, nunca
  só o braço.

### O que já existe e serve

| Peça | Onde | Serve para |
|---|---|---|
| Modelo do Kaiju No. 8 do Miguel (2 m) | `meshes/kaiju_no8*`, `geo/entity/kaiju_no8.geo.json`, `textures/entity/kaiju_no8.png` | O corpo do jogador transformado (malha presa aos ossos) |
| Animações do No. 8 | `animations/entity/kaiju_no8.animation.json`: andar, correr, parado, guarda e postura dos punhos, soco, golpe pesado, investida, golpe no chão, rugido, aparar, contra-ataque, ferido, salto | As mesmas animações no jogador transformado (corpo inteiro) |
| No. 8 aliado (NPC) | `KaijuNo8Entity`, `special_soldier/kaiju_no8.json` (técnicas, regeneração, guarda contra golpe pesado), `weapon/kaiju_no8_fist.json` (soco 11, alcance 3) | Os números e as técnicas da forma do jogador partem daqui |
| Efeitos do No. 8 | `vfx_profile/kaiju_no8.json` (troca de forma, socos, golpe no chão, rugido, regeneração), aura `kaiju_no8` com raios azuis | Os mesmos efeitos no jogador |
| Troca de forma do Kafka | `HoshinaEntity.tickForm`/`transformInto` (fumaça, explosão sem dano, rugido) | O efeito da transformação |
| Larva | `kaiju_larva` (voadora, 0,5 m, modelo do Miguel, `flyer/kaiju_larva.json`) | A larva que infecta |
| GeckoLib 4.7.5 | `GeoReplacedEntity`, `GeoReplacedEntityRenderer` (conferidos no jar) | Desenhar o jogador como o No. 8 |
| Escala vanilla | atributo `scale` (alturas da tabela mestra) | Caixa do jogador com o tamanho do No. 8 |
| Combate do jogador | `CombatService`, `ActionTimeline`, especiais na tecla R, PAL | Golpes da forma No. 8 com dano no servidor |

### O que falta

1. **Larva no mundo:** hoje ela só nasce por `/summon`; não infecta ninguém.
2. **Estado "infectado" e "transformado"** no jogador (servidor), salvo e sincronizado com todos os clientes (os
   outros jogadores precisam ver o No. 8).
3. **Desenho do jogador como No. 8:** um `GeoReplacedEntityRenderer` com a malha presa aos ossos (hoje a malha é
   desenhada pelo `SoldierRenderer`), as animações tocadas pelo estado do servidor, a câmera de 1ª pessoa com os braços
   do No. 8.
4. **Atributos da forma:** vida, armadura, velocidade, escala, sem armas nem traje enquanto transformado.
5. **Golpes da forma:** soco, Heavy Punch, Ground Smash, Roar, investida, regeneração e detecção de kaiju; o
   Grab/Throw é novo (o NPC não tem).
6. **Volta à forma humana:** por tecla, por tempo ou por cansaço; o que acontece com vida, calor e Release.
7. **Reação do mundo:** a Força de Defesa vê um kaiju; missões e carreira.

### Riscos técnicos

- O PAL (animação do jogador) e a GeckoLib não podem animar o jogador ao mesmo tempo: transformado, a camada de
  combate do PAL fica desligada e quem anima é a GeckoLib.
- Outros mods que mexem no desenho do jogador (skins, Better Combat) podem conflitar com o `GeoReplacedEntity`.
- A caixa do jogador maior (2 m) pode prender em portas e tetos de 2 blocos.

### Decisões para o Miguel

1. **Larva:** onde e com que raridade ela aparece? Proposta: aparece de noite, perto dos locais da obra e dos restos de
   kaiju, no máximo uma por mundo de cada vez, e foge se o jogador demorar; ao encostar, infecta.
2. **Ativação e limite:** tecla própria (proposta: **V**), com tempo máximo na forma e cansaço depois (como a fadiga do
   Release), ou sem limite?
3. **Força de Defesa:** os soldados atacam o jogador transformado (na obra o Kafka esconde o segredo), ignoram, ou só
   estranham (sem atacar)? Proposta: não atacam, mas a transformação na frente de soldados derruba o mérito.
4. **Equipamento:** transformado o jogador só usa os punhos (proposta) ou ainda segura arma?
5. **Força:** quanto mais forte que o jogador comum? Proposta: vida x3 a x5 da do jogador e o dano do punho do No. 8
   do NPC, crescendo com o Release do jogador (o limite pessoal vira a "sincronização" da forma).
6. **Multiplayer:** mais de um jogador pode ser infectado no mesmo mundo, ou só um?

### Decisões do Miguel (2026-10-24)

1. **Larva:** bem rara, aparece no meio de restos de kaiju ou nas obras [SUPOSIÇÃO: "obras" = os locais da obra,
   Shinagawa, Tachikawa e Ariake; corrigir se for outra coisa].
2. **Ativação:** a tecla fica a critério do Claude (**V**); limite de **2 minutos** na forma; passando disso o jogador
   **perde o controle do corpo** e sai atacando todos em volta.
3. **Força de Defesa:** os soldados atacam o jogador transformado; **a partir de certa patente** não atacam mais.
4. **Equipamento:** só os punhos.
5. **Força:** o Release **não** muda o poder da forma kaiju; só os atributos físicos treinados (`BodyStat`) a deixam
   mais forte. A forma já é muito forte por natureza (por isso a larva é muito rara).
6. **Multiplayer:** só um jogador por mundo.

## T-1 — Larva rara e infecção (feita, 2026-10-24)

- `LarvaService` + `KaijuHostData` + attachment `kn8:kaiju_host`; números em `[larva]` (`docs/BALANCEAMENTO.md`).
- A larva sorteia a cada 30 s (0,4%) para quem está em restos de kaiju (construção ou carcaça a até 24 blocos) ou num
  local da obra; circula ~5 s, mergulha no peito e infecta; título, chat, náusea, escuridão e batida de coração.
- Um hospedeiro por mundo; a infecção passa pela morte e fica salva no mundo.
- `/kn8 host status|spawn|infect|clear <jogador>`; roteiro §103; imagem `docs/img/t1_larva_infeccao.jpg`.

## T-2 — Transformação (feita, 2026-10-29)

- Tecla **B** (a V já é o bloqueio): `KaijuFormC2S` só com a intenção; `KaijuFormService` decide (só o hospedeiro,
  vivo, fora da espera de `[kaijuForm] cooldownTicks` depois de voltar).
- Estado público `kn8:kaiju_form` (sync nativo, salvo, não passa pela morte). Atributos transitórios reaplicados a cada
  segundo: vida, armadura, velocidade, salto, empurrão, queda, degrau e altura de 2 m, crescendo só com resistência,
  velocidade e agilidade treinadas; Release em 0 e aura `kaiju_no8` enquanto transformado.
- Desenho (método D): `PuppetNo8`, um Kaiju No. 8 só do cliente que segue o jogador e é desenhado pelo renderer do
  NPC no lugar do jogador (malha, andar/correr, postura dos punhos); em 1ª pessoa, a malha do braço direito do No. 8.
  [DECIDIR aprovado em princípio] fantoche de entidade em vez de um `GeoReplacedEntityRenderer` próprio: reaproveita
  malha, animações e efeitos do No. 8 aliado.
- Fica para a T-3: golpes (só punhos), o limite de 2 minutos e a perda de controle; hoje transformado o jogador ainda
  usa o item da mão no ataque vanilla e não há HUD de tempo.

## T-3 — Combate, limite e descontrole (feita, 2026-10-29)

- **Só punhos:** transformado, `CombatService.heldWeapon` é sempre o punho do No. 8 (`weapon/kaiju_no8_fist.json`),
  qualquer item na mão; combo de socos, soco pesado, carga e o **Golpe no Chão** (especial novo do punho, tecla R).
  Animações tocadas no fantoche (`attack`, `heavy_punch`, `ground_smash`, `dash`); armas/trajes numerados parados e sem
  domínio na forma.
- **Força:** punho x `[kaijuForm] damageMultiplier` 5 x força treinada; Release fora.
- **Regeneração** abaixo de 50% e **detecção de kaiju** + tempo restante na barra de ação, uma vez por segundo.
- **Limite de 2 minutos:** estado público `kn8:kaiju_form` = 2 (sem controle). O servidor escolhe o vivo mais perto
  (jogador fora do criativo/espectador, soldado, civil, kaiju, monstro), corre até ele e soca (`Rampage`); o cliente do
  dono trava a entrada de movimento e vira a câmera para o alvo publicado (`kn8:rampage_target`). 15 s depois volta
  humano, exausto, com 2 min de espera. Soldados e civis aceitam dano só do hospedeiro sem controle.
- Não feito: Roar e Grab/Throw na mão do jogador (só no NPC), arremesso; reação da Força de Defesa = T-4.

## T-4 — Reação da Força de Defesa (feita, 2026-10-30)

- **Soldados atacam o hospedeiro transformado** abaixo da patente `[kaijuForm] trustedRankOrder` (4, Vice-Capitão
  [SUPOSIÇÃO]); a partir dela só quando ele está sem controle (`KaijuFormService.hostileToForce`). Comuns e especiais:
  ele entra na escolha de alvo como um kaiju grande (`SoldierTargetGoal`), os golpes, cortes e tiros o atingem
  (`SoldierEntity.canAttack`, raio do golpe, `SpecialAttacks.isAlly`) e ele pode revidar. Voltou a ser humano ou subiu
  de patente: o soldado larga na hora. O próprio esquadrão também ataca [SUPOSIÇÃO].
- **Carreira:** transformar com soldados olhando (a até `witnessRadius` 32, linha de visão) tira `witnessMeritPenalty`
  100 de mérito, uma vez por transformação; com a patente de confiança, nada.
- **Base e missões:** transformado, oficiais, estação do esquadrão, terminal de pesquisa e o aceite de missões recusam
  (`closesServices`; `MissionService.Result.TRANSFORMED`, `ResearchService.Result.TRANSFORMED`).
- **Mundo:** civis fogem do jogador transformado como de um kaiju; dentro de um local da obra a região entra em alerta
  (sem controle, combate) (`alertsRegion`).
- Números em `docs/BALANCEAMENTO.md`; roteiro §128; imagem `docs/img/t4_reacao_forca_de_defesa.jpg`.

## HOST — Release humano e transformação parcial (feita, 2026-10-30)

- **Release humano** (Miguel: "começa em 0,1 e sobe até 1% com treino"): o hospedeiro em forma humana, de traje,
  fica com o Release travado de `hostReleaseMin` 0,1% a `hostReleaseMax` 1% pela média dos atributos do corpo
  (`PowerService.hostRelease/hostCap`); só o 1% inteiro conta nas regras de número inteiro (dano, stamina, calor),
  desespero incluído. HUD em décimos e ciano (`PowerView.hostCapTenths/hostActiveTenths`, protocolo 40).
- **Transformação parcial** (Shift + B, `KaijuFormC2S.partial`): `kn8:kaiju_form` = 3; o braço direito vira o do
  No. 8 por 5 s (camada `PartialArmLayer` em 3ª pessoa, braço vanilla escondido; 1ª pessoa com a malha do braço),
  golpes com o punho do No. 8 x 3 x força, espera de 20 s, gasta 10 s do tempo-limite da próxima forma inteira (volta
  aos poucos). O corpo continua humano (vida, traje, Release do hospedeiro); a Força de Defesa não ataca, mas a mudança
  vista custa mérito como a forma inteira.
- Roteiro §129; imagem `docs/img/host_release_e_braco.jpg`.
