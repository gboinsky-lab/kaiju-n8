# B-2 — jogador por patente contra cada kaiju (estimativa)

Gerado por `tools/balance/jogador_por_patente.py`. Cada celula: **segundos para derrubar com o golpe leve em sequencia** (corpo a corpo / arma de fogo) e **golpes medios do kaiju para o jogador cair**. So o combate solo, sem esquiva, bloqueio, pesado, especial, nucleo, pesquisa nem esquadrao. Perfis de cada patente [SUPOSICAO]:

| Patente | Release | Corpo | Traje | Corpo a corpo | Arma de fogo | Vida |
|---|---|---|---|---|---|---|
| Candidato | 8% | 0 | training_suit | combat_knife | - | 20 |
| Oficial | 15% | 10 | mk1 | combat_knife | rifle | 22 |
| Oficial Senior | 25% | 20 | mk1_reinforced | sword | rifle | 24 |
| Lider de Pelotao | 40% | 35 | mk1_reinforced | axe | rifle | 26 |
| Vice-Capitao | 60% | 50 | mk1_reinforced | hoshina_sword | rifle | 28 |
| Capitao | 80% | 70 | mk1_reinforced | hoshina_sword | rifle | 30 |

| Kaiju | Vida | Candidato | Oficial | Oficial Senior | Lider de Pelotao | Vice-Capitao | Capitao |
|---|---|---|---|---|---|---|---|
| trichonephila | 65 | 5 s · 7.2 | 5 / 3 s · 10.8 | 5 / 3 s · 16.0 | 6 / 3 s · 19.4 | 3 / 3 s · 24.4 | 3 / 2 s · 31.9 |
| camponotus | 200 | 18 s · 2.4 | 13 / 12 s · 3.5 | 7 / 9 s · 5.1 | 6 / 6 s · 6.1 | 3 / 4 s · 7.7 | 3 / 3 s · 10.1 |
| phaneroplasmodium | 240 | 20 s · 2.5 | 15 / 14 s · 3.7 | 8 / 10 s · 5.4 | 6 / 7 s · 6.5 | 3 / 5 s · 8.1 | 3 / 4 s · 10.7 |
| primigenius | 220 | 21 s · 1.9 | 15 / 14 s · 2.7 | 8 / 10 s · 3.9 | 6 / 7 s · 4.8 | 3 / 4 s · 6.0 | 3 / 3 s · 7.8 |
| primigenius_resurrected | 310 | 31 s · 1.4 | 22 / 21 s · 2.0 | 11 / 15 s · 2.8 | 8 / 10 s · 3.4 | 4 / 7 s · 4.3 | 3 / 5 s · 5.6 |
| primigenius_honju | 360 | 36 s · 1.1 | 26 / 25 s · 1.4 | 13 / 18 s · 2.0 | 9 / 12 s · 2.4 | 5 / 8 s · 3.0 | 3 / 5 s · 4.0 |
| primigenius_revived | 460 | 48 s · 0.9 | 35 / 33 s · 1.1 | 17 / 24 s · 1.5 | 11 / 16 s · 1.9 | 6 / 10 s · 2.3 | 4 / 7 s · 3.1 |
| philinosoma | 380 | 38 s · 1.3 | 27 / 26 s · 1.7 | 14 / 19 s · 2.4 | 9 / 12 s · 2.9 | 5 / 8 s · 3.7 | 4 / 6 s · 4.8 |
| diclonius | 430 | 48 s · 0.9 | 35 / 33 s · 1.1 | 17 / 24 s · 1.6 | 11 / 16 s · 1.9 | 6 / 10 s · 2.4 | 4 / 7 s · 3.1 |
| myxogasterocarp | 420 | 44 s · 1.3 | 32 / 31 s · 1.7 | 16 / 22 s · 2.4 | 10 / 15 s · 2.9 | 6 / 9 s · 3.6 | 4 / 6 s · 4.7 |
| trichonephila_honju | 400 | 42 s · 1.7 | 31 / 29 s · 2.4 | 15 / 21 s · 3.4 | 10 / 14 s · 4.1 | 5 / 9 s · 5.2 | 4 / 6 s · 6.8 |
| preondactyl | 430 | 45 s · 1.0 | 33 / 31 s · 1.2 | 16 / 22 s · 1.7 | 11 / 15 s · 2.1 | 6 / 9 s · 2.6 | 4 / 7 s · 3.4 |
| kaiju_no13 | 2600 | 362 s · 0.4 | 258 / 252 s · 0.5 | 119 / 178 s · 0.6 | 66 / 115 s · 0.7 | 36 / 71 s · 0.9 | 26 / 48 s · 1.2 |
| kaiju_no9 | 1450 | 187 s · 0.5 | 134 / 130 s · 0.7 | 63 / 92 s · 0.8 | 37 / 60 s · 1.0 | 20 / 37 s · 1.2 | 14 / 25 s · 1.6 |
| kaiju_no10_small | 1650 | 230 s · 0.4 | 164 / 160 s · 0.5 | 75 / 113 s · 0.6 | 42 / 73 s · 0.7 | 23 / 45 s · 0.9 | 16 / 30 s · 1.2 |
| kaiju_no10_giant | 4600 | 698 s · 0.3 | 494 / 486 s · 0.3 | 223 / 341 s · 0.4 | 119 / 218 s · 0.5 | 65 / 134 s · 0.6 | 46 / 89 s · 0.8 |
