# PLANO_0_6.md — Mobs melhores, numerados novos e defesa de pontos (pedido do Miguel, 2026-10-07)

Base: `docs/ESPECIFICACAO_HABILIDADES_MOBS.md` (texto do Miguel: habilidades do Hoshina, No. 10, No. 9,
Primigenius, Trichonephila e Preondactyl). Uma etapa por vez, cada uma testada com servidor dedicado + 2 clientes.

| Etapa | O que entra | Depende de modelo? |
|---|---|---|
| **0.6-A** | Mais ataques nos kaiju atuais: varredura (cauda/pernas), projetil (raio de energia, teia, Finger Gun), salto, combo de varios golpes; escolha por distancia e prioridade; aviso (telegraph) antes dos golpes fortes; furia do revivido. Soldados sem empurrar o kaiju (feito). | Nao (animacoes geradas nos ossos atuais) |
| 0.6-B | **Trichonephila Honju** (modelo recebido): rig de aranha, entidade, chefe que invoca Trichonephila, invasao/missao | Recebido |
| 0.6-C | **Trajes 3D** Mk1 e Mk1 Reforcado (modelos recebidos) no jogador e no soldado (`GeoArmorRenderer`) | Recebidos |
| 0.6-D | **Hoshina** (soldado especial, aura roxa): rig do modelo recebido, espadas duplas, Kuuchi, Kosa-uchi, dash, parry, Kaeshi-uchi, Ran-uchi, Kasumi-uchi, Yae-uchi | Modelo do Hoshina recebido; falta a espada dupla |
| 0.6-E | **Kaiju No. 10** (forma pequena 4 m fortitude 8,3; forma gigante 24 m fortitude 9,0) e **Preondactyl** (voador, fortitude ~6,3); invasao nivel 6 com o No. 10 na onda seguinte e chance do No. 9 aparecer | Sim |
| 0.6-F | **Hoshina + traje numerado 10** (cauda com terceira espada, sincronizacao, Full Release, Juni-hitoe) | Sim |
| 0.6-G | **Pontos de defesa**: a invasao ataca um ponto fixo (gerador/QG da Forca de Defesa com vida); estruturas novas e grandes (base) | Nao |
| 0.6-H | No. 9 avancado (clones, Finger Gun multiplo, casca defensiva, comando de aliados) | Opcional (asas/forma final) |

Regras da especificacao adotadas: habilidades com cooldown, prioridade, distancia, telegraph, efeitos e som; limite de
invocacoes, clones, particulas e destruicao; nada de busca de entidades a cada tick para todos os mobs.
