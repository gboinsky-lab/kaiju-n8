# Kaiju No. 8: Defense Force (`kn8`) — versão 1.0.0

Mod fan gratuito e não oficial para Minecraft **1.21.1** (NeoForge **21.1.252**). Precisa de **GeckoLib 4.7.5+** e
**Player Animation Library 1.1.5+** nos dois lados (servidor e cliente). Protocolo de rede `42`: cliente e servidor
precisam da mesma versão do mod.

A 1.0.0 junta tudo o que foi feito desde a 0.1 (etapas 0.1-B a R-1). Esta lista é por sistema, não por etapa; o
histórico de cada etapa, com as decisões e os números, está no `CLAUDE.md` e no `docs/ROTEIRO_TESTE_0_1_B.md` (§1 a
§148).

## Em números

| | |
|---|---|
| Kaiju (espécies e formas) | 42 |
| Habilidades de kaiju | 152 |
| Soldados especiais (perfis e formas) | 26, mais o soldado comum e o civil |
| Armas | 23 |
| Trajes | 6 |
| Invasões | 15 (níveis 1 a 6) |
| Missões, provas e cursos | 27 |
| Pesquisas | 6 |
| Módulos da base do jogador | 5 |
| Locais da obra | 8 (bases, cidades, bairros e o Campo do Exame) |
| Auras | 36 |
| Perfis de efeito visual | 71 |
| Sons próprios (`.ogg`) | 172, todos feitos por síntese |
| GameTests | 361 |

## Jogador

- **Release** (tecla **G** sobe, **Shift + G** desce): só com traje. Limite pessoal sorteado e que sobe com o
  treino; até 20 pontos acima do limite com calor, dano no corpo e fadiga ao voltar. Aura com raios na cor escolhida
  no Perfil (9 cores; a do traje vale por cima).
- **Corpo** (força, velocidade, resistência, agilidade) treinado por exercício: corrida, escalada, suporte de pesos e
  treino de combate contra soldado comum (Shift + clique de mão vazia). Lutar com kaiju dá experiência de batalha e
  mérito, não atributos.
- **Combate**: combo leve, golpe pesado e carregado, bloqueio (**V**), esquiva (**Z**) com **esquiva perfeita**,
  aparar, dash, crítico. Armas de fogo com pente e munição como itens, recarga (**R** sem especial), tiro preciso no
  clique direito. Técnicas por arma: **X** troca (Shift + X volta), **R** usa; cada uma com a própria recarga.
- **Técnicas da obra** no jogador: as 7 formas do Hoshina (Jūni-hitoe só com o Numbers 10 na força total), o estilo
  do machado da Kikoru (Falling Thunder pulando de cima), as do Narumi e do Numbers 1 (que sempre acertam), as do
  Isao com o Numbers 2 (Thunder Emperor só com a vida baixa), minigun da Rin que superaquece.
- **Armas numeradas** (Numbers 1, 2, 4, 6 e 10) com patente, pesquisa e limite exigidos e domínio que sobe com o uso;
  asas do Numbers 4 (Shift no ar) e cauda do Numbers 10 (corta e defende sozinha).
- **Parkour** (tecla **C**): vault, escalada curta e de prédios, salto longo, wall-run, wall-jump, segurar e subir
  bordas (automático no ar), andar de lado pendurado, slide e rolamento.
- **Traje cinético**: sem dano de queda e sem empurrão com traje.

## Hospedeiro do Kaiju No. 8

- Larva rara em restos de kaiju e nos locais da obra; um hospedeiro por mundo.
- Tecla **B**: forma do No. 8 (3 m) só com os punhos, força pelo corpo treinado, regeneração, 2 minutos de limite e
  depois perda de controle (vermelho, ataca tudo em volta). **Shift + B**: só o braço por 5 s.
- Release humano do hospedeiro preso entre 0,1% e 1%.
- A Força de Defesa reage: soldados atacam o hospedeiro transformado abaixo de Vice-Capitão (ou sem controle),
  mérito perdido ao ser visto, serviços fechados, civis fogem.

## Carreira e base

- Patentes por mérito e provas de promoção (tiro, esquadrão, comando, capitão); avaliação S a D de toda missão.
- **Exame de admissão** no Campo do Exame com kit entregue (o aprovado fica com ele) e 4 candidatos que ajudam.
- Menu da Força de Defesa (tecla **M**): Perfil, Missões, Alertas, Esquadrão, Bestiário, Arsenal (bancada e
  pesquisa), Base.
- **Esquadrão** pela patente (até 8; especiais a partir de Vice-Capitão) com ordens Seguir, Ficar, Atacar, Recuar,
  Focar e Guarda.
- Oficiais de Missões e de Provas, estação de preparação, terminal de pesquisa, bancada, suporte de pesos e Konomi
  (pesquisadora) nas bases.
- **Base do jogador**: Núcleo da Base com 8 lotes e 5 módulos (Oficina, Laboratório, Alojamento, Comando, Pátio de
  Treino).
- **Apoio militar** pelo Rádio de Campo em missão ou invasão: reconhecimento, suprimentos e bombardeio.

## Kaiju

- Yoju, Honju, numerados e Daikaiju com partes, núcleo, ferimentos por parte (perna, cabeça, cauda...), carcaça com
  desmonte em etapas e material para a bancada.
- Numerados do anime: No. 1, 2, 6, 8 (sem controle, pequeno e colossal), 9 e as formas (preta, fusão com o No. 10 e
  a gigante, formiga, com o Isao, Abissal, Neural e Couraçado), 10 (pequeno e gigante), 11, 12, 13, 14 e 15 (as
  duas formas).
- Kaiju do mod: Trichonephila e a Tecedeira Abissal, os quatro Primigenius, Philinosoma, Diclonius, as formigas, os
  cogumelos (o Myxogasterocarp se divide), Preondactyl voador, larva, Polyonax, Lacerta Fratris, Imitatio Mantidis
  (preta e verde, apara de frente), Maxilla Cornuta, Anguilla Ambulans.
- IA: alvo pela função e pela ameaça, busca, travamento, leitura de hábitos (os inteligentes), papéis em grupo
  (agressor, apoio, flanco, líder), fases de chefe, aviso no chão do golpe pesado, resistência crescente dos chefes a
  gelo/veneno/atordoamento, ilusão e ataque mental do No. 15, previsão do No. 1, retirada do No. 9.
- Ataques de kaiju quebram construções; kaiju não ficam presos em cima de prédios.

## Soldados

- Soldado comum por nível com rifle/pistola e faca de apoio; detecta kaiju, recua para recarregar, usa cobertura,
  pula obstáculos e vãos, sai da área do golpe pesado.
- Soldados especiais com modelo, aura, perfil tático e técnicas próprias: Hoshina e Hoshina No. 10, Kikoru e
  Kikoru No. 4, Mina, Reno e Reno No. 6, Narumi e Narumi No. 1, Isao e Isao sem o Numbers 2, Kafka e Kaiju No. 8,
  Soichiro, Jura, Rin, Iharu, Haruichi, Aoi, Hakua, Kota, Tae, Ryo, Konomi (não luta), Miguel e Camilly.
- Fogo, incentivo, escudo, marcas, veneno, gelo em etapas e paralisia entre eles.

## Mundo

- Base Marítima de Ariake e Base de Tachikawa (estruturas grandes, com versão atingida e missões pós-ataque),
  cercadas pelas cidades costeira de Ariake e de Tachikawa e por bairros; Shinagawa; base genérica grande com arena
  de treino; base pequena, posto avançado, prédio destruído, restos de kaiju e torre de vigia.
- Locais com nome na tela, guarnição dos personagens, alerta por região (normal, alerta, evacuação, combate,
  recuperação) e invasões próprias.
- Invasões em ondas com sirene, defensores em esquadrões, surpresas raras (formas do No. 9, No. 8 sem controle) e
  aviso de ameaça grande; evacuação com civis; rastro de destruição investigado com o Rastreador.

## Visual e som

- Modelos do Meshy feitos pelo Miguel com ossos articulados (cotovelo, joelho, pé, peito); trajes 3D no jogador;
  armas no tamanho real.
- Efeitos desenhados (raios, cortes, ondas, feixes, fumaça), sem partículas, crescendo com o tamanho de quem os faz;
  efeito próprio por personagem e por habilidade.
- Sons sintetizados para técnicas, habilidades, tiros, voo dos projéteis, alarmes de invasão (sirene em laço) e a
  ameaça grande; volume dos sons do mod ajustável (`[fx] modSoundVolume`).

## Configuração

- `config/kn8-server.toml` (servidor) e `config/kn8-client.toml` (cliente) com todos os números; dados de jogo em
  datapack (`data/kn8/kn8/...`), recarregados com `/reload`; JSON inválido é recusado com o motivo no log.
- Comandos de administração em `/kn8` (kaiju, invasion, release, host, research, vfx, anim, soldier...).

## Conhecido e em aberto

- Modelos ainda a fazer pelo Miguel: forma vermelha e forma final do No. 9, Narumi No. 1 e Isao sem o Numbers 2
  próprios, civil e Oficial de Missões definitivos, asas e cauda do jogador no modelo.
- Fichas a receber: No. 16 a 20 e a fusão do No. 9 com o No. 12; canhão grande da Mina.
- [DECIDIR] da matriz de duelos (`docs/ETAPA7D_MATRIZ.md`): Soichiro e Jura vencendo o No. 13 sozinhos, Miguel x
  Camilly no mesmo nível, No. 2 e No. 6 abaixo do nível T8.
- Os números marcados [SUPOSIÇÃO] no `docs/BALANCEAMENTO.md` são prévia de balanceamento.
- Testes instáveis conhecidos (`invasionRunsWavesAndBreaks`, `yojuBreaksAStoneBrickWallWhileWalking`) foram
  corrigidos na R-1 e passaram nas rodadas finais.
