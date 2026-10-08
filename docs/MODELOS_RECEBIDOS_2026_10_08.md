# Modelos recebidos do Miguel (2026-10-08)

Modelos refeitos e personagens novos enviados pelo Miguel. **So guardados; nada foi implementado.** Os personagens
novos so entram quando o Miguel mandar o documento de atualizacoes e pedir (ele: "nao comece a fazer os personagens
novos, estou apenas enviando os modelos para ja termos salvo"). GLB fora do Git (o Miguel guarda os originais).

Todos chegaram do Meshy sem esqueleto (sem skin/animacao), com ~1,9 m de altura normalizada (tamanho real abaixo) e
textura 4096 (2048 nos No. 10): reduzir para 512-1024 na conversao.

| Arquivo (nome do envio) | Id sugerido | Triangulos | O que e | Decisao do Miguel |
|---|---|---|---|---|
| `Defense_Soldier_Spine` | `soldier` | 12.832 | soldado com capacete, pose aberta | **substitui** o soldado comum |
| `Defense_Suit_Spine` | `mk1` | 11.882 | traje sem cabeca | **substitui** o Mk1 (traje 3D da 0.6-C) |
| `Kaiju_No_10_Retexture` | `kaiju_no10_small` | 16.330 | No. 10 forma pequena refeita | **substitui** a forma pequena |
| `Red_Minecraft_Kaiju_1` | `kaiju_no10_giant` | 17.633 | No. 10 forma gigante refeita | **substitui** a forma gigante |
| `Kaiju_No_8_Remesh_14k` | `kaiju_no8` | 14.247 | Kaiju No. 8 (Kafka transformado) | novo; **2 m** de altura |
| `Kaiju_Larva_Remesh_6k` | `kaiju_larva` | 6.005 | larva que infecta o Kafka (asas, patinhas, cauda) | nova; **pequena** |
| `Kafka_Hibino_Remesh` | `kafka` | 10.030 | Kafka Hibino, forma humana | novo; vira o Kaiju No. 8 quando esta perdendo |
| `Kikoru_Base_Suit` | `kikoru` | 10.016 | Kikoru Shinomiya, forma normal | soldado especial novo |
| `Kikoru_Numbers_4` | `kikoru_no4` | 13.347 | Kikoru com a arma numerada 4 (casaco longo) | soldado especial novo |
| `Numbers_4_Wings` | `kikoru_no4_wings` | 11.074 | 4 asas em X da Numbers 4 (separadas) | vai nas costas da `kikoru_no4` |
| `leno_clean_boxy` | `reno` | 9.508 | Reno Ichikawa (cabelo branco), forma normal | novo |
| `leno_numbers_6` | `reno_no6` | 9.821 | Reno com a arma numerada 6 (traje azul) | novo |
| `mina_ashiro` | `mina` | 9.119 | Mina Ashiro (rabo de cavalo) | nova |
| `Mina_Ashiro_Heavy_Can` | `mina_cannon` | 7.518 | canhao pesado da Mina (arma, deitado) | arma nova |
| `gen_narumi_remesh_10k` | `narumi` | 9.822 | Gen Narumi (cabelo rosa e preto) | novo |
| `Gen_Narumi_Bayonet` | `narumi_bayonet` | 7.655 | baioneta longa do Narumi (arma, em pe) | arma nova |
| `phanero_plasmodium` | `phaneroplasmodium` | 13.514 | kaiju cogumelo **Yoju** (chapeu branco e vermelho, boca com dentes, 8 patas) | kaiju novo |
| `myxogasterocarp` | `myxogasterocarp` | 15.450 | kaiju cogumelo **Honju** (varios chapeus escuros com vermelho, raizes como patas) | kaiju novo |
| `philinosoma_honju` | `philinosoma` | 15.564 | lagarto gigante (vermelho, barriga clara, espinhos azuis, cauda reta) | kaiju novo |
| `hoshina_standard` | `hoshina` | 8.450 | Hoshina com o traje normal refeito (bainhas nas costas) | **substitui** o Hoshina atual (0.6-D) |

## Observacoes da analise (para o rig)

- Todos os humanoides vieram em pose aberta, cabeca reta e sem pecas grudadas: bons para o `rig_soldier_mesh.py`.
- `kikoru_no4`: o casaco longo cobre as pernas ate o chao; cortar o casaco em faixas que seguem as pernas pela
  metade (fica um pouco mais duro que o resto).
- `kaiju_no10_small` e `kaiju_no10_giant`: cauda enrolada para o lado (balanco duro, como a do Hoshina com a arma
  10). Os modelos novos tambem trocam o rig atual (`rig_primigenius_mesh.py`: `head_shift` e cortes por especie
  precisam ser medidos de novo).
- `kaiju_larva`: cauda enrolada, mas e pequena.
- As asas da Numbers 4 vieram separadas: viram ossos proprios nas costas (podem bater).

## Como seguram as armas (referencias do Miguel, 2026-10-08)

- **Gen Narumi + baioneta:** a arma e **maior que ele** (lamina + cano, uns 1,3-1,5x a altura dele). Duas maos,
  na diagonal (ataque: uma mao no punho perto da ponta de tras, outra no meio, lamina para cima e para a frente);
  parado, **apoiada no ombro** com uma mao, lamina para cima atras da cabeca.
- **Mina + canhao:** canhao pesado **na altura da cintura/quadril**, duas maos (uma no punho, outra na alca de cima),
  cano comprido para a frente; o canhao tem mais ou menos a altura dela.
- **Personagem de cabelo branco com mascara (painel do manga):** fuzil grande de cano grosso **apoiado no ombro**,
  ajoelhado, mirando. [DECIDIR] confirmar de quem e (Reno?) e se e a arma da Numbers 6.
- **Duas laminas longas (imagem de jogo, ultimo quadro):** uma em cada mao, abertas para os lados. [DECIDIR]
  confirmar de quem e o estilo.
- Kikoru e Hoshina: referencias na proxima mensagem.

### Kikoru e Hoshina (segunda leva de referencias)

- **Kikoru + machado gigante** (AX-0112/0113): machado de lamina enorme (a cabeca do machado e quase do tamanho
  do tronco dela). Ataque: duas maos, girando de cima/de tras do ombro. Parada: **machado apoiado no ombro** com uma
  mao, lamina atras da cabeca. Liga com a regra da 0.4 ("o machado e de um soldado especial"): o `kn8:axe` atual
  e o machado dela (**decidido pelo Miguel, 2026-10-08: "esse machado e dela"**).
- **Aura da Kikoru:** **amarela/dourada com raios** (relampagos amarelos em volta do corpo e da arma, brilho
  amarelo no contorno). Vira um `aura/kikoru.json` no estilo `lightning` (como a roxa do Hoshina, so que amarela).
- **Hoshina:** postura **bem baixa e agachada**, pernas abertas, corpo inclinado para a frente, as duas facas
  curtas seguras **ao contrario** (lamina para tras, junto do antebraco), braços abertos para os lados; aura roxa
  no contorno. Serve para refazer o idle/guarda e as tecnicas dele.
