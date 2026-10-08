# Auditoria de texturas e materiais (0.5.0-C2, Biblioteca v22 Prioridade 2)

Feita em 2026-10-08 sobre tudo que tem textura no mod: 13 entidades com malha do Meshy, 2 trajes 3D, 6 armas OBJ e
os modelos comuns de item/bloco. Refazer depois de trocar qualquer modelo:
`python3 tools/art/audit_textures.py docs/AUDITORIA_TEXTURAS.md` (reescreve este arquivo so com a tabela; manter as
conclusoes abaixo).

## O que o script confere

- arquivos: cada OBJ citado nos `meshes/*.json`, a textura de cada entidade/traje/arma e as texturas dos modelos de
  item e bloco existem; todo osso da malha existe no `.geo.json` (senao a malha nao aparece);
- UV fora de [0, 1] (com a textura repetindo, puxa pixels do lado oposto);
- "sem textura": face que amostra pixel transparente (buraco) e magenta puro (textura de erro);
- UV esticada que atravessa ilhas da textura (a cor muda mais de 60 dentro do triangulo): o defeito das "linhas
  douradas" da aranha na 0.1-B;
- arestas abertas (so informativo, ver abaixo).

## Resultado

| Asset | Triangulos | UV fora de [0,1] | Sem textura | Magenta | UV esticada | Arestas abertas | Textura |
|---|---|---|---|---|---|---|---|
| hoshina | 9352 | 0 | 0 | 0 | 0 | 33 | 1024x1024 |
| hoshina_no10 | 13095 | 0 | 0 | 0 | 0 | 23 | 1024x1024 |
| kaiju_no10_giant | 19679 | 0 | 0 | 0 | 0 | 86 | 1024x1024 |
| kaiju_no10_small | 14239 | 0 | 0 | 0 | 0 | 45 | 1024x1024 |
| kaiju_no9 | 7946 | 0 | 0 | 0 | 0 | 28 | 1024x1024 |
| preondactyl | 14383 | 0 | 0 | 0 | 0 | 110 | 1024x1024 |
| primigenius | 10772 | 0 | 0 | 0 | 0 | 19 | 1024x1024 |
| primigenius_honju | 12576 | 0 | 0 | 0 | 0 | 45 | 1024x1024 |
| primigenius_resurrected | 10138 | 0 | 0 | 0 | 0 | 40 | 1024x1024 |
| primigenius_revived | 13148 | 0 | 0 | 0 | 0 | 43 | 1024x1024 |
| soldier | 10003 | 0 | 0 | 0 | 0 | 33 | 1024x1024 |
| trichonephila | 8582 | 0 | 0 | 0 | 0 | 145 | 1024x1024 |
| trichonephila_honju | 12700 | 0 | 0 | 0 | 0 | 68 | 1024x1024 |
| suit/mk1 | 8531 | 0 | 0 | 0 | 0 | 26 | 1024x1024 |
| suit/mk1_reinforced | 5948 | 0 | 0 | 0 | 0 | 36 | 1024x1024 |
| item/axe | 3979 | 0 | 0 | 0 | 0 | 9 | obj |
| item/combat_knife | 1246 | 0 | 0 | 0 | 0 | 4 | obj |
| item/hoshina_sword | 3076 | 0 | 0 | 0 | 0 | 0 | obj |
| item/pistol | 1566 | 0 | 0 | 0 | 0 | 2 | obj |
| item/rifle | 1512 | 0 | 0 | 0 | 0 | 27 | obj |
| item/sword | 2904 | 0 | 0 | 0 | 0 | 4 | obj |

Nenhum problema encontrado.

- **Corrigido:** o No. 10 pequeno tinha 20 triangulos com UV ate 0,2% fora de [0, 1] (vinha assim do Meshy). Os OBJs
  foram presos em [0, 1] e o `meshy_convert.py` passou a prender sempre.
- **Arestas abertas nao sao buracos visiveis:** a GeckoLib desenha as malhas com `entityCutoutNoCull` (conferido no
  `GeoModel#getRenderType` da 4.7.5) e o `SuitLayer` tambem usa `entityCutoutNoCull`, entao uma borda aberta mostra o
  lado de dentro com textura. Sao espinhos e asas feitos como laminas pelo Meshy e as bordas do corte entre ossos
  que o `cap_holes` dos scripts de rig nao fecha por nao formarem contorno fechado.
- **As texturas nao tem area vazia:** o `pad_texture.py` preenche todo pixel fora das ilhas com a cor da ilha mais
  perto, entao nenhuma face cai num "fundo" sem cor.

## Teste em jogo

Servidor dedicado + cliente (2026-10-08), cada entidade parada de frente e de costas, andando ate um alvo,
atacando e tomando dano (o vermelho do ultimo quadro e o flash de dano do vanilla). Nenhuma parte sem textura,
branca, preta, roxa ou com textura de outro modelo:

- `docs/img/auditoria_texturas_A.jpg`: soldado, Hoshina, Hoshina + No. 10, Kaiju No. 9, Trichonephila;
- `docs/img/auditoria_texturas_B.jpg`: Primigenius, ressurgido, Honju, revivido;
- `docs/img/auditoria_texturas_C.jpg`: Tecedeira Abissal, Preondactyl, No. 10 pequeno e gigante.

Trajes (Mk1 e Mk1 Reforcado) e armas na mao foram vistos na 0.5.0-B e na 0.5.0-D (posturas). Ficam para o teste do
Miguel: transformacao do No. 10 em jogo, carcacas tombadas e voo do Preondactyl de perto.
