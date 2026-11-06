# Etapa 7-D: chances das invasoes e matriz de duelos com os novos (2026-11-06)

Servidor dedicado, script `duel.py` do scratchpad (o mesmo da B-2), mundo plano, `/tick rate 60`,
datapack `kn8_duelo_isolado` regerado (No. 9, forma preta, formiga, fusao e No. 10 pequeno sem `transform`/`absorb`).
3 duelos por confronto, personagem (A) x kaiju (B), 14 blocos (18 no No. 14, 30 no No. 1, No. 2 e No. 6), limite
300 s. **V** = kaiju morto (carcaca), **E** = kaiju sumiu sem carcaca (retirada do No. 9 ou, no No. 14, teleporte
para fora da area medida) ou tempo, **D** = personagem morto. Vida que sobrou em % de cada lado. Log bruto:
`docs/ETAPA7D_MATRIZ.log`.

## Resultado

| A | B | V / E / D | Duelos |
|---|---|---|---|
| soichiro | primigenius_honju | 3 / 0 / 0 | V 4s A 99% B 0%, V 5s A 100% B 0%, V 5s A 99% B 0% |
| jura | primigenius_honju | 3 / 0 / 0 | V 6s A 99% B 0%, V 5s A 99% B 0%, V 5s A 99% B 0% |
| rin | primigenius_honju | 3 / 0 / 0 | V 19s A 93% B 0%, V 18s A 88% B 0%, V 19s A 83% B 0% |
| iharu | primigenius_honju | 3 / 0 / 0 | V 14s A 91% B 0%, V 13s A 91% B 0%, V 13s A 91% B 0% |
| haruichi | primigenius_honju | 3 / 0 / 0 | V 17s A 93% B 0%, V 22s A 83% B 0%, V 17s A 90% B 0% |
| aoi | primigenius_honju | 3 / 0 / 0 | V 15s A 93% B 0%, V 16s A 97% B 0%, V 14s A 96% B 0% |
| hakua | primigenius_honju | 3 / 0 / 0 | V 37s A 65% B 0%, V 33s A 83% B 0%, V 33s A 87% B 0% |
| kota | primigenius_honju | 3 / 0 / 0 | V 18s A 81% B 0%, V 18s A 81% B 0%, V 22s A 78% B 0% |
| tae | primigenius_honju | 2 / 0 / 1 | D 34s A 0% B 12%, V 36s A 96% B 0%, V 45s A 85% B 0% |
| ryo | primigenius_honju | 3 / 0 / 0 | V 35s A 89% B 0%, V 34s A 78% B 0%, V 32s A 70% B 0% |
| miguel | primigenius_honju | 3 / 0 / 0 | V 3s A 100% B 0%, V 5s A 98% B 0%, V 5s A 98% B 0% |
| camilly | primigenius_honju | 3 / 0 / 0 | V 11s A 94% B 0%, V 13s A 94% B 0%, V 12s A 94% B 0% |
| soichiro | kaiju_no13 | 3 / 0 / 0 | V 32s A 21% B 0%, V 35s A 31% B 0%, V 35s A 28% B 0% |
| jura | kaiju_no13 | 3 / 0 / 0 | V 40s A 39% B 0%, V 39s A 56% B 0%, V 38s A 45% B 0% |
| rin | kaiju_no13 | 0 / 0 / 3 | D 32s A 0% B 82%, D 22s A 0% B 89%, D 35s A 0% B 80% |
| iharu | kaiju_no13 | 0 / 0 / 3 | D 30s A 0% B 74%, D 39s A 0% B 67%, D 29s A 0% B 80% |
| haruichi | kaiju_no13 | 1 / 0 / 2 | D 30s A 0% B 80%, V 20s A 14% B 0%, D 31s A 0% B 79% |
| aoi | kaiju_no13 | 0 / 0 / 3 | D 26s A 0% B 86%, D 36s A 0% B 74%, D 19s A 0% B 88% |
| hakua | kaiju_no13 | 0 / 0 / 3 | D 33s A 0% B 92%, D 22s A 0% B 94%, D 27s A 0% B 90% |
| kota | kaiju_no13 | 0 / 0 / 3 | D 23s A 0% B 89%, D 19s A 0% B 89%, D 30s A 0% B 84% |
| tae | kaiju_no13 | 0 / 0 / 3 | D 12s A 0% B 97%, D 35s A 0% B 91%, D 19s A 0% B 96% |
| ryo | kaiju_no13 | 0 / 0 / 3 | D 18s A 0% B 95%, D 25s A 0% B 93%, D 26s A 0% B 93% |
| miguel | kaiju_no13 | 3 / 0 / 0 | V 35s A 24% B 0%, V 31s A 41% B 0%, V 32s A 50% B 0% |
| camilly | kaiju_no13 | 0 / 0 / 3 | D 46s A 0% B 49%, D 54s A 0% B 43%, D 52s A 0% B 40% |
| hoshina | polyonax | 3 / 0 / 0 | V 16s A 98% B 0%, V 18s A 98% B 0%, V 17s A 98% B 0% |
| hoshina | lacerta_fratris | 3 / 0 / 0 | V 14s A 98% B 0%, V 16s A 97% B 0%, V 11s A 98% B 0% |
| hoshina | imitatio_mantidis | 3 / 0 / 0 | V 12s A 96% B 0%, V 13s A 96% B 0%, V 14s A 96% B 0% |
| hoshina | maxilla_cornuta | 3 / 0 / 0 | V 17s A 97% B 0%, V 17s A 97% B 0%, V 20s A 97% B 0% |
| hoshina | anguilla_ambulans | 3 / 0 / 0 | V 14s A 96% B 0%, V 17s A 98% B 0%, V 15s A 94% B 0% |
| hoshina | myxogasterocarp | 3 / 0 / 0 | V 28s A 98% B 0%, V 18s A 95% B 0%, V 22s A 99% B 0% |
| hoshina_no10 | kaiju_no11 | 1 / 0 / 2 | D 40s A 0% B 18%, D 34s A 0% B 18%, V 44s A 22% B 0% |
| hoshina_no10 | kaiju_no12 | 3 / 0 / 0 | V 40s A 77% B 0%, V 40s A 68% B 0%, V 39s A 59% B 0% |
| hoshina_no10 | kaiju_no15 | 3 / 0 / 0 | V 46s A 66% B 0%, V 56s A 63% B 0%, V 53s A 63% B 0% |
| hoshina_no10 | kaiju_no15_kikoru | 3 / 0 / 0 | V 36s A 77% B 0%, V 41s A 70% B 0%, V 38s A 78% B 0% |
| hoshina_no10 | kaiju_no14 | 1 / 1 / 1 | E 26s A 48% B 0%, D 50s A 0% B 45%, V 21s A 39% B 0% |
| hoshina_no10 | kaiju_no1 | 2 / 0 / 1 | D 34s A 0% B 43%, V 45s A 28% B 0%, V 43s A 4% B 0% |
| hoshina_no10 | kaiju_no2 | 3 / 0 / 0 | V 43s A 84% B 0%, V 40s A 86% B 0%, V 41s A 66% B 0% |
| hoshina_no10 | kaiju_no6 | 3 / 0 / 0 | V 50s A 68% B 0%, V 48s A 82% B 0%, V 54s A 89% B 0% |
| hoshina_no10 | kaiju_no9_no11 | 1 / 1 / 1 | E 20s A 90% B 0%, D 37s A 0% B 93%, V 19s A 89% B 0% |
| hoshina_no10 | kaiju_no9_no15 | 0 / 0 / 3 | D 98s A 0% B 46%, D 73s A 0% B 53%, D 81s A 0% B 47% |
| hoshina_no10 | kaiju_no9_no14 | 0 / 0 / 3 | D 200s A 0% B 24%, D 291s A 0% B 26%, D 242s A 0% B 29% |

## Leitura

- **Especiais novos (T5, comandantes T6) x Primigenius Honju (T4):** 35 de 36 vitorias (Tae perdeu 1: e quem tem
  menos vida, 260). Dentro do papel.
- **Especiais novos x No. 13 (T7, 2.600):** so Soichiro, Jura e Miguel vencem (3/3, com 20-50% da vida); os outros
  perdem deixando o No. 13 com 65-95%. Camilly (comandante T6) perde as 3, mas deixa o No. 13 com 40-50%. Pela
  resposta 3 do Miguel ("os que estao mais fortes ficam"), nada mudou; [DECIDIR] se Soichiro e Jura devem vencer um
  numerado sozinhos (o dossie nao da tier para eles) e se o Miguel e a Camilly (mesmo tier) devem ficar tao distantes.
- **Hoshina x kaiju de nome cientifico (T4-T5):** 21/21, o Hoshina com 94-99% da vida. Myxogasterocarp vence a
  divisao em 22-28 s (os filhotes nascem e morrem). Dentro do papel (vice-capitao acima de Honju comum).
- **Hoshina No. 10 x numerados do anime (T8):** vence o No. 2 (25 m), o No. 6 (22 m), o No. 12 e as duas formas do
  No. 15 com 60-90% da vida; fica dividido com o No. 1 (2/3), o No. 11 (1/3) e o No. 14 (1 V, 1 E por teleporte, 1 D);
  perde para as fusoes Neural (0/3, ela com ~50%) e Couracado (0/3 em 200-290 s, ele com ~27%). A Abissal deu 1 V, 1 E
  e 1 D, mas o V e o E sairam em ~20 s com o Hoshina a 90% e a fusao tem 7.200 de vida: leitura suspeita (o mais
  provavel e a fusao sair da area medida, com uma carcaca de outro duelo na area), so o D (fusao com 93%) e confiavel;
  [A CONFERIR] em jogo.
  Os gigantes No. 2 e No. 6 rendem menos que o tier (o alvo grande e facil de acertar e eles erram o humano no chao);
  o No. 11 (2.500 de vida) e o mais duro dos numerados simples. Pela resposta 1 ("numerados ficam como estao"), nada
  mudou: [DECIDIR] se o No. 2 e o No. 6 devem ganhar mais dano/alcance contra alvo pequeno.
- **Fusoes:** acima de tudo o que existe sozinho, como no dossie (T9).

## Invasoes

Revisao de todas as 15 (nivel, peso natural, ondas, kaiju fixos, defensores e surpresas por chance): batem com o
nivel, exceto o **No. 14** (numerado T8) como surpresa de 30% no ataque a cidade de Tachikawa (nivel 3), onde o
aviso de ameaca grande (nivel 4+) nao toca e onde nenhum outro numerado aparece: passou para o Raide de Tachikawa
(nivel 6, 25%), junto do No. 12 e do No. 15 na forma Kikoru. As surpresas de 1% (formas e fusoes do No. 9, No. 8
sem controle) ficam como o Miguel decidiu.
