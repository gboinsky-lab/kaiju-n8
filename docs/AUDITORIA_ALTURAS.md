# Auditoria de alturas — tabela mestra v2.0 × jogo (2026-10-10)

Pedido: `docs/TABELA_MESTRA_ALTURAS_v2_0.md` (cópia do arquivo do Miguel). Seguindo o item 1 da tabela, esta é a
**auditoria antes de mudar qualquer coisa**. Nenhum modelo, hitbox ou número foi alterado nesta etapa.

## Como foi medido

- **Altura visual**: medida direto nas malhas que o jogo desenha (`assets/kn8/meshes/<espécie>/*.obj`, vértices em
  metros, 1 m = 1 bloco, pés em y = 0). Nenhum renderer aplica escala extra nas entidades (só no item na mão), então a
  malha é exatamente o que aparece no jogo. A escala vem do `height_m`/`length_m` de cada modelo em
  `tools/art/meshy_assets.json`, aplicada pelos scripts de rig.
- **Hitbox**: `.sized(largura, altura)` em `KN8Entities.java`.
- Asas medidas à parte (a tabela pede altura **corporal**).

## Personagens

| Entidade (id) | Altura atual (modelo) | Hitbox | Alvo da tabela | Diferença | Situação |
|---|---:|---:|---:|---:|---|
| Jogador | 1,80 (vanilla) | 0,6 × 1,8 | 1,80 | 0 | ok |
| Kafka (`kafka`) | 1,81 | 1,81 | 1,81 | 0 | ok |
| Kaiju No. 8 (`kaiju_no8`) | 2,00 | 2,0 | 2,00 | 0 | ok |
| Narumi e Narumi No. 1 (`narumi`, `narumi_no1`) | 1,78 | 1,78 | 1,78 | 0 | ok |
| Hoshina e Hoshina No. 10 (`hoshina`, `hoshina_no10`) | 1,85 | 1,85 | 1,72 | −0,13 | proposta |
| Mina (`mina`) | 1,65 | 1,65 | 1,75 | +0,10 | proposta |
| Reno e Reno No. 6 (`reno`, `reno_no6`) | 1,70 | 1,70 | 1,75 | +0,05 | proposta |
| Kikoru e Kikoru No. 4 (`kikoru`, `kikoru_no4`) | 1,57 (com asas: 1,68) | 1,57 | 1,60 | +0,03 | **conflito**: 1,57 m foi decisão sua em 2026-10-09 |
| Soldado comum (`soldier`, todos os níveis) | 1,90 (um modelo só) | 1,9 | recruta 1,70 · normal 1,75 · alto 1,80 · elite 1,85 · pesado 1,90 | −0,20 a 0 | proposta (precisa de escala por nível) |
| Isao, Iharu, Haruichi, Aoi, Eiji, Rin, Kota, Jugo, Toko, Soichiro, Hikari, Jura, Akari, Hakua, Ryo, Tae, Konomi, Keiji, Juzo, Akira | — | — | 1,60–1,90 | — | **não existem no jogo** (sem modelo) |

## Kaiju comuns

| Entidade (id) | Altura atual | Largura × comprimento | Hitbox (L × A) | Alvo | Diferença | Situação |
|---|---:|---:|---:|---:|---:|---|
| Trichonephila (`trichonephila`) | 1,68 | 5,50 × 5,25 | 3,4 × 1,8 | 4,5 | +2,82 (×2,7) | **conflito**: a aranha é baixa e larga; 4,5 parece ser comprimento, não altura (a própria tabela pede medir antes) |
| Trichonephila Honju (`trichonephila_honju`) | 4,12 | 6,78 × 8,00 | 6,0 × 4,0 | 9 | +4,88 (×2,2) | **conflito**: mesmo caso (comprimento 8,0) |
| Primigenius Yoju (`primigenius`) | 6,00 | 4,44 × 11,01 | 3,13 × 6,0 | 5,5 | −0,50 | proposta |
| Primigenius ressurgido (`primigenius_resurrected`) | 6,00 | 4,29 × 9,54 | 3,13 × 6,0 | 6 | 0 | ok |
| Primigenius Honju (`primigenius_honju`) | 9,00 | 7,85 × 13,52 | 5,73 × 9,0 | 8,5 | −0,50 | proposta |
| Primigenius revivido (`primigenius_revived`) | 9,00 | 7,17 × 11,79 | 5,73 × 9,0 | 9 | 0 | ok |
| Preondactyl (`preondactyl`) | 4,64 sem asas (5,00 com asas; envergadura 8,28) | 8,28 × 6,22 | 4,0 × 4,0 | 7 (corpo) | +2,36 (×1,5) | proposta |
| Philinosoma (`philinosoma`, Honju) | 9,00 | 7,42 × 14,10 | 6,0 × 9,0 | 6–15 | — | dentro da faixa |
| Diclonius (`diclonius`, Honju) | 9,00 | 6,77 × 14,63 | 5,0 × 9,0 | 6–15 | — | dentro da faixa |
| Myxogasterocarp (`myxogasterocarp`, Honju) | 9,00 | 8,06 × 9,57 | 7,0 × 9,0 | 6–15 | — | dentro da faixa |
| Phaneroplasmodium (`phaneroplasmodium`, Yoju) | 5,00 | 4,68 × 4,10 | 4,4 × 5,0 | 4–8 | — | dentro da faixa |
| Camponotus e revivida (`camponotus`, `camponotus_reborn`, Yoju) | 2,88 | 3,08 × 6,00 | 4,0 × 2,9 | 4–8 | abaixo da faixa em altura (6 de comprimento) | pendente: formiga é baixa e comprida, como a aranha |
| Larva (`kaiju_larva`) | 0,50 | 0,41 × 0,60 | 0,5 × 0,5 | — | — | exceção aprovada pela regra 7.1 (larva) |

## Numerados e formas

| Entidade (id) | Altura atual | Hitbox | Alvo | Diferença | Situação |
|---|---:|---:|---:|---:|---|
| No. 10 pequeno (`kaiju_no10_small`) | 4,00 | 2,0 × 4,0 | 4 | 0 | ok |
| No. 10 gigante (`kaiju_no10_giant`) | 24,00 | 10 × 24 | 24 | 0 | ok |
| **Fusão No. 9 + No. 10, forma pequena** (`kaiju_no9_fusion`) | **5,00** | 2,0 × 5,0 | **5** | 0 | **ok** (já está no valor que você pediu) |
| **Fusão No. 9 + No. 10, forma grande** | — | — | **20** | — | **não existe no jogo**: hoje só há uma fusão (a de 5 m). Criar a forma grande é conteúdo novo |
| No. 9 base (`kaiju_no9`) | 2,00 | 0,8 × 2,0 | 8 | +6 (×4) | **conflito**: em 2026-10-06 você decidiu o No. 9 humanoide de ~2 m (como no anime); a própria tabela diz "confirmar se era altura visual" |
| No. 9 forma preta (`kaiju_no9_black`) | 1,90 | 0,8 × 1,9 | 10 | +8,1 (×5,3) | **conflito**: modelo humanoide de 1,9 m |
| No. 9 fundido à formiga (`kaiju_no9_camponotus`) | 3,91 (comprimento 6,0) | 4,0 × 3,9 | 11 | +7,1 (×2,8) | conflito de escala com as outras formas (o torso do No. 9 em cima ficaria com ~5 m) |
| No. 9 vermelho, após Isao/No. 2, forma final | — | — | 22 / 25 / 28 | — | não existem (sem modelo) |
| No. 1, 2, 6 (kaiju) | — | — | 20 / 25 / 22 | — | não existem (o No. 1 e o No. 6 só como armas do Narumi e do Reno) |
| No. 8 colossal | — | — | 22 | — | não existe (só se for variante separada, como a tabela diz) |
| No. 3, 4, 5, 7, 11–15 | — | — | pendente | — | não existem |

## Resumo

- **Já batem com a tabela (11):** jogador, Kafka, No. 8, Narumi (2), Primigenius ressurgido e revivido, No. 10
  pequeno e gigante, e a **fusão pequena com 5 blocos** (o valor que você pediu).
- **Diferença pequena, proposta (6 entidades, 4 linhas):** Hoshina (2) −0,13, Mina +0,10, Reno (2) +0,05,
  Primigenius Yoju −0,5, Primigenius Honju −0,5, Preondactyl +2,4 no corpo.
- **Conflitos que precisam da sua decisão:** No. 9 base 2 → 8, forma preta 1,9 → 10, forma formiga 3,9 → 11, aranha
  1,7 → 4,5 e Tecedeira 4,1 → 9 (parece comprimento), Kikoru 1,57 → 1,60 (sua decisão de 1,57), soldados por nível.
- **Não existe no jogo:** a fusão grande de 20 blocos e os personagens/formas sem modelo.

## Método proposto para mudar a altura (quando aprovado)

Uma **escala por entidade num JSON** (`visual_scale`), aplicada no renderer e na hitbox juntas, sem refazer o rig:
ossos, pivôs, UVs, texturas, animações e pegada das armas ficam iguais (a escala multiplica tudo por igual). Testar
primeiro em uma entidade (ex.: Primigenius Yoju 6,0 → 5,5), conferir no jogo (altura, chão, hitbox, golpes) e só
depois aplicar nas outras. Vida, dano, IA e velocidade não mudam; o alcance dos golpes de kaiju é medido entre bordas
da hitbox, então muda um pouco junto com o tamanho (efeito físico do tamanho, não balanceamento).
