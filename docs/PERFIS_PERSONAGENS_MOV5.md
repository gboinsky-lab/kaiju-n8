# Perfis por personagem e variação (MOV-5)

Plano de mobilidade/combate v3, seção "Perfis separados por personagem": cada forma, traje ou arma de um personagem é
um perfil próprio, com os dados separados (`special_soldier/<id>.json` e uma entidade por perfil), sem herdar
habilidades da outra forma. Classificação de cada linha: **CÂNONE** (obra), **INFERÊNCIA** (leitura nossa da obra),
**ADAPTAÇÃO DO MOD** (decisão de jogo do Miguel ou nossa) e **A PESQUISAR**. Fontes da obra: as mesmas de
`docs/FICHAS_IA_PERSONAGENS.md` e `docs/MOBILIDADE_COMBATE_FASE0.md` §1 (o site oficial não abre nesta máquina:
[DECIDIR] conferir).

Uma etapa por personagem: **MOV-5A** Kafka ↔ Kaiju No. 8, **MOV-5B** Hoshina, **MOV-5C** Kikoru, **MOV-5D** Mina, **MOV-5E** Reno, **MOV-5F**
Narumi e **MOV-5G** Isao.

## "Sem traje" = sem o traje kaiju (Miguel, 2026-10-17)

"Sem traje" nos perfis quer dizer **sem o traje/arma numerada (feita de kaiju)**, não sem o uniforme da Força de
Defesa. Cada personagem com arma numerada tem os dois perfis:

| Personagem | Sem traje kaiju | Com traje kaiju |
|---|---|---|
| Hoshina | `hoshina` (espadas, até 92%) | `hoshina_no10` (cauda, até 100%) |
| Kikoru | `kikoru` (machado) | `kikoru_no4` (asas, voo) |
| Reno | `reno` (rifle) | `reno_no6` (gelo) |
| Narumi | `narumi` (baioneta) | `narumi_no1` (previsão) |
| Isao | `isao_base` (socos, MOV-5G; v1.2: 520/20/0,29) | `isao` (Numbers 2) |
| Mina | `mina` (canhão; não tem arma numerada) | — |
| Kafka | `kafka` (humano) | `kaiju_no8` (transformação, não traje) |

## Sistema de troca de perfil (MOV-5A)

| Passo do plano | Como ficou |
|---|---|
| 1. Detectar a condição | `HoshinaEntity.tickForm`: `transform.health_below` (Kafka perdendo) e `transform.revert_after_idle_ticks` (No. 8 sem alvo) do perfil ativo |
| 2. Selecionar o perfil | A forma nova é outra entidade (`EntityType` próprio), com o próprio `special_soldier/<id>.json` |
| 3. Só os dados daquele perfil | Vida, armadura, velocidade, arma, técnicas, reações, escalada, regeneração e táticas vêm só do JSON da forma ativa |
| 4. Cancelar ações incompatíveis | A forma antiga some (técnica, recarga e saque somem com ela); a nova nasce parada, com o mesmo alvo |
| 5. Arma, efeitos, IA | Arma do perfil na mão (rifle ↔ punho invisível), aura do perfil, IA de atirador só no Kafka |
| 6. Nada do perfil anterior ativo | GameTest `formSwitchKeepsSquadAndAmmoWithoutSharingAbilities`: o No. 8 não usa `punch_combo` nem fica atirador; o Kafka não usa `heavy_punch` |
| 7. Perfil ativo no log | `[kaijuAi] debugLog`: `perfil ativo kn8:kaiju_no8 (antes kn8:kafka)`; `HoshinaEntity.activeProfileId()` nos testes |

**O que passa de uma forma para a outra** (bug 10 da MOV-0): líder do esquadrão, ordem, posto, kaiju designado,
dados persistentes (marca de invasão, duelo, guarnição), lugar no esquadrão da invasão
(`InvasionService.replaceDefender`) e usos de técnica limitados. **O que fica guardado:** arma e munição da forma que
saiu (`kn8_form_memory`, salvo no mundo): o Kafka volta com o pente e a reserva que tinha (ou com a faca, se já tinha
ficado sem munição). **O que não passa:** técnicas, atributos, escalada [SUPOSIÇÃO: o Kafka não escala e o No. 8 só
volta depois de 15 s sem alvo, quando a escalada já caiu], vida (a forma nova nasce com a vida cheia, como na 0.7-F).

## Kafka Hibino — humano com traje da Força de Defesa (`kafka`)

| Item | Valor | Fonte |
|---|---|---|
| Estado do traje | Traje padrão da Força de Defesa, Release baixo (`release` 0, `max_release` 10) | CÂNONE (traje padrão; Release baixo no começo) / ADAPTAÇÃO DO MOD (números) |
| Atributos | Vida 24, armadura 6, velocidade 0,3, `kaiju_damage` 0,3 (v1.2 §11, T2) | ADAPTAÇÃO DO MOD |
| Arma | Rifle (pentes do perfil; sem munição, faca do `outOfAmmoWeapon`) | CÂNONE (rifle da Força de Defesa) / ADAPTAÇÃO DO MOD (faca) |
| Técnicas | `punch_combo` (2 socos de perto) | INFERÊNCIA |
| Reações | Esquiva (dash 100 ticks), contra-ataque, aparar 15% | ADAPTAÇÃO DO MOD |
| Movimentação | Atirador: mantém distância, recua com 60% da vida para 14 blocos, cobertura não (só soldado comum, MOV-3) | INFERÊNCIA |
| Troca | Vida abaixo de 50% → Kaiju No. 8 no mesmo lugar | CÂNONE (vira o No. 8 em perigo) / ADAPTAÇÃO DO MOD (gatilho por vida) |
| A PESQUISAR | Kafka sem traje lutando (limpador); limite do Release do Kafka em cada arco | A PESQUISAR |

## Kaiju No. 8 — forma kaiju do Kafka (`kaiju_no8`)

| Item | Valor | Fonte |
|---|---|---|
| Atributos | Vida 1.800, armadura 19, velocidade 0,4, `kaiju_damage` 0,6 (v1.2 §11, T8) | ADAPTAÇÃO DO MOD |
| Arma | Punhos (`kn8:kaiju_no8_fist`, dano 11) | CÂNONE (luta com o corpo) |
| Técnicas | `rush_combo`, `heavy_punch`, `dash_strike`, `ground_smash` (onda que explode), `roar` (empurra, lentidão) | CÂNONE (socos de força enorme) / INFERÊNCIA (golpe no chão, rugido) |
| Defesa | Guarda contra golpe pesado (metade do dano, arco de 120°), aparar 20%, esquiva 0,5 | ADAPTAÇÃO DO MOD (Miguel) |
| Regeneração | 1,5%/s abaixo de 50% | CÂNONE (regenera) / ADAPTAÇÃO DO MOD (número) |
| Escalada | Até +45 pontos de Release em luta, recargas até −35% | ADAPTAÇÃO DO MOD |
| Movimentação | Fecha distância e pressiona (`aggression` 0,95); pesado contra grande, rápido contra pequeno | INFERÊNCIA |
| Quebra blocos | Só os pesados (`heavy`: soco pesado, investida, golpe no chão; força 3, raio 3) | ADAPTAÇÃO DO MOD |
| Troca | 300 ticks sem alvo → Kafka | ADAPTAÇÃO DO MOD |
| A PESQUISAR | Transformação parcial (braço), controle do No. 8 pela Força de Defesa | A PESQUISAR |

**Diferenças objetivas:** o Kafka é atirador frágil que recua; o No. 8 é corpo a corpo, tanque, regenera e quebra
blocos com os pesados. Nenhuma técnica, reação ou número de um vale no outro.

### Testes (MOV-5A)

- GameTest `formSwitchKeepsSquadAndAmmoWithoutSharingAbilities` (ida e volta, 201/201 no total).
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5a_kafka_no8_troca_de_forma.jpg`): Kafka no esquadrão do Dev
  contra um Primigenius; com a vida baixa vira o No. 8, que segue o Dev, mata o Primigenius e, 15 s depois, volta a
  ser o Kafka ainda no esquadrão do Dev, com o rifle e a reserva gasta (114 balas, não 120: não encheu de novo).

## Soshiro Hoshina — traje padrão com as duas espadas (`hoshina`) e com a arma numerada 10 (`hoshina_no10`) (MOV-5B)

Plano v3 §7: "especialista em combate próximo e lâminas. Encurtar distância com controle, mover-se lateralmente para
alinhar cortes, executar sequências rápidas com eventos de impacto separados, explorar brechas e reposicionar-se diante
de golpes pesados. Não agir como atirador."

| Item | `hoshina` | `hoshina_no10` | Fonte |
|---|---|---|---|
| Estado do traje | Traje de combate padrão, Release 40% até 92% (escalada) | Arma/traje numerado 10 (cauda), sincronização até 100%, Full Release | CÂNONE (traje padrão; No. 10 com cauda que ataca e sincronização difícil, ep. 22) / ADAPTAÇÃO DO MOD (números) |
| Atributos | Vida 460, armadura 20, velocidade 0,3, `kaiju_damage` 1,0 | Vida 520, Release 50% até 100% | ADAPTAÇÃO DO MOD (Miguel, 2026-10-07/08) |
| Arma | Duas espadas (`kn8:hoshina_sword`, uma em cada mão) | As mesmas + cauda (corta e defende sozinha) | CÂNONE |
| Técnicas | Kuuchi, Kosa-uchi (cortes à distância), Ran-uchi, Kasumi-uchi, Yae-uchi (sequências de perto) | As mesmas + Juni-hitoe (só no Full Release) | CÂNONE (estilo Hoshina, anime/mangá e o jogo) / INFERÊNCIA (números) |
| Defesa | Esquiva (dash 30 ticks), Kaeshi-uchi contra golpe pesado, aparar 35%, guarda contra pesado ½ | + guarda da cauda pelas costas | CÂNONE (contra-ataque, reflexos) / ADAPTAÇÃO DO MOD (números) |
| Movimentação | Perto do alvo (3 blocos), passo lateral depois da técnica (agressão 0,6), recua com 25% para 8 | Agressão 0,65, recua com 20% | INFERÊNCIA |
| Brecha (MOV-5B) | Guarda do kaiju aberta (janela depois do golpe pesado, atordoado): a técnica que mais tira +3 e pressiona em vez de reposicionar | Igual | INFERÊNCIA ("explora brechas" do plano; Hoshina lê o adversário na obra) |
| Sequências (MOV-5B) | No máximo um golpe por tick (Release acelerando não junta cortes); combo não começa com o alvo saindo do alcance; combo que erra tudo espera +40 ticks | Igual | ADAPTAÇÃO DO MOD |
| Porte do alvo | Rápido contra Yoju +2; Yae-uchi contra Honju +4 (abre o núcleo) | Igual | INFERÊNCIA (melhor contra kaiju pequenos e rápidos: CÂNONE) |
| Não atirador | Sem bloco `ranged`; os cortes à distância (Kuuchi, Kosa-uchi) são técnicas de espada, não mantém distância | Igual | CÂNONE |
| Sem traje kaiju | É o próprio `hoshina`: traje padrão, sem a arma/traje numerado 10 (decisão do Miguel, 2026-10-17: "sem traje" = sem o traje kaiju) | — | Decisão do Miguel |

**Diferenças objetivas:** o No. 10 tem a cauda (alvo próprio, guarda pelas costas), vai a 100% e libera o
Juni-hitoe; o normal para em 92% e não tem guarda pelas costas. A troca entre os dois não acontece em luta (são NPCs
diferentes; por ora o No. 10 só por ovo/`/summon`).

### Testes (MOV-5B)

- JUnit `TechniqueMathTest` (3). GameTests `HoshinaProfileGameTests` (4, todos 205/205): brecha escolhe o Yae-uchi
  (fechada: Ran-uchi), alvo fugindo não recebe combo, Yae-uchi com o Release no máximo acerta os 8 golpes um por tick,
  Kasumi-uchi que não acerta nada soma 40 ticks à recarga.
- Duelos (servidor dedicado, 5 cada): × No. 10 pequeno 5/5, × gigante 0/5, × Primigenius Honju 5/5 em 10–14 s.
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5b_hoshina_brecha.jpg`, log ligado): contra o Primigenius
  Honju o log mostra "brecha do kaiju: pressiona" depois do primeiro golpe pesado, o núcleo aparece exposto (Yae-uchi)
  e o Honju cai em menos de 16 s; o Hoshina sai da área do pisão.

## Kikoru Shinomiya — traje padrão com o machado (`kikoru`) e com a arma numerada 4 (`kikoru_no4`) (MOV-5C)

Plano v3 §7: "agressiva, talentosa e móvel, usando machado. Alternar golpes rápidos e comprometidos, explorar
aberturas/flancos e esquivar de ataques pesados. Não ser invencível nem cancelar toda recuperação."

| Item | `kikoru` (sem traje kaiju) | `kikoru_no4` (com traje kaiju) | Fonte |
|---|---|---|---|
| Estado do traje | Traje padrão, Release 30% até 75% | Arma/traje numerado 4: asas e voo, Release 40% até 90% | CÂNONE (Numbers 4 com voo) / ADAPTAÇÃO DO MOD (números) |
| Atributos | Vida 390, armadura 18, velocidade 0,34, `kaiju_damage` **0,64** (era 0,62) | Vida 580, armadura 20, velocidade 0,44, `kaiju_damage` 0,5 | ADAPTAÇÃO DO MOD (v1.0/v1.2; 0,64 na MOV-5C) |
| Arma | Machado de duas mãos (`kn8:axe`) | O mesmo | CÂNONE |
| Técnicas | `axe_slash` (rápida), `heavy_swing`, `shockwave`, `dash_strike`, `ground_smash`, `guard_break` (pesadas: quebram blocos) | `high_speed_axe`, `air_combo`, `aerial_dash` (rápidas), `dive_strike`, `vertical_assault` (pesadas) | INFERÊNCIA (Biblioteca v22 §8) |
| Defesa | Esquiva (dash 100 ticks), contra-ataque, aparar 15%, guarda contra pesado ½ | Igual, dash 70 ticks | ADAPTAÇÃO DO MOD |
| Movimentação | Agressão 0,85, passo lateral, recua com 20% para 8 | Voo (paira 2,5 acima), agressão 0,9, recua com 20% para 10 | CÂNONE (voo) / INFERÊNCIA |
| Alternar (MOV-5C) | Depois de técnica pesada a rápida ganha +1 e vice-versa (`rhythm.alternate_bonus`) | Igual | INFERÊNCIA (plano §7: "alternar golpes rápidos e comprometidos") |
| Pressionar quem regenera (MOV-5C) | Com o kaiju regenerando (No. 9/No. 10 abaixo da faixa de cura) a técnica que acerta mais cedo ganha +2 e ela não reposiciona (`rhythm.press_regeneration`) | Igual | ADAPTAÇÃO DO MOD (a cura do No. 9 para logo depois de cada golpe) |
| Brecha | +2 na técnica que mais tira com a guarda aberta (`exploit_openings`); combo que erra espera +40 | Igual | INFERÊNCIA ("explorar aberturas") |
| A PESQUISAR | Flanqueio (o kaiju sempre gira para o alvo; sem efeito medido, não entrou) | — | A PESQUISAR |

**Diferenças objetivas:** a No. 4 voa (paira acima do alvo e desce no golpe), tem mais vida e técnicas aéreas; a
normal luta no chão e quebra blocos com mais técnicas pesadas.

### Kikoru × No. 9 (o [DECIDIR] da MOV-4)

Causa medida (log temporário de dano, 2 duelos): a Kikoru tirava ~2.000 de vida por duelo (o No. 9 tem 1.450), mas
abaixo de 50% ele regenera 2,5–5%/s nas pausas entre os golpes dela e parava em 21–38%. Recebido por ela: garra 673,
golpe comum 414, investida 186, Finger Gun 104. Com o ritmo novo: 1/10; com `kaiju_damage` 0,68: 7/10 (fio da navalha:
a fuga a 15% conta como vitória); com **0,64: 3/10 (30%; meta 25%)**. × No. 10 pequeno 0/5 (era 0% no v1.2 também; a
meta de 35% continua brigando com a do No. 9), × Primigenius Honju 3/3 com 95–98% da vida, Kikoru No. 4 × No. 9 5/5.

### Testes (MOV-5C)

- GameTests `KikoruProfileGameTests` (2; 207/207 duas vezes): depois do golpe no chão vem o corte rápido e depois do
  rápido um pesado; No. 9 abaixo de 50% conta como regenerando.
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5c_kikoru_x_no9.jpg`, log ligado): Kikoru × No. 9 em
  velocidade normal; "alvo regenerando: pressiona" no log; o No. 9 foge a 15% com a Kikoru viva (82 de 390).

## Mina Ashiro — traje padrão com o canhão (`mina`) (MOV-5D)

Plano v3 §7: "combate de longo alcance. Encontrar linha de tiro, priorizar alvos grandes/designados, preparar
disparos, reposicionar-se quando o alvo se aproxima e não entrar em corpo a corpo por padrão." Não tem arma numerada
(só um perfil).

| Item | `mina` | Fonte |
|---|---|---|
| Estado do traje | Traje padrão, Release 40% até 90% | CÂNONE / ADAPTAÇÃO DO MOD (números) |
| Atributos | Vida 420, armadura 20, velocidade 0,3, `kaiju_damage` 0,6 | ADAPTAÇÃO DO MOD (v1.2 §12) |
| Arma | Canhão (`kn8:mina_cannon`); sem munição, faca (IA-1B) | CÂNONE (canhão) / ADAPTAÇÃO DO MOD (faca) |
| Técnicas | Tiro de precisão; tiro de canhão e canhão carregado (explodem); Anti-Giant (30x, 4 por vida) | CÂNONE (tiros de grande potência) / ADAPTAÇÃO DO MOD (números) |
| Movimentação | Mantém 22 blocos, recua abaixo de 12, circula o alvo, persegue se ele foge (`ranged`) | INFERÊNCIA |
| Alvo | Prioriza kaiju grande (`big_target` 12), pesado contra grande +2 | CÂNONE (comandante, alvos grandes) / INFERÊNCIA |
| Validação do tiro (MOV-5D) | No disparo o tiro só sai com linha de tiro e o alvo ao alcance; no preparo, se o alvo chega mais perto que o alcance mínimo, ela cancela (o uso limitado volta, recarga = só o preparo) e recua | ADAPTAÇÃO DO MOD (plano v3, "validação final") |
| Anti-Giant guardado (MOV-5D) | `ranged.reserve_limited`: os 4 tiros não saem contra Yoju (Honju, numerados, Daikaiju sim) | ADAPTAÇÃO DO MOD (decisão do Miguel "qualquer kaiju" mantida para os grandes) [SUPOSIÇÃO] |
| Aliados | Os tiros e as explosões não ferem aliados (`SlashProjectile` poupa aliados): nada a mudar | — |
| Corpo a corpo | Não usa contra-ataque de espada (`melee_counter` false); só com a faca sem munição | INFERÊNCIA |
| Cobertura (Miguel 2026-10-18: "ela deve se ajustar de acordo com o combate") | Adaptativa: com o alvo até `keep_distance` + 3, contra kaiju que atira só sob pressão (vida abaixo de `ranged.cover_health` 0,5) e sem janela para pressionar (alvo regenerando ou de guarda aberta); para recarregar, sempre. Escondida não começa técnica; sai para o ponto de tiro e troca quando a cobertura é contornada/quebrada. Fora disso: recuo e órbita | ADAPTAÇÃO DO MOD (decisão do Miguel) [SUPOSIÇÃO 0,5] |

### Testes (MOV-5D)

- GameTests `MinaProfileGameTests` (4; 211/211): Anti-Giant nunca escolhido contra Yoju (20 escolhas) e escolhido
  contra o Honju; Anti-Giant com muro de bedrock posto no preparo é cancelado, o Honju não perde vida e os 4 usos
  continuam; canhão carregado com o kaiju teletransportado para 3 blocos é cancelado com recarga curta.
- Duelos (servidor dedicado, 5 cada, a 30 blocos): × No. 10 pequeno 5/5, × No. 10 gigante 4/5, × No. 9 4/5.
  **Mina × No. 9 — decidido (Miguel, 2026-10-18): aceito que ela vença** (opção c). A meta "perde" de 2026-10-15 sai:
  desde a IA-2 o Anti-Giant vale contra qualquer kaiju e um tiro dele leva o No. 9 de 50–80% a 0.
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5d_mina.jpg`, log ligado): contra o Primigenius Honju os tiros
  saem de longe e ele cai; contra o Primigenius (Yoju, sem Anti-Giant) ele chega perto e três tiros são cancelados
  ("cortada: alvo perto demais"); ela termina com 416 de 420.

### Cobertura adaptativa (decisão do Miguel, 2026-10-18)

- Vale para os atiradores especiais (`ranged`: Mina, Reno, Reno No. 6). `HoshinaEntity.wantsCoverFrom` decide se o
  kaiju que atira justifica a cobertura; o `ShooterCover` (o mesmo do soldado comum, MOV-3) acha o muro, sai para o
  ponto de tiro e troca quando ele é contornado ou quebrado. `keepDistance()` do especial passou a ser o do perfil.
- Medido (servidor dedicado, chão refeito a cada duelo): cobertura **sempre** contra kaiju que atira: Mina × No. 9
  0/5 (desligada 3/5; ela se escondia a cada mira do Finger Gun e o No. 9 regenerava). **Adaptativa:** Mina × No. 9 4/10 e × No. 10 pequeno 4/10 (cobertura desligada: 6/10 e 6/10; diferença dentro do ruído de 10 duelos, com o chão refeito a cada duelo); Reno No. 6 × No. 9 0/3 com e sem cobertura (era 6/6 no v1.2: queda anterior a esta mudança, fica para a etapa do Reno).
- GameTests (`MinaProfileGameTests`, 213/213): com a vida cheia não se esconde da formiga; com 30% acha a cobertura
  atrás do muro; contra o Primigenius (só de perto) não usa cobertura.
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5d_cobertura_adaptativa.jpg`): com 38% da vida contra a
  formiga, cobertura atrás do muro, sai para atirar, troca quando a formiga quebra o muro e a mata sem perder vida;
  contra o Primigenius nenhuma cobertura (luta de longe e o derruba).

## Reno Ichikawa — sem traje kaiju (`reno`) e Numbers 6 (`reno_no6`) (MOV-5E)

Plano v3 §7 e papel do Miguel (2026-10-15): "forte principalmente com aliados: congela, paralisa e expõe pontos
fracos". Fontes: `docs/FICHAS_IA_PERSONAGENS.md` (Reno) e `docs/MOBILIDADE_COMBATE_FASE0.md` §1 (gelo, barreira de gelo
no ep. 19, tiro no núcleo com o Iharu abrindo caminho no ep. 20).

| Item | `reno` | `reno_no6` | Fonte |
|---|---|---|---|
| Estado do traje | Traje padrão, Release 30% até 70% | Numbers 6 (gelo), 50% até 100% | CÂNONE / ADAPTAÇÃO DO MOD (números) |
| Atributos | Vida 360, armadura 17, velocidade 0,31, `kaiju_damage` 0,5 | Vida 650, armadura 20, velocidade 0,36, `kaiju_damage` **0,80** (era 0,75) | ADAPTAÇÃO DO MOD (v1.2 §12; 0,80 [SUPOSIÇÃO] MOV-5E) |
| Arma | Rifle; sem munição, faca | Rifle + gelo | CÂNONE |
| Técnicas | Tiro de precisão, rajada, munição congelante (Lentidão III, expõe), supressão, coronhada | Lança de gelo, explosão congelante (paralisa: Lentidão VII, expõe 5 s, vulnerável ×1,4), campo de gelo, congelamento múltiplo, canhões auxiliares | CÂNONE (gelo) / ADAPTAÇÃO DO MOD (números) |
| Movimentação | Atirador: mantém 16, recua abaixo de 9, circula; cobertura adaptativa (MOV-5D) | igual | INFERÊNCIA |
| Apoio (MOV-5E) | Kaiju paralisado (Lentidão 6+) conta como **brecha** (`isOpen`): Hoshina/Kikoru pressionam com a técnica que mais tira; o atirador não se esconde de alvo paralisado | igual (a explosão congelante paralisa) | CÂNONE (congela para os aliados) / ADAPTAÇÃO DO MOD |
| Pressão (MOV-5E) | `exploit_openings` 2, `rhythm.press_regeneration` 2 (alvo regenerando: técnica que acerta mais cedo e sem reposicionar) | igual | INFERÊNCIA |
| Golpe pesado do kaiju (MOV-5E) | Sai da área **atirando** (antes parava de atirar ao sair) | igual | ADAPTAÇÃO DO MOD |
| Aliados | Protege aliados (`protect_ally` 10), tiros poupam aliados | igual | INFERÊNCIA |
| Corpo a corpo | Sem contra-ataque de espada | igual | INFERÊNCIA |
| A PESQUISAR | Barreira de gelo (ep. 19) e tiro no núcleo com cobertura do aliado (ep. 20): sem mecânica ainda | — | A PESQUISAR |

### Diagnóstico (Reno No. 6 × No. 9)

- Hoje 0/4; calibrado em 9/10 em 2026-10-16. Bisseção (worktree com cópia do servidor de testes): antes da MOV-4 2/4,
  no próprio commit da calibragem 1/4 → o 9/10 foi medido em outra condição de arena; não é regressão de código. A
  MOV-4 piorou um pouco: o atirador parava de atirar enquanto saía da área do golpe pesado (`avoidDanger` desligado:
  1/3).
- Log temporário da adaptação do No. 9: todas as técnicas do Reno contam como o mesmo tipo de golpe (projétil) e ficam
  na redução máxima de 40%; o No. 9 chega a ~410 de vida e volta a ~725 (50%) quando o Reno perde o ritmo.

### Testes (MOV-5E)

- GameTests `RenoProfileGameTests` (3; 216/216): Lentidão II não é brecha e Lentidão VII é (paralisado e aberto); a
  explosão congelante do Reno No. 6 paralisa o kaiju; com 30% da vida ele quer cobertura contra a formiga, e com a
  formiga paralisada não (pressiona).
- Duelos (servidor dedicado, chão refeito a cada duelo): Reno No. 6 × No. 9 1/6 com o perfil e 0,75; **5/8 com 0,80**
  (0,85: 5/6; meta v1.2 70%); Reno × No. 9 0/3 (meta 12%); Hoshina + Reno × No. 10 gigante 1/3 (o gigante fica com
  18–20% nas derrotas; 2/3 em 2026-10-15).
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5e_reno.jpg`, log ligado): Reno No. 6 + Hoshina derrubam o
  Primigenius Honju em menos de 24 s ("brecha do kaiju: pressiona" no Hoshina); Reno No. 6 sozinho leva o No. 9 de
  1.450 a 778 em 24 s, saindo 16 vezes da área dos golpes, com 546 de 650.

## Gen Narumi — sem traje kaiju (`narumi`) e Numbers 1 (`narumi_no1`) (MOV-5F)

Fontes: `docs/FICHAS_IA_PERSONAGENS.md` (Narumi) e `docs/MOBILIDADE_COMBATE_FASE0.md` §1 ("retinas preveem o
movimento do inimigo; fuzil com baioneta; o No. 11, de água, anula a previsão").

| Item | `narumi` | `narumi_no1` | Fonte |
|---|---|---|---|
| Estado do traje | Traje padrão, Release 40% até 85% | Numbers 1, 50% até 100% | CÂNONE / ADAPTAÇÃO DO MOD (números) |
| Atributos | Vida 480, armadura 20, velocidade 0,32, `kaiju_damage` 1,3 | Vida 650, armadura 20, velocidade 0,36, `kaiju_damage` 0,85 | ADAPTAÇÃO DO MOD (1,3: Miguel 2026-10-16, vence o No. 13) **[DECIDIR] o Narumi sem o traje tira mais por golpe que com o No. 1** |
| Arma | Baioneta (estocadas, investida, tiro, varrida) | igual + golpe no ponto fraco (expõe o núcleo) e contra-golpe previsto | CÂNONE (baioneta que corta, estoca e atira) |
| Movimentação | Pressiona e reposiciona (`aggression` 0,75, passo lateral, 4 blocos), recua com 15% | igual (`aggression` 0,8) | INFERÊNCIA |
| **Previsão (MOV-5F)** | — | `rhythm.read_windup` 3: com o kaiju preparando um golpe, a técnica que acerta antes do impacto ganha o bônus; contra golpe mirado nele, a que só acertaria depois não começa (esquiva/contra-ataque); não reposiciona durante o preparo | CÂNONE (vê o movimento antes) / ADAPTAÇÃO DO MOD (números) [SUPOSIÇÃO] |
| Reações | Esquiva, contra-ataque, aparar 15% | aparar 35%, contra-ataque com reação mais longa (10 ticks) | ADAPTAÇÃO DO MOD (0.7-D) |
| Guarda | Contra golpe pesado: metade do dano, de frente | igual | ADAPTAÇÃO DO MOD (IA-3) |
| Alvo | Prioriza alvo grande (6) | 8 | INFERÊNCIA |
| A PESQUISAR | — | No. 11 (água) anulando a previsão: o kaiju não existe no mod | A PESQUISAR |

### Testes (MOV-5F)

- JUnit `TechniqueMathTest.readsTheWindupOnlyWhenTheHitComesFirst`. GameTests `NarumiProfileGameTests` (2; 218/218):
  com o Primigenius no preparo do pisão contra ele, as 20 escolhas do No. 1 acertam antes do impacto; com o impacto a
  3 ticks, nenhuma técnica começa.
- Duelos (servidor dedicado, chão refeito a cada duelo): Narumi No. 1 × No. 9 5/5 (por fuga; Narumi com 63–80%), ×
  No. 10 pequeno 5/5, × No. 13 5/5, × No. 10 gigante 0/3 (gigante com 34–42%; com `read_windup` 0: 41–66% e o Narumi
  morre antes).
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5f_narumi.jpg`, log ligado): o Primigenius Honju cai em menos
  de 24 s com o Narumi com 642 de 650; contra o No. 9, em 24 s: 6 técnicas escolhidas por acertar antes do impacto
  (contra-golpe previsto 3, estocadas, investida), uma segurada ("espera: o golpe do kaiju chega antes"), 4 vezes
  ficou para bater em vez de reposicionar; No. 9 em 565 de 1.450.

## Isao Shinomiya — sem traje kaiju (`isao_base`) e Numbers 2 (`isao`) (MOV-5G)

Fontes: `docs/FICHAS_0_8_A.md` (Isao: Diretor-Geral, 1,90 m, manoplas FS-1002 do Kaiju No. 2, "a força vem das pernas
para os punhos"; Barrage Blast, Spiral Blast, Thunder Emperor quebra a guarda e expõe o núcleo).

| Item | `isao_base` | `isao` | Fonte |
|---|---|---|---|
| Estado do traje | Traje padrão, Release 40% até 85% | Numbers 2, 50% até 96% | ADAPTAÇÃO DO MOD [SUPOSIÇÃO base] |
| Atributos | Vida 520, armadura 20, velocidade 0,29, `kaiju_damage` 1,2 | Vida 700, armadura 20, velocidade 0,32, `kaiju_damage` 1,2 | v1.2 §12 / ADAPTAÇÃO DO MOD (1,2 do base [SUPOSIÇÃO]) |
| Arma | Punhos (`kn8:isao_fist`, invisível) | Manoplas Numbers 2 (invisível; no modelo) | INFERÊNCIA (base) / CÂNONE |
| Técnicas | Jab (4 socos), cotovelada com avanço, direto pesado (quebra blocos) | Barrage Blast, Spiral Blast, Force Blast, Thunder Emperor | INFERÊNCIA (base: sem as ondas) / CÂNONE + jogo |
| Brecha (MOV-5G) | `exploit_openings` 3 | `exploit_openings` 3: o Thunder Emperor expõe o núcleo e o núcleo exposto conta como brecha (vale para todos) | CÂNONE (expõe o núcleo) / ADAPTAÇÃO DO MOD |
| Pressão (MOV-5G) | `rhythm.press_regeneration` 2 | igual | INFERÊNCIA (lutou contra o No. 9, que regenera) |
| Guarda | Metade do dano pesado, 120° | 45%, 140° | ADAPTAÇÃO DO MOD |
| Absorção | Não é presa do No. 9 | Presa do No. 9 abaixo de 30% (vira o No. 9 com o Isao) | CÂNONE |
| A PESQUISAR | O Isao lutando sem o Numbers 2 (não aparece na obra); modelo sem as manoplas [DECIDIR] | — | A PESQUISAR |

### Testes (MOV-5G)

- GameTests `IsaoProfileGameTests` (2; 220/220): o Isao sem o Numbers 2 nasce com 520 de vida, os punhos e nenhuma
  técnica de onda; núcleo exposto conta como brecha.
- Duelos (servidor dedicado, chão refeito a cada duelo): Isao sem o Numbers 2 com 1,2: × No. 10 pequeno 2/5 (meta
  45%; com 1,0: 0/5), × No. 9 5/5 por fuga (meta 30%; a fuga a 15% conta como vitória: [DECIDIR] antigo da matriz),
  × Primigenius Honju 3/3 em 11–15 s. Isao com o Numbers 2: × No. 9 5/5, × No. 10 pequeno 5/5, × No. 10 gigante 0/5
  (meta 60%; era 4/5 na 0.8-A; sem o perfil novo também 0/5; com `kaiju_damage` 1,35 ou 1,5 só 1/5) [DECIDIR].
  Hoshina × No. 10 pequeno 3/3 (núcleo exposto como brecha sem regressão).
- Em jogo (servidor dedicado + 2 clientes, `docs/img/mov5g_isao.jpg`, log ligado): o Isao com o Numbers 2 derruba o
  Primigenius Honju em menos de 24 s ("brecha do kaiju: pressiona" depois do Thunder Emperor) com 694 de 700; o Isao
  sem o Numbers 2 derruba o Primigenius em menos de 24 s com 513 de 520.


## Decisões do Miguel (2026-10-22)

- **Fuga = empate:** os resultados "por fuga" acima (Isao, Isao sem o Numbers 2, Narumi, Narumi No. 1, Kikoru, Reno
  No. 6 contra o No. 9) passam a ser empates; só vence quem mata o No. 9 antes dos 15%.
- **Isao (`isao`):** Thunder Emperor `expose_core_ticks` 60 → 120 [SUPOSIÇÃO] e, para todo numerado, núcleo exposto
  corta a regeneração (CÂNONE do v1.2: "destruir o core interrompe a regeneração avançada"). Diagnóstico: log temporário
  de dano mostrou o Isao tirando 5.944 por duelo do No. 10 gigante (4.600 de vida) e perdendo pela cura de até 92/s
  abaixo de 50%. Medido (fuga = empate): × No. 10 gigante **7/10** (meta 60%; com 1,35 de dano e sem o Thunder
  Emperor mais longo só 1/5).
- **Narumi No. 1 (`narumi_no1`):** mais forte que o Narumi pelos ataques precisos e pela previsão: `kaiju_damage`
  0,85 → 1,4 e `exploit_openings` 3 [SUPOSIÇÃO]. Medido (5 duelos): × No. 13 5/5 em ~25 s com 53–70% (Narumi 4/5 em
  ~40 s com 26–45%); × No. 10 pequeno 5/5 em ~17 s com 57–78%; × No. 9 5 empates por fuga em ~16 s com 77–93% (Narumi:
  em ~26 s com 56–79%); × No. 10 gigante 0/5 com o gigante a 1–16% (Narumi: 23–52%).
- Modelos próprios do Narumi No. 1 e do Isao sem o Numbers 2: futuramente (A PESQUISAR acima fica para essa etapa).
