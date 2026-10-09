# Novos personagens, kaiju e armas

Tudo o que entrou no mod a partir dos modelos que o Miguel mandou em 2026-10-08 (lista em
`docs/MODELOS_RECEBIDOS_2026_10_08.md`): a Kikoru normal (0.5.0-D8), os modelos refeitos da 0.5.0 e a atualização
0.7 inteira (pedido de 2026-10-09: "implemente os modelos que você tem guardado e comece a implementar os novos
soldados e kaijus"). Cada linha foi vista em jogo com servidor dedicado + 2 clientes, salvo onde está escrito "só
GameTest". Onde nascem e o balanceamento final ficam para depois de todos os personagens (Miguel, 2026-10-09); por
enquanto os kaiju aparecem por `/summon` ou `/kn8 kaiju spawn` e os soldados especiais pelo ovo ou `/summon`. Números
marcados [SUPOSIÇÃO] estão em `docs/BALANCEAMENTO.md`.

**Resumo:** 7 kaiju novos, 10 personagens novos (Kikoru, Reno, Mina, Narumi, Kikoru No. 4, Reno No. 6, Narumi No. 1,
Kafka e Kaiju No. 8 contam 9 entidades de soldado especial; o No. 8 é a forma kaiju do Kafka), 3 armas novas e 5
modelos refeitos.

## Kaiju

| Etapa | Id (`/summon kn8:<id>`) | Nome | Categoria | Tamanho / hitbox | Vida / dano / armadura / velocidade | Ataques | Núcleo |
|---|---|---|---|---|---|---|---|
| 0.7-A | `philinosoma` | Philinosoma | Honju | lagarto de 9 m, cauda longa; 6 × 9 | 380 / 14,2 / 12 / 0,27 | soco pesado, mordida, rabada, investida, **disparo de espinhos** (5–24 blocos) | peito |
| 0.7-A | `diclonius` | Diclonius | Honju | em pé, 9 m, placas nas costas; 5 × 9 | 430 / 15,5 / 14 / 0,23 | soco pesado, mordida, rabada, pancada no chão, **sopro de energia** (6–34 blocos, explode) | peito |
| 0.7-A | `camponotus` | Camponotus | Yoju | formiga de 6 patas, 6 m; 4 × 2,9 | 200 / 9,5 / 10 / 0,33 | mordida, estocada, varredura de patas, salto, **jato de ácido** (deixa lento) | cabeça |
| 0.7-A | `camponotus_reborn` | Camponotus revivida | Yoju | mesma forma, azul | 290 / 11,5 / 11 / 0,34 | os mesmos, ácido mais forte; **fúria** abaixo de 30% | cabeça |
| 0.7-B | `phaneroplasmodium` | Phaneroplasmodium | Yoju | cogumelo de 8 patas, 5 m; 4,4 × 5 | 240 / 10,5 / 9 / 0,27 | mordida, estocada, varredura e várias patas, **nuvem de esporos** (deixa lento) | chapéu |
| 0.7-B | `myxogasterocarp` | Myxogasterocarp | Honju | chapéus empilhados, raízes como patas, 9 m; 7 × 9 | 420 / 15 / 13 / 0,20 | mordida, estocada, varredura e várias raízes, **bomba de esporos** (explode) | caule |
| 0.7-B | `kaiju_larva` | Larva misteriosa | numerado (origem do No. 8) | 0,5 m, dois pares de asas | 20 / 2 / 0 / 0,30 (voo 0,45) | mordida, mergulho; **voa** em círculo a 3 blocos do alvo | — |

Ataques em negrito são novos; os outros reaproveitam habilidades que já existiam. Os ataques à distância dos kaiju
(espinhos, sopro, ácido, esporos) só foram confirmados pelo GameTest; em jogo os kaiju foram vistos andando,
perseguindo e lutando contra soldados.

## Soldados especiais

| Etapa | Id (ovo ou `/summon kn8:<id>`) | Personagem | Altura | Vida / armadura / velocidade (v1.2) | Arma | Técnicas | Aura |
|---|---|---|---|---|---|---|---|
| 0.5.0-D8 | `kikoru` | Kikoru Shinomiya (traje normal) | 1,57 m | 390 / 18 / 0,34 | machado de duas mãos (`kn8:axe`) | corte de machado, golpe pesado em giro, onda de choque, investida com golpe, golpe no chão, quebra-guarda; esquiva, contra-ataque e aparar (mais raros que os do Hoshina) | amarela com raios |
| 0.7-C | `reno` | Reno Ichikawa (traje normal) | 1,70 m | 360 / 17 / 0,31 | rifle | tiro de precisão, rajada de 3, munição congelante (lentidão), supressão em leque, coronhada de perto; fica longe atirando | azul-gelo |
| 0.7-C | `mina` | Mina Ashiro | 1,65 m | 420 / 20 / 0,30 | **canhão pesado** no quadril (`kn8:mina_cannon`) | tiro de precisão, tiro do canhão (explode), canhão carregado, Anti-Giant (preparo longo, prefere Honju/numerados); fica longe | laranja |
| 0.7-C | `narumi` | Gen Narumi (traje normal) | 1,78 m | 480 / 20 / 0,32 | **baioneta longa** de duas mãos (`kn8:narumi_bayonet`) | estocadas em sequência, investida, varrida que empurra, tiro da baioneta | rosa com raios |
| 0.7-D | `kikoru_no4` | Kikoru Shinomiya (Numbers 4) | 1,57 m, 4 asas em X nas costas | 580 / 20 / 0,44 | machado | **voa**: paira acima do alvo e mergulha nos golpes (machado veloz, mergulho, ataque vertical que expõe o núcleo, combo aéreo, dash aéreo); pousa sem alvo | azul e amarela |
| 0.7-D | `reno_no6` | Reno Ichikawa (Numbers 6) | 1,70 m, traje azul | 650 / 20 / 0,36 | rifle | criocinese: lança de gelo, explosão congelante, campo de gelo em volta, congelamento múltiplo, canhões auxiliares | azul-gelo |
| 0.7-D | `narumi_no1` | Gen Narumi (Numbers 1) | modelo do Narumi | 650 / 20 / 0,36 | baioneta | previsão: apara (35%) e contra-ataca mais, golpe no ponto fraco que expõe o núcleo, contra-golpe previsto | rosa e branca |
| 0.7-F | `kafka` | Kafka Hibino (humano) | 1,81 m | 24 / 6 / 0,30 | rifle | tiro, sequência de socos; **vira o Kaiju No. 8** abaixo de 50% da vida | da Força de Defesa |
| 0.7-F | `kaiju_no8` | Kaiju No. 8 | 2,0 m | 1.800 / 19 / 0,40 | punhos (`kn8:kaiju_no8_fist`, invisível) | rajada de socos, soco pesado, investida, golpe no chão (explode), rugido (deixa lento); **regenera** abaixo de 50%; **volta a ser o Kafka** depois de 15 s sem alvo | ciano em chamas |

Todos usam a IA dos soldados especiais (a mesma do Hoshina): esquiva, contra-ataque, aparar e escalada
de Release. As técnicas de tiro, a explosão e a munição congelante só foram confirmadas pelo GameTest; em jogo os
três foram vistos com arma e aura, e lutando contra um Primigenius.

A Kikoru normal defende nas invasões `honju_assault`, `web_queen`, `no9_resurrection` (nível 3) e `kaiju_horde`
(nível 4, com o Hoshina); vista de novo em jogo em 2026-10-09 contra um Primigenius (raios amarelos nos golpes, núcleo
exposto pelo quebra-guarda; `docs/img/kikoru_normal.jpg`). Os personagens da 0.7 ainda não aparecem em invasões. O Miguel pode animá-la no Blockbench
pelo modelo-base `tools/blockbench/modelos_base/kikoru.bbmodel`.

## Armas novas

| Id | Nome | Uso | Observação |
|---|---|---|---|
| `kn8:mina_cannon` | Canhão Pesado da Mina | arma de fogo, 12 de dano | na mão do jogador usa o perfil do rifle [SUPOSIÇÃO] |
| `kn8:narumi_bayonet` | Baioneta do Narumi | arma pesada, 9 de dano, alcance 3,6 | na mão do jogador usa o perfil do machado [SUPOSIÇÃO] |
| `kn8:kaiju_no8_fist` | Punhos do Kaiju No. 8 | só do No. 8, 30 de dano, alcance 3 | item invisível; não vai para o jogador |

## Modelos refeitos (substituíram os antigos)

| Etapa | O quê | Onde ver |
|---|---|---|
| 0.5.0-B | Soldado comum e traje Mk1 (corpo inteiro, estilo Minecraft) e Mk1 Reforçado | `docs/img/mk1_e_soldado_estilo_minecraft.png` |
| 0.5.0-B / D4 | Hoshina (sem o rosto duplicado, sem bainhas, pegada no meio do cabo, técnicas pelo anime) | `docs/img/hoshina_modelo.png` |
| 0.5.0-C | Kaiju No. 10 pequeno e gigante (cauda enrolada em 4 pedaços, cabeça centralizada) | `docs/img/no10_refeito_pequeno_gigante.png` |

## Pendências [DECIDIR]

- Onde cada um aparece (invasões, missões, defensores) e o balanceamento final: depois de todos os personagens.
- Desmonte: os kaiju novos reaproveitam o da aranha/Tecedeira (Yoju/Honju de patas) e o do Primigenius Honju.
- A larva entrar no corpo do Kafka e a transformação do jogador (M12): [DECIDIR].
- Animações próprias do canhão e da baioneta para o jogador.
- A pata de trás esquerda da formiga veio dobrada por baixo do abdômen no modelo e mexe pouco ao andar.

## Próximas etapas

- **0.7-E**: No. 9 forma preta, No. 9 fundido à formiga e No. 9 + No. 10.

## Onde ficam os arquivos

- Dados: `data/kn8/kn8/kaiju/`, `ability/`, `flyer/kaiju_larva.json`, `locomotion/`, `special_soldier/`, `weapon/`,
  `aura/`.
- Modelos (malha presa aos ossos): `assets/kn8/meshes/<id>/`, `geo/entity/<id>.geo.json`,
  `animations/entity/<id>.animation.json`, `textures/entity/<id>.png`; armas em `models/item/` e `textures/item/`.
- Java: `KN8Entities`, `KN8Items`, `KN8Client`, `soldier/special/KikoruEntity|RenoEntity|MinaEntity|NarumiEntity|KikoruNo4Entity|RenoNo6Entity|NarumiNo1Entity|KafkaEntity|KaijuNo8Entity`
  (troca de forma e regeneração no `HoshinaEntity.tickForm`),
  `combat/SlashProjectile` (bala, explosão, lentidão), `data/def/SlashSpec`.
- Ferramentas: `tools/art/rig_primigenius_mesh.py`, `rig_trichonephila_mesh.py`, `rig_preondactyl_mesh.py`,
  `rig_soldier_mesh.py`, `gen_ability_animations.py`, `build_primigenius_honju.py`, `gen_special_animations.py`.
- Imagens em jogo: `docs/img/kikoru_normal.jpg`, `kaiju_0_7_a_modelos.jpg`, `kaiju_0_7_b_modelos.jpg`, `especiais_0_7_c.jpg`, `numeradas_0_7_d.jpg`, `kafka_no8_0_7_f.jpg`.
- Roteiros de teste: `docs/ROTEIRO_TESTE_0_1_B.md` §32 a §37 (§37 = Kikoru normal).
