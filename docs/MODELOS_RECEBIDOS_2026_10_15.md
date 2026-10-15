# Modelos recebidos do Miguel (2026-10-15)

So guardados (GLB fora do Git, como os anteriores); entram quando a etapa deles chegar. Previa de frente e de costas:
`docs/img/modelos_recebidos_2026_10_15.jpg`.

Os tres vieram do Meshy sem esqueleto, com altura normalizada de 1,9 m e textura 4096 (reduzir para 512-1024 na
conversao, como os outros).

| Arquivo (nome do envio) | Id sugerido | Triangulos | O que e | Observacao |
|---|---|---|---|---|
| `isao_shinomiya_remesh` | `isao` | 9.996 | Isao Shinomiya (cabelo loiro, traje preto, manoplas brancas e pretas da arma numerada 2 nos dois bracos) | soldado especial novo (comandante, forca pesada; plano de IA §9.8). Rig como os especiais (`rig_soldier_mesh.py isao`); as manoplas ficam nos ossos dos antebracos |
| `kaiju_no9_fused_remes` | `kaiju_no9_isao` | 12.492 | Kaiju No. 9 depois de absorver o Isao (preto, espinhos vermelhos nos ombros/joelhos, boca com dentes, cabeca de chapeu) | forma nova do No. 9 (a "apos absorcao de Isao/No. 2" do plano de IA §10.5); transicao por `absorb` (presa = Isao) como a fusao com o No. 10 |
| `kaiju13_minecraft_rem` | `kaiju_no13` | 14.558 | Kaiju No. 13 (branco, bracos e pernas vermelhos, garras, cabeca com chifres para os lados) | numerado novo; humanoide como o No. 9 (`rig_kaiju_no9_mesh.py kaiju_no13`) |

[DECIDIR] alturas reais (Isao ~1,85 m [SUPOSICAO]; No. 9 com o Isao e No. 13: o Miguel decide), onde aparecem e as
habilidades (o plano de IA pede ficha com fontes antes de programar cada um).
