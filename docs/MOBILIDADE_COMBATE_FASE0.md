# Mobilidade e combate — Fase 0 (auditoria + pesquisa oficial)

Plano do Miguel (2026-10-16): `PLANO_MOVIMENTACAO_COMBATE_HABILIDADES_KAIJU_N8_v3.md` (soldados pulando estruturas,
cobertura, esquiva, combate tático, habilidades individuais por personagem/forma/kaiju). Fase 0 = **só leitura**:
nenhum código foi alterado. Etapas do plano neste mod: **MOV-0** (esta) a **MOV-7**.

## 1. Fontes oficiais

O site oficial abre por `https` nesta sessão (o "DNS" das sessões anteriores era do `WebFetch`, não do site):
`kaiju-no8.net/en/` e as sinopses dos episódios 1–23, a ficha da Production I.G (só equipe) e o jogo
`kj8-thegame.com/en/`. Os perfis de personagem do site são carregados por JavaScript (não lidos) e a Fandom está
atrás do Cloudflare. Complemento: Wikipedia (lista de personagens, episódios, série). Texto bruto baixado no
scratchpad da sessão.

| Tema | O que as fontes dizem | Classificação |
|---|---|---|
| Traje da Força de Defesa | exoesqueleto de fibra de kaiju; força, velocidade, resistência e potência da arma; escudo; % de poder de combate (capitães/vice > 90%); operadores tiram o limitador | CANON |
| Trabalho em equipe | recrutas apoiam os outros (ep 4), informação de anatomia passada aos pelotões (ep 6), tropa executa as ordens do Narumi (ep 14) | CANON |
| **Pular entre prédios / cobertura atrás de construções** | **nenhuma sinopse descreve**; Kafka no telhado da base (ep 8–9, fonte secundária) | INFERÊNCIA (o salto vem do traje); conferir nos ep 3–4, 9–10, 22 |
| Hoshina | espadachim, melhor contra kaiju pequenos, fraco com arma de fogo; tira o limitador (ep 7); leva o No. 10 para o campo de treino, o traje corta a saída por desgaste e o No. 10 o agarra (ep 9–10); **No. 10**: traje com cauda que ataca, salta de helicóptero, dificuldade de sincronizar (ep 22) | CANON |
| Kikoru | machado gigante, maior % de recruta; corre à frente e limpa o exame sozinha (ep 4); **No. 4**: velocidade e voo (ep 22) | CANON |
| Mina | canhão contra kaiju gigantes; railgun "além do horizonte" (ep 23); não usa lâmina | CANON |
| Kafka / No. 8 | força, regeneração, detecta kaiju; salta até a bomba de wyverns e a soca para cima (ep 10); aprende o estilo corpo a corpo do Isao (ep 21) | CANON |
| Reno / No. 6 | gelo, barreira de gelo (ep 19), tiro no núcleo com Iharu abrindo caminho (ep 20); desgasta o corpo | CANON |
| Narumi / No. 1 | retinas preveem o movimento do inimigo; fuzil com baioneta; o No. 11 (água) anula a previsão | CANON |
| Isao / No. 2 | armadura do No. 2; quase destrói o No. 9 e é absorvido (ep 16–17) | CANON |
| No. 9 | revive kaiju (ep 4), espaço que corta a comunicação (ep 7), cópias que infectam formigas, se divide em dois corpos que evoluem na luta (ep 14–15), absorve o Isao e foge (ep 16–17) | CANON |
| No. 10 | lidera wyverns (normalmente solitárias), procura o lutador mais forte, cresce, agarra | CANON |
| Wyverns / formigas | voam e se juntam numa bomba (ep 10); enxame que sai de um buraco (ep 13–14) | CANON |
| No. 13 | derruba o Tachibana, soco pesado na Shinonome, vencido pelo Kafka com um golpe (mangá 82–84) | CANON (resumido); detalhes incertos |
| Kaiju-aranha | não aparece na obra | MOD ORIGINAL (já era) |
| Jogo oficial | esquadrões de 4 com ataques de acompanhamento; Ultimate quando o núcleo fica exposto | CANON do jogo, não do anime |

**Não verificado:** preparo (wind-up) dos golpes dos kaiju, esquivas cena a cena, % exatos das armas numeradas.
Tudo isso fica INFERÊNCIA DE GAMEPLAY até alguém assistir às cenas.

## 2. O que já existe (etapas IA-1A a IA-7, 0.5.0-E, 0.7, 0.8-A)

- **Navegação:** soldados com a navegação terrestre do vanilla (degrau 0,6); kaiju com degrau proporcional (1–4),
  quebra de bloqueio ao andar, descida de construções, progresso/desistência de alvo (`KaijuCombatGoal.checkProgress`).
- **Destravar:** só nos soldados especiais (`HoshinaCombatGoal.unstick`: pula e corre para o lado; alvo no alto →
  pula). Impulsos reais por `dash` (investida, esquiva, contra-ataque, `dash_in`).
- **Linha de visão:** atiradores (comum e especial) só atiram vendo; `MeleeRaycast`, `SlashProjectile` e
  `KaijuProjectile` param em bloco.
- **Esquiva/defesa:** especiais leem o kaiju (`ticksToImpact`, `isPreparingHeavy`): esquiva, contra-ataque, aparar,
  guarda contra golpe pesado, recuo com pouca vida (`tactics`).
- **Armas:** `ActionTimeline` (impacto único por ação), pentes, recarga em etapas recuando, arma branca sem
  munição, órbita/recuo do atirador especial.
- **Habilidades:** técnicas por prioridade/alcance/visão (só SLASH)/usos; kaiju por prioridade/alcance/visão (só
  à distância)/fase de chefe; aviso de golpe pesado (`KaijuTelegraph`, visual).
- **Perfis por forma:** 12 `special_soldier/*.json`, uma classe por forma (Hoshina/No. 10, Kikoru/No. 4,
  Reno/No. 6, Narumi/No. 1, Kafka/No. 8, Isao/No. 2, Mina); kaiju com uma espécie por forma (No. 9 e 5 formas,
  No. 10 pequeno/gigante).
- **Esquadrões:** IA-5 (ordens, postos, kaiju designado, separação).
- **Reaproveitável:** o parkour do jogador já mede obstáculos para qualquer `Entity`:
  `ParkourProbe.obstacleHeight/wallAt/wallSide` e `ParkourMath.classify/launchSpeedFor/peakHeight` (gravidade dos
  mobs).
- **Testes:** GameTests de IA (esquiva, contra-ataque, parede, recarga, travamento de kaiju, esquadrões, parkour) e
  o medidor `tools/ai/cenarios_ia.py` (22 cenários).

## 3. O que falta e bugs (arquivo:linha conferidos)

1. **Golpes corpo a corpo atravessam parede:** técnicas `combo`, contra-ataque e cauda do No. 10 medem só a
   distância (`HoshinaEntity.strike` 564–574, `HoshinaNo10Entity` 120–137); melee/slam/sweep/charge/pouso do kaiju
   idem (`KaijuEntity` 743–751, 822–840, 853–876; `KaijuAbilities` 134–152, 202–218). Explosões sem bloqueio por
   blocos (`SlashProjectile.explode` 189–213, `KaijuProjectile.explode` 171–181).
2. **Ninguém sai da área do aviso:** `KaijuTelegraph` é só partícula; a esquiva usa um raio fixo, não a forma real
   (anel, arco, linha).
3. **Saltos e avanços sem conferir o pouso:** `dash`, `gapClose`, `dash_in` e o `unstick` (que manda para a altura do
   alvo) empurram sem olhar parede, vão ou queda (`HoshinaEntity` 605–612, 685–697; `HoshinaCombatGoal` 279–285);
   salto de kaiju idem.
4. **Soldado comum:** sem pulo, sem destravar, sem esquiva (`SoldierCombatGoal`); só a navegação do vanilla.
5. **Nenhum soldado pula obstáculo nem atravessa plataforma** de propósito.
6. **Nenhuma cobertura:** ninguém procura obstáculo para se proteger; sem visão o atirador só anda até o alvo.
7. **Linha de tiro com aliado:** o tiro atravessa aliados sem feri-los; ninguém se reposiciona para abrir a linha.
8. **Técnica sem alvo:** se o alvo morre durante a técnica ela segue até o fim sem golpes; `startTechnique` não
   revalida visão/alcance; sem contador de falhas repetidas (personagem e kaiju).
9. **Esquiva só contra kaiju:** ignora projéteis, mobs vanilla e o rival do duelo; direção aleatória sem checar
   parede/queda.
10. **Troca de forma perde estado:** Kafka ↔ No. 8 perde líder/ordem/posto/kaiju designado do esquadrão, munição,
    usos de técnica e escalada (`HoshinaEntity` 460–484).
11. **Dano somado no mesmo tick:** vários pontos zeram `invulnerableTime` (necessário para combos), então cauda +
    técnica + contra-ataque no mesmo tick somam; com velocidade > 1 o laço do `tickTechnique` pode soltar mais de um
    golpe por tick. Não é "dano repetido do mesmo evento", mas precisa de teste.
12. **Log de IA:** só registra troca de alvo.
13. **Perfis "sem traje":** não existem para nenhum personagem (o plano pede). Só faz sentido onde a obra mostra o
    personagem lutando sem traje (Kafka humano já existe). **Decidido (Miguel, 2026-10-17):** "sem traje" = sem o
    traje kaiju (arma numerada); os pares já existem, falta só o Isao sem o Numbers 2 (tabela em
    `docs/PERFIS_PERSONAGENS_MOV5.md`).

## 4. Plano proposto (uma etapa por vez, aprovação entre elas)

| Etapa | Conteúdo | Arquivos principais |
|---|---|---|
| **MOV-1 Navegação confiável** | destravar no soldado comum (mesma lógica do especial, generalizada); validação de pouso/destino para `dash`, `gapClose`, `dash_in`, esquiva e `unstick` (chão sob o destino, sem parede, sem queda > 3); direção de esquiva escolhida entre esquerda/direita/trás pela que tem espaço; log de travamento | `SoldierCombatGoal`, `HoshinaCombatGoal`, `HoshinaEntity`, novo `core/soldier/LandingCheck` (JUnit) |
| **MOV-2 Saltos** | pulo tático de NPC sobre obstáculo de 1–3 blocos e para plataforma alcançável, reaproveitando `ParkourProbe`/`ParkourMath` (altura medida, velocidade de lançamento, pouso conferido, anula se o pouso for inválido); animação sincronizada (`soldier.action.vault`) | goal novo `SoldierLeapGoal`, animações por script |
| **MOV-3 Cobertura** | busca de cobertura limitada (amostra de blocos sólidos que cortam a visão do kaiju, pontuação pelo plano §5), atirador sai para atirar com visão e troca quando contornado/destruído; abandona cobertura dentro da área do aviso | `core/soldier/CoverScore` (JUnit), goal novo |
| **MOV-4 Combate dos soldados** | sair da área do aviso (anel/arco/linha reais do `KaijuTelegraph`); visão também para golpes corpo a corpo e técnicas `combo` (sem acertar através de parede); revalidar alvo/alcance no início e cancelar técnica se o alvo morrer; reposicionar quando aliado bloqueia a linha; esquiva também de projéteis | `HoshinaEntity`, `KaijuTelegraph` (expor a forma), `SoldierCombatGoal` |
| **MOV-5 Perfis de personagens** | um por vez (Hoshina, Kikoru, Mina, Kafka/No. 8; depois Reno, Narumi, Isao), ficha com fontes por forma, estado da troca de forma preservado; "sem traje" = sem o traje kaiju (Miguel, 2026-10-17) | `special_soldier/*.json`, fichas |
| **MOV-6 Kaiju** | golpes de kaiju com visão/bloqueio, pouso de salto validado, voo da Preondactyl desviando de obstáculos, contador de falhas por habilidade | `KaijuEntity`, `KaijuAbilities`, `PreondactylEntity` |
| **MOV-7 Integração** | regressão (medidor + duelos + matriz), desempenho, multiplayer | — |

Cenários novos no medidor para cada etapa: degrau de 2, muro de 3, plataforma, teto baixo, vão, corredor estreito,
cobertura destruída, vários soldados no mesmo caminho; métricas do plano §12 (saltos ok/falhos, pousos inválidos,
tempo parado, tiros bloqueados, dano duplicado = 0).

## 5. Riscos

- Pulo de NPC mexe na navegação do vanilla (o caminho pode ser recalculado no meio do salto): o goal novo segura o
  controle até pousar ou estourar o tempo.
- Visão para golpes corpo a corpo deixa kaiju e personagens mais fracos contra alvo atrás de quina: rodar a matriz
  de duelos e o medidor depois (sem mexer em vida/dano; se mudar, é [DECIDIR]).
- Cobertura custa processamento: busca só a cada N ticks, em raio pequeno, só para atiradores em combate.
- Separar mudança de IA de balanceamento: nenhuma etapa MOV mexe em número de dano/vida.

## 6. MOV-1 feita (2026-10-16)

- `core/soldier/LandingMath` (JUnit) e `common/soldier/MoveSafety`: pouso conferido no mundo (passos de 0,5, sobe 1
  bloco, para na parede, queda até `landingMaxDrop`), direção de esquiva entre direita/esquerda/trás pela que tem chão
  (`dodgeDirection`) e ponto em pé (`standable`).
- `HoshinaEntity.dashSafe`: dash, `gapClose`, `dash_in`, contra-ataque (lados e avanço) e passo do Kasumi-uchi só
  com pouso válido; esquiva sem espaço não sai (e espera a próxima reação).
- `StuckWatch` em `SoldierCombatGoal` e `HoshinaCombatGoal` (comum e especial): travado por `stuckTicks` → pula
  obstáculo baixo ou anda `unstuckStep` para um lado com chão; travado de novo no mesmo lugar → volta. O `unstick` do
  especial também confere o chão dos dois lados.
- **Achado no cenário `beco`:** soldado de espada/faca contra kaiju alto não acertava (o golpe mirava o centro da
  caixa, fora do alcance) → mira no ponto mais perto da caixa do alvo (`SoldierEntity.nearestInside`). Armas de fogo
  continuam no centro.
- Log: `AiTuning.logMove` com `[kaijuAi] debugLog`.
- Medidor: cenários novos `beco` e `degraus` (kaiju morto nos dois); `parede` 99 de 220 (faixa de antes), `corredor`,
  `abrigo` e `grupo` sem regressão. GameTests 189/189 (4 novos em `MobilityGameTests`).
- Visto em jogo com 2 clientes: `docs/img/mov1_pouso_e_golpe_de_perto.jpg`.

## 7. MOV-2 feita (2026-10-16)

- `core/soldier/LeapMath` (JUnit 5): física do mob no ar (gravidade 0,08, freio 0,98 vertical e 0,91 horizontal),
  impulso vertical que passa da altura, tempo até pousar, impulso horizontal que cobre a distância, viabilidade
  (até 0,7 por tick).
- `common/soldier/SoldierLeap` (no `StuckWatch`, comum e especial): obstáculo de 1,2 a 3 a frente → pousa em cima
  ou atrás dele; sem obstáculo → vão/plataforma depois da beira (até 1,5 bloco de corrida), primeiro o outro lado,
  depois o fundo; pouso conferido (`MoveSafety.restsAt`), 1 bloco para dentro da quina, arco sem teto. Duas fases
  como o vault do jogador (sobe, depois vai para a frente); no ar segura o controle até pousar, sem dano de queda.
  Dispara quando o soldado quer andar e fica quase parado por `leapCheckTicks`, ou no travamento.
- `SoldierEntity.getMaxFallDistance` = `landingMaxDrop`: o caminho não se joga em quedas maiores (achado no cenário
  `vao`).
- Animação `<espécie>.action.vault` (corpo e pernas; braços ficam na postura da arma) por
  `tools/art/gen_vault_animations.py` em soldado e nos 11 especiais.
- Medidor: `plataforma` 2 soldados × Primigenius parado em cima de 2 blocos: sem salto nenhum dano em 30 s, com salto
  morto em 12 s; `vao` (3 de largura, 5 de fundo): antes pulavam no fundo e ficavam presos, agora atravessam e matam
  em 12 s (3 rodadas iguais); `plataforma_especiais` (Hoshina e Kikoru × Honju) 8 s; `beco`, `degraus`, `corredor`,
  `abrigo`, `grupo` sem regressão; `parede` varia muito por rodada (46–146 de dano com ou sem salto). GameTests
  192/192 duas vezes.
- Visto em jogo com 2 clientes: `docs/img/mov2_saltos_plataforma_e_vao.jpg` (a pose do salto aparece pequena nas
  capturas; conferir de perto no roteiro §56).

## 8. MOV-3 feita (2026-10-16)

- `core/soldier/CoverScore` (JUnit 3): só vale ponto onde um bloco corta a visão; recusa área de golpe pesado e kaiju
  perto demais; pontos por ponto de tiro ao lado, distância perto da de manter, pouco caminho, sem aliado no mesmo
  lugar e bloco resistente (vidro conta pouco).
- `common/soldier/CoverFinder`: 4 anéis × 12 direções até `coverRadius`, chão pelo `MoveSafety.standable`, visão do
  kaiju até os olhos **e** o peito do soldado pelo mesmo teste de colisão dos tiros (nada através de parede), ponto
  de tiro a 1,5 bloco para o lado com linha até o kaiju, coberturas dos aliados contam como ocupadas.
- `ShooterCover` no `SoldierCombatGoal` (atirador comum): contra kaiju com projétil fica no ponto de tiro e volta para
  trás da cobertura quando o kaiju prepara um ataque contra ele (`coverHideTicks`) e para recarregar; contra
  qualquer kaiju recarrega atrás de cobertura; confere a cada `coverCheckTicks` (contornada, quebrada, kaiju a menos
  de `coverMinThreatDistance`, área de golpe pesado) e troca; ainda escondendo mas sem ponto de tiro, fica com ela até
  achar outra (sem trocar a cada conferida).
- Soldados especiais atiradores (Mina, Reno) **não** usam cobertura nesta etapa: a IA de atirador deles circula o
  alvo para distrair (IA de 2026-10-15); entra nos perfis da MOV-5 [DECIDIR].
- Medidor, cenário `cobertura` (2 atiradores normais × Camponotus, 5 muros): com cobertura ~83 de dano na formiga e
  2 de 5 rodadas com sobrevivente aos 60 s; sem cobertura ~89 e 1 de 5. **Neutro** contra kaiju que avança para o
  corpo a corpo (a formiga quebra os muros ao passar); o ganho é contra ataque a distância, e o kaiju comum não
  escolhe tiro sem ver o alvo. Com `coverMinThreatDistance` 6 ficava pior (40–75 de dano, mortes em 15–21 s): os
  soldados ficavam na cobertura até a formiga chegar → 9. Regressão (`corredor`, `abrigo`, `grupo`, `parede`,
  `separacao`, `municao_alvo`) sem perda. GameTests 194/194.
- Visto em jogo com 2 clientes: `docs/img/mov3_cobertura.jpg`.

## 9. MOV-4 feita (2026-10-16)

- `core/kaiju/DangerZone` (JUnit 5): disco (pisão, pouso do salto), setor (soco, varredura) e faixa (investida, tiro)
  com `contains` e saída pelo caminho mais curto (de lado na faixa e perto da borda reta do setor).
  `KaijuTelegraph.zone` monta a área com as mesmas medidas do aviso; `KaijuEntity.dangerZone()` só durante o preparo
  de golpe pesado avisado.
- `DangerAvoid`: soldado comum e especial saem andando da área (`exitSpot`: chão conferido, direção mais curta e
  vizinhas); a esquiva do especial sai pela direção da área, e estar dentro dela conta como ameaça.
- Golpe de perto dos especiais (técnica, contra-ataque, cauda do No. 10) só com caminho livre até o alvo
  (`clearReach`); técnica corpo a corpo não é escolhida através de parede. O soldado comum já parava no bloco
  (`MeleeRaycast`).
- Técnica cortada (e a animação, `stopTriggeredAnim`) quando o alvo morre no meio.
- Atirador comum (também na cobertura) não atira com aliado (soldado ou jogador) na linha: passo de 3 para o lado.
- Especiais esquivam de projétil de kaiju que vai acertar em até `react_ticks` (ponto mais próximo da trajetória).
- Medidor, cenário `pisao` (3 soldados de espada × Primigenius Honju): com a saída 4/4 rodadas de ~15 s e o Honju
  perdendo 251–259; sem ela ~3 s e −43 (a investida pega os três). Duelos de referência (servidor dedicado, formas do
  No. 9/No. 10 isoladas pelo datapack): Hoshina × No. 10 pequeno 5/5 (7–50% da vida; antes 4–45%), Hoshina × No. 10
  gigante 0/5 (gigante com 46–67%), Narumi × No. 10 gigante 0/5 (20–48%; antes 18–55%) — sem mudança relevante.
  **Kikoru × No. 9: 0/5 com e sem a saída da área** (meta 25%; no v1.2 era 5/10): a queda não vem da MOV-4 de sair
  da área; causa não isolada (pode ser o golpe sem atravessar parede ou outra mudança desde o v1.2) [DECIDIR] no
  perfil dela (MOV-5). GameTests 200/200.
- Visto em jogo com 2 clientes: `docs/img/mov4_saindo_da_area_do_pisao.jpg` (anel do pisão no chão e o soldado na
  borda de fora).

## 10. MOV-5A feita (2026-10-17): troca de perfil Kafka ↔ Kaiju No. 8

- Matriz de perfis e sistema de troca em `docs/PERFIS_PERSONAGENS_MOV5.md` (uma seção por personagem nas próximas
  etapas).
- Bug 10 corrigido: `HoshinaEntity.inheritForm` (na troca) passa líder, ordem, posto e kaiju designado do esquadrão,
  dados persistentes (invasão, duelo), usos de técnica e o lugar no esquadrão da invasão
  (`InvasionService.replaceDefender`); arma e munição da forma que saiu ficam na memória de formas (`kn8_form_memory`,
  salva) e voltam com ela. Técnicas, atributos e escalada não passam (cada forma só com o próprio perfil).
- Perfil ativo no log (`perfil ativo kn8:kaiju_no8 (antes kn8:kafka)`) e em `activeProfileId()`.
- GameTest `formSwitchKeepsSquadAndAmmoWithoutSharingAbilities` (ida e volta: esquadrão, posto, kaiju designado,
  marca da invasão, pente e reserva do Kafka, nenhuma técnica cruzada, No. 8 não atirador). GameTests 201/201.
- Em jogo com 2 clientes (`docs/img/mov5a_kafka_no8_troca_de_forma.jpg`): Kafka no esquadrão do Dev vira o No. 8, que
  segue o Dev, mata o Primigenius e volta a ser o Kafka 20 s depois, no esquadrão, com 30 + 114 balas (a reserva que
  sobrou, não 120).

## 11. MOV-5B feita (2026-10-17): perfil do Hoshina

- Ficha do Hoshina e do Hoshina No. 10 em `docs/PERFIS_PERSONAGENS_MOV5.md`.
- Brecha: `KaijuEntity.isOpen()` (janela parada depois do golpe pesado, marcada pelo `KaijuCombatGoal`, ou atordoado);
  `tactics.exploit_openings` dá bonus à técnica de maior dano total e o goal pressiona em vez de reposicionar.
- Bug 11 (parte das técnicas): no máximo um golpe por tick no `tickTechnique`; a técnica só termina depois de todos os
  golpes.
- Decisão de uso (plano, parte B): combo não começa se o alvo vai estar fora do alcance no primeiro golpe
  (`core/soldier/TechniqueMath`, JUnit); combo que não acerta nada soma `tactics.miss_cooldown_ticks` à recarga.
- GameTests 205/205 (4 novos), duelos e jogo com 2 clientes em `docs/PERFIS_PERSONAGENS_MOV5.md`.

## 12. MOV-5C feita (2026-10-17): perfil da Kikoru

- Ficha da Kikoru e da Kikoru No. 4 em `docs/PERFIS_PERSONAGENS_MOV5.md`.
- Bloco novo `rhythm` no `special_soldier` (`SpecialSoldierDef.Rhythm`): `alternate_bonus` (alternar técnica rápida e
  pesada) e `press_regeneration` (alvo regenerando: técnica que acerta mais cedo e sem reposicionar;
  `No9Service.regenerating`).
- [DECIDIR] da MOV-4 resolvido: a Kikoru perdia para a regeneração do No. 9 (parava em 21–38%). Ritmo novo + `kaiju_damage`
  0,64: 3/10 (meta 25%).
- GameTests 207/207 (2 novos), duelos e jogo com 2 clientes na ficha.

## 13. MOV-5D feita (2026-10-17): perfil da Mina

- Ficha da Mina em `docs/PERFIS_PERSONAGENS_MOV5.md`.
- Validação final do tiro (plano, "ciclo de vida"): `HoshinaEntity.shotInvalid` no disparo (linha de tiro, alcance) e
  no preparo dos atiradores (`min_range`); `cancelTechnique` devolve o uso limitado e deixa a técnica pronta depois do
  preparo. Vale para todo especial (Hoshina, Reno...).
- `ranged.reserve_limited` (Mina): Anti-Giant guardado para alvo grande.
- Decidido (Miguel, 2026-10-18): Mina × No. 9, aceito que ela vença; cobertura de Mina/Reno adaptativa (§14).

## 14. Cobertura adaptativa dos atiradores especiais (2026-10-18)

- Miguel: "ela deve se ajustar de acordo com o combate". O `ShooterCover` (MOV-3) agora serve também o
  `HoshinaCombatGoal`: com o alvo até `keep_distance` + 3; contra kaiju que atira só sob pressão
  (`ranged.cover_health`) e sem janela para pressionar (alvo regenerando ou de guarda aberta); para recarregar, sempre;
  escondido não começa técnica. Fora disso, recuo e órbita como antes. `[soldierAi] specialCover`.
- Correção que vale também para o soldado comum: esconder-se do kaiju que mira dura até o impacto +
  `coverHideTicks` (antes renovava na recuperação e, contra ataques em sequência, o atirador nunca saía).
- Medidas: Mina × No. 9 4/10 e × No. 10 pequeno 4/10 (cobertura desligada: 6/10 e 6/10; diferença dentro do ruído de 10 duelos, com o chão refeito a cada duelo); Reno No. 6 × No. 9 0/3 com e sem cobertura (era 6/6 no v1.2: queda anterior a esta mudança, fica para a etapa do Reno); cena da formiga com muros e cena do Primigenius em `docs/img/mov5d_cobertura_adaptativa.jpg`.

## 15. MOV-5E feita (2026-10-18): perfil do Reno

- Ficha do Reno e do Reno No. 6 em `docs/PERFIS_PERSONAGENS_MOV5.md`.
- Kaiju paralisado (Lentidão no nível `[kaijuAi] paralyzedSlowLevel` ou acima) conta como brecha (`isOpen`): vale para
  todos os especiais que exploram brecha e para a cobertura adaptativa (alvo paralisado: pressionar).
- Atirador especial atira enquanto sai da área de um golpe pesado (MOV-4 deixava de atirar).
- Reno No. 6 `kaiju_damage` 0,80 [SUPOSIÇÃO]; diagnóstico da queda de 9/10 na ficha.

## 16. MOV-5F feita (2026-10-18): perfil do Narumi

- Ficha do Narumi e do Narumi No. 1 em `docs/PERFIS_PERSONAGENS_MOV5.md`.
- Previsão do Numbers 1 (`rhythm.read_windup`): com o kaiju no preparo, técnica que acerta antes do impacto tem
  prioridade; contra golpe mirado nele, a que acertaria depois não começa; sem reposicionar durante o preparo. Vale
  para qualquer perfil que tiver o bloco (hoje só o No. 1).

## 17. MOV-5G feita (2026-10-18): perfil do Isao

- Ficha do Isao e do Isao sem o Numbers 2 em `docs/PERFIS_PERSONAGENS_MOV5.md`; o perfil sem o traje kaiju que
  faltava (`isao_base`, 520/20/0,29) existe agora, com entidade, ovo e variante próprios.
- Núcleo exposto conta como brecha (`KaijuEntity.isOpen`): todo especial que explora brecha pressiona depois do
  Thunder Emperor, do gelo do Reno ou do Juni-hitoe.
- Com isso a MOV-5 fecha os perfis de todos os personagens (Kafka/No. 8, Hoshina, Kikoru, Mina, Reno, Narumi, Isao).


## 18. Decisões do Miguel (2026-10-22)

- Fuga do No. 9 conta como empate na matriz de duelos (antes vitória).
- Isao entre os mais fortes: numerado com o núcleo exposto não regenera (`[kaijuAi] exposedCoreStopsRegeneration`,
  regra de regeneração dinâmica do v1.2) e o Thunder Emperor expõe por 120 ticks: Isao × No. 10 gigante 0/5 → 7/10.
- Narumi No. 1 mais forte que o Narumi: `kaiju_damage` 1,4 (era 0,85, abaixo do 1,3 do Narumi) e explora brechas.
- Modelos próprios do Narumi No. 1 e do Isao sem o Numbers 2 ficam para depois. Próximas etapas: MOV-6 (kaiju) e MOV-7.
