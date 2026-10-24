# PM-12 — Integração final do Plano Mestre (2026-10-24)

Fecha o Plano Mestre de Ideias (`docs/PLANO_MESTRE_IDEIAS_KAIJU_N8_REBORN.md`, Parte V item 10 e Parte VI): os
sistemas das etapas PM-1 a PM-11 rodando juntos, com o mesmo estado no servidor, na HUD e no save. Nenhum sistema
novo: só testes de integração, um cenário em jogo com os dois clientes, medição de desempenho e duas correções
pequenas achadas no caminho.

## 1. Plano de testes da Parte VI

TESTADO = conferido nesta etapa ou na etapa indicada (GameTest e/ou em jogo com servidor dedicado + 2 clientes).

| # | Item | Estado | Como |
|---|---|---|---|
| 1 | Build e inicialização (1.21.1 + NeoForge 21.1.252) | TESTADO | `gradle build`, servidor dedicado e 2 clientes subindo; GameTests 287/287 em 3 rodadas seguidas |
| 2 | Armas: básico, especiais, recargas, munição, troca, dano sem duplicar | TESTADO | PM-1 (`SpecialAttackGameTests`, `WeaponProfileGameTests`), PM-11 (`NumberedGearGameTests`); em jogo o Force Blast e os cortes no Honju |
| 3 | Esquiva: tempo certo/errado, contra-ataque, golpe não bloqueável, multiplayer | TESTADO | PM-1 (`CombatDefenseGameTests`, esquiva perfeita uma vez e só na janela); golpe pesado sem bloqueio desde a IA-2 |
| 4 | IA adaptativa: repetição, reset, vários inimigos, sem laço | TESTADO | PM-3 (`KaijuHabitGameTests`; espera entre leituras, alvo novo esquece) |
| 5 | Ferimentos: limiar, teto, efeito, save/reload | TESTADO | PM-2 (`KaijuWoundGameTests`, salva e carrega antes de ferir) |
| 6 | Terreno: obstáculo, altura, rota impossível, limite de destruição | TESTADO | PM-4, MOV-1/2/6 (`KaijuGroupGameTests`, `MobilityGameTests`, `KaijuMobilityGameTests`) |
| 7 | Hierarquia: papéis, morte do líder, distâncias, atacantes simultâneos | TESTADO | PM-4 (líder passa o alvo; morte dele faz recuar/enfurecer; no máximo 3 colados) |
| 8 | Chefes: troca de fase, morte, reload, dano simultâneo, recompensa única | TESTADO (recompensa única por inspeção) | PM-5 (`BossAiGameTests`, fase salva sem repetir); a recompensa sai do `LivingDeathEvent`, que o vanilla dispara uma vez (`LivingEntity.die` confere `dead`); o jogador falso do GameTest não está na lista de jogadores, então o mérito do chefe com dois atacantes não dá para medir em GameTest |
| 9 | Esquadrões: ordens, munição, alvo inválido, navegação, reload, limite | TESTADO | IA-5, PM-6 (`SquadOrdersGameTests`, foco salvo e volta a seguir), PM-8 (estação de preparação); em jogo 4 soldados do Dev seguindo e lutando |
| 10 | Promoções: requisitos, métricas, falha, repetição, saves antigos | TESTADO | PM-7 (`ExamGameTests`, save antigo carrega zerado) |
| 11 | Base: interações, requisitos, custos, integração | TESTADO | PM-8, 8B, 8C, 8D (`BaseGameTests`, `ExamCourseGameTests`, `ResearchGameTests`, `PlayerBaseGameTests`) |
| 12 | Numerados: compatibilidade, domínio, desbloqueio, equipamento antigo | TESTADO | PM-11A/B (`NumberedGearGameTests`); em jogo o traje com cauda ganhou domínio na luta e manteve depois de reiniciar e de morrer |
| 13 | Avaliação: métricas, multiplayer, recompensa única | TESTADO | **Novo:** `twoPlayersSameMissionEachGradedOnce` (dois jogadores, cada um com a própria nota, sem recompensa repetida); em jogo Dev (nota S) e Dev2 (nota A, em inglês) na mesma região ao mesmo tempo |
| 14 | Alertas regionais: transições, recuperação, persistência, várias ameaças | TESTADO | PM-9/10A (`RegionGameTests`); em jogo EVACUAÇÃO → COMBATE (Honju) → RECUPERAÇÃO com duas missões e dois kaiju no mesmo local |
| 15 | Evacuação: sucesso, falha, expiração, desconexão, reload | TESTADO (desconexão por inspeção) | PM-10A + **novo** `evacuationTimesOutAndClearsCivilians` (prazo estourado fracassa e leva os civis); civil com dono fora do servidor só espera (`civilianTick` sai sem o dono) e conta de novo quando ele volta |
| 16 | Apoio militar: válida/inválida, espera, custo, sem alvo, spam | TESTADO | PM-10B + **novo** `supportSpamIsRefusedWithoutSpending` (duas chamadas pelo rádio no mesmo instante: a segunda é recusada e nada é gasto); em jogo reconhecimento e bombardeio (2 kaiju atingidos) |
| 17 | Rastros: criação, limite, expiração, reload, proteção de construções | TESTADO | PM-9 (`TrailGameTests`; o rastro não mexe em bloco nenhum); em jogo 11 pistas, mérito uma vez |
| 18 | Desempenho: sem varredura global nem criação sem limite | TESTADO | Seção 3 abaixo |

**Novo GameTest de save:** `careerWithEveryPlanFieldSurvivesSave` (pesquisa, domínio, melhor nota, missão ativa com as
medidas de evacuação, tudo junto no mesmo codec da carreira).

## 2. Cenário em jogo (Shinagawa, `world_c3c`, servidor dedicado + 2 clientes)

Dev (Vice-Capitão, traje Numbers 10, Manoplas Numbers 2, Rádio de Campo, Rastreador) com 4 soldados no esquadrão, na
missão **Evacuação sob Honju**; ao mesmo tempo Dev2 (Oficial, en_us) na **Evacuação do Bairro**, no mesmo local.

- Reconhecimento pelo rádio: os dois kaiju marcados através das paredes, contato no chat.
- Faixa da região: EVACUAÇÃO → COMBATE (Primigenius Honju) → RECUPERAÇÃO, igual nos dois clientes e na aba Alertas.
- Bombardeio: "2 kaiju atingidos", Honju 360 → 334.
- Honju derrubado pelo esquadrão e pela cauda do traje; os civis levados ao ponto.
- Avaliações: Dev **S** (6 civis, 0 perdidos, mérito extra), Dev2 **A** (4 civis, 1 perdido), cada um com a sua.
- Rastreador: rastros do Honju e do Primigenius ("o kaiju caiu aqui"), 11 pistas, +22 de mérito uma vez.
- Servidor reiniciado e Dev morto e renascido: patente, mérito, pesquisa, domínio do traje (51) e melhores notas iguais.

Imagem: `docs/img/pm12_integracao_shinagawa.jpg`.

## 3. Desempenho

- Servidor durante a luta inteira (2 jogadores, 4 soldados, Honju, Primigenius, 13 civis, cauda, rádio, região):
  **20 TPS, 2,2-5,1 ms/tick** (`/neoforge tps`); antes da luta 1,7-4,0 ms/tick.
- Ticks dos sistemas do Plano Mestre (lidos no código): missões e pesquisa a cada 20 ticks por jogador; região a cada
  `[region] kaijuCheckTicks` 40 por kaiju e uma vez por segundo por mundo (só com região fora do normal); civis a cada
  10 ticks; rastro sem tick (só em evento); apoio só na chamada; asas e cauda por jogador com equipamento numerado.
- **Corrigido:** a cauda do Numbers 10 procurava alvo a cada tick quando estava pronta e sem kaiju perto; agora espera
  10 ticks antes de procurar de novo.

## 4. Correções desta etapa

1. Cauda do Numbers 10 sem busca a cada tick (acima).
2. Dica da avaliação: a nota soma civis aos aliados perdidos; quando as perdas foram de civis a dica falava do
   esquadrão ("use Recuar ou Guarda") para quem nem tinha esquadrão. Agora há a dica própria dos civis
   (`kn8.mission.eval.tip.civilians`).

## 5. Observações (sem mudança)

- Na luta, os socos das manoplas do Dev quase não acertaram o Honju de 8,5 m (o domínio do Numbers 2 não subiu; o do
  traje subiu com a cauda): a manopla tem 3 de alcance contra a caixa grande do Honju, olhando para os olhos dele.
  É o alcance do JSON, não falha de integração [SUPOSIÇÃO: talvez o alcance das manoplas deva ser maior].
- A faixa da região mostra uma causa por vez (a primeira que levou ao estado); com duas evacuações no mesmo local
  aparece só o nome de uma delas.
