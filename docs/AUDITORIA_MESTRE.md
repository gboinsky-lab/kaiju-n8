# Auditoria mestre (Fase 1) — resposta ao documento mestre de 2026-11-11

Documento do Miguel: `docs/DOCUMENTO_MESTRE_MELHORIAS.md`. Esta auditoria foi feita lendo o codigo, os dados e os
recursos e conferindo em jogo (servidor dedicado + 2 clientes, 2026-11-11) as armas dos 11 personagens citados e as
6 armaduras vestidas no jogador. Nenhum arquivo do jogo foi mudado nesta fase.

Niveis: **1** inexistente, **2** incompleta, **3** funcional mas precisa melhorar, **4** completa e funcional.
"Visto em jogo" = conferido nesta auditoria; o resto foi lido no codigo/dados (e, quando citado, testado em etapas
anteriores com o registro no `CLAUDE.md`).

## 0. Ponto que muda o plano: Epic Fight

**O mod nao usa Epic Fight.** Nao ha dependencia no `build.gradle`, nenhuma classe, nenhum dado. O combate e
proprio: `CombatService` no servidor (ActionTimeline, dano no tick do JSON, raycast, combos, carga, bloqueio,
aparar, esquiva perfeita, tecnicas por arma com X/R), animacao do jogador pela Player Animation Library (PAL) e das
entidades pela GeckoLib. Isso foi uma decisao antiga do projeto (regras 1 e 6 do `CLAUDE.md`: servidor decide, dano
no tick do JSON).

Consequencia: tudo do documento que fala em "integracao com Epic Fight" precisa de uma decisao sua (pergunta 1 no
fim). Minha recomendacao e **nao** trocar o combate pelo Epic Fight: ele substitui o modelo do jogador, a camera, as
animacoes e o calculo de dano, e entraria em conflito com quase tudo que ja funciona (tecnicas, Release, parkour,
forma do No. 8, trajes 3D). Da para melhorar o combate atual em todos os pontos do documento (impacto, reacoes,
cancelamentos, finalizacoes) sem ele.

## 1. Inventario (numeros reais do projeto)

| Conteudo | Quantidade |
|---|---|
| Classes Java | 460 |
| Arquivos de recurso | 2.434 |
| Kaiju (`kaiju/*.json`) | 42 especies, todas com modelo, textura e animacoes |
| Soldados especiais (`special_soldier`) | 26 perfis (25 combatentes + Konomi) + soldado comum |
| Habilidades de kaiju (`ability`) | 152 |
| Armas (`weapon`) | 23, em 7 perfis de manuseio (`weapon_profile`) |
| Trajes (`suit`) | 6 (traje de treino, Mk1, Mk1 Reforcado, Numbers 4, 6 e 10) |
| Modelos de entidade (`geo` + `animations`) | 64 + 64 (nenhum sem animacao, nenhum sem textura) |
| Malhas (`meshes`) | 64 de entidade + 7 de traje/acessorio |
| Rig do Meshy (`tools/art/skins/*.npz`) | 40 humanoides; os nao humanoides usam o rig por forma (por projeto) |
| Animacoes do jogador | 137 de combate + 12 de parkour |
| Sons | 84 eventos, 172 `.ogg`, 27 perfis de som |
| Efeitos (`vfx_profile`) | 71 perfis |
| Missoes / invasoes / pesquisas / modulos de base | 27 / 15 / 6 / 5 |
| GameTests | 368 (passando na ultima etapa) |

## 2. Estado por sistema

| Sistema | Nivel | O que ha | O que falta ou esta errado |
|---|---|---|---|
| **Armaduras no jogador** (doc §15) | **2** | malha presa as partes do jogador, cauda, asas, manoplas, 1a pessoa | **visto em jogo: 4 de 6 aparecem "estilhacadas"** (causa na secao 3); sem capacete/cabeca em nenhuma |
| Armas na mao dos personagens (doc §4) | 3 | uma pegada por familia de bracos, tamanho real por tabela | **visto em jogo: os 11 problemas que voce listou existem** (secao 4) |
| Manuseio das armas do jogador | 3 | 7 perfis com postura/saque/guarda/recarga | foice, bola da Jura, Pile Driver e katana usam o perfil da espada; arco, canhao, minigun, espingarda e rifle da Camilly so tem "atirar" (sem postura, saque, recarga propria) |
| Combate do jogador | 3 | leve, combo, pesado carregado, especial, tecnicas por arma, bloqueio, aparar, esquiva perfeita, dash, tiro preciso | sem ataque aereo nem em movimento proprios, sem finalizacao, sem "hit-stop" (congelar 2-3 quadros no acerto), cancelamento so pelo fade |
| Reacoes fisicas (doc §8) | 2 | atordoamento de kaiju, tremor de camera, empurrao so nos especiais, ondas desenhadas | uma unica animacao de dano por entidade; sem reacao por direcao (costas/cabeca), sem cambaleio/queda de soldado, empurrao dos golpes comuns desligado |
| Animacoes de personagens (doc §6) | 3 | parado, andar, correr, golpes e tecnicas por personagem, esquiva, salto de obstaculo | **nenhuma animacao de morte** (tomba pelo vanilla); sem pulo/queda/aterrissagem proprios dos NPC; uma so de dano |
| Animacoes do jogador | 3 | combate (PAL) e parkour | andar/correr/pulo/queda/dano sao os do vanilla, menos as posturas das 5 familias |
| Animacoes de kaiju (doc §6, §13) | 3 | parado, andar, golpes por habilidade, respiracao, dano | **nenhuma animacao de morte** (a carcaca tomba por codigo), **nenhuma corrida** (o andar acelera), rugido em poucos, Primigenius/ressurgido so com 4 golpes |
| Rigs (doc §5) | 3 | 40 humanoides com rig do Meshy (cotovelo, joelho, pe, peito), nao humanoides por forma, Neural com tentaculos | conferir deformacao nas juntas em todos (ferramentas existem: `rig_audit.py`, `preview_rig.py`); No. 2, No. 6, No. 14 sem rig do Meshy |
| Modelos (doc §16) | 3 | todos carregam com textura | auditoria de geometria (faces invertidas, pedacos soltos) ainda nao feita em lote; modelos provisorios: Narumi No. 1, Isao sem o Numbers 2, civil, oficial |
| Habilidades e tecnicas | 4 | 152 habilidades, 59+ tecnicas com animacao, efeito e som (auditoria COMBATE-8: 0 lacunas) | preservar; melhorar impacto |
| Sons (doc §7.4, §17) | 3 | 84 eventos sintetizados, sirene, sons por tecnica | espingarda e minigun usam o tiro do rifle; arco usa o tiro do Narumi; foice, bola e Pile Driver usam o machado; sem recarga, saque, erro, bloqueio por arma, passos/aterrissagem com traje, ativacao do Release; kaiju com voz pelo tamanho, nao por especie |
| Efeitos visuais (doc §7.5) | 4 | tudo desenhado, por personagem, pelo tamanho do corpo | faltam rastro de arma continuo, efeito de aterrissagem pesada e de ativacao do traje |
| Ferimentos visuais de kaiju (doc §9) | 2 | ferimento por parte (lento, sem habilidade, "FERIDO" na barra), nucleo exposto | sem mudanca na textura/modelo (marcas, carapaca rachada) e sem perda de partes |
| IA de kaiju e NPCs (doc §10) | 4 | IA-1 a IA-7, MOV-1 a 7, PM-3/4/6 (habitos, grupos, esquadroes, cobertura, saltos) | preservar; poucas lacunas (secao 6) |
| Release das armaduras (doc §11) | 4 | %, limite pessoal, calor, aura por estado, forca/velocidade, Full Release, tecnicas por estagio | falta efeito/som de ativacao ao ligar o traje |
| Destruicao (doc §12) | 3 | por forca/categoria, golpes quebram, rachaduras, pedacos desenhados, restauracao | sem crateras permanentes nem desabamento de estrutura sem apoio |
| Cinematicas (doc §14) | 1 | — | nada (so as trocas de forma com efeito) |
| Otimizacao (doc §18) | 3 | teto de efeitos, nivel de qualidade, IA espacada longe do jogador | sem LOD de malha nem animacao parada fora da tela |
| Parkour / treino / progressao / missoes / base | 4 | FIX-3, TREINO, EXAME, carreira, PM-7/8 | preservar |

## 3. Armaduras no jogador: a causa real (visto em jogo)

Fotos: `docs/img/auditoria_mestre_trajes.jpg` (frente em cima, costas embaixo; ordem: Mk1, Mk1 Reforcado,
Numbers 10, Numbers 4, Numbers 6, traje de treino).

- **Mk1:** bom.
- **Mk1 Reforcado, Numbers 4, Numbers 6:** placas brancas/azuis saindo em pontas de uma caixa preta; parecem
  "estilhacadas".
- **Numbers 10:** volume enorme e disforme, pernas fundidas numa massa, maos somem; a cauda esta boa.
- **Traje de treino:** armadura vanilla (jaqueta), so o peito; sem malha 3D.
- **Todos:** nenhuma cobre a cabeca (a skin aparece). Em 1a pessoa o braco do traje aparece em todas as de malha.

**Causa** (em `tools/art/rig_suit_mesh.py`, nao no jogo):
1. O encaixe `fit_box` alarga cada fatia da malha so ate o tamanho da caixa do boneco (8x12x4 px no tronco). A
   "segunda pele" preta e essa mesma caixa **0,3 px maior**. Resultado: a superficie do traje fica por baixo da
   segunda pele, e so as pontas das placas atravessam. O Mk1 escapa porque e liso e justo.
2. Cada fatia de 2-3 cm de altura tem a propria escala: um triangulo com cantos em fatias diferentes e esticado de
   um jeito em cada canto e vira uma lasca.
3. Nas armaduras grandes (Numbers 10) o limite de esticamento 3,2 x incha as placas e junta as pernas.

**Feito na Fase 2-A (ver secao 9).** Correcao proposta (sem trocar nenhuma armadura): alvo do encaixe 1 px por fora da segunda pele; escala
interpolada por vertice pela altura, em vez de por fatia; esticamento limitado por parte; conferir as 6 de frente,
costas, andando, agachado, em combate e em 1a pessoa. A malha original do Meshy nao muda (so a conversao).

## 4. Armas dos 11 personagens (visto em jogo)

Fotos: `docs/img/auditoria_mestre_armas.jpg`, frente e 3/4 de cada um.

| Personagem | Arma | O que se ve | Causa provavel |
|---|---|---|---|
| Miguel | foice | segura em pe, a lamina grande no alto e para tras | display/pegada da foice herdados da lanca (familia `spear`); a foice precisa da propria rotacao |
| Kota | espingarda | maior que o corpo (1,6 m) | tamanho da tabela de 2026-11-05 |
| Iharu | espingarda | igual a do Kota | mesma arma |
| Aoi | Pile Driver | bloco na frente do peito cobrindo o tronco | pegada da espada; o Pile Driver e de antebraco |
| Haruichi | arco | segurado como rifle, o arco na vertical e a corda ate o chao | perfil do rifle; precisa de pegada de arco (mao esquerda no meio) |
| Rin | minigun | apontando para o chao, grande | pegada do rifle num item de 1,3 m |
| Jura | bola de ferro | a bola (~1,5 m) cobre o corpo a frente | proporcao do modelo com 2,4 m de comprimento total |
| Soichiro | katana | em pe na frente do peito; de lado vai para tras do ombro | pegada da familia `katana` |
| Narumi | baioneta | quase nao aparece de frente; de lado sai para tras | pegada da familia `spear` |
| Kikoru | machado | atravessado a frente, mao perto da cabeca do machado | pegada do jogador (43% do cabo) num machado de 2,5 m |
| Mina | canhao | no quadril, apontando para baixo/frente | o canhao fica no quadril pela obra; a pegada esta virada |

Plano: uma configuracao por arma (display do item) **e** por personagem (pose dos bracos e posicao do item nas
animacoes `arms`/`action` dele), conferida em jogo parado, andando, correndo e golpeando, e em 1a/3a pessoa quando
o jogador pode usar a arma.

## 5. Outros problemas achados (nao estavam no documento)

1. **Nenhuma entidade tem animacao de morte** (64 modelos). Kaiju tomba por codigo e vira carcaca; soldados tombam
   como mob vanilla.
2. **Nenhum kaiju tem animacao de corrida**: a de andar acelera (`Locomotion`), o que fica estranho nos grandes.
3. **Uma so animacao de dano** por entidade, sem direcao.
4. **Armas sem som proprio:** espingarda, minigun e rifle da Camilly (tiro do rifle), arco (tiro do Narumi),
   foice/bola/Pile Driver (machado).
5. **Armas sem manuseio proprio:** foice, bola, Pile Driver e katana com o perfil da espada; arco, canhao, minigun,
   espingarda e rifle da Camilly sem postura, saque e recarga.
6. **Trajes sem capacete** (a skin do jogador aparece em todos).
7. **Sem "hit-stop"** (o pequeno congelamento no acerto que da peso ao golpe em jogos de acao).
8. **Golpes comuns sem empurrao** em quem leva (decisao antiga: `soldierKnockback` 0, para o kaiju chegar ao grupo).

## 6. Plano por fases

Ordem pensada para o que voce marcou como prioridade (armaduras e armas) vir primeiro. Cada etapa: implementar,
GameTests, teste em jogo com servidor dedicado + 2 clientes, fotos, docs, commit, sua aprovacao.

### Fase 2 — correcoes visuais e tecnicas
- **2-A Armaduras** (prioridade maxima): correcao do encaixe (secao 3) nas 6; capacete opcional se voce quiser
  (pergunta 3). Arquivos: `tools/art/rig_suit_mesh.py`, `meshes/suit/*`, `client/render/suit/*`.
- **2-B Armas dos 11 personagens**: secao 4. Arquivos: `tools/art/meshy_assets.json`, `models/item/*.json`,
  `tools/art/gen_special_animations.py`, `humanoid_anims.py`, animacoes das especies, `SoldierRenderer`.
- **2-C Auditoria de geometria em lote**: script que procura faces invertidas, pedacos soltos e buracos em todas as
  malhas e na deformacao das juntas (`rig_audit.py` ampliado); corrigir o que aparecer.

### Fase 3 — jogador e combate
- **3-A Manuseio proprio**: perfis novos de foice, bola, Pile Driver, katana, arco, canhao, minigun e espingarda
  (postura, saque, guarda, recarga), sem tirar os atuais.
- **3-B Impacto**: hit-stop, empurrao proporcional configuravel, reacao por direcao (costas/cabeca), cambaleio.
- **3-C Ataques que faltam**: aereo e em movimento por familia; finalizacao em kaiju atordoado/nucleo exposto.
- **3-D Sons do jogador**: tiros proprios (espingarda, minigun, arco), golpes proprios (foice, bola, Pile Driver),
  recarga, saque, erro, bloqueio, passos e aterrissagem com traje, ativacao do Release.
- **3-E Animacoes do jogador**: pulo, queda, aterrissagem e dano com traje (PAL).

### Fase 4 — kaiju e NPCs
- **4-A Morte**: animacao de morte por familia de corpo antes da carcaca (humanoide, quadrupede, aranha, voador,
  monolito) e dos soldados.
- **4-B Corrida dos kaiju** e rugido para os que nao tem.
- **4-C Reacoes**: dano por direcao e cambaleio dos grandes.
- **4-D Ferimentos visuais**: textura de dano por estagio de vida (rachaduras, sangue/fluido na cor da especie) e
  nucleo exposto visivel.
- **4-E Voz por especie** para os numerados.

### Fase 5 — destruicao e efeitos
Crateras e desabamento, rastro continuo de arma, efeito de ativacao do traje e de aterrissagem pesada.

### Fase 6 — gameplay e otimizacao
LOD (malha simplificada longe e animacao parada fora da tela), revisao de parkour/treino/progressao.

### Fase 7 — regressao
GameTests, matriz de duelos, invasao grande com 2 clientes, desempenho, relatorio final.

### Cinematicas (doc §14)
Fora das fases por enquanto: entradas de chefe e finalizacoes com camera mexem no jogo de todos os jogadores do
servidor; proponho depois da Fase 4.

## 7. Respostas do Miguel (2026-11-11)

1. **Epic Fight:** mantem o combate atual e melhora.
2. **Tamanhos:** a espingarda (Kota e Iharu) so esta grossa demais: afinar, sem mudar o comprimento. A minigun da Rin
   e a bola da Jura: tamanho a criterio do Claude.
3. **Cabeca:** cada armadura cobre o que o modelo dela cobre (ex.: a da Kikoru, Numbers 4, so a boca). Nada de
   capacete inventado.
4. **Mina:** atira ajoelhada, segurando o canhao na frente e atras.
5. **Morte dos kaiju:** animacao de morte antes de virar carcaca.

**Itens da secao 5 (fora do documento):** encaixados nas fases (o Miguel deixou a criterio):

| Item | Fase |
|---|---|
| Animacao de morte (64 modelos) | 4-A (kaiju) e 3-E (soldados/especiais) |
| Corrida dos kaiju | 4-B |
| Dano por direcao | 3-B (impacto) e 4-C (reacoes) |
| Sons proprios de espingarda, minigun, arco, foice, bola, Pile Driver | 3-D |
| Manuseio proprio (postura, saque, recarga) | 3-A |
| Capacete | resolvido pela resposta 3 (o que o modelo cobre) |
| Hit-stop | 3-B |
| Empurrao dos golpes comuns | 3-B (configuravel, sem tirar a decisao antiga do soldado) |

## 8. Perguntas que foram feitas ao Miguel

1. **Epic Fight:** manter o combate atual e melhorar (recomendado) ou integrar o Epic Fight?
2. **Tamanhos:** a tabela de 2026-11-05 deu 1,6 m para a espingarda e 2,4 m para a bola da Jura. Qual tamanho voce
   quer para a espingarda (Kota/Iharu), a minigun da Rin e a bola?
3. **Capacete:** os trajes devem cobrir a cabeca (os modelos do Meshy nao tem capacete; eu faria uma gola/viseira
   a partir da malha) ou a cabeca do jogador fica a mostra como na obra?
4. **Mina:** o canhao fica no quadril (como na obra) ou no ombro/apoiado no braco?
5. **Morte dos kaiju:** animacao antes de virar carcaca, ou a carcaca continua tombando por codigo e so ganha um
   efeito melhor?

## 9. Fase 2-A feita: armaduras (2026-11-11)

Causa real e correcao, tudo na conversao (`tools/art/rig_suit_mesh.py`); nenhum modelo original, textura, item ou
codigo Java mudou:

1. **Casca por fora da segunda pele** (`push_out`): cada vertice anda para fora a partir do eixo da parte ate a
   camada de dentro de cada fatia passar a segunda pele em `SHELL_PX` 0,4 px, sem inflar as placas (ordem das
   camadas mantida; miolo perto do eixo fica, com passagem suave). Superficie escondida: Mk1 Reforcado 33-41% ->
   6-22%, Numbers 6 30-56% -> 19-30% (o resto e o lado de dentro das placas e o boneco de exposicao do Meshy).
2. **Escala interpolada pela altura de cada vertice** no `fit_box` (antes uma por fatia: triangulo com cantos em
   fatias vizinhas virava lasca).
3. **Pecas soltas andam inteiras** (`rigid_pieces`): placa ou bloco menor que 8 px vai para onde o ajuste mandou e so
   muda de escala em X/Z, igual na peca toda (o ajuste por altura entortava os blocos do Numbers 10 e do Numbers 4).
4. **Sem o fechamento de buracos** nas armaduras com segunda pele (o contorno aberto de cada placa virava um leque
   de uma cor so: os triangulos escuros enormes do peito do Numbers 10).
5. **Esticamento 3,2 -> 1,5** nos tres Numbers (era o que inchava o Numbers 10; a casca agora resolve a segunda
   pele), com a gola/mascara do Numbers 4 em 3,2 (`HEAD_STRETCH`).

Visto em jogo com servidor dedicado + 2 clientes (`docs/img/fase2a_armaduras_antes_depois.jpg`: em cima antes, em
baixo depois): as cinco de frente, costas e lado, andando, agachado, com a espada e em 1a pessoa. Nao conferido: golpe a golpe e com
skin de braco fino (Alex).

## 10. Fase 2-B feita: armas dos 11 personagens (2026-11-11)

Causa geral: as posturas de braco sao por familia (rifle, spear, katana, flail...) e cada arma usa o display do
item; armas de formato proprio herdavam a pegada de outra. Correcao por personagem em `tools/art/weapon_grips.py`
(bracos e giro do item nas posturas parado/andando/correndo/combate, escolhidos na previa do `rig_audit.py`):

| Personagem | Antes | Agora |
|---|---|---|
| Miguel | lamina grande para cima | modelo girado 180 graus no comprimento (`flip_up`): lamina para baixo |
| Kota, Iharu | espingarda grossa | `thickness` 0,6, mesmo comprimento |
| Aoi | Pile Driver atravessado no peito | na horizontal a frente do peito, estaca para a frente |
| Haruichi | arco segurado como rifle | arco em pe pelo meio, braco estendido (punho a 0,3 px do cabo) |
| Rin | minigun apontando para o chao | no quadril, cano na horizontal |
| Jura | bola de ~1,5 m cobrindo o corpo | 1,2 m na mao (bola ~0,8 m), pendurada pela corda ao lado |
| Soichiro | katana cruzando o rosto | para cima e para a frente, ao lado |
| Narumi | baioneta para tras/atravessada | baixa, duas maos, para a frente |
| Kikoru (e No. 4) | machado na horizontal, mao na ponta | guarda baixa (cabeca perto do chao a frente), maos no meio do cabo |
| Mina | canhao inclinado | na horizontal no quadril (frente e tras); ajoelhada nas tecnicas de tiro |

Nao conferido em jogo: andando, correndo, golpeando e as armas na mao do jogador (o manuseio do jogador com arco,
minigun, foice, bola e Pile Driver e a Fase 3-A). As tecnicas continuam com as poses de antes durante o golpe.
