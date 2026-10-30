# Etapa COMBATE — auditoria e plano (COMBATE-0)

Miguel, 2026-10-29: "agora vamos seguir para a etapa do combate, começando a alterar os estilos de combate".
Referência: `docs/referencias_combate/kaiju_no8_referencias_atualizadas.html` (documento do Miguel) e as imagens
baixadas dele em `docs/referencias_combate/img/` (todas abriram; as 4 primeiras da Kikoru só pelo caminho original
da imagem, a miniatura do documento dava 404).

Regras do documento (valem para toda a etapa):

- **Não apagar nada que já existe.** Técnica genérica renomeada para o nome da obra mantém o id interno quando o
  movimento é o mesmo; as que não correspondem a nada da obra ficam como golpes básicos.
- **Separar sem traje kaiju (forma A) e com traje kaiju / arma numerada (forma B).** Os pares já existem
  (`hoshina`/`hoshina_no10`, `kikoru`/`kikoru_no4`, `reno`/`reno_no6`, `narumi`/`narumi_no1`,
  `isao_base`/`isao`); a forma A nunca ganha ataque de energia da arma numerada.
- **O jogador também usa as técnicas**, não só o NPC (hoje o jogador tem **um** especial por arma, na tecla R).
- Preparação → execução → impacto; o que for inferido para o jogo fica marcado como [INFERÊNCIA].

## 1. Decisões do Miguel (2026-10-29)

| Tema | Decisão |
|---|---|
| Isao: Thunder Emperor | Fica **só para o Isao com o Numbers 2 e para o No. 9 com o Isao**, como **ataque especial**: barragem de socos consecutivos, cada soco forte, com **traços amarelos** (antes: um soco único sem raio, decisão de 2026-10-22) |
| Isao: técnicas novas | Seguir a obra: **Barrage Blast = cotovelada** curta (hoje é anel de ar), **Twin Blast** (os dois punhos) e **Rising Blast** (uppercut) entram; Force Blast continua |
| Kikoru | Técnicas com os **nomes da obra**; a **primeira (Falling Thunder) é um golpe que vem por cima e esmaga o kaiju embaixo** |
| Hoshina No. 10: Twelve-Fold Slash (Juni-hitoe) | **Só no último estágio do Hoshina** (hoje: só em Full Release, a 100%, com o traje numerado 10) |
| QG de Tachikawa (e os outros) | Cada QG segue o da obra, com salas dentro e fora e zonas separadas, **dentro da cidade que representa** (fica para a etapa CIDADES) |
| Hoshina No. 10 parado | Mesmo estilo agachado, mas com a perna dobrada como na figura enviada (um joelho no chão, a outra perna dobrada à frente, tronco inclinado, braços para trás com as lâminas) |
| Posturas (parado, andando, correndo) | Só as pernas dobram de verdade (joelho e pé); braços com arma como estão. Cotovelo e corpo inteiro nos golpes |

## 2. Auditoria: o mod hoje x a obra

| Personagem | Hoje no mod (técnicas do NPC) | Na obra (documento) | Diferença |
|---|---|---|---|
| Kikoru (forma A) | axe_slash, heavy_swing, shockwave, dash_strike, ground_smash, guard_break (nomes genéricos da Biblioteca v22) | Estilo do machado do Esquadrão: 1 Falling Thunder (de cima para baixo, onda de choque para trás acelera), 2 Water Skimmer (horizontal, onda de choque à frente no contato), 3 Half-Moon (giro), 5 Twin Swallows (dois cortes em X com avanço), 6 Daruma Dropper (4 cortes horizontais da cabeça às pernas), Daruma Dropper Ten Hit (10 cortes) | Nomes e trajetórias; nº 4 não aparece no documento |
| Kikoru No. 4 (forma B) | high_speed_axe, dive_strike, vertical_assault, air_combo, aerial_dash | Ten Hit é da luta com asas (cap. 84) | Ten Hit encaixa na forma B [INFERÊNCIA] |
| Hoshina / No. 10 | kuuchi, kosa_uchi, ran_uchi, kasumi_uchi, yae_uchi (+ kaeshi_uchi como contra-ataque); juni_hitoe só no No. 10 | Air Slicer (Kūuchi), Cross Slasher (Kōsa-uchi), Return Slasher (Kaeshi-uchi), Wild Slasher (Ran-uchi), Mist Slasher (Kasumi-uchi), Eightfold (Yae-uchi), 12-Layered (Jūni-hitoe: três lâminas + cauda) | Já batem (nomes japoneses); o Jūni-hitoe usa a cauda sem a terceira espada (decisão de 2026-10-08) |
| Narumi / No. 1 | thrust_combo, lunge, bayonet_shot, sweeping_slash (+ weak_point_thrust, predicted_counter no No. 1) | 1 Eruption Slash (cortes com tiros), 2 Slash Salvo (passa pelo inimigo cortando e atirando), 6 Seven-Branched Sword (sete direções com explosões), Thunder Fire, Flame Shower, Sky Turner (a confirmar) | Nomes e movimentos; quais são da forma A e quais da B: [DECIDIR] |
| Isao (forma B, Numbers 2) | barrage_blast (anel de ar), spiral_blast, force_blast, thunder_emperor (soco único) | Barrage Blast (cotovelada), Force Blast (onda sônica), Twin Blast, Rising Blast (uppercut), Spiral Blast (sem detalhe confirmado); Thunder Emperor é do No. 9 na obra | Ver decisões acima |
| Isao sem o Numbers 2 (forma A) | jab_combo, elbow_strike, heavy_straight | Combate físico sem energia | Já separado |
| Kafka / No. 8 | punch_combo (Kafka); rush_combo, heavy_punch, dash_strike, ground_smash, roar (No. 8) | Soco direto de ruptura (cap. 2), sem técnica nomeada | Só animação e impacto |
| Mina | precision_shot, cannon_shot, charged_cannon, anti_giant_shot | Canhão portátil e railgun; mira, clarão curto, impacto distante | Só animação/efeito |
| Reno / No. 6 | precision_shot, burst_fire, freeze_round, suppression, butt_strike / ice_spear, freeze_burst, ice_field, multi_freeze, auxiliary_cannons | Tiros congelantes, gelo que se espalha pelo corpo em etapas | Propagação do gelo em etapas |
| Jogador | Um especial por arma (tecla R): machado `seismic_cleave`, espada do Hoshina `kuuchi`, faca, espada, armas numeradas 1/2/4/6 | O documento pede as técnicas do personagem para o jogador | Falta o sistema de várias técnicas por arma |

## 3. Plano (uma subetapa por vez; cada uma espera teste e aprovação)

| Subetapa | Conteúdo |
|---|---|
| **COMBATE-1 — técnicas do jogador** | Sistema para o jogador ter várias técnicas por arma/traje (lista no `weapon/<id>.json` ou no `numbered_gear`, forma A/B pelo traje vestido), escolha e uso por tecla, recarga/stamina/calor como o especial de hoje, HUD com a técnica escolhida. O especial atual de cada arma vira a primeira técnica (nada some). Servidor decide tudo (regra 1); sem pacote novo se der, senão protocolo 34 |
| **COMBATE-2 — posturas com as pernas** | Joelho e pé dobrando nas posturas de todos os humanoides; Hoshina No. 10 parado na pose da figura |
| **COMBATE-3 — Kikoru e Kikoru No. 4** | Falling Thunder (salto por cima e esmagamento do kaiju embaixo), Water Skimmer, Half-Moon, Twin Swallows, Daruma Dropper (forma A); Ten Hit (forma B); para NPC e jogador (machado e machado do Numbers 4) |
| **COMBATE-4 — Isao e No. 9 com o Isao** | Barrage Blast cotovelada, Twin Blast, Rising Blast; Thunder Emperor como especial em barragem de socos com traços amarelos (Isao e No. 9 com o Isao); manoplas do Numbers 2 do jogador com as mesmas |
| **COMBATE-5 — Hoshina** | Conferir as 7 formas contra o documento (cortes, X, oito pontas); Jūni-hitoe só no último estágio; espada do Hoshina do jogador com as técnicas |
| **COMBATE-6 — Narumi** | Eruption Slash, Slash Salvo, Seven-Branched Sword (e as outras quando confirmadas); baioneta do jogador e Numbers 1 |
| **COMBATE-7 — Kafka/No. 8, Mina, Reno** | Soco de ruptura do No. 8 (forma do jogador também), mira/clarão/impacto da Mina, gelo do Reno em etapas |
| **COMBATE-8 — regressão** | Duelos da matriz (as técnicas mudam o dano), GameTests, efeitos (VFX) e sons de cada técnica nova |

## 4. Respostas do Miguel (2026-10-29) aos [DECIDIR]

1. **Escolha da técnica:** (a): tecla **X** troca (Shift + X volta uma), **R** usa a escolhida. Feito na COMBATE-1.
2. **Técnicas genéricas da Kikoru:** ficam como golpes básicos, abaixo das da obra.
3. **Técnica nº 4 da Kikoru:** criar uma técnica nova (não está na obra; marcar [INFERÊNCIA]).
4. **Narumi com o Numbers 1:** as técnicas são aprimoradas e **sempre acertam o alvo** (o Numbers 1 prevê o futuro;
   a previsão não muda o combate em si, só garante o acerto).
5. **Spiral Blast do Isao:** um soco que cria uma espiral amarela.
6. **Thunder Emperor (depois da COMBATE-4):** só com a vida baixa, para o jogador e para a NPC (feito: 40%
   [SUPOSIÇÃO]). **Falling Thunder:** altura mínima de 3 blocos aprovada.
7. **COMBATE-5 (Hoshina):** feita. NPC já batia com a obra (Jūni-hitoe só em Full Release, o último estágio); o
   jogador ganhou as 7 formas e o Jūni-hitoe só com o traje Numbers 10 na força total (Miguel: no limite pessoal ou a
   100%, a regra cobre as duas). Prévia de balanceamento das formas feita; refazer na COMBATE-8.
8. **COMBATE-6 (Narumi):** feita. Eruption Slash, Slash Salvo e Seven-Branched Sword nas duas formas (o documento não
   separa forma A/B: as duas usam, o No. 1 aprimorado e com acerto garantido). Thunder Fire, Flame Shower e Sky Turner
   ficaram de fora (o documento pede confirmar antes).

## 5. Perguntas que ficaram (histórico)

1. **Como o jogador escolhe a técnica:** (a) uma tecla troca a técnica e R usa (como munição/seletor); (b) R + número; (c) a técnica muda pelo combo (leve, leve, R = técnica 2...). Proposta: (a), tecla **X** para trocar [SUPOSIÇÃO].
2. **Técnicas genéricas da Kikoru que não existem na obra** (guard_break, dash_strike...): ficam como golpes básicos sem nome da obra, ou saem do NPC? O documento pede não apagar; proposta: ficam, com prioridade menor que as da obra.
3. **Técnica nº 4 da Kikoru:** não está no documento. Deixar de fora até ter referência?
4. **Narumi:** quais técnicas são sem o Numbers 1 (forma A) e quais só com ele (forma B)?
5. **Spiral Blast do Isao:** o documento diz que não há detalhe confirmado. Manter como está, tirar, ou trocar pelo Twin Blast?
