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
| 31.2 | Olhar as mãos do Hoshina e as suas (F5) com a espada dele | A mão segura o meio do cabo (sobra um pouco do pomo do outro lado), não a ponta |
| 31.3 | Perto do Hoshina parado: `/kn8 anim soldier action <kuuchi, kosa_uchi, kaeshi_uchi, ran_uchi, kasumi_uchi, yae_uchi>` | 1: um corte só, rápido, com avanço; 2: as duas lâminas fecham em X na frente do peito; 3: abaixa e gira com as lâminas cruzadas; 4: diagonal alta, horizontal baixa, subida e varredura girando, em sequência; 5: X com as duas e o terceiro corte baixo pelo lado; 6: oito cortes alternando; todas saem e voltam agachado |
| 31.4 | Hoshina lutando contra um kaiju | As técnicas aparecem na luta com a postura baixa |
| 31.5 | Jogador com uma espada do Hoshina em cada mão: combo e R | Golpes diferentes alternando as lâminas; o Kūuchi é um corte rápido para fora com avanço |
| 31.6 | Jogador com o machado: R | Salta com o machado erguido e crava no chão (Axe Slam da Kikoru) |
| 31.7 | Servidor dedicado + 2 clientes | Os dois veem as mesmas animações |
