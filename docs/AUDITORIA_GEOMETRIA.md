# Auditoria de geometria de todos os modelos (FASE 2-C, 2026-11-11)

Feita por `tools/art/mesh_audit.py` (so le os assets; regerar a tabela com
`python3 tools/art/mesh_audit.py --md docs/AUDITORIA_GEOMETRIA.md`, e acrescentar este cabecalho de novo).
Mede as 70 malhas do jogo (kaiju, personagens, trajes e acessorios), osso por osso:

- **Degenerados:** triangulos de area zero. Nao aparecem na tela; ficam (Tae 132, Maxilla 64, Kota 14).
- **Postes:** triangulo com a aresta 12x maior que a mediana da malha e acima de 0,5 m. Sao tampas da conversao
  (`cap_holes`) esticadas pelo corpo.
- **UV fora:** coordenada de textura fora de [0, 1]. Nenhum modelo tem.
- **Pedacos soltos:** pedaco ligado com menos de 24 triangulos a mais de 8 cm de qualquer outra malha do mesmo osso.
- **Textura:** a textura do modelo existe. Todas existem.

## O que foi corrigido

| Defeito | Modelos | Correcao |
|---|---|---|
| Poste visivel (lasca que atravessa o corpo) | Philinosoma (tronco, 6,8 m), Preondactyl (asa esquerda, 2,5 m), Primigenius revivido (cabeca, 2,7 m), Haruichi (coxa direita, 0,8 m ate o chao) | `mesh_audit.py --fix-poles`: so as faces sao apagadas, sem buraco visivel na previa |
| Pedacos do tronco no osso da perna/braco: rabo de cavalo e tiras do casaco do Soichiro, cinto/barra da jaqueta e pedacos acima do quadril | Soichiro, Hakua, Kota, Aoi, Reno, No. 1, No. 15 na forma Kikoru, No. 9 Neural (braco) | `tools/art/rig_strays.py`: o pedaco passa para o osso do tronco com a malha mais perto |

## O que ficou de proposito

- **Postes escondidos:** o Myxogasterocarp tem 8 postes dentro do chapeu, que nao aparecem na previa. Konomi, Rin,
  No. 9 Neural e Mk1 tem tampas planas de 0,5 a 0,8 m (alto da cabeca, gola) no lugar certo.
- **Pedacos soltos dos modelos de blocos:** No. 2, Polyonax, Imitatio, Lacerta, Anguilla e No. 6 sao feitos de
  blocos separados por 8 a 18 cm, e isso faz parte do desenho do modelo.
- **Juntas:** joelho e quadril abrem de 3,6 a 7,6 px correndo (Haruichi 7,6, Soichiro 7,4, Reno 6,2, Isao 6,0).
  Isso e normal em peca rigida da GeckoLib. A tampa fecha o corte, entao nao fica buraco aberto.
- **Casaco longo do Haruichi:** as abas de tras estao nos ossos das coxas e giram com elas ao correr, formando
  lascas na previa. Mudar isso exige uma decisao de desenho (ver [DECIDIR] no `docs/AUDITORIA_MESTRE.md` secao 11).

## Tabela

| Modelo | Triangulos | Degenerados | Postes | UV fora | Pedacos soltos | Textura | Osso pior |
|---|---|---|---|---|---|---|---|
| anguilla_ambulans | 15326 | 0 | 0 | 0 | 24 | sim | tail_4 |
| aoi | 11212 | 0 | 0 | 0 | 0 | sim | - |
| camilly | 12075 | 0 | 0 | 0 | 0 | sim | - |
| camponotus | 11764 | 0 | 0 | 0 | 0 | sim | - |
| camponotus_reborn | 11752 | 0 | 0 | 0 | 0 | sim | - |
| diclonius | 13805 | 0 | 0 | 0 | 0 | sim | - |
| hakua | 13679 | 12 | 0 | 0 | 0 | sim | - |
| haruichi | 11361 | 0 | 0 | 0 | 0 | sim | - |
| hoshina | 10636 | 0 | 0 | 0 | 0 | sim | - |
| hoshina_no10 | 13749 | 0 | 0 | 0 | 0 | sim | - |
| iharu | 11584 | 0 | 0 | 0 | 0 | sim | - |
| imitatio_mantidis | 18431 | 0 | 0 | 0 | 20 | sim | body |
| imitatio_mantidis_green | 18843 | 0 | 0 | 0 | 8 | sim | arm_right |
| isao | 11550 | 0 | 0 | 0 | 0 | sim | - |
| jura | 10915 | 0 | 0 | 0 | 0 | sim | - |
| kafka | 11298 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_larva | 6985 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no1 | 18397 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no10_giant | 20267 | 0 | 0 | 0 | 4 | sim | body |
| kaiju_no10_small | 14529 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no11 | 18196 | 6 | 0 | 0 | 0 | sim | - |
| kaiju_no12 | 18242 | 12 | 0 | 0 | 0 | sim | - |
| kaiju_no13 | 13400 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no14 | 11999 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no15 | 17105 | 6 | 0 | 0 | 0 | sim | - |
| kaiju_no15_kikoru | 18763 | 126 | 0 | 0 | 0 | sim | - |
| kaiju_no2 | 15980 | 0 | 0 | 0 | 91 | sim | body |
| kaiju_no6 | 14736 | 0 | 0 | 0 | 15 | sim | body |
| kaiju_no8 | 20974 | 8 | 0 | 0 | 0 | sim | - |
| kaiju_no9 | 8222 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no9_black | 10372 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no9_camponotus | 13441 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no9_fusion | 14580 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no9_isao | 12754 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no9_no11 | 19248 | 0 | 0 | 0 | 0 | sim | - |
| kaiju_no9_no14 | 19618 | 6 | 0 | 0 | 0 | sim | - |
| kaiju_no9_no15 | 16996 | 6 | 2 | 0 | 0 | sim | head |
| kikoru | 11387 | 0 | 0 | 0 | 0 | sim | - |
| kikoru_no4 | 19424 | 0 | 0 | 0 | 0 | sim | - |
| konomi | 10870 | 0 | 8 | 0 | 0 | sim | chest |
| kota | 14271 | 14 | 0 | 0 | 0 | sim | - |
| lacerta_fratris | 14740 | 6 | 0 | 0 | 18 | sim | body |
| maxilla_cornuta | 13890 | 64 | 0 | 0 | 1 | sim | body |
| miguel | 11736 | 0 | 0 | 0 | 0 | sim | - |
| mina | 10773 | 0 | 0 | 0 | 0 | sim | - |
| myxogasterocarp | 13652 | 0 | 8 | 0 | 0 | sim | body |
| narumi | 11044 | 0 | 0 | 0 | 0 | sim | - |
| phaneroplasmodium | 11476 | 0 | 0 | 0 | 0 | sim | - |
| philinosoma | 14163 | 0 | 0 | 0 | 0 | sim | - |
| polyonax | 16137 | 0 | 0 | 0 | 156 | sim | body |
| preondactyl | 14381 | 0 | 0 | 0 | 0 | sim | - |
| primigenius | 10772 | 0 | 0 | 0 | 0 | sim | - |
| primigenius_honju | 12576 | 0 | 0 | 0 | 1 | sim | head |
| primigenius_resurrected | 10138 | 0 | 0 | 0 | 0 | sim | - |
| primigenius_revived | 13146 | 0 | 0 | 0 | 0 | sim | - |
| reno | 10942 | 0 | 0 | 0 | 0 | sim | - |
| reno_no6 | 11941 | 0 | 0 | 0 | 0 | sim | - |
| rin | 10789 | 0 | 4 | 0 | 0 | sim | head |
| ryo | 10585 | 0 | 0 | 0 | 0 | sim | - |
| soichiro | 14473 | 6 | 0 | 0 | 0 | sim | - |
| soldier | 10779 | 0 | 0 | 0 | 0 | sim | - |
| tae | 17433 | 132 | 0 | 0 | 0 | sim | - |
| trichonephila | 8582 | 0 | 0 | 0 | 2 | sim | body |
| trichonephila_honju | 12700 | 0 | 0 | 0 | 0 | sim | - |
| suit/mk1 | 7781 | 0 | 5 | 0 | 0 | sim | body |
| suit/mk1_reinforced | 5180 | 0 | 0 | 0 | 0 | sim | - |
| suit/numbers10_suit | 8620 | 0 | 0 | 0 | 0 | sim | - |
| suit/numbers2_gauntlets | 4998 | 0 | 0 | 0 | 0 | sim | - |
| suit/numbers4_suit | 8976 | 0 | 0 | 0 | 4 | sim | right_arm |
| suit/numbers4_wings | 5952 | 0 | 0 | 0 | 0 | sim | - |
| suit/numbers6_suit | 8907 | 0 | 0 | 0 | 0 | sim | - |
