# MOV-7 — Integração e regressão geral (plano de mobilidade v3, 2026-10-22)

Fecha o plano de mobilidade e combate (MOV-0 a MOV-7). Nenhum código de jogo mudou nesta etapa: só medição. Tudo no
servidor dedicado; a invasão, também com os 2 clientes.

## 1. Testes automáticos

- `gradle build` (JUnit) passa.
- GameTests **224/224** em 4 rodadas desde a MOV-6 (uma falha isolada do `invasionRunsWavesAndBreaks` na primeira,
  já conhecida: kaiju carregado tarde; não se repetiu).

## 2. Medidor (`tools/ai/cenarios_ia.py`, 31 cenários, 40 s cada)

| Cenário | Resultado | Leitura |
|---|---|---|
| solda, formiga, formiga_s, aberto, narumi, no8 | kaiju derrubado ou quase em 5–11 s | sem regressão |
| so_formiga, formiga_aberta, formiga_larga | kaiju parado (alvo é suporte de armadura) | esperado: sem alvo e sem jogador perto o mob não passeia (IA Fase 0) |
| parede | kaiju 220 → 116, 0 de dano sem visão | sem regressão (faixa de 70–113) |
| corredor, alto, grupo, cerco, separacao, reno_cobre | vencidos; aliados colados ≤ 3% | sem regressão |
| alto_preso | kaiju desiste do soldado no pilar e mata o outro | esperado (IA-1A) |
| preondactyl, voo_torres | raio derruba soldados normais; 0 batida nas torres | MOV-6 ok |
| municao, municao_alvo | recargas e faca; No. 10 gigante com `NoAI` de propósito (alvo) | esperado |
| abrigo | casa aberta e soldados alcançados | sem regressão (6–9 s) |
| beco, degraus, plataforma, plataforma_especiais, vao | kaiju morto, soldados sem dano | MOV-1/MOV-2 ok |
| cobertura | formiga 197 → 77, um atirador a 12 | sem regressão |
| pisao | 15 s, Honju 360 → 101 | igual à MOV-4 (−255) |
| muro_kaiju | 0 de dano através do muro de bedrock | MOV-6 ok |
| pilares | Honju morto no 1º tiro do Anti-Giant em 2,5 s | esperado (registrado na auditoria da IA) |

## 3. Matriz de duelos (5 por confronto, fuga = empate)

Script `duel12e.py` (scratchpad): chão refeito a cada duelo, formas do No. 9 isoladas (pacote de dados do mundo sem
`transform`/`absorb`). V = morte do kaiju; E = fuga (No. 9 e formas a 15%) ou 5 min sem fim; D = personagem morto.
A coluna "antes" é a matriz do v1.2 (10 duelos, com a fuga contando como vitória) ou a última medida.

| Confronto | Meta v1.2 | Antes | Agora V / E / D | Observação |
|---|---|---|---|---|
| Hoshina × No. 9 | 70% | 100% (só fuga) | 0 / 5 / 0 | todas por fuga, No. 9 a 14–16% |
| Hoshina × No. 10 pequeno | 58% | 80% | 4 / 1 / 0 | o empate foi o No. 10 sumindo com 2% (ver 3.1): na prática 5/5 |
| Hoshina × No. 10 gigante | 10% | 0% | 0 / 0 / 5 | gigante com 18–34% |
| Hoshina No. 10 × No. 9 | 90% | 100% (só fuga) | 0 / 5 / 0 | |
| Hoshina No. 10 × No. 10 gigante | 60% | 100% | 5 / 0 / 0 | |
| Hoshina No. 10 × No. 9 preto | 45% | 40% | 0 / 5 / 0 | |
| Hoshina No. 10 × No. 9 formiga | 35% | 20% | 0 / 5 / 0 | |
| Hoshina No. 10 × fusão | 18% | 0% | 0 / 0 / 5 | fusão com 68–90% |
| Kaiju No. 8 × No. 10 pequeno | 75% | 90% | 5 / 0 / 0 | |
| Kaiju No. 8 × No. 10 gigante | 45% | 70% | 1 / 4 / 0 | 3 empates por tempo (5 min) e 1 sumiço com 0% (na prática 2 vitórias) |
| Kaiju No. 8 × No. 9 | 40% | 100% (só fuga) | 0 / 5 / 0 | |
| Kaiju No. 8 × No. 9 preto | 20% | 70% | 0 / 5 / 0 | 4 fugas, 1 por tempo |
| Kaiju No. 8 × No. 9 formiga | 15% | 100% (só fuga) | 0 / 5 / 0 | |
| Kaiju No. 8 × fusão | 8% | 20% | 0 / 5 / 0 | 5 min sem fim, fusão a 46–50% |
| Kikoru × No. 9 | 25% | 3/10 (MOV-5C) | 0 / 4 / 1 | |
| Kikoru × No. 10 pequeno | 35% | 0% | 0 / 0 / 5 | igual ao v1.2 |
| Kikoru No. 4 × No. 9 | 75% | 80% | 0 / 5 / 0 | |
| Mina × No. 10 gigante (40 blocos) | 30% | 4/5 (MOV-5D) | 4 / 1 / 0 | um empate com o gigante sumindo a 24% (ver 3.1) |
| Mina × No. 9 (40 blocos) | — | 4/10 (MOV-5D) | 0 / 3 / 2 | |
| Reno × No. 9 | 12% | 0% | 0 / 0 / 5 | igual ao v1.2 |
| Reno No. 6 × No. 9 | 70% | 5/8 (MOV-5E) | 0 / 5 / 0 | |
| Narumi × No. 9 | 35% | 60% | 0 / 5 / 0 | |
| Narumi No. 1 × No. 9 | 85% | 100% (só fuga) | 0 / 5 / 0 | em 16–18 s |
| Narumi × No. 13 | — | 4/5 | 4 / 1 / 0 | o empate foi o No. 13 sumindo com 0% (ver 3.1) |
| Narumi No. 1 × No. 13 | — | 5/5 | 5 / 0 / 0 | |
| Isao × No. 9 | 30% | 5/5 (só fuga) | 0 / 5 / 0 | |
| Isao × No. 10 pequeno | — | 5/5 | 5 / 0 / 0 | |
| Isao × No. 10 gigante | 60% | 7/10 (decisões de 2026-10-22) | 3 / 0 / 2 | |
| Isao sem o Numbers 2 × No. 10 pequeno | 45% | 2/5 (MOV-5G) | 1 / 0 / 4 | |

**Leitura:** nenhum confronto piorou por causa das etapas de mobilidade (MOV-1 a MOV-6); os resultados ficam na
faixa das medidas anteriores. A mudança grande é a da decisão de 2026-10-22: **com a fuga contando como empate,
nenhum personagem vence o No. 9 ou as formas dele em duelo solo** (todos chegam aos 15% e o No. 9 foge). As metas do
v1.2 para o No. 9 foram escritas contando a fuga; com a regra nova elas precisam ser relidas [DECIDIR].

### 3.1 Sumiço sem carcaça

O script conta "fuga" quando o kaiju some sem deixar carcaça. No. 10 e No. 13 não fogem; em 3 casos sumiram com 0–2%
(morte com a carcaça ainda não achada no instante da leitura: são vitórias). No caso da Mina × gigante, ele sumiu com
24% (causa não achada; os 4 outros duelos foram vitórias). Fica anotado para o próximo ajuste do script.

## 4. Desempenho (`tools/ai/carga_ia.py 40 40 30`: 40 kaiju + 40 soldados lutando)

| Condição | ms por tick (média) |
|---|---|
| Mundo de teste com 1.416 chunks forçados acumulados pelos cenários e duelos | 16,1–16,2 |
| Sem esses chunks, regras da MOV ligadas | 11,2–14,4 |
| Sem esses chunks, regras da MOV desligadas (cobertura, área do golpe, caminho livre, salto, voo, falhas) | 11,2–12,7 |
| Referência da IA-7 | 9–10 |

JFR durante a carga: o código do mod aparece em **5,8%** das amostras da thread do servidor (IA-7: 6,4%); o resto é
vanilla (gerenciamento de chunks, movimento, colisão). As regras de mobilidade não têm custo mensurável (ligadas e
desligadas ficam dentro do ruído). A diferença para a IA-7 vem do estado do mundo de teste.

## 5. Multiplayer (servidor dedicado + 2 clientes)

Invasão `honju_assault` completa com o Dev (pt_br) e o Dev2 (en_us) olhando de cima (`docs/img/mov7_invasao_dois_clientes.jpg`):
alerta nos dois idiomas, primeira onda eliminada, segunda onda, **vitória em 74 s**. 20 TPS o tempo todo (2,9–6,4 ms por
tick). Nenhum erro do mod no servidor nem nos clientes (só a falta de rede da autenticação e de dispositivo de som
desta máquina).

## 6. Pendências para o Miguel

- [DECIDIR] Metas do No. 9 com a fuga = empate: ninguém vence o No. 9 em duelo solo. Manter assim (vitória só em grupo
  ou matando antes dos 15%) ou baixar a fuga do No. 9 / aumentar a pressão de quem o enfrenta?
- Kikoru × No. 10 pequeno (meta 35%: 0/5), Reno × No. 9 (meta 12%: 0/5), Hoshina No. 10 × fusão (meta 18%: 0/5),
  Isao sem o Numbers 2 × No. 10 pequeno (meta 45%: 1/5): fora da meta desde o v1.2, não por causa da mobilidade.
