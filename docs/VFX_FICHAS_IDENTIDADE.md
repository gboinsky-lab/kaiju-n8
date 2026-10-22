# Fichas de identidade visual (KaijuVFX, VFX-0, 2026-10-22)

Ficha obrigatória da `docs/DIRETRIZ_KAIJU_VFX.md` §2, uma por personagem e forma. Atualizar a cada etapa VFX.

**Como as cores foram tiradas:** paleta dominante de cada textura do jogo (`textures/entity/<id>.png`, k-médias de 6
cores nos pixels opacos) e cores de destaque (pixels saturados e claros: olhos, núcleo, detalhes; k-médias de 3). Folha
de amostras: `docs/img/vfx0_paletas_das_texturas.png` (à esquerda a paleta, à direita os destaques com borda branca).
Script: `tools/art/vfx_paletas.py`.

**Fontes:** as texturas e modelos do Miguel (referência principal, como manda a diretriz §1); fichas já feitas
(`docs/FICHAS_IA_PERSONAGENS.md`, `docs/PERFIS_PERSONAGENS_MOV5.md`, `docs/FICHAS_0_8_A.md`); pesquisa na web em
2026-10-22. Da pesquisa, só três fatos visuais foram confirmados: o No. 8 tem corpo cinza-escuro com **linhas e olhos
ciano** que brilham (o autor citou TRON como inspiração); as lâminas do Hoshina são **pretas** (visual oficial de
2024); o Numbers 6 do Reno é de **gelo** (criocinese) e o Numbers 4 da Kikoru dá **voo**. Wikipédia e Fandom não
abriram nesta máquina (DNS). Todo o resto é marcado:

- **CANON**: confirmado na obra.
- **MODELO**: está no modelo/textura do Miguel.
- **[SUPOSIÇÃO]**: escolha minha, a confirmar.
- **[A PESQUISAR]**: a obra mostra, mas não consegui confirmar.

**Qualidade baixa (vale para todos):** o nível `[fx] particleLevel` (baixo 35%, médio 70%, alto 100%) reduz a
quantidade; no baixo, cada efeito mantém só a camada que define a silhueta (o arco do corte, o anel da onda, o feixe)
e perde fumaça, faíscas soltas e detritos. O tremor de câmera segue `[fx] cameraShake`.

---

## Personagens

### Kafka Hibino (`kafka`)

| Item | Ficha |
|---|---|
| Paleta (textura) | uniforme preto `#030303`/`#0C0C0B`, cinza `#51504C`, `#292925`; pele `#E3DCD6`; quase sem cor saturada (0,2%) |
| Arma | rifle comum (MODELO) |
| Hoje | aura `defense_force` (ciano `#3FD8FF`, faíscas, igual à do jogador); tiro com chamas pequenas no cano |
| Proposta | soldado de força baixa: **sem aura visível** até perto da transformação [SUPOSIÇÃO]; tiro com clarão amarelo-alaranjado curto e fumaça cinza (efeito de soldado comum); soco (`punch_combo`) com poeira curta |
| Transformação → No. 8 | efeito próprio, diferente de ataque: **linhas ciano rachando pelo corpo do Kafka** (de dentro para fora, origem no peito), estalo e uma coluna de fumaça escura com faíscas ciano; fecha com o anel de choque do No. 8. Ciano = CANON (linhas do No. 8); a sequência é [SUPOSIÇÃO] |
| Baixa qualidade | só as linhas ciano e o anel |

### Kaiju No. 8 (`kaiju_no8`)

| Item | Ficha |
|---|---|
| Paleta (textura) | corpo cinza-escuro `#343938`, `#242626`, `#131314`; verde-acinzentado `#405C5B`; **destaque ciano `#18E3E3`/`#1FA1A1`** (linhas do corpo, CANON) |
| Hoje | aura `kaiju_no8` (chama ciano `#3FF2E0`); golpes com os efeitos genéricos de impacto |
| Socos (`rush_combo`, `heavy_punch`, `dash_strike`) | rastro curto e grosso do punho (origem: osso da mão, pelo keyframe da animação) em ciano com miolo branco; no impacto, **anel de ar comprimido** branco-ciano e lascas escuras; o pesado empurra um cone de poeira para a frente. Silhueta: curta, explosiva, em linha reta (não é arco de lâmina) |
| `ground_smash` | rachaduras escuras saindo do punho com **brilho ciano dentro das fendas** e pedras subindo; coluna de poeira |
| `roar` | ondas de pressão em anéis concêntricos saindo da boca + as linhas do corpo pulsando |
| Regeneração | pontos ciano **convergindo** para o corpo e se apagando (o oposto do impacto, que espalha) [SUPOSIÇÃO] |
| Aura | manter a chama ciano, mais baixa e colada às linhas do corpo |
| Baixa qualidade | rastro do punho + anel do impacto |

### Soshiro Hoshina (`hoshina`)

| Item | Ficha |
|---|---|
| Paleta (textura) | preto `#1E1C20`, roxo `#4C3955`, cinza `#7C757D`, branco `#EFECEF`; **destaque roxo `#57246F`/`#6B3184`** (cabelo/detalhes) |
| Arma | duas espadas de **lâmina preta** (CANON, visual de 2024) |
| Hoje | aura violeta com raios (pedido do Miguel); cortes `SlashProjectile` roxos `#B070FF`/`#C890FF` |
| Cortes | **arcos finos e longos, quase pretos com borda violeta** (a cor da lâmina por dentro, o roxo só no fio); somem rápido (3–4 ticks): técnica de precisão, sem poeira grossa. Cada forma com a sua silhueta: Kuuchi = um arco horizontal longo; Kōsa-uchi = X à frente do peito; Kaeshi-uchi = meia-lua girando; Ran-uchi = 4 arcos em alturas diferentes; Kasumi-uchi = X duplo + um baixo; Yae-uchi = 8 arcos em estrela |
| Impacto | lascas pequenas violetas e um traço branco no ponto de contato (sem explosão) |
| Origem | lâmina de cada mão (keyframes `item_right`/`item_left` da animação) |
| Aura | manter violeta com raios, finos e rentes ao corpo |
| Baixa qualidade | só os arcos |

### Hoshina com o Numbers 10 (`hoshina_no10`)

| Item | Ficha |
|---|---|
| Paleta (textura) | **vermelho-escuro do traje `#732D37`, `#893D46`, `#933B46`** (MODELO: o traje é do No. 10, vermelho), preto `#1B1516` |
| Hoje | a mesma aura violeta do Hoshina e os mesmos cortes roxos |
| Cor (Miguel, 2026-10-22) | **continua violeta** (aura e cortes); **raios saindo do traje em vermelho puxado para o roxo** (ex.: `#9A2A5A`, entre o vermelho do traje e o violeta) [SUPOSIÇÃO o tom exato] |
| Proposta | cortes violeta como os do Hoshina; **raios do traje**: descargas finas vermelho-arroxeadas saindo do corpo/traje (aura), mais fortes com a sincronização; **cauda**: rastro vermelho com faíscas na ponta a cada golpe da cauda; guarda da cauda: faísca vermelha no ponto bloqueado; **Full Release**: o traje solta vapor vermelho fino; **Jūni-hitoe**: 12 arcos violeta com raios vermelho-arroxeados, o 12º maior, terminando num corte em cruz que expõe o núcleo |
| Baixa qualidade | arcos e rastro da cauda |

### Kikoru Shinomiya (`kikoru`)

| Item | Ficha |
|---|---|
| Paleta (textura) | **amarelo-dourado `#F0D673`/`#F2D870`** (cabelo), preto `#0F100E`, oliva `#403F30`, bege `#DBCEBB` |
| Arma | machado grande de duas mãos (CANON) |
| Hoje | aura amarela com raios (pedido do Miguel); cortes amarelos `#F2C94C`/`#FFD700` |
| Golpes | **arcos largos e grossos** (o oposto do Hoshina): miolo dourado, borda branca, duram mais (6–8 ticks), arrastam poeira e lascas de pedra; `heavy_swing` deixa uma meia-lua no chão |
| `shockwave` | semicírculo de energia dourada correndo pelo chão para a frente, levantando pedras |
| `ground_smash` | cratera: rachaduras + coluna de poeira + anel dourado |
| Origem | cabeça do machado (keyframe do item na animação) |
| Baixa qualidade | arco do machado e anel do impacto |

### Kikoru com o Numbers 4 (`kikoru_no4`)

| Item | Ficha |
|---|---|
| Paleta (textura) | cinza `#848782`, bege `#BEB8A0`, branco-azulado `#D5E3E3`; **destaque ciano `#3CC5EC`** (asas, MODELO) e dourado `#D8C167` |
| Hoje | aura `kikoru_no4_wind` ciano e dourada com raios |
| Proposta | **vento** (voo é CANON): linhas de ar ciano saindo das asas a cada batida e atrás dela no voo (streaks finos e longos); `dive_strike`: cone de ar ao redor do corpo e, no impacto, anel ciano com borda dourada; `aerial_dash`: rastro de vento reto; os golpes do machado mantêm o dourado da Kikoru |
| Origem | asas (`wing_left`/`wing_right`) e machado |
| Baixa qualidade | rastro do voo e anel do mergulho |

### Mina Ashiro (`mina`)

| Item | Ficha |
|---|---|
| Paleta (textura) | preto `#090908`, cinzas `#393B36`–`#D6D0CA`; **nenhuma cor saturada** (0%) |
| Arma | canhão maior que ela (CANON) |
| Hoje | aura laranja (`mina_command`, [SUPOSIÇÃO] antiga); tiros laranja/amarelo/branco com explosão |
| Cor (Miguel, 2026-10-22) | **verde** (aura e energia dos disparos); a aura laranja `mina_command` passa a verde na etapa dela |
| Proposta | clarão grande na boca do canhão com miolo branco e borda **verde**, **rastro de fumaça cinza grossa**, explosão com anel de choque verde e fumaça escura; `charged_cannon`/`anti_giant_shot`: carregamento com faíscas verdes convergindo para o cano (verde → branco), poeira nos pés no disparo (recuo), projétil espesso |
| Origem | boca do canhão (o cano fica na horizontal na animação: ponto pelo osso `item_right`) |
| Baixa qualidade | clarão, projétil e anel da explosão |

### Reno Ichikawa (`reno`)

| Item | Ficha |
|---|---|
| Paleta (textura) | cinza-claro `#B2B7AE`, `#D3D1C8`, preto `#0D0E0D`; sem cor saturada |
| Arma | rifle (MODELO) |
| Hoje | aura azul-gelo `reno_frost`; tiros azul-claro |
| Proposta | munição congelante do mod: rastro de bala branco-azulado com **cristais pequenos** caindo; impacto com lascas de gelo e névoa; `freeze_round`: geada no ponto atingido. Sem aura forte (Reno base) [SUPOSIÇÃO] |
| Baixa qualidade | rastro e lascas |

### Reno com o Numbers 6 (`reno_no6`)

| Item | Ficha |
|---|---|
| Paleta (textura) | **azul-marinho `#052F4B`**, preto-azulado `#0E151A`, cinza-claro `#C5CDCB`; destaques azul `#32556D`/`#14415E` |
| Hoje | aura `reno_no6_ice` (azul-gelo); tiros azul-claro |
| Proposta | **gelo** (CANON: criocinese): `ice_spear` = cristal longo e afiado com rastro de névoa; `freeze_burst` = estilhaços em esfera + névoa branca + geada no chão; `ice_field` = anel de geada se espalhando pelo chão com cristais pequenos; `multi_freeze` = vários rastros curtos; `auxiliary_cannons` = disparos menores azul-marinho. Diferente do Reno base pelo tamanho e pelo azul-marinho do traje na borda |
| Baixa qualidade | cristal/estilhaços sem névoa |

### Gen Narumi (`narumi`)

| Item | Ficha |
|---|---|
| Paleta (textura) | rosa-acinzentado `#9E7A83`, `#C2B2B1`, marrom `#685759`, preto; sem cor saturada |
| Arma | baioneta longa (fuzil com lâmina, CANON) |
| Hoje | aura rosa `narumi_pink` [SUPOSIÇÃO antiga]; tiro rosa |
| Proposta | **estocadas = linhas retas, curtas e finas** (silhueta de agulha, nada de arco), brilho no ponto da lâmina; `sweeping_slash` é o único arco; `bayonet_shot` = rastro fino e reto. Cor (Miguel, 2026-10-22): **rosa** (aura `narumi_pink` mantida) com miolo branco |
| Baixa qualidade | as linhas das estocadas |

### Narumi com o Numbers 1 (`narumi_no1`)

| Item | Ficha |
|---|---|
| Paleta | a mesma do Narumi (usa o modelo dele; modelo próprio futuramente) |
| Proposta | previsão (CANON: as retinas preveem o movimento): **brilho nos olhos** quando lê um golpe do kaiju e uma **linha fina de previsão** do kaiju até o ponto onde vai bater, durante o preparo [SUPOSIÇÃO da forma]; `weak_point_thrust` = marca no núcleo do kaiju antes da estocada; `predicted_counter` = rastro duplo (a "imagem" prevista e o golpe real). Cor: a do Narumi com mais branco |
| Origem | olhos (osso da cabeça) e ponta da baioneta |
| Baixa qualidade | brilho nos olhos e a estocada |

### Isao Shinomiya com o Numbers 2 (`isao`)

| Item | Ficha |
|---|---|
| Paleta (textura) | bege `#C1BA9B`, `#DFDDD4`, preto `#0E0F0F`, cinzas; sem cor saturada |
| Arma | manoplas do Numbers 2 (CANON) |
| Hoje | aura laranja `isao_numbers2` [SUPOSIÇÃO antiga]; ondas laranja `#FF9F40`/`#FFD480` |
| Proposta | **ar e impacto**, não fogo: Barrage Blast = sequência de anéis curtos de ar comprimido saindo dos punhos; Spiral Blast = projétil com rastro em espiral; Force Blast = leque de ondas largas; **Thunder Emperor** = o nome pede raio: descarga branco-amarelada no último golpe e o núcleo exposto marcado [A PESQUISAR a aparência na obra]. Cor (Miguel, 2026-10-22): **amarelo/laranja** (aura `isao_numbers2` mantida), com o bege das manoplas na borda |
| Baixa qualidade | anéis e a descarga final |

### Isao sem o Numbers 2 (`isao_base`)

| Item | Ficha |
|---|---|
| Proposta | socos sem energia: poeira e impacto físico; nenhuma onda. Usa o modelo do Isao (modelo próprio futuramente) |

### Soldado comum (`soldier`)

| Item | Ficha |
|---|---|
| Paleta (textura) | preto `#141414`, verde-oliva `#53574D`, branco `#ECEDEC`; sem cor saturada |
| Proposta | efeitos "de arma", sem poder: clarão do cano, cápsulas, fumaça; faca/espada com rastro cinza curto. Aura `defense_force` do jogador e do soldado: ciano claro (HUD da Força de Defesa) [SUPOSIÇÃO antiga] |

---

## Kaiju

Os kaiju dividem os mesmos arquivos de habilidade (`bite`, `slam`, `heavy_punch`...). Hoje o efeito vem da habilidade
(6 efeitos genéricos: impacto 38 usos, onda de choque 22, poeira 16, explosão de energia 7, rachadura 2, teia 2), então
a mordida da aranha e a do Honju são iguais. A proposta é o efeito vir da **espécie** (paleta + material) e da
habilidade (forma).

| Kaiju | Paleta (textura) | Destaques | Material | Assinatura proposta |
|---|---|---|---|---|
| `trichonephila` (aranha) | preto `#18181A`, oliva `#5D4E1F` | **amarelo `#EFCE0D`** | quitina | golpes das patas com lascas pretas e amarelas; teia branca (já tem); salto com poeira fina |
| `trichonephila_honju` | roxo `#6A4259`, marrom `#432D31` | **dourado `#BA901E`**, roxo `#5D325E` | quitina | explosão de teia em anel roxo-dourado; patas com lascas roxas |
| `camponotus` (formiga) | preto `#272526`, ferrugem `#623328` | **laranja `#C33917`**, âmbar `#D3A42F` | quitina | ácido âmbar-esverdeado que chia e deixa fumaça no chão [SUPOSIÇÃO a cor do ácido]; mandíbula com faísca |
| `camponotus_reborn` | grafite `#282C2E`, petróleo `#2C4E51` | **ciano `#29BCDB`** | quitina revivida | ácido ciano (da cor dela) + brilho de revivida (ver No. 9) |
| `primigenius` | branco-acinzentado `#C2BBB6` | marrom `#6C3F36` | couro | impactos de peso: poeira clara, terra, sem energia |
| `primigenius_resurrected` | verde-musgo `#769171` | bege `#A79358` | revivido | poeira + **vapor verde-acinzentado** saindo do corpo nos golpes fortes (marca de revivido) [SUPOSIÇÃO] |
| `primigenius_honju` | marrom `#5B4840` | **dourado `#D5B659`** | couro + chifres | `energy_blast`: feixe dourado-alaranjado com carga na boca; pisão com rachaduras |
| `primigenius_revived` | roxo `#5F4677` | **vermelho `#BE0C18`**, vinho `#892744` | revivido | feixe roxo com miolo vermelho; vapor roxo |
| `philinosoma` | vermelho-terra `#842D1C`, `#8D4E3F` | **azul `#3AA8A9`** (espinhos, MODELO) | escamas + espinhos | `spine_shot`: espinhos azuis com rastro; impacto com lascas azuis |
| `diclonius` | branco `#C3B5AE`, rosa `#B38288` | **vermelho `#C03B55`**, azul `#335D93` (placas) | placas | `atomic_breath`: feixe vermelho com borda azul, placas das costas acendendo em sequência na carga |
| `phaneroplasmodium` (cogumelo) | bege `#A08975`, marrom `#673F30` | vermelho `#9A1415` | fungo | esporos bege-avermelhados em nuvem lenta e baixa (não é explosão) |
| `myxogasterocarp` | roxo-acinzentado `#3A3641` | **vermelho `#B22624`** | fungo | bomba de esporos: nuvem densa roxa com pontos vermelhos que estoura |
| `kaiju_larva` | azul-ardósia `#393748` | **ciano `#36C5CA`** | larva | brilho ciano fraco (a mesma cor do No. 8: origem dele) [SUPOSIÇÃO] |
| `preondactyl` | azul-ardósia `#39394B` | bege `#A57A51` | pele/asas | raio: feixe bege-alaranjado [SUPOSIÇÃO]; mergulho com rastro de vento; autodestruição com carga crescente |
| `kaiju_no9` | bege-acinzentado `#B7AEA4` | nenhum | — | Finger Gun: tiro fino e rápido, branco; reviver: fios escuros puxando a carcaça [SUPOSIÇÃO]; fuga: dissolve em fumaça [A PESQUISAR] |
| `kaiju_no9_black` | preto `#151515` | **vermelho-escuro `#90222A`** | — | Multi-Finger Gun em leque vermelho-escuro; braço-lâmina com rastro vermelho; tentáculos pretos; pele endurecida com brilho vermelho |
| `kaiju_no9_fusion` (+ gigante) | **vermelho `#830D1F`** | vinho `#85353E` | fusão | raio híbrido vermelho com faixa bege do No. 9; rajada da cauda |
| `kaiju_no9_camponotus` | preto `#1F1C20` | **vermelho `#B12F1E`**, âmbar `#C28B35` | quitina | ácido da formiga com a cor do No. 9 |
| `kaiju_no9_isao` | preto `#110F0F` | vermelho `#9D1D1D` | — | as ondas das manoplas do Isao em preto e vermelho (ele as roubou) |
| `kaiju_no10_small` / `giant` | **vermelho `#A70D19`, `#BE1425`** | rosa-arroxeado `#AB3A81` | — | Finger Cannon vermelho; golpes de cauda com rastro vermelho; virar gigante: crescimento com vapor vermelho |
| `kaiju_no13` | branco-acinzentado `#CBC8C6` | **vermelho `#C32D22`** | — | velocidade: rastros curtos atrás do corpo na investida; agarrão com estalo |

---

## Regra de tamanho (Miguel, 2026-10-22)

"Os ataques devem seguir conforme o tamanho do personagem: um kaiju grande nao deve ter ataques pequenos ou com
efeitos pequenos." Todas as receitas sao escritas para um corpo humano (1,8 m) e o jogo as aumenta pela altura de
quem faz o efeito (VFX-2: tamanho x fator^0,75, alcance x fator^0,85, quantidade x fator^0,5; config `[fx]`).

## Feito na VFX-3 (Hoshina e Hoshina No. 10)

Receitas em `vfx_profile/hoshina.json` e `hoshina_no10.json`: um corte por golpe (`tecnica/n`), linha fina preta com
fio violeta em cada inclinação da forma (Kuuchi, Kōsa-uchi, Ran-uchi, Kasumi-uchi, Yae-uchi, Kaeshi-uchi), contato
com lascas violeta e traço branco; Jūni-hitoe com raios `#9A2A5A` e cruz final; cauda, guarda da cauda e Full Release
vermelhos; aura do No. 10 `hoshina_no10_sync`. Kafka: aura `kafka_gray` (decisão do Miguel, 2026-10-22). Os cortes
saem do ponto de contato no alvo (origem nas lâminas pelas marcas da animação: [SUPOSIÇÃO] fica para quando a
posição do osso for lida).

## Feito na VFX-2 (Kafka e Kaiju No. 8)

Receitas em `vfx_profile/kaiju_no8.json` e `kafka.json`, pela ficha acima: transformação (as duas direções), socos,
soco pesado, investida, contra-ataque, golpe no chão, rugido, regeneração, rastro do punho (marcas nas animações) e
o tiro/soco do Kafka. Aura do No. 8 mais rente (`size` 1,0). [DECIDIR] Kafka sem aura até perto da transformação.

## Perfis no jogo (VFX-1)

Cada ficha virou `data/kn8/kn8/vfx_profile/<id>.json` (`primary`/`secondary`/`accent`/`core` + `material`). Escolhas
desta etapa [SUPOSIÇÃO, a confirmar pelo Miguel]: Mina destaque `#3FCF5A` (verde), Narumi `#FF6FB0` (rosa), Narumi
No. 1 `#FF8CC0` com miolo branco, Isao `#FFA62B` (laranja) e miolo `#FFE07A`, Hoshina e Hoshina No. 10 `#8A4FC0`
(violeta) e miolo do No. 10 `#9A2A5A` (raios), Reno `#A8E4FF`/No. 6 `#8FD8FF` (gelo), Preondactyl `#E2551E`
(raio laranja/vermelho), No. 9 `#9A9A9A` (Finger Gun cinza), soldado e jogador com o clarão do cano `#FFD27A`.
Materiais: aranhas e formigas `chitin`, cogumelos `fungus`, ressurgido/revivido `revived`, larva e No. 8 `energy`,
Reno `ice`, Philinosoma/Diclonius `scales`, soldados `suit`, o resto `flesh`.

## Decisões e incertezas

**Decidido (Miguel, 2026-10-22):** Narumi rosa; Isao amarelo/laranja; **Mina verde**; Hoshina continua violeta e, com
o Numbers 10, raios saindo do traje em vermelho puxado para o roxo.

**Aparências decididas (Miguel, 2026-10-22):**
1. **Thunder Emperor:** um soco muito forte que reúne toda a força do traje do Numbers 2 num golpe só: carga visível
   nas manoplas (energia amarela/laranja se juntando no punho) e um impacto único e enorme (sem raio elétrico).
2. **Finger Gun do No. 9:** tiros **cinza**, como balas em velocidade extrema: risco fino e reto, quase instantâneo,
   com o estalo no impacto (vale para o Multi-Finger Gun das formas).
3. **Fuga do No. 9:** ele **tenta se retirar** de algum jeito; se não conseguir, morre (mudança de jogo, não só visual:
   ver a etapa proposta "Retirada do No. 9").
4. **Raio do Preondactyl:** laranja/vermelho.
5. **Ácido da formiga:** verde.

**Ainda a definir:** reviver do No. 9 (carcaça voltando).
