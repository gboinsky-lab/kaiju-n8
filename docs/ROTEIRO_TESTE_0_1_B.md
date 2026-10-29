# ROTEIRO_TESTE_0_1_B.md — Teste manual único da 0.1-B (servidor dedicado + 2 clientes)

Estado automático em 2026-10-06: `./gradlew build` OK (15 suítes JUnit, 69 testes), `./gradlew runGameTestServer` OK
(31/31), cliente abre até a tela inicial sem erro de recurso. **Nada abaixo foi visto em jogo ainda**: é o que falta.

Marque cada item com ✅ / ❌ e anote o que viu nos ❌ (com o trecho do `logs/latest.log` do lado que falhou).

## 0. Preparação

1. Feche o servidor antigo que ficou aberto (ele roda o código de antes da 0.1-B e ocupa a porta 25565).
2. Três terminais na pasta do projeto, com Java 21:
   - `./gradlew runServer` — espere `Done`.
   - `./gradlew runClientJoin` — entra como **Dev1**.
   - `./gradlew runClient2` — entra como **Dev2**.
3. Dev1 e Dev2 já são operadores (`run-server/ops.json`). Em Dev1: `/gamemode creative`, `/time set day`,
   `/gamerule doMobSpawning false`, vá para um terreno plano com algumas construções (uma vila serve).
4. Protocolo de rede subiu para `"6"`: cliente e servidor precisam ser desta mesma compilação.
5. Config do servidor: `run-server/config/kn8-server.toml` (ganhou chaves novas: o NeoForge corrige o arquivo sozinho
   na primeira subida); config do cliente: `run/config/kn8-client.toml`.

Comandos úteis: `/kn8 kaiju spawn <espécie> [quantidade]`, `/kn8 kaiju info`, `/kn8 kaiju stagger <ticks>`,
`/kn8 kaiju clear`, `/kn8 release set <0-100>`, `/kn8 stamina set <n>`.

## 1. Escala dos kaiju e kaiju grandes andando (Parte 1)

| # | Passo | Esperado |
|---|---|---|
| 1.1 | `/kn8 kaiju spawn primigenius`, F3+B | Hitbox ≈ 3,1 × 6,0 blocos; modelo do tamanho da hitbox, pés no chão |
| 1.2 | `/kn8 kaiju spawn primigenius_honju` | ≈ 9 blocos de altura; partes (cabeça, núcleo) no lugar do modelo |
| 1.3 | `/kn8 kaiju spawn primigenius_revived` e `trichonephila` | Sem regressão: revivido 3,5 × 5,5, aranha 3,0 × 3,4 |
| 1.4 | Em survival (Dev2), fique a ~25 blocos do Honju em terreno com degraus de 1–2 blocos | Anda em linha reta até você, sobe os degraus sem pular nem travar |
| 1.5 | Coloque uma parede de terra/madeira de 3 de altura entre você e o Honju | Ele abre passagem (blocos somem aos poucos) e continua; não fica parado girando |
| 1.6 | Mesmo teste com a Trichonephila (3,0 de largura, acima de `kaiju.largeKaijuWidth` = 2,5) e uma parede de pedra | Anda direto como kaiju largo, mas um Yoju (força 1) não abre pedra: contorna ou para |
| 1.7 | Veja o log do servidor ao iniciar | Sem aviso de `KaijuScale` fora da faixa para as 5 espécies (se houver, anote qual) |
| 1.8 | Os dois clientes olhando o mesmo kaiju | Mesma posição, tamanho e animação nos dois |

## 2. Efeitos e tremor (Parte 2)

| # | Passo | Esperado |
|---|---|---|
| 2.1 | Deixe o Primigenius usar **slam** perto de Dev2 (survival) | Onda de choque + poeira no tick do impacto (não no começo da animação), som, câmera treme |
| 2.2 | Dev1 a ~40 blocos e a ~70 blocos do mesmo slam | A 40: efeito visível e tremor mais fraco; a 70 (> 64): nada |
| 2.3 | **bite** e **charge** | Efeito e som próprios de cada um; a investida levanta poeira no caminho |
| 2.4 | `/kn8 release set 10`, depois `30`, depois `100` | Sem aura a 10; aura a partir de 20%, mais forte a 100. Dev2 enxerga a aura de Dev1 |
| 2.5 | Suba o calor até sobrecarga (`/kn8 heat set ...`) | Pulsos de sobrecarga (OVERLOAD/CRITICAL/PANIC) visíveis para os dois |
| 2.6 | Atire com rifle e pistola; corte com faca e espada (leve e pesado) | Clarão/fumaça no cano; impacto no alvo; corte da lâmina mais forte no pesado e no crítico |
| 2.7 | `kn8-client.toml`: `cameraShake = 0.0`, depois `particleLevel` no mínimo | Sem tremor; menos partículas. Só afeta o cliente que mudou |
| 2.8 | `kn8-server.toml`: `[vfx] enabled = false`, `/reload` ou reinicie | Nenhum efeito kn8; combate e dano continuam iguais |
| 2.9 | F3 com 5 kaiju lutando e destruindo | FPS aceitável; sem erro repetido no log do cliente |

## 3. Destruição (Parte 3)

| # | Passo | Esperado |
|---|---|---|
| 3.1 | `/kn8 destruction test 4 1` sobre grama/folhas/vidro | Só blocos frágeis somem, aos poucos (`blocksPerTick` 32), sem drop |
| 3.2 | `/kn8 destruction test 4 2`, `3`, `4` sobre madeira, pedra, obsidiana | Cada força quebra até a sua categoria; obsidiana/bedrock nunca |
| 3.3 | `/kn8 destruction test 5 3 true` | Cratera irregular (borda não é círculo perfeito), mais funda no centro |
| 3.4 | Baú, fornalha e placa dentro do raio | Blocos com block entity ficam intactos, com o conteúdo |
| 3.5 | `/kn8 destruction protect <x1 y1 z1> <x2 y2 z2> base`, `list`, depois `test` por cima | Área listada; nada quebra dentro dela; fora quebra normal |
| 3.6 | `/kn8 destruction unprotect base` e repita o `test` | Agora quebra |
| 3.7 | `/kn8 destruction restore` | Blocos voltam aos poucos (`rebuildBlocksPerTick` 16), no estado original (escadas, portas viradas certo) |
| 3.8 | `/gamerule mobGriefing false`; slam e investida de kaiju perto de casas | Nenhum bloco quebrado por kaiju; dano e efeitos iguais |
| 3.9 | `/gamerule mobGriefing true`; slam do Primigenius e investida | Slam abre cratera (raio ~3,5); investida abre passagem à frente |
| 3.10 | Destrua, saia e reinicie o servidor, depois `/kn8 destruction restore` | O registro (`DestructionLog`) e as áreas protegidas sobreviveram ao reinício |
| 3.11 | `[destruction] enabled = false` | Nenhuma destruição (nem do comando `test`) |
| 3.12 | Dev2 parado dentro da cratera enquanto restaura | Não fica preso/sufocando dentro de bloco restaurado (anote se ficar) |

## 4. Carcaça e desmonte (Parte 4)

| # | Passo | Esperado |
|---|---|---|
| 4.1 | Mate um Primigenius **sem** destruir o núcleo (dano no corpo) | No instante da morte vira carcaça (sem a animação vermelha vanilla), cai e assenta no chão |
| 4.2 | Segure clique direito na carcaça com a **faca** | Progresso por etapas; cada etapa entrega materiais; na última a carcaça some |
| 4.3 | Confira os itens | `Tecido Kaiju`, `Fibra Muscular`, e **Núcleo Kaiju Intacto** (núcleo preservado) |
| 4.4 | Mate outro destruindo o núcleo | Carcaça dá `Fragmento de Núcleo`, nunca núcleo intacto |
| 4.5 | Clique direito com a mão vazia e com o rifle | Não desmonta; o clique direito do rifle/bloqueio não dispara ataque carregado na carcaça |
| 4.6 | Dev1 e Dev2 desmontando a mesma carcaça | Progresso soma; ninguém recebe item duplicado; os dois veem a carcaça sumir |
| 4.7 | Carcaça do Honju e da Trichonephila | Modelo e hitbox da espécie certa (a aranha usa a malha dela) |
| 4.8 | Saia e entre de novo (e reinicie o servidor) com carcaça meio desmontada | Continua na mesma etapa (NBT) |
| 4.9 | Baixe `[carcass] despawnTicks` para 200 | Some sozinha depois de ~10 s sem uso |
| 4.10 | Ícones dos 4 materiais no inventário e na aba criativa | Sem textura roxa/preta |

## 5. Primigenius ressurgido e Honju (Parte 5)

| # | Passo | Esperado |
|---|---|---|
| 5.1 | `/kn8 kaiju spawn primigenius_resurrected` | Mesmo corpo do Primigenius, textura **verde**, 6 de altura, nome "Primigenius ressurgido" |
| 5.2 | `/kn8 kaiju spawn primigenius_honju` | Titã Bruto **marrom**, 9 de altura, nome "Primigenius Honju" |
| 5.3 | `/kn8 kaiju info` mirando em cada um | Fortitude 5,9 e 6,0; categoria Yoju / Honju |
| 5.4 | Lute com cada um | Usam bite/slam/charge; núcleo, partes e exposição do núcleo funcionam |
| 5.5 | Ande pelo mundo com `doMobSpawning true` por alguns minutos | Nenhum dos dois nasce naturalmente |

## 6. Barra de vida do kaiju (Parte 6)

| # | Passo | Esperado |
|---|---|---|
| 6.1 | Mire num kaiju a até 48 blocos (inclusive numa parte: cabeça, núcleo) | Barra no topo: nome, "categoria · porte · estado", vida e núcleo |
| 6.2 | Desvie a mira | A barra fica ~5 s e some |
| 6.3 | Cause dano no corpo e no núcleo | Vida e núcleo descem nos **dois** clientes |
| 6.4 | Golpe pesado da lâmina | Núcleo pisca "EXPOSTO" pelo tempo de exposição |
| 6.5 | Com um Wither/Dragão ativo (barra de chefe vanilla) | As duas barras não se sobrepõem |
| 6.6 | Mire a 60 blocos; mire numa carcaça | Sem barra |
| 6.7 | F1 e tela de inventário aberta | A barra respeita o HUD escondido |

## 7. Dash e ataque carregado (Parte 7)

| # | Passo | Esperado |
|---|---|---|
| 7.1 | Survival, stamina cheia, **Alt esquerdo** andando para frente/lado/parado | Avanço rápido na direção do movimento (parado: para frente), custa 25, poeira, animação |
| 7.2 | `/kn8 stamina set 10` e dash | Recusado, HUD avisa falta de stamina |
| 7.3 | Leve dano durante o dash | Toma dano (dash **não** dá invulnerabilidade; a esquiva Z continua dando) |
| 7.4 | Faca/espada: toque rápido no clique direito | Golpe pesado comum (custo do pesado) |
| 7.5 | Segure ~1 s e solte | HUD "CARGA"; golpe mais forte que o pesado, custo 20 |
| 7.6 | Segure > 1,5 s (30 ticks) e solte no kaiju | HUD "CARGA MÁXIMA"; dano ×2, crítico garantido, núcleo exposto |
| 7.7 | Segure a carga e troque de item / tome dano / abra o inventário | A carga cancela sem travar a postura nem o personagem |
| 7.8 | Clique direito com rifle/pistola | Sem carga (arma de fogo não carrega) |
| 7.9 | Dev2 olhando Dev1 | Vê a postura de carga, o golpe e o dash com a animação certa |
| 7.10 | Alt esquerdo em Controles | Tecla "Dash (avanço rápido)" na categoria do kn8, sem conflito |

## 8. Soldado 1 (Parte 8)

| # | Passo | Esperado |
|---|---|---|
| 8.1 | `/kn8 soldier spawn unarmed`, `rifle`, `pistol`, `sword`, `knife` | 5 variantes; a arma certa aparece **na mão direita**, na orientação certa |
| 8.2 | Modelo | Malha do soldado inteira (cabeça, tronco, braços, pernas), textura certa, anda e fica parado animado (ver §10) |
| 8.3 | `/kn8 soldier spawn rifle low` / `normal` / `high` / `elite` contra o mesmo kaiju | Dano e resistência sobem com o nível (compare o tempo para matar) |
| 8.4 | Soldado de rifle × Primigenius | Mantém ~12 blocos (recua se o kaiju chega perto), atira; o dano sai no tick do tiro |
| 8.5 | Soldado de espada/faca/desarmado × kaiju | Aproxima até o alcance e golpeia; não fica batendo no ar |
| 8.6 | Kaiju perto de soldado e de jogador | O kaiju também caça soldados |
| 8.7 | Bata e atire no soldado (Dev1 e Dev2) | Não fere (aliado); soldados não se ferem entre si; tiro de soldado não acerta jogador |
| 8.8 | Soldado atirando com jogador na linha de tiro | O tiro ignora o jogador e acerta o kaiju atrás |
| 8.9 | Ovo de spawn na aba criativa | Nasce soldado de rifle, nível normal |
| 8.10 | 6 soldados × 1 Honju, com destruição ligada | Sem queda grande de TPS (`/neoforge tps`); soldados morrem com animação/sumiço normal |
| 8.11 | Reinicie o servidor com soldados vivos | Voltam com a mesma variante e nível |

## 9. Regressão rápida

- Combo leve da faca, bloqueio (V), parry, esquiva (Z), rifle a > 20 blocos, HUD de stamina/calor/energia.
- Spawn natural da Trichonephila à noite; bando.
- Relog e troca de dimensão com Release ≠ 0: HUD volta certa (reenvio dos dados privados).

## Pendências (não cobertas ou em aberto)

1. **Nada do lado cliente foi visto em jogo** pelo Claude Code: só compilação, JUnit, GameTests e carga até a tela
   inicial. Renderers (kaiju em escala, carcaça, soldado com arma no osso), HUD, tremor e partículas dependem deste
   roteiro.
2. Sons próprios (.ogg): hoje são sons vanilla como placeholder.
3. Ícones dos materiais e itens de traje: placeholders; avisos de itens de traje ficam para o M14.
4. [SUPOSIÇÃO] a confirmar: fortitude 5,9 / 6,0 e ausência de spawn natural das duas espécies novas; níveis do
   soldado (low 5 / normal 10 / high 20 / elite 30); ataque "heavy" de kaiju atravessa o bloqueio comum.
5. Sinal do eixo X em partes assimétricas dos `.geo.json` (conferir no item 1.2 e 8.1).
6. GameTests que ainda usam `runAfterDelay` dentro de `runAfterDelay` (Carcass, KaijuAbility, KaijuAreaAbility):
   passam, mas o bloco de fora pode rodar duas vezes; achatar como foi feito em `criticalIsConsumedOnTheFirstHit`.
7. Não há GameTest para destruição (fila, áreas protegidas, restauração), dash e ataque carregado: só JUnit da
   matemática. Escrever depois do teste manual.
8. Corrida com custo de stamina (GDD §7) continua não implementada.
9. M11a e Etapas A/B continuam "aguardando teste" (este roteiro cobre só a regressão rápida delas).

## 10. Correções visuais (aranha, soldado, armas na mão, espada) — 2026-10-06

Feitas sem compilar (sem rede nesta sessão). Antes: `./gradlew build` e `./gradlew runGameTestServer`.
Prévias do que é esperado: `docs/img/preview_aranha.png`, `preview_armas_jogador.png`, `preview_soldado.png`.

| # | Passo | Esperado |
|---|---|---|
| 10.1 | `/kn8 kaiju spawn trichonephila`, olhe de perto e de longe | Listras amarelas/pretas limpas; sem linhas douradas finas nem manchas brancas espalhadas |
| 10.2 | `/kn8 soldier spawn rifle` e afaste-se ~10 blocos | Textura do soldado sem cintilar; sem riscos pretos/brancos nas costuras |
| 10.3 | Soldado parado e andando (sem kaiju) | Respira (tronco sobe/desce), pernas andam, braços seguram a arma (rifle apontado para a frente e para baixo); a cabeça vira para o jogador |
| 10.4 | Soldado de rifle × kaiju | Ao ter alvo, levanta a arma e mira (braços acompanham a altura do alvo); coice a cada tiro |
| 10.5 | Pistola, espada, faca e sem arma | Cada um com a sua pose; espada/faca: golpe por cima; sem arma: guarda |
| 10.6 | Jogador com rifle/pistola em 3ª pessoa (F5), parado, andando, olhando para cima/baixo | Dois braços à frente, cano para onde olha, cabo dentro da mão |
| 10.7 | Jogador com rifle/pistola em 1ª pessoa | Arma no canto de baixo à direita, cano para a frente (levemente para o centro) |
| 10.8 | Espada na mão (1ª e 3ª pessoa), no chão, no inventário | Espada nova (lâmina prateada com faixa ciano), na mão como uma espada vanilla |
| 10.9 | Dev2 olhando Dev1 e o soldado | Mesmas poses nos dois clientes |
| 10.10 | Se na mira os braços do soldado ABREM para os lados em vez de fechar na frente | Anote: é o sinal de Y das poses ([SUPOSIÇÃO] da convenção da GeckoLib) |

## 11. Modelos novos do Meshy (estilo Minecraft) — 2026-10-06

Prévias: `docs/img/preview_soldado.png`, `preview_aranha.png`, `preview_primigenius.png`.

| # | Passo | Esperado |
|---|---|---|
| 11.1 | `/kn8 soldier spawn rifle` (e as outras variantes) | Soldado novo em blocos, inteiro, textura sem riscos; poses de §10 valem |
| 11.2 | `/kn8 kaiju spawn trichonephila`, F3+B | 8 patas; hitbox ≈ 3,4 × 1,8 cobrindo o corpo; patas andam alternadas, presas mexem na mordida |
| 11.3 | Acerte a cabeça da aranha e depois o abdômen | Cabeça = núcleo (dano ×3); a caixa da cabeça fica sobre a cabeça do modelo |
| 11.4 | `/kn8 kaiju spawn primigenius`, F3+B | Modelo novo com cauda, 6 de altura; cabeça, torso, núcleo (peito) e pernas no lugar do modelo |
| 11.5 | Lute com o Primigenius (slam, charge, mordida) | Animações tocam com a malha nova (braços, mandíbula, cauda) sem peças soltas |
| 11.6 | Carcaça da aranha e do Primigenius | Carcaça usa o modelo novo |
| 11.7 | `primigenius_resurrected` | Continua o modelo de cubos verde (o novo ainda não chegou) |

## 12. Menu da Força de Defesa (0.2) — tecla M

Capturas de referência: `docs/img/menu_*.png`. Teste em pt_br e en_us; tamanho de GUI automático e 2.

| # | Passo | Esperado |
|---|---|---|
| 12.1 | M | Abre em tela cheia, jogo não pausa; M ou Esc fecha; tecla em Controles (categoria kn8) |
| 12.2 | Q / E e clique nas abas | Troca Perfil → Missões → Alertas → Esquadrão → Bestiário → Arsenal (dá a volta) |
| 12.3 | Perfil | Seu personagem segue o mouse; patente Candidato; Release atual/teto e XP de treino batem com a HUD (`/kn8 release set 5`); arma da mão destacada |
| 12.4 | Mate um kaiju em survival e reabra | "Kaiju abatidos" sobe; a espécie aparece registrada no Bestiário |
| 12.5 | Missões | 3 missões do datapack com categoria, objetivos (0/N), patente exigida e recompensa; ACEITAR desativado ("Em breve (Etapa 5)"); filtros Ativas/Concluídas vazios |
| 12.6 | Alertas com kaiju a até 128 blocos | Lista do mais perto ao mais longe, ameaça colorida, distância e direção (N/NE/L...) corretas; estado (Parado/Perseguindo) muda ao vivo |
| 12.7 | Esquadrão com soldados por perto | Lista com arma, nível de força e vida; clique escolhe o soldado em 3D à direita; ordens desativadas |
| 12.8 | Bestiário em survival sem abates | Espécies como silhueta escura "???"; em criativo todas aparecem com modelo girando, fraquezas e materiais |
| 12.9 | Arsenal | Armas com dano/alcance/cadência/peso do JSON (mude um número no JSON, `/reload`, reabra); Trajes com armadura; Fabricação "Etapa 3" |
| 12.10 | Dev2 | Menu do Dev2 mostra os dados dele (não os do Dev1) |

## 13. Sons, corrida e carcaça (0.2, Etapa 1)

**Já testado na nuvem (2026-10-06, todos ✅, ver `docs/ATUALIZACAO_0_1_B.md`):** cada som toca no evento certo, com a
legenda certa, e o Honju soa mais grave. **Fica para o Miguel:** ouvir se os sons ficaram bons (qualidade e volume)
e conferir a corrida/tombo no próprio PC.

Sons gerados por `tools/audio/gen_sounds.py` (trocar um som = substituir o `.ogg` em `assets/kn8/sounds/`). Ligue as
legendas (Opções → Acessibilidade) para conferir qual evento tocou. Capturas: `docs/img/corrida_stamina.png`,
`docs/img/carcaca_tomba.png`.

| # | Passo | Esperado |
|---|---|---|
| 13.1 | Chegue perto de um kaiju parado | Rosnado grave e longo de tempos em tempos (~10 s); rugido de boca aberta na investida; Honju mais grave que o Yoju; passos pesados ao andar |
| 13.2 | Bata no kaiju até morrer | Grunhido de dor a cada golpe, som de morte; o corpo **tomba de lado** em ~0,7 s e fica deitado, dentro da hitbox (F3+B) |
| 13.3 | Slam, mordida e investida do kaiju | Pancada grave no chão, mordida, rugido na investida (legendas "kaiju") |
| 13.4 | Faca/espada/machado: leve e pesado | Cada arma com som próprio: faca curta e aguda ("Faca cortando o ar", "Estocada de faca", "Faca acerta"), espada com "shing" de metal ("Espada cortando o ar", "Golpe largo de espada", "Espada acerta"), machado grave girando ("Machado girando", "Golpe pesado de machado", "Machado acerta"); o soldado com faca/espada/machado usa os mesmos |
| 13.5 | Rifle e pistola (jogador e soldado `/kn8 soldier spawn rifle`/`pistol`) | Tiro próprio de cada arma (pistola mais seca); nada de som de besta |
| 13.6 | Parry, guarda quebrada, dash (Alt) | Metal batendo no parry; estalo na quebra de guarda; sopro no dash |
| 13.7 | `/kn8 heat` até a pane | Alarme do traje |
| 13.8 | Desmonte da carcaça | Corte molhado a cada etapa |
| 13.9 | Survival: corra em linha reta | Stamina cai ~5/s; ao zerar, para de correr (mesmo segurando Ctrl); recupera andando; com 20+ aperte correr de novo |
| 13.10 | Criativo: corra | Stamina não cai |
| 13.11 | Dev2 olhando o Dev1 correr sem fôlego | Dev1 aparece andando (não correndo) para o Dev2 |

## 14. Modelos v2 do Meshy e aranha (0.2) — 2026-10-06

Já visto na nuvem com 2 clientes (`docs/img/modelos_v2_jogo.png`, `soldado_v2_jogo.png`, `modelos_v2_hitbox.png`).

| # | Passo | Esperado |
|---|---|---|
| 14.1 | `/summon` dos 4 Primigenius (`primigenius`, `_resurrected`, `_honju`, `_revived`) | Em pé sobre a sombra/hitbox, de frente para onde andam, cauda para trás; texturas inteiras, sem buraco |
| 14.2 | Deixe cada um atacar (slam, investida) | Braços, mãos e garras acompanham o golpe; nada fica parado no ar nem some; chifres presos na cabeça |
| 14.3 | F3+B | Cabeça, tronco, pernas e núcleo (peito) sobre o modelo |
| 14.4 | Mate cada um | Carcaça tomba de lado e fica deitada; desmonte funciona |
| 14.5 | Soldado com rifle/pistola/espada, de perto | Placas brancas limpas (sem manchas pretas ou linhas); mirando, sem buraco no quadril |
| 14.6 | Aranha andando (de lado e de cima) | Patas alternam sem cruzar e sem entrar no corpo |

## 15. Carreira, missões e chefe (0.2, Etapas 2, 5 e 6)

Visto na nuvem: XP do boneco (+5 por golpe, teto por minuto), aba Missões (exame travado pelo pré-requisito),
aceitar pelo menu faz 2 Primigenius surgirem a ~45 m e a seta do rastreador aponta certo. GameTests cobrem promoção,
teto de Release 100, limite do boneco, contagem de abates e troca de fase do chefe.

| # | Passo | Esperado |
|---|---|---|
| 15.1 | Survival, bata no boneco de treino (item `kn8:training_dummy`) | "+5 XP de treino" por golpe; depois de 60 XP no minuto, aviso de limite; Shift + golpe de mão vazia derruba o boneco como item |
| 15.2 | M → Perfil | Patente Candidato, mérito, próxima patente com "precisa da missão" e estatísticas |
| 15.3 | M → Missões → aceite "Primeiro Desmonte" e "Extermínio" | Rastreador no canto superior direito com objetivo, tempo e seta; kaiju surgem no ponto |
| 15.4 | Conclua uma missão | Título "MISSÃO CUMPRIDA", mérito/XP/itens no chat; missão vai para "Concluídas" |
| 15.5 | `/kn8 boss spawn kn8:revived_honju` | Barra de chefe; ao perder vida troca de fase (rugido, invulnerável um instante) e invoca só Primigenius ressurgidos |
| 15.6 | Exame de Admissão completo + mérito | Promoção a Oficial (título, som); vida máxima sobe |

## 16. Bancada, trajes e suprimentos (0.2, Etapa 3)

Visto na nuvem (`docs/img/bancada_fabricacao.png`, `traje_mk1.png`): a bancada abre o Arsenal na Fabricação,
receitas travadas pela patente em vermelho, faca fabricada com 2 ferros + 1 graveto, Mk1 vestido (armadura 12).

| # | Passo | Esperado |
|---|---|---|
| 16.1 | Faça a bancada (ferro em volta, mesa de trabalho no meio, bloco de redstone embaixo) e use-a | Abre o menu em Arsenal → Fabricação |
| 16.2 | Candidato: veja pistola, rifle, Mk1 | "Travado pela patente" e "Precisa da patente: Oficial" |
| 16.3 | Com os materiais, FABRICAR | Materiais saem, item entra, som de forja e faíscas |
| 16.4 | Longe da bancada (sem criativo) abra o menu (M) | Botão apagado e aviso "Use perto de uma bancada" |
| 16.5 | Vista o Mk1 (peito) e veja pelo Dev2 | Traje preto com faixas ciano; armadura 12 e menos calor ao usar Release |
| 16.6 | Resfriador, estimulante, catalisador (clique direito) | Calor cai 50; stamina cheia; +500 XP de treino |

## 17. Alertas de invasão (0.2, Etapa 7)

Visto na nuvem com 2 clientes (`docs/img/invasao_alertas.png`, `invasao_barras.png`, `invasao_vitoria.png`).
Kaiju não nascem mais sozinhos (`spawn.natural` desligado); vêm de alertas, invasões e missões.

| # | Passo | Esperado |
|---|---|---|
| 17.1 | `/kn8 invasion start kn8:primigenius_raid` | Sirene, chat "ALERTA DE INVASÃO" com X/Z, barra vermelha no topo para quem está na área, soldados de defesa no centro |
| 17.2 | Espere 20 s (ou `/kn8 invasion next`) | Onda 1/3 chega de 40–60 m e marcha para o centro; aba Alertas mostra onda, tempo, vivos e distância |
| 17.3 | Mate a onda | "Onda eliminada"; intervalo com sirene; próxima onda |
| 17.4 | Vença a última onda | Título "ÁREA DEFENDIDA" e recompensa (mérito, XP, itens) para quem esteve na área; Perfil conta "Invasões contidas" |
| 17.5 | `/kn8 invasion stop` no meio | Barra some; kaiju que já chegaram continuam |
| 17.6 | Oficial: missão "Defesa da Cidade" | A invasão começa perto e o ponto vai para o rastreador |
| 17.7 | Noite cair (config `invasion.naturalChance`, padrão 20%/dia) | Às vezes uma invasão começa sozinha perto de um jogador na superfície |

## 18. Kaiju No. 9 (0.2, Etapa 8)

Visto na nuvem com 2 clientes (`docs/img/no9_revivendo.png`): No. 9 reviveu as duas carcaças de Primigenius em
ressurgidos, fugiu com 30% da vida (+250 de mérito para quem estava perto) e a invasão terminou com vitória.

| # | Passo | Esperado |
|---|---|---|
| 18.1 | `/kn8 invasion start kn8:no9_resurrection`, mate a onda 1 perto do centro | Carcaças ficam no chão |
| 18.2 | Onda 2: o No. 9 (humanoide de 2 m) chega | Para perto de uma carcaça, ergue os braços, fio verde até ela; aviso "está revivendo"; 3 s depois levanta um Primigenius ressurgido (Honju vira revivido) |
| 18.3 | Desmonte a carcaça antes dos 3 s | O gesto falha |
| 18.4 | Lute com o No. 9 | Garra rápida e investida; os kaiju por perto atacam o mesmo alvo dele |
| 18.5 | Tire 70% da vida dele | Some em fumaça verde ("recuou... vai voltar"), +250 de mérito; a onda segue sem ele |
| 18.6 | Animações vistas de perto | Andar, garra, investida e gesto de reviver sem peças soltas |

## 19. Construções (0.2)

Vistas na nuvem com `/place structure` (`docs/img/construcao_*.png`). Geradas por `tools/world/gen_structures.py`;
aparecem sozinhas em mundo novo (ou em chunks ainda não gerados), nos biomas de `tags/worldgen/biome/has_structure/`.

| # | Passo | Esperado |
|---|---|---|
| 19.1 | `/place structure kn8:defense_outpost` | Muro com portão listrado, holofotes nos cantos, sede branca com faixa ciano (bancada, baú, mesa de cartografia), heliporto, pátio com 3 bonecos e alvos, bandeira azul, 3 soldados de guarda |
| 19.2 | `/place structure kn8:ruined_building` | Prédio cinza com o canto arrancado, rasgos de garra na fachada, entulho em volta, baú no térreo |
| 19.3 | `/place structure kn8:kaiju_remains` | Cratera com espinha, costelas e crânio de osso, núcleo de magma no peito, baú com fragmentos |
| 19.4 | `/place structure kn8:watchtower` | Torre de 19 m com escada, sino (sirene), holofote, baú e um soldado com rifle no alto |
| 19.5 | Mundo novo, `/locate structure kn8:defense_outpost` (e os outros) | Acha uma perto; chegando lá, a construção está assentada no terreno |
| 19.6 | Abra os baús | Itens do mod (tecido, fibra, resfriador, estimulante, fragmento de núcleo...) |

## 20. Níveis de invasão (0.3)

Visto na nuvem com 2 clientes (`docs/img/invasao_nivel5_*.png`): nível 5 completo, 26 kaiju revividos de uma vez
(o Honju volta como chefe "Honju revivido"), servidor a 7,6 ms por tick.

| # | Passo | Esperado |
|---|---|---|
| 20.1 | Qualquer invasão | Barra "INVASÃO NÍVEL N"; aba Alertas com selo colorido do nível (1 baixo … 5 catástrofe) |
| 20.2 | `/kn8 invasion start kn8:kaiju_horde` | Nível 4: 10 kaiju na onda 1, 9 + Honju chefe na onda 2 (20 no total) |
| 20.3 | `/kn8 invasion start kn8:mass_resurrection` | Nível 5: a mesma horda; depois dela o No. 9 chega, para, ergue os braços e avisa "ESTÁ REVIVENDO N CARCAÇAS"; 5 s depois o exército levanta um a um (do mais perto ao mais longe), o Honju volta como chefe com barra |
| 20.4 | Desmonte carcaças no intervalo antes da onda 3 | Menos kaiju revividos |
| 20.5 | Derrube o No. 9 durante o gesto | Ele foge e a ressurreição não acontece (ou para no meio) |
| 20.6 | Oficial Sênior: missões "Contenção da Horda" e, depois dela e de "Ameaça Revivida", "Noite da Ressurreição" | A invasão do nível certo começa perto |

## 21. Soldados comuns: variantes (0.4)

Visto na nuvem com 2 clientes (`docs/img/esquadrao_variantes.png`, `soldado_faca_apoio.png`).

| # | Passo | Esperado |
|---|---|---|
| 21.1 | Ovo de soldado várias vezes (ou `/kn8 soldier spawn random`) | Variantes sorteadas: Atirador (rifle), Patrulheiro (pistola), Espadachim (espada), Batedor (faca); nunca machado |
| 21.2 | M → Esquadrão | Cada soldado com o papel no nome ("Atirador 5", "Batedor B"...) |
| 21.3 | Atirador com um kaiju chegando perto | Troca para a faca (som de equipar) quando o kaiju fica a menos de 3,5 blocos; volta ao rifle quando ele se afasta ou some |
| 21.4 | Invasões | Defensores misturados (variante "random") |
| 21.5 | M → Perfil com o Mk1 vestido | "Armadura 12 · corta 15% do calor"; sem traje, a dica de fabricar na bancada |

## 22. Hoshina, o primeiro soldado especial (0.6-D)

Visto na nuvem (`docs/img/hoshina_modelo.png`, `hoshina_vs_honju.png`, `hoshina_espada_kuuchi_jogador.png`).

| # | Passo | Esperado |
|---|---|---|
| 22.0 | `/kn8 invasion start kn8:kaiju_horde` (nível 4) | O Hoshina entra junto com os defensores; nas invasões de nível 1–3 ele não aparece |
| 22.1 | Ovo do Vice-Capitão Hoshina (aba de itens do kn8) | Hoshina com cabelo roxo, bainhas nas costas e uma espada em cada mão; aura roxa com faíscas em volta |
| 22.2 | `/summon kn8:primigenius_honju ~ ~ ~10` perto dele | Ele corre (dash) até o kaiju, solta cortes roxos de longe (Kūuchi, Kōsa-uchi em X) e combos de perto |
| 22.3 | Observar os golpes pesados do Honju (soco pesado, investida) | Hoshina desvia de lado e contra-ataca (Kaeshi-uchi); golpes comuns às vezes são aparados (faíscas + som metálico) |
| 22.4 | Luta longa | O Release dele sobe 2%/s (40% → 90%): aura mais forte, mais rápido, recargas mais curtas; com a vida baixa sobe mais |
| 22.5 | M → Esquadrão | "Vice-Capitão Hoshina" na lista |
| 22.6 | Bater nele / atirar perto dele | Não fere jogadores nem soldados; os cortes dele atravessam aliados sem ferir |
| 22.7 | Bancada como Vice-Capitão | Receita da espada do Hoshina (8 ferro, 6 fibra, 4 fragmentos, 1 núcleo intacto) |
| 22.8 | Com a espada, R | Kūuchi do jogador: corte roxo que voa 12 blocos; recarga de 2 s no HUD |

## 23. Kaiju No. 10, Preondactyl e invasão nível 6 (0.6-E)

| # | Passo | Esperado |
|---|---|---|
| 23.1 | `/kn8 kaiju spawn kaiju_no10_small` | No. 10 vermelho de 4 m com cauda; ataca com socos, cauda, Finger Cannon (projétil vermelho que explode) e rajada de golpes |
| 23.2 | Lutar com ele por 60 s (ou tirar metade da vida) | Explosão, rugido e "O Kaiju No. 10 assumiu a forma gigante!": surge a forma de 24 m com a vida cheia, quebrando o que está em volta |
| 23.3 | Ferir o No. 10 abaixo de 50% e parar de bater | Partículas verdes: ele regenera (mais rápido abaixo de 20%) |
| 23.4 | `/kn8 kaiju spawn preondactyl` perto de você | Decola, circula ~9 blocos acima, atira um raio de energia (aviso na boca) e mergulha para morder |
| 23.5 | Bater no Preondactyl de frente e por trás | Por trás tira bem mais (frente blindada ×0,35, costas ×1,3) |
| 23.6 | Deixar o Preondactyl com pouca vida | "vai se autodestruir! Afaste-se!", contagem 3-2-1 na tela e explosão |
| 23.7 | No. 10 + Preondactyls juntos | Os Preondactyls atacam o mesmo alvo do No. 10 |
| 23.8 | `/kn8 invasion start kn8:no10_assault` (nível 6, "ameaça numerada") | Ondas: Preondactyls com Yoju; depois Preondactyls e ressurgidos (às vezes o No. 9 junto); por último o No. 10. Defensores fortes e o Hoshina |
| 23.9 | Missão "Ameaça Numerada: No. 10" (Líder de Pelotão) | Aceitar inicia a invasão nível 6 |
| 23.10 | Hoshina (§22) na luta | Release no máximo 92% na forma normal (aura) |

## 24. Hoshina com o traje numerado 10 (0.6-F)

| # | Passo | Esperado |
|---|---|---|
| 24.1 | Ovo do Hoshina com a Arma Numerada 10 ou `/summon kn8:hoshina_no10` | Hoshina de traje escuro com a cauda enrolada atrás; duas espadas nas mãos e **nenhuma espada na cauda**; a cauda balança parada |
| 24.2 | `/kn8 kaiju spawn primigenius` atrás dele | A cauda corta sozinha o kaiju das costas (animação da cauda), mesmo com o Hoshina olhando para outro lado |
| 24.3 | Deixar a luta seguir | A aura sobe com a escalada até **100%** (o Hoshina comum para em 92%): é a sincronização com o traje |
| 24.4 | Em 100% (Full Release) | Mais rápido, mais dano, recargas menores; contra kaiju perto usa o **Jūni-hitoe** (12 golpes seguidos, o último forte, expõe o núcleo); não é interrompido por esquiva/contra-ataque |
| 24.5 | Kaiju atirando nele por trás (Finger Cannon do No. 10, raio do Preondactyl) | A cauda gira para trás e defende (faíscas e som metálico): dano bem menor |
| 24.6 | `/summon kn8:hoshina_no10` contra `kaiju_no10_giant` | Luta equilibrada a favor do Hoshina (ver `docs/BALANCEAMENTO.md`) |

## 25. Release do jogador e atributos do corpo (0.5.0-A)

| # | Passo | Esperado |
|---|---|---|
| 25.1 | Entrar num mundo novo (ou com um jogador novo) | Mensagem no chat com o limite inicial: comum 5-10% ou "Talento raro!" 15-30%. A HUD mostra `0% / limite%` |
| 25.2 | Sem traje, segurar **G** | Aviso "O Release so funciona com o traje da Forca de Defesa vestido."; a % fica em 0 |
| 25.3 | Vestir o traje (Mk1, peitoral) e segurar **G** | A % sobe **2% por segundo** e a aura aparece junto; soltar deixa a % onde parou |
| 25.4 | Ficar abaixo do limite; depois com a % exatamente no limite por ~1 minuto | Abaixo: o calor nao sobe. No limite (forca total): sobe devagar ate "morno" e para ali (cansaco: stamina volta mais devagar), sem dano |
| 25.5 | Passar do limite (segurar G alem da marca da barra) | A parte acima do limite fica laranja; o calor sobe rapido; em sobrecarga, critico e maximo o corpo perde vida (alarme no maximo). **A % nao cai sozinha** |
| 25.6 | Continuar segurando G | A % para em **limite + 20** (ex.: limite 40 -> 60%); quanto mais acima, mais vida perde |
| 25.6b | **Shift + G** ate voltar para dentro do limite | "Voce passou do seu limite: fadiga por N s" e "FADIGA" na HUD: quase parado, fraco, Release desligado e G nao sobe ate acabar |
| 25.6c | Bater num kaiju com o Release alto (golpe comum, carregado, critico) | Nunca tira mais de 15% da vida dele por golpe; so o especial (R) pode matar de uma vez |
| 25.7 | Tirar o traje com o Release ligado | A % zera e a aura some |
| 25.8 | Menu (M) > Perfil | Limite (com estrela se o talento for raro), "Acima do limite" e linha "Corpo: FOR / VEL / RES / AGI" |
| 25.9 | Bater em kaiju ou no boneco, correr, apanhar, esquivar/dash/parry | Sobem forca, velocidade, resistencia e agilidade (`/kn8 power` mostra os niveis) |
| 25.10 | Servidor dedicado + 2 clientes | Cada um ve a aura do outro subir e descer; a HUD de cada um e so dele |

## 26. Traje de corpo inteiro, soldado e Hoshina refeitos (0.5.0-B)

| # | Passo | Esperado |
|---|---|---|
| 26.1 | Vestir o **Mk1** (modelo novo do Miguel) e olhar de frente, de costas (F5) e andando | Traje no corpo todo: tronco, bracos ate a mao, pernas e botas; "espinha" branca nas costas; **a skin nao aparece** em nenhum lugar (segunda pele escura por baixo) |
| 26.2 | Mesmo com o **Mk1 Reforcado** | Igual: sem skin aparecendo |
| 26.3 | Primeira pessoa com o traje | A luva/braco do traje no lugar do braco da skin |
| 26.4 | `/summon kn8:soldier` | Soldado novo (capacete, pose aberta), bracos e pernas animando sem pedacos soltos |
| 26.5 | `/summon kn8:hoshina` | Hoshina novo; as bainhas ficam presas nas costas (nao giram com o braco) |
| 26.6 | Soldado e Hoshina contra um kaiju | Andam, atacam e as tecnicas do Hoshina saem como antes |

## 27. No. 10 refeito e perfis de locomocao (0.5.0-C)

| # | Passo | Esperado |
|---|---|---|
| 27.1 | `/summon kn8:kaiju_no10_small` e olhar de frente, de lado e de costas | Modelo novo do Miguel (vermelho, chifre para a frente, cauda enrolada para tras e para a esquerda); cabeca no eixo do corpo; cauda, bracos e pernas animando sem pedacos soltos |
| 27.2 | `/summon kn8:kaiju_no10_giant` (area aberta) | Forma gigante nova (24 m, chifre longo para a frente); cauda enrolada inteira, sem rasgos ao andar |
| 27.3 | No. 10 pequeno perseguindo soldados | Os pes acompanham o chao (sem "patinar"); ao andar devagar a animacao desacelera, em furia acelera |
| 27.4 | Forma gigante andando | Passos lentos e pesados, um som de passo por pe e poeira do bloco do chao a cada passo |
| 27.5 | Soldados e Hoshina andando/correndo | A animacao de andar acompanha a velocidade (Hoshina correndo anima mais rapido que soldado andando) |
| 27.6 | Batalha da forma pequena ate virar gigante (60 s ou abaixo de 50%) | A troca de forma continua funcionando com os modelos novos |
| 27.7 | Servidor dedicado + 2 clientes | Os dois veem a mesma cadencia de passos |

## 28. Balanceamento v1.0 e auditoria de texturas (0.5.0-C2)

| # | Passo | Esperado |
|---|---|---|
| 28.1 | `/kn8 kaiju info` perto de cada kaiju | Vida da tabela nova: aranha 65, Primigenius 220, ressurgido 310, Honju 360, revivido 460, Tecedeira 400, Preondactyl 430, No. 9 1.450, No. 10 1.650 / 4.600 |
| 28.2 | Golpe comum e golpe pesado com Release alto num Honju | Comum tira no máximo 12% da vida dele, pesado/carregado no máximo 15%; o especial (R) passa disso |
| 28.3 | `/kn8 soldier spawn rifle low` e `... elite` | Recruta/baixo com 20 de vida, elite com 34 |
| 28.4 | Chefes (Honju do Exame, Primigenius Honju, Honju revivido) | Mesma vida de antes: 160, 480, 844 |
| 28.5 | Olhar cada kaiju, o soldado e o Hoshina andando, atacando e tomando dano, de perto | Nenhuma parte sem textura, branca, preta, roxa ou com textura de outro modelo (referência: `docs/img/auditoria_texturas_*.jpg`) |

## 29. Perfis de arma: postura, saque, guarda, recarga, coice e NPC (0.5.0-D)

| # | Passo | Esperado |
|---|---|---|
| 29.1 | Em terceira pessoa (F5), trocar entre faca, espada, espada do Hoshina e machado | Cada uma com o saque (mão vai ao quadril e traz a arma) e depois a postura própria: faca em guarda curta, espada na diagonal, espadas do Hoshina em pegada invertida com o tronco inclinado, machado com as duas mãos |
| 29.2 | Trocar de arma e atacar logo em seguida | Durante o saque o golpe não sai (faca 0,2 s, espada 0,4 s, machado 0,7 s) |
| 29.3 | Segurar a guarda (V) com cada arma | Guarda da família: uma mão na frente, machado atravessado, espadas do Hoshina cruzadas em X |
| 29.4 | Golpes de lâmina em primeira pessoa | Os braços do modelo aparecem fazendo o golpe e a guarda |
| 29.5 | Rifle: atirar 30 vezes | HUD "Pente N/30" descendo; vazio, "RECARREGANDO" com barra, três sons (soltar, colocar, engatilhar) e volta a 30/30; durante a recarga não atira |
| 29.6 | Rifle: atirar uma vez e apertar R | Recarga manual; o rifle continua na tela em primeira pessoa |
| 29.7 | Atirar com rifle e com pistola em primeira pessoa | A mira sobe um pouco a cada tiro e volta em parte sozinha; a pistola dá mais coice |
| 29.8 | Soldado com rifle ou pistola lutando contra um kaiju | Depois de 30 (rifle) ou 12 (pistola) tiros para, faz a animação de recarga com os sons e volta a atirar |
| 29.9 | Soldado que troca para a faca de apoio | Espera o saque antes do primeiro golpe |
| 29.10 | Servidor dedicado + 2 clientes | Cada um vê a postura, o saque, a guarda e a recarga do outro; a HUD do pente só aparece para o dono |

## 30. Municao, posturas de corpo inteiro e espadas de par (0.5.0-D2/D3)

| # | Passo | Esperado |
|---|---|---|
| 30.1 | Sobrevivência: rifle sem nenhum pente na mochila, atirar até esvaziar | "Sem pente carregado"; não recarrega do nada |
| 30.2 | Fabricar pente e munição de rifle na bancada (patente Oficial), botão direito no pente | Pente enche (barra amarela e "30/30" na dica) gastando a munição |
| 30.3 | Atirar até esvaziar com o pente carregado na mochila | Recarga troca os pentes: o carregado sai da mochila, o vazio volta; HUD "Pente N/30 +M" |
| 30.4 | Soldado de rifle lutando muito tempo | Gasta os 3 pentes de reserva e troca para a faca |
| 30.5 | F5, cada arma parada, andando (W) e correndo (W + Ctrl), de frente e de costas | Corpo inteiro: pernas e tronco mudam; espadas do Hoshina agachado e inclinado, lâminas invertidas; machado na horizontal à frente |
| 30.6 | Combo (clique repetido) com cada arma | Cada golpe do combo com animação própria (faca 3, espada 3, espadas do Hoshina 4, machado 2) e o pesado diferente |
| 30.7 | Espada do Hoshina só na mão direita, apertar R | Kūuchi negado: "Técnica de par: segure uma espada em cada mão" |
| 30.8 | Outra espada do Hoshina na mão secundária (F troca), apertar R e atacar | Kūuchi sai; as duas espadas aparecem e golpeiam no combo |
| 30.9 | `/summon kn8:hoshina` perto de um kaiju | Hoshina agachado parado, andando e correndo com as duas lâminas invertidas |
| 30.10 | Servidor dedicado + 2 clientes | Cada um vê a postura, o andar e o correr do outro |

## 31. Hoshina refeito e tecnicas pelo anime (0.5.0-D4)

| # | Passo | Esperado |
|---|---|---|
| 31.1 | `/summon kn8:hoshina ~ ~ ~ {NoAI:1b}` e olhar a nuca de perto | Modelo novo, sem rosto na parte de trás da cabeça |
| 31.1b | Hoshina e jogador com as duas espadas, parados, andando e correndo, vistos de lado e de costas | Parado: braços para trás do corpo, o direito mais alto e o esquerdo mais baixo, lâminas saindo para trás; correndo: agachamento fundo, braços abertos para os lados, o direito quase no ombro com a lâmina para cima e o esquerdo mais baixo; andando: quase reto, braços abertos para os lados e para baixo |
| 31.2 | Olhar as mãos do Hoshina e as suas (F5) com a espada dele | A mão segura o meio do cabo (sobra um pouco do pomo do outro lado), não a ponta |
| 31.3 | Perto do Hoshina parado: `/kn8 anim soldier action <kuuchi, kosa_uchi, kaeshi_uchi, ran_uchi, kasumi_uchi, yae_uchi>` | 1: um corte só, rápido, com avanço; 2: as duas lâminas fecham em X na frente do peito; 3: abaixa e gira com as lâminas cruzadas; 4: diagonal alta, horizontal baixa, subida e varredura girando, em sequência; 5: X com as duas e o terceiro corte baixo pelo lado; 6: oito cortes alternando; todas saem e voltam agachado |
| 31.4 | Hoshina lutando contra um kaiju | As técnicas aparecem na luta com a postura baixa |
| 31.5 | Jogador com uma espada do Hoshina em cada mão: combo e R | Golpes diferentes alternando as lâminas; o Kūuchi é um corte rápido para fora com avanço |
| 31.6 | Jogador com o machado: R | Salta com o machado erguido e crava no chão (Axe Slam da Kikoru) |
| 31.7 | Servidor dedicado + 2 clientes | Os dois veem as mesmas animações |

## 32. Kaiju novos: Philinosoma, Diclonius e Camponotus (0.7-A)

| # | Passo | Esperado |
|---|---|---|
| 32.1 | `/summon kn8:philinosoma ~ ~ ~ {NoAI:1b}` e dar a volta | Lagarto vermelho de 9 m com espinhos azuis e cauda longa; pés no centro da hitbox (F3+B), cabeça e tronco dentro dela, a cauda sai para trás |
| 32.2 | O mesmo com `kn8:diclonius` | Kaiju branco em pé, placas vermelhas e azuis nas costas, cauda em S |
| 32.3 | O mesmo com `kn8:camponotus` e `kn8:camponotus_reborn` | Formiga preta e vermelha de cabeça laranja e antenas amarelas; a revivida é azul, mesma forma |
| 32.4 | Cada um contra 3 soldados (`/summon kn8:soldier`) em dificuldade normal | Anda com a passada certa, persegue, ataca; os golpes mexem os ossos certos (braço, mandíbula, cauda, patas) |
| 32.5 | Ficar a ~18 blocos do Diclonius / ~14 do Philinosoma / ~10 da formiga | Diclonius: aviso na boca e sopro azul que explode; Philinosoma: espinhos azuis; formiga: abdômen dobra por baixo e o ácido verde (azul na revivida) deixa lento |
| 32.6 | Bater na cabeça da formiga e no peito dos Honju | Dano maior (núcleo) |
| 32.7 | Menu (M) > Bestiário depois de abater | Os 4 aparecem com nome e descrição em pt_br e en_us |
| 32.8 | Servidor dedicado + 2 clientes | Os dois veem o mesmo modelo, animação e ataque |

## 33. Cogumelos e larva (0.7-B)

| # | Passo | Esperado |
|---|---|---|
| 33.1 | `/summon kn8:phaneroplasmodium ~ ~ ~ {NoAI:1b}` e dar a volta | Cogumelo branco e vermelho de 5 m sobre oito patas, boca com dentes sob o chapéu |
| 33.2 | O mesmo com `kn8:myxogasterocarp` | Cogumelo escuro de 9 m com chapéus vermelhos empilhados e raízes como patas |
| 33.3 | `/summon kn8:kaiju_larva` perto de você (sobrevivência, dificuldade normal) | Larva pequena com asas: levanta voo, circula em volta e mergulha para morder |
| 33.4 | Cogumelos contra 3 soldados | Andam com as patas/raízes, atacam com as patas; o chapéu recua e solta esporos (Phaneroplasmodium deixa lento; Myxogasterocarp explode) |
| 33.5 | Bater no chapéu do Phaneroplasmodium e no caule do Myxogasterocarp | Dano maior (núcleo) |
| 33.6 | Menu (M) > Bestiário depois de abater | Os 3 aparecem com nome e descrição em pt_br e en_us |
| 33.7 | Servidor dedicado + 2 clientes | Os dois veem o mesmo modelo, animação e ataque |

## 34. Reno, Mina e Narumi (0.7-C)

| # | Passo | Esperado |
|---|---|---|
| 34.1 | Ovos do Reno, da Mina e do Narumi (aba do mod) | Os três nascem com o modelo do Miguel; Reno com o rifle, Mina com o canhão no quadril, Narumi com a baioneta nas duas mãos |
| 34.2 | Cada um perto de um Primigenius (dificuldade normal) | Reno e Mina ficam longe atirando; Narumi avança e estoca |
| 34.3 | Mina contra um Honju | Tiro do canhão e canhão carregado explodem no alvo (sem quebrar blocos); Anti-Giant com preparo longo e agachada |
| 34.4 | Reno | Rajada, munição congelante (o kaiju fica lento, partículas azuis), coronhada se o kaiju chega perto |
| 34.5 | Narumi | Estocadas em sequência, investida, varrida que empurra e tiro rosa da baioneta |
| 34.6 | Pegar o canhão e a baioneta na aba do mod | Aparecem na mão do jogador (perfis provisórios: rifle e machado) |
| 34.7 | Servidor dedicado + 2 clientes | Os dois veem as mesmas posturas, golpes, tiros e auras |

## 35. Formas com as armas numeradas (0.7-D)

| # | Passo | Esperado |
|---|---|---|
| 35.1 | Ovo da Kikoru (Numbers 4) e olhar de costas | Casaco longo, 4 asas azuis em X nas costas, recolhidas no chão |
| 35.2 | Kikoru (Numbers 4) perto de um kaiju (dificuldade normal) | Decola, paira acima do kaiju com as asas batendo e as pernas recolhidas; desce em mergulho a cada técnica e sobe de novo; sem alvo, pousa |
| 35.3 | Ovo do Reno (Numbers 6) perto de um Honju | Traje azul; lança de gelo, explosão congelante (o kaiju fica lento), campo de gelo quando o kaiju chega perto |
| 35.4 | Ovo do Gen Narumi (Numbers 1) contra um kaiju | Mesmo modelo do Narumi, aura rosa e branca; apara e contra-ataca mais vezes, golpe que expõe o núcleo |
| 35.5 | Servidor dedicado + 2 clientes | Os dois veem o voo, as asas e os golpes iguais |

## 36. Kafka e Kaiju No. 8 (0.7-F)

| # | Passo | Esperado |
|---|---|---|
| 36.1 | Ovo do Kafka perto de um Primigenius (dificuldade normal) | Kafka de traje da Força de Defesa atira com o rifle e apanha (24 de vida) |
| 36.2 | Quando a vida dele cai abaixo da metade | Explosão de fumaça e rugido: no lugar surge o Kaiju No. 8 com a vida cheia e o mesmo alvo |
| 36.3 | No. 8 lutando | Guarda de luta, sequência de socos, soco pesado que empurra, investida, golpe no chão (explosão), rugido (deixa lento); regenera abaixo da metade |
| 36.4 | Matar o kaiju e esperar 15 s | O No. 8 volta a ser o Kafka |
| 36.5 | Servidor dedicado + 2 clientes | Os dois veem a troca de forma ao mesmo tempo |

## 37. Kikoru Shinomiya, traje normal (0.5.0-D8)

| # | Passo | Esperado |
|---|---|---|
| 37.1 | Ovo da Kikoru Shinomiya (aba do mod) | Kikoru de 1,57 m com marias-chiquinhas, machado de duas mãos e aura amarela com raios |
| 37.2 | Kikoru perto de um Primigenius (dificuldade normal) | Postura baixa com o machado; corte, golpe pesado em giro, onda de choque, investida, golpe no chão e quebra-guarda |
| 37.3 | `/kn8 invasion start kn8:honju_assault` | A Kikoru aparece entre os defensores |
| 37.4 | Servidor dedicado + 2 clientes | Os dois veem os mesmos golpes e a aura |

## 38. Formas do Kaiju No. 9 (0.7-E)

| # | Passo | Esperado |
|---|---|---|
| 38.1 | `/summon kn8:kaiju_no9_black`, `kaiju_no9_fusion` e `kaiju_no9_camponotus` | Forma preta (1,9 m, preta com espinhos vermelhos e sorriso branco), fusão (5 m, vermelha e branca com cauda) e formiga (6 m, torso do No. 9 sobre a formiga) |
| 38.2 | `/summon kn8:kaiju_no9` e baixar a vida para menos de 40% (`/data modify entity @e[type=kn8:kaiju_no9,limit=1] Health set value 500f`) | Explosão de fumaça, aviso "está mutando": surge a forma preta com a vida cheia |
| 38.3 | No. 9 com menos de 75% da vida e um `kaiju_no10_small` a até 24 blocos | Aviso "está absorvendo", tentáculo vermelho puxando o No. 10 por 3 s; o No. 10 some e surge a fusão |
| 38.4 | No. 9 com menos de 60% da vida e uma `camponotus` por perto | Mesma absorção (2,5 s); surge a forma formiga |
| 38.5 | Forma preta contra soldados | Braço-lâmina, tentáculo (deixa lento), espinhos do corpo; às vezes a pele endurece (faíscas e som de bigorna) e o dano cai pela metade |
| 38.6 | Fusão e forma formiga contra soldados | Fusão: rajada de cauda e raio híbrido que explode; formiga: salto longo, mordidas em sequência e ácido |
| 38.7 | Servidor dedicado + 2 clientes | Os dois veem as trocas de forma, os tentáculos e os ataques iguais |

## 39. Balanceamento v1.2 (todos os kaiju e personagens)

| # | Passo | Esperado |
|---|---|---|
| 39.1 | `/summon kn8:hoshina` e `/summon kn8:kaiju_no9` a 16 blocos, terreno aberto | O Hoshina leva o No. 9 a 15% em ~40–50 s e o No. 9 foge (`docs/BALANCEAMENTO_v1_2_APLICADO.md` tem a matriz inteira) |
| 39.2 | Bater no No. 9 só com o mesmo golpe (ex.: só a espada) e depois trocar de arma | O dano de cada golpe cai aos poucos (até −20%); com a arma nova volta ao normal (análise do No. 9) |
| 39.3 | `/summon kn8:kaiju_no9_black` e ficar a 10–20 blocos | Às vezes 3 tiros em leque (Multi-Finger Gun) e salto longo (voo curto) |
| 39.4 | `/summon kn8:kaiju_no9_fusion` a 10–25 blocos | 5 tiros em leque que explodem (Multi-Finger Cannon) e o canhão de dedo do No. 10 |
| 39.5 | `/summon kn8:kaiju_no9_camponotus` a 8–20 blocos | Investida longa (Ant Rush) |
| 39.6 | `/summon kn8:kaiju_no8` contra `kn8:kaiju_no10_small` | O No. 8 vence em ~25 s com ~80% da vida (antes: 5 s) |
| 39.7 | `/summon kn8:mina` a 40 blocos de `kn8:kaiju_no10_giant` | Anti-Giant Shot (preparo de 2,5 s) tira 4–8% da vida do gigante por tiro (cresce com o Release dela); ela ainda perde (falta recuar: pendência) |
| 39.8 | Servidor dedicado + 2 clientes | Os dois veem os mesmos tiros em leque e a mesma vida nas barras |

## 40. Alturas da tabela mestra (2026-10-15)

| # | Passo | Esperado |
|---|---|---|
| 40.1 | Em fila: `/summon kn8:soldier ~ ~ ~ {kn8_power_level:"low"}` e `{kn8_power_level:"elite"}`, `kn8:hoshina`, `kn8:mina`, `kn8:reno`, `kn8:kikoru`, `kn8:narumi`, `kn8:kafka` | Recruta 1,70, elite 1,85, Hoshina 1,72, Mina e Reno 1,75, Kikoru 1,60, Narumi 1,78, Kafka 1,81 (F3+B mostra a hitbox no mesmo tamanho do corpo); armas na mão e animações iguais às de antes |
| 40.2 | `/summon kn8:trichonephila` e `kn8:trichonephila_honju` | Aranha com 4,5 m de altura e Tecedeira com 9 m; patas e golpes iguais, só maiores |
| 40.3 | `/summon kn8:primigenius`, `kn8:primigenius_honju`, `kn8:preondactyl` | 5,5 m, 8,5 m e Preondactyl com o corpo de 7 m |
| 40.4 | `/summon kn8:kaiju_no9`, `kn8:kaiju_no9_black`, `kn8:kaiju_no9_camponotus` | No. 9 continua com 2 m; forma preta 5 m; formiga 11 m |
| 40.5 | `/summon kn8:kaiju_no9_fusion` e baixar a vida para menos da metade (`/data modify entity @e[type=kn8:kaiju_no9_fusion,limit=1] Health set value 2000f`) | Aviso "virou um gigante de 20 metros"; surge a fusão gigante (20 m) com a vida cheia, que anda e ataca com as animações da fusão |
| 40.6 | Matar um kaiju grande (`/kill`) | A carcaça cai no tamanho dele e o desmonte funciona |
| 40.7 | Servidor dedicado + 2 clientes | Os dois veem os mesmos tamanhos |

## 41. Parkour do jogador (0.5.0-E)

Tecla nova: **C** (Parkour; troca em Opções > Controles > Kaiju No. 8). Números em `[parkour]` do `kn8-server.toml`.

| # | Passo | Esperado |
|---|---|---|
| 41.1 | Correr (Ctrl+W) contra um muro de 1 bloco e pular | Vault: passa por cima com as mãos no muro, sem parar; gasta um pouco de stamina |
| 41.2 | Andar contra um muro de 2 blocos e pular (com ou sem correr) | Escalada curta: sobe o muro de 2 (o vanilla só sobe 1,25); muro de 3+ não sobe |
| 41.3 | Correndo no chão, apertar C | Salto longo (~8 blocos) com os braços para cima; sem stamina, nada |
| 41.4 | Correndo, pular ao lado de uma parede alta e segurar C | Corre pela parede (sobe um pouco e desce) por até 1,5 s, inclinado para a parede; segurando C a mesma parede não recomeça sem pousar |
| 41.5 | No wall-run, apertar Espaço | Wall-jump: salta para longe da parede; dá para encadear entre duas paredes (até 4 sem tocar o chão) |
| 41.6 | Cair encostado e de frente para uma parede segurando C | Escorrega devagar (wall-slide) sem dano de queda; Espaço = wall-jump, Shift = solta |
| 41.7 | Pular e segurar C perto de uma borda na altura das mãos (muro de 2–3 blocos) | Segura a borda pendurado (até 3 s); Espaço sobe, Shift solta |
| 41.8 | Correndo, apertar Shift | Slide deitado (mergulho de cabeça) por ~1 s; passa por vão de 1 bloco; soltar Shift levanta |
| 41.9 | Cair de 6 blocos segurando Shift | Rolamento: cambalhota sem dano; de 10 blocos, dano bem menor (−60%) |
| 41.10 | Servidor dedicado + 2 clientes | O outro jogador vê as animações (vault, wall-run, borda, slide deitado, rolamento) |

## 42. Base da Força de Defesa (construções, etapa C1)

| # | Passo | Esperado |
|---|---|---|
| 42.1 | Em área plana carregada: `/place structure kn8:defense_base` | Base de ~65 x 65: QG branco de 3 andares com faixa ciano no centro, 4 alas sorteadas, torre de vigia em cada canto, muro de 4 blocos fechando tudo com portão listrado em cada lado |
| 42.2 | Entrar no QG (portas nos 4 lados) | Térreo iluminado com 2 bancadas, baú, mesa de cartografia e atril; escada de pedra até o 1º andar (sala de comando), 2º andar (sala do capitão, cama, estantes) e telhado (heliporto com H amarelo, grade, antena) |
| 42.3 | Andar pelas alas | Alojamento (dois prédios com beliches e armários), arsenal (oficina com 2 bancadas, bigorna, baús de munição e 2 caminhões), treino (bonecos, alvos e pista de parkour), hangar (heliporto e helicóptero de blocos) e enfermaria (camas brancas, poção, baús de remédio, cruz vermelha); as alas variam a cada base |
| 42.4 | Pista de parkour da ala de treino | Muro de 1 (vault), muro de 2 (escalada), corredor de parede (wall-run e wall-jump), borda de 3 (segurar e subir) e barra baixa (slide) |
| 42.5 | Soldados e bonecos | ~15 soldados de guarda (portas do QG, portões, torres) e 3 bonecos de treino; os baús abrem com o loot da base (comando, arsenal, enfermaria, alojamento) |
| 42.6 | Mundo novo em planície, savana, deserto ou neve | Bases aparecem sozinhas, bem mais raras que o posto avançado (uma a cada ~1.150 blocos) |
| 42.7 | Servidor dedicado + 2 clientes | Os dois veem a mesma base |

## 43. Cidade e base grande com arena de treino (construções C2)

| # | Passo | Esperado |
|---|---|---|
| 43.1 | Área plana carregada (`/forceload add` em volta): `/place structure kn8:city` | Cidade com praça central (fonte, 4 árvores, monumento azul, bancos), ruas que se ramificam com faixa amarela, postes e cruzamentos com faixa de pedestre; dos lados das ruas: loja de conveniência, restaurante de ramen, casas com telhado, prédios de 4 e 8 andares com sacadas, torre de escritório de vidro com heliporto, parque com lago, estacionamento com carros e prédio destruído por kaiju; ruas terminam em barreira listrada |
| 43.2 | Entrar nas construções da cidade | Portas abertas, escada de mão nos prédios, baús de loja e de casa com loot |
| 43.3 | `/place structure kn8:defense_base_large` (precisa de ~130 x 130 carregado) | Base grande: centro com o QG, refeitório, garagem com 3 caminhões, torre de rádio e pátio de formatura com soldados; 3 alas duplas (duas alas da base pequena lado a lado + estacionamento); cantos com torre, tanques e heliporto; do 4º lado a arena de treino |
| 43.4 | Arena de treino | Arena fechada de paredes escuras (51 x 41, 16 de altura) com luzes, coberturas por dentro, arquibancada com janelas de vidro, laboratório de kaiju (jaulas) e enfermaria; portão listrado para entrar e o Painel da Arena de Treino na frente |
| 43.5 | Clicar no painel (com ou sem arma na mão) | "Treino começou: Primigenius está na arena!" e o kaiju aparece no centro da arena e persegue quem está dentro |
| 43.6 | Shift + clique no painel de mão vazia | "Kaiju de treino: ..." troca a cada clique (aranha, Primigenius, formiga, cogumelo, ressurgido, Honju, Tecedeira, Philinosoma, Diclonius, Myxogasterocarp, revivido, No. 9, No. 10 pequeno) |
| 43.7 | Lutar dentro da arena | O kaiju não quebra as paredes; se sair (salto, fuga do No. 9, `/tp`), volta ao centro em 1 s; morto, some em fumaça sem carcaça, sem mérito e sem desmonte |
| 43.8 | Clicar no painel com o treino em andamento | "Treino encerrado." e o kaiju some |
| 43.9 | Sair para longe (mais de 24 blocos da arena) por 1 minuto | O treino acaba sozinho |
| 43.10 | Servidor dedicado + 2 clientes | Os dois veem a cidade, a base, o kaiju e a luta iguais |

## 44. IA dos kaiju: detecção, alvo, busca, travamento e atordoamento (IA-1A)

| # | Passo | Esperado |
|---|---|---|
| 44.1 | Dois soldados (`/kn8 soldier spawn`), um a 4 e outro a 12 blocos de um Primigenius; dar o alvo do longe (`/kn8 kaiju` ou deixar ele escolher) | Em até 1 s o kaiju vai para o soldado mais perto; morto esse, passa para o próximo (todos em volta apanham) |
| 44.2 | Esconder-se atrás de um muro a mais de ~12 blocos de um Primigenius que não viu você | Ele não percebe; perto (menos de ~11 blocos) ou depois de levar um tiro seu, percebe |
| 44.3 | No. 10 pequeno com um soldado de espada perto e um de rifle a ~10 blocos | O No. 10 ignora o espadachim e vai atrás do atirador; quem mais o fere também vira prioridade |
| 44.4 | Soldado parado em cima de um pilar de bedrock de 8 blocos, você no chão a ~15 blocos | O kaiju fica embaixo do pilar, pula (tentativa de destravar), em ~10–14 s desiste e vem em você; volta ao pilar depois de ~20 s se nada mais estiver por perto |
| 44.5 | Fugir do campo de visão do kaiju (atrás de uma casa, longe) | Ele vai até onde te viu por último, olha em volta e, sem achar, desiste em ~10 s |
| 44.6 | `/kn8 kaiju stagger 100` (ou parry/especial que atordoa) com o kaiju perseguindo | Fica parado no lugar ~5 s (sem andar nem girar para o alvo) e depois volta a perseguir |
| 44.7 | No. 9 comandando kaiju sem alvo | Os comandados atacam o alvo do No. 9 por 10 s e depois voltam a escolher sozinhos |
| 44.8 | Servidor dedicado + 2 clientes | Os dois veem o mesmo comportamento |

## 45. IA dos soldados: detecção, recarga, cobertura e arma branca (IA-1B)

| # | Passo | Esperado |
|---|---|---|
| 45.1 | Ovo de soldado várias vezes | Só nascem atiradores (rifle ou pistola, faca na outra mão); espada e faca só por `/kn8 soldier spawn sword/knife` |
| 45.2 | Muro de bedrock entre dois soldados de rifle e um Primigenius do outro lado | Em 1–2 s os soldados contornam o muro (detectam sem ver) e atiram; antes ficavam parados |
| 45.3 | Kaiju a ~50 blocos de um soldado, atrás de uma casa | O soldado vai até ele |
| 45.4 | Soldado de rifle lutando até esvaziar o pente | Recarrega recuando (se afasta ~6 blocos além da distância normal) e volta a atirar |
| 45.5 | Soldado com o pente pela metade e sem kaiju por perto | Recarrega sozinho em ~1–2 s (gasta um pente reserva) |
| 45.6 | Luta longa (No. 10 gigante sem IA: `/summon kn8:kaiju_no10_giant ~ ~ ~20 {NoAI:1b}`) | Depois de 4 pentes (~1 min) o soldado troca para a faca e vai lutar de perto, ficando com a arma de fogo na outra mão |
| 45.7 | Reno ou Mina até acabar a munição | Pegam uma faca (não ficam de mão vazia) e lutam de perto |
| 45.8 | Dois soldados, um recarregando com o kaiju em cima dele | O outro soldado passa a atirar nesse kaiju (cobertura) |
| 45.9 | Servidor dedicado + 2 clientes | Os dois veem o mesmo comportamento |

## 46. Combate: destruição, golpe pesado sem bloqueio, rajada, munição e Anti-Giant (IA-2)

| # | Passo | Esperado |
|---|---|---|
| 46.1 | Fique dentro de uma casa de tijolo/pedra com um Primigenius a ~10 blocos (`gamerule mobGriefing true`) | O kaiju bate na parede e abre um rombo em poucos golpes; entra e ataca |
| 46.2 | Honju ou No. 10 usando golpe pesado perto de construções | Quebra mais (raio maior, também deepslate/ferro com o bônus de força) |
| 46.3 | Raio/sopro de kaiju batendo num muro | O tiro quebra o ponto onde bateu |
| 46.4 | Kikoru, Mina, Narumi e Kaiju No. 8 lutando perto de pilares de pedra | Os golpes pesados e os tiros de canhão quebram os blocos em volta do alvo |
| 46.5 | Hoshina, Reno, soldados comuns lutando perto de pilares | Não quebram nada |
| 46.6 | Bloquear (com o jogador) o raio de energia do Honju | O bloqueio não segura (dano cheio); só a esquiva evita |
| 46.7 | Bloquear um golpe comum de kaiju | O bloqueio continua reduzindo o dano; parry continua atordoando só no corpo a corpo |
| 46.8 | No. 9 forma preta com pele endurecida (partículas de ferro) levando golpe pesado do machado | O golpe pesado passa inteiro |
| 46.9 | Multi-Finger Gun do No. 9 em você | Cada tiro da rajada tira vida (antes só um contava) |
| 46.10 | Soldado de rifle numa luta longa | 5 pentes no total (150 tiros); recarregar sem alvo não perde as balas que sobravam |
| 46.11 | Mina contra um Primigenius pequeno | Pode usar o Anti-Giant; depois de 4 tiros não usa mais (nem depois de sair e voltar ao mundo) |
| 46.12 | Servidor dedicado + 2 clientes | Os dois veem a mesma destruição |

## 47. Personagens: perfis táticos e guarda contra golpe pesado (IA-3)

| # | Passo | Esperado |
|---|---|---|
| 47.1 | Hoshina e Narumi contra um Primigenius Honju | Depois de algumas técnicas eles se afastam para o lado (reposicionam) em vez de emendar golpe atrás de golpe; continuam vencendo |
| 47.2 | Narumi, No. 8, Hoshina ou Kikoru levando um golpe pesado de frente (raio do Honju, soco pesado) | Faíscas e animação de defesa; levam cerca de metade do dano (não zero) |
| 47.3 | Hoshina No. 10 levando golpe pesado pelas costas | A cauda defende (como antes) |
| 47.4 | Mina com uma aranha perto e um Honju mais longe | Ela mira o Honju |
| 47.5 | Reno com um soldado sendo atacado por um kaiju e outro kaiju mais perto dele | Reno prefere o kaiju que ataca o aliado |
| 47.6 | Mina ou Reno com um kaiju preparando golpe pesado em cima deles | Esquivam (às vezes), nunca dão o dash de espada para dentro do kaiju |
| 47.7 | Kafka humano perdendo vida (antes de virar o No. 8) | Abaixo de ~60% ele recua por alguns segundos e volta |
| 47.8 | Kaiju No. 8 contra um Honju | Pressiona sem parar, prefere os golpes pesados |
| 47.9 | Servidor dedicado + 2 clientes | Os dois veem o mesmo comportamento |

## 48. Kaiju por espécie (IA-4)

| # | Passo | Esperado |
|---|---|---|
| 48.1 | Três Primigenius contra você (resistência ligada) | Chegam por lados diferentes (cercam), não ficam um em cima do outro; às vezes circulam em volta antes de voltar a atacar |
| 48.2 | Bata num Primigenius até ~20% da vida | Ele recua uns blocos por ~2 s e volta |
| 48.3 | Lute com um Primigenius Honju | Depois do soco pesado, pisão ou raio, fica ~1 s parado (janela para atacar); gira devagar para te seguir |
| 48.4 | Fique longe (~10 blocos) de uma Trichonephila | Ela prefere a teia; perto, morde |
| 48.5 | No. 10 pequeno | Persegue mais rápido; a janela depois do golpe pesado é curta |
| 48.6 | Preondactyl contra soldados | Mergulha só quando vai golpear e o golpe acerta (desce até o chão do alvo) |
| 48.7 | Revivido enfurecido (abaixo de 30%), salve e reabra o mundo | Continua enfurecido e não ruge de novo |
| 48.8 | Servidor dedicado + 2 clientes | Os dois veem o mesmo comportamento |

## 49. Esquadrões (IA-5)

| # | Passo | Esperado |
|---|---|---|
| 49.1 | `/kn8 rank set kn8:oficial Dev`, menu (M) > Esquadrão, escolha um soldado e Recrutar | Recusado: "Sua patente ainda não comanda esquadrão"; botões de ordem cinza |
| 49.2 | `/kn8 rank set kn8:lider_pelotao Dev`, recrute 3 soldados comuns e o Hoshina | Os 3 entram (faixa verde e "Seguir" na linha, "Esquadrão: 3/4"); o Hoshina é recusado (especial só de Vice-Capitão para cima) |
| 49.3 | Ande uns 20 blocos | Os 3 vêm atrás e param em volta de você, cada um num lado |
| 49.4 | Ordem Ficar e afaste-se 40 blocos | Ficam no lugar; um kaiju a até 20 blocos do posto é atacado, um mais longe não |
| 49.5 | Com eles lutando, ordem Recuar | Largam o kaiju e correm até você |
| 49.6 | Ordem Atacar | Caçam qualquer kaiju no alcance (64 blocos) |
| 49.7 | Escolha um membro e Dispensar | Sai do esquadrão (sem faixa verde) e volta a agir sozinho |
| 49.8 | `/kn8 invasion start kn8:honju_assault` | Os 7 defensores nascem em 3 grupos (postos a ~12 blocos do centro); no aviso ficam no posto |
| 49.9 | `/kn8 invasion next` | Cada grupo vai atrás de um kaiju diferente; quando o dele morre, pega outro; entre ondas voltam ao posto |
| 49.10 | Vários soldados de perto (Hoshina, Kikoru, Narumi) contra o mesmo kaiju | Chegam por lados diferentes, sem ficar um em cima do outro |
| 49.11 | Servidor dedicado + 2 clientes | O segundo cliente vê os mesmos movimentos; o menu do outro jogador não mostra o seu esquadrão como dele |

## 50. Chefes, aviso de golpe pesado e adaptação (IA-6)

| # | Passo | Esperado |
|---|---|---|
| 50.1 | Lute com um Primigenius Honju | Antes do pisão aparece um anel de partículas no chão onde ele vai bater; antes do soco pesado, um arco à frente dele; antes da investida e do raio, uma linha até você. Começa amarelo e fica vermelho perto do impacto; um som grave avisa no começo |
| 50.2 | Saia do anel/linha quando aparecer | O golpe não acerta (dá tempo de reagir) |
| 50.3 | `/kn8 boss spawn kn8:honju` (ou missão do chefe) e lute acima de 50% | Usa mais soco pesado e pisão |
| 50.4 | Leve o chefe abaixo de 50% | "Fase 2", ruge; passa a usar mais investida e raio |
| 50.5 | Bata no No. 9 sempre com a mesma arma por ~30 s, depois troque | Com a mesma arma as faíscas cinzas aumentam e o dano cai (no máximo o teto do JSON, nunca imune); com a arma nova volta o dano cheio |
| 50.6 | Servidor dedicado + 2 clientes | O segundo cliente vê os mesmos avisos |

## 51. Dificuldade e desempenho da IA (IA-7)

| # | Passo | Esperado |
|---|---|---|
| 51.1 | `/difficulty easy`, lute com um Primigenius Honju | O aviso do pisão dura mais (dá mais tempo para sair) |
| 51.2 | `/difficulty hard` | O aviso continua aparecendo, mas o golpe chega mais rápido |
| 51.3 | `debugLog = true` no `kn8-server.toml` (reiniciar), lute | O log do servidor mostra `[kn8 IA] <entidade> alvo X -> Y (motivo)` |
| 51.4 | `python3 tools/ai/carga_ia.py 40 40 40` com o servidor ligado | Média abaixo de ~12 ms/tick nesta máquina de testes (80 entidades lutando) |
| 51.5 | Invasão de nível 4–5 com 2 clientes | Sem travadas novas; os kaiju longe de todos continuam lutando (só trocam de alvo mais devagar) |

## 52. Kaiju e soldados presos (correção de 2026-10-16)

| # | Passo | Esperado |
|---|---|---|
| 52.1 | Mate um kaiju grande no meio de soldados | Os soldados atravessam a carcaça (não ficam presos embaixo nem atrás dela); você continua batendo nela para desmontar |
| 52.2 | `/place structure kn8:defense_base`, No. 10 pequeno do lado do QG, leve-o abaixo de 50% | A forma gigante nasce no chão (não em cima do QG/prédios) e quebra o que ocupa o corpo |
| 52.3 | `/summon kn8:primigenius_honju` em cima do telhado do QG | Ele quebra os andares embaixo e desce até o chão em poucos segundos |
| 52.4 | Kaiju num morro natural acima de você | Não cava o morro (só construções) |
| 52.5 | Kaiju preso entre prédios sem conseguir chegar no alvo | Quebra o caminho à frente em vez de desistir parado |
| 52.6 | `/gamerule mobGriefing false` | Nada é quebrado; a forma nova nasce no chão livre mais próximo, como antes |

## 53. Locais da obra: Ariake, Tachikawa e Shinagawa (construções C3)

Mundo **novo** (as estruturas só nascem em chunks novos). Pesquisa e fontes: `docs/LOCAIS_KAIJU_N8.md`.

| # | Passo | Esperado |
|---|---|---|
| 53.0 | No `server.properties`, `generate-structures=true` (com `false` nenhuma estrutura nasce, mas o `/locate` ainda procura e nunca acha) | — |
| 53.1 | `/locate structure #kn8:defense_sites` (qualquer local) e `/locate structure kn8:ariake_base` (e `tachikawa_base`, `shinagawa_city`, `tachikawa_city`) | Acha cada um em segundos; os locais ficam a alguns milhares de blocos uns dos outros |
| 53.2 | `/tp` até cada um e olhe em volta (F5, voando) | Nenhum em cima de lago, rio ou mar; nenhuma vila, posto de saqueadores ou outra construção grudada |
| 53.3 | Entre andando na base de Ariake | Título "Base Marítima de Ariake" / "Força de Defesa, Primeira Divisão"; na primeira vez aparecem o Narumi e 2 soldados elite na entrada oeste |
| 53.4 | Passeie por Ariake | QG de vidro de 7 andares com heliporto, laboratório de armas numeradas, celas (a do Kafka), catapulta eletromagnética, salão das divisões com 6 bandeiras; ao sul, o porto (cais, bacia com água, 2 barcos, guindastes, contêineres, farol) |
| 53.5 | Entre na base de Tachikawa | Título "Base de Tachikawa" / "Terceira Divisão"; na primeira vez Mina, Hoshina, Kikoru e Reno aparecem |
| 53.6 | Passeie por Tachikawa | QG comprido com pórtico e torre com o "3" laranja, sala de operações, pátio de formatura; a arena de treino (painel funciona) e o aeródromo (pista, hangares com helicópteros, torre de controle, tanques) |
| 53.7 | Entre em Shinagawa | Estação com trilhos elevados, Shinkansen e plataforma (escada de mão no pilar); torres do Intercity; ruas com torres altas, calçadão coberto e restos de formigueiro |
| 53.8 | Entre na cidade de Tachikawa | Monotrilho elevado com estação e trem (escada de mão), estação da JR dos dois lados ligada pela passarela, loja de departamentos, Parque Showa Kinen (girassóis, lago) e a sala de shows |
| 53.9 | Saia e entre de novo num local; outro jogador entra | O título aparece de novo para quem entra; a guarnição **não** aparece de novo (uma vez por mundo) |
| 53.10 | Dentro de um local, `/time set 12990` e espere (com `naturalChance` 1 no config) | A invasão é a do local (ex.: Raide do No. 10 em Tachikawa) e começa na entrada da peça inicial |

## 54. Isao (Numbers 2), No. 9 com o Isao e Kaiju No. 13 (0.8-A)

Fichas e fontes: `docs/FICHAS_0_8_A.md`. Servidor dedicado + 2 clientes, mundo plano.

| # | Passo | Esperado |
|---|---|---|
| 54.1 | Ovo do Isao Shinomiya (aba criativa) ao lado de um Dev | Isao de 1,90 m, cabelo loiro e manoplas brancas/pretas nos dois braços, aura laranja com faíscas; em guarda de punhos na frente do rosto |
| 54.2 | `/summon kn8:primigenius` a 10 blocos do Isao | Ele vai até o kaiju: sequência de 5 socos (Barrage Blast), onda que vai longe (Spiral Blast), leque de ondas abrindo os braços (Force Blast) e o soco duplo (Thunder Emperor) que deixa o núcleo do kaiju exposto (brilho) |
| 54.3 | `/summon kn8:kaiju_no9` perto do Isao e deixe lutarem (ou `/data modify entity <isao> Health set value 150`) | Quando o Isao cai abaixo de 30%, o No. 9 o agarra com tentáculos, aviso "O Kaiju No. 9 absorveu o Isao Shinomiya e a arma numerada 2!" e surge o **No. 9 com o Isao** no lugar (preto, espinhos vermelhos, boca com dentes, **8 m** de altura) |
| 54.4 | Isao inteiro perto do No. 9 | O No. 9 **não** absorve (só quem ele já derrotou) |
| 54.5 | `/summon kn8:kaiju_no9_isao` contra o Hoshina No. 10 ou o Kaiju No. 8 | Chefe muito forte (7.500 de vida): rajada de socos, soco duplo pesado que quebra blocos, onda em volta dele, onda sônica a distância, Finger Gun, tentáculo e voo curto; regenera abaixo de 50% |
| 54.6 | `/summon kn8:kaiju_no13` perto de soldados | Humanoide branco de **6 m** com braços/pernas vermelhos; rápido (investida e salto), agarra pelo pescoço (o alvo fica quase parado 2 s), sequência de socos e soco pesado |
| 54.7 | Kaiju No. 8 contra o No. 13 | O No. 8 vence com folga (na obra ele o derruba com um soco) |
| 54.8 | Mundo novo, Base Marítima de Ariake | Na guarnição, além do Narumi, aparece o Isao; no `ariake_assault` o Isao defende e o No. 13 pode vir na última onda |

## 55. Navegação confiável dos soldados (MOV-1)

Servidor dedicado + 2 clientes, mundo plano. Para ver os motivos no log do servidor: `[kaijuAi] debugLog = true`
(linhas `[kn8 IA] ... sem pouso valido`, `esquiva sem espaco`, `travado ...`).

| # | Passo | Esperado |
|---|---|---|
| 55.1 | Plataforma de pedra de 5 de altura; Hoshina na borda e um Primigenius Honju em cima | O Hoshina não se joga da plataforma no dash, na esquiva nem no avanço das técnicas (pode descer andando); com o kaiju atrás da parede da plataforma, o avanço é recusado no log |
| 55.2 | Corredor de 1 bloco de largura no alto (queda dos dois lados), Hoshina lutando | A esquiva escolhe o lado com chão; sem nenhum, ele fica e o log diz "esquiva sem espaco" |
| 55.3 | Beco em U de bedrock com 2 soldados de espada e um Primigenius parado (`NoAI`) no fundo | Os soldados chegam e **acertam** o kaiju (antes paravam a ~4 blocos sem dano: o golpe mirava o centro do corpo alto); kaiju perde vida |
| 55.4 | Soldado comum preso atrás de um muro de 2 tentando chegar ao kaiju | Em ~3 s ele sai para um lado com chão (ou volta) e tenta de novo; nunca teleporta |
| 55.5 | Degraus de pedra de 1 bloco entre soldado/Hoshina e o kaiju | Sobem e lutam normalmente |

## 56. Saltos táticos dos soldados (MOV-2)

Servidor dedicado + 2 clientes, mundo plano. Log dos saltos com `[kaijuAi] debugLog = true` (`[kn8 IA] ... salto
subindo 2.0`, `salto por cima do vao`, `salto pousou`).

| # | Passo | Esperado |
|---|---|---|
| 56.1 | Plataforma de pedra de 2 blocos sem rampa (17 x 16), Primigenius parado (`NoAI`) no meio; 2 soldados de espada no chão a 6 blocos | Em ~1 s parados na parede, saltam para cima (corpo inclina, pernas encolhem) e matam o kaiju em cima |
| 56.2 | Dois platôs de 5 de altura com um vão de 3 entre eles; soldados num, kaiju no outro | Os soldados **não** pulam no fundo da vala (5 é mais que a queda de 3); saltam por cima do vão e pousam do outro lado, sem perder vida |
| 56.3 | Muro de 5 de altura entre o soldado e o kaiju | Não salta (acima de 3): contorna ou sai para o lado (MOV-1) |
| 56.4 | Muro de 2 de altura e 1 de espessura | Salta para cima dele ou por cima |
| 56.5 | Hoshina e Kikoru na cena 56.1 | Chegam ao kaiju (salto, dash ou alcance da técnica) |

## 57. Cobertura dos atiradores (MOV-3)

Servidor dedicado + 2 clientes, mundo plano. Log com `[kaijuAi] debugLog = true` (`[kn8 IA] ... cobertura em ...
com ponto de tiro`, `cobertura perdida`).

| # | Passo | Esperado |
|---|---|---|
| 57.1 | 5 muros de tijolo de 3 x 3 entre 2 soldados de rifle e uma Camponotus (cospe ácido) a 26 blocos | Os soldados vão para trás dos muros e atiram de um passo ao lado (o "ponto de tiro"); quando a formiga mira um deles, ele volta para trás do muro por ~1 s |
| 57.2 | A formiga contorna ou quebra o muro | O soldado larga aquela cobertura e procura outra (ou recua se a formiga está a menos de 9) |
| 57.3 | Soldado de rifle recarregando contra um Primigenius (sem ataque a distância) com um muro por perto | Recarrega atrás do muro |
| 57.4 | Mesmos muros contra um Primigenius, sem recarregar | Não usa cobertura: mantém distância e atira como antes |
| 57.5 | `cover = false` no config | Comportamento antigo |

## 58. Armadura cinética: sem dano de queda (Miguel, 2026-10-16)

| # | Passo | Esperado |
|---|---|---|
| 58.1 | Sobrevivência, traje Mk1 (ou de treino/Mk1 Reforçado) no peito, cair de 25 blocos | Nenhum dano |
| 58.2 | Sem traje, cair de 25 blocos | Dano normal do vanilla (morre) |
| 58.3 | `/summon` soldado e Hoshina a 20 blocos do chão | Caem sem perder vida |
| 58.4 | `soldierNoFallDamage`/`kineticSuitNoFallDamage = false` no config | Volta o dano de queda |

## 59. Combate dos soldados (MOV-4)

Servidor dedicado + 2 clientes, mundo plano. Log com `[kaijuAi] debugLog = true`.

| # | Passo | Esperado |
|---|---|---|
| 59.1 | 3 soldados de espada (alto) colados num Primigenius Honju | Quando o anel/arco/linha do golpe pesado aparece no chão, os soldados saem andando dele (log "saindo da area do golpe pesado") e voltam a atacar depois; duram ~15 s e tiram ~250 de vida (com `avoidDanger = false`: ~3 s, a investida pega os três) |
| 59.2 | Hoshina contra o Honju | Esquiva para fora da área real do golpe (de lado na investida, para fora do anel no pisão) |
| 59.3 | Muro de bedrock entre o Hoshina e um golem colado | Nenhuma técnica sai e nenhum golpe passa pela parede |
| 59.4 | Hoshina começa o Ran-uchi e o alvo morre no meio (`/kill`) | A técnica e a animação param na hora |
| 59.5 | Dois soldados de rifle, um na frente do outro, contra um kaiju | O de trás não atira através do da frente: dá um passo para o lado e atira |
| 59.6 | Hoshina contra uma Camponotus (cospe ácido) | Esquiva de lado de alguns jatos (pela chance de esquiva do perfil) |

## 60. Troca de perfil: Kafka <-> Kaiju No. 8 (MOV-5A)

Servidor dedicado + 2 clientes, mundo plano. Log com `[kaijuAi] debugLog = true`.

| # | Passo | Esperado |
|---|---|---|
| 60.1 | Recrutar o Kafka (Esquadrao, patente com vaga), ordem Seguir, soltar um Primigenius perto | O Kafka atira; quando cai abaixo de 50% vira o Kaiju No. 8 (explosao, fumaca, rugido); log "perfil ativo kn8:kaiju_no8 (antes kn8:kafka)" |
| 60.2 | Aba Esquadrao do menu durante a luta | O No. 8 aparece no esquadrao do jogador (mesma vaga do Kafka) e segue as ordens (Ficar/Recuar/Atacar) |
| 60.3 | Matar o kaiju e esperar 15 s | O No. 8 volta a ser o Kafka, ainda no esquadrao, com a mesma ordem e o rifle com a municao que tinha (nao enche de novo) |
| 60.4 | Kafka ja sem municao (faca na mao) antes de virar o No. 8 | Volta do No. 8 com a faca |
| 60.5 | Invasao `honju_assault` com o Kafka entre os defensores (`/summon` com a marca nao serve: usar invasao que o chame ou pular) | O No. 8 continua no esquadrao e no posto do Kafka |
| 60.6 | Salvar e sair com o No. 8 em campo, voltar | O No. 8 continua no esquadrao e, ao voltar a ser o Kafka, devolve a municao guardada |

## 61. Perfil do Hoshina (MOV-5B)

Servidor dedicado + 2 clientes, mundo plano. Log com `[kaijuAi] debugLog = true`.

| # | Passo | Esperado |
|---|---|---|
| 61.1 | Hoshina contra um Primigenius Honju | Depois do golpe pesado do Honju (janela parada), log "brecha do kaiju: pressiona" e logo uma técnica forte (Yae-uchi: núcleo exposto, "EXPOSED" na barra) |
| 61.2 | Hoshina contra um kaiju que foge (`/effect give <kaiju> speed`) | Log "tecnica ... recusada: o alvo sai do alcance"; ele persegue em vez de cortar o ar |
| 61.3 | Hoshina com o Release alto (luta longa) usando Yae-uchi | Os oito cortes saem um a um (som e partícula separados), nenhum some |
| 61.4 | Mesmos duelos de antes | Vence o No. 10 pequeno, perde para o gigante (como antes) |

## 62. Perfil da Kikoru (MOV-5C)

Servidor dedicado + 2 clientes, mundo plano. Log com `[kaijuAi] debugLog = true`.

| # | Passo | Esperado |
|---|---|---|
| 62.1 | Kikoru contra um Primigenius | Golpes rápidos e pesados alternados (corte rápido depois do golpe no chão / giro pesado), Primigenius cai rápido |
| 62.2 | Kikoru contra o No. 9 (datapack `kn8_duelo_isolado` ligado para ele não mudar de forma) | Abaixo de 50% o No. 9 solta partículas verdes (regenera); log "alvo regenerando: pressiona" e ela não se afasta; às vezes ela o leva a 15% (foge), na maioria perde |
| 62.3 | Kikoru No. 4 contra o No. 9 | Voa, mergulha nos golpes e o leva a 15% |

## 63. Perfil da Mina (MOV-5D)

Servidor dedicado + 2 clientes, mundo plano. Log com `[kaijuAi] debugLog = true`.

| # | Passo | Esperado |
|---|---|---|
| 63.1 | Mina contra um Primigenius (Yoju) | Nunca usa o Anti-Giant (tiro branco grande); recua quando ele chega; log "cortada: alvo perto demais" quando ele chega no meio do preparo |
| 63.2 | Mina contra um Primigenius Honju | Usa o Anti-Giant (até 4 por vida) e os outros tiros de longe |
| 63.3 | Muro de bedrock entre a Mina e o Honju durante o preparo de um tiro | Log "cortada: sem linha de tiro"; nenhum tiro explode no muro |

## 64. Cobertura adaptativa da Mina e do Reno (MOV-5D, decisões de 2026-10-18)

Ligar `[kaijuAi] debugLog` para ver "cobertura em ..." no log do servidor.

| # | Passo | Esperado |
|---|---|---|
| 64.1 | Cinco muros de tijolo de 3 de altura; Mina com a vida cheia e uma Camponotus a 30 blocos | Ela vai atrás da formiga e atira de longe, sem cobertura (vida acima de 50%) |
| 64.2 | O mesmo com a Mina em 160 de 420 (`/data merge entity ... {Health:160f}`) | Log "cobertura em ... com ponto de tiro"; ela fica atrás de um muro, sai para atirar, se abaixa enquanto a formiga prepara o ácido e volta a atirar; muro quebrado ou contornado: "cobertura perdida" e outra cobertura |
| 64.3 | O mesmo com um Primigenius no lugar da formiga | Nenhuma cobertura (ele não atira): recua e circula atirando |
| 64.4 | Mina contra o No. 9 algumas vezes | Ela pode vencer (decisão do Miguel); não fica parada escondida enquanto ele regenera |
| 64.5 | Soldados comuns de rifle contra a formiga com os muros (como o §57) | Usam cobertura e saem para atirar entre um ácido e outro (não ficam escondidos o tempo todo) |

## 65. Perfil do Reno (MOV-5E)

Ligar `[kaijuAi] debugLog`.

| # | Passo | Esperado |
|---|---|---|
| 65.1 | Reno No. 6 + Hoshina contra um Primigenius Honju | O Reno congela (explosão congelante: o Honju quase para); o Hoshina mostra "brecha do kaiju: pressiona" e emenda técnicas no Honju congelado |
| 65.2 | Reno No. 6 sozinho contra o No. 9 | Sai da área dos golpes pesados ("saindo da area do golpe pesado") sem parar de atirar; com o No. 9 abaixo da metade (regenerando) não recua para reposicionar; vence boa parte das vezes (meta 70%) |
| 65.3 | Reno (sem traje kaiju) contra o No. 9 | Perde quase sempre (meta 12%) |
| 65.4 | `/effect give @e[type=kn8:primigenius,limit=1] slowness 10 6` com o Hoshina lutando contra ele | Log "brecha do kaiju: pressiona" no Hoshina |

## 66. Perfil do Narumi (MOV-5F)

Ligar `[kaijuAi] debugLog`.

| # | Passo | Esperado |
|---|---|---|
| 66.1 | Narumi No. 1 contra o No. 9 | Quando o No. 9 prepara um golpe, o Narumi ataca antes do impacto (log "previsao: <tecnica> antes do impacto do kaiju"), quase sempre o contra-golpe previsto ou as estocadas |
| 66.2 | O mesmo, com o golpe do No. 9 já perto do impacto | O Narumi não começa técnica lenta (log "espera: o golpe do kaiju chega antes") e esquiva/contra-ataca |
| 66.3 | Narumi (sem o traje kaiju) contra o No. 9 | Luta como antes, sem a previsão (nenhum log "previsao") |
| 66.4 | Narumi No. 1 contra o Primigenius Honju | Derruba o Honju com pouco dano levado |

## 67. Perfil do Isao (MOV-5G)

Ligar `[kaijuAi] debugLog`.

| # | Passo | Esperado |
|---|---|---|
| 67.1 | Ovo "Isao Shinomiya" (sem o Numbers 2) no menu criativo, ou `/summon kn8:isao_base` | Isao com 520 de vida e aura azul padrão; luta só com socos, cotoveladas e o direto pesado (nenhuma onda de choque) |
| 67.2 | Isao com o Numbers 2 contra o Primigenius Honju | Depois do Thunder Emperor (núcleo exposto) o log mostra "brecha do kaiju: pressiona" e ele emenda o Barrage Blast |
| 67.3 | Hoshina contra um Primigenius com o núcleo exposto pelo gelo do Reno No. 6 | "brecha do kaiju: pressiona" no Hoshina |
| 67.4 | No. 9 contra o Isao sem o Numbers 2 até ele cair abaixo de 30% | O No. 9 não o absorve (só o Isao com o Numbers 2 vira o No. 9 com o Isao) |

## 68. Decisões de 2026-10-22 (fuga = empate, Isao, Narumi No. 1)

| # | Passo | Esperado |
|---|---|---|
| 68.1 | `/summon kn8:isao` e `/summon kn8:kaiju_no10_giant` a ~18 blocos; olhar a barra do gigante | Depois do Thunder Emperor a barra mostra "EXPOSTO"; abaixo de 50% de vida o gigante só solta partículas verdes (regeneração) quando o núcleo **não** está exposto |
| 68.2 | Repetir 68.1 algumas vezes | O Isao vence a maioria (medido 7/10), às vezes com pouca vida |
| 68.3 | `[kaijuAi] exposedCoreStopsRegeneration = false` e repetir | O gigante volta a se curar com o núcleo exposto e o Isao quase sempre perde |
| 68.4 | `/summon kn8:narumi_no1` e `/summon kn8:narumi`, cada um contra um No. 13 (`/summon kn8:kaiju_no13`) | O Narumi No. 1 vence mais rápido (~25 s) e com mais vida que o Narumi (~40 s) |
| 68.5 | Qualquer especial contra o No. 9 até ele fugir a 15% | A fuga é empate (o No. 9 continua vivo); só conta vitória se ele morrer antes |

## 69. Kaiju: golpe com caminho livre, salto, voo e falhas (MOV-6)

Ligar `[kaijuAi] debugLog`.

| # | Passo | Esperado |
|---|---|---|
| 69.1 | Muro de bedrock de 8 de altura; dois soldados parados (`NoAI`) colados do outro lado; `/summon kn8:primigenius_honju` a ~12 blocos | O Honju encosta no muro e ataca, mas nenhum soldado perde vida |
| 69.2 | Repetir 69.1 com o muro de tijolo de pedra | Os golpes quebram o muro e, depois de aberto, ferem os soldados |
| 69.3 | Repetir 69.1 com `[kaijuAi] strikeNeedsClearPath = false` (reiniciar o servidor) | O pisão/varrida fere os soldados através do muro (como era antes) |
| 69.4 | Seis torres de bedrock de 24 blocos; dois soldados de rifle entre elas; `/summon kn8:preondactyl` no alto | O Preondactyl passa por cima e pelos lados das torres sem bater; o log mostra "voo desviando de obstaculo" e, quando o soldado está num vão estreito demais para o corpo, "mergulho sem caminho: volta a circular" (e ele usa o raio) |
| 69.5 | Formiga (`/summon kn8:camponotus`) sob um teto de pedra baixo, soldado a ~10 blocos | Ela não salta (o arco bateria no teto); morde, varre ou cospe ácido |
| 69.6 | Soldado no alto de um pilar de bedrock, fora do alcance da mordida, e uma aranha embaixo | Depois de 3 golpes no vazio o log mostra "habilidade ... falhou 3 vezes seguidas: descansa" e ela troca de ataque |

## 70. Integração geral (MOV-7)

| # | Passo | Esperado |
|---|---|---|
| 70.1 | Dois jogadores; `/kn8 invasion start kn8:honju_assault ~ ~ ~` e `/kn8 invasion next` | Alerta para os dois, defensores em esquadrões, as duas ondas e a vitória; o jogo segue liso (20 TPS) |
| 70.2 | Durante a invasão, construir um muro de pedra entre um soldado e um kaiju | O soldado usa o muro como cobertura (atirador) ou contorna/pula; o kaiju quebra o muro antes de ferir |
| 70.3 | `/neoforge tps` com 40 kaiju e 40 soldados lutando | Abaixo de ~15 ms por tick (medido 11–14 nesta máquina) |

## 71. Retirada do No. 9

| # | Passo | Esperado |
|---|---|---|
| 71.1 | `/summon kn8:kaiju_no9_black` e `/summon kn8:kaiju_no8` perto; baixar a vida do No. 9 (`/data merge entity @e[type=kn8:kaiju_no9_black,limit=1] {Health:300f}`) | Aviso "O Kaiju No. 9 está recuando! Não deixe ele escapar!"; ele para de atacar e corre para longe soltando fumaça |
| 71.2 | Ficar longe dele (mais de 24 blocos) ou sem vê-lo por 3 s | "O Kaiju No. 9 recuou... ele vai voltar." e ele some (quem lutou ganha o mérito da fuga) |
| 71.3 | Persegui-lo e matá-lo durante a retirada | Ele morre de verdade: fica a carcaça |
| 71.4 | Cercá-lo (muro de bedrock em volta, alguém sempre a vista) por 15 s | "O Kaiju No. 9 foi encurralado e vai lutar até o fim!" e ele volta a atacar até morrer (não tenta fugir de novo) |

## 72. Base dos efeitos (VFX-1)

Servidor dedicado + 2 clientes. Os efeitos agora usam a paleta e o material de quem os faz (`vfx_profile/<id>.json`).

| # | Passo | Esperado |
|---|---|---|
| 72.1 | Em pé num lugar aberto: `/execute positioned ~3 ~1 ~ run kn8 vfx play kn8:impact kn8:trichonephila 1.5` e o mesmo com `kn8:primigenius`, `kn8:phaneroplasmodium`, `kn8:primigenius_resurrected`, `kn8:kaiju_larva`, `kn8:reno_no6` | Cada impacto com o seu material: lascas pretas e amarelas (aranha), poeira marrom (Primigenius), esporos bege/vermelhos subindo (cogumelo), vapor verde (ressurgido), brilho ciano (larva), cristais brancos e névoa azul (Reno No. 6); o outro jogador vê o mesmo |
| 72.2 | `kn8 vfx play kn8:shockwave <perfil> 1.2` com `primigenius_honju`, `kaiju_no8`, `hoshina_no10` | Anel deitado no chão crescendo na cor de destaque (dourado, ciano, violeta) |
| 72.3 | `kn8 vfx play kn8:energy_burst kn8:mina 1.2` | Anel e brilho verdes (cor da Mina) |
| 72.4 | `/summon kn8:trichonephila ~ ~ ~5 {NoAI:1b}` e `/kn8 anim kaiju action action.bite` olhando a cabeça | Pequeno brilho amarelo perto da frente da aranha no início da mordida (marca da animação; a origem exata no osso vem na VFX-2) |
| 72.5 | Config do cliente: `[fx] particleLevel` LOW, depois `maxParticlesPerEffect` 8, depois `effectDistance` 16 e afastar-se 20 blocos | Menos partículas; quase nenhuma; nada desenhado longe (o tremor de câmera continua) |
| 72.6 | `[fx] reduceFlashes` true e repetir 72.3 | Brilhos menores e sem branco puro |
| 72.7 | Servidor: `[vfx] enabled` false e deixar um kaiju morder um soldado | Sem efeito nenhum; o dano é o mesmo |

## 73. Kafka e Kaiju No. 8; efeito pelo tamanho do corpo (VFX-2)

Servidor dedicado + 2 clientes.

| # | Passo | Esperado |
|---|---|---|
| 73.1 | Em lugar aberto, a 10 blocos um do outro: `/execute positioned ~ ~1 ~ run kn8 vfx play kn8:shockwave kn8:soldier 1`, depois com `kn8:primigenius`, `kn8:primigenius_honju` e `kn8:kaiju_no10_giant` | O anel cresce com o tamanho de cada um (o do No. 10 gigante ocupa vários blocos); dá para ver de longe |
| 73.2 | `/summon kn8:primigenius_honju ~10 ~ ~`, `/summon kn8:kafka`, e `/data merge entity @e[type=kn8:kafka,limit=1] {Health:8f}` | O Kafka vira o No. 8: linhas ciano saindo do peito, coluna escura com faíscas ciano, anel ciano no chão e um pulso |
| 73.3 | Olhar o No. 8 lutando | Socos com pulso de ar branco-ciano e lascas escuras (sem arco de lâmina); rastro ciano curto do punho; golpe no chão com fenda ciano em anel, pedras e poeira; rugido com anéis saindo da boca |
| 73.4 | Deixar o No. 8 com menos de metade da vida | A cada segundo, pontos ciano entrando no corpo (regeneração) |
| 73.5 | Matar o kaiju e esperar 15 s sem alvo | Volta ao Kafka: linhas ciano apagando e vapor |
| 73.6 | Kafka atirando | Clarão curto amarelo-alaranjado e fumaça cinza no cano |
| 73.7 | Config do cliente `[fx] scaleSizeExponent` 0 e repetir 73.1 | Todos os anéis do mesmo tamanho (a regra de tamanho desligada) |

## 74. Hoshina e Hoshina No. 10; aura do Kafka (VFX-3)

Servidor dedicado + 2 clientes.

| # | Passo | Esperado |
|---|---|---|
| 74.1 | `/execute positioned ~ ~1.2 ~3 run kn8 vfx play kn8:slash kn8:hoshina yae_uchi/1` até `yae_uchi/8` em sequência | Oito arcos finos, pretos com borda violeta, em ângulos diferentes formando uma estrela |
| 74.2 | `/execute positioned ~ ~ ~3 run kn8 vfx play kn8:technique kn8:hoshina 1 kosa_uchi` | Um X à frente |
| 74.3 | `/execute positioned ~ ~1.2 ~3 run kn8 vfx play kn8:slash kn8:hoshina_no10 1 juni_hitoe/12` | Cruz grande com pulso violeta e raios vermelho-arroxeados |
| 74.4 | `/summon kn8:hoshina_no10` e um `kn8:primigenius` a 6 blocos | Os cortes aparecem na frente do kaiju (não dentro dele); cada técnica com o seu desenho; a cauda corta com arco vermelho e faíscas |
| 74.5 | Olhar o Hoshina com o Numbers 10 parado | Aura violeta com raios vermelho-arroxeados saindo do traje |
| 74.6 | `/data merge entity @e[type=kn8:hoshina_no10,limit=1] {...}` ou deixar a luta chegar ao Full Release (100%) | Vapor vermelho fino e descargas saindo do traje |
| 74.7 | `/summon kn8:kafka` | Aura cinza bem baixa (quase nada parado) |

## 75. Auras com raios (VFX-4A)

Servidor dedicado + 2 clientes, de noite para ver melhor.

| # | Passo | Esperado |
|---|---|---|
| 75.1 | Invocar os 12 especiais (`/summon kn8:hoshina`, `kn8:kikoru`, `kn8:mina`, ...) e `/kn8 aura release @e[type=kn8:<id>] 100` | Cada um com raios em zigue-zague saindo do corpo e correndo pelo traje, na cor dele: Hoshina violeta fino, No. 10 vermelho-arroxeado, Kikoru dourado com galhos, No. 4 ciano, Mina verde, Reno branco-azulado, No. 6 azul, Narumi rosa, Isao laranja, No. 8 azul |
| 75.2 | `/kn8 aura release @e[type=kn8:hoshina] 45` e depois 100 | Poucos raios a 45%, muitos a 100% |
| 75.3 | Vestir o Mk1, `/kn8 release set 80` e `/kn8 release active 80`; o outro jogador olha | Raios ciano em volta do jogador |
| 75.4 | `/summon kn8:kafka` | Quase nada (aura cinza bem baixa); com a vida baixa, faíscas cinza pequenas |

## 76. Efeitos desenhados, sem partículas (VFX-R1)

Servidor dedicado + 2 clientes, de noite. Comando: `/execute positioned ~ ~1.2 ~4 rotated 90 0 run kn8 vfx play
kn8:impact kn8:demo 1 <forma>` (para `wave` e `smoke`, `~ ~ ~4`).

| # | Passo | Esperado |
|---|---|---|
| 76.1 | `bolt` | Raio azul em zigue-zague com galhos, piscando, ~0,5 s |
| 76.2 | `band` e `band_x` | Arco de corte fino (miolo escuro, borda violeta) que se abre e some; `band_x` = dois arcos em X |
| 76.3 | `wave` | Anel dourado no chão que cresce e some |
| 76.4 | `pulse` | Anel branco virado para quem olha, crescendo |
| 76.5 | `beam` e `ray` | `beam`: risco cinza rápido (tiro); `ray`: feixe laranja largo que dura um pouco |
| 76.6 | `flash` | Clarão amarelo redondo, curto |
| 76.7 | `smoke` | Fumaça cinza desenhada subindo (discreta sobre chão escuro) |
| 76.8 | Os dois jogadores olhando de lados diferentes | As mesmas formas para os dois; atrás de uma parede, somem |
| 76.9 | Config do cliente `[fx] maxDrawnEffects` 0 | Nenhuma forma aparece |

## 77. Tudo desenhado, sem partículas (VFX-R2)

Servidor dedicado + 2 clientes, de noite.

| # | Passo | Esperado |
|---|---|---|
| 77.1 | `/execute positioned ~ ~1.2 ~4 run kn8 vfx play kn8:explosion kn8:soldier 3` | Clarão laranja, anel de ar, onda laranja no chão, linhas de fogo e fumaça escura desenhada subindo; nenhum ponto/floco solto |
| 77.2 | Repetir com `kn8:impact`, `kn8:shockwave`, `kn8:parry`, `kn8:roar`, `kn8:transform`, `kn8:regenerate` | Anel e linhas de impacto; onda no chão; clarão dourado com faíscas; três anéis saindo para a frente; clarão, coluna de fumaça e onda; labaredas verdes entrando |
| 77.3 | `/execute positioned ~ ~1.2 ~4 run kn8 vfx play kn8:slash kn8:hoshina 1 yae_uchi/3` e `kn8:technique kn8:hoshina 1 kosa_uchi` | Corte inclinado preto com o fio violeta; um X |
| 77.4 | `/summon kn8:primigenius_honju`, três `kn8:soldier` de rifle, `kn8:mina` e `kn8:hoshina` a ~12 blocos | Antes do pisão, a área no chão amarela que vai a laranja/vermelho; tiros como riscos amarelos; cortes do Hoshina voando em meia-lua violeta; explosão do canhão da Mina desenhada |
| 77.5 | `/summon kn8:hoshina` e `/kn8 aura release @e[type=kn8:hoshina] 100` | Raios contínuos violeta saindo do corpo, labaredas subindo, anel no chão; nenhum pó colorido |
| 77.6 | Kaiju andando, golpe no chão do machado, desmonte de carcaça | Continuam os pedacinhos de bloco/item do vanilla (meio-termo) |

## 78. Kikoru e Kikoru No. 4 (VFX-4B)

Servidor dedicado + 2 clientes, de noite. Prévia: `/execute positioned ~ ~1.2 ~4 run kn8 vfx play kn8:slash kn8:kikoru 1 <golpe>` (para o chão: `~ ~ ~4` e `kn8:technique`).

| # | Passo | Esperado |
|---|---|---|
| 78.1 | `axe_slash/1` e `heavy_swing/1` | Arco largo e grosso dourado com fio claro (bem mais largo que o corte do Hoshina), raios dourados, lascas de pedra caindo, poeira |
| 78.2 | `kn8:technique kn8:kikoru 1 heavy_swing_1_ground` e `ground_smash` | Rachaduras escuras com fio dourado, anel dourado no chão, coluna de poeira; no esmagamento, cratera maior e raios subindo |
| 78.3 | `kn8:technique kn8:kikoru 1 shockwave` | Onda dourada e pedras saltando numa linha à frente |
| 78.4 | `kn8:slash kn8:kikoru_no4 1 dive_strike/1` e `kn8:technique kn8:kikoru_no4 1 dive_strike_1_ground` | Arco dourado vertical com anel de ar ciano; no chão, anel ciano com borda dourada |
| 78.5 | `/summon kn8:kikoru` e `kn8:kikoru_no4` perto de um `kn8:primigenius_honju` | Na luta, os golpes da Kikoru com arco dourado e, nos pesados, as rachaduras no chão; a No. 4 voando com traços ciano saindo das costas a cada batida das asas |

## 79. Mina, Reno e Reno No. 6 (VFX-5)

Servidor dedicado + 2 clientes, de noite. Prévia: `/execute positioned ~ ~ ~6 rotated 0 0 run kn8 vfx play kn8:technique <perfil> 1 <receita>` (para impacto/explosão: `~ ~1 ~6`).

| # | Passo | Esperado |
|---|---|---|
| 79.1 | `kn8:mina` `cannon_shot` e `anti_giant_shot` | Clarão branco com anel verde na boca do canhão (altura do quadril), faíscas verdes, fumaça cinza grossa; no Anti-Giant, poeira e anel de poeira nos pés (recuo) |
| 79.2 | `kn8:mina` `charged_cannon_charge` e `anti_giant_shot_charge` | Raios verdes convergindo para a boca do canhão, passando a brancos no fim, com o brilho crescendo; no Anti-Giant, anel verde no chão durante a carga inteira |
| 79.3 | `kn8:mina` `cannon_shot_explosion` e `anti_giant_shot_explosion` | Clarão, anel de ar e onda verdes, raios verdes, faíscas e fumaça escura subindo; a do Anti-Giant bem maior |
| 79.4 | `kn8:reno` `freeze_round_impact` e `kn8:reno_no6` `ice_spear_impact` | Anel de geada, lascas de gelo azul caindo, faíscas brancas e névoa clara |
| 79.5 | `kn8:reno_no6` `freeze_burst_explosion` e `ice_field` | Explosão de estilhaços azuis com névoa e duas ondas (azul e azul-marinho); campo: onda azul de 6 blocos no chão, rachaduras de gelo com fio branco, cristais subindo e névoa baixa |
| 79.6 | `/summon kn8:mina` a ~20 blocos de um `kn8:primigenius_honju`; depois `kn8:reno` e `kn8:reno_no6` | Tiros da Mina verdes com fumaça cinza atrás e explosão verde; tiros do Reno branco-azulados, lascas de gelo no kaiju e a barra com "EXPOSTO" depois da munição congelante |

## 80. Narumi, Narumi No. 1, Isao e Isao sem o Numbers 2 (VFX-6)

Servidor dedicado + 2 clientes, de noite, câmera de lado. Prévia: `/execute positioned ~ ~1.2 ~4 rotated 0 0 run kn8 vfx play kn8:slash <perfil> 1 <golpe>` (chão e carga: `~ ~ ~4` e `kn8:technique`).

| # | Passo | Esperado |
|---|---|---|
| 80.1 | `kn8:narumi` `thrust_combo/3` e `lunge/1` | Estocada = agulha reta rosa com miolo branco terminando no ponto do golpe (nada de arco), brilho na ponta; a investida mais longa com riscos atrás |
| 80.2 | `kn8:narumi` `sweeping_slash/1` | O único arco do Narumi: faixa rosa com fio branco |
| 80.3 | `kn8:narumi_no1` `predicted_counter/1`, `weak_point_thrust/1` e (`kn8:technique`, `~ ~1.6 ~4`) `foresight` | Rastro duplo (imagem prevista mais apagada, depois o golpe real); marca no ponto fraco com anéis; brilho pequeno nos olhos com anel rosa |
| 80.4 | `/summon kn8:narumi_no1` perto de um `kn8:primigenius_honju`, com `[kaijuAi] debugLog` ligado | A cada "previsao: … antes do impacto" no log, anel nos olhos do Narumi e uma linha fina rosa no chão do kaiju até onde o golpe vai bater, até o impacto |
| 80.5 | `kn8:isao` `barrage_blast/5`, `force_blast`, `spiral_blast_impact` | Anéis de ar laranja saindo do golpe com poeira; leque de ondas laranja e bege no chão; impacto em anel |
| 80.6 | `kn8:isao` `thunder_emperor_charge`, `thunder_emperor/2` e `thunder_emperor_2_ground` | Carga laranja crescendo nos punhos; soco único com clarão enorme e três anéis (sem raio, decisão do Miguel); no chão onda dupla, rachaduras com fio laranja e coluna de poeira |
| 80.7 | `kn8:isao_base` `heavy_straight/1` | Só impacto físico: anel de ar claro, lascas e poeira; nenhuma cor de energia |

## 81. Kaiju No. 9 e formas (VFX-7)

Servidor dedicado + 2 clientes, de noite. Prévia: `/execute positioned ~ ~1.2 ~6 rotated 0 0 run kn8 vfx play kn8:technique <forma> 1 <receita>` (chão: `~ ~ ~6`). Na prévia o tamanho é o de referência; na luta cresce com o corpo.

| # | Passo | Esperado |
|---|---|---|
| 81.1 | `kn8:kaiju_no9` `finger_gun` e `finger_gun_muzzle`; luta do No. 9 contra soldados | Finger Gun cinza (decisão do Miguel): risco cinza do tiro, estalo branco no impacto e clarão pequeno na saída; o Multi-Finger Gun da forma preta também cinza |
| 81.2 | `kn8:kaiju_no9` `harden`, `adapt`, `escape` | Pele endurecida: anel e faíscas na paleta da forma; adaptação: anel e labaredas; fuga: fumaça escura subindo com onda e clarão |
| 81.3 | `kn8:kaiju_no9_black` `no9_black_blade_arm` e `no9_black_spike_burst` | Braço-lâmina: arco escuro com fio vermelho; espinhos: rachaduras escuras com fio vermelho, espinhos vermelhos subindo e onda |
| 81.4 | `kn8:kaiju_no9_fusion` `no9_fusion_hybrid_beam_muzzle` e `no9_fusion_hybrid_beam` | Carga com raios vermelhos convergindo e clarão; impacto com ondas vermelha e bege (a faixa do No. 9), raios e fumaça |
| 81.5 | `kn8:kaiju_no9_camponotus` `no9_ant_acid_spray` e `no9_ant_mandible_rush` | Ácido vermelho-alaranjado em nuvem com anel; mandíbulas em X âmbar |
| 81.6 | `kn8:kaiju_no9_isao` `no9_isao_thunder_emperor` e `no9_isao_force_blast` | As ondas do Isao em preto e vermelho; o Thunder Emperor dele sem raio, com clarão, três anéis e rachaduras no chão |
| 81.7 | `/summon kn8:kaiju_no9_fusion` perto de `kn8:isao` e dois `kn8:soldier` | Na luta, tudo no tamanho do corpo de 5 m: raios vermelhos da carga, explosões vermelhas, ondas no chão; tentáculos da absorção na cor de destaque da forma |

## 82. No. 10, No. 13, Preondactyl e larva (VFX-8)

Servidor dedicado + 2 clientes, de noite. Prévia: `/execute positioned ~ ~1 ~6 rotated 0 0 run kn8 vfx play kn8:technique <espécie> 1 <receita>` (chão: `~ ~ ~6`). Para a forma gigante e a autodestruição: `/datapack disable "file/kn8_duelo_isolado"` se estiver ligado.

| # | Passo | Esperado |
|---|---|---|
| 82.1 | `kn8:kaiju_no10_small` `energy_charge` e `no10_finger_cannon` | Carga com raios vermelhos convergindo e brilho; impacto vermelho com onda, raios, faíscas e fumaça escura |
| 82.2 | `kn8:kaiju_no10_giant` `grow`; luta do No. 10 pequeno com a vida abaixo de 50% | Ao virar gigante: labaredas, vapor e raios vermelhos subindo, onda no chão, além da explosão de sempre; tudo no tamanho do corpo de 24 m |
| 82.3 | `kn8:preondactyl` `preondactyl_energy_beam`, `self_destruct`; luta | Raio laranja/vermelho (decisão do Miguel) com impacto laranja; autodestruição com clarão enorme, anéis, raios e fumaça escura; carga da autodestruição com pulsos laranja e raios entrando |
| 82.4 | `kn8:kaiju_no13` `no13_throat_grab`, `no13_pounce`; luta | Agarrão com estalo branco e anel vermelho; salto com onda e rachaduras de fio vermelho; na investida, riscos curtos atrás do corpo |
| 82.5 | `kn8:kaiju_larva` `preondactyl_dive_strike` | Anel e pulso ciano pequenos (o corpo de 0,5 m deixa o efeito pequeno) |

## 83. Decisões de 2026-10-23: aura sorteada do jogador e pedaços desenhados

Servidor dedicado + 2 clientes.

| # | Passo | Esperado |
|---|---|---|
| 83.1 | Os dois jogadores com `kn8:mk1` no peito e `/kn8 release cap 100`, `/kn8 release set 80`, `/kn8 release active 80`; um olhando para o outro | Cada jogador com uma aura sorteada (cores diferentes na maioria das vezes), com raios na cor dela; `/data get entity <jogador> neoforge:attachments."kn8:player_aura"` mostra a sorteada |
| 83.2 | Morrer e renascer; sair e entrar de novo | A aura sorteada continua a mesma |
| 83.3 | `/execute positioned ~ ~1 ~4 run kn8 vfx play kn8:debris kn8:soldier 6 #8C8C8C` | Lascas cinza desenhadas voando e caindo, com um pouco de poeira; nenhuma partícula de bloco do Minecraft |
| 83.4 | Muro de tijolos de pedra entre um `kn8:primigenius_honju` e um `kn8:soldier`; Honju andando | Blocos quebram sem as partículas de bloco do Minecraft (som de quebra continua); pedaços desenhados na cor do bloco; a cada passo do Honju, poeira desenhada na cor do chão |
| 83.5 | Desmontar uma carcaça (segurar o botão direito) | A cada etapa, pedaços desenhados na cor do corpo do kaiju, sem partícula de item |

## 84. Outros kaiju (VFX-9)

Servidor dedicado + 2 clientes. Prévia: `/execute positioned ~ ~ ~6 run kn8 vfx play kn8:technique kn8:<espécie> 1 <receita>`.

| # | Passo | Esperado |
|---|---|---|
| 84.1 | Prévia `kn8:primigenius_honju` `slam` e `kn8:primigenius_revived` `slam` | Onda no chão, rachaduras e lascas na cor do corpo; no revivido, vapor subindo junto |
| 84.2 | `kn8:primigenius_honju` `energy_charge` e `energy_blast`; `kn8:primigenius_revived` `energy_blast_revived` | Carga com raios na cor do Honju e explosão com anel e fumaça; o revivido na paleta roxa |
| 84.3 | `kn8:philinosoma` `philinosoma_spine_shot` | Espinhos azuis em leque (traços), estalo no impacto |
| 84.4 | `kn8:diclonius` `energy_charge` e `diclonius_atomic_breath` | As placas das costas acendem em sequência com raios rosa/vermelhos; sopro vermelho (`#FF3B55`) com explosão e ondas vermelha e azul |
| 84.5 | `kn8:camponotus` e `kn8:camponotus_reborn` `<espécie>_acid_spray` | Ácido verde na formiga e ciano na revivida (nuvem e anel no chão) |
| 84.6 | `kn8:trichonephila_honju` `web_burst`; `kn8:trichonephila` `multi_leg` | Explosão de teia com anel branco e fios; golpes das patas com traços amarelos |
| 84.7 | `kn8:phaneroplasmodium` `phaneroplasmodium_spore_shot`; `kn8:myxogasterocarp` `myxogasterocarp_spore_bomb` | Nuvem de esporos clara; bomba com clarão, nuvens vermelhas e fumaça |
| 84.8 | Luta: `kn8:diclonius` contra Mina e dois soldados | Placas acendendo na carga, sopro vermelho em linha até o alvo, nos dois clientes |
| 84.9 | Luta: `kn8:primigenius_honju` contra Hoshina e soldados; `kn8:camponotus` contra Kikoru | Pisão e investida com efeitos do tamanho do corpo; ácido verde quando a formiga atira |

## 85. Regressão dos efeitos (VFX-10)

Servidor dedicado + 2 clientes. Nos dois clientes, em `config/kn8-client.toml`, `[fx] perfLog = true` (o log do cliente
mostra a cada 5 s `[kn8 perf] fps=... drawn=... drawnPeak=... drawMs=... memMb=...`).

| # | Passo | Esperado |
|---|---|---|
| 85.1 | Batalha grande: os 13 especiais, 6 `kn8:soldier` e 6 kaiju grandes (Primigenius Honju, Diclonius, No. 10 pequeno, Myxogasterocarp, Tecedeira, Philinosoma) lutando a ~20 blocos dos dois jogadores | Auras, cortes, tiros, ondas e avisos de todos nos dois clientes; nenhum erro no log; `memMb` sobe e desce (sem crescer sempre) |
| 85.2 | Repetir com `maxDrawnEffects = 0` | Nenhum efeito desenhado; a luta e o dano continuam iguais (o servidor não muda nada) |
| 85.3 | `particleLevel` `LOW`, `MEDIUM`, `HIGH` com muitos efeitos ao mesmo tempo (ex.: vários `/kn8 vfx play kn8:technique kn8:kikoru 1 ground_smash` seguidos) | `drawnPeak` no máximo 35%, 70% e 100% de `maxDrawnEffects`; FPS maior no LOW |
| 85.4 | `reduceFlashes = true` e uma explosão da Mina | Clarões menores e sem branco puro |

## 86. Combate: esquiva perfeita e funções das armas (PM-1)

Servidor dedicado + 2 clientes. Jogador de Mk1 com Resistência; o outro jogador assistindo de lado.

| # | Passo | Esperado |
|---|---|---|
| 86.1 | Espada na mão, dois `minecraft:husk` com `NoAI` a ~3 blocos (um na frente, outro 4 blocos ao lado) e um terceiro atrás de um muro de pedra; tecla **R** | "Corte em Avanço" na HUD com recarga; o jogador avança; só o da frente perde vida (o do lado e o de trás do muro não) |
| 86.2 | Faca na mão, golem de ferro com `NoAI` a 2,5 blocos; **R** | "Sequência de Cortes": 4 cortes seguidos (golem ~100 → ~78 com o Release a 0), dois traços cruzados a cada corte |
| 86.3 | Rifle com balas, golem a ~6 blocos; clique esquerdo, depois clique direito | Tiro comum (−5) e tiro preciso mais lento (−11), cada um gastando uma bala; a pistola faz o mesmo com o tiro preciso dela |
| 86.4 | Espada na mão, `kn8:primigenius` atacando; apertar **Z** (esquiva) bem no momento do golpe | Às vezes "ESQUIVA PERFEITA!" em dourado na HUD, som agudo de parry e um anel ciano; o golpe seguinte sai crítico; o Primigenius fica atordoado; esquivar cedo ou tarde demais só esquiva |
| 86.5 | Apertar **Z** várias vezes seguidas | A stamina cai (cada esquiva gasta 20, a perfeita devolve 12); sem stamina, "Sem stamina" e nada de esquiva |

## 87. Ferimentos por parte dos kaiju (PM-2)

Servidor dedicado + 2 clientes (um em pt_br, outro em en_us). Jogador de Mk1 com Resistência e espada; o outro
assistindo de lado. Ligar `[kaijuAi] debugLog` para ver no log do servidor quando o ferimento abre e sara.

| # | Passo | Esperado |
|---|---|---|
| 87.1 | `kn8:primigenius` com `NoAI` de frente; 3-4 golpes de espada nas pernas | Na barra do kaiju, "FERIDO: Pernas" / "WOUNDED: Legs" em laranja nos dois clientes; lascas desenhadas na cor do corpo saindo da perna e um gemido grave; no log `ferimento: legs` |
| 87.2 | `/attribute @e[type=kn8:primigenius,limit=1] minecraft:generic.movement_speed get` logo depois | Velocidade ~70% da base (0,23 → ~0,16); golpear mais a perna ferida não baixa mais |
| 87.3 | Esperar 20 s | "FERIDO" some da barra, a velocidade volta e o log diz `ferimento sarou: legs`; com a perna ferida o kaiju não usa a investida |
| 87.4 | Golpes fortes na cabeça (golpe pesado ou carregado) | "FERIDO: Cabeça", o kaiju fica parado 1,5 s; enquanto a cabeça está ferida, aparar/esquiva perfeita o atordoam 50% mais tempo |
| 87.5 | Golpes no peito (núcleo) | Nenhum ferimento: o núcleo tem o sistema dele (barra laranja, EXPOSTO) |
| 87.6 | `kn8:trichonephila` com golpes no abdômen | "FERIDO: Abdômen" e ela para de atirar teia enquanto dura; Preondactyl e larva (sem partes) nunca ficam feridos |
| 87.7 | Ferir a perna, sair do mundo e voltar em menos de 20 s | O kaiju volta ainda ferido (lento, "FERIDO" na barra) e sara no tempo que faltava |

## 88. Kaiju inteligentes leem hábitos (PM-3)

Servidor dedicado + 2 clientes, `[kaijuAi] debugLog` ligado (o log do servidor diz "habito do alvo: ..."). Só o
No. 9, as formas dele e o No. 10 (inteligência 4+) leem hábitos; o resto luta como antes.

| # | Passo | Esperado |
|---|---|---|
| 88.1 | `kn8:kaiju_no10_small` contra `kn8:mina` (luta de longe) por ~45 s | No log, `keeps_distance` em ~8 s: rosnado grave e o No. 10 persegue mais rápido, preferindo investida/salto/tiro; depois `repeats` (os tiros iguais) e `evades` (golpes dele que não pegam) |
| 88.2 | Contar o tempo entre duas leituras no log | Pelo menos ~13 s (5 s de resposta + 8 s de espera): ele não fica trocando de jeito a todo tick |
| 88.3 | Jogador de Mk1 com espada colado no No. 10, só golpes leves seguidos | `repeats` e depois `stays_close`: o No. 10 abre 4-5 blocos e usa golpe de área/tiro; voltar a variar (pesado, recuar) não reabre a mesma leitura até repetir o hábito |
| 88.4 | `kn8:primigenius` contra a Mina | Nenhum "habito do alvo" no log (não é inteligente) |
| 88.5 | Trocar de alvo / ficar 10 s longe do combate | A memória zera: a leitura começa do zero com o alvo novo |
| 88.6 | A Mina sozinha contra o No. 10 pequeno | A luta continua ganhável (no teste ela o deixou com 334 de 1.650 em 45 s) |

## 89. Terreno por espécie e papéis em grupo (PM-4)

Servidor dedicado + 2 clientes, `[kaijuAi] debugLog` ligado. Coordenação entre kaiju é mecânica de jogo (não canon),
exceto o comando do No. 9/No. 10.

| # | Passo | Esperado |
|---|---|---|
| 89.1 | Muro de tijolo de pedra 15 x 6 com passagem numa ponta; `kn8:trichonephila` de um lado e um soldado com Resistência do outro | A aranha contorna/passa sem derrubar o muro (no teste 4-11 de 90 tijolos, dos golpes); só depois de 3 s batendo sem sair do lugar o log diz "contorno travado ... quebra a frente" |
| 89.2 | O mesmo com `kn8:primigenius` | Ele abre o muro (no teste 42 de 90) |
| 89.3 | Honju + 5 Primigenius + 1 formiga contra um Hoshina com Resistência | No máximo 3 Primigenius colados nele; os outros circulam em volta (log "espera a vez") e entram quando abre vaga; a formiga fica a ~7 blocos atirando ácido |
| 89.4 | Matar o Honju (`/kill`) no meio | Log "lider morto: retreat": os Primigenius e a formiga se afastam ~4 s e voltam; o ressurgido (`primigenius_resurrected`) fica mais forte e rápido 10 s (rugido) |
| 89.5 | Honju com alvo e um Primigenius parado sem alvo a até 24 blocos | O Primigenius recebe o alvo do Honju (log "alvo do lider"); a 40 blocos não |
| 89.6 | Honju/revivido/gigantes atravessando um lago | Andam pela água como chão (os Yoju contornam a água) |

## 90. Chefes: mudança concreta por fase (PM-5)

Servidor dedicado + 2 clientes em **criativo ou sobrevivência** (espectador não entra na barra do chefe nem recebe o
aviso: o vanilla não conta espectador), `[kaijuAi] debugLog` ligado.

| # | Passo | Esperado |
|---|---|---|
| 90.1 | `/kn8 boss spawn kn8:honju` e lutar (ou soldados/Kikoru) até a vida passar de 50% | Rugido, aviso vermelho na barra de ação "Fase 2: o Honju fica mais rápido e carrega o raio de longe! Núcleo exposto!" (en_us no outro cliente); log "fase do chefe: 2 (velocidade x1.2, recargas x0.75, distancia 10.0, nucleo exposto 40 ticks)"; Primigenius invocados |
| 90.2 | Logo depois da troca | 2 s invulnerável e então 2 s de núcleo exposto ("EXPOSTO" na barra do kaiju) |
| 90.3 | Continuar a luta na fase 2 | Com o raio pronto o Honju segura ~10 blocos (log "fase do chefe: segura 10.0 blocos com tiro pronto") e atira; com o raio em recarga avança como antes; anda mais rápido e ataca com mais frequência |
| 90.4 | `/kn8 boss spawn kn8:revived_honju`, levar a 60% e depois a 30% | Duas trocas com avisos próprios; na última ("enlouquece") fica ainda mais rápido e não segura distância |
| 90.5 | Salvar e sair com o chefe na fase 2, voltar | Continua na fase 2 com a velocidade da fase, sem novo aviso, sem nova invocação, sem nova janela do núcleo |
| 90.6 | Matar o chefe (também depois de `/data merge` nele) | Uma única barra na tela, que some na morte; mérito e loot só na derrota (nada na troca de fase) |

## 91. Esquadrões: focar, guarda e funções (PM-6)

Servidor dedicado + 2 clientes, `[kaijuAi] debugLog` ligado, jogador com patente que comanda esquadrão (ou criativo).

| # | Passo | Esperado |
|---|---|---|
| 91.1 | Menu (M) → Esquadrão com soldados de rifle, Hoshina e Kikoru por perto | Cada linha mostra "Nível: ... · Assalto" ou "· Atirador" (rifle = atirador, mesmo com a faca de apoio na mão); 6 ordens: Seguir, Ficar, Atacar, Recuar, Focar, Guarda |
| 91.2 | Recrutar 4 e dar **Guarda** | Aviso "4 soldados protegendo a área (raio 16)"; sem kaiju, atiradores a ~4 blocos de onde você estava e assalto a ~8, cada um num ângulo |
| 91.3 | Um Primigenius a ~12 blocos do centro e outro parado a ~30 | Todos lutam com o de dentro (log "melhor pontuacao entre 1": o de fora nem conta); depois voltam ao anel |
| 91.4 | Com um kaiju perto do esquadrão, mirar outro a ~24 blocos e dar **Focar** | Log "designado pelo lider"; todos largam o de perto e vão no mirado, mesmo fora da coleira |
| 91.5 | Derrubar o designado | Aviso verde "Alvo designado abatido: o esquadrão volta a seguir"; log "alvo designado caiu"; voltam a lutar com o de perto |
| 91.6 | **Focar** mirando o céu | Aviso vermelho "Mire em um kaiju para designar o alvo"; a ordem não muda |
| 91.7 | Salvar e sair com o esquadrão focado, voltar | Continua focado no mesmo kaiju |

## 92. Provas de promoção e avaliação pós-missão (PM-7)

Servidor dedicado + 2 clientes. `/kn8 career rank <patente>` (ou o mérito) para chegar à patente de cada prova.

| # | Passo | Esperado |
|---|---|---|
| 92.1 | Menu (M) → Missões como Oficial | A "Prova de Tiro" mostra a linha "Prova: precisão 35% (15 tiros) dano ≤ 100% nota C+"; Perfil mostra "Precisão" com "-" |
| 92.2 | Aceitar a Prova de Tiro | No chat: "Requisitos da prova:" com precisão (mínimo de tiros), dano recebido, tempo ideal (só na nota) e nota mínima |
| 92.3 | Abater os 3 Primigenius errando muito (atirar no ar) | Título "Prova reprovada", resumo com nota, tempo, dano causado/recebido, aliados perdidos e "precisão X% (acertos/tiros)", linha ✘ da precisão, "Sem recompensa. Você pode tentar de novo em 10 min.", uma dica; a missão volta como "em espera" |
| 92.4 | Depois da espera, de novo com boa pontaria | "PROVA APROVADA", recompensa uma vez só, "Nota X" ao lado do nome no menu; com mérito ≥ 500 a promoção a Oficial Sênior sai na hora; a missão não pode ser repetida |
| 92.5 | Concluir uma missão comum (Extermínio) | Mesmo resumo (sem linhas de prova); em S/A "Mérito extra pela nota: +N" |
| 92.6 | Oficial Sênior sem soldados aceitando a "Prova de Esquadrão" | Recusada: "A prova pede um esquadrão maior (aba Esquadrão)"; com 2 recrutados aceita |
| 92.7 | Na Prova de Esquadrão deixar um soldado morrer | Reprovada pela linha "No máximo 0 soldado(s) do esquadrão perdido(s)" |
| 92.8 | O outro cliente fazendo a mesma missão junto | Cada um tem a própria avaliação (os tiros e o dano de um não contam para o outro) |
| 92.9 | Salvar e sair no meio de uma prova, voltar e terminar | As medidas continuam (a precisão do resumo conta os tiros de antes de sair) |

## 93. Instalações da base: oficial de missões e estação de preparação (PM-8)

Servidor dedicado + 2 clientes. Base gerada com `/place structure kn8:defense_base` (ou Ariake/Tachikawa).

| # | Passo | Esperado |
|---|---|---|
| 93.1 | Entrar no QG da base | Dois oficiais na recepção com o nome acima da cabeça: "Oficial de Missões" e "Oficial de Provas"; não andam, só olham quem chega |
| 93.2 | Botão direito no Oficial de Missões | No chat: saudação, "Próxima patente: X (mérito a/b)" e a prova pendente; o menu abre em "Quadro: Oficial de Missões" só com as missões dele (Patrulha, Defesa da Cidade, horda...) |
| 93.3 | Botão direito no Oficial de Provas | Quadro só com as provas (Exame de Admissão, Prova de Tiro, Esquadrão, Comando, Capitão); aceitar funciona como pelo menu |
| 93.4 | Abrir o menu pela tecla M | Aba Missões normal, com todas as missões (o oficial não tranca nada) |
| 93.5 | Bater/atirar no oficial, kaiju por perto | Não toma dano; kaiju não o atacam; não aparece na aba Esquadrão para recrutar |
| 93.6 | Recrutar 2 soldados, deixá-los feridos e sem pentes (luta ou `/data`), ir ao pátio do alojamento e usar a Estação de Preparação com munição de rifle no inventário | "Esquadrão preparado: 2 soldado(s), 2 curado(s), N balas repostas"; a munição do inventário diminui; quem estava na faca volta ao rifle |
| 93.7 | Usar de novo logo em seguida | "Estação recarregando: espere X s" (1 min) |
| 93.8 | Sem munição suficiente | Repõe o que dá e avisa "Faltaram N balas" |
| 93.9 | Soldados a mais de 16 blocos, ou sem esquadrão | "Nenhum soldado do seu esquadrão a até 16 blocos da estação" |
| 93.10 | Bancada como Oficial Sênior | A Estação de Preparação aparece em Treino (6 ferro, 2 estimulantes, 1 pente de rifle) |

## 94. Provas (cursos) dentro da base, diferentes das missões (PM-8B)

Servidor dedicado + 2 clientes. Base gerada com `/place structure kn8:defense_base` (ou Ariake/Tachikawa).

| # | Passo | Esperado |
|---|---|---|
| 94.1 | Menu (M) → Missões, longe do Oficial de Provas | As provas (etiqueta verde "PROVA") aparecem com o "Aceitar" apagado; passando o mouse: "Aceite com o Oficial de Provas" |
| 94.2 | Botão direito no Oficial de Provas | "Quadro: Oficial de Provas" com os cursos (Agilidade, Força, Resistência) e as provas de patente; recompensa mostra "+N Força/Velocidade/Resistência/Agilidade" |
| 94.3 | Aceitar o Curso de Força | Chat verde "A prova acontece aqui na base: não se afaste mais de 64 blocos"; 2 Primigenius surgem a 6-14 blocos, dentro da base |
| 94.4 | Lutar com eles dentro da base | Golpes não quebram os prédios da base (área protegida durante a prova) |
| 94.5 | Abatê-los | Sem carcaça (somem em fumaça); sem mérito de abate; "Corpo: +160 XP em Força", "+40 XP em Resistência", avaliação e "PROVA APROVADA" |
| 94.6 | Aceitar outra prova e correr para mais de 64 blocos | "Prova cancelada (você saiu da base)"; os kaiju da prova somem; a base volta a poder ser quebrada |
| 94.7 | Abater um kaiju fora da base com uma prova ativa | Não conta para a prova |
| 94.8 | Missões do Oficial de Missões (Defesa da Cidade, horda...) | Continuam em campo (em volta do jogador ou invasão), aceitas também pelo menu |
| 94.9 | O outro cliente fazendo um curso ao mesmo tempo | Cada um só vê contar os próprios kaiju |

## 95. Pesquisa no laboratório (PM-8C)

Servidor dedicado + 2 clientes. Base gerada com `/place structure kn8:defense_base` (terminal na recepção do QG), a
base grande (laboratório de kaiju da arena), Ariake (laboratório de armas numeradas) ou Tachikawa (recepção).

| # | Passo | Esperado |
|---|---|---|
| 95.1 | Botão direito no Terminal de Pesquisa | O menu abre no Arsenal, seção **PESQUISA**, com os 6 projetos; cada um mostra descrição, material (tem/precisa), tempo, o que libera e o bônus |
| 95.2 | Menu (M) → Arsenal → Pesquisa longe do terminal | Botão apagado e "Use um Terminal de Pesquisa (laboratório da base)" |
| 95.3 | Desmontar kaiju até ter 8 tecidos e pesquisar a Análise de Tecido Yoju | "Pesquisa iniciada: ... (pronta em 1:00, mesmo longe da base)"; os 8 tecidos saem do inventário; a linha mostra "Em andamento: m:ss" e a barra |
| 95.4 | Tentar outra pesquisa ao mesmo tempo | Botão apagado, "Laboratório ocupado: ..." |
| 95.5 | Esperar 1 min (pode sair da base) | "Pesquisa concluída", "+10% de dano" no chat e som; a linha vira "Concluída" |
| 95.6 | Bater num Primigenius antes e depois | Golpe ~10% mais forte depois (mesmo teto por golpe) |
| 95.7 | Como Oficial, pesquisar Mecânica Muscular e abrir a bancada | O Mk1 Reforçado, antes "Precisa da patente: Oficial Sênior / ou a pesquisa: Mecânica Muscular", fica liberado |
| 95.8 | `/kn8 research list Dev` | Lista com OK nas concluídas e o motivo nas outras (BUSY, RANK_TOO_LOW, MISSING_PREREQUISITE, MISSING, NO_TERMINAL) |
| 95.9 | Sair do servidor no meio de uma pesquisa e voltar | Continua com o mesmo fim |

## 96. Base modular do jogador (PM-8D)

Servidor dedicado + 2 clientes. Terreno aberto (os lotes limpam o que estiver acima do chão).

| # | Passo | Esperado |
|---|---|---|
| 96.1 | Como Oficial, fabricar o Núcleo da Base na bancada e colocá-lo | "Base fundada!"; usar o núcleo abre o menu na aba **BASE** com a grade 3 x 3 (núcleo no meio, 8 lotes vazios) |
| 96.2 | Escolher o lote de cima, Oficina, CONSTRUIR | O material sai do inventário, a oficina aparece no lote ao norte (porta em cada lado), "Módulo construído: Oficina"; o lote mostra "Oficina" |
| 96.3 | Tentar o Comando antes da Oficina / sem material / abaixo da patente | Botão apagado com "Precisa do módulo: Oficina", material em vermelho, "Precisa da patente" |
| 96.4 | Construir Laboratório, Comando, Alojamento e Pátio de Treino | Cada um no seu lote; no Comando os dois oficiais; Oficial de Provas abre o quadro de provas (as provas valem na sua base) |
| 96.5 | Laboratório: usar o terminal | Abre a seção Pesquisa (PM-8C) |
| 96.6 | DEMOLIR um módulo (dois cliques: DEMOLIR e CONFIRMAR) | O lote fica limpo, baús soltam o conteúdo, os oficiais/bonecos somem, material não volta |
| 96.7 | Colocar um segundo Núcleo da Base | "Você já tem uma base nesta dimensão (núcleo em ...)"; o bloco volta |
| 96.8 | O outro cliente usar o seu núcleo | "Este Núcleo da Base é de outro jogador" |
| 96.9 | Kaiju dentro da grade da base | Não quebra os módulos (`[base] protect`) |
| 96.10 | Quebrar o núcleo | "Seu Núcleo da Base foi destruído..."; os módulos ficam no mundo; dá para fundar outra base |

## 97. Alerta por região e rastro de destruição (PM-9)

Servidor dedicado + 2 clientes num mundo normal com um local da obra (`/locate structure kn8:shinagawa_city`; o
`/place` desses locais não funciona em mundo plano porque eles conferem o terreno).

| # | Passo | Esperado |
|---|---|---|
| 97.1 | Entrar em Shinagawa e soltar um Primigenius na rua (jogador em criativo) | Em até 2 s: chat amarelo "Shinagawa, Tóquio: ALERTA (Primigenius)" e faixa no canto superior esquerdo com o tempo |
| 97.2 | Soldados ou o jogador em sobrevivência lutando com ele | "COMBATE" em vermelho; a faixa desce para baixo da barra de vida do kaiju quando ela aparece |
| 97.3 | Menu M → Alertas | Linha "Região: Shinagawa — COMBATE: Primigenius (m:ss)", igual à faixa |
| 97.4 | Em combate: estação de preparação, terminal de pesquisa e aceitar prova no local | "Instalação fechada: a região está em combate" / "Laboratório fechado" / "Provas fechadas" |
| 97.5 | Matar o kaiju e esperar | 30 s depois "RECUPERAÇÃO" (azul), 2 min depois "NORMAL" (verde); a faixa some |
| 97.6 | Rastreador de Kaiju (bancada) e botão direito perto de onde o kaiju passou | Chat: "Rastro de Primigenius: N pista(s), a mais recente há m:ss, indo para ... — o kaiju caiu aqui / ainda está no mundo, a X blocos / rastro frio"; caminho laranja no chão entre as pistas; "+N de mérito" só na primeira vez |
| 97.7 | Usar o Rastreador de novo | Mesmo rastro, sem mérito novo |
| 97.8 | Esperar 10 min | As pistas somem ("Nenhuma pista de kaiju...") |
| 97.9 | Invasão (`/kn8 invasion start ...`) com o centro no local | COMBATE enquanto ela dura; ao terminar, RECUPERAÇÃO; nesse meio tempo a invasão natural não sorteia outra do local |

## 98. Evacuação com civis (PM-10A)

Servidor dedicado + 2 clientes. Para ver o estado de EVACUAÇÃO na faixa, aceitar a missão dentro de um local da obra
(Shinagawa, como no §97); fora deles a missão funciona igual, só sem a faixa.

| # | Passo | Esperado |
|---|---|---|
| 98.1 | Menu M → Missões → "Evacuação do Bairro" (ou `/kn8 mission accept kn8:evacuation_district`) | Chat: objetivo, "5 civis precisam de escolta; podem ser perdidos no máximo 1", "Ameaça na área: Primigenius"; 5 civis a 10-18 blocos; disco verde no chão no ponto de extração (marcado na HUD) |
| 98.2 | Chegar perto dos civis e andar até o ponto | Eles seguem quem estiver até 12 blocos (jogador ou soldado); sem ninguém perto ficam parados |
| 98.3 | Dentro de um local da obra | Faixa laranja "EVACUAÇÃO" e linha igual na aba Alertas; se o kaiju lutar ali, a faixa vira COMBATE e volta a EVACUAÇÃO depois |
| 98.4 | Levar os civis até perto do ponto | A 16 blocos eles vão sozinhos; a 4 blocos somem com fumaça verde e aparece "Civil evacuado (n/4)" |
| 98.5 | Deixar o Primigenius pegar um civil | "Civil perdido (1 de 1 permitidos)"; com a segunda perda "Missão fracassada (civis demais perdidos)" e os civis que sobraram somem |
| 98.6 | Golpear um civil (jogador) | Nada acontece (fogo amigo não fere) |
| 98.7 | Concluir com 4 evacuados | Recompensa, avaliação com "civis evacuados 4, perdidos N"; o civil de folga some |
| 98.8 | Sair e voltar ao mundo no meio da missão | Os mesmos civis no lugar, sem civis novos |

## 99. Apoio militar (PM-10B)

Servidor dedicado + 2 clientes. Rádio de Campo na bancada (ou `/give @s kn8:field_radio`). Botão direito chama o apoio
selecionado no bloco olhado; Shift + botão direito troca entre Reconhecimento, Suprimentos e Bombardeio (a barra mostra
os usos e a espera).

| # | Passo | Esperado |
|---|---|---|
| 99.1 | Usar o rádio sem missão nem invasão | "Apoio só durante uma missão de campo ou uma invasão"; nada gasto |
| 99.2 | Aceitar uma missão de campo (ex.: Patrulha) e chamar Reconhecimento num lugar sem kaiju | "nenhum kaiju na área (nada foi gasto)" |
| 99.3 | Reconhecimento perto de um kaiju | Chat "Contato: Primigenius a N blocos (X% de vida)"; o kaiju brilha (visto pelo outro jogador também); disco azul no chão |
| 99.4 | Suprimentos perto de um soldado ferido | Disco verde; 3 s depois caem munição e resfriador; o soldado ganha Regeneração |
| 99.5 | Bombardeio no kaiju (Oficial Sênior) | Disco vermelho por 5 s; explosão; kaiju perde até 8% da vida; aliados na área intactos; "Bombardeio concluído: 1 kaiju atingido(s)" |
| 99.6 | Chamar de novo o mesmo apoio | "Este apoio já foi usado nesta operação" ou a espera; nada é gasto |
| 99.7 | Patente baixa pedindo bombardeio | "Patente insuficiente para este apoio" |
| 99.8 | Dentro da área de uma invasão (`/kn8 invasion start ...`) | O rádio funciona sem missão |

## 100. Armas numeradas do jogador: Numbers 1, 2 e 6 (PM-11A)

Servidor dedicado + 2 clientes. `/give @s kn8:numbers2_gauntlets` (e `numbers1_bayonet`, `numbers6_rifle[kn8:rounds=30]`).

| # | Passo | Esperado |
|---|---|---|
| 100.1 | Inventário (E), passar o mouse sobre a arma, como Candidato e sem pesquisa | Dica com "Equipamento numerado", ✘ vermelho em Patente e Pesquisa, ✔ ou ✘ no limite pessoal, "Domínio 0/4 · dano 70%" e "Instável" |
| 100.2 | Golpear um kaiju assim (sobrevivência) | Nada sai; barra "Arma numerada exige a patente ..." e HUD "Arma numerada: requisitos não cumpridos" |
| 100.3 | `/kn8 rank set kn8:lider_pelotao @s` e concluir a pesquisa (cadeia até "Teoria das Armas Numeradas") | Chat "Liberado na bancada: Numbers 1/2/6"; dica com ✔ verdes (Numbers 1 continua pedindo Vice-Capitão) |
| 100.4 | Golpear e usar o especial (R) das manoplas | Golpes saem; Force Blast com a onda no chão; HUD "Force Blast: 11s"; o calor sobe mais que num especial comum |
| 100.5 | Tiro Congelante do Numbers 6 num kaiju | O kaiju fica com Lentidão III por ~4 s |
| 100.6 | Bater no kaiju até somar 300 de dano | Chat "Domínio do Numbers 2: nível 1 de 4"; a dica mostra o dano maior (77%) e o XP |
| 100.7 | Morrer e voltar | O domínio continua |
| 100.8 | Segundo cliente em inglês | Dica "Numbered equipment", "Rank", "Research", "Mastery" |

## 101. Numbers 4 e Numbers 10 do jogador (PM-11B)

Servidor dedicado + 2 clientes. `/give @s kn8:numbers4_axe` e `kn8:numbers10_suit` (vestir no peito). Requisitos:
Vice-Capitão (`/kn8 rank set kn8:vice_capitao @s`), pesquisa "Teoria das Armas Numeradas" e limite pessoal 30%/40%.

| # | Passo | Esperado |
|---|---|---|
| 101.1 | Passar o mouse no machado e no traje | Dica com os requisitos (✔/✘), domínio e as linhas "Asas: segure Shift no ar para planar" e "Cauda: corta sozinha kaiju a até 4.5 blocos e segura até 70%..." |
| 101.2 | Sem os requisitos: pular de um lugar alto com o machado e Shift; ficar perto de um kaiju com o traje | Cai normalmente; a cauda não corta |
| 101.3 | Com os requisitos: pular de 20 blocos com o machado e segurar Shift | Impulso para a frente, queda lenta, traços ciano das asas saindo das costas; stamina descendo; sem dano ao pousar |
| 101.4 | Soltar o Shift no ar | As asas fecham e a queda volta ao normal |
| 101.5 | Kaiju parado atrás do jogador de traje | A cada 2 s o kaiju leva um corte (pisca vermelho) e a dica do traje mostra o XP subindo |
| 101.6 | Kaiju atirando nas costas do jogador de traje (ex.: Philinosoma) | Faísca vermelha nas costas e menos dano; de frente, dano inteiro |
| 101.7 | R com o machado perto do kaiju | Mergulho do Céu: onda e rachadura no chão, kaiju empurrado e ferido |
| 101.8 | Segundo cliente em inglês | Dica "Wings: hold Shift in the air to glide" e "Tail: ..." |

## 102. Integração do Plano Mestre (PM-12)

Servidor dedicado + 2 clientes, num local da obra (Shinagawa: `/locate structure kn8:shinagawa_city` num mundo normal).
Relatório e resultados em `docs/PM12_INTEGRACAO.md`.

| # | Passo | Esperado |
|---|---|---|
| 102.1 | Jogador 1 aceita "Evacuação sob Honju" e jogador 2 "Evacuação do Bairro" no mesmo local | Civis, pontos de extração e ameaças de cada um; faixa EVACUAÇÃO nos dois |
| 102.2 | Rádio: reconhecimento e bombardeio no Honju | Kaiju marcados, faixa COMBATE, "Bombardeio concluído: N kaiju atingido(s)" |
| 102.3 | Derrubar o Honju (esquadrão, traje Numbers 10) e levar os civis | Cada jogador com a própria avaliação (nota, civis evacuados/perdidos); perdas de civis dão a dica de escolta |
| 102.4 | Rastreador no lugar da luta | Rastros "o kaiju caiu aqui" e mérito uma vez só |
| 102.5 | Reiniciar o servidor e morrer/renascer | Patente, mérito, pesquisa, domínio e notas iguais |
| 102.6 | `/neoforge tps` durante a luta | 20 TPS |

## 103. Larva rara e infecção (T-1)

Servidor dedicado + 2 clientes. A larva aparece sozinha só em restos de kaiju (construção de restos ou carcaça a até 24
blocos) ou dentro de um local da obra, com chance de 0,4% a cada 30 s por jogador; para testar, use
`/kn8 host spawn <jogador>`. `/kn8 host status` diz quem é o hospedeiro e se há larva voando; `/kn8 host clear
<jogador>` desfaz.

| # | Passo | Esperado |
|---|---|---|
| 103.1 | `/kn8 host spawn Dev` e ficar parado | Zumbido baixo; a larva aparece a ~10 blocos, circula o jogador por ~5 s e mergulha no peito |
| 103.2 | Quando a larva encosta | Some; título "Algo entrou no seu corpo", subtítulo, mensagem no chat, náusea, escuridão rápida e batida de coração |
| 103.3 | `/kn8 host status` e `/kn8 host spawn Dev2` | "Hospedeiro: Dev"; para o Dev2: "Este mundo já tem um hospedeiro..." (só um jogador por mundo) |
| 103.4 | Morrer e renascer, reiniciar o servidor | O Dev continua hospedeiro |
| 103.5 | `/kn8 host clear Dev`, `/kn8 host spawn Dev` e se afastar 50+ blocos | A larva vai embora (fumaça); status "nenhuma larva no mundo" |
| 103.6 | Soldados perto da larva; larva num local da obra | Os soldados não atacam a larva; a faixa da região não entra em alerta |

## 104. Transformação em Kaiju No. 8 (T-2)

Servidor dedicado + 2 clientes. O jogador precisa ser o hospedeiro (`/kn8 host infect Dev` ou a larva da T-1). Tecla
**B** (configurável em Controles, categoria kn8). Golpes da forma, limite de 2 minutos e perda de controle vêm na T-3.

| # | Passo | Esperado |
|---|---|---|
| 104.1 | Dev2 (não infectado) aperta B | "Nada responde dentro de você." na barra de ação; nada muda |
| 104.2 | Dev (hospedeiro) aperta B | Rugido, fumaça e linhas ciano; Dev2 vê o Dev como o Kaiju No. 8 (2 m, nome em cima, aura azul com raios); vida 200 (com resistência treinada, mais) |
| 104.3 | Dev olha em 1ª pessoa e em 3ª (F5) | Em 1ª pessoa o braço escuro do No. 8 no lugar da mão/arma; em 3ª pessoa o corpo do No. 8 |
| 104.4 | Dev anda e corre | Dev2 vê o No. 8 andando/correndo com a passada |
| 104.5 | Dev segura G (Release) transformado | A % fica em 0 (a forma não usa Release) |
| 104.6 | Dev aperta B de novo e logo depois outra vez | Volta a humano (vida e altura normais); a segunda vez avisa a espera (~10 s) |
| 104.7 | Transformado, relogar e morrer | Relogando continua No. 8; morrendo renasce humano |

## 105. Combate da forma, limite de tempo e descontrole (T-3)

Servidor dedicado + 2 clientes; Dev hospedeiro (`/kn8 host infect Dev`, B para transformar). Observação: o servidor de
teste tem `spawn-animals=false` (animais somem sozinhos); use zumbis, husks e soldados como alvo.

| # | Passo | Esperado |
|---|---|---|
| 105.1 | Transformado, com qualquer arma na mão, clicar num kaiju | Socos do No. 8 (combo de 3, o fantoche soca para o Dev2), dano grande (Primigenius cai em ~5 socos), sem a arma; HUD "Golpe no Chão PRONTO [R]" |
| 105.2 | R perto do kaiju | Golpe no Chão: onda, kaiju empurrado e atordoado, recarga de 10 s |
| 105.3 | Barra de ação | "Forma kaiju 1:55 · Primigenius a 3 m (S)": tempo que resta e o kaiju mais perto (até 64 blocos); vermelho nos últimos 20 s |
| 105.4 | Ficar abaixo de metade da vida | Regenera 1,5% da vida por segundo |
| 105.5 | `/kn8 host rampage Dev` (ou esperar 2 min) com zumbi, husk e soldado em volta | Rugido, "Você perdeu o controle do corpo!", "SEM CONTROLE"; o Dev não anda nem soca pela própria vontade, a câmera vira sozinha para o alvo e o No. 8 corre até cada um e o derruba (soldados e civis também); jogador em criativo/espectador não é atacado |
| 105.6 | 15 s depois | Volta humano, "Você recupera o controle. O corpo está exausto.", Lentidão e Fraqueza; B avisa espera de 2 min |
| 105.7 | Com traje/arma numerada, transformado | O domínio do numerado não sobe; a cauda e as asas não agem |
| 105.8 | Olhar o Dev sem controle pelo Dev2 e o próprio braço em 1ª pessoa | As linhas e os olhos ciano do No. 8 ficam vermelhos enquanto durar o descontrole e voltam ao ciano depois; o No. 8 NPC continua ciano |

## 106. Ossos articulados e animações refeitas (RIG-1)

Servidor dedicado + 2 clientes; área plana; `/summon kn8:isao`, `kn8:isao_base`, `kn8:kaiju_no13`, `kn8:kaiju_no9_isao`
(com `{NoAI:1b}` para disparar golpes por comando: `/execute positioned <x y z> run kn8 anim soldier action <técnica>`
ou `kn8 anim kaiju action action.<golpe>`; a técnica precisa ser da própria entidade: o Heavy Straight é do Isao sem o
Numbers 2).

| # | Passo | Esperado |
|---|---|---|
| 106.1 | Olhar os três parados, de frente e de lado | Sem buraco nem peça solta nas juntas (cotovelo, punho, joelho, cintura, pescoço); Isao com os braços meio dobrados; No. 13 curvado com joelhos e cotovelos dobrados; No. 9 com o Isao ereto, manoplas meio erguidas |
| 106.2 | Isao (Numbers 2): Thunder Emperor, Force Blast, Spiral Blast, Barrage Blast, contra-ataque, aparar | Golpes de corpo inteiro: o peito e o quadril giram, o cotovelo dobra no preparo e estica no impacto, a perna da frente dobra no avanço; nada de braço reto "de zumbi"; volta à guarda sem corte seco |
| 106.3 | Isao sem o Numbers 2: Jab Combo, Elbow Strike, Heavy Straight | Jabs alternados saindo da guarda; cotovelada cruzando o corpo; direto com avanço |
| 106.4 | Isao andando e correndo (tirar o `NoAI`, dar um alvo) | Joelhos dobram na passada, pés acompanham, guarda de boxeador mantida, peito balança contra o quadril |
| 106.5 | No. 13: garra, rajada, soco, agarrão, investida, bote | Garra de cima para baixo cruzando o corpo; agarrão com o braço esticado e a mão fechando; investida curvada com os braços para trás; bote agachando fundo e caindo com as garras à frente |
| 106.6 | No. 9 com o Isao: Finger Gun, Multi-Finger Gun, tentáculo, Thunder Emperor, Force Blast, rajada, absorção | Braço esticado na mira com o coice no tiro; tentáculo com avanço; absorção puxando em pulsos |
| 106.7 | Isao x No. 13 com IA | Andam, correm e golpeiam com as animações novas; o dano continua no tick do JSON |

## 107. Ossos articulados dos outros 16 modelos (RIG-2)

Servidor dedicado + 2 clientes. `/summon` de cada um com `{NoAI:1b}` em fila (soldado, Hoshina, Hoshina No. 10, Kikoru,
Kikoru No. 4, Mina, Reno, Reno No. 6, Narumi, Kafka, Kaiju No. 8, No. 9, forma preta, fusão, No. 10 pequeno e
gigante) e depois sem `NoAI` com um kaiju por perto.

| # | Passo | Esperado |
|---|---|---|
| 107.1 | Olhar a fila de frente e de lado | Sem buraco nas juntas; cauda do Hoshina No. 10, asas da Kikoru No. 4, cauda/mandíbula dos No. 10 e da fusão no lugar; armas nas mãos como antes |
| 107.2 | Kaiju No. 8 (modelo novo, mais orgânico, espinhos nas costas) | Guarda de brigão; combo, soco pesado, investida com soco, Golpe no Chão com os dois punhos, rugido abrindo o peito, gancho no contra-ataque |
| 107.3 | Hoshina (modelo novo) | As duas espadas presas nas mãos em todas as posturas e técnicas (a pegada foi recalculada) |
| 107.4 | Soldados, Kikoru, Mina, Reno, Narumi, Kafka andando, correndo e atacando | Joelhos dobram na passada, pés acompanham, peito gira junto do quadril; braços e armas como antes |
| 107.5 | No. 9 e forma preta | No. 9 ereto e frio; forma preta curvada; garra, Finger Gun, braço-lâmina, espinhos encolhendo e explodindo, tentáculos, bote |
| 107.6 | No. 10 pequeno, gigante e fusão andando e golpeando | Joelhos, pés e cotovelos acompanham; cauda como antes |
| 107.7 | Jogador hospedeiro transformado (B), em 1ª pessoa | O braço do No. 8 novo inteiro (ombro, antebraço e mão) aparece; em 3ª pessoa o fantoche usa o modelo novo |

## 108. Correcoes da FIX-1 (ovo do oficial, espada, Release acima do limite, empurrao, volume)

Servidor dedicado + 2 clientes; volume geral como o Miguel usa (29%).

| # | Passo | Esperado |
|---|---|---|
| 108.1 | Criativo, aba de ovos do kn8: ovo do Oficial de Missoes | Ovo preto e dourado (antes cubo roxo e preto); usar no chao faz surgir o oficial |
| 108.2 | `kn8:sword` na mao, 3a pessoa (F5) e visto pelo outro cliente, parado e andando | Espada em guarda: braco a frente na altura do peito, lamina para a frente e para cima (antes apontava para o chao) |
| 108.3 | Traje Mk1, limite 40 (`/kn8 release set 40 Dev`), Release a 60% (`/kn8 release active 60 Dev`), sobrevivencia | Calor sobe sem tirar vida; so com o calor no maximo a vida cai (~29 s sem dano no teste; depois ~3 de vida/s); voltar para 40% para o desgaste |
| 108.4 | Zumbi/kaiju batendo no jogador | O jogador toma o dano e nao e empurrado (`[combat] playerKnockback` 0; 1 = como antes) |
| 108.5 | Rugido de kaiju, tiros, golpes de espada, sirene | Sons do mod bem mais baixos (35%), ainda ouvidos de longe como antes; `modSoundVolume` no `kn8-client.toml` ajusta (0 a 1) |

## 109. FIX-2: modelos inteiros e armas na mao depois do RIG-2

Servidor dedicado + 2 clientes. `/summon` de cada um com `{NoAI:1b}` para olhar parado e sem `NoAI` perto de um kaiju
para andar, correr e lutar. Imagem: `docs/img/fix2_armas_e_juntas.jpg`.

| # | Passo | Esperado |
|---|---|---|
| 109.1 | Soldado, Kafka, Reno, Reno No. 6: parado, andando, correndo, mirando | Fuzil na mao como antes do RIG-2 (antes do FIX-2 saia de lado/para fora da mao) |
| 109.2 | Kikoru e Kikoru No. 4 com o machado | Machado na horizontal a frente com as duas maos; correndo e nos golpes o cabo continua na mao (antes da correcao a No. 4 ficava com o machado a ~17 px da mao) |
| 109.3 | Mina (canhao) e Narumi (baioneta) | Canhao no quadril e baioneta nas maos |
| 109.4 | Hoshina e Hoshina No. 10 | As duas espadas no punho parado, andando e correndo (pegada refeita com o eixo ombro -> mao) |
| 109.5 | No. 10 pequeno, No. 10 gigante e fusao andando, investindo e golpeando | Tronco e pernas juntos: a bacia nao abre quando o tronco inclina (pernas voltaram a ser filhas do tronco, como antes do RIG-2) |
| 109.6 | Os personagens acima atacando e saltando (vault) | Tronco, ombros e quadril giram no mesmo ponto de antes do RIG-2; joelho, pe, peito e cotovelo novos continuam |
| 109.7 | No. 8, No. 9, forma preta, Isao, No. 13, No. 9 com o Isao | Sem mudanca nesta etapa (animacoes feitas sobre o rig novo) |

## 110. COMBATE-1: varias tecnicas por arma para o jogador

Servidor dedicado + 2 clientes, sobrevivencia. Duas espadas do Hoshina (uma em cada mao), Primigenius a frente.
Imagem: `docs/img/combate1_tecnicas_do_jogador.jpg`.

| # | Passo | Esperado |
|---|---|---|
| 110.1 | Olhar a HUD com as duas espadas | "Kūuchi PRONTO [R] (1/2, troca [X])" |
| 110.2 | R | Sai o Kūuchi (corte que voa); a linha mostra a recarga dele |
| 110.3 | X | A linha troca para "Kōsa-uchi ... (2/2 ...)" e acende por um instante; Shift + X volta |
| 110.4 | R com o Kūuchi ainda recarregando | Sai o Kōsa-uchi (dois cortes em X); o outro cliente ve os cortes |
| 110.5 | X e R logo depois | Kūuchi negado (recarga propria dele continua) |
| 110.6 | Machado, faca, espada | Uma tecnica so: X nao muda nada e R continua como antes |
| 110.7 | Controles > kn8 | Tecla "Trocar tecnica da arma" (X), configuravel |

## 111. COMBATE-2: pernas dobrando nas posturas e Hoshina No. 10 ajoelhado

Servidor dedicado + 2 clientes. Imagem: `docs/img/combate2_pernas_e_no10.jpg`.

| # | Passo | Esperado |
|---|---|---|
| 111.1 | `/summon kn8:hoshina_no10 ~ ~ ~ {NoAI:1b}` e olhar de frente e de lado | Ajoelhado: joelho direito no chao, perna esquerda a frente com o pe plano, tronco inclinado, cabeca para a frente, laminas nas maos, respirando |
| 111.2 | Hoshina No. 10 com alvo | Sai da pose ajoelhada para andar/correr e volta a ela parado |
| 111.3 | Soldado, Hoshina, Kikoru, Kikoru No. 4, Mina, Reno, Reno No. 6, Narumi, Kafka andando e correndo (de lado) | A perna que vai a frente dobra o joelho e estica ao pisar; correndo o pe de tras sobe; pe plano no apoio; armas nas maos como antes |
| 111.4 | Os mesmos parados | Joelhos levemente dobrados (mais com a perna aberta), pes no chao |
| 111.5 | No. 10 pequeno, gigante e fusao andando | Joelho dobrando na passada; cauda como antes |

## 112. COMBATE-3: Kikoru e Kikoru No. 4 com as tecnicas da obra

Servidor dedicado + 2 clientes. Imagem: `docs/img/combate3_kikoru.jpg`.

| # | Passo | Esperado |
|---|---|---|
| 112.1 | Machado (`kn8:axe`) na mao, olhar a HUD e apertar X varias vezes | Golpe Sismico (1/7), No. 1 Falling Thunder, No. 2 Water Skimmer, No. 3 Half-Moon, No. 4 Rising Thunder, No. 5 Twin Swallows, No. 6 Daruma Dropper |
| 112.2 | Falling Thunder no chao | Recusado: "Pule de cima: precisa estar no alto, acima do alvo" |
| 112.2b | Subir numa construcao (parkour), pular em cima do kaiju e apertar R no ar | Mergulha com o machado erguido; ao tocar o kaiju (ou o chao) crava: raio dourado caindo do ceu, clarao, rachadura e onda; quanto mais alto, mais dano |
| 112.3 | R em cada uma das outras | Corte horizontal com onda a frente; giro inteiro; corte de baixo para cima que lanca; avanco com dois cortes em X; quatro cortes descendo; cada uma com a propria recarga |
| 112.4 | Machado do Numbers 4 (com os requisitos) | Mergulho do Ceu, as seis e o Daruma Dropper: Ten Hit (10 cortes) |
| 112.5 | Kikoru e Kikoru No. 4 NPC contra um Honju | Usam as tecnicas da obra (os golpes genericos aparecem menos); no Falling Thunder a Kikoru salta alto (4-9 blocos) e cai sobre o kaiju com o raio do ceu; a No. 4 tambem o Ten Hit |

## 113. COMBATE-4: Isao e No. 9 com o Isao com as tecnicas da obra

Servidor dedicado + 2 clientes. Imagem: `docs/img/combate4_isao.jpg`.

| # | Passo | Esperado |
|---|---|---|
| 113.1 | Manoplas do Numbers 2 (com os requisitos ou no criativo), X varias vezes | Force Blast (1/6), Barrage Blast, Twin Blast, Rising Blast, Spiral Blast, Thunder Emperor |
| 113.2 | R em cada uma perto de um kaiju | Cotovelada curta; duas ondas dos dois punhos; uppercut que lanca; soco com espiral amarela que voa; barragem de 9 socos com tracos amarelos (recarga longa) |
| 113.3 | Isao (`kn8:isao`) contra um Honju | Usa as tecnicas acima; o Thunder Emperor e uma barragem de socos e o ultimo, maior, expoe o nucleo ("EXPOSTO") |
| 113.4 | Isao sem o Numbers 2 (`kn8:isao_base`) | So golpes fisicos (jab, cotovelada, direto); sem ondas nem Thunder Emperor |
| 113.5 | No. 9 com o Isao contra um soldado especial | Cotovelada e Thunder Emperor em barragem com os tracos amarelos |
| 113.6 | Thunder Emperor das manoplas com a vida cheia (sobrevivencia) | Recusado: "Ultimo recurso: so com a vida baixa"; com 40% da vida ou menos sai |
| 113.7 | Isao NPC e No. 9 com o Isao com a vida acima de 40% | Nao usam o Thunder Emperor (so as outras); abaixo de 40% ele volta a aparecer |
