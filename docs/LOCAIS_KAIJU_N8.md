# Locais da obra no mod (etapa C3)

Pedido do Miguel (2026-10-16): duas bases (Base Marítima de Ariake, Base de Tachikawa) e duas cidades (Shinagawa e
Tachikawa), com a divisão de cada lugar e os ataques principais que acontecem nele, nascendo pelo mapa sem cair em
água nem em cima de vilas e outras construções, e construídas pelas referências da obra.

Legenda: **CANON** = está no mangá/anime (fonte abaixo); **REAL** = lugar real em que a obra se baseia;
**[SUPOSIÇÃO]** = escolha do mod onde a obra não diz.

## 1. Base Marítima de Ariake (有明りんかい基地)

| | |
|---|---|
| Divisão | **Primeira Divisão** (capitão **Gen Narumi**, vice-capitão Hasegawa) — CANON. É a sede da Força de Defesa do Japão (o QG, onde ficam o Diretor-Geral Isao Shinomiya e o setor de pesquisa) — CANON |
| Onde | Bairro de Koto, Tóquio, na beira da baía (Ariake/Rinkai, área de aterro à beira-mar) — REAL |
| Na obra | Kafka fica detido e é avaliado ali depois de ser descoberto como o No. 8 (caps. 33–39); teste do Kafka contra o Isao no QG; **catapulta eletromagnética** (電磁射出装置) que lança a Kikoru no combate (anime, ep. 22); reunião das divisões (temporada 2, ep. 6); laboratório de armas numeradas (as armas feitas de kaiju numerados são guardadas e ajustadas no QG) — CANON |
| Ataques | Ataque direto à base **não confirmado** nas fontes. A Primeira Divisão é a que enfrenta a **horda de kaiju-formiga** e o **Kaiju No. 9** em Tóquio (arco do ataque simultâneo do No. 9) — CANON. No mod: invasão `ariake_assault` com o No. 9 e as formas dele contra o QG [SUPOSIÇÃO] |
| No mod | `kn8:ariake_base`: QG alto de vidro (6 andares, heliporto), laboratório de armas numeradas, bloco de detenção (onde o Kafka ficou), catapulta eletromagnética (rampa de trilho), alas grandes da base e, ao sul, o **porto**: cais, bacia com água dentro da base, barco de patrulha, guindastes, contêineres e farol. Guarnição: **Narumi** (uma vez por mundo) |

## 2. Base de Tachikawa (立川基地)

| | |
|---|---|
| Divisão | **Terceira Divisão** (capitã **Mina Ashiro**, vice-capitão **Soichiro Hoshina**); Kafka, Reno, Kikoru, Iharu e o resto da turma do exame entram nela — CANON |
| Onde | Tachikawa, oeste de Tóquio. Modelo real: **Acampamento de Tachikawa da Força Terrestre** (立川駐屯地, Midori-cho), junto ao aeródromo; é por isso que a base da obra é maior que as outras — REAL |
| Na obra | Treino dos novatos, campo de tiro, vista aérea da base no ep. 9 — CANON |
| Ataques | **Raide do Kaiju No. 10** com kaiju do tipo wyvern/pterossauro (翼竜系) contra a base, mangá caps. 24–32, anime eps. 9–10: ~75% da base destruída, Hoshina luta com o No. 10, Kafka se transforma diante de todos — CANON. No mod: invasão `tachikawa_raid` (Preondactyl = os wyvern, Yoju em terra e o No. 10, que vira gigante) |
| No mod | `kn8:tachikawa_base`: QG da Terceira Divisão, alas grandes, **campo de treino** (a arena com o painel, já existente) e o **aeródromo** (pista, hangar com helicópteros, torre de controle, tanques de combustível). Guarnição: **Mina, Hoshina, Kikoru e Reno** (uma vez por mundo) |

## 3. Shinagawa (品川)

| | |
|---|---|
| Divisão | Área de atuação da **Primeira Divisão** (Tóquio central) — CANON pelo arco; [SUPOSIÇÃO] como "dono" do lugar |
| Onde | Estação de Shinagawa (trilhos elevados de várias linhas e Shinkansen), torres do **Shinagawa Intercity** (aparecem no encerramento do anime), calçadão coberto **Musashi-Koyama Palm** (locação da temporada 2) — REAL |
| Ataques | **Horda de kaiju-formiga** em Shinagawa (vol. 5) e o **No. 9 atacando Shinagawa**, onde absorve o Diretor-Geral Isao Shinomiya; um Honju já tinha sido abatido em Shinagawa dois anos antes da história — CANON. No mod: invasão `shinagawa_ants` (Camponotus, Camponotus revivida, No. 9 por chance, a forma formiga do No. 9 também por chance) |
| No mod | `kn8:shinagawa_city`: estação com plataformas e trilhos elevados, praça da estação com as torres de escritório, ruas próprias e lotes de Shinagawa (torres de escritório altas, calçadão coberto com lojas) mais lotes da cidade comum |

## 4. Cidade de Tachikawa (立川市)

| | |
|---|---|
| Divisão | Cidade da **Terceira Divisão** (a base fica nela) — CANON |
| Onde | Estação de Tachikawa, **monotrilho Tama** elevado, loja de departamentos junto à estação (GRANDUO/Isetan), **Parque Showa Kinen** (alameda de ginkgos, lago), Tachikawa Stage Garden — REAL |
| Ataques | Nenhum ataque à cidade em si confirmado nas fontes. [SUPOSIÇÃO] invasão `tachikawa_city_raid`: Yoju comuns e Preondactyl de passagem (o mesmo tipo do raide à base) |
| No mod | `kn8:tachikawa_city`: estação com o monotrilho elevado por cima da praça, ruas próprias, lotes de Tachikawa (loja de departamentos, prédios de apartamento, parque com alameda de ginkgos e lago) mais lotes da cidade comum |

## 5. Onde nascem no mapa

- Um conjunto só (`kn8:defense_sites`) com os quatro lugares, ~1 a cada 1.000–1.500 blocos [SUPOSIÇÃO], só em
  biomas planos.
- **Sem água:** antes de gerar, o mod mede o terreno numa grade em volta do ponto (`kn8:checked_jigsaw`): se em
  algum ponto a superfície estiver acima do fundo (água, rio, lago) ou o desnível passar do limite, o lugar não nasce
  ali. A água do porto de Ariake é da própria construção.
- **Sem vilas e outras construções:** a posição (`kn8:avoiding_spread`) recusa o chunk se houver chunk de vila,
  posto de saqueadores, templo do deserto, iglu, cabana da bruxa ou mansão a poucos chunks; e as construções
  pequenas do mod (posto avançado, prédio destruído, restos de kaiju, torre de vigia, base pequena) não nascem perto
  dos lugares.

## Fontes

- Wikipedia (en), "Kaiju No. 8" e "List of Kaiju No. 8 chapters" (divisões, capitães, arco do No. 10, do No. 9).
- Sportskeeda, análises dos eps. 9 e 10 (raide do No. 10 a Tachikawa, base destruída).
- Animate Times, ep. 9 (vista aérea da base de Tachikawa); Natalie (notícias do anime).
- X oficial @kaijuno8ex ("第1部隊 有明りんかい基地").
- Dressy / Musubi10 (立川駐屯地 como modelo da base de Tachikawa).
- Pixiv Encyclopedia e Ciatr (品川 e os kaiju-formiga; Isao em Shinagawa).
- Anime Pilgrimage, temporada 2 (11 locais de Shinagawa, Shinagawa Intercity, Musashi-Koyama).
- ONE Esports (Primeira Divisão e Narumi); Fandom "Ariake Maritime Base" (cap. 33).
