# VFX-0 — Auditoria dos efeitos e plano (diretriz KaijuVFX, 2026-10-22)

Etapa 0 da `docs/DIRETRIZ_KAIJU_VFX.md` (§5: inspecionar, pesquisar, definir). Nenhum efeito mudou nesta etapa. As
fichas por personagem estão em `docs/VFX_FICHAS_IDENTIDADE.md`.

## 1. O que existe hoje

| Peça | Arquivo | O que faz |
|---|---|---|
| Serviço central | `common/vfx/VfxService` | o servidor decide quando e onde; manda `VfxS2C` (id do efeito, posição, direção, intensidade, tremor) a quem está a até 64 blocos. Já é um evento compacto por efeito (diretriz §4 ok) |
| Efeitos do cliente | `client/vfx/VfxEffects` | 12 efeitos feitos de partículas vanilla: `impact`, `shockwave`, `dust`, `slash`, `weapon_fire`, `roar`, `suit_release`, `overheat`, `ground_crack`, `energy_charge`, `energy_burst`, `web` |
| Efeitos de habilidade de kaiju | `common/vfx/AbilityEffects` + `vfx` no `ability/*.json` | no tick do impacto (mesmo tick do dano); efeito + intensidade + tremor + som |
| Aura | `client/vfx/AuraRenderer` + `aura/*.json` (12) | cada cliente desenha a aura todo tick pelo valor público do Release; 3 estilos (`sparks`, `lightning`, `flame`), cor e cor secundária por personagem |
| Cortes e tiros dos especiais | `SlashProjectile` + `slash.color` nas técnicas | rastro em `DustParticleOptions` da cor da técnica |
| Projétil de kaiju | `KaijuProjectile` + `behavior.color` | rastro de pó da cor + `END_ROD` |
| Aviso de golpe pesado | `KaijuTelegraph` | anel/arco/linha de pó amarelo → vermelho |
| Qualidade | `[fx] particleLevel` (cliente) | baixo/médio/alto = 35/70/100% da quantidade |
| Tremor | `[fx] cameraShake` (cliente), `CameraShake` | 0 desliga |
| Liga/desliga | `[vfx] enabled` (servidor) | sem efeitos (o dano não muda: é só visual) |

## 2. O que a diretriz pede e falta

1. **Efeito genérico com troca de cor (§1, §3).** É o caso de hoje: os 23 kaiju usam os mesmos 6 efeitos (impacto 38
   vezes, onda de choque 22, poeira 16, explosão de energia 7) e a mordida da aranha é igual à do Honju; os
   personagens diferem só pela cor da aura e do rastro do corte.
2. **Ficha de identidade visual (§2):** feita nesta etapa para os 13 perfis e os 23 kaiju.
3. **Partículas com textura própria (§4):** só partículas vanilla e `DustParticleOptions`; nenhuma silhueta própria
   (arco de corte, anel, cristal, linha de vento, espinho).
4. **Sincronia com a animação pela GeckoLib (§4):** o impacto já sai no tick do dano do JSON (que é a marca da
   animação), mas nada visual acompanha o golpe *durante* a animação (rastro da lâmina, do punho, da asa). Conferido no
   jar da GeckoLib 4.7.5: `AnimationController.setParticleKeyframeHandler` e `setCustomInstructionKeyframeHandler`
   existem — dá para pôr eventos nas animações e soltar rastros no quadro certo, só no cliente.
5. **Efeito de origem certa (§2):** hoje quase tudo sai do centro do alvo ou da "boca" calculada (frente do corpo a
   3/4 da altura); não sai da mão, da lâmina, do cano, dos olhos.
6. **Regeneração/transformação (§3):** No. 9 e No. 10 regeneram com partículas verdes vanilla (`HAPPY_VILLAGER`); a
   troca Kafka → No. 8 e as mutações do No. 9 usam fumaça e explosão genéricas.
7. **Desempenho e acessibilidade (§6):** falta um teto de partículas por efeito e por quadro, distância de desenho
   configurável e uma opção para reduzir clarões.
8. **Duração e limpeza (§6):** os efeitos atuais são todos de um quadro (partículas soltas que morrem sozinhas): ok.
   Os efeitos novos que durarem (rastro, carga, campo de gelo) precisam terminar com a habilidade.

## 3. Arquitetura proposta (VFX-1)

- **Perfil visual por personagem/espécie:** `vfx_profile/<id>.json` (id = perfil de soldado especial ou espécie de
  kaiju; registro de dados sincronizado, como `locomotion`): paleta (`primary`, `secondary`, `accent`, `core`),
  material (`flesh`, `chitin`, `fungus`, `ice`, `energy`...) e, por habilidade ou técnica, a **receita** do efeito:
  camadas (forma `arc`/`ring`/`burst`/`streak`/`beam`/`spiral`/`cloud`/`shards`, cor da paleta, quantidade, tamanho,
  velocidade, duração, origem). Sem receita, cai no efeito genérico de hoje com a paleta (nada some).
- **Evento compacto:** o `VfxS2C` ganha o id do perfil e o da habilidade (o cliente monta tudo a partir dos dados que
  já tem); protocolo 19 → 20.
- **Partículas próprias:** um `ParticleType` com opções (cor, tamanho, vida) e texturas desenhadas para as silhuetas:
  `kn8:arc` (pedaço de arco de corte), `kn8:ring` (anel plano que cresce), `kn8:shard` (lasca/cristal), `kn8:streak`
  (risco de vento/velocidade), `kn8:glow` (ponto de energia), `kn8:spore`. Tingidas pela paleta: a silhueta é a mesma
  peça, mas cada personagem usa outra combinação, ritmo e tamanho (a diretriz §3 pede diferença além da cor).
- **Rastros pela animação:** eventos de partícula/instrução nas animações GeckoLib (geradores `gen_*_animations.py`
  escrevem os keyframes `particle_effects`/`instructions`), tratados no cliente pelo `setParticleKeyframeHandler`;
  origem no osso (`item_right`, `item_left`, mão, asa, boca).
- **Qualidade:** `[fx] particleLevel` continua; novos `[fx] maxParticlesPerEffect`, `[fx] effectDistance` e
  `[fx] reduceFlashes` (clarões menores e sem branco puro).
- **Servidor intocado:** dano, colisão e execução continuam no servidor (regra 1 e 6); desligar os efeitos não muda o
  combate (GameTest).

## 4. Plano por etapas (uma de cada vez, com aprovação entre elas)

| Etapa | Conteúdo |
|---|---|
| **VFX-1 Base** | perfil visual por dados, evento compacto (protocolo 20), partículas próprias com texturas, eventos de keyframe da GeckoLib, níveis de qualidade e limites; os efeitos de hoje passam a ler a paleta da espécie (primeiro ganho para os 23 kaiju) |
| **VFX-2 Kafka e Kaiju No. 8** | transformação, socos, golpe no chão, rugido, regeneração |
| **VFX-3 Hoshina e Hoshina No. 10** | arcos de cada forma, cauda, Full Release, Jūni-hitoe |
| **VFX-4 Kikoru e Kikoru No. 4** | arcos pesados, onda no chão, cratera, vento das asas, mergulho |
| **VFX-5 Mina, Reno e Reno No. 6** | canhão (carga, disparo, explosão), munição congelante, lança/campo/explosão de gelo |
| **VFX-6 Narumi, Narumi No. 1 e Isao** | estocadas, previsão, manoplas, Thunder Emperor |
| **VFX-7 No. 9 e formas** | Finger Gun, leques, tentáculos, pele endurecida, reviver, absorção, fuga, regeneração |
| **VFX-8 No. 10, No. 13, Preondactyl, larva** | Finger Cannon, cauda, crescimento, investida, raio, mergulho, autodestruição |
| **VFX-9 Demais kaiju** | Primigenius (4), aranhas (2), formigas (2), cogumelos (2), Philinosoma, Diclonius |
| **VFX-10 Regressão** | vários personagens ao mesmo tempo, FPS e memória com 2 clientes, níveis de qualidade, efeitos desligados com o dano igual |

## 5. VFX-1 feita (2026-10-22)

- `vfx_profile/<id>.json` para os 23 kaiju, os 13 perfis de soldado especial, `soldier` e `player` (paletas das
  fichas; Mina verde `#3FCF5A`, Narumi rosa `#FF6FB0`, Isao laranja `#FFA62B`, Hoshina violeta `#8A4FC0`, Hoshina
  No. 10 com miolo `#9A2A5A` para os raios [SUPOSIÇÃO o tom]).
- Partículas próprias com textura (`tools/art/gen_vfx_particles.py`), evento com o perfil (protocolo 20), camada
  da paleta e do material sobre os 12 efeitos genéricos, receitas por habilidade (substituem o genérico), marcas
  `particle_effects` das animações (`tools/art/vfx_keyframes.py`), teto/distância/clarões no `[fx]`.
- Comando de prévia `/kn8 vfx play <efeito> <perfil> [intensidade] [receita]`.
- **Limitações:** a origem das marcas de animação ainda é aproximada pela caixa do corpo (a posição real do osso
  entra na VFX-2); só uma receita de demonstração (presas da aranha); os efeitos de cada personagem/kaiju são das
  etapas VFX-2 a VFX-9. Testado em jogo: o mesmo impacto/onda/explosão em perfis diferentes, nos dois clientes.

## 6. Raios em todos os efeitos e auras (Miguel, 2026-10-22)

"Lembre de mudar todos os efeitos inclusive a aura dos trajes e personagens para sair raios e etc igual no anime."
Vale para as etapas que faltam e para as já feitas: a aura de cada traje e personagem passa a soltar descargas/raios
saindo do corpo (na cor dele, mais fortes com o Release, maiores com o corpo), e os golpes ganham raios onde fizer
sentido. Proposta de ordem:

| Etapa | Conteúdo |
|---|---|
| **VFX-4A Auras com raios (todos)** | aura nova por traje/personagem com descargas ramificadas saindo do corpo (raio que sai e volta ao corpo, faíscas nas pontas), linhas do traje acendendo com o Release; jogador e soldado (traje da Força de Defesa), Kaiju No. 8 (raios azuis/ciano, confirmados pela descrição de figura oficial), Hoshina, Hoshina No. 10, Kikoru, Kikoru No. 4, Mina, Reno, Reno No. 6, Narumi, Narumi No. 1, Isao; Kafka com faíscas cinza pequenas (decisão do Miguel) |
| **VFX-4B Kikoru e Kikoru No. 4** | golpes do machado com raios dourados, onda no chão, cratera, vento das asas, mergulho |
| VFX-5 a VFX-10 | como antes, cada golpe com os raios do personagem |

Pesquisa: as fontes abertas só descrevem os raios azuis do Kaiju No. 8 ao liberar poder; os trajes dos outros
personagens seguem a referência do Miguel [A PESQUISAR o desenho de cada um no anime].

**VFX-4A feita (2026-10-22):** as 14 auras com o bloco `bolts` (ver CLAUDE.md). Próxima: VFX-4B (Kikoru e Kikoru No. 4).

## 7. Sem partículas: efeitos desenhados (Miguel, 2026-10-22)

"Não quero que nenhum tenha partículas; quero que todos sejam mudados para efeitos especiais como raios ou aura."
Todos os efeitos passam a ser formas desenhadas pelo cliente (sem pontos soltos), com tipos de render que o próprio
Minecraft 1.21.1 tem (conferidos no jar): `lightning()` (o do raio vanilla: faixas coloridas brilhantes),
`energySwirl` (o brilho do creeper carregado, desenhado por cima do modelo), `beaconBeam` (feixe), `eyes` (brilho).

| Etapa | Conteúdo |
|---|---|
| **VFX-R1 Base desenhada** | renderizador próprio de efeitos (`client/vfx/draw`): raio contínuo ramificado, faixa de corte (arco sólido que some), anel/onda plana, feixe, pulso esférico, brilho no modelo; tudo com duração, cor da paleta, tamanho pelo corpo e teto por quadro; o evento de rede continua o mesmo |
| **VFX-R2 Converter o que já existe** | auras (brilho no modelo + raios contínuos), cortes do Hoshina, socos/rugido/transformação do No. 8, impactos/ondas/explosões de todos os kaiju, aviso do golpe pesado, rastros de tiros e cortes (`SlashProjectile`, `KaijuProjectile`), regeneração |
| VFX-4B a VFX-10 | já no formato desenhado |

**Meio-termo (Miguel, 2026-10-22: "faça um meio termo então"):** tudo o que é efeito vira forma desenhada, inclusive
fumaça e clarão de explosão (desenhados na VFX-R2); só os pedaços de bloco da destruição continuam como partículas
vanilla (são o bloco caindo, não um efeito).

**Progresso:** VFX-R1 feita (2026-10-22): `client/vfx/draw` (`Drawn` com as formas `bolt` raio ramificado, `band`
arco de corte com miolo escuro e borda brilhante, `wave` anel no chão que cresce, `pulse` anel virado para a câmera,
`beam` feixe/tiro em movimento, `flash` clarão, `smoke` fumaça desenhada), tipos de render `KN8RenderTypes.GLOW`
(shader e mistura do raio vanilla, soma ao fundo) e `SHADE` (transparência comum), desenho depois das partículas em
duas passadas; geometria pura em `core/vfx/DrawMath` (JUnit). As receitas do `vfx_profile` aceitam essas formas como
`shape` (com `edge`, a cor da borda); a escala pelo corpo (VFX-2) vale igual. Teto `[fx] maxDrawnEffects` 400. Demo:
`/kn8 vfx play kn8:impact kn8:demo 1 <forma>`. Os efeitos antigos ainda são partículas até a VFX-R2.

VFX-R2 feita (2026-10-22): tudo o que era partícula é forma desenhada. As camadas antigas das receitas são traduzidas
(`DrawnFactory.fromParticle`), os efeitos comuns, a aura, os projéteis (renderizadores próprios), o aviso do golpe
pesado (área inteira no chão) e os efeitos que o servidor mandava como partícula (No. 9, No. 10, chefe, arena,
Preondactyl, aparar, tiro do jogador) passam pelo `VfxService` e são desenhados. Ficam do vanilla só os pedaços de
bloco (passo, rachadura) e da carcaça no desmonte. Próximo: VFX-4B (Kikoru) a VFX-10 já no formato desenhado.

VFX-4B feita (2026-10-22): Kikoru e Kikoru No. 4 (ver a ficha). Próxima: VFX-5.

VFX-5 feita (2026-10-22): Mina, Reno e Reno No. 6 (ver as fichas); o tiro da técnica leva a chave dela (receitas `_charge`, `_trail`, `_impact`, `_explosion`). Próxima: VFX-6 (Narumi, Narumi No. 1, Isao).

VFX-6 feita (2026-10-22): Narumi, Narumi No. 1, Isao e Isao sem o Numbers 2 (ver as fichas). Próxima: VFX-7 (formas do No. 9).

VFX-7 feita (2026-10-22): No. 9 e as 5 formas (ver as fichas). Próxima: VFX-8 (No. 10, No. 13, Preondactyl, larva).

VFX-8 feita (2026-10-22): No. 10, No. 13, Preondactyl e larva (ver as fichas). Próxima: VFX-9 (outros kaiju).

Decisões de 2026-10-23 (Miguel): reviver do No. 9 fica como está; **nada mais fica vanilla** (pedaços de bloco da destruição, poeira dos passos e pedaços do desmonte viraram `kn8:debris` desenhado); jogador com aura sorteada (`aura/player_*.json`). Tamanhos e tempos: o Miguel manda depois.
