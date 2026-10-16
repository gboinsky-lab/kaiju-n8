# IA — Fase 0: auditoria (2026-10-15)

Primeira fase do `docs/PLANO_TECNICO_IA_INDIVIDUAL_KAIJU_N8_REBORN.md` (Parte 3 e Parte 14, Fase 0). Pedido do
Miguel: "vamos prosseguir com as melhoras das IAs". O plano manda **auditar antes de editar**: nesta fase nenhum
código de IA foi alterado. Saídas: este relatório, o medidor de cenários `tools/ai/cenarios_ia.py` e o plano das
próximas fases (seção 6).

Método:
- Leitura do código de IA, combate e animação em três frentes: kaiju, soldados/especiais, combate/animação/dano.
- Medição de 8 cenários do plano (§15.1) no servidor dedicado (tabela da seção 3).
- Conferência pessoal, no código, dos achados mais graves.

Versões: Minecraft 1.21.1, NeoForge 21.1.252, GeckoLib 4.7.5, PAL 1.1.5, Java 21.

## 1. Inventário

| Quem | Classe / Goals | Arquivo |
|---|---|---|
| Kaiju terrestres (todos) | `KaijuEntity`. Goals: `FloatGoal` 0, `KaijuCombatGoal` 2, passeio 5, olhar 6-7. Alvos: `HurtByTarget` 1 (alerta o bando na aranha), jogador 2 e soldado 3 (`NearestAttackableTarget`, precisa ver) | `common/kaiju/KaijuEntity.java:382-397`, `KaijuCombatGoal.java` |
| Kaiju No. 9 (5 formas) | Herda o kaiju + `No9Service`: reviver, comandar, fugir, regenerar, esquivar de tiro, análise, endurecer, absorver, mudar de forma | `common/numbered/` |
| Kaiju No. 10 (2 formas) | Herda o kaiju + `No10Service`: regenerar, comandar, virar gigante | `common/numbered/` |
| Preondactyl e larva | `PreondactylEntity`: voo próprio (circular → mergulhar → subir) em `customServerAiStep`, sem `KaijuCombatGoal` | `common/kaiju/PreondactylEntity.java:121-219` |
| Chefes | `BossService`: fases, barra, lacaios. **A fase não muda a IA** (o próprio código marca [SUPOSIÇÃO], `BossService.java:164`) | `common/boss/` |
| Soldado comum | `SoldierEntity` + `SoldierCombatGoal` (atirador mantém 12 blocos e recua; corpo a corpo aproxima e bate). Alvo: kaiju mais perto, precisa ver | `common/soldier/` |
| 11 soldados especiais | `HoshinaEntity` (e Kikoru, Mina, Reno, Narumi, versões numeradas, Kafka, No. 8) + `HoshinaCombatGoal`: técnica de maior prioridade pronta e ao alcance, reações lidas do kaiju (esquiva, Kaeshi-uchi, parry), `rangedTick` (Mina/Reno), `unstick`, escalada do Release, voo (Kikoru No. 4), cauda (Hoshina No. 10), troca de forma (Kafka ↔ No. 8) | `common/soldier/special/` |

Fluxo atual (igual para todos, com variações):

1. **Percepção:** goals do vanilla. Alvo mais próximo visto, dentro do *follow range*. Sem memória além dos 60 ticks
   do vanilla e sem posição conhecida.
2. **Decisão:** kaiju = habilidade de maior prioridade pronta e ao alcance (`KaijuAbilities.choose`); especial =
   técnica de maior prioridade (`chooseTechnique`). Não há camada tática nem perfil por personagem; só os números
   das técnicas mudam.
3. **Movimento:** kaiju grande vai em linha reta (`MoveControl`); os outros usam caminho a cada 8-10 ticks.
4. **Ataque e dano:** `ActionTimeline` com impacto no tick do JSON, decidido no servidor (a animação é só visual). Isso
   já segue a Parte 7/8 do plano.
5. **Recuperação:** duração fixa da ação.

## 2. O que já está bom (manter)

- Dano no servidor, no tick de impacto do JSON, uma vez por ação (`ActionTimeline.consumeImpact`). Não há dano por
  animação.
- Investida e corte com conjunto "já acertou" (`chargeHits`, `SlashProjectile.struck`). Explosões em kaiju contam uma
  vez por tick. Parte do kaiju passa o dano para o corpo.
- Escolha de habilidade e técnica por prioridade, alcance, setor e vida, toda vinda do JSON.
- Reação do Hoshina lendo o golpe do kaiju (o que vem e quanto falta para o impacto), e esquiva/parry com números no JSON.
- No. 9 já tem análise (adaptação limitada, com teto) e esquiva de tiro pesado: base para a Fase 6.
- Mina/Reno já recuam, circulam e perseguem; todo especial sai de luta travada (`unstick`).

## 3. Medições (servidor dedicado, `tools/ai/cenarios_ia.py`)

40 s por cenário, amostra a cada 0,25 s.

| Cenário | Quem | Resultado | Leitura |
|---|---|---|---|
| aberto | Hoshina × Primigenius a 18 blocos | vence em 7,6 s, perde 4 de vida | ok |
| alto | Kikoru em cima de um bloco de 6 × Primigenius | vence em 7,8 s | ok (ela desce; o kaiju não precisou subir) |
| corredor (7 de largura) | 3 soldados de espada nível alto × formiga | os 3 morrem em 7,9 s; a formiga fica com 48% | sem cobertura nem recuo dos soldados; ninguém trava |
| grupo | 4 soldados + Hoshina × aranha, Primigenius, formiga | os 3 kaiju morrem em 14,6 s; 2 soldados morrem | todos engajam; sem divisão de alvos (não dá para medir o alvo pelo RCON) |
| solda | 1 soldado de rifle × Primigenius a 12 | o soldado morre em 20 s; o kaiju fica com 76% | recuo do atirador não basta |
| **parede** | 2 atiradores × Primigenius, parede de 6 entre eles | **40 s sem combate**: ninguém vê ninguém, ninguém procura nem contorna | falta de memória e investigação (Parte 5) |
| pilares | Mina × Primigenius Honju a 28 | **o 1º tiro (Anti-Giant) tira 820 de 360**: Honju morto em 3 s | balanceamento, não IA (seção 5) |
| sozinho | formiga entre paredes, sem alvo | parada 40 s | sem alvo e sem jogador por perto, o vanilla desliga até o passeio (normal) |

Cuidados de medição achados nesta fase (anotados no medidor):
- O `/fill` tem limite de 32.768 blocos e falha calado.
- Matar o kaiju de um cenário deixa uma carcaça com colisão no ponto onde o próximo nasce.
- A limpeza em 4 partes não pode pular a coluna do meio.

As três primeiras rodadas saíram erradas por isso (kaiju sufocando em bedrock que sobrou); só a última valeu.

## 4. Bugs e lacunas de IA

Ordenados por gravidade. Cada item foi conferido no código (os de gravidade alta, pessoalmente).

| # | Gravidade | Onde | Problema | Fase |
|---|---|---|---|---|
| 1 | Alta | `soldier/SoldierEntity.java:284-288` | **Mina, Reno, Reno No. 6 e Kafka ficam desarmados**: acabam os pentes, `swapHands()` passa a arma para a mão secundária vazia (eles não têm arma de apoio); a técnica exige arma e eles orbitam o kaiju a 16-22 blocos sem fazer nada. A munição também não é salva (recarregar o chunk enche de novo) | 1 |
| 2 | Alta | `kaiju/KaijuCombatGoal.java:74-84` | **Kaiju parado para sempre embaixo de alvo no alto**: com a distância de borda menor que o alcance mas o alvo acima da faixa vertical, nenhum movimento é pedido e o ataque falha | 1 |
| 3 | Alta | `KaijuCombatGoal`, `SoldierCombatGoal`, `HoshinaCombatGoal` | **Sem memória nem investigação**: perder a visão é perder o alvo (cenário parede: 40 s parados). Não há última posição conhecida, nem procurar ou contornar | 1 |
| 4 | Alta | `KaijuCombatGoal.java:74-82` | **Sem detecção de travamento no kaiju** nem desistência de alvo inalcançável: kaiju grande vai em linha reta e só quebra o que bate; os pequenos recalculam caminho a cada 10 ticks para sempre | 1 |
| 5 | Média | `KaijuEntity.java:655-661`, `695-709`, `742-745`; `HoshinaEntity.java:507-534` | **Golpes sem linha de visão nem ângulo no impacto**: mordida, pisão, investida, varrida da técnica e cauda acertam através de parede e para trás | 2 |
| 6 | Média | `KaijuCombatGoal.java:62-84` | **Kaiju atordoado continua andando**: o stagger corta só o ataque, não para a animação (sem `stopTriggeredAnim`) e não existe animação de stagger | 1/2 |
| 7 | Média | `KaijuProjectile.java:172-175` | **Projétil "heavy" perde o heavy**: o bloqueio comum segura o raio do Honju, o sopro do Diclonius etc.; parry contra projétil atordoa o kaiju à distância | 2 |
| 8 | Média | `KaijuAbilities.java:139-145` | **Rajada de vários tiros soma um só** no mesmo alvo (a invulnerabilidade vanilla engole os outros): Multi-Finger Gun/Cannon mais fracos que o JSON | 2 |
| 9 | Média | `KaijuEntity.java:137`, `601-606` | **Salto fora da timeline**: a aterrissagem pode vir até 80 ticks depois, sobrepondo a próxima habilidade; salto atordoado ainda dá dano | 2 |
| 10 | Média | `KaijuEntity.java:393-396` | **Troca de alvo sem histerese**: qualquer golpe de outro mob toma o alvo (HurtBy 1 > jogador 2 > soldado 3). Em grupo o kaiju oscila | 1 |
| 11 | Média | `No9Service.java:565`, `No10Service.java:87`, `BossService.java:263` | Alvo dado por "comandar" não tem limite de distância nem de visão: o kaiju atravessa o mapa | 1 |
| 12 | Média | soldados (sem código) | **Sem separação entre aliados**: corpo a corpo vão todos ao mesmo ponto; atiradores calculam o mesmo recuo e a mesma órbita (empilham) | 5 |
| 13 | Média | `InvasionService.java:249-271` | **Defensores de invasão não defendem**: a marca `kn8_invasion` nunca é lida e eles vagam | 5 |
| 14 | Média | `client/menu/SquadTab.java` | **Ordens do esquadrão só desenhadas** (seguir, ficar, atacar, recuar são "em breve") | 5 |
| 15 | Baixa | `HoshinaCombatGoal.java:75-78`, `183-212` | Estado do goal não zera ao trocar de alvo; o `unstick` confunde regeneração com progresso e não atua a mais de 8 blocos | 1 |
| 16 | Baixa | `HoshinaEntity.java:616-621` | Mina e Reno usam o contra-ataque de espada (dash para dentro do kaiju); o contra-ataque corta a técnica em andamento | 3 |
| 17 | Baixa | `KikoruNo4Entity.java:103` | Kikoru No. 4: caminho pedido a cada 8 ticks e cancelado no mesmo tick; voo sem desvio de obstáculos; nunca dá dash no ar | 1 |
| 18 | Baixa | `PreondactylEntity.java:181-192` | Mergulho à toa quando nenhuma habilidade está pronta; o golpe do mergulho exige sobreposição vertical (tende a errar) | 4 |
| 19 | Baixa | `KaijuEntity.java:825-832` | Aproximação até o **menor** alcance de todas as habilidades (inclusive as bloqueadas): o kaiju sempre cola no alvo | 4 |
| 20 | Baixa | `HoshinaEntity.java:727`, `343` | Velocidade da técnica do cliente e do servidor podem divergir; com velocidade > 1 vários golpes saem no mesmo tick | 2 |
| 21 | Baixa | `KaijuEntity.java:1140-1159`, `HoshinaEntity.java:767-797` | Animações pedidas que não existem: `*.action.attack` em 11 espécies (inofensivo hoje), `breathe` do Preondactyl e da larva (pedido todo quadro), `hoshina.action.juni_hitoe` | 2 |
| 22 | Baixa | `KaijuEntity.java:175` | Fúria não é salva: depois de recarregar o mundo, ruge de novo | 4 |
| 23 | Baixa | `BossService.java:164` | Fases do chefe não mudam a escolha de ataques | 6 |

**Desempenho** (Parte 13), por entidade:

- **Todo tick:**
  - `react()` do especial: busca de kaiju numa caixa de 33 × 33 × 33.
  - Esquiva do No. 9: busca de projéteis.
  - Ressurreição em massa do No. 9: busca de carcaças na área inteira da invasão, **antes** do intervalo de 10 ticks.
  - `approachReach()` duas vezes, com stream e registros.
- **Sem escalonamento:** nenhuma redução de IA para entidades longe dos jogadores.
- **Logs de depuração:** nenhum log opcional de estado, alvo ou motivo de decisão.

### Situação depois da IA-1A (2026-10-15)

- **#2 resolvido**: progresso por aproximação ou dano no alvo; sem progresso por 10 s o alvo vira inalcançável por
  20 s (`KaijuCombatGoal`). Cenário novo `alto_preso`: antes nunca saía do pilar; agora desiste e mata o soldado do
  chão aos 17,9 s.
- **#3 resolvido no lado do kaiju**: última posição conhecida + busca (`KaijuSearchGoal`). Do lado dos soldados
  (detectar o kaiju em qualquer lugar, decisão do Miguel) fica para a IA-1B: no cenário `parede` o kaiju comum não
  percebe soldados escondidos a 16 blocos (audição 11,2, como pedido) e os soldados ainda não o procuram.
- Alvo: comum ataca o mais perto (troca com margem), inteligente prioriza atirador e ameaça; atordoado parado.
- `cenarios_ia.py`: cenário `alto_preso` (soldados sem IA, pilar de bedrock).

### Situação depois da IA-1B (2026-10-15)

- **#1 resolvido**: sem pentes, quem não tem arma de apoio pega a do config (`[soldierAi] outOfAmmoWeapon`, faca) e
  o atirador especial sem munição luta de perto; GameTest `specialShooterOutOfAmmoIsNotEmptyHanded`.
- **#3 resolvido no lado dos soldados**: detecção sem visão até 64 blocos (`SoldierTargetGoal`). Cenário `parede`:
  antes 40 s sem combate; agora os soldados contornam o muro em 2 s e tiram metade da vida do Primigenius.
- Funções: recarga recuando, recarga sem alvo, cobertura de quem recarrega, revide; soldado comum só nasce com arma
  de fogo e parte para a faca depois de 3 recargas (cenário `municao_alvo`: faca na mão aos ~60–100 s).

### Situação depois da IA-2 (2026-10-16)

- **#5 mudou de sentido por decisão do Miguel**: ataques de kaiju passam por construções de propósito e quebram o que
  está no ponto do golpe (`attackDestruction`). O ângulo (golpe acertando para trás) ficou como estava.
- **#7 resolvido**: o projétil guarda o "heavy" (o bloqueio não segura) e o parry só vale no corpo a corpo.
- **#8 resolvido**: cada tiro da rajada conta (GameTest `everyShotOfABurstCounts`).
- **#9 em parte**: o salto atordoado não dá dano; a aterrissagem continua fora da timeline.
- **#1 (resto)**: munição salva no mundo.
- **Pendente:** #20 (velocidade da técnica) e #21 (animações que faltam), para a IA-3.

### Situação depois da IA-3 e da IA-4 (2026-10-16)

- **#16 resolvido (IA-3)**: atiradores sem contra-ataque de espada; o contra-ataque continua cortando a técnica (é a
  defesa contra golpe pesado).
- **#18, #19 e #22 resolvidos (IA-4)**: Preondactyl só mergulha com golpe pronto e desce até o pé do alvo;
  aproximação pelo alcance das habilidades prontas; fúria salva.

### Situação depois da IA-5 (2026-10-16)

- **#12 resolvido**: com aliado a até 4 blocos (`[squad] separationRadius`), cada soldado chega ao alvo girando o
  ângulo para longe do aliado mais perto; atiradores recuam e orbitam por linhas próprias (desvio pelo id). Cenário
  novo `separacao` (Hoshina, Kikoru, Narumi e um soldado × Primigenius Honju, 4 rodadas cada): menor distância média
  entre aliados na luta 3,5 blocos com a separação contra 2,2 sem ela; colados (< 1 bloco) 1,5% do tempo contra 7,8%.
- **#13 resolvido**: defensores de invasão em esquadrões de 3, cada um num posto a 12 blocos do centro e com um
  kaiju da onda designado (os esquadrões se dividem entre os kaiju antes de dobrar em um; quando o dele morre, pega
  outro); sem alvo voltam ao posto. Visto em jogo na `honju_assault`: entre ondas cada grupo no seu posto; na
  segunda onda a Kikoru foi atrás do Honju designado (fora do raio do posto) e o derrubou.
- **#14 resolvido**: aba Esquadrão com Recrutar/Dispensar e as 4 ordens (`SquadCommandC2S`, protocolo 19); tamanho
  pela patente (`npc_squad_size`), especiais a partir de Vice-Capitão [SUPOSIÇÃO].
- Medidor: `grupo` e `cerco` vencidos; `parede` tira ~110 de 220 do Primigenius (como na IA-1B), mas os dois
  soldados morrem perto dos 30 s (4 rodadas); não medi o `parede` com o código da IA-4 para saber desde quando.

### Situação depois da IA-6 (2026-10-16)

- **#23 resolvido**: os ataques da fase atual do chefe ganham prioridade (`[kaijuAi] bossPhaseBonus`) e o peso do JSON
  decide o empate (`AbilitySelection.pickWeighted`); ataque listado na fase que a espécie não tem fica liberado. A
  fase só avança (sem laço de transição). Os chefes Honju e revivido ganharam listas diferentes por fase.
- **Ataques perigosos legíveis (Parte 12)**: toda habilidade `heavy` marca a área ameaçada durante o preparo
  (`KaijuTelegraph`: anel no pisão e no salto, arco no soco/varredura, linha na investida/tiro), amarelo → vermelho
  conforme o impacto chega, com som grave no início. Visto em jogo (`docs/img/ia6_aviso_golpe_pesado.jpg`).
- **Adaptação do No. 9 revisada**: já tinha teto (JSON, no máximo 0,25 × 2 contra tiro = 50%) e expiração por marca;
  faltava limitar a memória: no máximo `[kaijuAi] adaptationMemory` 6 tipos, tipos sem marca são esquecidos.
- **Pressão do No. 10**: já veio na IA-4 (`chase_speed` 1,2 e janela curta depois do golpe pesado).
- Dificuldade mudando a janela de reação: não feito ([DECIDIR] se o Miguel quiser por dificuldade do mundo).

## 5. Fora da IA (registrado, não mexido)

- **Balanceamento:** Anti-Giant da Mina (×30) mata um Honju de 360 com um tiro (820). O piso de 6 m pega qualquer
  Honju. [DECIDIR] subir o piso (só numerados/gigantes) ou baixar o multiplicador; mexer só com duelos (regra 3 do
  plano).
- **Dano duplicado possível** (não testado): jogador sem arma do kn8 acerta parte do kaiju e a varredura vanilla pega
  o corpo; o zeramento da invulnerabilidade (`KaijuEntity.java:1058`) deixa os dois contarem.
- **Fichas da obra (Parte 2):** ainda não feitas. O plano pede ficha com fontes oficiais por entidade
  (CANON / INFERÊNCIA / MOD ORIGINAL) antes de mexer no perfil dela: entram na Fase 3, uma por personagem.

## 6. Plano das próximas fases (proposta)

Uma etapa por vez, cada uma com GameTests, os cenários acima antes e depois, e duelos para provar que o
balanceamento não mudou.

| Etapa | Escopo | Itens |
|---|---|---|
| **IA-1A** Kaiju: movimento e estados | travamento, alvo no alto, stagger parado, memória curta (última posição; ir até lá e procurar 5-10 s), desistir de alvo inalcançável com recuo do caminho, histerese de alvo (só troca por ameaça bem maior), alvo comandado com limite | 2, 3, 4, 6, 10, 11 |
| **IA-1B** Soldados e especiais: movimento e estados | munição dos especiais (pente para NPC especial ou técnica sem munição; nunca mão vazia) e salva em NBT, memória/investigação, reset do goal, `unstick` melhor (também longe), voo da Kikoru No. 4 sem caminho desperdiçado | 1, 3, 15, 17 |
| **IA-2** Combate | linha de visão e ângulo no impacto (kaiju, técnica, cauda), alvo revalidado no impacto, projétil heavy, rajada com dano por tiro, salto dentro da timeline, stagger com animação, velocidade da técnica sincronizada, animações que faltam | 5-9, 20, 21 |
| **IA-3** Personagens | ficha com fontes por personagem e perfil individual em JSON (distância ideal e de segurança, agressividade, tolerância a dano, preferência de golpe, esquiva, proteger aliado). Piloto: Hoshina, Kikoru, Mina, Kafka/No. 8, Reno, Narumi; Isao quando entrar o modelo | 16 + Parte 9 |
| **IA-4** Kaiju por espécie | Yoju (circular, recuar, cercar sem empilhar), Honju (golpes comprometidos, janelas), No. 9 (já adaptativo: revisar), No. 10 (pressão), Trichonephila (teia controla área), Preondactyl (mergulho com trajetória) | 18, 19, 22 + Parte 10 |
| **IA-5** Esquadrões | separação, divisão de alvos por papel, formação, ordens do menu (seguir, ficar, atacar, recuar, reagrupar), defensores de invasão segurando o ponto | 12-14 + Parte 11 |
| **IA-6** Chefes e adaptação | fases mudando prioridades, sinais de ataque perigoso, revisão da adaptação do No. 9 | 23 + Parte 12 |
| **IA-7** Desempenho e regressão | buscas por tick → intervalos, escalonamento por distância, logs de depuração opcionais (`[ai] debugLog`), matriz §15.4 inteira | Parte 13 |

**Recomendação:** começar pela **IA-1A**. Ela não muda personagem nenhum (só tira kaiju de situações travadas) e
destrava as outras: um kaiju que procura e contorna é o que torna medível a tática dos soldados.
