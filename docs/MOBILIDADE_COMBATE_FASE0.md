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
    personagem lutando sem traje (Kafka humano já existe). **[DECIDIR]**.

## 4. Plano proposto (uma etapa por vez, aprovação entre elas)

| Etapa | Conteúdo | Arquivos principais |
|---|---|---|
| **MOV-1 Navegação confiável** | destravar no soldado comum (mesma lógica do especial, generalizada); validação de pouso/destino para `dash`, `gapClose`, `dash_in`, esquiva e `unstick` (chão sob o destino, sem parede, sem queda > 3); direção de esquiva escolhida entre esquerda/direita/trás pela que tem espaço; log de travamento | `SoldierCombatGoal`, `HoshinaCombatGoal`, `HoshinaEntity`, novo `core/soldier/LandingCheck` (JUnit) |
| **MOV-2 Saltos** | pulo tático de NPC sobre obstáculo de 1–3 blocos e para plataforma alcançável, reaproveitando `ParkourProbe`/`ParkourMath` (altura medida, velocidade de lançamento, pouso conferido, anula se o pouso for inválido); animação sincronizada (`soldier.action.vault`) | goal novo `SoldierLeapGoal`, animações por script |
| **MOV-3 Cobertura** | busca de cobertura limitada (amostra de blocos sólidos que cortam a visão do kaiju, pontuação pelo plano §5), atirador sai para atirar com visão e troca quando contornado/destruído; abandona cobertura dentro da área do aviso | `core/soldier/CoverScore` (JUnit), goal novo |
| **MOV-4 Combate dos soldados** | sair da área do aviso (anel/arco/linha reais do `KaijuTelegraph`); visão também para golpes corpo a corpo e técnicas `combo` (sem acertar através de parede); revalidar alvo/alcance no início e cancelar técnica se o alvo morrer; reposicionar quando aliado bloqueia a linha; esquiva também de projéteis | `HoshinaEntity`, `KaijuTelegraph` (expor a forma), `SoldierCombatGoal` |
| **MOV-5 Perfis de personagens** | um por vez (Hoshina, Kikoru, Mina, Kafka/No. 8; depois Reno, Narumi, Isao), ficha com fontes por forma, estado da troca de forma preservado; [DECIDIR] perfis "sem traje" | `special_soldier/*.json`, fichas |
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
